package com.kadiyya.engreader

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * The level map: a winding trail through each world, one stop per level and a
 * bigger boss stop at the end of every world.
 */
class MapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var onLevelClick: ((Level) -> Unit)? = null

    private var worlds: List<World> = emptyList()
    private var stars: (Int) -> Int = { 0 }
    private var unlocked = 1

    private class Node(val level: Level, val world: World, val x: Float, val y: Float, val radius: Float)
    private class Banner(val world: World, val rect: RectF)
    private class Decoration(val emoji: String, val x: Float, val y: Float, val size: Float)

    private val nodes = mutableListOf<Node>()
    private val banners = mutableListOf<Banner>()
    private val decorations = mutableListOf<Decoration>()

    private val dp = resources.displayMetrics.density
    private val sp = resources.displayMetrics.scaledDensity

    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6 * dp
        strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val centerText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val labelText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13 * sp }
    private val emojiText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val dash = DashPathEffect(floatArrayOf(14 * dp, 12 * dp), 0f)

    private var pulse = 0f
    private val pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1400
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener {
            pulse = it.animatedValue as Float
            invalidate()
        }
    }

    fun setData(worlds: List<World>, unlocked: Int, stars: (Int) -> Int) {
        this.worlds = worlds
        this.unlocked = unlocked
        this.stars = stars
        requestLayout()
        invalidate()
    }

    /** Y position of the stop the player should play next. */
    fun currentNodeY(): Float = nodes.firstOrNull { it.level.number == unlocked }?.y ?: 0f

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        pulseAnimator.start()
    }

    override fun onDetachedFromWindow() {
        pulseAnimator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = layoutMap(width.toFloat())
        setMeasuredDimension(width, height.toInt())
    }

    private fun layoutMap(width: Float): Float {
        nodes.clear(); banners.clear(); decorations.clear()
        val random = Random(7)
        val decor = mapOf(
            0 to listOf("🌲", "🍄", "🌳", "🦊"),
            1 to listOf("🍄", "🃏", "🫖", "🌹"),
            2 to listOf("❄️", "🔔", "🎄", "⛄"),
            3 to listOf("🎩", "🕯️", "🗝️", "🚂"),
            4 to listOf("🌸", "💌", "🏰", "🎻"),
            5 to listOf("🦇", "🕯️", "🌙", "⚰️"),
        )
        var y = 24 * dp
        var stopIndex = 0
        for (world in worlds) {
            val bannerRect = RectF(16 * dp, y, width - 16 * dp, y + 76 * dp)
            banners += Banner(world, bannerRect)
            y = bannerRect.bottom + 56 * dp
            val worldTop = y
            for (level in world.levels) {
                val x = width / 2 + sin(stopIndex * 0.85f) * width * 0.27f
                val radius = (if (level.boss) 40 else 30) * dp
                nodes += Node(level, world, x, y, radius)
                y += (if (level.boss) 150 else 118) * dp
                stopIndex++
            }
            val icons = decor[world.index % decor.size].orEmpty()
            repeat(5) {
                decorations += Decoration(
                    icons[random.nextInt(icons.size)],
                    if (random.nextBoolean()) random.nextFloat() * width * 0.18f + 18 * dp
                    else width - random.nextFloat() * width * 0.18f - 18 * dp,
                    worldTop + random.nextFloat() * (y - worldTop - 60 * dp),
                    (18 + random.nextInt(14)) * sp,
                )
            }
            y += 8 * dp
        }
        return y + 40 * dp
    }

    override fun onDraw(canvas: Canvas) {
        for (d in decorations) {
            emojiText.textSize = d.size
            emojiText.alpha = 70
            canvas.drawText(d.emoji, d.x, d.y, emojiText)
        }
        emojiText.alpha = 255

        drawTrail(canvas)
        banners.forEach { drawBanner(canvas, it) }
        nodes.forEach { drawNode(canvas, it) }
    }

    private fun drawTrail(canvas: Canvas) {
        for (i in 0 until nodes.size - 1) {
            val a = nodes[i]
            val b = nodes[i + 1]
            val done = stars(a.level.number) > 0
            trailPaint.color = if (done) a.world.color else Color.argb(70, 255, 255, 255)
            trailPaint.pathEffect = if (done) null else dash
            val midY = (a.y + b.y) / 2
            val path = Path().apply {
                moveTo(a.x, a.y)
                cubicTo(a.x, midY, b.x, midY, b.x, b.y)
            }
            canvas.drawPath(path, trailPaint)
        }
    }

    private fun drawBanner(canvas: Canvas, banner: Banner) {
        val world = banner.world
        val open = world.levels.first().number <= unlocked
        fillPaint.color = if (open) withAlpha(world.color, 46) else Color.argb(20, 255, 255, 255)
        canvas.drawRoundRect(banner.rect, 20 * dp, 20 * dp, fillPaint)
        ringPaint.strokeWidth = 1.5f * dp
        ringPaint.color = if (open) withAlpha(world.color, 160) else Color.argb(50, 255, 255, 255)
        canvas.drawRoundRect(banner.rect, 20 * dp, 20 * dp, ringPaint)

        emojiText.textSize = 30 * sp
        canvas.drawText(if (open) world.emoji else "🔒", banner.rect.left + 36 * dp, banner.rect.centerY() + 11 * sp, emojiText)

        val textLeft = banner.rect.left + 70 * dp
        labelText.isFakeBoldText = true
        labelText.textSize = 18 * sp
        labelText.color = if (open) Color.WHITE else Color.argb(120, 255, 255, 255)
        canvas.drawText("Dünya ${world.index + 1} · ${world.name}", textLeft, banner.rect.top + 32 * dp, labelText)
        labelText.isFakeBoldText = false
        labelText.textSize = 13 * sp
        labelText.color = if (open) withAlpha(world.color, 230) else Color.argb(90, 255, 255, 255)
        val earned = world.levels.sumOf { stars(it.number) }
        canvas.drawText(
            "${world.book} — ${world.author}   ★ $earned/${world.levels.size * 3}",
            textLeft, banner.rect.top + 56 * dp, labelText,
        )
    }

    private fun drawNode(canvas: Canvas, node: Node) {
        val number = node.level.number
        val earned = stars(number)
        val locked = number > unlocked
        val current = number == unlocked && earned == 0

        if (current) {
            ringPaint.strokeWidth = 4 * dp
            ringPaint.color = withAlpha(node.world.color, ((1 - pulse) * 200).toInt())
            canvas.drawCircle(node.x, node.y, node.radius + pulse * 22 * dp, ringPaint)
        }

        fillPaint.color = Color.argb(120, 0, 0, 0)
        canvas.drawCircle(node.x, node.y + 5 * dp, node.radius, fillPaint)
        fillPaint.color = when {
            locked -> Color.rgb(42, 45, 54)
            earned > 0 -> node.world.color
            else -> Color.rgb(245, 245, 247)
        }
        canvas.drawCircle(node.x, node.y, node.radius, fillPaint)
        ringPaint.strokeWidth = 3 * dp
        ringPaint.color = if (locked) Color.argb(60, 255, 255, 255) else withAlpha(node.world.color, 255)
        canvas.drawCircle(node.x, node.y, node.radius, ringPaint)

        if (locked) {
            emojiText.textSize = (if (node.level.boss) 26 else 20) * sp
            canvas.drawText("🔒", node.x, node.y + emojiText.textSize * 0.36f, emojiText)
        } else {
            centerText.textSize = (if (node.level.boss) 26 else 22) * sp
            centerText.color = if (earned > 0) Color.rgb(20, 20, 24) else Color.rgb(30, 32, 40)
            canvas.drawText(number.toString(), node.x, node.y + centerText.textSize * 0.36f, centerText)
        }

        if (node.level.boss) {
            emojiText.textSize = 24 * sp
            canvas.drawText(if (locked) "💀" else "👑", node.x, node.y - node.radius - 6 * dp, emojiText)
        }

        if (!locked) {
            centerText.textSize = 15 * sp
            centerText.color = Color.rgb(255, 196, 70)
            val starText = "★".repeat(earned) + "☆".repeat(3 - earned)
            canvas.drawText(starText, node.x, node.y + node.radius + 20 * sp, centerText)
        }

        drawLabel(canvas, node, locked)
    }

    private fun drawLabel(canvas: Canvas, node: Node, locked: Boolean) {
        val onRight = node.x < width / 2
        val gap = node.radius + 14 * dp
        val available = (if (onRight) width - node.x - gap else node.x - gap) - 12 * dp
        if (available < 60 * dp) return

        labelText.textSize = 14 * sp
        labelText.isFakeBoldText = true
        labelText.color = if (locked) Color.argb(110, 255, 255, 255) else Color.WHITE
        val title = if (node.level.boss) "BOSS · ${node.level.title}" else node.level.title
        val titleLayout = StaticLayout.Builder
            .obtain(title, 0, title.length, labelText, available.toInt())
            .setAlignment(if (onRight) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_OPPOSITE)
            .setMaxLines(2)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        val left = if (onRight) node.x + gap else node.x - gap - available
        canvas.save()
        canvas.translate(left, node.y - titleLayout.height / 2f - 8 * dp)
        titleLayout.draw(canvas)
        canvas.restore()

        labelText.isFakeBoldText = false
        labelText.textSize = 12 * sp
        labelText.color = Color.argb(if (locked) 80 else 160, 255, 255, 255)
        val meta = "${node.level.wpm} k/dk · ${node.level.wordCount} kelime"
        val metaWidth = labelText.measureText(meta)
        val metaX = if (onRight) node.x + gap else node.x - gap - metaWidth
        canvas.drawText(meta, metaX, node.y + titleLayout.height / 2f + 8 * dp, labelText)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val hit = nodes.firstOrNull {
                hypot(event.x - it.x, event.y - it.y) <= it.radius + 16 * dp
            }
            if (hit != null) {
                performClick()
                onLevelClick?.invoke(hit.level)
            }
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}
