package com.noreels.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.noreels.blocking.GuardEngine
import com.noreels.blocking.GuardNotifier
import com.noreels.blocking.ShortBlockerService

/**
 * After reboot or app update: load the persisted ledger, roll the day if needed, re-arm the midnight
 * alarm, and warn if blocking is off. The accessibility service itself is restarted by the system.
 */
class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      Intent.ACTION_BOOT_COMPLETED,
      Intent.ACTION_MY_PACKAGE_REPLACED -> {
        GuardEngine.get(context).rollDayIfNeeded()
        MidnightResetReceiver.schedule(context)
        // A force-stop switches Accessibility off; tell the user instead of failing silently.
        if (!ShortBlockerService.isEnabledInSettings(context)) GuardNotifier(context).blockingOff()
      }
    }
  }
}
