package pl.vestmedia.tennisreferee.ui.tutorial

import android.app.Activity
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.data.model.Court
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.ui.match.MatchActivity
import pl.vestmedia.tennisreferee.ui.match.MatchView
import pl.vestmedia.tennisreferee.ui.playerselection.PlayerSelectionActivity

/**
 * Single boundary between production screens and the interactive umpire tutorial.
 *
 * Keeps `TutorialSession`, `TutorialCatalog`, `TutorialSnapshots`, and
 * `TutorialOverlayController` encapsulated inside `ui.tutorial` instead of
 * sprinkling singleton checks across production Activities, ViewModels, and dialogs.
 */
object TutorialScreenHost {
    const val COURT_1_ID: String = TutorialCatalog.COURT_1

    fun isActive(activity: Activity): Boolean =
        activity.intent?.getBooleanExtra(TutorialNavigator.EXTRA_TUTORIAL, false) == true ||
            TutorialSession.isActive

    fun pinPromptMessage(activity: Activity): String? =
        if (TutorialSession.isActive) TutorialSession.stringFor(activity, "tutorialPinBody") else null

    sealed interface PinCheckResult {
        data object NotInTutorial : PinCheckResult
        data object WrongPin : PinCheckResult
        data object Accepted : PinCheckResult
    }

    fun checkCourtPin(activity: Activity, pin: String): PinCheckResult {
        if (!TutorialSession.isActive) return PinCheckResult.NotInTutorial
        if (pin != TutorialCatalog.PIN) return PinCheckResult.WrongPin
        TutorialSession.noteAction("enterPin", activity)
        if (TutorialSession.canAdvance(activity)) {
            TutorialSession.goNext(activity)
        }
        activity.startActivity(PlayerSelectionActivity.createTutorialIntent(activity))
        return PinCheckResult.Accepted
    }

    fun createMatchStartIntentIfActive(activity: Activity): android.content.Intent? {
        if (!TutorialSession.isActive) return null
        TutorialSession.noteAction("startMatch", activity)
        if (TutorialSession.canAdvance(activity)) {
            TutorialSession.goNext(activity)
        }
        val snapshot = TutorialSnapshots.load(activity, "serve") ?: return null
        return MatchActivity.createTutorialIntent(activity, snapshot)
    }

    class CourtSelectionSession(
        private val activity: AppCompatActivity,
        private val showPinForCourt: (Court, onShown: (AlertDialog) -> Unit, onDismissed: () -> Unit) -> Unit,
    ) {
        private var overlay: TutorialOverlayController? = null

        fun courts(): List<Court> = TutorialCatalog.courts()

        fun attachAndMaybeOpenPin() {
            overlay = TutorialOverlayController(
                activity = activity,
                onBack = { TutorialNavigator.goBackScene(activity) },
                onNext = {
                    TutorialSession.goNext(activity)
                    val step = TutorialSession.currentStep(activity)
                    if (step?.scene == "pin") {
                        overlay?.refresh()
                        openTutorialPin(TutorialCatalog.courts().first())
                    } else if (TutorialNavigator.applyStep(activity)) {
                        activity.finish()
                    } else {
                        overlay?.refresh()
                    }
                },
                onSkip = { TutorialNavigator.exit(activity) },
            ).also { it.attach() }

            if (TutorialSession.currentStep(activity)?.scene == "pin") {
                openTutorialPin(TutorialCatalog.courts().first())
            }
        }

        fun onCourtTapped(court: Court): Boolean {
            if (!TutorialSession.isActive) return false
            if (court.id != TutorialCatalog.COURT_1) return true
            TutorialSession.noteAction("selectCourt", activity)
            if (TutorialSession.canAdvance(activity)) {
                TutorialSession.goNext(activity)
            }
            overlay?.refresh()
            openTutorialPin(court)
            return true
        }

        fun onBack(): Boolean {
            if (!isActive(activity)) return false
            TutorialNavigator.goBackScene(activity)
            return true
        }

        private fun openTutorialPin(court: Court) {
            showPinForCourt(
                court,
                { dialog ->
                    val host = dialog.findViewById<ViewGroup>(R.id.tutorialPinGuide)
                        ?: dialog.window?.decorView as? ViewGroup
                    if (host != null) overlay?.reparent(host)
                },
                {
                    overlay?.reparent(activity.window.decorView as ViewGroup)
                },
            )
        }
    }

    class PlayerSelectionSession(
        private val activity: AppCompatActivity,
        private val proceedToConfig: () -> Unit,
    ) {
        private var overlay: TutorialOverlayController? = null

        fun players(): List<Player> = TutorialCatalog.players(activity)

        fun attach() {
            overlay = TutorialOverlayController(
                activity = activity,
                onBack = { TutorialNavigator.goBackScene(activity) },
                onNext = {
                    val step = TutorialSession.currentStep(activity)
                    if (step?.scene == "players" || step?.scene == "config") {
                        proceedToConfig()
                        return@TutorialOverlayController
                    }
                    TutorialSession.goNext(activity)
                    if (TutorialNavigator.applyStep(activity)) {
                        activity.finish()
                    } else {
                        overlay?.refresh()
                    }
                },
                onSkip = { TutorialNavigator.exit(activity) },
            ).also { it.attach() }
        }

        fun onRequiredPlayersSelected(): Boolean {
            if (!TutorialSession.isActive) return false
            TutorialSession.noteAction("selectPlayers", activity)
            if (TutorialSession.canAdvance(activity)) {
                TutorialSession.goNext(activity)
            }
            proceedToConfig()
            overlay?.refresh()
            return true
        }

        fun onBack(): Boolean {
            if (!TutorialSession.isActive) return false
            TutorialNavigator.goBackScene(activity)
            return true
        }
    }

    class MatchScreenSession(
        private val activity: AppCompatActivity,
    ) {
        private var overlay: TutorialOverlayController? = null

        val active: Boolean
            get() = isActive(activity)

        fun initialRestoreParams(): Triple<MatchView, String?, Boolean>? {
            if (!activity.intent.getBooleanExtra(MatchActivity.EXTRA_TUTORIAL, false)) return null
            val viewName = activity.intent.getStringExtra(MatchActivity.EXTRA_TUTORIAL_VIEW) ?: MatchView.SERVER_SELECTION.name
            val view = runCatching { MatchView.valueOf(viewName) }.getOrDefault(MatchView.SERVER_SELECTION)
            return Triple(
                view,
                activity.intent.getStringExtra(MatchActivity.EXTRA_TUTORIAL_ANNOUNCEMENT),
                activity.intent.getBooleanExtra(MatchActivity.EXTRA_TUTORIAL_CAN_UNDO, false),
            )
        }

        fun attachIfTutorialIntent() {
            if (!activity.intent.getBooleanExtra(MatchActivity.EXTRA_TUTORIAL, false)) return
            overlay = TutorialOverlayController(
                activity = activity,
                onBack = {
                    TutorialNavigator.goBackScene(activity)
                    overlay?.refresh()
                },
                onNext = {
                    if (TutorialSession.isLast(activity)) {
                        TutorialNavigator.exit(activity)
                        return@TutorialOverlayController
                    }
                    TutorialSession.goNext(activity)
                    if (TutorialNavigator.applyStep(activity)) {
                        activity.finish()
                    } else {
                        overlay?.refresh()
                    }
                },
                onSkip = { TutorialNavigator.exit(activity) },
            ).also { it.attach() }
        }

        fun noteAction(action: String): Boolean {
            if (!active) return false
            TutorialNavigator.afterRequiredAction(activity, action) {
                overlay?.refresh()
            }
            return true
        }

        fun onRetirementPicked(): Boolean {
            if (!active) return false
            TutorialSession.noteAction("pickRetirement", activity)
            TutorialSession.jumpToLast(activity)
            if (TutorialNavigator.applyStep(activity)) {
                activity.finish()
            } else {
                overlay?.refresh()
            }
            return true
        }

        fun onNextMatch(): Boolean {
            if (!active) return false
            TutorialNavigator.exit(activity)
            return true
        }

        fun onBackPressed(): Boolean {
            if (!active) return false
            TutorialNavigator.goBackScene(activity)
            overlay?.refresh()
            return true
        }
    }
}
