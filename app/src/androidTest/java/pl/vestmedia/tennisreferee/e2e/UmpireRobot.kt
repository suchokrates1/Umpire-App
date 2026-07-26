package pl.vestmedia.tennisreferee.e2e

import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.hamcrest.Matchers.allOf
import pl.vestmedia.tennisreferee.R
import java.io.ByteArrayOutputStream

class UmpireRobot(
    private val doubles: Boolean,
    firstServer: Int
) {
    private var serverSlot = firstServer
    private var completedGames = 0
    private var sidesSwapped = false

    fun playGame(team1Wins: Boolean) {
        repeat(4) { playNormalPoint(team1Wins) }
        completeGame()
        dismissAnnouncementIfVisible()
    }

    fun playDeuceGame(team1Wins: Boolean, noAdvantage: Boolean) {
        listOf(true, true, true, false, false, false).forEach { playNormalPoint(it) }
        dismissAnnouncementIfVisible()
        if (noAdvantage) {
            playNormalPoint(team1Wins)
        } else {
            playNormalPoint(team1Wins)
            playNormalPoint(!team1Wins)
            playNormalPoint(team1Wins)
            playNormalPoint(team1Wins)
        }
        completeGame()
        dismissAnnouncementIfVisible()
    }

    fun playTiebreak(points: List<Boolean>) {
        points.forEachIndexed { index, team1Wins ->
            playPoint(team1Wins, tiebreakPointIndex = index + 1)
            dismissAnnouncementIfVisible()
        }
    }

    private fun playNormalPoint(team1Wins: Boolean) {
        playPoint(team1Wins, tiebreakPointIndex = null)
    }

    private fun playPoint(team1Wins: Boolean, tiebreakPointIndex: Int?) {
        dismissDialogIfVisible()
        dismissAnnouncementIfVisible()
        selectServerIfVisible()
        val serverTeam1 = serverSlot == 1 || (doubles && serverSlot == 3)
        if (team1Wins == serverTeam1) {
            clickFirstDisplayed(R.id.buttonAceLeft, R.id.buttonAceRight)
        } else {
            clickFirstDisplayed(R.id.buttonFaultLeft, R.id.buttonFaultRight)
            clickFirstDisplayed(R.id.buttonFaultLeft, R.id.buttonFaultRight)
        }

        if (tiebreakPointIndex != null && tiebreakPointIndex % 2 == 1) {
            advanceServer()
        }
    }

    private fun completeGame() {
        completedGames++
        sidesSwapped = completedGames % 2 == 1
        advanceServer()
    }

    private fun advanceServer() {
        serverSlot = if (doubles) {
            when (serverSlot) {
                1 -> 2
                2 -> 3
                3 -> 4
                else -> 1
            }
        } else {
            if (serverSlot == 1) 2 else 1
        }
    }

    private fun selectServerIfVisible() {
        val buttonId = if (doubles) {
            when {
                !sidesSwapped && serverSlot == 1 -> R.id.buttonPlayer1Serves
                !sidesSwapped && serverSlot == 2 -> R.id.buttonPlayer2Serves
                !sidesSwapped && serverSlot == 3 -> R.id.buttonPlayer3Serves
                !sidesSwapped && serverSlot == 4 -> R.id.buttonPlayer4Serves
                sidesSwapped && serverSlot == 1 -> R.id.buttonPlayer2Serves
                sidesSwapped && serverSlot == 2 -> R.id.buttonPlayer1Serves
                sidesSwapped && serverSlot == 3 -> R.id.buttonPlayer4Serves
                else -> R.id.buttonPlayer3Serves
            }
        } else {
            when {
                !sidesSwapped && serverSlot == 1 -> R.id.buttonPlayer1Serves
                !sidesSwapped && serverSlot == 2 -> R.id.buttonPlayer2Serves
                sidesSwapped && serverSlot == 1 -> R.id.buttonPlayer2Serves
                else -> R.id.buttonPlayer1Serves
            }
        }
        try {
            onView(allOf(withId(buttonId), isDisplayed())).perform(click())
        } catch (_: Throwable) {
            // Already on the serve/rally/basic scoring screen.
        }
    }

    companion object {
        fun clickServerButton(server: Int) {
            val buttonId = when (server) {
                2 -> R.id.buttonPlayer2Serves
                3 -> R.id.buttonPlayer3Serves
                4 -> R.id.buttonPlayer4Serves
                else -> R.id.buttonPlayer1Serves
            }
            waitForView(buttonId)
            onView(withId(buttonId)).perform(click())
        }

        fun clickFirstDisplayed(vararg ids: Int) {
            waitUntil("one of ${ids.joinToString()} displayed", timeoutMs = 10_000) {
                ids.any { id ->
                    try {
                        onView(allOf(withId(id), isDisplayed())).perform(click())
                        true
                    } catch (_: Throwable) {
                        false
                    }
                }
            }
        }

        fun dismissAnnouncementIfVisible() {
            clickIfDisplayed(R.id.buttonAnnouncementContinue, timeoutMs = 2_000)
        }

        fun dismissDialogIfVisible() {
            try {
                onView(allOf(withText("OK"), isDisplayed())).perform(click())
            } catch (_: Throwable) {
                // No blocking dialog is currently visible.
            }
        }

        fun waitForView(id: Int, timeoutMs: Long = 10_000) {
            waitUntil("view $id displayed", timeoutMs) {
                onView(allOf(withId(id), isDisplayed())).check(matches(isDisplayed()))
                true
            }
        }

        fun clickIfDisplayed(id: Int, timeoutMs: Long = 1_000): Boolean {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < deadline) {
                try {
                    onView(allOf(withId(id), isDisplayed())).perform(click())
                    return true
                } catch (_: Throwable) {
                    Thread.sleep(100)
                }
            }
            return false
        }

        fun debugView(view: View): String {
            val chain = generateSequence(view as View?) { it.parent as? View }
                .joinToString(" <- ") { parent ->
                    val resourceName = runCatching { parent.resources.getResourceEntryName(parent.id) }.getOrDefault("no-id")
                    val visibility = when (parent.visibility) {
                        View.VISIBLE -> "VISIBLE"
                        View.INVISIBLE -> "INVISIBLE"
                        View.GONE -> "GONE"
                        else -> parent.visibility.toString()
                    }
                    val childCount = (parent as? ViewGroup)?.childCount?.let { " children=$it" }.orEmpty()
                    "${parent.javaClass.simpleName}#$resourceName visibility=$visibility shown=${parent.isShown} alpha=${parent.alpha} size=${parent.width}x${parent.height}$childCount"
                }
            return "Expected first server button to be shown. $chain"
        }
    }
}

fun waitUntil(description: String, timeoutMs: Long, condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    var lastError: Throwable? = null
    while (System.currentTimeMillis() < deadline) {
        try {
            if (condition()) return
        } catch (error: Throwable) {
            lastError = error
        }
        Thread.sleep(250)
    }
    throw AssertionError("Timed out waiting for $description\n${activeWindowDump()}", lastError)
}

fun activeWindowDump(): String {
    return try {
        val output = ByteArrayOutputStream()
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).dumpWindowHierarchy(output)
        output.toString(Charsets.UTF_8.name()).take(30_000)
    } catch (error: Throwable) {
        "Unable to dump active window: ${error.message}"
    }
}
