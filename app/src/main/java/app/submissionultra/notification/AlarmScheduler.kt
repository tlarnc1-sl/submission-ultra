package app.submissionultra.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.submissionultra.MainActivity
import app.submissionultra.data.Assignment
import app.submissionultra.domain.Timing

/**
 * 最後の開始時刻に緊急通知を発火させるための、正確なアラームのスケジューラ。
 *
 * 使うのは [AlarmManager.setAlarmClock]。目覚まし時計と同じ扱いになり、Doze・バッテリー
 * 最適化・App Standby バケットのいずれからも配信を絞られない唯一の API だから。
 *
 * setExactAndAllowWhileIdle では足りない。あれは Doze 中は 9 分に 1 回までに制限され、
 * さらに「めったに開かないアプリ」ほど App Standby バケットが下がって配信枠が削られる。
 * 提出物アプリは本質的にめったに開かないので、最も締め付けの強いバケットに落ちる。
 * つまり、いちばん鳴ってほしい状況でいちばん鳴らなくなる API だった。
 *
 * WorkManager は正確な時刻を保証しないため、そもそも緊急通知には使わない。
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /**
     * 全ての未完了提出物について、最後の開始時刻にアラームを（再）設定する。
     * 余裕時間の変更・再起動・アプリ更新・見張りの発火時に呼ぶ。
     *
     * ここを通ったこと自体を [AlarmHealth] に記録し、見張りごと止められていた期間を
     * 後から検出できるようにする。併せて次の見張りも張り直す。
     */
    fun rescheduleAll(active: List<Assignment>, marginMinutes: Int) {
        val now = System.currentTimeMillis()
        for (assignment in active) {
            scheduleOne(assignment, marginMinutes, now)
        }
        AlarmHealth.recordReschedule(context)
        scheduleWatchdog()
    }

    /** 1 課題分の緊急アラームを設定し直す。追加・編集・完了の切り替え時に呼ぶ。 */
    fun schedule(assignment: Assignment, marginMinutes: Int) {
        scheduleOne(assignment, marginMinutes, System.currentTimeMillis())
    }

    private fun scheduleOne(assignment: Assignment, marginMinutes: Int, now: Long) {
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

    /**
     * 目覚まし時計として時刻を予約する。
     *
     * 権限が無い端末では黙って落とさず、不正確でも設定はしておく（何も鳴らないよりましだが、
     * 時刻の保証は無い）。「正確に通知できない」ことは ReadinessChecker がバナーで明示する。
     */
    private fun scheduleExact(assignmentId: Long, triggerAtMillis: Long) {
        val pendingIntent = pendingIntentFor(assignmentId)
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
        try {
            if (canExact) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMillis, showAlarmIntent()),
                    pendingIntent,
                )
            } else {
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

    /**
     * アラームの見張りを張る。
     *
     * 再発火の鎖（[scheduleEmergencyRetry]）は、前回の発火に成功したときにしか次を予約しない。
     * 一度でも配信を落とされれば鎖は黙って切れ、次にアプリを手で開くまで永久に無音になる。
     * この見張りだけは鎖の外にあり、定期的に全部を引き直して切れた鎖を繋ぎ直す。
     *
     * 見張り自体の時刻は厳密でなくてよいので、権限の要らない不正確なアラームで張る。
     * 目覚まし時計として張ると、鳴らす予定の無い時刻にも通知領域へ目覚ましアイコンが出てしまう。
     */
    fun scheduleWatchdog() {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + NotificationConstants.WATCHDOG_INTERVAL_MILLIS,
            watchdogPendingIntent(),
        )
    }

    private fun watchdogPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        NotificationConstants.WATCHDOG_REQUEST_CODE,
        Intent(context, WatchdogReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /**
     * 通知領域の目覚ましアイコンをタップしたときに開く先。
     * 目覚まし時計として予約する以上、その正体をアプリで示せる必要がある。
     */
    private fun showAlarmIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        NotificationConstants.ALARM_SHOW_REQUEST_CODE,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

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
