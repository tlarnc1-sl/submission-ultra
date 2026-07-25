package app.submissionultra.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.submissionultra.data.Assignment
import app.submissionultra.domain.Timing

/**
 * 最後の開始時刻に緊急通知を発火させるための、正確なアラームのスケジューラ。
 *
 * 正確な時刻保証のため AlarmManager.setExactAndAllowWhileIdle() を使う（Doze 中でも発火）。
 * WorkManager は正確な時刻を保証しないため緊急通知には使わない。
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /**
     * 全ての未完了提出物について、最後の開始時刻にアラームを（再）設定する。
     * 余裕時間の変更・課題の追加編集・再起動時に呼ぶ。
     */
    fun rescheduleAll(active: List<Assignment>, marginMinutes: Int) {
        val now = System.currentTimeMillis()
        for (assignment in active) {
            val triggerAt = Timing.criticalStartMillis(assignment, marginMinutes)
            if (triggerAt > now) {
                scheduleExact(assignment.id, triggerAt)
            } else {
                // 最後の開始時刻を既に過ぎている未完了課題は、今この瞬間に緊急通知を出す。
                // ただし「開く」で作業に入った直後だけは鳴らさない（鳴らすとアプリを開けなくなる）。
                cancel(assignment.id)
                if (!EmergencyNotifier.isRecentlyAcknowledged(context, assignment.id)) {
                    EmergencyNotifier.fireEmergencyNotification(context, assignment)
                }
                scheduleEmergencyRetry(assignment)
            }
        }
    }

    /**
     * 緊急通知を鳴らし直す予約。無視されたまま無音に戻らないようにする。
     *
     * 期限を過ぎてまで鳴らし続けるのは、もう間に合わない相手を延々と叩き続けるだけなので、
     * 次の発火予定が期限を越える場合は打ち切る。完了にすれば [cancel] で止まる。
     */
    fun scheduleEmergencyRetry(assignment: Assignment) {
        val nextAt = System.currentTimeMillis() + NotificationConstants.EMERGENCY_RETRY_INTERVAL_MILLIS
        if (nextAt >= assignment.deadlineEpochMillis) return
        scheduleExact(assignment.id, nextAt)
    }

    private fun scheduleExact(assignmentId: Long, triggerAtMillis: Long) {
        val pendingIntent = pendingIntentFor(assignmentId)
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            } else {
                // 正確なアラーム権限が無い場合でも黙って落とさず、不正確でも設定はしておく。
                // 「正確に通知できない」ことは ReadinessChecker がバナーで明示する。
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
    }

    fun cancel(assignmentId: Long) {
        alarmManager.cancel(pendingIntentFor(assignmentId))
    }

    /**
     * 指定秒後に緊急通知のテストを、本番と同じ経路（AlarmManager→AlarmReceiver→fireEmergencyNotification）で発火する。
     * 画面をロックして待てば、本番と同じく全画面アラームが出るかを確認できる。
     */
    fun scheduleEmergencyTest(delayMillis: Long) {
        scheduleExact(NotificationConstants.TEST_ASSIGNMENT_ID, System.currentTimeMillis() + delayMillis)
    }

    private fun pendingIntentFor(assignmentId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(NotificationConstants.EXTRA_ASSIGNMENT_ID, assignmentId)
        }
        return PendingIntent.getBroadcast(
            context,
            assignmentId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
