package app.submissionultra.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.submissionultra.readiness.ReadinessChecker
import app.submissionultra.readiness.ReadinessItem
import app.submissionultra.readiness.ReadinessReport
import app.submissionultra.ui.AppMessenger
import app.submissionultra.ui.components.BackTopBar
import app.submissionultra.ui.theme.okColor

/**
 * 緊急通知の状態。権限は信頼せず、この画面を開くたびに確認し直す。
 * 不足があれば「設定」から該当のシステム設定へ直接飛べる。
 */
@Composable
fun ReadinessScreen(
    readiness: ReadinessReport,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    messenger: AppMessenger,
) {
    val context = LocalContext.current
    val ok = okColor()
    val alert = MaterialTheme.colorScheme.error

    // 開いた瞬間に必ず再確認する。
    LaunchedEffect(Unit) { onRefresh() }

    Scaffold(
        topBar = { BackTopBar("緊急通知の状態", onBack) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Verdict(canFire = readiness.canFireEmergency, okColor = ok, alertColor = alert)

            readiness.items.forEach { item ->
                ReadinessRow(
                    item = item,
                    okColor = ok,
                    alertColor = alert,
                    onFix = { context.startActivity(ReadinessChecker.settingsIntent(context, item.key)) },
                )
            }

            OutlinedButton(
                onClick = {
                    onRefresh()
                    messenger.show("緊急通知の状態を更新しました")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("状態を更新")
            }
        }
    }
}

@Composable
private fun Verdict(canFire: Boolean, okColor: Color, alertColor: Color) {
    val container = if (canFire) okColor.copy(alpha = 0.12f) else alertColor.copy(alpha = 0.12f)
    val accent = if (canFire) okColor else alertColor
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            if (canFire) "この端末で緊急通知は鳴ります" else "緊急通知はまだ鳴りません",
            style = MaterialTheme.typography.titleSmall,
            color = accent,
        )
        Text(
            if (canFire) {
                "テスト通知で実際の鳴り方を確認できます。"
            } else {
                "下の赤い項目を「設定」から順に許可してください。"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ReadinessRow(
    item: ReadinessItem,
    okColor: Color,
    alertColor: Color,
    onFix: () -> Unit,
) {
    // ドットの色: 満たしていれば緑、必須で未達なら赤、推奨で未達なら控えめ。
    val dotColor = when {
        item.satisfied -> okColor
        item.required -> alertColor
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
                .align(Alignment.Top),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(item.label, style = MaterialTheme.typography.bodyLarge)
            if (!item.satisfied) {
                Text(
                    item.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (item.satisfied) {
            Text("OK", style = MaterialTheme.typography.labelLarge, color = okColor)
        } else {
            OutlinedButton(onClick = onFix) { Text("設定") }
        }
    }
}
