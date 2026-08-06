package app.submissionultra.ui.edit

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.material3.AlertDialog
import app.submissionultra.appGraph
import app.submissionultra.calendar.CalendarImport
import app.submissionultra.data.AssignmentType
import app.submissionultra.data.deadlineLabel
import app.submissionultra.ui.AppMessenger
import app.submissionultra.ui.components.BackTopBar
import app.submissionultra.ui.formatDateTime
import app.submissionultra.ui.formatDayLabel
import app.submissionultra.ui.formatTime
import app.submissionultra.ui.theme.Emphasis
import app.submissionultra.ui.theme.EmphasisScope
import app.submissionultra.ui.theme.ScreenLead
import app.submissionultra.ui.theme.Space
import app.submissionultra.ui.theme.SurfaceLevel
import app.submissionultra.ui.theme.border
import app.submissionultra.ui.theme.color
import app.submissionultra.ui.theme.radius
import app.submissionultra.ui.theme.shadow
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
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCalendarPicker by remember { mutableStateOf(false) }

    // 許可を求めるのは「取り込む」を押したときだけ。起動時にまとめて聞かない。
    // 使わない人はカレンダーを一度も読まれずに済む。
    val calendarPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            showCalendarPicker = true
        } else {
            messenger.show("カレンダーを読む許可がないと取り込めません")
        }
    }

    val screenTitle = if (state.isEditing) "提出物を編集" else "提出物を追加"

    EmphasisScope(screenTitle) {
        Scaffold(
            topBar = {
                BackTopBar(
                    title = screenTitle,
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
                    .padding(Space.lg),
                verticalArrangement = Arrangement.spacedBy(Space.xl),
            ) {
                // 新規のときだけ出す。編集中に上書きされると、直していた内容が消える。
                if (!state.isEditing) {
                    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        OutlinedButton(
                            onClick = {
                                if (CalendarImport.isGranted(context)) {
                                    showCalendarPicker = true
                                } else {
                                    calendarPermission.launch(Manifest.permission.READ_CALENDAR)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("カレンダーから取り込む")
                        }
                        Text(
                            "Classroom の課題は、期限が設定されていればカレンダーアプリに出ます。" +
                                "名前と期限だけ写すので、作業時間はご自身で入れてください。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text("提出物の名前") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Text("種類", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
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

                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Text(state.type.deadlineLabel(), style = MaterialTheme.typography.labelLarge)
                    Text(formatDateTime(state.deadlineMillis), style = MaterialTheme.typography.bodyLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        OutlinedButton(onClick = { showDatePicker = true }) { Text("日付を選ぶ") }
                        OutlinedButton(onClick = { showTimePicker = true }) { Text("時刻を選ぶ") }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    OutlinedTextField(
                        value = state.effortText,
                        onValueChange = viewModel::onEffortChange,
                        label = { Text("本気でやれば終わる作業時間（分）") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
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
                // この画面の主役はここ。保存ボタンは、その時刻を確定させる操作でしかない。
                ScreenLead {
                    CriticalStartPreview(previewMillis = state.criticalStartPreviewMillis)
                }

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
                    // 取り消せない操作なので、幅も色も主要アクションから明確に落とす。
                    // 危険の周知はボタンの見た目ではなく、確認ダイアログが担う。
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text("削除")
                    }
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

    if (showCalendarPicker) {
        CalendarPickerDialog(
            onPick = { item ->
                viewModel.onTitleChange(item.title)
                viewModel.onDeadlineChange(item.deadlineMillis)
                showCalendarPicker = false
                messenger.show(
                    if (item.timeKnown) {
                        "「${item.title}」を写しました。作業時間を入れてください"
                    } else {
                        "「${item.title}」を写しました。終日の予定なので締切を 23:59 にしています"
                    },
                )
            },
            onDismiss = { showCalendarPicker = false },
        )
    }

    // 削除は取り消せない。完了のように Snackbar で戻すことができないので、
    // 実行する前に一度止めて、何が消えるのかを名前で示す。
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("削除しますか") },
            text = {
                Text("「${state.title.ifBlank { "この提出物" }}」を削除します。元に戻せません。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.delete {
                            messenger.show("削除しました")
                            onDone()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("削除する")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("やめる") }
            },
        )
    }
}

/**
 * この画面の主役。入力に応じて「最後の開始時刻」を即時に見せる。
 *
 * 日付と時刻を分けて組み、時刻だけを大きな数字にする。ここで伝えたいのは
 * 「何時までに机に向かうか」であって、日付はその文脈でしかない。
 * まとめて一行にすると全角14文字になり、大きな文字では画面に収まらない。
 */
@Composable
private fun CriticalStartPreview(previewMillis: Long?) {
    val level = SurfaceLevel.Lead

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(level.radius),
        color = level.color(),
        shadowElevation = level.shadow,
        border = level.border(),
    ) {
        Column(
            modifier = Modifier.padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            Text(
                "最後の開始時刻",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (previewMillis != null) {
                Text(
                    formatDayLabel(previewMillis),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatTime(previewMillis),
                    fontSize = Emphasis.Lead,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
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
