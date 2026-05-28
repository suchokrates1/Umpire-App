package pl.vestmedia.tennisreferee.ui.match

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import androidx.core.view.isVisible
import pl.vestmedia.tennisreferee.databinding.LayoutServerSelectionBinding

class CourtSideSwapAnimator(
    private val binding: LayoutServerSelectionBinding
) {
    fun animate() {
        val animatedButtons = listOf(
            binding.buttonPlayer1Serves,
            binding.buttonPlayer2Serves,
            binding.buttonPlayer3Serves,
            binding.buttonPlayer4Serves
        ).filter { it.isVisible }

        val fadeOut = animatedButtons.map { button ->
            ObjectAnimator.ofFloat(button, "alpha", 1f, 0f).apply {
                duration = 150
            }
        }

        val fadeIn = animatedButtons.map { button ->
            ObjectAnimator.ofFloat(button, "alpha", 0f, 1f).apply {
                duration = 150
            }
        }

        AnimatorSet().apply {
            playTogether(fadeOut)
            playTogether(fadeIn)
            fadeIn.forEach { play(it).after(fadeOut.first()) }
            start()
        }
    }
}