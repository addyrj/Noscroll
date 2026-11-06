package com.noreels.blocking

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class PauseOverlay(
    private val service: AccessibilityService,
    private val onContinue: () -> Unit,
    private val onGoBack: () -> Unit,
) {
  private val wm = service.getSystemService(WindowManager::class.java)
  private val handler = Handler(Looper.getMainLooper())
  private val density = service.resources.displayMetrics.density

  private var root: FrameLayout? = null
  private lateinit var countdownText: TextView
  private lateinit var continueBtn: TextView
  private var tickRunnable: Runnable? = null
  private var secondsLeft = 0

  val isShowing get() = root != null

  fun show(delaySec: Int, appLabel: String) {
    if (root != null) return
    secondsLeft = delaySec
    root = build(appLabel)
    wm.addView(root, layoutParams())
    startCountdown()
  }

  fun hide() {
    stopCountdown()
    root?.let { runCatching { wm.removeViewImmediate(it) } }
    root = null
  }

  private fun startCountdown() {
    stopCountdown()
    updateCountdown()
    val r = object : Runnable {
      override fun run() {
        secondsLeft--
        updateCountdown()
        if (secondsLeft > 0) handler.postDelayed(this, 1000)
      }
    }
    tickRunnable = r
    handler.postDelayed(r, 1000)
  }

  private fun stopCountdown() {
    tickRunnable?.let { handler.removeCallbacks(it) }
    tickRunnable = null
  }

  private fun updateCountdown() {
    if (secondsLeft > 0) {
      countdownText.text = "Take a moment... $secondsLeft"
      countdownText.visibility = View.VISIBLE
      continueBtn.visibility = View.GONE
    } else {
      countdownText.visibility = View.GONE
      continueBtn.visibility = View.VISIBLE
    }
  }

  private fun build(appLabel: String): FrameLayout {
    val frame = FrameLayout(service).apply { setBackgroundColor(BG) }
    val column = LinearLayout(service).apply {
      orientation = LinearLayout.VERTICAL
      gravity = Gravity.CENTER
      setPadding(dp(32), dp(48), dp(32), dp(48))
    }

    val emoji = text(56f, TEXT).apply { text = "🧘" } // 🧘
    val title = text(24f, TEXT, bold = true).apply { text = "Wait..." }
    val subtitle = text(16f, TEXT_SECONDARY).apply {
      text = "You're about to open $appLabel.\nDo you really need it right now?"
    }
    countdownText = text(20f, ACCENT, bold = true)
    continueBtn = text(16f, TEXT, bold = true).apply {
      background = pill(ACCENT)
      setPadding(dp(28), dp(14), dp(28), dp(14))
      text = "Yes, continue"
      setOnClickListener {
        hide()
        onContinue()
      }
    }
    val backBtn = text(16f, TEXT_SECONDARY).apply {
      background = pill(SURFACE)
      setPadding(dp(28), dp(14), dp(28), dp(14))
      text = "No, go back"
      setOnClickListener {
        hide()
        onGoBack()
      }
    }

    column.addView(emoji, spaced(24))
    column.addView(title, spaced())
    column.addView(subtitle, spaced())
    column.addView(countdownText, spaced(32))
    column.addView(continueBtn, spaced(32))
    column.addView(backBtn, spaced())
    frame.addView(column,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
    return frame
  }

  private fun layoutParams() =
      WindowManager.LayoutParams(
          WindowManager.LayoutParams.MATCH_PARENT,
          WindowManager.LayoutParams.MATCH_PARENT,
          WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
          WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
              WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
          PixelFormat.OPAQUE)
          .apply { gravity = Gravity.CENTER }

  private fun text(sizeSp: Float, color: Int, bold: Boolean = false) =
      TextView(service).apply {
        textSize = sizeSp
        setTextColor(color)
        gravity = Gravity.CENTER
        if (bold) setTypeface(typeface, Typeface.BOLD)
      }

  private fun pill(color: Int) =
      GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(28).toFloat()
      }

  private fun spaced(top: Int = 12) =
      LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
          .apply {
            topMargin = dp(top)
            gravity = Gravity.CENTER_HORIZONTAL
          }

  private fun dp(v: Int) = (v * density).toInt()

  companion object {
    private val BG = Color.parseColor("#0A0A0A")
    private val SURFACE = Color.parseColor("#1E1E1E")
    private val ACCENT = Color.parseColor("#FF3B5C")
    private val TEXT = Color.parseColor("#FFFFFF")
    private val TEXT_SECONDARY = Color.parseColor("#888888")
  }
}
