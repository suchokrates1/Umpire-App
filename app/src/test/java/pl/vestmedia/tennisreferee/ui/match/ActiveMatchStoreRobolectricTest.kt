package pl.vestmedia.tennisreferee.ui.match

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.startup.StartupTestApp

@RunWith(RobolectricTestRunner::class)
@Config(application = StartupTestApp::class, sdk = [34])
class ActiveMatchStoreRobolectricTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs = context.getSharedPreferences("active_match_store", Context.MODE_PRIVATE)
    private val store = ActiveMatchStore(context, dispatcher = Dispatchers.Unconfined)

    private val playerOne = Player(id = 1, name = "Kowalski", firstName = "Jan", lastName = "Kowalski")
    private val playerTwo = Player(id = 2, name = "Nowak", firstName = "Adam", lastName = "Nowak")

    @Before
    @After
    fun cleanUp() {
        prefs.edit().clear().commit()
        store.resetInMemoryForTests()
    }

    @Test
    fun savingANewMatchPrunesAbandonedOlderMatchesFromPreferences() {
        val abandoned = MatchState(
            clientMatchUuid = "abandoned-1",
            player1 = playerOne,
            player2 = playerTwo,
            courtId = "t9-1",
            courtName = "Court 1",
        ).apply { player1Points = 2 }
        store.save(abandoned)
        assertEquals(setOf("match_abandoned-1", "last_match_uuid"), prefs.all.keys)

        val current = MatchState(
            clientMatchUuid = "current-2",
            player1 = playerOne,
            player2 = playerTwo,
            courtId = "t9-1",
            courtName = "Court 1",
        ).apply { player2Points = 1 }
        store.save(current)

        assertEquals(setOf("match_current-2", "last_match_uuid"), prefs.all.keys)
        assertNull(store.get("abandoned-1"))
        assertEquals("current-2", store.getLast()?.clientMatchUuid)
    }

    @Test
    fun mutatingLiveMatchAfterSaveDoesNotCorruptStoredSnapshot() {
        val live = MatchState(
            clientMatchUuid = "snap-1",
            player1 = playerOne,
            player2 = playerTwo,
            courtId = "t9-1",
            courtName = "Court 1",
        ).apply { player1Points = 1 }
        store.save(live)

        live.player1Points = 99
        live.player1Stats.aces = 42

        val restored = store.getLast()!!
        assertEquals(1, restored.player1Points)
        assertEquals(0, restored.player1Stats.aces)
    }
}
