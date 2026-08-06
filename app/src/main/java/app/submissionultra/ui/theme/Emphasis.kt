package app.submissionultra.ui.theme

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.sp

// 強調は有限の資源として扱う。1 画面につき、最も強調された要素は 1 つ。
//
// これは守られにくい規則なので、口頭の約束にせずコードで見張る。
// 画面のルートを [EmphasisScope] で包み、主役を [ScreenLead] で宣言すると、
// 同じ画面に主役が 2 つ現れた時点で気づける。
//
// 規則の意図と「2 つになりそうなときの解き方」は DESIGN.md に書いてある。

/**
 * 強調の段階。
 *
 * 数字の大きさで優先度を表すのがこのアプリの原則なので、段階を増やすと原則そのものが濁る。
 * 本文と補助情報は Type.kt の Typography に委ね、ここでは「主役」と「それに次ぐもの」だけを決める。
 */
object Emphasis {

    /**
     * 画面の主役。1 画面に 1 つだけ。
     *
     * 36sp は既存の実測値から決めた。完了済みの成績が 36sp、ホームのヒーローが 40sp、
     * チュートリアルの見本が 32sp とばらけていたので、いちばん長い文字列（`2:30:45`）が
     * 入るカウントダウンで溢れない側に揃えている。
     */
    val Lead = 36.sp

    /** 主役に次ぐ数字。カード内のカウントダウンなど。 */
    val Strong = 22.sp
}

/**
 * 画面のルートに置く。この中で [ScreenLead] が 2 回宣言されると、
 * 開発ビルドと Preview で例外になる。
 *
 * 画面ごとに独立した数え上げにしたいので、CompositionLocal で下へ配る。
 * リリースビルドでは数えるだけで何もしない。
 *
 * @param screenName 例外メッセージに出す画面名。どこで壊れたかを一目で分かるようにする。
 */
@Composable
fun EmphasisScope(screenName: String, content: @Composable () -> Unit) {
    // 実機の開発ビルドと、Android Studio の Preview の両方で見張る。
    // BuildConfig は生成していない（buildFeatures に buildConfig が無い）ので、
    // インストールされたアプリ自身の debuggable フラグを見る。
    val context = LocalContext.current
    val inspecting = LocalInspectionMode.current
    val registry = remember(screenName, inspecting) {
        val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        LeadRegistry(screenName = screenName, enforcing = debuggable || inspecting)
    }
    CompositionLocalProvider(LocalLeadRegistry provides registry, content = content)
}

/**
 * この画面の主役をここに置く。
 *
 * [EmphasisScope] の外で呼んでも何も起きない（部品単体の Preview を壊さないため）。
 * 見張りが要るのは画面全体を組んだときだけなので、それでよい。
 */
@Composable
fun ScreenLead(content: @Composable () -> Unit) {
    val registry = LocalLeadRegistry.current
    // 数え上げは composition の回数ではなく、画面に出ている実体の数で行う。
    // 再コンポーズのたびに増えないよう DisposableEffect で出入りを取る。
    DisposableEffect(registry) {
        registry?.acquire()
        onDispose { registry?.release() }
    }
    content()
}

/**
 * 条件つきで主役として宣言する。false なら素通しする。
 *
 * 「状況によって主役を入れ替える」書き方のためにある。同じ画面の 2 箇所を
 * 排反な条件で [LeadIf] に包めば、どちらか一方だけが主役になる。
 */
@Composable
fun LeadIf(condition: Boolean, content: @Composable () -> Unit) {
    if (condition) ScreenLead(content) else content()
}

/**
 * 主役がいくつ出ているかを数える。
 *
 * 条件分岐で主役を差し替える書き方（片方が消えて片方が現れる）でも誤検知しないよう、
 * Compose が「消える側の onDispose を先に走らせてから、現れる側の効果を走らせる」
 * 順序に乗っている。差し替えの一瞬に 2 になることはない。
 */
internal class LeadRegistry(
    private val screenName: String,
    private val enforcing: Boolean,
) {
    private var count = 0

    fun acquire() {
        count++
        if (enforcing && count > 1) {
            error(
                "$screenName に主役が $count つあります。ScreenLead は 1 画面に 1 つだけです。\n" +
                    "どちらがこの画面の存在理由かを決めてください。解き方は DESIGN.md の" +
                    "「2 つになりそうなときの解き方」にあります。",
            )
        }
    }

    fun release() {
        count--
    }
}

private val LocalLeadRegistry = staticCompositionLocalOf<LeadRegistry?> { null }
