@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.domain.match

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.person.Person
import com.maxeydev.picklejot.domain.photo.PhotoRef
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Match(
    val id: Uuid,
    val format: MatchFormat,
    val date: AppDate,
    val result: MatchResult,
    val createdAt: AppInstant,
    val updatedAt: AppInstant,
    val startTime: AppTime? = null,
    val endTime: AppTime? = null,
    val location: String? = null,
    val opponents: List<Person> = emptyList(),
    val partner: Person? = null,
    val games: List<GameScore> = emptyList(),
    val paddle: String? = null,
    val notes: String? = null,
    val photos: List<PhotoRef> = emptyList(),
)
