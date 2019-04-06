package lol.adel.graph.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import help.*
import lol.adel.graph.Dragging
import lol.adel.graph.Handle
import lol.adel.graph.R
import kotlin.math.max

class ScrollBarView(ctx: Context) : View(ctx) {

    interface Listener {
        fun onBoundsChange(left: Float, right: Float)
        fun onTouchStop()
    }

    private val pale = Paint().apply {
        color = ctx.color(R.color.scroll_overlay_pale)
    }
    private val bright = Paint().apply {
        color = ctx.color(R.color.scroll_overlay_bright)
    }
    private val touch = Paint().apply {
        color = ctx.color(R.color.scroll_overlay_touch)
        isAntiAlias = true
    }

    var listener: Listener? = null

    private var left: Float = -1f
    private var right: Float = -1f

    private val dragging: SparseArray<Dragging> = SparseArray()
    private val wasDragging: SparseArray<Dragging> = SparseArray()

    private fun around(x: X, view: X): Boolean =
        Math.abs(x - view) <= 24.dp

    private fun set(left: Float, right: Float) {
        this.left = left
        this.right = right
        listener?.onBoundsChange(left = left / width, right = right / width)
        invalidate()
    }

    fun toggleNight() {
        animateColor(pale, R.color.scroll_overlay_pale)
        animateColor(bright, R.color.scroll_overlay_bright)
        animateColor(touch, R.color.scroll_overlay_touch)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        event.multiTouch(
            down = { pointerId, evX, _ ->
                val draggingSize = dragging.size()
                if (draggingSize == 1 && dragging.valueAt(0)?.handle is Handle.Between) {
                    return true
                }

                if (dragging[pointerId] == null && draggingSize < 2) {
                    val handle = when {
                        evX in (left + 12.dp)..(right - 12.dp) && draggingSize == 0 ->
                            Handle.Between(left = left, right = right, x = evX)

                        around(evX, left) ->
                            Handle.Left

                        around(evX, right) ->
                            Handle.Right

                        else ->
                            null
                    }
                    val d = handle?.let {
                        var self: Dragging? = null

                        self = Dragging(
                            feedbackRadius = 0f,
                            handle = it,
                            radiusAnim = animateFloat(0f, (heightF + 32.dp) / 2) {
                                self?.feedbackRadius = it
                                invalidate()
                            }.apply {
                                duration = 100
                                start()
                            }
                        )

                        self
                    }
                    dragging.put(pointerId, d)

                    parent.requestDisallowInterceptTouchEvent(true)
                }
            },
            move = { pointerId, evX, _ ->
                when (val handle = dragging[pointerId]?.handle) {
                    Handle.Left ->
                        set(clamp(evX, 0f, right - 48.dp), right)

                    Handle.Right ->
                        set(left, clamp(evX, left + 48.dp, widthF))

                    is Handle.Between -> {
                        val diff = evX - handle.x
                        val newLeft = handle.left + diff
                        val newRight = handle.right + diff
                        val distance = handle.right - handle.left

                        when {
                            newLeft >= 0 && newRight < width ->
                                set(newLeft, newRight)

                            newLeft <= 0 ->
                                set(left = 0f, right = distance)

                            newRight >= widthF ->
                                set(left = max(0f, width - 1 - distance), right = width - 1f)
                        }
                    }
                }
            },
            up = { pointerId, _, _ ->
                dragging[pointerId]?.run {
                    radiusAnim.cancel()
                    radiusAnim = animateFloat(feedbackRadius, 0f) {
                        feedbackRadius = it
                        invalidate()
                    }.apply {
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator?) {
                                wasDragging.delete(pointerId)
                            }
                        })
                        start()
                    }
                    wasDragging.put(pointerId, this)
                }
                dragging.delete(pointerId)

                parent.requestDisallowInterceptTouchEvent(false)
            }
        )

        return true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (this.left < 0) {
            val quarter = width / 4f
            set(quarter * 3, quarter * 4)
        } else {
            set(this.left, this.right)
        }
    }

    private fun draw(d: Dragging?, canvas: Canvas, halfHeight: Float): Unit =
        when (d?.handle) {
            Handle.Left ->
                canvas.drawCircle(left, halfHeight, d.feedbackRadius, touch)

            Handle.Right ->
                canvas.drawCircle(right, halfHeight, d.feedbackRadius, touch)

            is Handle.Between ->
                canvas.drawCircle((right - left) / 2 + left, halfHeight, d.feedbackRadius, touch)

            else ->
                Unit
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = widthF
        val height = heightF
        val halfHeight = height / 2

        val lineWidth = 5.dpF
        val lineHeight = 2.dpF
        val halfLineWidth = lineWidth / 2

        canvas.drawRect(0f, 0f, left - halfLineWidth, height, pale)
        canvas.drawRect(right + halfLineWidth, 0f, width, height, pale)

        canvas.drawRect(left - halfLineWidth, 0f, left + halfLineWidth, height, bright)
        canvas.drawRect(left + halfLineWidth, 0f, right - halfLineWidth, lineHeight, bright)
        canvas.drawRect(left + halfLineWidth, height - lineHeight, right - halfLineWidth, height, bright)
        canvas.drawRect(right - halfLineWidth, 0f, right + halfLineWidth, height, bright)

        dragging.forEach { _, d -> draw(d, canvas, halfHeight) }
        wasDragging.forEach { _, d -> draw(d, canvas, halfHeight) }
    }
}
