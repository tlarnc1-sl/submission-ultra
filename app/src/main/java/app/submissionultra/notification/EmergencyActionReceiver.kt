package app.submissionultra.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import app.submissionultra.SubmissionUltraApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 緊急通知の「完了にする」アクションを受け取る。通知トレイ/ヘッドアップから直接完了できる。
 * 完了処理は AssignmentRepository.setCompleted に委譲（アラーム/リマインダー解除・completedAt 記録も行われる）。
 */
class EmergencyActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NotificationConstants.ACTION_COMPLETE) return
        val assignmentId = intent.getLongExtra(NotificationConstants.EXTRA_ASSIGNMENT_ID, -1L)

        // 対象が実在すれば完了にし、いずれにせよこの通知は消す（テスト用ダミーは no-op）。
        val pending = goAsync()
        val app = context.applicationContext as SubmissionUltraApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val assignment = app.graph.assignmentRepository.getById(assignmentId)
                if (assignment != null && !assignment.isCompleted) {
                    app.graph.assignmentRepository.setCompleted(assignment, true)
                }
                NotificationManagerCompat.from(context)
                    .cancel(EmergencyNotifier.emergencyNotificationId(assignmentId))
            } finally {
                pending.finish()
            }
        }
    }
}
