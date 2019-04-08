package lol.adel.graph.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import help.*
import lol.adel.graph.R
import kotlin.math.max

@SuppressLint("ViewConstructor")
class ScrollBarView(ctx: Context, size: Int) : View(ctx) {

    private sealed class Handle {

        object Left : Handle()

        object Right : Handle()

        data class Between(
            val left: X,
            val right: X,
            val x: X
        ) : Handle()
    }

    interface Listener {
        fun onBoundsChange(left: Float, right: Float)
    }

    private val pale = Paint().apply {
        color = ctx.color(R.color.scroll_overlay_pale)
    }
    private val bright = Paint().apply {
        color = ctx.color(R.color.scroll_overlay_bright)
    }

    var listener: Listener? = null

    var left: PxF = -1f
        set(value) {
            field = value
        }
    var right: PxF = -1f
        set(value) {
            field = value
        }

    private val dragging: SparseArray<Handle> = SparseArray()

    private fun around(x: X, view: X): Boolean =
        Math.abs(x - view) <= 24.dp

    private fun set(left: Float, right: Float) {
        this.left = left
        this.right = right
        listener?.onBoundsChange(left = left / width, right = right / width)
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        event.multiTouch(
            down = { pointerId, evX, _ ->
                val draggingSize = dragging.size()
                if (draggingSize == 1 && dragging.valueAt(0) is Handle.Between) {
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
                    dragging.put(pointerId, handle)

                    parent.requestDisallowInterceptTouchEvent(true)
                }
            },
            move = { pointerId, evX, _ ->
                when (val handle = dragging[pointerId]) {
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
                dragging.delete(pointerId)

                parent.requestDisallowInterceptTouchEvent(false)
            }
        )

        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = widthF
        val height = heightF

        val lineWidth = 5.dpF
        val lineHeight = 2.dpF
        val halfLineWidth = lineWidth / 2

        canvas.drawRect(0f, 0f, left - halfLineWidth, height, pale)
        canvas.drawRect(right + halfLineWidth, 0f, width, height, pale)

        canvas.drawRect(left - halfLineWidth, 0f, left + halfLineWidth, height, bright)
        canvas.drawRect(left + halfLineWidth, 0f, right - halfLineWidth, lineHeight, bright)
        canvas.drawRect(left + halfLineWidth, height - lineHeight, right - halfLineWidth, height, bright)
        canvas.drawRect(right - halfLineWidth, 0f, right + halfLineWidth, height, bright)
    }
}
