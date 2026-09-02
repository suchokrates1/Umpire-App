package pl.vestmedia.tennisreferee.ui.tutorial

import android.content.Context
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.data.model.Court
import pl.vestmedia.tennisreferee.data.model.Player

object TutorialCatalog {
    const val PIN = "1234"
    const val COURT_1 = "tutorial-1"
    const val COURT_2 = "tutorial-2"
    const val MATCH_UUID = "tutorial-demo-match"
    const val PLAYER_1_ID = 9001
    const val PLAYER_2_ID = 9002

    fun courts(): List<Court> = listOf(
        Court(id = COURT_1, name = "1", isAvailable = true),
        Court(id = COURT_2, name = "2", isAvailable = true),
    )

    fun players(context: Context): List<Player> = listOf(
        Player(
            id = PLAYER_1_ID,
            name = "${context.getString(R.string.tutorial_demo_player1_first)} ${context.getString(R.string.tutorial_demo_player1_last)}",
            firstName = context.getString(R.string.tutorial_demo_player1_first),
            lastName = context.getString(R.string.tutorial_demo_player1_last),
            flag = "IT",
            gender = "F",
        ),
        Player(
            id = PLAYER_2_ID,
            name = "${context.getString(R.string.tutorial_demo_player2_first)} ${context.getString(R.string.tutorial_demo_player2_last)}",
            firstName = context.getString(R.string.tutorial_demo_player2_first),
            lastName = context.getString(R.string.tutorial_demo_player2_last),
            flag = "PL",
            gender = "M",
        ),
    )
}
