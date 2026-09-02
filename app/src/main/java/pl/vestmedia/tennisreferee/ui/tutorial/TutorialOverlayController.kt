package pl.vestmedia.tennisreferee.ui.tutorial

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import pl.vestmedia.tennisreferee.R

class TutorialOverlayController(
    private val activity: Activity,
    private val onBack: () -> Unit,
    private val onNext: () -> Unit,
    private val onSkip: () -> Unit,
) {
    private var root: View? = null

    fun attach() {
        if (root != null || !TutorialSession.isActive) return
        val step = TutorialSession.currentStep(activity) ?: return
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 28, 32, 28)
            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
        }
        card.addView(TextView(activity).apply {
            text = activity.getString(R.string.tutorial_title)
            textSize = 13f
        })
        card.addView(TextView(activity).apply {
            text = TutorialSession.stringFor(activity, step.titleKey)
            textSize = 18f
            setPadding(0, 8, 0, 8)
        })
        card.addView(TextView(activity).apply {
            text = TutorialSession.stringFor(activity, step.bodyKey)
            textSize = 15f
        })
        val buttons = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 20, 0, 0)
        }
        buttons.addView(Button(activity).apply {
            text = activity.getString(R.string.tutorial_back)
            setOnClickListener { onBack() }
        })
        buttons.addView(Button(activity).apply {
            text = if (TutorialSession.isLast(activity)) {
                activity.getString(R.string.tutorial_exit)
            } else {
                activity.getString(R.string.tutorial_next)
            }
            isEnabled = TutorialSession.canAdvance(activity)
            setOnClickListener { onNext() }
        })
        buttons.addView(Button(activity).apply {
            text = activity.getString(R.string.tutorial_skip)
            setOnClickListener { onSkip() }
        })
        card.addView(buttons)

        val parent = activity.window.decorView as ViewGroup
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM,
        ).apply {
            leftMargin = 24
            rightMargin = 24
            bottomMargin = 24
        }
        parent.addView(card, params)
        root = card
        TutorialTargets.viewId(step.target)?.let { id ->
            activity.findViewById<View>(id)?.let { target ->
                target.isSelected = true
                target.requestFocus()
            }
        }
    }

    fun refresh() {
        detach()
        attach()
    }

    fun detach() {
        (root?.parent as? ViewGroup)?.removeView(root)
        root = null
    }
}
