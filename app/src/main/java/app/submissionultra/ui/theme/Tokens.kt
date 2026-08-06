package app.submissionultra.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// 見た目の寸法をここに集める。画面側で dp を直書きしない。
//
// 値は思いつきではなく、既存コードで実際に使われていた数値の分布から決めた。
// 間隔は 118 箇所のうち 83 箇所が 4/8/12/16/24dp に集中していたので、4 の倍数へ寄せる。
// 角丸は 8/12/16/20dp が無規則に散っていたので、面の大きさに応じた 3 段階に畳む。
//
// 規則の意図は DESIGN.md に書いてある。値を足したくなったら、まずそちらを読む。

/**
 * 要素どうしの間隔。
 *
 * 中間の値（6, 10, 14, 20, 28dp）は作らない。刻みが増えるほど「なんとなく」で選べてしまい、
 * 画面ごとに間隔がずれて、そのずれが情報の区切りだと誤読される。
 */
object Space {
    /** 行間の詰め。ラベルとその値のような、ひと塊の内側。 */
    val xs = 4.dp

    /** 要素間の標準。 */
    val sm = 8.dp

    /** カード内の要素間。 */
    val md = 12.dp

    /** 画面の左右余白、カードの内側の余白。 */
    val lg = 16.dp

    /** セクション間。ここで話題が変わることを示す。 */
    val xl = 24.dp

    /** 画面の上下に取る大きな余白。 */
    val xxl = 32.dp
}

/**
 * 角の丸み。面が大きいほど丸みも大きい。
 *
 * この 3 つ以外を新しく作らない。中途半端な差は「別の種類の面だ」という誤った合図になる。
 */
object Radius {
    /** 内側の小さい面（バナー、プレビュー枠）。 */
    val sm = 8.dp

    /** カード。既定の面はこれ。 */
    val md = 12.dp

    /** 主役の面（ヒーロー）。 */
    val lg = 16.dp
}

/** 面の境界線。太さはこれ 1 つしかない。 */
object Stroke {
    val hairline = 1.dp
}

/**
 * Material 3 のコンポーネントが参照する形。[Radius] と対応させる。
 *
 * これを [MaterialTheme] に渡さないと、Card や Dialog だけが M3 の既定値
 * （4/8/12/16/28dp）で描かれ、自前のカードと角丸が食い違う。
 * extraLarge は M3 既定が 28dp だが、ここでは lg(16dp) に寄せる。段階を増やさないため。
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.sm),
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(Radius.lg),
)

/**
 * 面の階層。
 *
 * 影ではなく面色で階層を表す。背景をわずかに色味のあるグレーにして、カード(白)が
 * 浮いて見えるようにしてある（Color.kt 参照）。影はその補助でしかない。
 *
 * 段階はこの 3 つだけ。[Lead] は 1 画面に 1 つまで。
 */
enum class SurfaceLevel {
    /** 画面の地。 */
    Base,

    /** 通常のカード。 */
    Raised,

    /** 主役の面。1 画面に 1 つ。 */
    Lead,
}

/** その階層の面の色。 */
@Composable
@ReadOnlyComposable
fun SurfaceLevel.color(): Color = when (this) {
    SurfaceLevel.Base -> MaterialTheme.colorScheme.background
    SurfaceLevel.Raised, SurfaceLevel.Lead -> MaterialTheme.colorScheme.surface
}

/**
 * その階層の枠線。地には枠を引かない。
 *
 * 主役の面だけ [MaterialTheme.colorScheme.outline]（濃い方）を使い、
 * 通常のカードは outlineVariant（薄い方）にする。枠の濃さも階層の一部として働く。
 */
@Composable
@ReadOnlyComposable
fun SurfaceLevel.border(): BorderStroke? = when (this) {
    SurfaceLevel.Base -> null
    SurfaceLevel.Raised -> BorderStroke(Stroke.hairline, MaterialTheme.colorScheme.outlineVariant)
    SurfaceLevel.Lead -> BorderStroke(Stroke.hairline, MaterialTheme.colorScheme.outline)
}

/**
 * その階層の影。
 *
 * 通常のカードには影を落とさない。面色と枠線だけで階層が読めていることを先に成立させ、
 * 影は主役の面にだけ使う。ダークモードでは影はほとんど見えないため、影に意味を持たせない。
 */
val SurfaceLevel.shadow
    get() = when (this) {
        SurfaceLevel.Base, SurfaceLevel.Raised -> 0.dp
        SurfaceLevel.Lead -> 2.dp
    }

/** その階層に合う角丸。 */
val SurfaceLevel.radius
    get() = when (this) {
        SurfaceLevel.Base -> Radius.sm
        SurfaceLevel.Raised -> Radius.md
        SurfaceLevel.Lead -> Radius.lg
    }
