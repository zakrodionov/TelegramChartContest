package lol.adel.graph.widget

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.collection.SimpleArrayMap
import help.*
import lol.adel.graph.Dates
import lol.adel.graph.R
import lol.adel.graph.Typefaces
import lol.adel.graph.data.*

@SuppressLint("ViewConstructor")
class ToolTipView(ctx: Context, val data: Chart, val enabledLines: List<LineId>) : LinearLayout(ctx) {

    private val floatingText: TextDiffView
    private val floatingContainer: ViewGroup

    private val lineTexts = SimpleArrayMap<LineId, ViewGroup>()

    private fun makeLineText(ctx: Context, chart: Chart, id: LineId, medium: Typeface): ViewGroup =
        LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL

            visibility = visibleOrGone(id in enabledLines)

            addView(TextView(ctx).apply {
                textSize = 12f
                setTextColor(ctx.color(R.attr.label_text))
                text = chart.names[id]
            })

            addView(TextDiffView(ctx).apply {
                textSizeDp = 12.dpF
                textColor = chart.color(id)
                typeface = medium
                fullFlip = true
            }, LayoutParams(MATCH_PARENT, 20.dp))
        }

    init {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.floating_bg)
        elevation = 2.dpF
        updatePadding(left = 16.dp, top = 8.dp, right = 16.dp, bottom = 8.dp)

        floatingText = TextDiffView(ctx).apply {
            typeface = Typefaces.medium
            textColor = ctx.color(R.attr.floating_text)
            textSizeDp = 12.dpF
        }
        addView(floatingText, LinearLayout.LayoutParams(MATCH_PARENT, 20.dp))

        floatingContainer = LinearLayout(ctx).apply {
            layoutTransition = LayoutTransition()
            orientation = LinearLayout.VERTICAL
            showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
            dividerDrawable = ctx.getDrawable(R.drawable.h_space_16)
        }
        addView(floatingContainer)

        data.lineIds.forEachByIndex { id ->
            val text = makeLineText(ctx, data, id, Typefaces.medium)
            floatingContainer.addView(text)
            lineTexts[id] = text
        }
    }

    fun lineChecked(select: List<LineId>, deselect: List<LineId>) {
        deselect.forEachByIndex {
            lineTexts[it]?.visibility = View.GONE
        }
        select.forEachByIndex {
            lineTexts[it]?.visibility = View.VISIBLE
        }
    }

    fun show(idx: Idx, x: PxF) {
        val floatingWidth = width
        val parentWidth = parent.parent.let { it as View }.width

        val target = x - 20.dp
        val altTarget = x - floatingWidth + 40.dp
        val rightOk = target + floatingWidth <= parentWidth

        translationX = when {
            target > 0 && rightOk ->
                target

            !rightOk && altTarget > 0 ->
                altTarget

            else ->
                0f
        }

        floatingText.text = Dates.tooltip(data.xs[idx])
        lineTexts.forEach { id, view ->
            view.component2().toTextDiff().text = tooltipValue(data.columns[id][idx])
        }
    }
}
