package pl.vestmedia.tennisreferee.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.VisibleForTesting
import org.json.JSONObject
import pl.vestmedia.tennisreferee.utils.AppLogger
import java.security.KeyStore
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

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
 * Stores the court authorization session as one AES-256-GCM blob whose key never leaves
 * the Android Keystore. A PIN is retained only for an authorization response from a legacy
 * server that did not issue a token, so the legacy player-creation request can still be
 * authorized.
 *
 * The key is created in the constructor, so a device without a usable Keystore fails here
 * and [createCourtSessionStore] falls back to [SharedPreferencesCourtSessionStore].
 */
class KeystoreCourtSessionStore(context: Context) : CourtSessionStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val key: SecretKey = loadOrCreateKey()

    init {
        // Sessions written by the removed EncryptedSharedPreferences store; the umpire
        // authorizes the court once more after this update.
        context.deleteSharedPreferences(LEGACY_PREFERENCES_NAME)
    }

    override fun current(): CourtSession? {
        val blob = preferences.getString(KEY_BLOB, null) ?: return null
        return try {
            val (iv, ciphertext) = blob.split(':', limit = 2)
                .map { Base64.decode(it, Base64.NO_WRAP) }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            JSONObject(String(cipher.doFinal(ciphertext), Charsets.UTF_8)).toCourtSession()
        } catch (_: Exception) {
            // Tampered data or a key lost in a backup restore: the session is unusable.
            clear()
            null
        }
    }

    override fun save(session: CourtSession) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val ciphertext = cipher.doFinal(session.toJson().toString().toByteArray(Charsets.UTF_8))
        val blob = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        preferences.edit().putString(KEY_BLOB, blob).apply()
    }

    override fun clear() {
        preferences.edit().clear().apply()
    }

    private fun loadOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        const val PREFERENCES_NAME = "court_session_keystore"
        private const val LEGACY_PREFERENCES_NAME = "court_session"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "court_session_key"
        private const val KEY_BLOB = "session"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_BITS = 128
    }
}

/**
 * Unencrypted fallback used when the Keystore key cannot be created
 * (no usable Keystore on the device, Robolectric).
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
            encryptedFactory = { KeystoreCourtSessionStore(appContext) },
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
 * Keystore failures must not kill Application.onCreate.
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

private fun CourtSession.toJson(): JSONObject = JSONObject().apply {
    put(KEY_COURT_ID, courtId)
    token?.let { put(KEY_TOKEN, it) }
    expiresAtMillis?.let { put(KEY_EXPIRES_AT, it) }
    legacyPin?.let { put(KEY_LEGACY_PIN, it) }
}

private fun JSONObject.toCourtSession(): CourtSession = CourtSession(
    courtId = getString(KEY_COURT_ID),
    token = optString(KEY_TOKEN).takeIf { has(KEY_TOKEN) },
    expiresAtMillis = if (has(KEY_EXPIRES_AT)) getLong(KEY_EXPIRES_AT) else null,
    legacyPin = optString(KEY_LEGACY_PIN).takeIf { has(KEY_LEGACY_PIN) }
)

private val FRACTION_REGEX = Regex("""\.(\d+)(?=[Zz+-]|$)""")

internal fun parseSessionExpiry(value: String?): Long? {
    val normalized = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    normalized.toLongOrNull()?.let { numeric ->
        return if (numeric < 10_000_000_000L) numeric * 1_000L else numeric
    }

    // The server sends Python isoformat() (microseconds, +00:00): SimpleDateFormat reads
    // milliseconds only, so the fraction is cut or padded to exactly three digits.
    val iso = FRACTION_REGEX.replace(normalized) { match ->
        "." + match.groupValues[1].take(3).padEnd(3, '0')
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
            }.parse(iso)?.time
        } catch (_: ParseException) {
            null
        }
    }
}
