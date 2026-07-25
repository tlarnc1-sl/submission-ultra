package app.submissionultra.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.submissionultra.domain.Urgency
import app.submissionultra.ui.home.HomeItem

/**
 * 「今この瞬間、何に着手すべきか」を一目で示すホーム上部のヒーロー。
 * 最も切迫した 1 件だけを、大きなカウントダウンとともに提示する。
 * 色分けはせず、文字の大きさと配置だけで注目させる。
 */
@Composable
fun HeroCard(
    item: HomeItem,
    onOpen: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val headline = when (item.urgency) {
        Urgency.OVERDUE -> "期限を過ぎています"
        Urgency.PAST_START -> "今すぐ着手"
        Urgency.HAS_TIME -> "この時間までに開始"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = headline,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            CountdownDisplay(
                urgency = item.urgency,
                criticalStartMillis = item.criticalStartMillis,
                deadlineMillis = item.assignment.deadlineEpochMillis,
                valueFontSize = CountdownLarge,
                labelStyle = MaterialTheme.typography.labelLarge,
            )

            Text(
                text = item.assignment.title,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = supportingLine(item),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = onComplete, modifier = Modifier.weight(1f)) {
                    Text("完了にする")
                }
                OutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f)) {
                    Text("編集")
                }
            }
        }
    }
}
