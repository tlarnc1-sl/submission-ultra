package app.submissionultra.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.submissionultra.domain.Urgency
import app.submissionultra.ui.home.HomeItem
import app.submissionultra.ui.theme.Emphasis
import app.submissionultra.ui.theme.Space
import app.submissionultra.ui.theme.SurfaceLevel
import app.submissionultra.ui.theme.border
import app.submissionultra.ui.theme.color
import app.submissionultra.ui.theme.radius
import app.submissionultra.ui.theme.shadow

/**
 * 「今この瞬間、何に着手すべきか」を一目で示すホーム上部のヒーロー。
 * 最も切迫した 1 件だけを、大きなカウントダウンとともに提示する。
 * 色分けはせず、文字の大きさと配置だけで注目させる。
 *
 * @param emphasized この画面の主役として出すか。false のときは通常のカードまで落ちる。
 *   緊急通知が成立していないときは、その解消の方が先なので主役を譲る（DESIGN.md 参照）。
 */
@Composable
fun HeroCard(
    item: HomeItem,
    onOpen: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = true,
) {
    val headline = when (item.urgency) {
        Urgency.OVERDUE -> "期限を過ぎています"
        Urgency.PAST_START -> "今すぐ着手"
        Urgency.HAS_TIME -> "この時間までに開始"
    }

    val level = if (emphasized) SurfaceLevel.Lead else SurfaceLevel.Raised

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(level.radius),
        color = level.color(),
        shadowElevation = level.shadow,
        border = level.border(),
    ) {
        Column(
            // 主役のときだけ内側を広く取る。余白も階層の一部として働く。
            modifier = Modifier.padding(if (emphasized) Space.xl else Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md),
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
                valueFontSize = if (emphasized) Emphasis.Lead else Emphasis.Strong,
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

            // 幅を等分しない。このカードでさせたいのは「完了にする」で、編集はその副次。
            // 同じ大きさで並べると、どちらが本筋か読み取れなくなる。
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = onComplete, modifier = Modifier.weight(1f)) {
                    Text("完了にする")
                }
                TextButton(onClick = onOpen) {
                    Text("編集")
                }
            }
        }
    }
}
