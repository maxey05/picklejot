package com.maxeydev.picklejot.ui.settings

import androidx.annotation.StringRes
import com.maxeydev.picklejot.domain.backup.ImportProblem
import com.maxeydev.picklejot.ui.R

enum class BackupMessage(
    @StringRes val text: Int,
) {
    EXPORT_FAILED(R.string.backup_error_export),
    NOT_READABLE(R.string.backup_error_not_readable),
    NOT_AN_EXPORT(R.string.backup_error_not_export),
    NEWER_VERSION(R.string.backup_error_newer),
    CORRUPT(R.string.backup_error_corrupt),
    TOO_LARGE(R.string.backup_error_too_large),
    WRITE_FAILED(R.string.backup_error_write),
    ;

    companion object {
        fun of(problem: ImportProblem): BackupMessage =
            when (problem) {
                ImportProblem.NOT_READABLE -> NOT_READABLE
                ImportProblem.NOT_A_PICKLEJOT_EXPORT -> NOT_AN_EXPORT
                ImportProblem.NEWER_VERSION -> NEWER_VERSION
                ImportProblem.CORRUPT_CONTENT -> CORRUPT
                ImportProblem.TOO_LARGE -> TOO_LARGE
                ImportProblem.WRITE_FAILED -> WRITE_FAILED
            }
    }
}
