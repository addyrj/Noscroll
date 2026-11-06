package com.noreels.bridge

import android.app.AppOpsManager
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.noreels.blocking.DetectionRules
import com.noreels.blocking.GuardEngine
import com.noreels.blocking.ShortBlockerService
import com.noreels.ledger.GuardSettings
import java.util.Calendar
import org.json.JSONArray
import org.json.JSONObject

/** Read-mostly bridge to React Native. The engine writes the ledger; the UI only reads it. */
class NoReelsModule(private val ctx: ReactApplicationContext) : NativeNoReelsSpec(ctx) {
  private val engine get() = GuardEngine.get(ctx)

  override fun getName() = NAME

  override fun getStatus(promise: Promise) = resolve(promise) {
    engine.status(ShortBlockerService.isRunning).toString()
  }

  override fun getSettings(promise: Promise) = resolve(promise) {
    engine.settings.toJson().toString()
  }

  override fun saveSettings(settingsJson: String, promise: Promise) = resolve(promise) {
    engine.updateSettings(GuardSettings.fromJson(settingsJson)).toJson().toString()
  }

  override fun getWeek(promise: Promise) = resolve(promise) { engine.week().toString() }

  override fun getPermissions(promise: Promise) = resolve(promise) {
    val pm = ctx.getSystemService(PowerManager::class.java)
    JSONObject()
        .apply {
          put("accessibility", ShortBlockerService.isEnabledInSettings(ctx))
          put("serviceRunning", ShortBlockerService.isRunning)
          put("notifications", ctx.getSystemService(NotificationManager::class.java).areNotificationsEnabled())
          put("battery", pm.isIgnoringBatteryOptimizations(ctx.packageName))
          put("usage", hasUsageAccess())
        }
        .toString()
  }

  override fun openPermissionSettings(kind: String) {
    val pkgUri = Uri.parse("package:${ctx.packageName}")
    val intent =
        when (kind) {
          "accessibility" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
          "notifications" ->
              Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                  .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
          "battery" -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
          "usage" -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
          "appInfo" -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri)
          else -> return
        }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
      ctx.startActivity(intent)
    } catch (e: Exception) {
      ctx.startActivity(
          Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri)
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
  }

  override fun startFocus(minutes: Double, promise: Promise) = resolve(promise) {
    engine.startFocus(minutes.toInt())
    engine.focusRemainingSec().toDouble()
  }

  override fun getEventLog(promise: Promise) = resolve(promise) { engine.recentLogs().toString() }

  override fun getLifetimeStats(promise: Promise) = resolve(promise) { engine.lifetimeStats().toString() }

  override fun getInstalledApps(promise: Promise) = resolve(promise) {
    val pm = ctx.packageManager
    val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
    } else {
      @Suppress("DEPRECATION") pm.getInstalledApplications(0)
    }
    val own = ctx.packageName
    val knownSocial = DetectionRules.allKnownPackages()
    val out = JSONArray()
    for (app in apps) {
      if (app.packageName == own) continue
      if (app.flags and ApplicationInfo.FLAG_SYSTEM != 0 &&
          app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0) continue
      val label = pm.getApplicationLabel(app).toString()
      out.put(JSONObject().apply {
        put("pkg", app.packageName)
        put("label", label)
        put("social", app.packageName in knownSocial)
      })
    }
    out.toString()
  }

  /** System-reported foreground minutes today per target package (needs Usage Access). */
  override fun getAppUsageToday(promise: Promise) = resolve(promise) {
    val out = JSONObject()
    if (hasUsageAccess()) {
      val start =
          Calendar.getInstance()
              .apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
              }
              .timeInMillis
      val usm = ctx.getSystemService(UsageStatsManager::class.java)
      usm.queryAndAggregateUsageStats(start, System.currentTimeMillis()).forEach { (pkg, stats) ->
        if (stats.totalTimeInForeground > 0) out.put(pkg, stats.totalTimeInForeground / 1000)
      }
    }
    out.toString()
  }

  private fun hasUsageAccess(): Boolean {
    val ops = ctx.getSystemService(AppOpsManager::class.java)
    val mode =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        } else {
          @Suppress("DEPRECATION")
          ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        }
    return mode == AppOpsManager.MODE_ALLOWED
  }

  private inline fun resolve(promise: Promise, block: () -> Any) {
    try {
      promise.resolve(block())
    } catch (e: Exception) {
      promise.reject("E_SHORTS_GUARD", e.message, e)
    }
  }

  companion object {
    const val NAME = "NoReels"
  }
}
