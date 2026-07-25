package app.submissionultra.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * 初回起動時に一度だけ出す、OS 独自の省電力機能についての警告。
 *
 * この設定はアプリからは状態を読めず、許可されていなくても「鳴らない」と表示できない。
 * 起動時チェックの5項目がすべて OK でも通知が遅れうる唯一の穴なので、
 * 気づかないまま使い始めないよう、最初に一度だけ正面から知らせる。
 *
 * 端末をタップやバックキーで閉じられないようにして、どちらのボタンを押すかを必ず選ばせる。
 */
@Composable
fun OemPowerWarningDialog(
    osName: String,
    onOpenSettings: () -> Unit,
    onLater: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { /* ボタンを選ぶまで閉じない */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        title = { Text("この端末では通知が遅れることがあります") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "$osName は、Android 標準の設定とは別に、独自の省電力機能でバックグラウンドの" +
                        "アプリを止めます。通知やアラームの権限をすべて許可していても、" +
                        "画面を消している間に緊急通知が遅れることがあります。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "端末の設定でこのアプリの「自動起動」を許可してください。" +
                        "この設定はアプリから確認できないため、許可されているかを画面に表示できません。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) { Text("設定を開く") }
        },
        dismissButton = {
            TextButton(onClick = onLater) { Text("あとで") }
        },
    )
}
