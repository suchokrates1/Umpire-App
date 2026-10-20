package pl.vestmedia.tennisreferee.ui

import android.os.Build
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * API 35+ draws content under the status bar and, for the framework action bar, under that bar too.
 * Screens that only padded the navigation bar left titles and the last button clipped.
 */
object SystemBarPadding {
    fun apply(activity: AppCompatActivity, root: View, underWindowActionBar: Boolean) {
        val baseLeft = root.paddingLeft
        val baseTop = root.paddingTop
        val baseRight = root.paddingRight
        val baseBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val topExtra = if (Build.VERSION.SDK_INT >= 35) {
                bars.top + if (underWindowActionBar) actionBarHeight(view) else 0
            } else {
                0
            }
            view.setPadding(baseLeft, baseTop + topExtra, baseRight, baseBottom + bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun actionBarHeight(view: View): Int {
        val typed = TypedValue()
        return if (view.context.theme.resolveAttribute(androidx.appcompat.R.attr.actionBarSize, typed, true)) {
            TypedValue.complexToDimensionPixelSize(typed.data, view.resources.displayMetrics)
        } else {
            (56 * view.resources.displayMetrics.density).toInt()
        }
    }
}
