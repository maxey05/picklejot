package com.maxeydev.picklejot.data.match

internal const val OPPONENTS_OF_OUTER_MATCH =
    "FROM match_person mp INNER JOIN person p ON p.id = mp.person_id " +
        "WHERE mp.match_id = m.id AND mp.role = '${MatchPersonEntity.ROLE_OPPONENT}' ORDER BY mp.slot"

internal const val FIRST_OPPONENT_NAME = "(SELECT p.display_name $OPPONENTS_OF_OUTER_MATCH LIMIT 1)"

internal const val SECOND_OPPONENT_NAME = "(SELECT p.display_name $OPPONENTS_OF_OUTER_MATCH LIMIT 1 OFFSET 1)"

internal const val FIRST_OPPONENT_SORT_KEY = "(SELECT p.normalized_name $OPPONENTS_OF_OUTER_MATCH LIMIT 1)"

internal const val PARTNER_NAME =
    "(SELECT p.display_name FROM match_person mp INNER JOIN person p ON p.id = mp.person_id " +
        "WHERE mp.match_id = m.id AND mp.role = '${MatchPersonEntity.ROLE_PARTNER}' LIMIT 1)"

internal const val PRIMARY_PHOTO_PATH =
    "(SELECT ph.relative_path FROM photo ph WHERE ph.match_id = m.id ORDER BY ph.sort_index LIMIT 1)"

internal const val MATCH_LIST_SELECT =
    "SELECT m.id AS id, m.date AS date, m.format AS format, m.result AS result, " +
        "$FIRST_OPPONENT_NAME AS first_opponent, " +
        "$SECOND_OPPONENT_NAME AS second_opponent, " +
        "$PARTNER_NAME AS partner_name, " +
        "m.location AS location, " +
        "$PRIMARY_PHOTO_PATH AS primary_photo_path " +
        "FROM `match` m"

internal const val NEWEST_FIRST = "m.date DESC, m.created_at DESC, m.id ASC"

internal const val OLDEST_FIRST = "m.date ASC, m.created_at ASC, m.id ASC"

internal const val WINS_BEFORE_LOSSES = "CASE m.result WHEN 'WIN' THEN 0 ELSE 1 END"

internal const val LOSSES_BEFORE_WINS = "CASE m.result WHEN 'LOSS' THEN 0 ELSE 1 END"

internal const val NO_OPPONENT_LAST_THEN_A_TO_Z =
    "$FIRST_OPPONENT_SORT_KEY IS NULL, $FIRST_OPPONENT_SORT_KEY ASC"

internal const val PAGE_LIMIT = "LIMIT :limit"

internal const val MATCH_LIST_FILTER =
    "WHERE (:format IS NULL OR m.format = :format) " +
        "AND (:result IS NULL OR m.result = :result) " +
        "AND (:fromDate IS NULL OR m.date >= :fromDate) " +
        "AND (:toDate IS NULL OR m.date <= :toDate) " +
        "AND (:location IS NULL OR m.location = :location) " +
        "AND (:opponentId IS NULL OR EXISTS (SELECT 1 FROM match_person fo " +
        "WHERE fo.match_id = m.id AND fo.role = '${MatchPersonEntity.ROLE_OPPONENT}' " +
        "AND fo.person_id = :opponentId)) " +
        "AND (:textPattern IS NULL " +
        "OR m.location LIKE :textPattern ESCAPE '\\' " +
        "OR m.paddle LIKE :textPattern ESCAPE '\\' " +
        "OR m.notes LIKE :textPattern ESCAPE '\\' " +
        "OR EXISTS (SELECT 1 FROM match_person sp INNER JOIN person sq ON sq.id = sp.person_id " +
        "WHERE sp.match_id = m.id AND sq.normalized_name LIKE :namePattern ESCAPE '\\'))"

internal const val MATCH_STAT_SELECT = "SELECT m.date AS date, m.format AS format, m.result AS result FROM `match` m"

internal const val ADVANCED_MATCH_SELECT =
    "SELECT m.id AS id, m.date AS date, m.format AS format, m.result AS result, " +
        "m.location AS location, m.paddle AS paddle FROM `match` m"

internal const val MATCH_PEOPLE_FOR_FILTER =
    "SELECT mp.match_id AS match_id, mp.person_id AS person_id, mp.role AS role FROM match_person mp " +
        "WHERE mp.match_id IN (SELECT m.id FROM `match` m $MATCH_LIST_FILTER)"

internal const val FILTERED_MATCH_LIST = "$MATCH_LIST_SELECT $MATCH_LIST_FILTER"

private const val START_SECONDS = "CAST(strftime('%s', m.start_time) AS INTEGER)"

private const val END_SECONDS = "CAST(strftime('%s', m.end_time) AS INTEGER)"

internal const val DURATION_SECONDS =
    "(CASE WHEN m.start_time IS NULL OR m.end_time IS NULL THEN NULL " +
        "WHEN $END_SECONDS >= $START_SECONDS THEN $END_SECONDS - $START_SECONDS " +
        "ELSE $END_SECONDS - $START_SECONDS + 86400 END)"

internal const val NO_DURATION_LAST = "$DURATION_SECONDS IS NULL"

internal const val NO_LOCATION_LAST_THEN_A_TO_Z =
    "(m.location IS NULL OR m.location = ''), COALESCE(m.location, '') COLLATE NOCASE ASC"
