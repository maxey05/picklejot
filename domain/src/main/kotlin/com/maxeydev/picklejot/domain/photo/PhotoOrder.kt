@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.domain.photo

import kotlin.uuid.ExperimentalUuidApi

fun <T> List<T>.movedTo(
    from: Int,
    to: Int,
): List<T> {
    require(from in indices) { "Cannot move item $from of a list of $size." }
    if (to !in indices || from == to) {
        return this
    }
    val reordered = toMutableList()
    val item = reordered.removeAt(from)
    reordered.add(to, item)
    return reordered
}

fun List<PhotoRef>.inDisplayOrder(): List<PhotoRef> = sortedBy { it.sortIndex }

fun List<PhotoRef>.primaryPhoto(): PhotoRef? = minByOrNull { it.sortIndex }

fun List<PhotoRef>.reindexed(): List<PhotoRef> = mapIndexed { index, photo -> photo.copy(sortIndex = index) }
