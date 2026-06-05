package com.krata.orbit.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.krata.orbit.MainActivity
import com.krata.orbit.R
import java.util.Calendar

// ── Channel constants ─────────────────────────────────────────────────────────
object NotificationChannels {
    const val TASKS    = "orbit_tasks"
    const val EVENTS   = "orbit_events"
    const val POTD     = "orbit_potd"
    const val CONTESTS = "orbit_contests"
}

// ── Notification IDs ──────────────────────────────────────────────────────────
object NotificationIds {
    const val TASKS    = 1001
    const val POTD     = 1002
    const val EVENTS   = 1003
    const val CONTESTS = 1004
}

// ── Intent extras ─────────────────────────────────────────────────────────────
const val EXTRA_NOTIF_TYPE = "notif_type"
const val EXTRA_TAB_ROUTE  = "tab_route"

// ── Receiver ─────────────────────────────────────────────────────────────────
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // Re-schedule alarms after reboot
                NotificationScheduler.rescheduleAll(context)
            }
            "com.krata.orbit.NOTIFICATION_ACTION" -> {
                val type = intent.getStringExtra(EXTRA_NOTIF_TYPE) ?: return
                NotificationHelper.showNotification(context, type)
            }
        }
    }
}

// ── Helper ────────────────────────────────────────────────────────────────────
object NotificationHelper {

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        listOf(
            NotificationChannel(NotificationChannels.TASKS,    "Tasks",    NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Daily task reminders" },
            NotificationChannel(NotificationChannels.EVENTS,   "Events",   NotificationManager.IMPORTANCE_HIGH).apply { description = "Upcoming event alerts" },
            NotificationChannel(NotificationChannels.POTD,     "POTD",     NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Problem of the day" },
            NotificationChannel(NotificationChannels.CONTESTS, "Contests", NotificationManager.IMPORTANCE_HIGH).apply { description = "Upcoming contest alerts" }
        ).forEach { nm.createNotificationChannel(it) }
    }

    fun showNotification(context: Context, type: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val tabRoute = when (type) {
            "tasks"    -> "home"
            "potd"     -> "coding"
            "events"   -> "events"
            "contests" -> "coding"
            else       -> "home"
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TAB_ROUTE, tabRoute)
        }
        val tapPending = PendingIntent.getActivity(
            context, type.hashCode(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (channel, notifId, title, body) = when (type) {
            "tasks"    -> Quad(NotificationChannels.TASKS,    NotificationIds.TASKS,    "Tasks Reminder", "Check your tasks for today 📋")
            "potd"     -> Quad(NotificationChannels.POTD,     NotificationIds.POTD,     "Problem of the Day", "Your daily coding challenge is ready 💻")
            "events"   -> Quad(NotificationChannels.EVENTS,   NotificationIds.EVENTS,   "Upcoming Event", "You have an event coming up soon 📅")
            "contests" -> Quad(NotificationChannels.CONTESTS, NotificationIds.CONTESTS, "Contest Starting Soon", "A coding contest is about to begin 🏆")
            else       -> Quad(NotificationChannels.TASKS,    NotificationIds.TASKS,    "Orbit", "Check your Orbit dashboard")
        }

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .build()

        nm.notify(notifId, notification)
    }
}

private data class Quad(val channel: String, val id: Int, val title: String, val body: String)

// ── Scheduler ─────────────────────────────────────────────────────────────────
object NotificationScheduler {

    fun scheduleTasksNotification(context: Context, hour: Int, minute: Int) {
        scheduleDaily(context, "tasks", hour, minute, 2001)
    }

    fun schedulePotdNotification(context: Context, hour: Int, minute: Int) {
        scheduleDaily(context, "potd", hour, minute, 2002)
    }

    fun cancelTasksNotification(context: Context) = cancelAlarm(context, 2001)
    fun cancelPotdNotification(context: Context)  = cancelAlarm(context, 2002)

    fun scheduleEventNotification(context: Context, eventId: Long, triggerAtMillis: Long) {
        val alarmId = (3000 + eventId).toInt()
        scheduleOneShot(context, "events", triggerAtMillis, alarmId)
    }

    fun scheduleContestNotification(context: Context, contestId: Long, triggerAtMillis: Long) {
        val alarmId = (4000 + contestId).toInt()
        scheduleOneShot(context, "contests", triggerAtMillis, alarmId)
    }

    fun cancelEventNotification(context: Context, eventId: Long) {
        cancelAlarm(context, (3000 + eventId).toInt())
    }

    fun rescheduleAll(context: Context) {
        // WorkManager workers will re-schedule; this is a no-op placeholder
        // The midnight reset worker reschedules itself on reboot via WorkManager.
    }

    private fun scheduleDaily(context: Context, type: String, hour: Int, minute: Int, requestCode: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, type, requestCode)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    private fun scheduleOneShot(context: Context, type: String, triggerAtMillis: Long, requestCode: Int) {
        if (triggerAtMillis <= System.currentTimeMillis()) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, type, requestCode)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun cancelAlarm(context: Context, requestCode: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.krata.orbit.NOTIFICATION_ACTION"
        }
        val pending = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pending?.let { am.cancel(it) }
    }

    private fun buildPendingIntent(context: Context, type: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.krata.orbit.NOTIFICATION_ACTION"
            putExtra(EXTRA_NOTIF_TYPE, type)
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
