package com.maxeydev.picklejot.domain.backup

enum class ImportProblem {
    NOT_READABLE,
    NOT_A_PICKLEJOT_EXPORT,
    NEWER_VERSION,
    CORRUPT_CONTENT,
    TOO_LARGE,
    WRITE_FAILED,
}
