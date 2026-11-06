package com.noreels.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.noreels.blocking.GuardEngine

/**
 * Rolls the daily total over at local midnight. The engine also rolls lazily on every tick, so an
 * inexact alarm (no SCHEDULE_EXACT_ALARM permission needed) is enough.
 */
class MidnightResetReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    GuardEngine.get(context).rollDayIfNeeded()
    schedule(context)
  }

  companion object {
    fun schedule(context: Context) {
      val am = context.getSystemService(AlarmManager::class.java)
      val pi =
          PendingIntent.getBroadcast(
              context,
              0,
              Intent(context, MidnightResetReceiver::class.java),
              PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
      val at = GuardEngine.nextMidnight(System.currentTimeMillis()) + 1_000
      am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }
  }
}
