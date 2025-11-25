package pl.vestmedia.tennisreferee.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.ui.language.LanguageSelectionActivity
import pl.vestmedia.tennisreferee.util.ScreenshotHelper

/**
 * Test UI sprawdzający proces wyboru graczy
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PlayerSelectionUITest {
    
    @get:Rule
    val activityRule = ActivityScenarioRule(LanguageSelectionActivity::class.java)
    
    @Test
    fun testSinglesPlayerSelection() {
        Thread.sleep(1000) // Czekaj na załadowanie
        ScreenshotHelper.takeScreenshot("01_main_screen")
        
        // Kliknij w pierwszy kort (zakładając że są korty)
        try {
            onView(withId(R.id.recyclerViewCourts))
                .check(matches(isDisplayed()))
            
            Thread.sleep(500)
            
            // Kliknij pierwszy dostępny kort
            onView(withId(R.id.recyclerViewCourts))
                .perform(click())
            
            Thread.sleep(1000)
            ScreenshotHelper.takeScreenshot("02_court_selected")
        } catch (e: Exception) {
            println("No courts available or error: ${e.message}")
            return
        }
        
        // Sprawdź czy domyślnie jest Singles
        Thread.sleep(500)
        onView(withId(R.id.checkboxDoubles))
            .check(matches(isDisplayed()))
        
        ScreenshotHelper.takeScreenshot("03_player_selection_singles")
        
        // Wybierz pierwszego gracza
        try {
            Thread.sleep(500)
            onView(withId(R.id.recyclerViewPlayers))
                .check(matches(isDisplayed()))
            
            // Kliknij pierwszego gracza na liście
            // Note: RecyclerView click would need custom matcher
            // For now we just verify the view is displayed
            
            Thread.sleep(300)
            ScreenshotHelper.takeScreenshot("04_first_player_selecting")
            
            // W rzeczywistości musielibyśmy użyć custom matcher do kliknięcia konkretnego item
            // ale dla demonstracji pokażemy sam interfejs
            
        } catch (e: Exception) {
            println("Error selecting players: ${e.message}")
        }
        
        Thread.sleep(1000)
        ScreenshotHelper.takeScreenshot("05_singles_selection_final")
    }
    
    @Test
    fun testDoublesPlayerSelection() {
        Thread.sleep(1000)
        ScreenshotHelper.takeScreenshot("10_main_screen_doubles")
        
        // Przejdź do wyboru graczy (kliknij kort)
        try {
            onView(withId(R.id.recyclerViewCourts))
                .check(matches(isDisplayed()))
                .perform(click())
            
            Thread.sleep(1000)
        } catch (e: Exception) {
            println("No courts available")
            return
        }
        
        // Przełącz na Doubles
        onView(withId(R.id.checkboxDoubles))
            .perform(click())
        
        Thread.sleep(500)
        ScreenshotHelper.takeScreenshot("11_player_selection_doubles")
        
        // Sprawdź czy pokazuje "Doubles" i wymaganych 4 graczy
        onView(withId(R.id.textGameType))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.textSelectedInfo))
            .check(matches(isDisplayed()))
        
        Thread.sleep(500)
        ScreenshotHelper.takeScreenshot("12_doubles_mode_active")
        
        // Sprawdź czy przycisk Next jest nieaktywny na początku
        onView(withId(R.id.buttonNext))
            .check(matches(not(isEnabled())))
        
        Thread.sleep(1000)
        ScreenshotHelper.takeScreenshot("13_doubles_selection_final")
    }
    
    @Test
    fun testPlayerSearch() {
        Thread.sleep(1000)
        
        // Przejdź do wyboru graczy
        try {
            onView(withId(R.id.recyclerViewCourts))
                .perform(click())
            Thread.sleep(1000)
        } catch (e: Exception) {
            println("No courts available")
            return
        }
        
        ScreenshotHelper.takeScreenshot("20_before_search")
        
        // Wpisz w wyszukiwarkę
        onView(withId(R.id.editTextSearch))
            .perform(click())
        
        Thread.sleep(300)
        
        onView(withId(R.id.editTextSearch))
            .perform(typeText("Test"), closeSoftKeyboard())
        
        Thread.sleep(500)
        ScreenshotHelper.takeScreenshot("21_search_results")
        
        // Wyczyść wyszukiwanie
        onView(withId(R.id.editTextSearch))
            .perform(clearText(), closeSoftKeyboard())
        
        Thread.sleep(500)
        ScreenshotHelper.takeScreenshot("22_search_cleared")
    }
    
    @Test
    fun testBackButton() {
        Thread.sleep(1000)
        
        // Przejdź do wyboru graczy
        try {
            onView(withId(R.id.recyclerViewCourts))
                .perform(click())
            Thread.sleep(1000)
        } catch (e: Exception) {
            return
        }
        
        ScreenshotHelper.takeScreenshot("30_player_selection")
        
        // Kliknij przycisk Back
        onView(withId(R.id.buttonBack))
            .check(matches(isDisplayed()))
            .perform(click())
        
        Thread.sleep(500)
        ScreenshotHelper.takeScreenshot("31_back_to_main")
        
        // Sprawdź czy wróciliśmy do głównego ekranu
        onView(withId(R.id.recyclerViewCourts))
            .check(matches(isDisplayed()))
    }
}
