package pl.vestmedia.tennisreferee.utils

import android.content.Context
import android.util.Log

/**
 * Survives a failed cold start so the next launch (or adb) can show why the
 * process died on the logo. Native Keystore crashes still will not appear here.
 */
object StartupCrashLog {
    const val FILE_NAME = "startup_crash.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            write(appContext, error)
            previous?.uncaughtException(thread, error)
        }
    }

    fun write(context: Context, error: Throwable) {
        try {
            context.applicationContext.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use { out ->
                out.write(Log.getStackTraceString(error).toByteArray(Charsets.UTF_8))
            }
        } catch (_: Throwable) {
            // Last-resort logging must never take the process down by itself.
        }
    }
}
