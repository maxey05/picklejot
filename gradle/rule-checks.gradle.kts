import org.w3c.dom.Element

fun sourceTree(): Sequence<File> {
    val self = rootDir.resolve("gradle/rule-checks.gradle.kts").canonicalFile
    return rootDir.walkTopDown()
        .onEnter { dir ->
            dir.name !in setOf("build", "bin", "out", "gen", ".git", ".gradle", ".idea", "node_modules", ".kotlin")
        }
        .filter { it.isFile && it.canonicalFile != self }
}

fun uncommentedLines(file: File): List<Pair<Int, String>> =
    file.readLines()
        .mapIndexed { index, line -> (index + 1) to line }
        .filterNot { (_, line) ->
            val t = line.trim()
            t.startsWith("//") || t.startsWith("#") || t.startsWith("*") || t.startsWith("/*")
        }
        .map { (number, line) -> number to line.substringBefore("//").substringBefore(" #") }

fun report(
    taskName: String,
    rule: String,
    hits: List<String>,
    explanation: String,
) {
    if (hits.isEmpty()) return
    throw GradleException(
        buildString {
            appendLine("$taskName FAILED — $rule")
            appendLine(explanation)
            appendLine()
            hits.forEach { appendLine("  $it") }
        },
    )
}

val checkNoVersionLiterals by tasks.registering {
    group = "verification"
    description = "Fails if any build.gradle.kts contains a quoted semantic-version literal (R2, AC-1.5)."
    doLast {
        val quotedLiteral = Regex("\"([^\"\\n]*)\"")
        val versionInside = Regex("\\d+\\.\\d+")
        val hits =
            sourceTree()
                .filter { it.name == "build.gradle.kts" }
                .flatMap { file ->
                    uncommentedLines(file)
                        .filter { (_, line) ->
                            quotedLiteral.findAll(line).any { versionInside.containsMatchIn(it.groupValues[1]) }
                        }
                        .map { (number, line) ->
                            "${file.relativeTo(rootDir)}:$number  ${line.trim()}"
                        }
                }
                .toList()
        report(
            "checkNoVersionLiterals",
            "R2 / AC-1.5",
            hits,
            "Move the version into gradle/libs.versions.toml and reference it through the catalog.\n" +
                "One place to read, one place to change.",
        )
    }
}

val checkNoDestructiveMigration by tasks.registering {
    group = "verification"
    description = "Fails if fallbackToDestructiveMigration appears anywhere in the tree (R32, AC-1.12)."
    doLast {
        val hits =
            sourceTree()
                .filter { it.extension in setOf("kt", "kts", "java") }
                .flatMap { file ->
                    uncommentedLines(file)
                        .filter { (_, line) -> line.contains("fallbackToDestructiveMigration") }
                        .map { (number, _) -> "${file.relativeTo(rootDir)}:$number" }
                }
                .toList()
        report(
            "checkNoDestructiveMigration",
            "R32 / AC-1.12",
            hits,
            "fallbackToDestructiveMigration silently deletes the user's entire match history on a\n" +
                "schema mismatch. Write a versioned Migration and test it with MigrationTestHelper\n" +
                "against a POPULATED database (R33, R80).",
        )
    }
}

val checkNoReadMediaImages by tasks.registering {
    group = "verification"
    description = "Fails if READ_MEDIA_IMAGES appears in any manifest, source or merged (R49, AC-1.13)."
    doLast {
        val sourceManifests = sourceTree().filter { it.name == "AndroidManifest.xml" }
        val mergedManifests =
            rootDir.walkTopDown()
                .filter { it.isFile && it.name == "AndroidManifest.xml" && it.path.contains("intermediates") }
        val xmlComment = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
        val hits =
            (sourceManifests + mergedManifests)
                .filter { xmlComment.replace(it.readText(), "").contains("READ_MEDIA_IMAGES") }
                .map { it.relativeTo(rootDir).path }
                .distinct()
                .toList()
        report(
            "checkNoReadMediaImages",
            "R49 / AC-1.13",
            hits,
            "The Android photo picker needs no runtime permission, so READ_MEDIA_IMAGES is both\n" +
                "unnecessary and an invitation to Play policy scrutiny over restricted permissions.\n" +
                "Use ActivityResultContracts.PickVisualMedia instead (F9, FR-11).",
        )
    }
}

val checkNoPrereleaseVersions by tasks.registering {
    group = "verification"
    description = "Fails if the version catalog pins an alpha, beta, rc or snapshot (R5, AC-1.14)."
    doLast {
        val catalog = rootDir.resolve("gradle/libs.versions.toml")
        val markers = listOf("-alpha", "-beta", "-rc", "-SNAPSHOT")
        val hits =
            uncommentedLines(catalog)
                .filter { (_, line) -> markers.any { line.contains(it, ignoreCase = true) } }
                .map { (number, line) -> "gradle/libs.versions.toml:$number  ${line.trim()}" }
        report(
            "checkNoPrereleaseVersions",
            "R5 / AC-1.14",
            hits,
            "Release builds depend only on stable versions. WorkManager 2.12.0-rc01 and Glance\n" +
                "1.3.0-alpha02 exist and are explicitly out.\n" +
                "Note this scans the catalog TEXT, so a dynamic selector such as `latest.release`\n" +
                "would hide what it resolves to. Test dependencies are pinned for that reason.",
        )
    }
}

val checkNoKeystoreCommitted by tasks.registering {
    group = "verification"
    description = "Fails if a .jks or .keystore file is tracked or staged in git (R10, AC-1.11)."
    doLast {
        if (!rootDir.resolve(".git").exists()) {
            logger.lifecycle("checkNoKeystoreCommitted: not a git repository yet — nothing to check.")
            return@doLast
        }

        fun git(vararg args: String): List<String> {
            val result =
                providers.exec {
                    commandLine(listOf("git") + args)
                    workingDir = rootDir
                    isIgnoreExitValue = true
                }
            return result.standardOutput.asText.get().lines().filter { it.isNotBlank() }
        }
        val suspicious =
            (git("ls-files") + git("diff", "--cached", "--name-only"))
                .filter { it.endsWith(".jks") || it.endsWith(".keystore") }
                .distinct()
        report(
            "checkNoKeystoreCommitted",
            "R10 / AC-1.11",
            suspicious,
            "Signing material must never enter git history. Remove it from the index\n" +
                "(`git rm --cached <file>`) and confirm .gitignore covers keystore/*.jks.\n" +
                "A key that reaches a public remote must be replaced, not merely un-committed.",
        )
    }
}

val checkDomainDateTimeIndirection by tasks.registering {
    group = "verification"
    description = "Fails if a :domain file reaches past the AppDate/AppTime/AppInstant typealiases (R6, AC-2.25)."
    doLast {
        val domainRoot = rootDir.resolve("domain")
        val indirection =
            rootDir
                .resolve("domain/src/main/kotlin/com/maxeydev/picklejot/domain/datetime/DateTime.kt")
                .canonicalFile
        val reachingPast = listOf("kotlinx.datetime", "kotlin.time.Instant")
        val hits =
            sourceTree()
                .filter { it.extension == "kt" && it.canonicalFile.startsWith(domainRoot) }
                .filter { it.canonicalFile != indirection }
                .flatMap { file ->
                    uncommentedLines(file)
                        .filter { (_, line) -> reachingPast.any { line.contains(it) } }
                        .map { (number, line) -> "${file.relativeTo(rootDir)}:$number  ${line.trim()}" }
                }
                .toList()
        report(
            "checkDomainDateTimeIndirection",
            "R6 / AC-2.25",
            hits,
            "kotlinx-datetime is pre-1.0 and its Instant is already deprecated in favour of\n" +
                "kotlin.time.Instant. :domain reaches the date/time library ONLY through\n" +
                "domain/datetime/DateTime.kt, so the next breaking change costs one file, not forty.\n" +
                "Use AppDate / AppTime / AppInstant.",
        )
    }
}

val checkSchemaJsonCommitted by tasks.registering {
    group = "verification"
    description = "Fails if Room's exported schema JSON for the current version is absent (R33, AC-2.11)."
    doLast {
        val databaseSource =
            rootDir.resolve("data/src/main/kotlin/com/maxeydev/picklejot/data/db/PicklejotDatabase.kt")
        if (!databaseSource.exists()) {
            logger.lifecycle("checkSchemaJsonCommitted: no Room database yet — nothing to check.")
            return@doLast
        }
        val version =
            Regex("PICKLEJOT_DB_VERSION\\s*=\\s*(\\d+)")
                .find(databaseSource.readText())
                ?.groupValues
                ?.get(1)
                ?: throw GradleException("checkSchemaJsonCommitted could not read PICKLEJOT_DB_VERSION.")
        val exported =
            rootDir
                .resolve("data/schemas")
                .walkTopDown()
                .filter { it.isFile && it.name == "$version.json" }
                .toList()
        val hits =
            if (exported.isEmpty()) {
                listOf("data/schemas/**/$version.json is missing")
            } else {
                emptyList()
            }
        report(
            "checkSchemaJsonCommitted",
            "R33 / AC-2.11",
            hits,
            "Room exports the schema through room.schemaLocation and it must be committed.\n" +
                "MigrationTestHelper cannot run without it, so a missing file means the next\n" +
                "migration ships untested. Build :data, then commit data/schemas/.",
        )
    }
}

fun rulesRoot(file: File): Element =
    javax.xml.parsers.DocumentBuilderFactory
        .newInstance()
        .newDocumentBuilder()
        .parse(file)
        .documentElement

fun Element.childElements(name: String): List<Element> =
    (0 until childNodes.length)
        .map { childNodes.item(it) }
        .filterIsInstance<Element>()
        .filter { it.tagName == name }

fun Element.excludes(
    domain: String,
    path: String,
): Boolean =
    childElements("exclude").any {
        it.getAttribute("domain") == domain && it.getAttribute("path").trimEnd('/') == path
    }

val checkBackupRules by tasks.registering {
    group = "verification"
    description = "Fails unless both Auto Backup rules files keep photos out of cloud backup (F48, AC-14.19)."
    doLast {
        val xmlDirectory = rootDir.resolve("app/src/main/res/xml")
        val extraction = xmlDirectory.resolve("data_extraction_rules.xml")
        val legacy = xmlDirectory.resolve("full_backup_content.xml")
        val manifest = rootDir.resolve("app/src/main/AndroidManifest.xml")
        val problems = mutableListOf<String>()

        if (!extraction.exists()) {
            problems += "app/src/main/res/xml/data_extraction_rules.xml is missing (the API 31+ file)"
        } else {
            val root = rulesRoot(extraction)
            val cloud = root.childElements("cloud-backup").singleOrNull()
            val transfer = root.childElements("device-transfer").singleOrNull()
            if (cloud == null) {
                problems += "data_extraction_rules.xml has no <cloud-backup> section"
            } else {
                if (!cloud.excludes("file", "photos")) {
                    problems += "data_extraction_rules.xml does not exclude photos/ from <cloud-backup>"
                }
            }
            if (transfer == null) {
                problems += "data_extraction_rules.xml has no <device-transfer> section"
            } else if (transfer.excludes("file", "photos")) {
                problems += "data_extraction_rules.xml excludes photos/ from <device-transfer>; it must keep them"
            }
        }

        if (!legacy.exists()) {
            problems += "app/src/main/res/xml/full_backup_content.xml is missing (the API 30 and below file)"
        } else {
            val root = rulesRoot(legacy)
            if (!root.excludes("file", "photos")) {
                problems += "full_backup_content.xml does not exclude photos/"
            }
        }

        val databaseBackup = rootDir.resolve("data/src/main/kotlin/com/maxeydev/picklejot/data/db/DatabaseBackup.kt")
        if (!databaseBackup.exists() || !databaseBackup.readText().contains("context.noBackupFilesDir")) {
            problems += "DatabaseBackup.kt must keep the pre-migration copy under noBackupFilesDir, which Auto Backup " +
                "never includes (AC-14.18)"
        }

        val manifestText = manifest.readText().replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
        if (!manifestText.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\"")) {
            problems += "AndroidManifest.xml does not reference @xml/data_extraction_rules"
        }
        if (!manifestText.contains("android:fullBackupContent=\"@xml/full_backup_content\"")) {
            problems += "AndroidManifest.xml does not reference @xml/full_backup_content"
        }

        report(
            "checkBackupRules",
            "F48 / AC-14.16 to AC-14.19",
            problems,
            "Both rules files are required. Shipping only one silently does nothing on the other API\n" +
                "branch, and photos left in cloud backup can push it past the 25 MB quota, which makes\n" +
                "Android skip the whole backup, database included (R37, R38).",
        )
    }
}

tasks.register("checkRules") {
    group = "verification"
    description = "Runs every mechanically-enforceable RULES.md check (R113)."
    dependsOn(
        checkNoVersionLiterals,
        checkNoDestructiveMigration,
        checkNoReadMediaImages,
        checkNoPrereleaseVersions,
        checkNoKeystoreCommitted,
        checkDomainDateTimeIndirection,
        checkSchemaJsonCommitted,
        checkBackupRules,
    )
}
