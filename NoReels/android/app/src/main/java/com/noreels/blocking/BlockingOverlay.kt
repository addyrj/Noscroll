package com.noreels.blocking

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Full-screen block screen. Drawn as TYPE_ACCESSIBILITY_OVERLAY from the accessibility service, so
 * it needs no extra permission and covers every app. It cannot be swiped away: the user leaves via
 * the back key or the Go Back button (5-second hold + reason in Strict Mode).
 */
class BlockingOverlay(
    private val service: AccessibilityService,
    private val engine: GuardEngine,
) {
  private val wm = service.getSystemService(WindowManager::class.java)
  private val audioManager = service.getSystemService(AudioManager::class.java)
  private val handler = Handler(Looper.getMainLooper())
  private val density = service.resources.displayMetrics.density

  private val audioFocusRequest: AudioFocusRequest =
      AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
          .setAudioAttributes(
              AudioAttributes.Builder()
                  .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                  .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                  .build())
          .build()

  private var root: FrameLayout? = null
  private var reason: BlockReason? = null
  private lateinit var emoji: TextView
  private lateinit var title: TextView
  private lateinit var timer: TextView
  private lateinit var quote: TextView
  private lateinit var button: TextView
  private lateinit var reasons: LinearLayout
  private var holdRunnable: Runnable? = null

  val isShowing get() = root != null

  fun show(r: BlockReason) {
    val wasHidden = root == null
    if (root == null) {
      root = build()
      wm.addView(root, layoutParams())
    }
    if (wasHidden) {
      audioManager.requestAudioFocus(audioFocusRequest)
    }
    if (r != reason) {
      reason = r
      resetDismissUi()
    }
    refresh(r)
  }

  fun hide() {
    cancelHold()
    if (root != null) {
      audioManager.abandonAudioFocusRequest(audioFocusRequest)
    }
    root?.let { runCatching { wm.removeViewImmediate(it) } }
    root = null
    reason = null
  }

  private fun refresh(r: BlockReason) {
    val s = engine.settings
    when (r) {
      BlockReason.COOLDOWN -> {
        emoji.text = "🛑"
        title.text = "Time's Up"
        timer.text = "Cooldown: ${mmss(engine.cooldownRemainingSec())}"
      }
      BlockReason.DAILY -> {
        emoji.text = "📅"
        title.text = "Daily allowance used"
        val untilMidnight = (GuardEngine.nextMidnight(System.currentTimeMillis()) - System.currentTimeMillis()) / 1000
        timer.text = "Resets in ${hhmm(untilMidnight.toInt())}"
      }
      BlockReason.NIGHT -> {
        emoji.text = "🌙"
        title.text = "Night schedule"
        timer.text = "Short videos are blocked until ${s.nightEndHour}:00"
      }
      BlockReason.APP_LIMIT -> {
        emoji.text = "⏳"
        title.text = "App limit reached"
        timer.text = "You've used this app's daily limit"
      }
      BlockReason.FOCUS -> {
        emoji.text = "🎯"
        title.text = "Hard Focus"
        timer.text = "Social media blocked · ${mmss(engine.focusRemainingSec())} left"
      }
    }
    quote.visibility = if (s.showQuote) View.VISIBLE else View.GONE
    quote.text = "“${s.quoteText}”"
  }

  private fun resetDismissUi() {
    cancelHold()
    reasons.visibility = View.GONE
    button.visibility = View.VISIBLE
    button.text = if (engine.settings.strictMode) "Hold 5s to go back" else "← Go Back"
  }

  @SuppressLint("ClickableViewAccessibility")
  private fun build(): FrameLayout {
    val frame = FrameLayout(service).apply { setBackgroundColor(BG) }
    val column =
        LinearLayout(service).apply {
          orientation = LinearLayout.VERTICAL
          gravity = Gravity.CENTER
          setPadding(dp(32), dp(48), dp(32), dp(48))
        }
    emoji = text(56f, TEXT)
    title = text(28f, TEXT, bold = true)
    timer = text(18f, ACCENT, bold = true)
    quote = text(17f, TEXT_SECONDARY).apply { setTypeface(typeface, Typeface.ITALIC) }
    button =
        text(16f, TEXT, bold = true).apply {
          background = pill(ACCENT)
          setPadding(dp(28), dp(14), dp(28), dp(14))
        }
    reasons =
        LinearLayout(service).apply {
          orientation = LinearLayout.VERTICAL
          gravity = Gravity.CENTER
          visibility = View.GONE
          addView(text(15f, TEXT_SECONDARY).apply { text = "Why are you leaving this screen?" })
          REASONS.forEach { r ->
            addView(
                text(15f, TEXT).apply {
                  text = r
                  background = pill(SURFACE)
                  setPadding(dp(20), dp(12), dp(20), dp(12))
                  setOnClickListener {
                    reason?.let { engine.logDismissal(r, it) }
                    leave()
                  }
                },
                spaced())
          }
        }

    button.setOnClickListener { if (!engine.settings.strictMode) leave() }
    button.setOnTouchListener { _, ev ->
      if (!engine.settings.strictMode) return@setOnTouchListener false
      when (ev.action) {
        MotionEvent.ACTION_DOWN -> startHold()
        MotionEvent.ACTION_UP,
        MotionEvent.ACTION_CANCEL -> if (reasons.visibility != View.VISIBLE) resetDismissUi()
      }
      true
    }

    column.addView(emoji, spaced())
    column.addView(title, spaced())
    column.addView(timer, spaced())
    column.addView(quote, spaced(top = 32))
    column.addView(button, spaced(top = 40))
    column.addView(reasons, spaced(top = 24))
    frame.addView(
        column,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
    return frame
  }

  private fun startHold() {
    cancelHold()
    var left = HOLD_SECONDS
    val r =
        object : Runnable {
          override fun run() {
            if (left <= 0) {
              button.visibility = View.GONE
              reasons.visibility = View.VISIBLE
              holdRunnable = null
              return
            }
            button.text = "Keep holding… $left"
            left--
            handler.postDelayed(this, 1000)
          }
        }
    holdRunnable = r
    handler.post(r)
  }

  private fun cancelHold() {
    holdRunnable?.let { handler.removeCallbacks(it) }
    holdRunnable = null
  }

  /** Back first (leaves the Shorts/Reels feed), then Home if still blocked. */
  private fun leave() {
    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    handler.postDelayed(
        { if (isShowing) service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME) },
        900)
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
    private const val HOLD_SECONDS = 5
    private val REASONS = listOf("I'm bored", "Pure habit", "Avoiding something", "I really need it")

    // Keep in sync with src/theme/colors.ts (dark palette).
    private val BG = Color.parseColor("#0A0A0A")
    private val SURFACE = Color.parseColor("#1E1E1E")
    private val ACCENT = Color.parseColor("#FF3B5C")
    private val TEXT = Color.parseColor("#FFFFFF")
    private val TEXT_SECONDARY = Color.parseColor("#888888")

    fun mmss(sec: Int) = "%d:%02d".format(sec / 60, sec % 60)

    fun hhmm(sec: Int) = "%dh %02dm".format(sec / 3600, (sec % 3600) / 60)
  }
}
