@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.photo

import java.io.File
import java.io.IOException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

const val PHOTO_DIRECTORY = "photos"

class PhotoStore(
    private val rootDirectory: File,
) {
    fun resolve(relativePath: String): File {
        val root = rootDirectory.canonicalFile
        val file = File(root, relativePath).canonicalFile
        require(file != root && file.toPath().startsWith(root.toPath())) {
            "A photo path must resolve to a file inside the app's own storage."
        }
        return file
    }

    fun newPhotoPath(): String = "$PHOTO_DIRECTORY/${Uuid.random()}.jpg"

    fun deletePhotoFiles(relativePaths: List<String>) {
        relativePaths.forEach { relativePath ->
            val file = resolve(relativePath)
            if (file.exists() && !file.delete()) {
                throw IOException("A photo file that is no longer used could not be removed.")
            }
        }
    }

    fun deleteAll() {
        val directory = resolve(PHOTO_DIRECTORY)
        directory.listFiles().orEmpty().forEach { file ->
            if (!file.deleteRecursively()) {
                throw IOException("A photo file could not be removed.")
            }
        }
    }

    fun deleteOrphans(
        referencedPaths: Set<String>,
        olderThanMillis: Long,
    ): List<String> {
        val directory = resolve(PHOTO_DIRECTORY)
        val files = directory.listFiles() ?: return emptyList()
        val referencedFiles = referencedPaths.map { resolve(it) }.toSet()
        val orphans =
            files.filter { file ->
                file.isFile && file.canonicalFile !in referencedFiles && file.lastModified() < olderThanMillis
            }
        orphans.forEach { it.delete() }
        return orphans.map { "$PHOTO_DIRECTORY/${it.name}" }
    }
}
