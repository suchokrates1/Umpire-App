package pl.vestmedia.tennisreferee.ui.playerselection

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.MatchConfig
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.ui.match.ActiveMatchStore
import pl.vestmedia.tennisreferee.ui.match.MatchActivity
import pl.vestmedia.tennisreferee.utils.AppLogger

/**
 * Handles next-match result from MatchActivity and launching / resuming a match
 * with a chosen (or reused) MatchConfig.
 */
class NextMatchController(
    private val activity: AppCompatActivity,
    private val activeMatchStore: ActiveMatchStore,
    private val matchLauncher: ActivityResultLauncher<Intent>,
    private val getCourtId: () -> String,
    private val getCourtName: () -> String,
    private val getSelectedScheduleId: () -> Int?,
    private val getIsDoubles: () -> Boolean,
    private val getTeam1Name: () -> String? = { null },
    private val getTeam2Name: () -> String? = { null },
    private val isMixedDoublesSelection: (List<Player>) -> Boolean,
    private val onPrepareForNextMatch: (reuseSetup: Boolean) -> Unit
) {
    var lastStartedMatchConfig: MatchConfig? = null
        private set

    fun handleMatchResult(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }

        when (MatchActivity.resultAction(data)) {
            MatchActivity.RESULT_NEXT_MATCH_SAME_SETUP -> onPrepareForNextMatch(true)
            MatchActivity.RESULT_NEXT_MATCH_NEW_SETUP -> onPrepareForNextMatch(false)
        }
    }

    fun resolveSavedConfigForNextMatch(
        reuseSetup: Boolean,
        lastStarted: MatchConfig?
    ): MatchConfig? {
        return if (reuseSetup) lastStarted else null
    }

    fun startMatchWithConfig(
        selectedPlayers: List<Player>,
        config: MatchConfig,
        umpireName: String = "",
        manualStartTime: Long? = null
    ) {
        val isDoublesMatch = getIsDoubles()
        val isMixedDoubles = isDoublesMatch && isMixedDoublesSelection(selectedPlayers)
        val playerNames = selectedPlayers.joinToString(", ") { it.getDisplayName() }
        lastStartedMatchConfig = config
        val scheduleId = getSelectedScheduleId()
        AppLogger.navigate(
            "PlayerSelection",
            "Match",
            "players=[$playerNames] doubles=$isDoublesMatch mixed=$isMixedDoubles schedule=${scheduleId ?: "-"} umpire=${umpireName.ifBlank { "-" }} config=$config"
        )

        val matchState = if (isDoublesMatch && selectedPlayers.size == 4) {
            MatchState(
                player1 = selectedPlayers[0],
                player2 = selectedPlayers[2],
                player3 = selectedPlayers[1],
                player4 = selectedPlayers[3],
                courtId = getCourtId(),
                courtName = getCourtName(),
                scheduleId = scheduleId,
                isDoubles = true,
                isMixedDoubles = isMixedDoubles,
                team1Name = getTeam1Name(),
                team2Name = getTeam2Name(),
                umpireName = umpireName.ifBlank { null },
                manualStartTime = manualStartTime,
                currentServer = 1,
                statsMode = config.statsMode,
                noAdvantage = config.noAdvantage,
                matchConfig = config
            )
        } else {
            MatchState(
                player1 = selectedPlayers[0],
                player2 = selectedPlayers[1],
                courtId = getCourtId(),
                courtName = getCourtName(),
                scheduleId = scheduleId,
                isDoubles = false,
                umpireName = umpireName.ifBlank { null },
                manualStartTime = manualStartTime,
                statsMode = config.statsMode,
                noAdvantage = config.noAdvantage,
                matchConfig = config
            )
        }

        if (config.tiebreakOnly) {
            matchState.isSuperTiebreak = true
        }

        if (pl.vestmedia.tennisreferee.ui.tutorial.TutorialSession.isActive) {
            pl.vestmedia.tennisreferee.ui.tutorial.TutorialSession.noteAction("startMatch", activity)
            if (pl.vestmedia.tennisreferee.ui.tutorial.TutorialSession.canAdvance(activity)) {
                pl.vestmedia.tennisreferee.ui.tutorial.TutorialSession.goNext(activity)
            }
            val snapshot = pl.vestmedia.tennisreferee.ui.tutorial.TutorialSnapshots.load(activity, "serve")
            if (snapshot != null) {
                matchLauncher.launch(MatchActivity.createTutorialIntent(activity, snapshot))
                return
            }
        }

        activeMatchStore.save(matchState)
        matchLauncher.launch(MatchActivity.createIntent(activity, matchState.clientMatchUuid, isDoublesMatch))
    }
}
