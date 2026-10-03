package pl.vestmedia.tennisreferee.ui.match

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.gson.Gson
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.domain.match.model.MatchConfig
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.domain.match.model.SetScore

/**
 * The upgrade a referee does between rounds: the tablet already holds a match written by
 * the previous release. Reading it back is the difference between resuming the set and
 * starting it again.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class ActiveMatchStoreMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs = context.getSharedPreferences("active_match_store", Context.MODE_PRIVATE)
    private val store = ActiveMatchStore(context)

    private val match = MatchState(
        matchId = 4711,
        clientMatchUuid = "7b1f0f2e-0000-4000-8000-00000000abcd",
        player1 = Player(id = 7, name = "Ciborowski", firstName = "Mateusz", lastName = "Ciborowski"),
        player2 = Player(id = 11, name = "Zgrzebska", firstName = "Dajana", lastName = "Zgrzebska"),
        courtId = "t32-2",
        courtName = "Kort 2",
        matchConfig = MatchConfig(gamesPerSet = 4),
        player1Sets = 1,
        player1Games = 3,
        player2Games = 2,
        player1Points = 3,
        setsHistory = mutableListOf(SetScore(setNumber = 1, player1Games = 4, player2Games = 2)),
    )

    @Before
    @After
    fun clearStore() {
        prefs.edit().clear().commit()
    }

    @Test
    fun a_match_written_by_the_previous_release_is_resumed() {
        prefs.edit()
            .putString("match_${match.clientMatchUuid}", Gson().toJson(match))
            .putString("last_match_uuid", match.clientMatchUuid)
            .commit()

        val restored = store.getLast()

        assertNotNull("the match the referee was playing must come back", restored)
        assertEquals(match, restored)
    }

    @Test
    fun what_this_release_writes_is_read_back() {
        store.save(match)

        assertEquals(match, store.getLast())
        assertEquals(match, store.get(match.clientMatchUuid))
    }

    @Test
    fun a_payload_that_is_not_a_match_is_dropped_rather_than_crashing_the_app() {
        prefs.edit()
            .putString("match_${match.clientMatchUuid}", "{\"this\":\"is not a match\"}")
            .putString("last_match_uuid", match.clientMatchUuid)
            .commit()

        assertNull(store.getLast())
    }

    @Test
    fun clearing_one_match_leaves_nothing_to_resume() {
        store.save(match)

        store.clear(match.clientMatchUuid)

        assertNull(store.getLast())
        assertNull(store.get(match.clientMatchUuid))
    }
}
