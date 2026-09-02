package pl.vestmedia.tennisreferee.ui.tutorial

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.ui.courtselection.CourtSelectionActivity
import pl.vestmedia.tennisreferee.ui.match.MatchActivity
import pl.vestmedia.tennisreferee.ui.playerselection.PlayerSelectionActivity
import pl.vestmedia.tennisreferee.ui.settings.SettingsActivity

object TutorialNavigator {
    const val EXTRA_TUTORIAL = "tutorial"

    fun startFromSettings(activity: Activity) {
        TutorialPrefs.markPrompted(activity)
        TutorialSession.start(fromSettings = true)
        activity.startActivity(CourtSelectionActivity.createTutorialIntent(activity))
    }

    fun startFromBanner(activity: Activity) {
        TutorialPrefs.markPrompted(activity)
        TutorialSession.start(fromSettings = false)
        activity.startActivity(CourtSelectionActivity.createTutorialIntent(activity))
    }

    fun maybeShowBanner(activity: Activity, onLater: () -> Unit = {}) {
        if (!TutorialPrefs.shouldShowBanner(activity) || TutorialSession.isActive) return
        AlertDialog.Builder(activity)
            .setTitle(R.string.tutorial_banner_title)
            .setMessage(R.string.tutorial_banner_message)
            .setPositiveButton(R.string.tutorial_start) { _, _ -> startFromBanner(activity) }
            .setNegativeButton(R.string.tutorial_later) { _, _ ->
                TutorialPrefs.markPrompted(activity)
                onLater()
            }
            .show()
    }

    fun afterRequiredAction(activity: Activity, action: String, onSameActivity: () -> Unit = {}) {
        TutorialSession.noteAction(action, activity)
        if (!TutorialSession.canAdvance(activity) || TutorialSession.isLast(activity)) {
            onSameActivity()
            return
        }
        TutorialSession.goNext(activity)
        if (applyStep(activity)) {
            if (activity is MatchActivity) activity.finish()
        } else {
            onSameActivity()
        }
    }

    fun goBackScene(activity: Activity) {
        if (TutorialSession.stepIndex <= 0) {
            exit(activity)
            return
        }
        TutorialSession.goBack(activity)
        if (applyStep(activity)) {
            if (activity is MatchActivity || activity is PlayerSelectionActivity) {
                activity.finish()
            }
        }
    }

    /**
     * @return true when a different Activity was started
     */
    fun applyStep(activity: Activity): Boolean {
        val step = TutorialSession.currentStep(activity) ?: return false
        if (!step.snapshot.isNullOrBlank()) {
            val snapshot = TutorialSnapshots.load(activity, step.snapshot) ?: return false
            val sameMatch = activity is MatchActivity &&
                activity.intent.getBooleanExtra(MatchActivity.EXTRA_TUTORIAL, false) &&
                activity.intent.getStringExtra(MatchActivity.EXTRA_TUTORIAL_SNAPSHOT) == snapshot.id &&
                activity.intent.getStringExtra(MatchActivity.EXTRA_TUTORIAL_VIEW) == snapshot.view.name
            if (sameMatch) return false
            activity.startActivity(MatchActivity.createTutorialIntent(activity, snapshot))
            return true
        }
        return when (step.scene) {
            "court", "pin" -> {
                if (activity is CourtSelectionActivity) false
                else {
                    activity.startActivity(
                        CourtSelectionActivity.createTutorialIntent(activity).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        },
                    )
                    true
                }
            }
            "players", "config" -> {
                if (activity is PlayerSelectionActivity) false
                else {
                    activity.startActivity(
                        PlayerSelectionActivity.createTutorialIntent(activity).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        },
                    )
                    true
                }
            }
            "finish" -> {
                val snapshot = TutorialSnapshots.load(activity, "finished") ?: return false
                activity.startActivity(MatchActivity.createTutorialIntent(activity, snapshot))
                true
            }
            else -> false
        }
    }

    fun exit(activity: Activity) {
        TutorialSession.stop(activity)
        if (TutorialSession.returnToSettings) {
            activity.startActivity(Intent(activity, SettingsActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            })
        }
        if (activity !is SettingsActivity) activity.finish()
    }
}
