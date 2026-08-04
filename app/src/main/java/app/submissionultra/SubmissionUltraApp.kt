package app.submissionultra

import android.app.Application
import android.content.Context
import app.submissionultra.data.AppDatabase
import app.submissionultra.data.AssignmentDao
import app.submissionultra.data.AssignmentRepository
import app.submissionultra.data.SettingsRepository
import app.submissionultra.notification.AlarmScheduler
import app.submissionultra.notification.EmergencyNotifier
import app.submissionultra.notification.ReminderNotifier
import app.submissionultra.notification.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * アプリ全体の依存を組み立てる軽量な依存グラフ。Hilt を使わずシンプルに保つ。
 */
class AppGraph(context: Context) {
    /**
     * 画面の生死に左右されない仕事のためのスコープ。
     *
     * アラームの引き直しを Activity のスコープで走らせると、途中で画面が回っただけで
     * 中断され、一部の提出物だけアラームが張られない状態が残りうる。鳴らすための仕事は
     * 画面より長く生きなければならない。
     */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database = AppDatabase.get(context)
    val assignmentDao: AssignmentDao = database.assignmentDao()
    val settingsRepository = SettingsRepository(context)
    val alarmScheduler = AlarmScheduler(context)
    val reminderScheduler = ReminderScheduler(context)
    val assignmentRepository =
        AssignmentRepository(assignmentDao, settingsRepository, alarmScheduler, reminderScheduler)
}

class SubmissionUltraApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        // 緊急通知チャンネルは早期に確実に作成しておく（起動時チェックの前提）。
        EmergencyNotifier.ensureChannel(this)
        // リマインダーチャンネルも作成しておく。
        ReminderNotifier.ensureChannel(this)
    }
}

/** Composable / ViewModel から依存グラフを取得するためのヘルパー。 */
val Context.appGraph: AppGraph
    get() = (applicationContext as SubmissionUltraApp).graph
