package app.submissionultra.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.submissionultra.appGraph
import app.submissionultra.notification.EmergencyNotifier
import app.submissionultra.notification.ReminderNotifier
import app.submissionultra.notification.testAssignment
import app.submissionultra.ui.AppMessenger
import app.submissionultra.ui.components.BackTopBar

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

    Scaffold(
        topBar = { BackTopBar("通知のテスト", onBack) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // 緊急通知のテスト。本番(AlarmReceiver)と同一の関数を通る。
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    onClick = {
                        // 本番と同一の関数を、テスト用のダミー Assignment で呼ぶだけ。分岐は無い。
                        EmergencyNotifier.fireEmergencyNotification(context, testAssignment())
                        messenger.show("緊急通知をテスト送信しました")
                    },
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
                    onClick = {
                        context.appGraph.alarmScheduler.scheduleEmergencyTest(10_000L)
                        messenger.show("10秒後にテスト発火します。画面をロックして待ってください")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("10秒後にテスト（ロックして確認）")
                }
            }

            HorizontalDivider()

            // リマインダー通知のテスト。本番(ReminderReceiver)と同一の関数を通る。
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Button(
                    onClick = {
                        // 本番と同一の関数を、テスト用のダミー Assignment で呼ぶだけ。分岐は無い。
                        ReminderNotifier.fireReminder(context, testAssignment())
                        messenger.show("リマインダーをテスト送信しました")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("リマインダーをテスト")
                }
            }
        }
    }
}
