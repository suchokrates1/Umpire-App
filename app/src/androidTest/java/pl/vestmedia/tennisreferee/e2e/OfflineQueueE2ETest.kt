package pl.vestmedia.tennisreferee.e2e

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.domain.match.model.MatchConfig
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.domain.match.model.StatsMode
import pl.vestmedia.tennisreferee.ui.match.MatchActivity

/**
 * Score while the radio is off, then send the queued match once it is back.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class OfflineQueueE2ETest {

    private val backend = E2EBackendClient()
    private lateinit var fixture: TournamentFixture

    @Before
    fun setUp() {
        setAirplane(false)
        overrideUmpireBackend(backend.baseUrl)
        val marker = "E2E-${System.currentTimeMillis()}-offline"
        backend.cleanup(marker)
        fixture = backend.createTournamentFixture(marker)
        runBlocking {
            ApplicationProvider.getApplicationContext<TennisRefereeApp>()
                .matchHistoryRepository
                .deleteAllMatches()
        }
    }

    @After
    fun tearDown() {
        try {
            setAirplane(false)
            if (::fixture.isInitialized) backend.cleanup(fixture.marker)
            runBlocking {
                ApplicationProvider.getApplicationContext<TennisRefereeApp>()
                    .matchHistoryRepository
                    .deleteAllMatches()
            }
        } finally {
            overrideUmpireBackend(null)
            backend.close()
        }
    }

    @Test
    fun pointScoredOfflineArrivesAfterTheRadioReturns() {
        val matchScenario = scenario(
            name = "offline_queue",
            playerIndexes = listOf(0, 1),
            config = MatchConfig(gamesPerSet = 4, setsToWin = 1, statsMode = StatsMode.ADVANCED),
            steps = listOf(Game(true)),
            expectedSets = 0 to 0,
            expectedSetScores = emptyList(),
        )
        val matchState = matchScenario.toMatchState(fixture, courtIndex = 0)
        backend.seedAppCourtSession(matchState.courtId)

        setAirplane(true)
        try {
            ActivityScenario.launch<MatchActivity>(intentFor(matchState)).use {
                UmpireRobot.waitForView(R.id.buttonPlayer1Serves)
                UmpireRobot.clickServerButton(matchScenario.firstServer)
                Thread.sleep(2_000)
                setAirplane(false)
                Thread.sleep(3_000)
                UmpireRobot(doubles = false, firstServer = matchScenario.firstServer).playGame(team1Wins = true)
            }
        } finally {
            setAirplane(false)
        }

        backend.waitForInProgressMatch(
            fixture.marker,
            matchState.getTeam1FullName(),
            matchState.getTeam2FullName(),
        )
    }

    private fun intentFor(matchState: MatchState): Intent {
        val extras = Bundle().apply {
            classLoader = MatchState::class.java.classLoader
            putParcelable(MatchActivity.EXTRA_MATCH_STATE, matchState)
            putBoolean(MatchActivity.EXTRA_IS_DOUBLES, matchState.isDoubles)
        }
        return Intent(ApplicationProvider.getApplicationContext(), MatchActivity::class.java)
            .putExtras(extras)
    }

    private fun setAirplane(enabled: Boolean) {
        val command = if (enabled) {
            "cmd connectivity airplane-mode enable"
        } else {
            "cmd connectivity airplane-mode disable"
        }
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream ->
            stream.readBytes()
        }
    }

    companion object {
        @JvmStatic
        @AfterClass
        fun finishInstrumentation() {
            InstrumentationRegistry.getInstrumentation().finish(Activity.RESULT_OK, Bundle())
        }
    }
}
