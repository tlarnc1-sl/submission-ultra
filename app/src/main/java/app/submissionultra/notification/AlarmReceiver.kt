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
                    if (EmergencyNotifier.isWorkingOnIt(context, assignmentId)) {
                        // アプリを前面に開いて、この課題に着手している最中。画面には残り時間が
                        // 出ているので、ここで鳴らすのは知らせるためではなく作業の邪魔でしかない。
                        // 抑制を延長するだけにして、前面から離れれば次の発火で必ず鳴るようにする。
                        EmergencyNotifier.acknowledge(context, assignmentId)
                    } else {
                        EmergencyNotifier.fireEmergencyNotification(context, assignment)
                    }
                    // 鳴らしたかどうかに関わらず、無視されたまま終わらないよう次の発火を予約する。
                    app.graph.alarmScheduler.scheduleEmergencyRetry(assignment)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
