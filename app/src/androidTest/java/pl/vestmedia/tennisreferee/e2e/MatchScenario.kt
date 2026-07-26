package pl.vestmedia.tennisreferee.e2e

import pl.vestmedia.tennisreferee.domain.match.model.MatchConfig
import pl.vestmedia.tennisreferee.domain.match.model.MatchState

data class MatchScenario(
    val name: String,
    val playerIndexes: List<Int>,
    val isDoubles: Boolean,
    val isMixedDoubles: Boolean,
    val config: MatchConfig,
    val startInSuperTiebreak: Boolean,
    val firstServer: Int,
    val steps: List<ScenarioStep>,
    val expectedSets: Pair<Int, Int>,
    val expectedSetScores: List<Pair<Int, Int>>,
    val expectedLastTiebreakLoserPoints: Int?,
    val expectedLastSetSuperTiebreak: Boolean
) {
    fun toMatchState(fixture: TournamentFixture, courtIndex: Int? = null): MatchState {
        val selected = playerIndexes.map { fixture.players[it] }
        val courtId = if (courtIndex != null) fixture.courtIdFor(courtIndex) else fixture.nextCourtId()
        val courtName = if (courtIndex != null) {
            fixture.courtNameFor(courtIndex)
        } else {
            "E2E Court ${fixture.courtOrdinal}"
        }
        val state = if (isDoubles) {
            MatchState(
                player1 = selected[0].toPlayer(),
                player2 = selected[2].toPlayer(),
                player3 = selected[1].toPlayer(),
                player4 = selected[3].toPlayer(),
                courtId = courtId,
                courtName = courtName,
                isDoubles = true,
                isMixedDoubles = isMixedDoubles,
                umpireName = "E2E Umpire ${fixture.marker}",
                currentServer = firstServer,
                statsMode = config.statsMode,
                noAdvantage = config.noAdvantage,
                matchConfig = config
            )
        } else {
            MatchState(
                player1 = selected[0].toPlayer(),
                player2 = selected[1].toPlayer(),
                courtId = courtId,
                courtName = courtName,
                umpireName = "E2E Umpire ${fixture.marker}",
                currentServer = firstServer,
                statsMode = config.statsMode,
                noAdvantage = config.noAdvantage,
                matchConfig = config
            )
        }
        if (startInSuperTiebreak) {
            state.isSuperTiebreak = true
        }
        return state
    }
}

sealed interface ScenarioStep
data class Game(val team1Wins: Boolean) : ScenarioStep
data class DeuceGame(val team1Wins: Boolean, val noAdvantage: Boolean) : ScenarioStep
data class Tiebreak(val points: List<Boolean>) : ScenarioStep

enum class MatrixType { SINGLES, DOUBLES, MIXED }

fun scenario(
    name: String,
    playerIndexes: List<Int>,
    config: MatchConfig,
    steps: List<ScenarioStep>,
    expectedSets: Pair<Int, Int>,
    expectedSetScores: List<Pair<Int, Int>>,
    isDoubles: Boolean = false,
    isMixedDoubles: Boolean = false,
    startInSuperTiebreak: Boolean = false,
    firstServer: Int = 1,
    expectedLastTiebreakLoserPoints: Int? = null,
    expectedLastSetSuperTiebreak: Boolean = false
) = MatchScenario(
    name = name,
    playerIndexes = playerIndexes,
    isDoubles = isDoubles,
    isMixedDoubles = isMixedDoubles,
    config = config,
    startInSuperTiebreak = startInSuperTiebreak,
    firstServer = firstServer,
    steps = steps,
    expectedSets = expectedSets,
    expectedSetScores = expectedSetScores,
    expectedLastTiebreakLoserPoints = expectedLastTiebreakLoserPoints,
    expectedLastSetSuperTiebreak = expectedLastSetSuperTiebreak
)
