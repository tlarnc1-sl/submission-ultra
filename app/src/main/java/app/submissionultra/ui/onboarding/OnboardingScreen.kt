package app.submissionultra.ui.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.submissionultra.data.Assignment
import app.submissionultra.data.AssignmentType
import app.submissionultra.data.SettingsRepository
import app.submissionultra.domain.Timing
import app.submissionultra.domain.Urgency
import app.submissionultra.ui.components.AssignmentCard
import app.submissionultra.ui.home.HomeItem
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 4

/**
 * 初回起動時のチュートリアル。設定の「使い方」からも同じものを開く。
 *
 * 教えるのは「見ただけでは気づけない操作」に絞る。カードの横スワイプ、削除が編集画面の下に
 * あること、完了済みの `…` メニュー、そしてこのアプリの根幹である最後の開始時刻の考え方。
 *
 * 見本は実物の [AssignmentCard] をそのまま使う。作り物の絵を置くと本番の画面とずれていくが、
 * 本物を置けば必ず一致する。
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    finishLabel: String = "はじめる",
    skipLabel: String = "スキップ",
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val sample = rememberSampleItem()

    // 背景は自分で塗る。他の画面は Scaffold が colorScheme.background を塗ってくれるが、
    // この画面は Scaffold を使わないため、塗らないと window テーマ
    // (android:Theme.Material.Light = 白固定) が透けて、ダークモードで白地に白文字になる。
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { page ->
            when (page) {
                0 -> CriticalStartPage()
                1 -> AddPage(sample)
                2 -> CompletePage(sample)
                else -> CompletedTabPage()
            }
        }

        val lastPage = pagerState.currentPage == PAGE_COUNT - 1
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            PageDots(currentPage = pagerState.currentPage)

            TextButton(
                onClick = onFinish,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Text(skipLabel)
            }

            TextButton(
                onClick = {
                    if (lastPage) {
                        onFinish()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Text(if (lastPage) finishLabel else "次へ")
            }
        }
    }
}

@Composable
private fun PageDots(currentPage: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(PAGE_COUNT) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    ),
            )
        }
    }
}

/**
 * 1ページの共通の形。見出し・見本・説明の順で、縦に中央寄せ。
 * 小さい画面でも切れないようスクロールできるようにしておく。
 */
@Composable
private fun OnboardingPage(
    title: String,
    body: String,
    note: String,
    visual: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )

        Box(modifier = Modifier.padding(vertical = 28.dp)) { visual() }

        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = note,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun CriticalStartPage() {
    OnboardingPage(
        title = "最後の開始時刻",
        body = "締切ではなく、これを過ぎたら間に合わない時刻を計算します。" +
            "過ぎても終わっていなければ、緊急通知が鳴ります。",
        note = "カウントダウンは、開始リミットを過ぎると「締切まで」に自動で切り替わります。",
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Text(
                text = "期限 −（作業時間 ＋ 余裕時間）",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun AddPage(sample: HomeItem) {
    OnboardingPage(
        title = "追加する・直す",
        body = "右下の「追加」から登録します。カードをタップすると編集できます。",
        note = "削除は編集画面のいちばん下にあります。",
    ) {
        AssignmentCard(item = sample, onClick = {})
    }
}

@Composable
private fun CompletePage(sample: HomeItem) {
    OnboardingPage(
        title = "完了にする",
        body = "カードを横にスワイプすると完了になります。",
        note = "間違えたときは、下に出る「取消」から戻せます。",
    ) {
        SwipeDemo(sample)
    }
}

@Composable
private fun CompletedTabPage() {
    OnboardingPage(
        title = "完了済みで振り返る",
        body = "完了した提出物は「完了済み」にたまります。" +
            "開始リミットにどれだけ余裕を持って終えられたかが記録されます。",
        note = "各行の「…」から、未完了に戻したり履歴から削除できます。",
    ) {
        StatsPreview()
    }
}

/**
 * カードが右へ滑り、左から完了のチェックが覗く様子を繰り返す。
 * 背景はホームのスワイプ時（HomeScreen の SwipeToComplete）と同じ見た目にそろえる。
 */
@Composable
private fun SwipeDemo(sample: HomeItem) {
    val transition = rememberInfiniteTransition(label = "swipe")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 72f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "offset",
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AssignmentCard(
            item = sample,
            onClick = {},
            modifier = Modifier.offset(x = offset.dp),
        )
    }
}

/** 完了済みタブの成績パネルの見本。実画面と同じ文言・組み方にする。 */
@Composable
private fun StatsPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "開始リミットより平均で",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "2時間30分",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                " 早く完了",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }
}

/**
 * 見本の提出物。最後の開始時刻は本物と同じ [Timing] の式で出すので、
 * カウントダウンの表示も本番とまったく同じ挙動になる。
 */
@Composable
private fun rememberSampleItem(): HomeItem = remember {
    val assignment = Assignment(
        title = "数学のプリント",
        type = AssignmentType.PAPER,
        deadlineEpochMillis = System.currentTimeMillis() + SAMPLE_DEADLINE_AHEAD_MILLIS,
        effortMinutes = 30,
    )
    HomeItem(
        assignment = assignment,
        criticalStartMillis = Timing.criticalStartMillis(
            assignment,
            SettingsRepository.DEFAULT_MARGIN_MINUTES,
        ),
        urgency = Urgency.HAS_TIME,
    )
}

/** 見本の締切は 3 時間後。開始リミットまでまだ余裕がある状態を見せる。 */
private const val SAMPLE_DEADLINE_AHEAD_MILLIS = 3 * 60 * 60 * 1000L
