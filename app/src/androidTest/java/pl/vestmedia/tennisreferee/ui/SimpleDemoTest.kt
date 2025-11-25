package pl.vestmedia.tennisreferee.ui

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.vestmedia.tennisreferee.ui.language.LanguageSelectionActivity
import pl.vestmedia.tennisreferee.util.ScreenshotHelper

/**
 * Prosty test demonstrujący działanie aplikacji i robienie screenszotów
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SimpleDemoTest {
    
    @get:Rule
    val activityRule = ActivityScenarioRule(LanguageSelectionActivity::class.java)
    
    @Test
    fun demonstrateAppFlow() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val screenshotDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        
        println("=================================================")
        println("SCREENSHOT DIRECTORY: ${screenshotDir?.absolutePath}/screenshots")
        println("=================================================")
        
        // Czekaj na załadowanie
        Thread.sleep(2000)
        
        // Zrób screenshot ekranu startowego
        ScreenshotHelper.takeScreenshot("demo_01_language_selection")
        
        println("Screenshot taken: demo_01_language_selection")
        
        Thread.sleep(2000)
        
        // Drugi screenshot
        ScreenshotHelper.takeScreenshot("demo_02_language_selection_after_2s")
        
        println("Screenshot taken: demo_02_language_selection_after_2s")
        
        Thread.sleep(2000)
        
        println("=================================================")
        println("Test completed. Check screenshots at:")
        println("${screenshotDir?.absolutePath}/screenshots")
        println("=================================================")
        
        // Lista plików w katalogu
        val screenshotsFolder = java.io.File(screenshotDir, "screenshots")
        if (screenshotsFolder.exists()) {
            println("Files in screenshots folder:")
            screenshotsFolder.listFiles()?.forEach {
                println("  - ${it.name} (${it.length()} bytes)")
            }
        } else {
            println("Screenshots folder does not exist yet!")
        }
    }
}
