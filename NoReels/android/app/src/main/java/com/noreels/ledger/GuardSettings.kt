package com.noreels.ledger

import org.json.JSONArray
import org.json.JSONObject

/** Enforcement settings. Kotlin owns these so the UI can never bypass the bounds. */
data class GuardSettings(
    val sessionLimitMin: Int = 5,
    val dailyLimitMin: Int = 30,
    val cooldownMin: Int = 15,
    val strictMode: Boolean = false,
    val nightSchedule: Boolean = false,
    val showQuote: Boolean = true,
    val quoteText: String = DEFAULT_QUOTE,
    val apps: Map<String, Boolean> = DEFAULT_APPS,
    val customPackages: List<String> = emptyList(),
    val pauseEnabled: Boolean = false,
    val pauseDelaySec: Int = 5,
    val curiousMode: Boolean = false,
    val curiousTimeSec: Int = 30,
    val reinterventionEnabled: Boolean = false,
    val reinterventionMin: Int = 10,
    val nightStartHour: Int = 22,
    val nightEndHour: Int = 7,
    val appTimeLimits: Map<String, Int> = emptyMap(),
) {
  val sessionLimitSec get() = sessionLimitMin * 60
  val dailyLimitSec get() = dailyLimitMin * 60
  val cooldownMs get() = cooldownMin * 60_000L

  fun appTimeLimitSec(appKey: String): Int? {
    val min = appTimeLimits[appKey] ?: return null
    return if (min > 0) min * 60 else null
  }

  fun isAppEnabled(key: String) = apps[key] ?: false

  fun clamped() =
      copy(
          sessionLimitMin = sessionLimitMin.coerceIn(1, 5),
          dailyLimitMin = dailyLimitMin.coerceIn(5, 30),
          cooldownMin = cooldownMin.coerceIn(5, 30),
          quoteText = quoteText.trim().take(140).ifEmpty { DEFAULT_QUOTE },
          apps = DEFAULT_APPS.keys.associateWith { apps[it] ?: false },
          customPackages =
              customPackages.map { it.trim() }.filter { PACKAGE_RE.matches(it) }.distinct().take(100),
          pauseDelaySec = pauseDelaySec.coerceIn(3, 30),
          curiousTimeSec = curiousTimeSec.coerceIn(10, 60),
          reinterventionMin = reinterventionMin.coerceIn(5, 60),
          nightStartHour = nightStartHour.coerceIn(0, 23),
          nightEndHour = nightEndHour.coerceIn(0, 23),
      )

  fun toJson(): JSONObject =
      JSONObject().apply {
        put("sessionLimitMin", sessionLimitMin)
        put("dailyLimitMin", dailyLimitMin)
        put("cooldownMin", cooldownMin)
        put("strictMode", strictMode)
        put("nightSchedule", nightSchedule)
        put("showQuote", showQuote)
        put("quoteText", quoteText)
        put("apps", JSONObject().apply { apps.forEach { (k, v) -> put(k, v) } })
        put("customPackages", JSONArray(customPackages))
        put("pauseEnabled", pauseEnabled)
        put("pauseDelaySec", pauseDelaySec)
        put("curiousMode", curiousMode)
        put("curiousTimeSec", curiousTimeSec)
        put("reinterventionEnabled", reinterventionEnabled)
        put("reinterventionMin", reinterventionMin)
        put("nightStartHour", nightStartHour)
        put("nightEndHour", nightEndHour)
        put("appTimeLimits", JSONObject().apply { appTimeLimits.forEach { (k, v) -> put(k, v) } })
      }

  companion object {
    const val DEFAULT_QUOTE = "Build the life you want to live"
    val DEFAULT_APPS =
        linkedMapOf(
            "youtube" to true,
            "instagram" to true,
            "facebook" to true,
            "tiktok" to true,
            "snapchat" to true,
        )
    private val PACKAGE_RE = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

    fun fromJson(raw: String?): GuardSettings {
      if (raw.isNullOrBlank()) return GuardSettings()
      return try {
        val o = JSONObject(raw)
        val d = GuardSettings()
        val appsJson = o.optJSONObject("apps")
        val pkgs = o.optJSONArray("customPackages")
        GuardSettings(
                sessionLimitMin = o.optInt("sessionLimitMin", d.sessionLimitMin),
                dailyLimitMin = o.optInt("dailyLimitMin", d.dailyLimitMin),
                cooldownMin = o.optInt("cooldownMin", d.cooldownMin),
                strictMode = o.optBoolean("strictMode", d.strictMode),
                nightSchedule = o.optBoolean("nightSchedule", d.nightSchedule),
                showQuote = o.optBoolean("showQuote", d.showQuote),
                quoteText = o.optString("quoteText", d.quoteText),
                apps =
                    DEFAULT_APPS.mapValues { (k, v) -> appsJson?.optBoolean(k, v) ?: v },
                customPackages =
                    if (pkgs == null) emptyList()
                    else List(pkgs.length()) { pkgs.optString(it) },
                pauseEnabled = o.optBoolean("pauseEnabled", d.pauseEnabled),
                pauseDelaySec = o.optInt("pauseDelaySec", d.pauseDelaySec),
                curiousMode = o.optBoolean("curiousMode", d.curiousMode),
                curiousTimeSec = o.optInt("curiousTimeSec", d.curiousTimeSec),
                reinterventionEnabled = o.optBoolean("reinterventionEnabled", d.reinterventionEnabled),
                reinterventionMin = o.optInt("reinterventionMin", d.reinterventionMin),
                nightStartHour = o.optInt("nightStartHour", d.nightStartHour),
                nightEndHour = o.optInt("nightEndHour", d.nightEndHour),
                appTimeLimits = o.optJSONObject("appTimeLimits")?.let { atl ->
                  val m = mutableMapOf<String, Int>()
                  atl.keys().forEach { k -> m[k] = atl.optInt(k) }
                  m
                } ?: emptyMap(),
            )
            .clamped()
      } catch (e: Exception) {
        GuardSettings()
      }
    }
  }
}
