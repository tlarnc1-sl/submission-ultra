package app.submissionultra.ui.components

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
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import app.submissionultra.data.AssignmentType
import app.submissionultra.data.deadlineLabel
import app.submissionultra.ui.formatDateTime
import app.submissionultra.ui.home.HomeItem
import app.submissionultra.ui.theme.Emphasis
import app.submissionultra.ui.theme.Space
import app.submissionultra.ui.theme.SurfaceLevel
import app.submissionultra.ui.theme.border
import app.submissionultra.ui.theme.color
import app.submissionultra.ui.theme.radius
import app.submissionultra.ui.theme.shadow

/**
 * ホームの提出物カード。3行に絞って走査しやすくする。
 * 1行目=課題名/種類、2行目=カウントダウン(主役)、3行目=補助情報。
 * 緊急度による色分けはせず、文字の大きさ・太さだけで優先度を表す。
 *
 * 完了操作はカードのスワイプに委ねるため、ここにはボタンを置かない。
 * ただしスワイプは指でしか届かないので、[onComplete] を渡すと同じ操作を
 * 読み上げ用の動作としても公開する。
 */
@Composable
fun AssignmentCard(
    item: HomeItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onComplete: (() -> Unit)? = null,
) {
    val typeLabel = when (item.assignment.type) {
        AssignmentType.CLASSROOM -> "Classroom"
        AssignmentType.PAPER -> "紙"
    }

    val level = SurfaceLevel.Raised

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(level.radius),
        color = level.color(),
        shadowElevation = level.shadow,
        border = level.border(),
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onClick)
                .completeAction(onComplete)
                .padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.sm),
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
                valueFontSize = Emphasis.Strong,
            )

            Text(
                text = supportingLine(item),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 完了を、指のスワイプ以外からも届く操作として公開する。
 *
 * スワイプだけに頼ると、読み上げを使う人と、細かい動きが難しい人はこのカードを完了にできない。
 * タップ（編集）と同じノードに載せることで、読み上げの「操作」一覧にそのまま現れる。
 */
private fun Modifier.completeAction(onComplete: (() -> Unit)?): Modifier =
    if (onComplete == null) {
        this
    } else {
        semantics {
            customActions = listOf(
                CustomAccessibilityAction(label = "完了にする") {
                    onComplete()
                    true
                },
            )
        }
    }

/** 補助情報を1行に統合する（先生 ・ 期限）。 */
internal fun supportingLine(item: HomeItem): String {
    val deadline = "${item.assignment.type.deadlineLabel()} ${formatDateTime(item.assignment.deadlineEpochMillis)}"
    val teacher = item.assignment.teacherName
    return if (teacher.isNullOrBlank()) deadline else "${teacher}先生 ・ $deadline"
}
