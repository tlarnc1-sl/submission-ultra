package app.submissionultra.ui.alarm

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import app.submissionultra.MainActivity
import app.submissionultra.appGraph
import app.submissionultra.data.AssignmentType
import app.submissionultra.data.deadlineLabel
import app.submissionultra.notification.EmergencyNotifier
import app.submissionultra.notification.NotificationConstants
import app.submissionultra.ui.formatDateTime
import app.submissionultra.ui.theme.SubmissionUltraTheme
import kotlinx.coroutines.launch

private data class AlarmData(
    val id: Long = -1L,
    val title: String = "提出物",
    val deadlineMillis: Long = 0L,
    val type: AssignmentType = AssignmentType.CLASSROOM,
    val teacherName: String? = null,
)

/**
 * 緊急通知が発火したときに全画面で出るアラーム画面。ロック画面上でも表示し、画面を点灯させる。
 * 「開く」と「完了にする」だけの、迷わないミニマルな画面。
 */
class EmergencyAlarmActivity : ComponentActivity() {

    // 溜まった後続の緊急通知（singleTask で onNewIntent 配信）でも最新の内容に差し替える。
    private val data = mutableStateOf(AlarmData())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        applyIntent(intent)

        setContent {
            SubmissionUltraTheme {
                val d = data.value
                AlarmContent(
                    title = d.title,
                    deadlineLabel = d.type.deadlineLabel(),
                    deadlineText = formatDateTime(d.deadlineMillis),
                    deadlineMillis = d.deadlineMillis,
                    teacherName = d.teacherName,
                    onOpen = { openApp(d.id) },
                    onComplete = { complete(d.id) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyIntent(intent)
    }

    private fun applyIntent(intent: Intent) {
        data.value = AlarmData(
            id = intent.getLongExtra(NotificationConstants.EXTRA_ASSIGNMENT_ID, -1L),
            title = intent.getStringExtra(NotificationConstants.EXTRA_TITLE) ?: "提出物",
            deadlineMillis = intent.getLongExtra(NotificationConstants.EXTRA_DEADLINE_MILLIS, 0L),
            type = runCatching {
                AssignmentType.valueOf(intent.getStringExtra(NotificationConstants.EXTRA_TYPE) ?: "")
            }.getOrDefault(AssignmentType.CLASSROOM),
            teacherName = intent.getStringExtra(NotificationConstants.EXTRA_TEACHER),
        )
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun openApp(assignmentId: Long) {
        cancelNotification(assignmentId)
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
        finish()
    }

    private fun complete(assignmentId: Long) {
        // 実在すれば完了に。いずれにせよ通知を消して閉じる（テスト用ダミーは no-op）。
        lifecycleScope.launch {
            appGraph.assignmentRepository.getById(assignmentId)?.let {
                if (!it.isCompleted) appGraph.assignmentRepository.setCompleted(it, true)
            }
            cancelNotification(assignmentId)
            finish()
        }
    }

    private fun cancelNotification(assignmentId: Long) {
        NotificationManagerCompat.from(this)
            .cancel(EmergencyNotifier.emergencyNotificationId(assignmentId))
    }
}

// 緊急を最優先にした、テーマに依存しない固定の見た目：白背景 × 赤文字。中央に集約。
private val AlarmBackground = Color.White
private val AlarmRed = Color(0xFFC62828)
private val AlarmInk = Color(0xFF1A1A1A)
private val AlarmMuted = Color(0xFF6B6B6B)

@Composable
private fun AlarmContent(
    title: String,
    deadlineLabel: String,
    deadlineText: String,
    deadlineMillis: Long,
    teacherName: String?,
    onOpen: () -> Unit,
    onComplete: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AlarmBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "今すぐ始めないと間に合わない",
                color = AlarmRed,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                title,
                color = AlarmInk,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "最後の開始時刻を過ぎました。今始めれば間に合います。",
                color = AlarmRed,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))
            Countdown(deadlineMillis = deadlineMillis)

            Spacer(Modifier.height(12.dp))
            Text(
                "$deadlineLabel $deadlineText",
                color = AlarmMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )

            if (!teacherName.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "${teacherName}先生の期待を裏切ることになります。",
                    color = AlarmRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlarmRed,
                    contentColor = Color.White,
                ),
            ) {
                Text("開く", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.5.dp, AlarmRed),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlarmRed),
            ) {
                Text("完了にする")
            }
        }
    }
}

/**
 * 締切までの残り時間を 0.001 秒刻みでライブ表示。「あと {残り} で終わらせましょう！」。
 * 1 秒未満（ミリ秒）の位は小さめの文字にする。
 */
@Composable
private fun Countdown(deadlineMillis: Long) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(deadlineMillis) {
        // 毎フレーム現在時刻を更新（約60fps）。ミリ秒の位が動いて見える。
        while (true) {
            withFrameMillis { now = System.currentTimeMillis() }
        }
    }

    val remaining = (deadlineMillis - now).coerceAtLeast(0L)
    val hours = remaining / 3_600_000L
    val minutes = (remaining % 3_600_000L) / 60_000L
    val seconds = (remaining % 60_000L) / 1000L
    val millis = remaining % 1000L
    val main = if (hours > 0L) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
    val sub = String.format(".%03d", millis)

    Text(
        text = buildAnnotatedString {
            append("あと ")
            withStyle(SpanStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold)) { append(main) }
            withStyle(SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)) { append(sub) }
        },
        color = AlarmRed,
        fontSize = 18.sp,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        "で終わらせましょう！",
        color = AlarmInk,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        textAlign = TextAlign.Center,
    )
}
