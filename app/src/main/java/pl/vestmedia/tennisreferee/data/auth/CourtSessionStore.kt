package pl.vestmedia.tennisreferee.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class CourtSession(
    val courtId: String,
    val token: String? = null,
    val expiresAtMillis: Long? = null,
    val legacyPin: String? = null
) {
    fun hasValidToken(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return !token.isNullOrBlank() && expiresAtMillis?.let { it > nowMillis } == true
    }
}

interface CourtSessionStore {
    fun current(): CourtSession?
    fun save(session: CourtSession)
    fun clear()
}

/**
 * Court authorization session in ordinary SharedPreferences.
 *
 * EncryptedSharedPreferences / MasterKey / Tink must not run during process
 * start: a broken Android Keystore can native-crash the process (logo, then
 * the app closes) and try/catch cannot catch that.
 */
class SharedPreferencesCourtSessionStore(context: Context) : CourtSessionStore {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    override fun current(): CourtSession? = preferences.readCourtSession()

    override fun save(session: CourtSession) = preferences.writeCourtSession(session)

    override fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        const val PREFERENCES_NAME = "court_session_fallback"
    }
}

object CourtSessionProvider {
    @Volatile
    private var sessionStore: CourtSessionStore? = null

    fun initialize(context: Context) {
        sessionStore = SharedPreferencesCourtSessionStore(context.applicationContext)
    }

    fun get(): CourtSessionStore {
        return checkNotNull(sessionStore) {
            "CourtSessionProvider must be initialized from Application.onCreate()"
        }
    }

    @VisibleForTesting
    fun initializeForTests(store: CourtSessionStore) {
        sessionStore = store
    }

    @VisibleForTesting
    fun resetForTests() {
        sessionStore = null
    }
}

internal const val KEY_COURT_ID = "court_id"
internal const val KEY_TOKEN = "token"
internal const val KEY_EXPIRES_AT = "expires_at"
internal const val KEY_LEGACY_PIN = "legacy_pin"

internal fun SharedPreferences.readCourtSession(): CourtSession? {
    val courtId = getString(KEY_COURT_ID, null) ?: return null
    return CourtSession(
        courtId = courtId,
        token = getString(KEY_TOKEN, null),
        expiresAtMillis = if (contains(KEY_EXPIRES_AT)) {
            getLong(KEY_EXPIRES_AT, 0L)
        } else {
            null
        },
        legacyPin = getString(KEY_LEGACY_PIN, null)
    )
}

internal fun SharedPreferences.writeCourtSession(session: CourtSession) {
    edit()
        .putString(KEY_COURT_ID, session.courtId)
        .putString(KEY_TOKEN, session.token)
        .putString(KEY_LEGACY_PIN, session.legacyPin)
        .apply {
            if (session.expiresAtMillis == null) {
                remove(KEY_EXPIRES_AT)
            } else {
                putLong(KEY_EXPIRES_AT, session.expiresAtMillis)
            }
        }
        .apply()
}

internal fun parseSessionExpiry(value: String?): Long? {
    val normalized = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    normalized.toLongOrNull()?.let { numeric ->
        return if (numeric < 10_000_000_000L) numeric * 1_000L else numeric
    }

    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )
    return formats.firstNotNullOfOrNull { pattern ->
        try {
            SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }.parse(normalized)?.time
        } catch (_: ParseException) {
            null
        }
    }
}
