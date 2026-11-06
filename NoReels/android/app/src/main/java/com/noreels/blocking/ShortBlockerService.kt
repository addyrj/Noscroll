package com.noreels.blocking

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.noreels.receivers.MidnightResetReceiver

class ShortBlockerService : AccessibilityService() {
  private val handler = Handler(Looper.getMainLooper())
  private lateinit var engine: GuardEngine
  private lateinit var overlay: BlockingOverlay
  private lateinit var pauseOverlay: PauseOverlay
  private lateinit var notifier: GuardNotifier
  private lateinit var power: PowerManager
  private var lastStepRealtime = 0L
  private var lastLoggedState = ""

  private var lastForegroundPkg: String? = null
  private var pauseDismissedFor: String? = null
  private var pauseDismissedAt = 0L
  private var appEnteredAt = 0L
  private var reinterventionShownFor: String? = null

  private val loop =
      object : Runnable {
        override fun run() {
          step()
          handler.postDelayed(this, STEP_MS)
        }
      }

  override fun onServiceConnected() {
    engine = GuardEngine.get(this)
    overlay = BlockingOverlay(this, engine)
    pauseOverlay = PauseOverlay(this,
        onContinue = {
          pauseDismissedFor = lastForegroundPkg
          pauseDismissedAt = SystemClock.elapsedRealtime()
          appEnteredAt = SystemClock.elapsedRealtime()
          reinterventionShownFor = null
        },
        onGoBack = {
          performGlobalAction(GLOBAL_ACTION_HOME)
        })
    notifier = GuardNotifier(this)
    power = getSystemService(PowerManager::class.java)
    MidnightResetReceiver.schedule(this)
    lastStepRealtime = SystemClock.elapsedRealtime()
    isRunning = true
    notifier.cancelBlockingOff()
    handler.post(loop)
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (isRunning && event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
      handler.removeCallbacks(loop)
      handler.post(loop)
    }
  }

  override fun onInterrupt() = Unit

  override fun onDestroy() {
    shutdown()
    super.onDestroy()
  }

  private fun shutdown() {
    if (!isRunning) return
    isRunning = false
    handler.removeCallbacksAndMessages(null)
    overlay.hide()
    pauseOverlay.hide()
    notifier.cancelAll()
    notifier.blockingOff()
    engine.flush()
  }

  private fun step() {
    val nowRt = SystemClock.elapsedRealtime()
    val delta = nowRt - lastStepRealtime
    lastStepRealtime = nowRt

    var feed: Detection? = null
    var focusAppOpen = false
    if (power.isInteractive) {
      try {
        val root = foregroundRoot()
        val pkg = root?.packageName?.toString()
        if (root != null && pkg != null && pkg != packageName) {
          val settings = engine.settings

          // Track app switches for pause/re-intervention
          if (pkg != lastForegroundPkg) {
            if (lastForegroundPkg != null) {
              reinterventionShownFor = null
            }
            lastForegroundPkg = pkg
          }

          // Pause-before-opening check
          if (settings.pauseEnabled && !pauseOverlay.isShowing && !overlay.isShowing) {
            val isTarget = DetectionRules.isTargetApp(pkg, settings) ||
                DetectionRules.isFocusTarget(pkg, settings)
            if (isTarget && needsPause(pkg, nowRt)) {
              val label = appLabel(pkg)
              pauseOverlay.show(settings.pauseDelaySec, label)
              return
            }
          }

          if (pauseOverlay.isShowing) return

          // Re-intervention check
          if (settings.reinterventionEnabled && !overlay.isShowing) {
            val isTarget = DetectionRules.isTargetApp(pkg, settings) ||
                DetectionRules.isFocusTarget(pkg, settings)
            if (isTarget && pkg == pauseDismissedFor && reinterventionShownFor != pkg) {
              val inAppMs = nowRt - appEnteredAt
              if (inAppMs >= settings.reinterventionMin * 60_000L) {
                reinterventionShownFor = pkg
                val label = appLabel(pkg)
                pauseOverlay.show(settings.pauseDelaySec, label)
                return
              }
            }
          }

          focusAppOpen = DetectionRules.isFocusTarget(pkg, settings)
          if (DetectionRules.isTargetApp(pkg, settings)) {
            val d = DetectionRules.detect(root, pkg, settings)
            when (d?.confidence) {
              Confidence.HIGH -> feed = d
              Confidence.LOW -> if (engine.logUncertain(d)) notifier.softWarning(d)
              null -> Unit
            }
          }
        }
      } catch (e: Exception) {
        Log.w(TAG, "Detection failed", e)
      }
    }

    val result = engine.tick(feed, focusAppOpen, delta, System.currentTimeMillis())
    val state = "focusApp=$focusAppOpen feed=${feed?.detail} block=${result.block}"
    if (state != lastLoggedState) {
      Log.d(TAG, state)
      lastLoggedState = state
    }
    if (result.block != null) overlay.show(result.block) else overlay.hide()
    if (result.warn) notifier.oneMinuteWarning()
    notifier.updateSession(result.remainingSec)
  }

  private fun needsPause(pkg: String, nowRt: Long): Boolean {
    if (pkg == pauseDismissedFor && nowRt - pauseDismissedAt < PAUSE_COOLDOWN_MS) return false
    return true
  }

  private fun appLabel(pkg: String): String {
    return try {
      val pm = packageManager
      val ai = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
      } else {
        @Suppress("DEPRECATION") pm.getApplicationInfo(pkg, 0)
      }
      pm.getApplicationLabel(ai).toString()
    } catch (_: Exception) {
      pkg
    }
  }

  private fun foregroundRoot(): AccessibilityNodeInfo? {
    val appWindows = windows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
    val window = appWindows.firstOrNull { it.isActive } ?: appWindows.maxByOrNull { it.layer }
    return window?.root ?: rootInActiveWindow
  }

  companion object {
    private const val TAG = "ShortBlockerService"
    private const val STEP_MS = 500L
    private const val PAUSE_COOLDOWN_MS = 5 * 60_000L

    @Volatile
    var isRunning = false
      private set

    fun isEnabledInSettings(context: Context): Boolean {
      val enabled =
          Settings.Secure.getString(
              context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
      val me = ComponentName(context, ShortBlockerService::class.java)
      return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }
  }
}
