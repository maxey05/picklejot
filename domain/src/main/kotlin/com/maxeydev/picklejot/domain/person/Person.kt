@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.domain.person

import com.maxeydev.picklejot.domain.datetime.AppInstant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Person(
    val id: Uuid,
    val displayName: String,
    val createdAt: AppInstant,
)
