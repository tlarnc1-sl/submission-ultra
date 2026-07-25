package app.submissionultra.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.submissionultra.data.Assignment
import app.submissionultra.data.ReminderConfig
import app.submissionultra.data.ReminderMode
import java.time.Instant
import java.time.ZoneId

/**
 * 締切の3日前から毎日、未完了なら鳴らすリマインダーのスケジューラ。
 *
 * リマインダーは緊急通知ほど厳密な時刻保証を必要としないため、正確なアラーム権限が不要な
 * setAndAllowWhileIdle()（不正確だが Doze 中でも発火）を使う。
 * これにより、正確なアラーム権限が無い端末でもリマインダーは機能する。
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** 1課題分のリマインダーを設定し直す。完了済みなら全て取り消す。 */
    fun reschedule(assignment: Assignment, config: ReminderConfig) {
        cancel(assignment.id)
        if (assignment.isCompleted) return

        val now = System.currentTimeMillis()
        val deadline = assignment.deadlineEpochMillis
        for ((slot, triggerAt) in reminderSlots(deadline, config)) {
            // 今より後、かつ締切より前のものだけ設定する。
            if (triggerAt in (now + 1) until deadline) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntentFor(assignment.id, slot),
                )
            }
        }
    }

    fun cancel(assignmentId: Long) {
        for (slot in 0 until NotificationConstants.REMINDER_SLOT_COUNT) {
            alarmManager.cancel(pendingIntentFor(assignmentId, slot))
        }
    }

    /**
     * リマインダーを鳴らす (枠番号, 時刻ミリ秒) の一覧。
     * FIXED:    締切の3日前〜当日の各日、決まった時刻。
     * DEADLINE: 締切の3日前・2日前・1日前の、締切と同じ時刻。
     */
    private fun reminderSlots(deadlineMillis: Long, config: ReminderConfig): List<Pair<Int, Long>> {
        return when (config.mode) {
            ReminderMode.FIXED -> {
                val deadlineDate = Instant.ofEpochMilli(deadlineMillis)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                (ReminderConfig.DAYS_BEFORE downTo 0).map { daysBefore ->
                    val millis = deadlineDate.minusDays(daysBefore.toLong())
                        .atTime(config.hour, config.minute)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    daysBefore to millis
                }
            }

            ReminderMode.DEADLINE -> {
                (ReminderConfig.DAYS_BEFORE downTo 1).map { daysBefore ->
                    daysBefore to (deadlineMillis - daysBefore * DAY_MILLIS)
                }
            }
        }
    }

    private fun pendingIntentFor(assignmentId: Long, slot: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(NotificationConstants.EXTRA_ASSIGNMENT_ID, assignmentId)
        }
        val requestCode = (assignmentId * NotificationConstants.REMINDER_SLOT_COUNT + slot).toInt()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}
