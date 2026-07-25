package app.submissionultra.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.submissionultra.SubmissionUltraApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 最後の開始時刻に AlarmManager から呼ばれる。ここが本番の緊急通知の入口。
 * テスト経路（設定画面のボタン）と同じ [EmergencyNotifier.fireEmergencyNotification] を呼ぶ。
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val assignmentId = intent.getLongExtra(NotificationConstants.EXTRA_ASSIGNMENT_ID, -1L)

        // 遅延テスト：本番と同じ関数を、テスト用ダミーで呼ぶだけ（DB は引かない）。
        if (assignmentId == NotificationConstants.TEST_ASSIGNMENT_ID) {
            EmergencyNotifier.fireEmergencyNotification(context, testAssignment())
            return
        }

        if (assignmentId < 0L) return

        val pending = goAsync()
        val app = context.applicationContext as SubmissionUltraApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val assignment = app.graph.assignmentDao.getById(assignmentId)
                // 発火時点で未完了の場合のみ緊急通知を出す。
                if (assignment != null && !assignment.isCompleted) {
                    EmergencyNotifier.fireEmergencyNotification(context, assignment)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
