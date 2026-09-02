package pl.vestmedia.tennisreferee.ui.tutorial

import android.app.Activity
import android.content.Context

object TutorialSession {
    var isActive: Boolean = false
        private set
    var stepIndex: Int = 0
        private set
    var actionSatisfied: Boolean = false
        private set
    var returnToSettings: Boolean = true

    fun start(fromSettings: Boolean) {
        isActive = true
        stepIndex = 0
        actionSatisfied = false
        returnToSettings = fromSettings
    }

    fun stop(context: Context?) {
        isActive = false
        stepIndex = 0
        actionSatisfied = false
        context?.let { TutorialPrefs.markDone(it) }
    }

    fun currentStep(context: Context): TutorialStep? {
        val steps = TutorialScript.load(context).steps
        return steps.getOrNull(stepIndex)
    }

    fun noteAction(action: String, context: Context) {
        val step = currentStep(context) ?: return
        if (step.requireAction == action) actionSatisfied = true
    }

    fun canAdvance(context: Context): Boolean {
        val step = currentStep(context) ?: return false
        return step.requireAction.isNullOrBlank() || actionSatisfied
    }

    fun isLast(context: Context): Boolean {
        val steps = TutorialScript.load(context).steps
        return stepIndex >= steps.lastIndex
    }

    fun goNext(context: Context): TutorialStep? {
        if (!canAdvance(context)) return currentStep(context)
        val steps = TutorialScript.load(context).steps
        if (stepIndex >= steps.lastIndex) return currentStep(context)
        stepIndex += 1
        actionSatisfied = currentStep(context)?.requireAction.isNullOrBlank()
        return currentStep(context)
    }

    fun goBack(context: Context): TutorialStep? {
        if (stepIndex <= 0) return currentStep(context)
        stepIndex -= 1
        actionSatisfied = currentStep(context)?.requireAction.isNullOrBlank()
        return currentStep(context)
    }

    fun jumpToLast(context: Context) {
        val steps = TutorialScript.load(context).steps
        if (steps.isEmpty()) return
        stepIndex = steps.lastIndex
        actionSatisfied = true
    }

    fun stringFor(context: Context, camelKey: String): String {
        val name = TutorialScript.titleRes(camelKey)
        val id = context.resources.getIdentifier(name, "string", context.packageName)
        return if (id != 0) context.getString(id) else camelKey
    }

    fun finishHost(activity: Activity) {
        if (returnToSettings) {
            activity.finish()
        }
    }
}
