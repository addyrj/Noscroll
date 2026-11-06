package com.noreels.ledger

/** One committed short-video session. Seconds are counted per second, never rounded up. */
data class SessionEntity(
    val date: String,
    val app: String,
    val startTs: Long,
    val endTs: Long,
    val secondsUsed: Int,
)

/** One row per local calendar day (yyyy-MM-dd). */
data class DailyRow(
    val date: String,
    val usedSec: Int,
    val sessions: Int,
    val blocked: Int,
    val limitSec: Int,
)

/** A low-confidence detection or a Strict Mode dismissal, shown in the in-app log. */
data class LogEntry(
    val ts: Long,
    val kind: String,
    val app: String,
    val detail: String,
)
