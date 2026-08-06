package app.submissionultra.ui.completed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.submissionultra.appGraph
import app.submissionultra.ui.AppMessenger
import app.submissionultra.ui.formatDateTime
import app.submissionultra.ui.formatDuration
import app.submissionultra.ui.theme.Emphasis
import app.submissionultra.ui.theme.EmphasisScope
import app.submissionultra.ui.theme.ScreenLead
import app.submissionultra.ui.theme.Space
import app.submissionultra.ui.theme.SurfaceLevel
import app.submissionultra.ui.theme.border
import app.submissionultra.ui.theme.color
import app.submissionultra.ui.theme.radius
import app.submissionultra.ui.theme.shadow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedScreen(messenger: AppMessenger) {
    val context = LocalContext.current
    val viewModel: CompletedViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                CompletedViewModel(
                    context.appGraph.assignmentRepository,
                    context.appGraph.settingsRepository,
                )
            }
        },
    )
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    // 開くたびに集計し直す。
    LaunchedEffect(Unit) { viewModel.refresh() }

    var expanded by remember { mutableStateOf(false) }

    EmphasisScope("完了済み") {
        Scaffold(
            topBar = { TopAppBar(title = { Text("完了済み") }) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = Space.xl),
            ) {
                item(key = "stats") {
                    ScreenLead {
                        StatsPanel(stats = stats, modifier = Modifier.padding(Space.lg))
                    }
                }

                item(key = "toggle") {
                    ExpandHeader(
                        title = "完了済みの課題",
                        count = stats.totalCount,
                        expanded = expanded,
                        onToggle = { expanded = !expanded },
                    )
                }

                if (expanded) {
                    if (rows.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                "まだ完了した提出物はありません",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(Space.lg),
                            )
                        }
                    } else {
                        item(key = "table-header") { TableHeader() }
                        items(rows, key = { it.assignment.id }) { row ->
                            TableRow(
                                row = row,
                                onRestore = {
                                    viewModel.restore(row.assignment)
                                    messenger.show("「${row.assignment.title}」を未完了に戻しました")
                                },
                                onDelete = {
                                    viewModel.delete(row.assignment)
                                    messenger.show("「${row.assignment.title}」を履歴から削除しました")
                                },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

/**
 * この画面の主役：開始リミットに対してどれだけ余裕を持って終えられているか。
 *
 * チュートリアルの見本にもこれをそのまま使う（`OnboardingScreen`）。
 * 見本を手で複製すると本番とずれていくので、必ず本物を呼ぶ。そのための internal。
 */
@Composable
internal fun StatsPanel(stats: CompletedStats, modifier: Modifier = Modifier) {
    val average = stats.averageLeadMillis
    val level = SurfaceLevel.Lead

    Surface(
        modifier = modifier.fillMaxWidth(),
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
                "開始リミットより平均で",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (average == null) {
                Text(
                    "まだ記録がありません",
                    fontSize = Emphasis.Strong,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "提出物を完了にすると、ここに成績が出ます",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        formatDuration(average),
                        fontSize = Emphasis.Lead,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        if (average >= 0L) " 早く完了" else " 遅れて完了",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = Space.xs),
                    )
                }
                Text(
                    "対象 ${stats.ratedCount}件 ・ うち開始リミット前に完了 ${stats.beforeLimitCount}件",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExpandHeader(
    title: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = Space.lg, vertical = Space.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$title（${count}件）", style = MaterialTheme.typography.titleSmall)
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "閉じる" else "開く",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = Space.lg, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderCell("提出物", Modifier.weight(1f))
        HeaderCell("完了", Modifier.weight(0.8f))
        HeaderCell("リミット差", Modifier.weight(0.7f))
        // 各行の操作ボタン（IconButton）と列を揃えるための余白。
        Spacer(Modifier.width(48.dp))
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun TableRow(
    row: CompletedRow,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val lead = row.leadMillis

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Space.lg, top = Space.md, bottom = Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = row.assignment.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = row.assignment.completedAtEpochMillis?.let { formatDateTime(it) } ?: "-",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.8f),
        )
        Text(
            text = when {
                lead == null -> "-"
                lead >= 0L -> "${formatDuration(lead)}前"
                else -> "${formatDuration(lead)}後"
            },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.7f),
        )

        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "この提出物の操作",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("未完了に戻す") },
                    onClick = {
                        menuOpen = false
                        onRestore()
                    },
                )
                DropdownMenuItem(
                    text = { Text("履歴から削除") },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    },
                )
            }
        }
    }
}
