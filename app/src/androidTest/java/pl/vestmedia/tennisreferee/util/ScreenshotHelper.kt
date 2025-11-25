package pl.vestmedia.tennisreferee.util

import android.app.Activity
import android.graphics.Bitmap
import android.os.Environment
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper do robienia screenszotów podczas testów UI
 */
object ScreenshotHelper {
    
    private val screenshotDir: File by lazy {
        val externalDir = InstrumentationRegistry.getInstrumentation().targetContext
            .getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        File(externalDir, "screenshots").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    /**
     * Robi screenshot i zapisuje z podaną nazwą
     */
    fun takeScreenshot(name: String) {
        try {
            val activity = getCurrentActivity() ?: return
            
            Thread.sleep(500) // Krótka pauza na renderowanie
            
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                try {
                    val view = activity.window.decorView.rootView
                    view.isDrawingCacheEnabled = true
                    view.buildDrawingCache(true)
                    
                    val bitmap = Bitmap.createBitmap(view.drawingCache)
                    view.isDrawingCacheEnabled = false
                    
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val filename = "${name}_$timestamp.png"
                    val file = File(screenshotDir, filename)
                    
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    
                    println("Screenshot saved: ${file.absolutePath}")
                } catch (e: Exception) {
                    println("Failed to take screenshot: ${e.message}")
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            println("Error in takeScreenshot: ${e.message}")
        }
    }
    
    /**
     * Pobiera aktualnie widoczną aktywność
     */
    private fun getCurrentActivity(): Activity? {
        var currentActivity: Activity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val resumedActivities = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
            if (resumedActivities.iterator().hasNext()) {
                currentActivity = resumedActivities.iterator().next()
            }
        }
        return currentActivity
    }
}
