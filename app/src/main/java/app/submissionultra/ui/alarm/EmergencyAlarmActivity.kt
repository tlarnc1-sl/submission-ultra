package app.submissionultra.ui.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
 *
 * この画面は逃げ道を作らない:
 * - 表示中はアラーム音と振動を鳴らし続ける（通知音の一発だけでは寝ていれば気づけない）
 * - 戻るキーでは閉じられない（2つのボタンのどちらかを押すまで居座る）
 * - 期限を過ぎたら文言と残り時間表示を切り替える（「間に合う」と嘘をつかない）
 */
class EmergencyAlarmActivity : ComponentActivity() {

    // 溜まった後続の緊急通知（singleTask で onNewIntent 配信）でも最新の内容に差し替える。
    private val data = mutableStateOf(AlarmData())

    private val siren by lazy { AlarmSiren(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        applyIntent(intent)

        setContent {
            SubmissionUltraTheme {
                val d = data.value
                // 戻るキーで無かったことにはできない。ホームキーで離れても、通知は残り続け、
                // 一定間隔で鳴らし直される（AlarmScheduler.scheduleEmergencyRetry）。
                BackHandler(enabled = true) { /* 意図的に何もしない */ }
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

    /** 画面が見えている間だけ鳴らす。ホームキーで離れれば止まり、戻れば また鳴る。 */
    override fun onStart() {
        super.onStart()
        siren.start()
    }

    override fun onStop() {
        super.onStop()
        siren.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        siren.stop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyIntent(intent)
        // 別の課題の緊急通知で呼び直された場合も、確実に鳴っている状態にする。
        siren.start()
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
        siren.stop()
        // 起動直後の rescheduleAll でここへ引き戻されないようにする。
        EmergencyNotifier.acknowledge(this, assignmentId)
        cancelNotification(assignmentId)
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
        finish()
    }

    private fun complete(assignmentId: Long) {
        // 実在すれば完了に。いずれにせよ通知を消して閉じる（テスト用ダミーは no-op）。
        siren.stop()
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

/**
 * アラーム音と振動を鳴らし続ける。
 *
 * 通知チャンネルの音は一度きりで、寝ている相手には届かないことがある。この画面が出ている間は
 * 目覚まし相当の音を USAGE_ALARM で鳴らし続ける（マナーモードでもアラーム音量で鳴る）。
 * 音が取れない端末でも振動だけは続くように、両者は独立して動かす。
 */
private class AlarmSiren(private val context: Context) {

    private var player: MediaPlayer? = null

    fun start() {
        if (player == null) player = buildPlayer()
        startVibration()
    }

    fun stop() {
        player?.let { p ->
            runCatching { if (p.isPlaying) p.stop() }
            runCatching { p.release() }
        }
        player = null
        runCatching { vibrator()?.cancel() }
    }

    private fun buildPlayer(): MediaPlayer? {
        // 端末にアラーム音が無い場合は着信音で代替する。どちらも取れなければ振動だけで知らせる。
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: return null
        return runCatching {
            MediaPlayer().apply {
                setDataSource(context, uri)
                setAudioAttributes(ALARM_AUDIO_ATTRIBUTES)
                isLooping = true
                prepare()
                start()
            }
        }.getOrNull()
    }

    private fun startVibration() {
        val vibrator = vibrator() ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(PATTERN, REPEAT_FROM_START),
                    ALARM_AUDIO_ATTRIBUTES,
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(PATTERN, REPEAT_FROM_START, ALARM_AUDIO_ATTRIBUTES)
            }
        }
    }

    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    private companion object {
        val ALARM_AUDIO_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        /** 通知チャンネルと同じ刻み。末尾に間を置いてから先頭に戻る。 */
        val PATTERN = longArrayOf(0, 500, 250, 500, 250, 500, 1000)

        /** 先頭から繰り返す。止めるまで振動し続ける。 */
        const val REPEAT_FROM_START = 0
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
    // 現在時刻はここで一元的に刻み、文言と残り時間の両方に効かせる。
    // 表示中に期限をまたいだ瞬間、そのまま「期限を過ぎています」へ切り替わる。
    val now = rememberFrameTicker()
    val overdue = now >= deadlineMillis

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
                if (overdue) "期限を過ぎています" else "今すぐ始めないと間に合わない",
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
                if (overdue) {
                    "まだ提出できていません。今すぐ出してください。"
                } else {
                    "最後の開始時刻を過ぎました。今始めれば間に合います。"
                },
                color = AlarmRed,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))
            Countdown(deadlineMillis = deadlineMillis, now = now, overdue = overdue)

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
                    if (overdue) {
                        "${teacherName}先生が待っています。"
                    } else {
                        "${teacherName}先生の期待を裏切ることになります。"
                    },
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

/** 毎フレーム現在時刻を刻む（約60fps）。ミリ秒の位が動いて見える。 */
@Composable
private fun rememberFrameTicker(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { now = System.currentTimeMillis() }
        }
    }
    return now
}

/**
 * 締切までの残り時間を 0.001 秒刻みでライブ表示。「あと {残り} で終わらせましょう！」。
 * 1 秒未満（ミリ秒）の位は小さめの文字にする。
 * 期限を過ぎたあとは、残り 0 のまま固まらせず「経過時間」に切り替える。
 */
@Composable
private fun Countdown(deadlineMillis: Long, now: Long, overdue: Boolean) {
    val delta = if (overdue) now - deadlineMillis else deadlineMillis - now
    val elapsedOrRemaining = delta.coerceAtLeast(0L)

    val hours = elapsedOrRemaining / 3_600_000L
    val minutes = (elapsedOrRemaining % 3_600_000L) / 60_000L
    val seconds = (elapsedOrRemaining % 60_000L) / 1000L
    val millis = elapsedOrRemaining % 1000L
    val main = if (hours > 0L) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
    val sub = String.format(".%03d", millis)

    Text(
        text = buildAnnotatedString {
            if (!overdue) append("あと ")
            withStyle(SpanStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold)) { append(main) }
            withStyle(SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)) { append(sub) }
            if (overdue) append(" 経過")
        },
        color = AlarmRed,
        fontSize = 18.sp,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        if (overdue) "1分でも早く出しましょう。" else "で終わらせましょう！",
        color = AlarmInk,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        textAlign = TextAlign.Center,
    )
}
