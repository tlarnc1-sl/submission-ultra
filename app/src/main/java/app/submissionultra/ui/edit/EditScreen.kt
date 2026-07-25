package app.submissionultra.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.material3.AlertDialog
import app.submissionultra.appGraph
import app.submissionultra.data.AssignmentType
import app.submissionultra.data.deadlineLabel
import app.submissionultra.ui.AppMessenger
import app.submissionultra.ui.components.BackTopBar
import app.submissionultra.ui.formatDateTime
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(
    assignmentId: Long,
    onDone: () -> Unit,
    messenger: AppMessenger,
) {
    val context = LocalContext.current
    val viewModel: EditViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                EditViewModel(
                    context.appGraph.assignmentRepository,
                    context.appGraph.settingsRepository,
                    assignmentId,
                )
            }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BackTopBar(
                title = if (state.isEditing) "提出物を編集" else "提出物を追加",
                onBack = onDone,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("提出物の名前") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("種類", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.type == AssignmentType.CLASSROOM,
                        onClick = { viewModel.onTypeChange(AssignmentType.CLASSROOM) },
                        label = { Text("Classroom") },
                    )
                    FilterChip(
                        selected = state.type == AssignmentType.PAPER,
                        onClick = { viewModel.onTypeChange(AssignmentType.PAPER) },
                        label = { Text("紙") },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.type.deadlineLabel(), style = MaterialTheme.typography.labelLarge)
                Text(formatDateTime(state.deadlineMillis), style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDatePicker = true }) { Text("日付を選ぶ") }
                    OutlinedButton(onClick = { showTimePicker = true }) { Text("時刻を選ぶ") }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.effortText,
                    onValueChange = viewModel::onEffortChange,
                    label = { Text("本気でやれば終わる作業時間（分）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 45, 60, 90).forEach { minutes ->
                        FilterChip(
                            selected = state.effortMinutes == minutes,
                            onClick = { viewModel.onEffortChange(minutes.toString()) },
                            label = { Text("${minutes}分") },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.teacherName,
                onValueChange = viewModel::onTeacherChange,
                label = { Text("管理をする先生（任意）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // 入力に応じて「最後の開始時刻」を即時プレビュー（このアプリの核）。
            CriticalStartPreview(previewMillis = state.criticalStartPreviewMillis)

            Button(
                onClick = {
                    val creating = !state.isEditing
                    viewModel.save {
                        messenger.show(if (creating) "追加しました" else "保存しました")
                        onDone()
                    }
                },
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("保存")
            }

            if (state.isEditing) {
                OutlinedButton(
                    onClick = {
                        viewModel.delete {
                            messenger.show("削除しました")
                            onDone()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("削除")
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.deadlineMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { picked ->
                        viewModel.onDeadlineChange(mergeDate(picked, state.deadlineMillis))
                    }
                    showDatePicker = false
                }) { Text("決定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        val current = Instant.ofEpochMilli(state.deadlineMillis).atZone(ZoneId.systemDefault())
        val timeState = rememberTimePickerState(
            initialHour = current.hour,
            initialMinute = current.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDeadlineChange(mergeTime(state.deadlineMillis, timeState.hour, timeState.minute))
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

@Composable
private fun CriticalStartPreview(previewMillis: Long?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                "最後の開始時刻",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (previewMillis != null) {
                Text(formatDateTime(previewMillis), style = MaterialTheme.typography.titleMedium)
                Text(
                    "この時刻を過ぎても未完了なら緊急通知が出ます",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "作業時間を入力すると表示されます",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** DatePicker が返す UTC 基準の日付と、既存の時刻（端末TZ）を合成する。 */
private fun mergeDate(pickedUtcMillis: Long, existingMillis: Long): Long {
    val pickedDate = Instant.ofEpochMilli(pickedUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    val existingTime = Instant.ofEpochMilli(existingMillis).atZone(ZoneId.systemDefault()).toLocalTime()
    return pickedDate.atTime(existingTime)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

/** 既存の日付（端末TZ）と、選ばれた時刻を合成する。 */
private fun mergeTime(existingMillis: Long, hour: Int, minute: Int): Long {
    val existingDate = Instant.ofEpochMilli(existingMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return existingDate.atTime(LocalTime.of(hour, minute))
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}
