package pl.vestmedia.tennisreferee.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
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

    override fun current(): CourtSession? {
        val courtId = preferences.getString(KEY_COURT_ID, null) ?: return null
        return CourtSession(
            courtId = courtId,
            token = preferences.getString(KEY_TOKEN, null),
            expiresAtMillis = if (preferences.contains(KEY_EXPIRES_AT)) {
                preferences.getLong(KEY_EXPIRES_AT, 0L)
            } else {
                null
            },
            legacyPin = preferences.getString(KEY_LEGACY_PIN, null)
        )
    }

    override fun save(session: CourtSession) {
        preferences.edit()
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

    override fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "court_session"
        private const val KEY_COURT_ID = "court_id"
        private const val KEY_TOKEN = "token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_LEGACY_PIN = "legacy_pin"
    }
}

object CourtSessionProvider {
    @Volatile
    private var sessionStore: CourtSessionStore? = null

    fun initialize(context: Context) {
        sessionStore = EncryptedCourtSessionStore(context.applicationContext)
    }

    fun get(): CourtSessionStore {
        return checkNotNull(sessionStore) {
            "CourtSessionProvider must be initialized from Application.onCreate()"
        }
    }
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
