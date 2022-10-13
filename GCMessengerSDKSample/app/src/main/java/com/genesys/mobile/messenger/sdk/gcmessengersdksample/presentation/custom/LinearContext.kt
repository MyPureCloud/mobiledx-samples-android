package com.genesys.mobile.messenger.sdk.gcmessengersdksample.presentation.custom

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import kotlin.math.max

class LinearContext @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), ContextContainer {

    var scroller: ScrollView? = null

    override fun addContextView(view: View) {
        addView(view, max(childCount - 1, 0))
    }

    override fun removeContext(contextView: ContextViewHolder) {
        removeView(contextView.getView())
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (changed) {
            scroller?.scrollTo(l, b)
        }
    }

    override fun clear() {
        removeViews(0, childCount - 1)
    }

    override fun getContextList(): Map<String, String> {
        return (0 until childCount - 1).map { idx ->
            val entry = (getChildAt(idx) as? ContextViewHolder)?.getBotContext() ?: Pair("", "")
            entry.first to entry.second
        }.filterNot { it.first.isBlank() }.toMap() // remove empty pairs
    }

    override fun getLast(): Pair<String, String>? {
        return (takeIf { childCount > 1 }?.getChildAt(childCount - 2) as? ContextViewHolder)?.getBotContext()
    }
}