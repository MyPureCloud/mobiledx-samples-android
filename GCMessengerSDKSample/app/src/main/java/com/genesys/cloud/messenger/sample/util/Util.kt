package com.genesys.cloud.messenger.sample.util

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.apply
import kotlin.run

internal fun toast(context: Context, string: String, timeout: Int = Toast.LENGTH_LONG, background: Drawable? = null) {
    Toast.makeText(context, string, timeout).apply {
        background?.run { view?.background = this }
    }.show()
}

internal fun View.snack(text: String, timeout: Int = Snackbar.LENGTH_LONG, @IdRes anchorId: Int = -1,
                        anchorGravity: Int = Gravity.CENTER, margins: IntArray = IntArray(0),
                        backgroundColor: Int = -1, disableSwipes: Boolean = true): Snackbar {

    val snackbar = Snackbar.make(this, text, timeout)

    (snackbar.view.layoutParams as? CoordinatorLayout.LayoutParams)?.run {
        if (anchorId != -1) {
            this.anchorId = anchorId
            this.anchorGravity = anchorGravity
        }
    } ?: run {
        (snackbar.view.layoutParams as? FrameLayout.LayoutParams)?.apply {
            gravity = Gravity.BOTTOM
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }

    if (margins.size >= 4) {
        (layoutParams as? ViewGroup.MarginLayoutParams)?.setMargins(margins[0], margins[1], margins[2], margins[3])
    }

    if (backgroundColor != -1) {
        snackbar.view.setBackgroundColor(backgroundColor)
    }



    val snackbarView = snackbar.view

    if (disableSwipes) {
        snackbarView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                snackbarView.viewTreeObserver.removeOnPreDrawListener(this)
                (snackbarView.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior = null
                return true
            }
        })
    }
    snackbar.show()

    return snackbar
}

internal fun <T> T.runMain(
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
    coroutineScope: CoroutineScope? = null,
    run: (T) -> Unit
) {
    this.run {
        if (Thread.currentThread() == Looper.getMainLooper().thread) {
            run(this)
        } else {
            (coroutineScope ?: CoroutineScope(dispatcher)).launch {
                withContext(dispatcher) { run(this@run) }
            }
        }
    }
}

internal fun IOScope(job: Job = SupervisorJob(), dispatcher: CoroutineDispatcher = Dispatchers.IO) : CoroutineScope {
    return CoroutineScope(job + dispatcher)
}

internal val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()