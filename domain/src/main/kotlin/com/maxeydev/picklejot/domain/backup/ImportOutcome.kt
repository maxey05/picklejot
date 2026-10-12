package com.maxeydev.picklejot.domain.backup

data class ImportSummary(
    val added: Int,
    val alreadyPresent: Int,
    val heldBack: Int,
    val photosNotRestored: Int,
)

sealed interface ImportOutcome {
    data class Imported(
        val summary: ImportSummary,
    ) : ImportOutcome

    data class Rejected(
        val problem: ImportProblem,
    ) : ImportOutcome
}
