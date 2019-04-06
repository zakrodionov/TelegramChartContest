package lol.adel.graph

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Paint
import android.view.animation.AccelerateInterpolator
import androidx.core.util.Pools
import help.alphaF
import help.color
import help.dpF
import help.onEnd
import lol.adel.graph.widget.HorizontalLabelsView

data class YLabel(
    var min: Float,
    var max: Float,
    val linePaint: Paint,
    val labelPaint: Paint,
    val animator: ValueAnimator
) {
    companion object {

        private val H_LINE_THICKNESS = 2.dpF
        private val POOL = Pools.SimplePool<YLabel>(100)
        private val START_FAST = AccelerateInterpolator()

        fun create(ctx: Context): YLabel {
            val linePaint = Paint().apply {
                color = ctx.color(R.color.divider)
                strokeWidth = H_LINE_THICKNESS
            }

            val labelPaint = Paint().apply {
                color = ctx.color(R.color.label_text)
                textSize = HorizontalLabelsView.TEXT_SIZE_PX
                isAntiAlias = true
            }

            return YLabel(
                min = 0f,
                max = 0f,
                linePaint = linePaint,
                animator = ValueAnimator.ofFloat(1f, 0f),
                labelPaint = labelPaint
            )
        }

        fun obtain(ctx: Context, list: MutableList<YLabel>): YLabel =
            POOL.acquire() ?: create(ctx).also { label ->
                label.animator.run {
                    interpolator = START_FAST
                    addUpdateListener {
                        label.setAlpha(1 - it.animatedFraction)
                    }
                    onEnd {
                        release(label, list)
                    }
                }
            }

        fun release(label: YLabel, list: MutableList<YLabel>) {
            label.animator.removeAllListeners()
            label.animator.cancel()
            list -= label
            POOL.release(label)
        }
    }
}

inline fun YLabel.iterate(steps: Int, paint: Paint, f: (Long, Paint) -> Unit) {
    val origStepSize = (max - min) / steps
    val newMax = max - origStepSize / 3
    val newStepSize = (newMax - min) / steps
    help.iterate(from = min, to = newMax, stepSize = newStepSize, f = { f(it.toLong(), paint) })
}

fun YLabel.set(from: MinMax) {
    min = from.min
    max = from.max
}

fun YLabel.setAlpha(alpha: Float) {
    labelPaint.alphaF = alpha
    linePaint.alphaF = alpha
}
