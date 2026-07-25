package app.submissionultra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.submissionultra.readiness.ReadinessReport

/**
 * 緊急通知が成立しない、または推奨設定が不足しているときにホーム上部へ常時表示する警告。
 * 隠せない・無視できない位置に置く。タップで設定画面（詳細と導線）へ移動する。
 */
@Composable
fun ReadinessBanner(
    report: ReadinessReport,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (report.unmet.isEmpty()) return

    val blocking = !report.canFireEmergency
    val headline = if (blocking) {
        "緊急通知できません"
    } else {
        "推奨設定が不足しています"
    }
    val body = if (blocking) {
        "この端末では今、緊急通知が成立しません。タップして不足している設定を確認してください。"
    } else {
        "バッテリー最適化により通知が遅れる可能性があります。タップして確認してください。"
    }

    val container = if (blocking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant
    val onContainer = if (blocking) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (blocking) container else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (blocking) container else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .clickable(onClick = onOpenSettings)
            .padding(16.dp),
    ) {
        Text(
            text = headline,
            style = MaterialTheme.typography.titleMedium,
            color = if (blocking) onContainer else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = if (blocking) onContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
