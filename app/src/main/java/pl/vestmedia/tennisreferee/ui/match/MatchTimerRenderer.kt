package pl.vestmedia.tennisreferee.ui.match

import android.os.Handler
import android.os.Looper
import android.view.View
import pl.vestmedia.tennisreferee.data.model.MatchState
import pl.vestmedia.tennisreferee.databinding.LayoutScoreboardBinding
import java.util.Locale

class MatchTimerRenderer(
    private val binding: LayoutScoreboardBinding,
    private val getState: () -> MatchState?
) {
    private val timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    fun render(state: MatchState) {
        if (state.matchStartTime > 0 && !state.isMatchFinished) {
            binding.textMatchTimer.visibility = View.VISIBLE
            startTimerUpdates()
        } else if (state.isMatchFinished) {
            clear()
            binding.textMatchTimer.text = formatDuration(state.matchDuration)
            binding.textMatchTimer.visibility = View.VISIBLE
        }
    }

    fun clear() {
        timerRunnable?.let {
            timerHandler.removeCallbacks(it)
            timerRunnable = null
        }
    }

    private fun startTimerUpdates() {
        if (timerRunnable != null) return

        timerRunnable = object : Runnable {
            override fun run() {
                val state = getState() ?: return
                if (state.matchStartTime > 0 && !state.isMatchFinished) {
                    val elapsed = System.currentTimeMillis() - state.matchStartTime
                    binding.textMatchTimer.text = formatDuration(elapsed)
                    timerHandler.postDelayed(this, 1000)
                }
            }
        }
        timerHandler.post(timerRunnable!!)
    }

    private fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = durationMs / (1000 * 60 * 60)

        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}