package lol.adel.graph.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.text.TextPaint
import android.view.View
import help.*
import lol.adel.graph.Dates
import lol.adel.graph.MinMax
import lol.adel.graph.R
import lol.adel.graph.len

@SuppressLint("ViewConstructor")
class XLabelsView(ctx: Context, private val xs: LongArray, private val cameraX: MinMax) : View(ctx) {

    companion object {
        val TEXT_SIZE_PX: PxF = 11.dpF
        private val GAP: PxF = 80.dpF
    }

    private val opaque = TextPaint().apply {
        color = ctx.color(R.attr.label_text)
        textSize = TEXT_SIZE_PX
        isAntiAlias = true
    }
    private val transparent = TextPaint().apply {
        color = ctx.color(R.attr.label_text)
        textSize = TEXT_SIZE_PX
        isAntiAlias = true
    }

    init {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    fun cameraXChanged() {
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = widthF
        val halfHeight = heightF / 2

        val visibleIdxRange = cameraX.len()
        val daysToShow = width / GAP
        val pxPerIdx = width / visibleIdxRange

        val rawStep = visibleIdxRange / daysToShow
        val everyLog2 = rawStep.log2()
        val stepFloor = everyLog2.floor().pow2()
        val stepCeil = everyLog2.ceil().pow2()

        val fraction = if (stepCeil == stepFloor) 1f
        else (rawStep - stepFloor) / (stepCeil - stepFloor)

        val (start, end) = cameraX

        val lastIdx = xs.size - 1
        val startFromIdx = clamp((start - start % stepCeil).toInt(), 0, lastIdx)
        val hiddenEnd = clamp(end.ceil(), 0, lastIdx)

        iterate(from = startFromIdx, to = hiddenEnd, step = stepCeil) { idx ->
            val text = Dates.xLabel(xs[idx])
            val textWidth = opaque.measureText(text)
            canvas.drawText(text, pxPerIdx * (idx - start) - textWidth / 2f, halfHeight, opaque)
        }
        transparent.alphaF = 1 - fraction
        iterate(from = startFromIdx + stepFloor, to = hiddenEnd, step = stepCeil) { idx ->
            val text = Dates.xLabel(xs[idx])
            val textWidth = transparent.measureText(text)
            canvas.drawText(text, pxPerIdx * (idx - start) - textWidth / 2f, halfHeight, transparent)
        }
    }
}
