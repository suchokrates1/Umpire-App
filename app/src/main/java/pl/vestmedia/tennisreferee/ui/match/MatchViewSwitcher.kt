package pl.vestmedia.tennisreferee.ui.match

import android.view.View
import androidx.core.view.isVisible
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.databinding.ActivityMatchBinding

class MatchViewSwitcher(
    private val binding: ActivityMatchBinding,
    private val getState: () -> MatchState?,
    private val renderAnnouncement: (MatchState) -> Unit,
    private val renderServe: (MatchState, Boolean) -> Unit,
    private val renderBasicScoring: (MatchState) -> Unit,
    private val renderMatchFinished: (MatchState) -> Unit,
    private val onViewShown: (MatchView) -> Unit
) {
    fun show(view: MatchView) {
        onViewShown(view)
        hideAllScoringViews()

        binding.layoutScoreboard.root.visibility = if (view == MatchView.SERVER_SELECTION) {
            View.GONE
        } else {
            View.VISIBLE
        }

        getState()?.let { state ->
            when (view) {
                MatchView.SERVER_SELECTION -> animateViewTransition(binding.layoutServerSelection.root, View.VISIBLE)
                MatchView.ANNOUNCEMENT -> {
                    renderAnnouncement(state)
                    animateViewTransition(binding.layoutAnnouncement.root, View.VISIBLE)
                }
                MatchView.SERVE -> {
                    animateViewTransition(binding.layoutServe.root, View.VISIBLE)
                    renderServe(state, false)
                }
                MatchView.RALLY -> animateViewTransition(binding.layoutRally.root, View.VISIBLE)
                MatchView.BASIC_SCORING -> {
                    animateViewTransition(binding.layoutBasicScoring.root, View.VISIBLE)
                    renderBasicScoring(state)
                }
                MatchView.MATCH_FINISHED -> {
                    animateViewTransition(binding.layoutMatchFinished.root, View.VISIBLE)
                    renderMatchFinished(state)
                }
            }
        }
    }

    private fun hideAllScoringViews() {
        animateViewTransition(binding.layoutServerSelection.root, View.GONE)
        animateViewTransition(binding.layoutServe.root, View.GONE)
        animateViewTransition(binding.layoutRally.root, View.GONE)
        animateViewTransition(binding.layoutBasicScoring.root, View.GONE)
        animateViewTransition(binding.layoutMatchFinished.root, View.GONE)
        animateViewTransition(binding.layoutAnnouncement.root, View.GONE)
    }

    private fun animateViewTransition(view: View, newVisibility: Int) {
        when (newVisibility) {
            View.VISIBLE -> {
                view.alpha = 0f
                view.translationX = 100f
                view.visibility = View.VISIBLE
                view.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(300)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }
            View.GONE -> {
                if (view.isVisible) {
                    view.animate()
                        .alpha(0f)
                        .translationX(-100f)
                        .setDuration(200)
                        .setInterpolator(android.view.animation.AccelerateInterpolator())
                        .withEndAction {
                            view.visibility = View.GONE
                            view.alpha = 1f
                            view.translationX = 0f
                        }
                        .start()
                } else {
                    view.visibility = View.GONE
                }
            }
        }
    }
}