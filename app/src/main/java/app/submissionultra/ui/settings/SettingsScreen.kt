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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.submissionultra.appGraph
import app.submissionultra.data.ReminderMode
import app.submissionultra.readiness.ReadinessReport
import app.submissionultra.ui.theme.okColor

/**
 * 設定の目次。各行に現在の値をサマリ表示して、開かなくても状態が分かるようにする。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    readiness: ReadinessReport,
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
    val statusColor = if (statusOk) okColor() else MaterialTheme.colorScheme.error

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
            SettingsRow(
                title = "時間の設定",
                summary = timeSummary,
                onClick = onOpenTime,
            )
            HorizontalDivider()
            SettingsRow(
                title = "通知のテスト",
                summary = "緊急通知・リマインダーを実際に鳴らして確かめる",
                onClick = onOpenTest,
            )
            HorizontalDivider()
            SettingsRow(
                title = "緊急通知の状態",
                summary = if (statusOk) "鳴ります" else "鳴りません（設定が不足）",
                summaryColor = statusColor,
                onClick = onOpenStatus,
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

@Composable
private fun SettingsRow(
    title: String,
    summary: String,
    onClick: () -> Unit,
    summaryColor: Color? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            summary,
            style = MaterialTheme.typography.bodyMedium,
            color = summaryColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

