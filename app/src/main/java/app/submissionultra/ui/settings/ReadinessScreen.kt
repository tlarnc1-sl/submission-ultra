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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.submissionultra.readiness.OemPowerSettings
import app.submissionultra.readiness.ReadinessChecker
import app.submissionultra.readiness.ReadinessItem
import app.submissionultra.readiness.ReadinessReport
import app.submissionultra.ui.AppMessenger
import app.submissionultra.ui.components.BackTopBar
import app.submissionultra.ui.theme.EmphasisScope
import app.submissionultra.ui.theme.Radius
import app.submissionultra.ui.theme.ScreenLead
import app.submissionultra.ui.theme.Space
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

    EmphasisScope("緊急通知の状態") {
        Scaffold(
            topBar = { BackTopBar("緊急通知の状態", onBack) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(Space.lg),
                verticalArrangement = Arrangement.spacedBy(Space.md),
            ) {
                // この画面の主役は「鳴るのか、鳴らないのか」の一言。
                ScreenLead {
                    Verdict(canFire = readiness.canFireEmergency, okColor = ok, alertColor = alert)
                }

                readiness.items.forEach { item ->
                    ReadinessRow(
                        item = item,
                        okColor = ok,
                        alertColor = alert,
                        onFix = {
                            val opened = runCatching {
                                context.startActivity(ReadinessChecker.settingsIntent(context, item.key))
                            }.isSuccess
                            if (!opened) {
                                messenger.show("設定画面を開けませんでした。端末の設定から操作してください")
                            }
                        },
                    )
                }

                DeviceOsSection(
                    alertColor = alert,
                    onOpenAutoStart = {
                        // ROM 独自の設定画面は非公開のため、開けないことがある。
                        // その場合は黙って何も起きないのではなく、手動での操作を促す。
                        val opened = runCatching {
                            context.startActivity(OemPowerSettings.autoStartIntent(context))
                        }.isSuccess
                        if (!opened) {
                            messenger.show("設定画面を開けませんでした。端末の設定から操作してください")
                        }
                    },
                )

                // この画面でさせたいのは不足項目を「設定」から直すこと。再確認はその副次。
                // 全幅のボタンにすると、いちばん大きい操作が「更新」になってしまう。
                TextButton(
                    onClick = {
                        onRefresh()
                        messenger.show("緊急通知の状態を更新しました")
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("状態を更新")
                }
            }
        }
    }
}

/**
 * この端末の OS。どの OS でも必ず名前を出す。
 *
 * 独自の省電力機能を持つ OS（HyperOS・MIUI・ColorOS）では、上の項目がすべて OK でも
 * 通知が遅れうる。許可されたかをアプリから確認する手段が無い以上、
 * 「解決済み」にできる項目として扱わず、消えない注意としてここに常に出しておく。
 */
@Composable
private fun DeviceOsSection(alertColor: Color, onOpenAutoStart: () -> Unit) {
    val restrictive = OemPowerSettings.current != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .background(
                if (restrictive) alertColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
            )
            .padding(Space.lg),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Text(
            "この端末の OS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(OemPowerSettings.osName, style = MaterialTheme.typography.titleSmall)
        Text(
            OemPowerSettings.osDetail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (restrictive) {
            Text(
                "${OemPowerSettings.osName} は、Android 標準の設定とは別に、独自の省電力機能で" +
                    "バックグラウンドのアプリを止めます。上の項目がすべて OK でも、" +
                    "画面を消している間は通知が遅れることがあります。" +
                    "端末の設定でこのアプリの「自動起動」を許可してください。" +
                    "許可されているかどうかは、アプリからは確認できません。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = Space.sm),
            )
            OutlinedButton(
                onClick = onOpenAutoStart,
                modifier = Modifier.padding(top = Space.xs),
            ) {
                Text("自動起動の設定を開く")
            }
        }
    }
}

/**
 * この画面の主役。ここに数字は無いので、大きさではなく面と位置で主役だと示す。
 * それでも画面内でいちばん大きい文字にはしておく。
 */
@Composable
private fun Verdict(canFire: Boolean, okColor: Color, alertColor: Color) {
    val container = if (canFire) okColor.copy(alpha = 0.12f) else alertColor.copy(alpha = 0.12f)
    val accent = if (canFire) okColor else alertColor
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .background(container)
            .padding(Space.lg),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Text(
            if (canFire) "この端末で緊急通知は鳴ります" else "緊急通知はまだ鳴りません",
            style = MaterialTheme.typography.titleLarge,
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
        horizontalArrangement = Arrangement.spacedBy(Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(top = Space.xs)
                .size(DotSize)
                .clip(CircleShape)
                .background(dotColor)
                .align(Alignment.Top),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(item.label, style = MaterialTheme.typography.bodyLarge)
            if (!item.satisfied) {
                // 必須と推奨の違いをドットの色だけに委ねない。色が読めなくても、
                // これを直さないと鳴らないのかどうかが分かるようにする。
                if (item.required) {
                    Text(
                        "必須",
                        style = MaterialTheme.typography.labelMedium,
                        color = alertColor,
                    )
                }
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
            // 直させたいのはここ。未達の行にだけ出るので、塗りボタンでも過剰にはならない。
            Button(onClick = onFix) { Text("設定") }
        }
    }
}

/** 状態を表すドットの直径。間隔ではなく寸法なので Space には含めない。 */
private val DotSize = 10.dp
