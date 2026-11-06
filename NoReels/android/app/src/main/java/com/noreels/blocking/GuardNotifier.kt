package com.noreels.blocking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.noreels.MainActivity
import com.noreels.R

/** Persistent session countdown, 1-minute heads-up warning, and uncertain-detection notices. */
class GuardNotifier(private val context: Context) {
  private val nm = context.getSystemService(NotificationManager::class.java)
  private var lastShownRemaining: Int? = null

  init {
    nm.createNotificationChannels(
        listOf(
            NotificationChannel(CH_SESSION, "Session timer", NotificationManager.IMPORTANCE_LOW),
            NotificationChannel(CH_WARNING, "1-minute warning", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(
                CH_DETECTION, "Uncertain detection", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(CH_STATUS, "Blocking turned off", NotificationManager.IMPORTANCE_HIGH),
        ))
  }

  /** Shows "x:xx left" while a session is running; removes it otherwise. */
  fun updateSession(remainingSec: Int?) {
    if (remainingSec == lastShownRemaining) return
    lastShownRemaining = remainingSec
    if (remainingSec == null) {
      nm.cancel(ID_SESSION)
      return
    }
    nm.notify(
        ID_SESSION,
        builder(CH_SESSION)
            .setContentTitle("Short-video session")
            .setContentText("${BlockingOverlay.mmss(remainingSec)} left")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build())
  }

  fun oneMinuteWarning() {
    nm.notify(
        ID_WARNING,
        builder(CH_WARNING)
            .setContentTitle("1 minute left")
            .setContentText("Your short-video session ends in 60 seconds.")
            .setCategory(Notification.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .build())
  }

  fun softWarning(d: Detection) {
    nm.notify(
        ID_DETECTION,
        builder(CH_DETECTION)
            .setContentTitle("Not sure if this is a short-video feed")
            .setContentText("${d.appKey}: ${d.detail}. Not counted, but logged.")
            .setAutoCancel(true)
            .build())
  }

  /** Shown when the blocker stops (service disabled, crashed, or found off after reboot). */
  fun blockingOff() {
    nm.notify(
        ID_STATUS,
        builder(CH_STATUS)
            .setContentTitle("Blocking is OFF")
            .setContentText("Shorts and Reels are not being blocked. Tap to turn it back on.")
            .setCategory(Notification.CATEGORY_ERROR)
            .setAutoCancel(true)
            .build())
  }

  fun cancelBlockingOff() = nm.cancel(ID_STATUS)

  fun cancelAll() {
    lastShownRemaining = null
    nm.cancel(ID_SESSION)
  }

  private fun builder(channel: String): Notification.Builder {
    val open =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE)
    return Notification.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_stat_guard)
        .setContentIntent(open)
  }

  companion object {
    private const val CH_SESSION = "session"
    private const val CH_WARNING = "warning"
    private const val CH_DETECTION = "detection"
    private const val CH_STATUS = "status"
    private const val ID_SESSION = 1
    private const val ID_WARNING = 2
    private const val ID_DETECTION = 3
    private const val ID_STATUS = 4
  }
}
