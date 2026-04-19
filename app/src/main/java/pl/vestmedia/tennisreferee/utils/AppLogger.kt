package pl.vestmedia.tennisreferee.utils

import android.util.Log

/**
 * Centralne logowanie akcji sędziów - każdy przycisk, ekran, decyzja.
 * Tag: TennisRef — łatwy do filtrowania w logcat.
 */
object AppLogger {
    private const val TAG = "TennisRef"

    /** Sędzia przeszedł na nowy ekran */
    fun screen(name: String, details: String? = null) {
        val msg = "SCREEN ▸ $name" + (details?.let { " | $it" } ?: "")
        Log.i(TAG, msg)
    }

    /** Sędzia nacisnął przycisk */
    fun button(screen: String, button: String, details: String? = null) {
        val msg = "BUTTON ▸ $screen ▸ $button" + (details?.let { " | $it" } ?: "")
        Log.i(TAG, msg)
    }

    /** Akcja punktowa / scoringowa */
    fun action(screen: String, action: String, details: String? = null) {
        val msg = "ACTION ▸ $screen ▸ $action" + (details?.let { " | $it" } ?: "")
        Log.i(TAG, msg)
    }

    /** Wywołanie API */
    fun api(endpoint: String, result: String) {
        Log.d(TAG, "API ▸ $endpoint ▸ $result")
    }

    /** Dialog pokazany / wybór */
    fun dialog(name: String, choice: String? = null) {
        val msg = "DIALOG ▸ $name" + (choice?.let { " ▸ $choice" } ?: "")
        Log.i(TAG, msg)
    }

    /** Nawigacja między Activity */
    fun navigate(from: String, to: String, extras: String? = null) {
        val msg = "NAV ▸ $from → $to" + (extras?.let { " | $it" } ?: "")
        Log.i(TAG, msg)
    }

    /** Błąd */
    fun error(context: String, error: Throwable) {
        Log.e(TAG, "ERROR ▸ $context: ${error.message}", error)
    }

    /** Błąd tekstowy */
    fun error(context: String, message: String) {
        Log.e(TAG, "ERROR ▸ $context: $message")
    }

    /** Health check / stan systemu */
    fun health(message: String) {
        Log.d(TAG, "HEALTH ▸ $message")
    }

    /** Informacja ogólna */
    fun info(message: String) {
        Log.i(TAG, message)
    }
}
