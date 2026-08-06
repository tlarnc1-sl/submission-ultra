package app.submissionultra.ui.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.submissionultra.calendar.CalendarImport
import app.submissionultra.calendar.CalendarItem
import app.submissionultra.ui.formatDateTime
import app.submissionultra.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 一覧が画面を埋め尽くさないよう、この高さで頭打ちにしてスクロールさせる。 */
private val ListMaxHeight = 360.dp

/**
 * カレンダーの予定から 1 件選ぶ。
 *
 * 選んでも保存はしない。課題名と締切を入力欄に写すだけで、作業時間は本人が入れる。
 * 「本気でやれば終わる作業時間」はカレンダーには書かれていないし、
 * それを見積もることがこのアプリの本体なので、そこは自動化しない。
 */
@Composable
fun CalendarPickerDialog(
    onPick: (CalendarItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    // ContentResolver への問い合わせは IO なので、画面を止めないよう別スレッドで読む。
    val items by produceState<List<CalendarItem>?>(initialValue = null, context) {
        value = withContext(Dispatchers.IO) { CalendarImport.upcoming(context) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("カレンダーから取り込む") },
        text = {
            val loaded = items
            when {
                loaded == null -> Text(
                    "読み込んでいます…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                loaded.isEmpty() -> Text(
                    "これから60日以内の予定が見つかりませんでした。" +
                        "Classroom の課題は、期限が設定されていればカレンダーアプリに出ます。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> LazyColumn(modifier = Modifier.heightIn(max = ListMaxHeight)) {
                    // 繰り返しの予定は同じ EVENT_ID で日付ちがいの回が並ぶので、
                    // ID だけを鍵にすると重複して落ちる。開始時刻まで含めて一意にする。
                    items(loaded, key = { "${it.eventId}-${it.deadlineMillis}" }) { item ->
                        CalendarRow(item = item, onClick = { onPick(item) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("閉じる") }
        },
    )
}

@Composable
private fun CalendarRow(item: CalendarItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Space.md),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Text(item.title, style = MaterialTheme.typography.bodyLarge)
        Text(
            // 終日の予定には時刻が無いので 23:59 を補っている。推測であることを隠さない。
            buildString {
                append(formatDateTime(item.deadlineMillis))
                if (!item.timeKnown) append("（終日）")
                if (item.calendarName.isNotEmpty()) append(" ・ ${item.calendarName}")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
