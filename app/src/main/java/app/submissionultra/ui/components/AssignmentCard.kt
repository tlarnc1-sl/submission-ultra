package app.submissionultra.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.submissionultra.data.AssignmentType
import app.submissionultra.data.deadlineLabel
import app.submissionultra.ui.formatDateTime
import app.submissionultra.ui.home.HomeItem

/**
 * ホームの提出物カード。3行に絞って走査しやすくする。
 * 1行目=課題名/種類、2行目=カウントダウン(主役)、3行目=補助情報。
 * 緊急度による色分けはせず、文字の大きさ・太さだけで優先度を表す。
 * 完了操作はカードのスワイプに委ねるため、ここにはボタンを置かない。
 */
@Composable
fun AssignmentCard(
    item: HomeItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeLabel = when (item.assignment.type) {
        AssignmentType.CLASSROOM -> "Classroom"
        AssignmentType.PAPER -> "紙"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = item.assignment.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            CountdownDisplay(
                urgency = item.urgency,
                criticalStartMillis = item.criticalStartMillis,
                deadlineMillis = item.assignment.deadlineEpochMillis,
                valueFontSize = CountdownMedium,
            )

            Text(
                text = supportingLine(item),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 補助情報を1行に統合する（先生 ・ 期限）。 */
internal fun supportingLine(item: HomeItem): String {
    val deadline = "${item.assignment.type.deadlineLabel()} ${formatDateTime(item.assignment.deadlineEpochMillis)}"
    val teacher = item.assignment.teacherName
    return if (teacher.isNullOrBlank()) deadline else "${teacher}先生 ・ $deadline"
}
