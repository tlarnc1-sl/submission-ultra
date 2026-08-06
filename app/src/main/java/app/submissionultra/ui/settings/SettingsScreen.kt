package app.submissionultra.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.submissionultra.appGraph
import app.submissionultra.data.ReminderMode
import app.submissionultra.readiness.ReadinessReport
import app.submissionultra.ui.theme.EmphasisScope
import app.submissionultra.ui.theme.ScreenLead
import app.submissionultra.ui.theme.Space
import app.submissionultra.ui.theme.SurfaceLevel
import app.submissionultra.ui.theme.border
import app.submissionultra.ui.theme.color
import app.submissionultra.ui.theme.radius
import app.submissionultra.ui.theme.okColor

/**
 * 設定の目次。各行に現在の値をサマリ表示して、開かなくても状態が分かるようにする。
 *
 * 「通知」と「このアプリ」の 2 群に分ける。平らに 5 行並べると、いちばん見てほしい
 * 「緊急通知の状態」が他と同じ重さになってしまうため、群の先頭に置いて面を与える。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    readiness: ReadinessReport,
    onOpenGuide: () -> Unit,
    onOpenTime: () -> Unit,
    onOpenTest: () -> Unit,
    onOpenStatus: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SettingsViewModel(context.appGraph.settingsRepository, context.appGraph.assignmentRepository)
            }
        },
    )
    val margin by viewModel.marginMinutes.collectAsStateWithLifecycle()
    val reminderConfig by viewModel.reminderConfig.collectAsStateWithLifecycle()
    val appInfo = rememberAppInfo()

    val timeSummary = buildString {
        append("余裕時間 ${margin ?: "-"}分")
        reminderConfig?.let { config ->
            append(" ・ リマインダー ")
            append(
                when (config.mode) {
                    ReminderMode.FIXED -> String.format("%02d:%02d", config.hour, config.minute)
                    ReminderMode.DEADLINE -> "締切と同時刻"
                },
            )
        }
    }

    val statusOk = readiness.canFireEmergency

    EmphasisScope("設定") {
        Scaffold(
            topBar = { TopAppBar(title = { Text("設定") }) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            ) {
                // 「鳴るのか」→「鳴らしてみる」→「いつ鳴らすか」の順に並べる。
                // 時間の設定は余裕時間とリマインダー時刻、つまり「いつ鳴るか」を決める設定なので、
                // 見た目の分類ではなく役割でここに属する。
                SectionHeader("通知")

                ScreenLead {
                    EmergencyStatusCard(ok = statusOk, onClick = onOpenStatus)
                }

                SettingsRow(
                    title = "通知のテスト",
                    summary = "緊急通知・リマインダーを実際に鳴らして確かめる",
                    onClick = onOpenTest,
                )
                HorizontalDivider()
                SettingsRow(
                    title = "時間の設定",
                    summary = timeSummary,
                    onClick = onOpenTime,
                )

                SectionHeader("このアプリ")

                SettingsRow(
                    title = "使い方",
                    summary = "追加・完了・削除のしかた",
                    onClick = onOpenGuide,
                )
                HorizontalDivider()
                SettingsRow(
                    title = "このアプリについて",
                    summary = appInfo?.let { "バージョン ${it.versionName}" } ?: "アプリの情報",
                    onClick = onOpenAbout,
                )
            }
        }
    }
}

/**
 * 緊急通知が鳴るかどうか。この画面でいちばん見てほしいのはここ。
 *
 * 色は状態を補強するために使う。色が読めなくても「鳴ります」「鳴りません」の
 * 文言そのものが結論なので、色だけが手がかりになることはない。
 */
@Composable
private fun EmergencyStatusCard(ok: Boolean, onClick: () -> Unit) {
    val level = SurfaceLevel.Raised

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            // 左右は行と同じ位置に揃え、上下は見出しと次の行から少しだけ離す。
            .padding(horizontal = Space.lg, vertical = Space.sm),
        shape = RoundedCornerShape(level.radius),
        color = level.color(),
        border = level.border(),
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            Text(
                "緊急通知の状態",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (ok) "鳴ります" else "鳴りません",
                style = MaterialTheme.typography.titleLarge,
                color = if (ok) okColor() else MaterialTheme.colorScheme.error,
            )
            Text(
                if (ok) {
                    "権限は足りています。タップすると内訳とテストへの導線が見られます。"
                } else {
                    "設定が不足しています。タップして不足している項目を直してください。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 群の見出し。行そのものより小さく、かつ地の文と混ざらない色にする。
 * 見出しは押せないので、押せる行と同じ大きさにしない。
 */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = Space.lg,
            end = Space.lg,
            top = Space.xl,
            bottom = Space.sm,
        ),
    )
}

@Composable
private fun SettingsRow(
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(Space.lg),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

