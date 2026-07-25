package app.submissionultra

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import app.submissionultra.notification.EmergencyNotifier
import app.submissionultra.notification.NotificationConstants
import app.submissionultra.ui.AppRoot
import app.submissionultra.ui.theme.SubmissionUltraTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* 結果は起動時チェックが反映する */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()

        // 緊急通知から開かれたなら、その課題は「今から着手する」とみなす。
        // これが無いと、直後の rescheduleAll がその場でまた鳴らし、アプリを開けなくなる。
        val fromEmergencyId = intent.getLongExtra(NotificationConstants.EXTRA_ASSIGNMENT_ID, -1L)
        if (fromEmergencyId >= 0L) {
            EmergencyNotifier.acknowledge(this, fromEmergencyId)
        }

        // 起動のたびに、未完了課題のアラームを引き直す。
        // 端末再起動やアプリ強制終了でアラームが失われていても、ここで復旧する。
        // 最後の開始時刻を既に過ぎている課題があれば、この時点で緊急通知が出る。
        lifecycleScope.launch {
            appGraph.assignmentRepository.rescheduleAll()
        }

        setContent {
            SubmissionUltraTheme {
                AppRoot()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
