package app.submissionultra.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import app.submissionultra.domain.Urgency
import app.submissionultra.ui.formatCountdownClock
import app.submissionultra.ui.theme.Space
import kotlinx.coroutines.delay

/**
 * このアプリで表示するカウントダウンは常にこれ 1 つ。
 * 「何までの残り時間か」を緊急度から自動で決めるので、開始まで/締切までが並んで混乱することがない。
 *
 * HAS_TIME   … 開始リミット（最後の開始時刻）までの残り
 * PAST_START … 締切までの残り（開始リミットは過ぎたので、以降は締切が主役）
 * OVERDUE    … 数値ではなく「期限を過ぎています」
 *
 * 色分けはしない。重要さは文字の大きさと太さだけで表し、読みやすさを最優先する。
 */
@Composable
fun CountdownDisplay(
    urgency: Urgency,
    criticalStartMillis: Long,
    deadlineMillis: Long,
    valueFontSize: TextUnit,
    modifier: Modifier = Modifier,
    labelStyle: TextStyle = MaterialTheme.typography.labelMedium,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    // 自分で現在時刻を刻む。ViewModel 側の再計算に頼ると、リストの中身が前回と等しい限り
    // StateFlow が値を捨ててしまい（等値なら通知しない）、秒が動かなくなるため。
    val now = rememberTickingNow()

    val label: String
    val value: String
    when (urgency) {
        Urgency.HAS_TIME -> {
            // 「あと何分の猶予か」ではなく「いつまでに始めなければならないか」を伝える。
            label = "開始リミットまで"
            value = formatCountdownClock(criticalStartMillis, now) ?: "まもなく"
        }
        Urgency.PAST_START -> {
            label = "締切まで"
            value = formatCountdownClock(deadlineMillis, now) ?: "まもなく"
        }
        Urgency.OVERDUE -> {
            label = ""
            value = "期限を過ぎています"
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = labelStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = value,
            fontSize = valueFontSize,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * 秒が変わる瞬間に合わせて現在時刻を更新する。
 * 秒の境界まで待ってから更新するので、表示が飛んだり遅れたりしない。
 */
@Composable
private fun rememberTickingNow(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            val current = System.currentTimeMillis()
            now = current
            delay(1_000L - (current % 1_000L))
        }
    }
    return now
}
