package app.submissionultra.ui.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import app.submissionultra.MainActivity
import app.submissionultra.appGraph
import app.submissionultra.notification.EmergencyNotifier
import app.submissionultra.notification.NotificationConstants
import app.submissionultra.ui.theme.SubmissionUltraTheme
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos

private data class AlarmData(
    val id: Long = -1L,
    val title: String = "提出物",
    val deadlineMillis: Long = 0L,
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
        applyDarkSystemBars()
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
            teacherName = intent.getStringExtra(NotificationConstants.EXTRA_TEACHER),
        )
    }

    /**
     * 暗い画面に合わせて、ウィンドウ側も暗くする。
     *
     * このアプリのウィンドウテーマは Theme.Material.Light 固定なので、放っておくと
     * (1) Compose が最初のフレームを描くまで白がちらつき、(2) ステータスバーのアイコンが
     * 黒のままで暗い背景に埋もれる。どちらもここで潰す。
     */
    private fun applyDarkSystemBars() {
        window.setBackgroundDrawable(ColorDrawable(AlarmBackground.toArgb()))
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
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

// 緊急を最優先にした、テーマに依存しない固定の見た目。システムのダーク/ライトには追従しない。
//
// 暗い面にするのは、鳴るのが夜であることが多いため。全画面の白は反射的に目をそらさせる。
// 赤は暗い面ではコントラストが落ちるので明るい側へ振ってある（従来の #C62828 は
// この背景に対して約 3.2:1 しかなく沈む。#FF6B60 なら約 6.6:1）。
private val AlarmBackground = Color(0xFF121519)
private val AlarmRed = Color(0xFFFF6B60)
private val AlarmInk = Color(0xFFF2F5F9)
private val AlarmMuted = Color(0xFF9AA4B2)

// 上端のハザードテープ。文字を読まなくても「警告」と分かる記号として置く。
private val HazardRed = Color(0xFFE03A2F)
private val HazardStripe = Color(0xFFF5F5F5)

private val HazardBarBottomGap = 28.dp
private val HazardBarHeight = 28.dp
private val HazardStripeWidth = 24.dp
private val BorderWidth = 3.dp

/** ボタンは画面幅いっぱいには広げない。押し間違えない大きさを保ちつつ、数字を主役に残す。 */
private const val ActionButtonWidthFraction = 0.62f

/** 1 文字あたりの送り幅（em 比）。実測より気持ち広く見積もって、切れる側に倒さない。 */
private const val MonospaceAdvance = 0.65f

/** ミリ秒は主の数字に対するこの比率で小さくする。 */
private const val MillisSizeRatio = 0.35f

/** 縦に収まらなくなるので、いくら幅があってもここまで。 */
private const val MaxCountdownSize = 80f

/**
 * 画面全体の枠。秒が変わる瞬間に最も濃くなるよう、透明度を1秒周期で往復させる。
 *
 * 明滅（点滅）にはしない。画面の縁は面積が大きく、高コントラストで点滅させるのは
 * 光過敏性発作のリスクがあるうえ、情報を持たない装飾が主役のカウントダウンより目立ってしまう。
 * カウントダウンの秒に同期した脈動なら、画面全体が秒針として働き、主役を打ち消さない。
 */
private fun borderAlpha(now: Long): Float {
    val phase = (now % 1000L) / 1000.0
    val pulse = (1.0 + cos(2.0 * PI * phase)) / 2.0
    return (0.25 + 0.60 * pulse).toFloat()
}

@Composable
private fun AlarmContent(
    title: String,
    deadlineMillis: Long,
    teacherName: String?,
    onOpen: () -> Unit,
    onComplete: () -> Unit,
) {
    // 現在時刻はここで一元的に刻み、残り時間・状態ラベル・枠の脈動すべてに効かせる。
    // 表示中に期限をまたいだ瞬間、そのまま超過の表示へ切り替わる。
    val now = rememberFrameTicker()
    val overdue = now >= deadlineMillis

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AlarmBackground)
            .border(BorderWidth, AlarmRed.copy(alpha = borderAlpha(now)))
            .padding(BorderWidth),
    ) {
        // 中央に寄せた塊の先頭にテープを置き、課題名のすぐ上に来るようにする。
        // 左右の余白は内側の列にだけ掛ける。テープは帯として読ませたいので端まで渡す。
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 背の低い端末で数字が伸びても切れないようスクロールを許す。
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HazardBar()
            Spacer(Modifier.height(HazardBarBottomGap))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // 数字を大きく取りたいので左右は詰める。ボタンは幅を絞るので窮屈にはならない。
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    title,
                    color = AlarmInk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(32.dp))
                // 数字を文の途中に置き、上下と合わせて一文として読ませる。
                // 「あと … で終わらせてください」／「期限を過ぎて … 経過しています」。
                // 数だけでは進んでいるのか戻っているのか分からないので、状態もこの文が担う。
                Text(
                    if (overdue) "期限を過ぎて" else "あと",
                    color = AlarmMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Countdown(deadlineMillis = deadlineMillis, now = now, overdue = overdue)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (overdue) "経過しています" else "で終わらせてください",
                    color = AlarmInk,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )

                if (!teacherName.isNullOrBlank()) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "${teacherName}先生が待っています",
                        color = AlarmRed,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(48.dp))

                Button(
                    onClick = onOpen,
                    modifier = Modifier.fillMaxWidth(ActionButtonWidthFraction),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlarmRed,
                        contentColor = AlarmBackground,
                    ),
                ) {
                    Text("開く", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth(ActionButtonWidthFraction),
                    border = BorderStroke(1.5.dp, AlarmRed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlarmRed),
                ) {
                    Text("完了にする")
                }
            }
        }
    }
}

/**
 * 上端のハザードテープ。斜めの縞を繰り返しタイルで描く。
 * 動かさない。流れると視線がそちらに移り、カウントダウンと競合する。
 */
@Composable
private fun HazardBar() {
    val stripe = with(LocalDensity.current) { HazardStripeWidth.toPx() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HazardBarHeight)
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to HazardRed,
                        0.5f to HazardRed,
                        0.5f to HazardStripe,
                        1f to HazardStripe,
                    ),
                    start = Offset.Zero,
                    end = Offset(stripe, stripe),
                    tileMode = TileMode.Repeated,
                ),
            ),
    )
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
 * この画面の主役。締切までの残り時間（超過後は経過時間）をライブ表示する。
 *
 * 文章で挟まず数字だけを置く。何の時間かは上のラベルが担う。
 * 1 秒未満は常に 3 桁で出し、止まっていないことを見せ続ける。
 *
 * 時が付くと桁が 4 つ増えるので、そのぶん文字を落として 1 行に収める。
 * 残りが 1 時間を切ると数字が大きくなり、そこから緊迫感が増す。
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
    val millisText = String.format(".%03d", millis)

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        // 等幅フォントなので、字数が決まれば必要な幅も決まる。固定サイズにすると
        // 幅の狭い端末や桁の多い表示で切れるため、使える幅から入る最大の大きさを逆算する。
        // 端末の文字サイズ設定（fontScale）も勘定に入れないと、大きく設定した人だけ溢れる。
        val units = main.length + millisText.length * MillisSizeRatio
        val fitted = maxWidth.value / (MonospaceAdvance * units * LocalDensity.current.fontScale)
        val mainSize = fitted.coerceAtMost(MaxCountdownSize).sp
        val millisSize = mainSize * MillisSizeRatio

        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontSize = mainSize, fontWeight = FontWeight.Bold)) {
                    append(main)
                }
                withStyle(SpanStyle(fontSize = millisSize, fontWeight = FontWeight.Bold)) {
                    append(millisText)
                }
            },
            color = AlarmRed,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}
