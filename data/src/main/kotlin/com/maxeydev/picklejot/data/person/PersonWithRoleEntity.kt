package com.maxeydev.picklejot.data.person

import androidx.room.Embedded
import androidx.room.Relation
import com.maxeydev.picklejot.data.match.MatchPersonEntity

data class PersonWithRoleEntity(
    @Embedded
    val link: MatchPersonEntity,
    @Relation(parentColumn = "person_id", entityColumn = "id")
    val person: PersonEntity,
)
