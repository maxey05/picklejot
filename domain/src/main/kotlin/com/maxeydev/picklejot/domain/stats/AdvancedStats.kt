@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.domain.stats

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.person.normalizePersonName
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val MONTHS_PER_YEAR = 12

private val WHITESPACE_RUN = Regex("\\s+")

data class PersonRecord(
    val personId: Uuid,
    val record: WinLoss,
    val lastPlayedOn: AppDate,
)

data class LabelRecord(
    val label: String,
    val record: WinLoss,
    val lastPlayedOn: AppDate,
)

data class MonthRecord(
    val year: Int,
    val month: Int,
    val record: WinLoss?,
)

fun WinLoss.percentIfEnoughMatches(): Int? = if (total >= AdvancedStats.MIN_MATCHES_FOR_PERCENT) winPercent else null

data class AdvancedStats(
    val headToHead: List<PersonRecord>,
    val withPartner: List<PersonRecord>,
    val byLocation: List<LabelRecord>,
    val byPaddle: List<LabelRecord>,
    val monthly: List<MonthRecord>,
    val insights: ProInsights = ProInsights.EMPTY,
) {
    val isEmpty: Boolean
        get() = monthly.isEmpty()

    val topOpponent: PersonRecord?
        get() = headToHead.firstOrNull()

    companion object {
        const val MIN_MATCHES_FOR_PERCENT = 3

        val EMPTY: AdvancedStats =
            AdvancedStats(
                headToHead = emptyList(),
                withPartner = emptyList(),
                byLocation = emptyList(),
                byPaddle = emptyList(),
                monthly = emptyList(),
            )

        fun from(
            lines: Iterable<AdvancedMatchLine>,
            today: AppDate,
        ): AdvancedStats {
            val all = lines.toList()
            if (all.isEmpty()) {
                return EMPTY
            }
            return AdvancedStats(
                headToHead = personRecords(all) { it.opponentIds },
                withPartner = personRecords(all) { listOfNotNull(it.partnerId) },
                byLocation = labelRecords(all) { it.location },
                byPaddle = labelRecords(all) { it.paddle },
                monthly = monthlyRecords(all, today),
                insights = ProInsights.from(all, today),
            )
        }

        private fun personRecords(
            lines: List<AdvancedMatchLine>,
            peopleOf: (AdvancedMatchLine) -> List<Uuid>,
        ): List<PersonRecord> =
            lines
                .flatMap { line -> peopleOf(line).map { personId -> personId to line } }
                .groupBy({ it.first }, { it.second })
                .map { (personId, played) ->
                    PersonRecord(
                        personId = personId,
                        record = recordOf(played),
                        lastPlayedOn = played.maxOf { it.date },
                    )
                }.sortedWith(
                    compareByDescending<PersonRecord> { it.record.total }
                        .thenByDescending { it.lastPlayedOn }
                        .thenBy { it.personId.toString() },
                )

        private fun labelRecords(
            lines: List<AdvancedMatchLine>,
            labelOf: (AdvancedMatchLine) -> String?,
        ): List<LabelRecord> =
            lines
                .mapNotNull { line -> cleanLabel(labelOf(line))?.let { it to line } }
                .groupBy({ normalizePersonName(it.first) }, { it })
                .map { (_, entries) ->
                    val latest = entries.maxByOrNull { it.second.date }!!
                    LabelRecord(
                        label = latest.first,
                        record = recordOf(entries.map { it.second }),
                        lastPlayedOn = latest.second.date,
                    )
                }.sortedWith(
                    compareByDescending<LabelRecord> { it.record.total }
                        .thenByDescending { it.lastPlayedOn }
                        .thenBy { normalizePersonName(it.label) },
                )

        private fun monthlyRecords(
            lines: List<AdvancedMatchLine>,
            today: AppDate,
        ): List<MonthRecord> {
            val byMonth = lines.groupBy { monthIndex(it.date) }
            val first = byMonth.keys.min()
            val last = maxOf(byMonth.keys.max(), monthIndex(today))
            return (last downTo first).map { index ->
                MonthRecord(
                    year = index.floorDiv(MONTHS_PER_YEAR),
                    month = index.mod(MONTHS_PER_YEAR) + 1,
                    record = byMonth[index]?.let(::recordOf),
                )
            }
        }

        private fun cleanLabel(raw: String?): String? =
            raw
                ?.trim()
                ?.replace(WHITESPACE_RUN, " ")
                ?.takeIf { it.isNotEmpty() }

        private fun monthIndex(date: AppDate): Int = date.year * MONTHS_PER_YEAR + date.month.ordinal

        private fun recordOf(lines: List<AdvancedMatchLine>): WinLoss =
            WinLoss(
                wins = lines.count { it.result == MatchResult.WIN },
                losses = lines.count { it.result == MatchResult.LOSS },
            )
    }
}
