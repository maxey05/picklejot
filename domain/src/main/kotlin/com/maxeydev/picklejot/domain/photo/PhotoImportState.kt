package com.maxeydev.picklejot.domain.photo

sealed interface PhotoImportState {
    data object Importing : PhotoImportState

    data class Ready(
        val photo: ImportedPhoto,
    ) : PhotoImportState

    data object Failed : PhotoImportState
}
