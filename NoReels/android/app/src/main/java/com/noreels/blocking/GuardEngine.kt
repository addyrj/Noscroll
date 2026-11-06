package com.noreels.blocking

import android.content.Context
import com.noreels.ledger.DailyRow
import com.noreels.ledger.GuardSettings
import com.noreels.ledger.LedgerDatabase
import com.noreels.ledger.LogEntry
import com.noreels.ledger.SessionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONObject

enum class BlockReason {
  COOLDOWN,
  DAILY,
  APP_LIMIT,
  NIGHT,
  FOCUS,
}

data class TickResult(
    val block: BlockReason? = null,
    /** Seconds left before the overlay appears, or null when no session is running. */
    val remainingSec: Int? = null,
    val warn: Boolean = false,
)

/**
 * The timing state machine. Single source of truth for session, daily budget, cooldown and focus
 * state. All mutation goes through synchronized methods; DB writes go to a background thread.
 *
 * Leaving the feed pauses the session. Coming back continues it, so leaving at 4:59 does not buy a
 * fresh 5 minutes. A paused session is committed (without extra cooldown) once the user has stayed
 * away for a full cooldown period.
 */
class GuardEngine private constructor(context: Context) {
  private val db = LedgerDatabase.get(context)
  private val io = Executors.newSingleThreadExecutor()

  @Volatile
  var settings: GuardSettings = GuardSettings.fromJson(db.getKv(K_SETTINGS))
    private set

  private var day = ""
  private var dailyUsedSec = 0
  private var sessionsToday = 0
  private var blockedToday = 0
  private var sessionActive = false
  private var sessionElapsedSec = 0
  private var sessionStartTs = 0L
  private var sessionApp = ""
  private var lastInFeedTs = 0L
  private var cooldownUntil = 0L
  private var focusUntil = 0L
  private var warnedThisSession = false
  private var msAccumulator = 0L
  private var blockEdgeShown = false
  private var lastPersistTs = 0L
  private val lastLowLogTs = HashMap<String, Long>()
  private val dailyAppUsedSec = HashMap<String, Int>()

  init {
    val now = System.currentTimeMillis()
    day = db.getKv(K_DAY) ?: dateKey(now)
    db.getDaily(day)?.let {
      dailyUsedSec = it.usedSec
      sessionsToday = it.sessions
      blockedToday = it.blocked
    }
    sessionActive = db.getKv(K_SESSION_ACTIVE) == "1"
    sessionElapsedSec = db.getKv(K_SESSION_ELAPSED)?.toIntOrNull() ?: 0
    sessionStartTs = db.getKv(K_SESSION_START)?.toLongOrNull() ?: 0L
    sessionApp = db.getKv(K_SESSION_APP) ?: ""
    lastInFeedTs = db.getKv(K_LAST_IN_FEED)?.toLongOrNull() ?: 0L
    cooldownUntil = db.getKv(K_COOLDOWN_UNTIL)?.toLongOrNull() ?: 0L
    focusUntil = db.getKv(K_FOCUS_UNTIL)?.toLongOrNull() ?: 0L
    loadDailyAppUsage()
    rollDayIfNeeded(now)
    persistNow()
  }

  private fun loadDailyAppUsage() {
    dailyAppUsedSec.clear()
    val raw = db.getKv(K_APP_USAGE)
    if (!raw.isNullOrBlank()) {
      try {
        val obj = JSONObject(raw)
        obj.keys().forEach { dailyAppUsedSec[it] = obj.optInt(it) }
      } catch (_: Exception) {}
    }
  }

  // ---- Ticking (called by ShortBlockerService about every 500 ms) ----

  @Synchronized
  fun tick(feed: Detection?, focusAppOpen: Boolean, deltaMs: Long, now: Long): TickResult {
    rollDayIfNeeded(now)

    if (focusAppOpen && focusUntil > now) return blockedEntry(BlockReason.FOCUS)

    if (feed == null) {
      msAccumulator = 0
      blockEdgeShown = false
      expirePausedSession(now)
      persistIfDue(now)
      return TickResult()
    }

    blockReasonAt(now, feed.appKey)?.let { return blockedEntry(it) }

    if (!sessionActive) startSession(feed.appKey, now)
    lastInFeedTs = now
    msAccumulator += deltaMs.coerceIn(0, MAX_TICK_MS)
    var warn = false
    while (msAccumulator >= 1000) {
      msAccumulator -= 1000
      sessionElapsedSec++
      dailyUsedSec++
      dailyAppUsedSec[feed.appKey] = (dailyAppUsedSec[feed.appKey] ?: 0) + 1
      if (!warnedThisSession && remainingSec() <= 60) {
        warnedThisSession = true
        warn = true
      }
      val appLimitSec = settings.appTimeLimitSec(feed.appKey)
      val appUsed = dailyAppUsedSec[feed.appKey] ?: 0
      if (remainingSec() <= 0 || (appLimitSec != null && appUsed >= appLimitSec)) {
        commitSession(now, startCooldown = true)
        blockEdgeShown = true
        return TickResult(block = blockReasonAt(now, feed.appKey), warn = warn)
      }
    }
    persistIfDue(now)
    return TickResult(remainingSec = remainingSec(), warn = warn)
  }

  private fun blockedEntry(reason: BlockReason): TickResult {
    if (!blockEdgeShown) {
      blockEdgeShown = true
      blockedToday++
      persistNow()
    }
    msAccumulator = 0
    return TickResult(block = reason)
  }

  private fun effectiveSessionLimitSec(): Int =
      if (settings.curiousMode) settings.curiousTimeSec else settings.sessionLimitSec

  private fun remainingSec() =
      minOf(effectiveSessionLimitSec() - sessionElapsedSec, settings.dailyLimitSec - dailyUsedSec)

  private fun blockReasonAt(now: Long, appKey: String? = null): BlockReason? =
      when {
        settings.nightSchedule && isNight(now, settings.nightStartHour, settings.nightEndHour) -> BlockReason.NIGHT
        dailyUsedSec >= settings.dailyLimitSec -> BlockReason.DAILY
        appKey != null && isAppLimitExceeded(appKey) -> BlockReason.APP_LIMIT
        cooldownUntil > now -> BlockReason.COOLDOWN
        else -> null
      }

  private fun isAppLimitExceeded(appKey: String): Boolean {
    val limitSec = settings.appTimeLimitSec(appKey) ?: return false
    return (dailyAppUsedSec[appKey] ?: 0) >= limitSec
  }

  private fun startSession(app: String, now: Long) {
    sessionActive = true
    sessionElapsedSec = 0
    sessionStartTs = now
    sessionApp = app
    warnedThisSession = false
    sessionsToday++
    persistNow()
  }

  private fun commitSession(now: Long, startCooldown: Boolean) {
    if (sessionActive && sessionElapsedSec > 0) {
      val s = SessionEntity(day, sessionApp, sessionStartTs, now, sessionElapsedSec)
      io.execute { db.insertSession(s) }
    }
    sessionActive = false
    sessionElapsedSec = 0
    warnedThisSession = false
    msAccumulator = 0
    if (startCooldown) cooldownUntil = now + settings.cooldownMs
    persistNow()
  }

  private fun expirePausedSession(now: Long) {
    if (sessionActive && now - lastInFeedTs >= settings.cooldownMs) {
      commitSession(lastInFeedTs, startCooldown = false)
    }
  }

  @Synchronized
  fun rollDayIfNeeded(now: Long = System.currentTimeMillis()) {
    val today = dateKey(now)
    if (today == day) return
    if (sessionActive) commitSession(now, startCooldown = false)
    day = today
    dailyUsedSec = 0
    sessionsToday = 0
    blockedToday = 0
    dailyAppUsedSec.clear()
    persistNow()
  }

  // ---- Persistence ----

  private fun persistIfDue(now: Long) {
    if (now - lastPersistTs >= PERSIST_EVERY_MS) persistNow()
  }

  private fun persistNow() {
    lastPersistTs = System.currentTimeMillis()
    val daily = DailyRow(day, dailyUsedSec, sessionsToday, blockedToday, settings.dailyLimitSec)
    val kv =
        mapOf(
            K_DAY to day,
            K_SESSION_ACTIVE to if (sessionActive) "1" else "0",
            K_SESSION_ELAPSED to sessionElapsedSec.toString(),
            K_SESSION_START to sessionStartTs.toString(),
            K_SESSION_APP to sessionApp,
            K_LAST_IN_FEED to lastInFeedTs.toString(),
            K_COOLDOWN_UNTIL to cooldownUntil.toString(),
            K_FOCUS_UNTIL to focusUntil.toString(),
            K_APP_USAGE to JSONObject(dailyAppUsedSec as Map<*, *>).toString(),
        )
    io.execute {
      db.upsertDaily(daily)
      db.putKvs(kv)
    }
  }

  @Synchronized fun flush() = persistNow()

  // ---- Settings, focus ----

  @Synchronized
  fun updateSettings(newSettings: GuardSettings): GuardSettings {
    settings = newSettings.clamped()
    val json = settings.toJson().toString()
    io.execute { db.putKvs(mapOf(K_SETTINGS to json)) }
    persistNow()
    return settings
  }

  @Synchronized
  fun startFocus(minutes: Int, now: Long = System.currentTimeMillis()) {
    focusUntil = maxOf(focusUntil, now + minutes.coerceIn(30, 50) * 60_000L)
    persistNow()
  }

  // ---- Logging ----

  /** Logs a LOW-confidence detection, rate limited per app. Returns true when it was logged. */
  @Synchronized
  fun logUncertain(d: Detection, now: Long = System.currentTimeMillis()): Boolean {
    if (now - (lastLowLogTs[d.appKey] ?: 0L) < LOW_LOG_INTERVAL_MS) return false
    lastLowLogTs[d.appKey] = now
    io.execute { db.insertLog(LogEntry(now, "uncertain", d.appKey, d.detail)) }
    return true
  }

  fun logDismissal(reason: String, block: BlockReason) {
    val entry = LogEntry(System.currentTimeMillis(), "strict_dismiss", block.name, reason)
    io.execute { db.insertLog(entry) }
  }

  // ---- Read models for the UI ----

  @Synchronized fun cooldownRemainingSec(now: Long = System.currentTimeMillis()) =
      ((cooldownUntil - now).coerceAtLeast(0) / 1000).toInt()

  @Synchronized fun focusRemainingSec(now: Long = System.currentTimeMillis()) =
      ((focusUntil - now).coerceAtLeast(0) / 1000).toInt()

  @Synchronized
  fun status(serviceRunning: Boolean, now: Long = System.currentTimeMillis()): JSONObject {
    rollDayIfNeeded(now)
    expirePausedSession(now)
    return JSONObject().apply {
      put("serviceRunning", serviceRunning)
      put("dailyUsedSec", dailyUsedSec)
      put("dailyLimitSec", settings.dailyLimitSec)
      put("sessionActive", sessionActive)
      put("sessionInFeed", sessionActive && now - lastInFeedTs < 2_000)
      put("sessionElapsedSec", sessionElapsedSec)
      put("sessionLimitSec", effectiveSessionLimitSec())
      put("cooldownRemainingSec", cooldownRemainingSec(now))
      put("focusRemainingSec", focusRemainingSec(now))
      put("sessionsToday", sessionsToday)
      put("blockedToday", blockedToday)
      put("nightActive", settings.nightSchedule && isNight(now, settings.nightStartHour, settings.nightEndHour))
      put("block", blockReasonAt(now)?.name ?: JSONObject.NULL)
      put("streak", streak(now))
    }
  }

  /** Completed days in a row under the daily limit. 0 if today's limit is already used up. */
  @Synchronized
  fun streak(now: Long = System.currentTimeMillis()): Int {
    if (dailyUsedSec >= settings.dailyLimitSec) return 0
    val first = db.firstDailyDate() ?: return 0
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val rows = db.getDailyRange(dateKey(now - 400L * DAY_MS), day).associateBy { it.date }
    var count = 0
    while (true) {
      cal.add(Calendar.DAY_OF_YEAR, -1)
      val key = dateKey(cal.timeInMillis)
      if (key < first) break
      val row = rows[key]
      if (row != null && row.usedSec >= row.limitSec) break
      count++
    }
    return count
  }

  /** Monday..Sunday of the current week. */
  @Synchronized
  fun week(now: Long = System.currentTimeMillis()): JSONObject {
    val cal =
        Calendar.getInstance().apply {
          timeInMillis = now
          firstDayOfWeek = Calendar.MONDAY
          set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
          if (timeInMillis > now) add(Calendar.DAY_OF_YEAR, -7)
        }
    val keys =
        List(7) { i ->
          if (i > 0) cal.add(Calendar.DAY_OF_YEAR, 1)
          dateKey(cal.timeInMillis)
        }
    val rows = db.getDailyRange(keys.first(), keys.last()).associateBy { it.date }
    val days = JSONArray()
    keys.forEach { key ->
      val row = if (key == day) DailyRow(day, dailyUsedSec, sessionsToday, blockedToday, settings.dailyLimitSec) else rows[key]
      days.put(
          JSONObject().apply {
            put("date", key)
            put("usedSec", row?.usedSec ?: 0)
            put("limitSec", row?.limitSec ?: settings.dailyLimitSec)
            put("blocked", row?.blocked ?: 0)
            put("sessions", row?.sessions ?: 0)
            put("tracked", row != null)
            put("isToday", key == day)
            put("isFuture", key > day)
          })
    }
    return JSONObject().apply {
      put("days", days)
      put("streak", streak(now))
    }
  }

  @Synchronized
  fun lifetimeStats(now: Long = System.currentTimeMillis()): JSONObject {
    val allDaily = db.getDailyRange("2020-01-01", dateKey(now))
    val totalDays = allDaily.size
    val daysUnderLimit = allDaily.count { it.usedSec < it.limitSec }
    val totalBlocked = allDaily.sumOf { it.blocked }
    val totalSessions = allDaily.sumOf { it.sessions }
    val totalUsedMin = allDaily.sumOf { it.usedSec } / 60

    var bestStreak = 0
    var currentRun = 0
    for (row in allDaily.sortedBy { it.date }) {
      if (row.usedSec < row.limitSec) { currentRun++; bestStreak = maxOf(bestStreak, currentRun) }
      else currentRun = 0
    }

    val currentStreak = streak(now)
    val points = daysUnderLimit * 10 + currentStreak * 5 + totalSessions * 2 + totalBlocked
    val level = when {
      points >= 1000 -> 5
      points >= 500 -> 4
      points >= 200 -> 3
      points >= 50 -> 2
      else -> 1
    }

    val achievements = JSONArray()
    if (totalDays >= 1) achievements.put(achievement("first_day", "First Day", "Started your journey"))
    if (currentStreak >= 3) achievements.put(achievement("streak_3", "3-Day Streak", "Three days strong"))
    if (currentStreak >= 7) achievements.put(achievement("streak_7", "Weekly Warrior", "A full week under limit"))
    if (currentStreak >= 30) achievements.put(achievement("streak_30", "Monthly Master", "30 days under limit"))
    if (bestStreak >= 7) achievements.put(achievement("best_7", "Best Week", "Best streak hit 7 days"))
    if (totalBlocked >= 10) achievements.put(achievement("blocked_10", "Willpower", "Blocked 10 times and stayed strong"))
    if (totalBlocked >= 100) achievements.put(achievement("blocked_100", "Iron Will", "100 blocked attempts resisted"))
    if (daysUnderLimit >= 7) achievements.put(achievement("under_7", "Steady Hand", "7 days under limit total"))
    if (daysUnderLimit >= 30) achievements.put(achievement("under_30", "Habit Builder", "30 days under limit total"))
    if (totalSessions >= 50) achievements.put(achievement("sessions_50", "Veteran", "50 sessions completed"))

    return JSONObject().apply {
      put("totalDays", totalDays)
      put("daysUnderLimit", daysUnderLimit)
      put("totalBlocked", totalBlocked)
      put("totalSessions", totalSessions)
      put("totalUsedMin", totalUsedMin)
      put("bestStreak", bestStreak)
      put("currentStreak", currentStreak)
      put("points", points)
      put("level", level)
      put("achievements", achievements)
    }
  }

  private fun achievement(id: String, title: String, desc: String) =
      JSONObject().apply {
        put("id", id)
        put("title", title)
        put("description", desc)
      }

  fun recentLogs(): JSONArray =
      JSONArray().apply {
        db.recentLogs(100).forEach {
          put(
              JSONObject().apply {
                put("ts", it.ts)
                put("kind", it.kind)
                put("app", it.app)
                put("detail", it.detail)
              })
        }
      }

  companion object {
    private const val PERSIST_EVERY_MS = 10_000L
    private const val MAX_TICK_MS = 1_500L
    private const val LOW_LOG_INTERVAL_MS = 120_000L
    private const val DAY_MS = 86_400_000L
    private const val DEFAULT_NIGHT_START = 22
    private const val DEFAULT_NIGHT_END = 7

    private const val K_SETTINGS = "settings"
    private const val K_DAY = "day"
    private const val K_SESSION_ACTIVE = "session_active"
    private const val K_SESSION_ELAPSED = "session_elapsed"
    private const val K_SESSION_START = "session_start"
    private const val K_SESSION_APP = "session_app"
    private const val K_LAST_IN_FEED = "last_in_feed"
    private const val K_COOLDOWN_UNTIL = "cooldown_until"
    private const val K_FOCUS_UNTIL = "focus_until"
    private const val K_APP_USAGE = "daily_app_usage"

    @Volatile private var instance: GuardEngine? = null

    fun get(context: Context): GuardEngine =
        instance
            ?: synchronized(this) {
              instance ?: GuardEngine(context.applicationContext).also { instance = it }
            }

    fun dateKey(ts: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ts))

    fun isNight(ts: Long, startHour: Int = DEFAULT_NIGHT_START, endHour: Int = DEFAULT_NIGHT_END): Boolean {
      val hour = Calendar.getInstance().apply { timeInMillis = ts }.get(Calendar.HOUR_OF_DAY)
      return if (startHour > endHour) hour >= startHour || hour < endHour
      else hour >= startHour && hour < endHour
    }

    fun nextMidnight(ts: Long): Long =
        Calendar.getInstance()
            .apply {
              timeInMillis = ts
              add(Calendar.DAY_OF_YEAR, 1)
              set(Calendar.HOUR_OF_DAY, 0)
              set(Calendar.MINUTE, 0)
              set(Calendar.SECOND, 0)
              set(Calendar.MILLISECOND, 0)
            }
            .timeInMillis
  }
}
