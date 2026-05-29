package pl.vestmedia.tennisreferee.ui.match

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.TextView
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.databinding.LayoutScoreboardBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScoreboardRenderer(
    private val context: Context,
    private val binding: LayoutScoreboardBinding
) {
    private val metadataDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    private var lastActiveSet = 0

    fun render(state: MatchState) {
        renderPlayerHeaders(state)
        renderServerIcons(state)
        renderMatchMetadata(state)
        renderPoints(state)
        renderSets(state)
        renderGameMode(state)
    }

    private fun renderPlayerHeaders(state: MatchState) {
        if (state.isDoubles) {
            binding.textPlayer1Flag.text = "👥"
            binding.textPlayer2Flag.text = "👥"
            binding.textPlayer1Name.text = state.getTeam1ServerAwareDisplayName()
            binding.textPlayer2Name.text = state.getTeam2ServerAwareDisplayName()
            return
        }

        binding.textPlayer1Flag.text = getCountryFlag(state.player1.flag)
        binding.textPlayer2Flag.text = getCountryFlag(state.player2.flag)
        binding.textPlayer1Name.text = state.player1.getDisplayName()
        binding.textPlayer2Name.text = state.player2.getDisplayName()
    }

    private fun renderServerIcons(state: MatchState) {
        if (state.isDoubles) {
            binding.imagePlayer1ServerIcon.visibility = View.GONE
            binding.imagePlayer2ServerIcon.visibility = View.GONE
            return
        }

        binding.imagePlayer1ServerIcon.visibility = if (state.isPlayer1Serving) View.VISIBLE else View.INVISIBLE
        binding.imagePlayer2ServerIcon.visibility = if (!state.isPlayer1Serving) View.VISIBLE else View.INVISIBLE
    }

    private fun renderMatchMetadata(state: MatchState) {
        val metadataParts = mutableListOf(
            when {
                state.isMixedDoubles -> context.getString(R.string.match_type_mixed)
                state.isDoubles -> context.getString(R.string.match_type_doubles)
                else -> context.getString(R.string.match_type_singles)
            }
        )

        state.umpireName?.takeIf { it.isNotBlank() }?.let {
            metadataParts.add(context.getString(R.string.match_metadata_umpire, it))
        }
        state.manualStartTime?.let {
            metadataParts.add(context.getString(R.string.match_metadata_datetime, metadataDateFormat.format(Date(it))))
        }

        binding.textMatchMetadata.text = metadataParts.joinToString(" • ")
        binding.textMatchMetadata.visibility = if (metadataParts.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun renderPoints(state: MatchState) {
        animateScoreChange(binding.textPlayer1Points, state.getPlayer1PointsDisplay())
        animateScoreChange(binding.textPlayer2Points, state.getPlayer2PointsDisplay())
    }

    private fun renderSets(state: MatchState) {
        val currentSetIndex = state.setsHistory.size

        if (state.setsHistory.isEmpty()) {
            binding.textPlayer1Set1.text = state.player1Games.toString()
            binding.textPlayer2Set1.text = state.player2Games.toString()
        } else {
            val set1 = state.setsHistory[0]
            binding.textPlayer1Set1.text = formatSetScore(
                games = set1.player1Games,
                opponentGames = set1.player2Games,
                tiebreakLoserPoints = set1.tiebreakLoserPoints
            )
            binding.textPlayer2Set1.text = formatSetScore(
                games = set1.player2Games,
                opponentGames = set1.player1Games,
                tiebreakLoserPoints = set1.tiebreakLoserPoints
            )
        }

        if (state.setsHistory.size == 1) {
            binding.textPlayer1Set2.text = state.player1Games.toString()
            binding.textPlayer2Set2.text = state.player2Games.toString()
        } else if (state.setsHistory.size > 1) {
            val set2 = state.setsHistory[1]
            binding.textPlayer1Set2.text = formatSetScore(
                games = set2.player1Games,
                opponentGames = set2.player2Games,
                tiebreakLoserPoints = set2.tiebreakLoserPoints
            )
            binding.textPlayer2Set2.text = formatSetScore(
                games = set2.player2Games,
                opponentGames = set2.player1Games,
                tiebreakLoserPoints = set2.tiebreakLoserPoints
            )
        } else {
            binding.textPlayer1Set2.text = context.getString(R.string.zero_score)
            binding.textPlayer2Set2.text = context.getString(R.string.zero_score)
        }

        highlightActiveSet(currentSetIndex)
    }

    private fun renderGameMode(state: MatchState) {
        when {
            state.isSuperTiebreak -> {
                binding.textGameMode.text = context.getString(
                    R.string.super_tiebreak_mode,
                    state.matchConfig.superTiebreakPoints
                )
                binding.textGameMode.visibility = View.VISIBLE
            }
            state.isTiebreak -> {
                binding.textGameMode.text = context.getString(
                    R.string.tiebreak_mode,
                    state.matchConfig.tiebreakPoints
                )
                binding.textGameMode.visibility = View.VISIBLE
            }
            else -> {
                binding.textGameMode.text = ""
                binding.textGameMode.visibility = View.GONE
            }
        }
    }

    private fun animateScoreChange(view: View, newText: String) {
        if (view is TextView) {
            view.text = newText
        }

        view.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(150)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    private fun highlightActiveSet(setIndex: Int) {
        val accentColor = context.resources.getColor(R.color.accent, context.theme)
        val transparentColor = Color.TRANSPARENT

        if (setIndex != lastActiveSet && lastActiveSet < 2) {
            when (lastActiveSet) {
                0 -> {
                    binding.backgroundPlayer1Set1.animate().alpha(0f).setDuration(200).start()
                    binding.backgroundPlayer2Set1.animate().alpha(0f).setDuration(200).start()
                }
                1 -> {
                    binding.backgroundPlayer1Set2.animate().alpha(0f).setDuration(200).start()
                    binding.backgroundPlayer2Set2.animate().alpha(0f).setDuration(200).start()
                }
            }
        }

        when (setIndex) {
            0 -> {
                binding.backgroundPlayer1Set1.setBackgroundColor(accentColor)
                binding.backgroundPlayer2Set1.setBackgroundColor(accentColor)
                binding.backgroundPlayer1Set1.alpha = 0.3f
                binding.backgroundPlayer2Set1.alpha = 0.3f

                binding.backgroundPlayer1Set2.setBackgroundColor(transparentColor)
                binding.backgroundPlayer2Set2.setBackgroundColor(transparentColor)

                if (lastActiveSet != 0) {
                    binding.backgroundPlayer1Set1.alpha = 0f
                    binding.backgroundPlayer2Set1.alpha = 0f
                    binding.backgroundPlayer1Set1.animate().alpha(0.3f).setDuration(300).start()
                    binding.backgroundPlayer2Set1.animate().alpha(0.3f).setDuration(300).start()
                }
            }
            1 -> {
                binding.backgroundPlayer1Set1.setBackgroundColor(transparentColor)
                binding.backgroundPlayer2Set1.setBackgroundColor(transparentColor)

                binding.backgroundPlayer1Set2.setBackgroundColor(accentColor)
                binding.backgroundPlayer2Set2.setBackgroundColor(accentColor)
                binding.backgroundPlayer1Set2.alpha = 0.3f
                binding.backgroundPlayer2Set2.alpha = 0.3f

                if (lastActiveSet != 1) {
                    binding.backgroundPlayer1Set2.alpha = 0f
                    binding.backgroundPlayer2Set2.alpha = 0f
                    binding.backgroundPlayer1Set2.animate().alpha(0.3f).setDuration(300).start()
                    binding.backgroundPlayer2Set2.animate().alpha(0.3f).setDuration(300).start()
                }
            }
            else -> {
                binding.backgroundPlayer1Set1.setBackgroundColor(transparentColor)
                binding.backgroundPlayer2Set1.setBackgroundColor(transparentColor)
                binding.backgroundPlayer1Set2.setBackgroundColor(transparentColor)
                binding.backgroundPlayer2Set2.setBackgroundColor(transparentColor)
            }
        }

        lastActiveSet = setIndex
    }

    private fun formatSetScore(games: Int, opponentGames: Int, tiebreakLoserPoints: Int?): String {
        val suffix = if (tiebreakLoserPoints != null && games < opponentGames) {
            context.getString(R.string.scoreboard_tiebreak_suffix, tiebreakLoserPoints)
        } else {
            ""
        }
        return context.getString(R.string.scoreboard_set_score, games, suffix)
    }

    private fun getCountryFlag(countryCode: String?): String {
        if (countryCode.isNullOrEmpty() || countryCode.length != 2) return ""

        val upperCode = countryCode.uppercase()
        val firstChar = Character.codePointAt(upperCode, 0) - 0x41 + 0x1F1E6
        val secondChar = Character.codePointAt(upperCode, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    }
}