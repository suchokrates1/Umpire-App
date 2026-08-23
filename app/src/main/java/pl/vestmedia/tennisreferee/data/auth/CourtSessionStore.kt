package pl.vestmedia.tennisreferee.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import pl.vestmedia.tennisreferee.utils.AppLogger
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
 * Stores the court authorization session encrypted with an Android Keystore-backed key.
 * A PIN is retained only for an authorization response from a legacy server that did not
 * issue a token, so the legacy player-creation request can still be authorized.
 */
class EncryptedCourtSessionStore(context: Context) : CourtSessionStore {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        PREFERENCES_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun current(): CourtSession? = preferences.readCourtSession()

    override fun save(session: CourtSession) = preferences.writeCourtSession(session)

    override fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        const val PREFERENCES_NAME = "court_session"
    }
}

/**
 * Unencrypted fallback used when Keystore / EncryptedSharedPreferences cannot be created
 * (missing Tink classes after R8, backup-restore without the key, emulator/Robolectric).
 * Uses a separate file so a corrupted encrypted blob is never read as plaintext prefs.
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
        val appContext = context.applicationContext
        sessionStore = createCourtSessionStore(
            encryptedFactory = { EncryptedCourtSessionStore(appContext) },
            fallbackFactory = { SharedPreferencesCourtSessionStore(appContext) }
        )
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

/**
 * Builds the court-session store used at process start.
 * EncryptedSharedPreferences / Tink / Keystore failures must not kill Application.onCreate.
 */
internal fun createCourtSessionStore(
    encryptedFactory: () -> CourtSessionStore,
    fallbackFactory: () -> CourtSessionStore
): CourtSessionStore {
    return try {
        encryptedFactory()
    } catch (encryptedError: Throwable) {
        try {
            AppLogger.error("CourtSessionStore", encryptedError)
        } catch (_: Throwable) {
            // android.util.Log is unavailable in plain JVM unit tests.
        }
        try {
            fallbackFactory()
        } catch (fallbackError: Throwable) {
            fallbackError.addSuppressed(encryptedError)
            throw fallbackError
        }
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
