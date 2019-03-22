package lol.adel.graph.widget

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import help.*
import lol.adel.graph.Dragging
import lol.adel.graph.R
import kotlin.math.max

class ScrollBarView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(ctx, attrs, defStyleAttr) {

    interface Listener {
        fun onBoundsChange(left: Float, right: Float)
    }

    private val pale = Paint().apply { color = ctx.color(R.color.scroll_overlay_pale) }
    private val bright = Paint().apply { color = ctx.color(R.color.scroll_overlay_bright) }
    private val touch = Paint().apply { color = ctx.color(R.color.scroll_overlay_touch) }

    var listener: Listener? = null

    private var left: Float = -1f
    private var right: Float = -1f
    private val dragging: SparseArray<Dragging> = SparseArray()

    private var radius: PxF = 0f
        set(value) {
            field = value
            invalidate()
        }

    private var anim: Animator? = null

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
        val pointerId = event.pointerId()
        val evX = event.x()

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (dragging[pointerId] == null) {
                    println("down $pointerId")

                    val d = when {
                        around(evX, left) ->
                            Dragging.Left

                        around(evX, right) ->
                            Dragging.Right

                        evX in left..right && pointerId == 0 ->
                            Dragging.Between(left = left, right = right, x = evX)

                        else ->
                            null
                    }
                    dragging.put(pointerId, d)

                    anim?.cancel()
                    anim = animateFloat(radius, (heightF + 32.dp) / 2) {
                        radius = it
                    }
                    anim?.start()
                }
            }

            MotionEvent.ACTION_MOVE -> {
//                println("moving $pointerId")

                when (val d = dragging[pointerId]) {
                    Dragging.Left ->
                        set(clamp(evX, 0f, right - 48.dp), right)

                    Dragging.Right ->
                        set(left, clamp(evX, left + 48.dp, widthF))

                    is Dragging.Between -> {
                        val diff = evX - d.x
                        val newLeft = d.left + diff
                        val newRight = d.right + diff
                        val distance = d.right - d.left

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
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> {
                println("up $pointerId")

                dragging.delete(pointerId)

                anim?.cancel()
                anim = animateFloat(radius, 0f) {
                    radius = it
                }
                anim?.start()
            }
        }
        return true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (this.left < 0) {
            val quarter = width / 4f
            set(quarter, quarter * 3)
        } else {
            set(this.left, this.right)
        }
    }

    override fun onSaveInstanceState(): Parcelable =
        Bundle().apply {
            putParcelable("super", super.onSaveInstanceState())
            putFloat("left", left)
            putFloat("right", right)
        }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            super.onRestoreInstanceState(state.getParcelable("super"))
            left = state.getFloat("left")
            right = state.getFloat("right")
        } else {
            super.onRestoreInstanceState(state)
        }
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

        dragging.forEach { _, d ->
            when (d) {
                Dragging.Left ->
                    canvas.drawCircle(left, halfHeight, radius, touch)

                Dragging.Right ->
                    canvas.drawCircle(right, halfHeight, radius, touch)

                is Dragging.Between ->
                    canvas.drawCircle((right - left) / 2 + left, halfHeight, radius, touch)
            }
        }
    }
}