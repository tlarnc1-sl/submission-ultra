package app.submissionultra.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.submissionultra.appGraph
import app.submissionultra.notification.EmergencyNotifier
import app.submissionultra.notification.ReminderNotifier
import app.submissionultra.notification.testAssignment
import app.submissionultra.ui.AppMessenger
import app.submissionultra.ui.components.BackTopBar
import app.submissionultra.ui.theme.EmphasisScope
import app.submissionultra.ui.theme.ScreenLead
import app.submissionultra.ui.theme.Space
import app.submissionultra.ui.theme.SurfaceLevel
import app.submissionultra.ui.theme.border
import app.submissionultra.ui.theme.color
import app.submissionultra.ui.theme.radius
import app.submissionultra.ui.theme.shadow

/**
 * 通知のテスト。テスト通知は本番と同じ関数・同じチャンネルを通る。
 * ここで鳴ることが、本番でこの端末設定でも鳴ることの証明になる。
 */
@Composable
fun TestNotificationScreen(
    onBack: () -> Unit,
    messenger: AppMessenger,
) {
    val context = LocalContext.current

    EmphasisScope("通知のテスト") {
        Scaffold(
            topBar = { BackTopBar("通知のテスト", onBack) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(Space.lg),
                verticalArrangement = Arrangement.spacedBy(Space.xl),
            ) {
                // この画面の存在理由は「本当に鳴るのか」を自分の耳で確かめさせること。
                // リマインダーの確認はその副次なので、同じ重さで並べない。
                ScreenLead {
                    EmergencyTestSection(
                        onFireNow = {
                            // 本番と同一の関数を、テスト用のダミー Assignment で呼ぶだけ。分岐は無い。
                            EmergencyNotifier.fireEmergencyNotification(context, testAssignment())
                            messenger.show("緊急通知をテスト送信しました")
                        },
                        onScheduleLocked = {
                            context.appGraph.alarmScheduler.scheduleEmergencyTest(10_000L)
                            messenger.show("10秒後にテスト発火します。画面をロックして待ってください")
                        },
                    )
                }

                HorizontalDivider()

                ReminderTestSection(
                    onFire = {
                        // 本番と同一の関数を、テスト用のダミー Assignment で呼ぶだけ。分岐は無い。
                        ReminderNotifier.fireReminder(context, testAssignment())
                        messenger.show("リマインダーをテスト送信しました")
                    },
                )
            }
        }
    }
}

/** 緊急通知のテスト。本番(AlarmReceiver)と同一の関数を通る。 */
@Composable
private fun EmergencyTestSection(
    onFireNow: () -> Unit,
    onScheduleLocked: () -> Unit,
) {
    val level = SurfaceLevel.Lead

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(level.radius),
        color = level.color(),
        shadowElevation = level.shadow,
        border = level.border(),
    ) {
        Column(
            modifier = Modifier.padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            Text("緊急通知のテスト", style = MaterialTheme.typography.titleMedium)
            Text(
                "本番の緊急通知と全く同じ経路・音・振動・DND 例外で発火します。ここで鳴ることが、" +
                    "本番でこの端末設定でも鳴ることの証明になります。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "音は、端末に設定されている既定のアラーム音（目覚まし用の音）で鳴ります。" +
                    "変えたいときは端末の「音」設定でアラーム音を変更してください。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onFireNow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("今すぐテスト")
            }
            Text(
                "全画面のアラーム画面はロック中/画面オフのときだけ出ます。下のボタンを押し、" +
                    "10秒以内に画面をロックして待つと、本番と同じ全画面が確認できます。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onScheduleLocked,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("10秒後にテスト（ロックして確認）")
            }
        }
    }
}

/**
 * リマインダー通知のテスト。本番(ReminderReceiver)と同一の関数を通る。
 *
 * ボタンを枠線にしてあるのは、こちらが従だから。緊急通知と同じ塗りにすると、
 * どちらを試すべきかが読み取れなくなる。
 */
@Composable
private fun ReminderTestSection(onFire: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Text("リマインダー通知のテスト", style = MaterialTheme.typography.titleMedium)
        Text(
            "本番のリマインダーと全く同じ経路・チャンネルで発火します。緊急通知とは別で、" +
                "マナーモード・おやすみモードを尊重するため、静かに出るのが正しい動作です。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "音は、端末に設定されている既定の通知音で鳴ります。" +
                "変えたいときは端末の「音」設定で通知音を変更してください。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onFire,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("リマインダーをテスト")
        }
    }
}
