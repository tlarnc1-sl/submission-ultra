package app.submissionultra.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.submissionultra.appGraph
import app.submissionultra.data.ReminderConfig
import app.submissionultra.data.ReminderMode
import app.submissionultra.ui.AppMessenger
import app.submissionultra.ui.components.BackTopBar

/** 時間の設定：余裕時間（緊急通知の基準）とリマインダーの通知時刻。 */
@Composable
fun TimeSettingsScreen(
    onBack: () -> Unit,
    messenger: AppMessenger,
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

    Scaffold(
        topBar = { BackTopBar("時間の設定", onBack) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            MarginSection(
                currentMargin = margin,
                onSave = { minutes ->
                    viewModel.setMargin(minutes)
                    messenger.show("余裕時間を ${minutes} 分に保存しました")
                },
            )

            HorizontalDivider()

            ReminderSection(
                config = reminderConfig,
                onModeChange = { mode ->
                    viewModel.setReminderMode(mode)
                    messenger.show(
                        when (mode) {
                            ReminderMode.FIXED -> "決まった時刻で通知します"
                            ReminderMode.DEADLINE -> "締切と同じ時刻で通知します"
                        },
                    )
                },
                onTimeChange = { hour, minute ->
                    viewModel.setReminderTime(hour, minute)
                    messenger.show("通知時刻を " + String.format("%02d:%02d", hour, minute) + " に設定しました")
                },
            )
        }
    }
}

@Composable
private fun MarginSection(currentMargin: Int?, onSave: (Int) -> Unit) {
    var text by remember(currentMargin) { mutableStateOf(currentMargin?.toString() ?: "") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("余裕時間（全課題共通）", style = MaterialTheme.typography.titleMedium)
        Text(
            "最後の開始時刻 = 期限 −（作業時間 ＋ 余裕時間）。全ての課題に共通で適用されます。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() }.take(4) },
                label = { Text("分") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { text.toIntOrNull()?.let(onSave) },
                enabled = text.toIntOrNull() != null,
            ) {
                Text("保存")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderSection(
    config: ReminderConfig?,
    onModeChange: (ReminderMode) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("リマインダー通知", style = MaterialTheme.typography.titleMedium)
        Text(
            "締切の3日前から締切当日まで、未完了の提出物を毎日通知します。" +
                "緊急通知とは別で、マナーモード・おやすみモードを尊重します。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (config != null) {
            Text("通知時刻の決め方", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = config.mode == ReminderMode.FIXED,
                    onClick = { onModeChange(ReminderMode.FIXED) },
                    label = { Text("決まった時刻") },
                )
                FilterChip(
                    selected = config.mode == ReminderMode.DEADLINE,
                    onClick = { onModeChange(ReminderMode.DEADLINE) },
                    label = { Text("締切と同じ時刻") },
                )
            }
            if (config.mode == ReminderMode.FIXED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "通知時刻 " + String.format("%02d:%02d", config.hour, config.minute),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    OutlinedButton(onClick = { showTimePicker = true }) { Text("時刻を選ぶ") }
                }
            } else {
                Text(
                    "各リマインダーは締切と同じ時刻に鳴ります。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showTimePicker && config != null) {
        val timeState = rememberTimePickerState(
            initialHour = config.hour,
            initialMinute = config.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onTimeChange(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text("決定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
            text = { TimePicker(state = timeState) },
        )
    }
}
