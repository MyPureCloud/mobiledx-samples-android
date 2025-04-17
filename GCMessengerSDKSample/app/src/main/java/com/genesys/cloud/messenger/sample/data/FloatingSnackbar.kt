package com.genesys.cloud.messenger.sample.data

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatTextView
import com.genesys.cloud.core.utils.children
import com.genesys.cloud.core.utils.px
import com.genesys.cloud.messenger.sample.R
import com.google.android.material.snackbar.Snackbar

class FloatingSnackbar {

    companion object {
        fun make(parent: View, @StringRes resId: Int, duration: Int): Snackbar {
            return Snackbar.make(parent, parent.resources.getText(resId), duration).apply {
                val snackbar = this
                val params = view.layoutParams as FrameLayout.LayoutParams
                params.gravity = Gravity.TOP
                params.topMargin = 8.px
                params.bottomMargin = 8.px
                params.leftMargin = 16.px
                params.rightMargin = 16.px
                view.layoutParams = params
                view.setBackgroundResource(R.drawable.snackbar_floating_background)
                view.setOnClickListener { snackbar.dismiss() }
                val contentLayout = view.children().firstOrNull() as LinearLayout
                contentLayout.children().filterIsInstance<AppCompatTextView>().firstOrNull()
                    ?.setTextColor(Color.rgb(0, 0, 0))
                (contentLayout as? LinearLayout?)?.addView(
                    ImageView(context).apply {
                        setImageResource(R.drawable.close)
                    },
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER
                    }
                )
            }
        }
    }
}