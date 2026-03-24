package com.artsunique.figbridge.config

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * Manages the 14-day trial period.
 * First-use date is stored obfuscated in FigBridgeSettings.
 * After trial expires, code generation and asset export are locked.
 * Preview/inspect remains free.
 */
object TrialManager {

    private const val TRIAL_DAYS = 14L
    private const val OBFUSCATION_KEY = 0x5F3A.toLong()

    /** Activate trial on first use (no-op if already activated) */
    fun activateIfNeeded() {
        val settings = FigBridgeSettings.getInstance()
        if (settings.state.trialStart.isBlank()) {
            val encoded = encode(System.currentTimeMillis())
            settings.state.trialStart = encoded
        }
    }

    /** Days remaining in the trial, or 0 if expired */
    fun daysRemaining(): Int {
        val startMillis = getTrialStartMillis() ?: return TRIAL_DAYS.toInt()
        val elapsed = System.currentTimeMillis() - startMillis
        val elapsedDays = TimeUnit.MILLISECONDS.toDays(elapsed)
        return (TRIAL_DAYS - elapsedDays).coerceAtLeast(0).toInt()
    }

    /** Whether the trial has expired */
    fun isExpired(): Boolean = daysRemaining() <= 0

    /** Whether the trial is active (not expired) */
    fun isActive(): Boolean = !isExpired()

    /** Whether this is the first run (trial not yet started) */
    fun isFirstRun(): Boolean = FigBridgeSettings.getInstance().state.trialStart.isBlank()

    /** Show trial reminder notification if appropriate (day 7, 12, 14) */
    fun checkAndNotify(project: Project) {
        val remaining = daysRemaining()
        val settings = FigBridgeSettings.getInstance()
        val lastNotified = settings.state.lastTrialNotifyDay

        val shouldNotify = when (remaining) {
            7 -> lastNotified != 7
            2 -> lastNotified != 2
            0 -> lastNotified != 0
            else -> false
        }

        if (shouldNotify) {
            settings.state.lastTrialNotifyDay = remaining
            val message = when (remaining) {
                7 -> "Your FigBridge trial has 7 days remaining."
                2 -> "Your FigBridge trial expires in 2 days."
                0 -> "Your FigBridge trial has expired. Upgrade to continue generating code."
                else -> return
            }
            showNotification(project, message, if (remaining == 0) NotificationType.WARNING else NotificationType.INFORMATION)
        }
    }

    private fun showNotification(project: Project, message: String, type: NotificationType) {
        try {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("FigBridge")
                .createNotification("FigBridge", message, type)
                .notify(project)
        } catch (_: Exception) {
            // Notification group may not be registered yet during tests
        }
    }

    private fun getTrialStartMillis(): Long? {
        val encoded = FigBridgeSettings.getInstance().state.trialStart
        if (encoded.isBlank()) return null
        return decode(encoded)
    }

    private fun encode(millis: Long): String {
        val obfuscated = millis xor OBFUSCATION_KEY
        return Base64.getEncoder().encodeToString(obfuscated.toString().toByteArray())
    }

    private fun decode(encoded: String): Long? {
        return try {
            val decoded = String(Base64.getDecoder().decode(encoded))
            decoded.toLong() xor OBFUSCATION_KEY
        } catch (_: Exception) {
            null
        }
    }
}
