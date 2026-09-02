package pl.vestmedia.tennisreferee.ui.tutorial

import android.app.Activity
import android.graphics.drawable.GradientDrawable
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
    private var insetHost: View? = null
    private var insetOriginal = 0

    fun attach(parent: ViewGroup? = null) {
        if (root != null || !TutorialSession.isActive) return
        val step = TutorialSession.currentStep(activity) ?: return
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 28, 32, 28)
            background = GradientDrawable().apply {
                setColor(activity.getColor(R.color.card_background))
                cornerRadius = 24f
            }
            elevation = 16f
        }
        card.addView(TextView(activity).apply {
            text = activity.getString(R.string.tutorial_title)
            textSize = 13f
            setTextColor(activity.getColor(R.color.on_surface))
        })
        card.addView(TextView(activity).apply {
            text = TutorialSession.stringFor(activity, step.titleKey)
            textSize = 18f
            setPadding(0, 8, 0, 8)
            setTextColor(activity.getColor(R.color.on_surface))
        })
        card.addView(TextView(activity).apply {
            text = TutorialSession.stringFor(activity, step.bodyKey)
            textSize = 15f
            setTextColor(activity.getColor(R.color.on_surface))
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

        val host = parent ?: (activity.window.decorView as ViewGroup)
        host.addView(card, layoutParamsFor(host))
        root = card
        applyContentInset(host, card)
        TutorialTargets.viewId(step.target)?.let { id ->
            activity.findViewById<View>(id)?.let { target ->
                target.isSelected = true
                target.requestFocus()
            }
        }
    }

    fun refresh(parent: ViewGroup? = null) {
        val nextParent = parent ?: (root?.parent as? ViewGroup)
        detach()
        attach(nextParent)
    }

    fun reparent(parent: ViewGroup) {
        val card = root ?: return attach(parent)
        (card.parent as? ViewGroup)?.removeView(card)
        clearInset()
        parent.addView(card, layoutParamsFor(parent))
        applyContentInset(parent, card)
    }

    fun detach() {
        clearInset()
        (root?.parent as? ViewGroup)?.removeView(root)
        root = null
    }

    private fun layoutParamsFor(parent: ViewGroup): ViewGroup.MarginLayoutParams {
        return if (parent is LinearLayout) {
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        } else {
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM,
            ).apply {
                leftMargin = 24
                rightMargin = 24
                bottomMargin = 24
            }
        }
    }

    private fun applyContentInset(host: ViewGroup, card: View) {
        if (host !== activity.window.decorView) return
        val content = activity.findViewById<View>(android.R.id.content) ?: return
        insetHost = content
        insetOriginal = content.paddingBottom
        card.post {
            val extra = card.height + ((card.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0)
            content.setPadding(
                content.paddingLeft,
                content.paddingTop,
                content.paddingRight,
                insetOriginal + extra,
            )
        }
    }

    private fun clearInset() {
        insetHost?.setPadding(
            insetHost!!.paddingLeft,
            insetHost!!.paddingTop,
            insetHost!!.paddingRight,
            insetOriginal,
        )
        insetHost = null
        insetOriginal = 0
    }
}
