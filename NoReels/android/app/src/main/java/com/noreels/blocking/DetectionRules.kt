package com.noreels.blocking

import android.view.accessibility.AccessibilityNodeInfo
import com.noreels.ledger.GuardSettings

enum class Confidence {
  HIGH,
  LOW,
}

data class Detection(
    val appKey: String,
    val packageName: String,
    val confidence: Confidence,
    val detail: String,
)

/**
 * Per-app feed detection. Android has no "user is watching a Short" API, so these rules match
 * known view ids and tab labels. They WILL need updating when the apps change their UI. Labels are
 * English-only.
 *
 * HIGH = counted and enforced. LOW = logged + soft warning, never counted (no over-counting).
 */
object DetectionRules {
  const val YOUTUBE = "youtube"
  const val INSTAGRAM = "instagram"
  const val FACEBOOK = "facebook"
  const val TIKTOK = "tiktok"
  const val SNAPCHAT = "snapchat"

  private const val YOUTUBE_PKG = "com.google.android.youtube"
  private const val INSTAGRAM_PKG = "com.instagram.android"
  private const val FACEBOOK_PKG = "com.facebook.katana"
  private const val SNAPCHAT_PKG = "com.snapchat.android"
  private val TIKTOK_PKGS =
      setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.zhiliaoapp.musically.go")

  private class FeedRule(val key: String, val viewIds: List<String>, val label: String)

  private val FEED_RULES =
      mapOf(
          YOUTUBE_PKG to
              FeedRule(
                  YOUTUBE,
                  listOf("reel_recycler", "reel_player_page_container", "reel_watch_player"),
                  "Shorts"),
          INSTAGRAM_PKG to
              FeedRule(
                  INSTAGRAM,
                  listOf("clips_viewer_view_pager", "clips_viewer_container", "clips_video_container",
                         "reels_viewer_view_pager", "reels_player_container", "reel_viewer_fragment_container"),
                  "Reels"),
          FACEBOOK_PKG to FeedRule(FACEBOOK, emptyList(), "Reels"),
          SNAPCHAT_PKG to FeedRule(SNAPCHAT, listOf("spotlight_container"), "Spotlight"),
      )

  fun allKnownPackages(): Set<String> = FEED_RULES.keys + TIKTOK_PKGS

  private const val MAX_NODES = 1500

  /** True when [pkg] is a target app that is enabled in settings (whole app, feed or not). */
  fun isTargetApp(pkg: String, settings: GuardSettings): Boolean =
      appKeyFor(pkg, settings) != null

  /** Hard Focus blocks every known social app plus the custom list, even if toggled off. */
  fun isFocusTarget(pkg: String, settings: GuardSettings): Boolean =
      pkg in FEED_RULES || pkg in TIKTOK_PKGS || pkg in settings.customPackages

  private fun appKeyFor(pkg: String, settings: GuardSettings): String? {
    if (pkg in settings.customPackages) return "custom:$pkg"
    val key = FEED_RULES[pkg]?.key ?: if (pkg in TIKTOK_PKGS) TIKTOK else return null
    return if (settings.isAppEnabled(key)) key else null
  }

  fun detect(root: AccessibilityNodeInfo, pkg: String, settings: GuardSettings): Detection? {
    val key = appKeyFor(pkg, settings) ?: return null
    if (key.startsWith("custom:")) return Detection(key, pkg, Confidence.HIGH, "custom block list")
    if (key == TIKTOK) return Detection(key, pkg, Confidence.HIGH, "whole app")
    val rule = FEED_RULES.getValue(pkg)

    for (id in rule.viewIds) {
      val hits = root.findAccessibilityNodeInfosByViewId("$pkg:id/$id")
      if (hits.any { it.isVisibleToUser }) {
        return Detection(key, pkg, Confidence.HIGH, "view id $id")
      }
    }

    var labelSelected = false
    var labelVisible = false
    var homeVisible = false
    walk(root) { node ->
      if (!node.isVisibleToUser) return@walk
      val text = node.text?.toString()?.trim()
      val desc = node.contentDescription?.toString()?.trim()
      // Facebook tab descriptions look like "Reels, tab 3 of 6".
      val isLabel =
          text.equals(rule.label, ignoreCase = true) ||
              desc.equals(rule.label, ignoreCase = true) ||
              desc?.startsWith("${rule.label},", ignoreCase = true) == true
      if (isLabel) {
        labelVisible = true
        if (node.isSelected) labelSelected = true
      }
      if (text.equals("Home", true) || desc?.startsWith("Home", true) == true) homeVisible = true
    }

    return when {
      labelSelected -> Detection(key, pkg, Confidence.HIGH, "selected '${rule.label}' tab")
      // Label on screen but the normal tab bar is gone: maybe a fullscreen feed, maybe not.
      labelVisible && !homeVisible ->
          Detection(key, pkg, Confidence.LOW, "'${rule.label}' visible without tab bar")
      else -> null
    }
  }

  private inline fun walk(root: AccessibilityNodeInfo, visit: (AccessibilityNodeInfo) -> Unit) {
    val stack = ArrayDeque<AccessibilityNodeInfo>()
    stack.addLast(root)
    var seen = 0
    while (stack.isNotEmpty() && seen < MAX_NODES) {
      val node = stack.removeLast()
      seen++
      visit(node)
      for (i in 0 until node.childCount) {
        node.getChild(i)?.let { stack.addLast(it) }
      }
    }
  }
}
