package pl.vestmedia.tennisreferee.data.repository

import kotlinx.coroutines.delay
import pl.vestmedia.tennisreferee.data.api.RetrofitClient
import pl.vestmedia.tennisreferee.data.api.dto.CourtPinRequestDto
import pl.vestmedia.tennisreferee.data.api.dto.toDto
import pl.vestmedia.tennisreferee.data.api.dto.toModel
import pl.vestmedia.tennisreferee.data.auth.CourtSession
import pl.vestmedia.tennisreferee.data.auth.CourtSessionProvider
import pl.vestmedia.tennisreferee.data.auth.CourtSessionStore
import pl.vestmedia.tennisreferee.data.auth.parseSessionExpiry
import pl.vestmedia.tennisreferee.data.model.Court
import pl.vestmedia.tennisreferee.domain.match.model.Match
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.data.model.CourtAuthResponse
import pl.vestmedia.tennisreferee.domain.match.model.FinishMatchRequest
import pl.vestmedia.tennisreferee.data.model.ScheduleSuggestion
import pl.vestmedia.tennisreferee.data.model.TournamentOption
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository obsługujące operacje na kortach i meczach
 */
class TennisRepository(
    private val sessionStore: CourtSessionStore = CourtSessionProvider.get()
) {
    
    private val apiService = RetrofitClient.apiService
    private val playersCache = ConcurrentHashMap<String, List<Player>>()
    
    /**
     * Pobiera listę dostępnych kortów
     */
    suspend fun getCourts(tournamentId: Int? = null): Result<List<Court>> {
        return request { apiService.getCourts(tournamentId) }
            .mapCatching { response -> response.courts.map { it.toModel() } }
    }

    /**
     * Pobiera listę aktywnych turniejów
     */
    suspend fun getActiveTournaments(): Result<List<TournamentOption>> {
        return request { apiService.getActiveTournaments() }
            .mapCatching { tournaments -> tournaments.map { it.toModel() } }
    }
    
    /**
     * Pobiera listę zawodników
     */
    suspend fun getPlayers(courtId: String? = null, forceRefresh: Boolean = false): Result<List<Player>> {
        if (!forceRefresh) {
            playersCache[cacheKey(courtId)]?.let { return Result.success(it) }
        }

        return request { apiService.getPlayers(courtId) }
            .mapCatching { response -> response.players.map { it.toModel() } }
            .onSuccess { playersCache[cacheKey(courtId)] = it }
    }

    suspend fun getSuggestedMatch(
        courtId: String,
        tournamentId: Int? = null,
        at: String = currentScheduleTimeIso()
    ): Result<ScheduleSuggestion?> {
        return request { apiService.getSuggestedMatch(courtId, tournamentId, at) }
            .mapCatching { it.suggestion?.toModel() }
    }
    
    /**
     * Weryfikuje PIN dla kortu
     */
    suspend fun verifyCourtPin(courtId: String, pin: String): Result<CourtAuthResponse> {
        return request { apiService.verifyCourtPin(courtId, CourtPinRequestDto(pin)) }
            .map { it.toModel() }
            .fold(
                onSuccess = { authResponse ->
                    when {
                        !authResponse.authorized -> {
                            Result.failure(Exception(authResponse.error ?: "Authorization failed"))
                        }
                        authResponse.courtId != null && authResponse.courtId != courtId -> {
                            Result.failure(Exception("Authorization response was issued for another court"))
                        }
                        authResponse.token.isNullOrBlank() && authResponse.expiresAt == null -> {
                            // Old servers do not issue a token. Keep the fallback PIN encrypted and
                            // use it only for the legacy player-creation request.
                            sessionStore.save(CourtSession(courtId = courtId, legacyPin = pin))
                            Result.success(authResponse)
                        }
                        authResponse.token.isNullOrBlank() || authResponse.expiresAt.isNullOrBlank() -> {
                            Result.failure(Exception("Authorization response is missing a complete court session"))
                        }
                        else -> {
                            val expiresAtMillis = parseSessionExpiry(authResponse.expiresAt)
                            if (expiresAtMillis == null || expiresAtMillis <= System.currentTimeMillis()) {
                                Result.failure(Exception("Authorization response contains an invalid or expired session"))
                            } else {
                                sessionStore.save(
                                    CourtSession(
                                        courtId = courtId,
                                        token = authResponse.token,
                                        expiresAtMillis = expiresAtMillis
                                    )
                                )
                                Result.success(authResponse)
                            }
                        }
                    }
                },
                onFailure = { Result.failure(it) }
            )
    }
    
    /**
     * Pobiera szczegóły meczu
     */
    suspend fun getMatch(matchId: Int): Result<Match> {
        return request { apiService.getMatch(matchId) }
            .map { it.toModel() }
    }
    
    /**
     * Tworzy nowy mecz
     */
    suspend fun createMatch(match: Match): Result<Match> {
        return request { apiService.createMatch(match.toDto()) }
            .map { it.toModel() }
    }
    
    /**
     * Aktualizuje wynik meczu
     */
    suspend fun updateMatch(matchId: Int, match: Match): Result<Match> {
        return request { apiService.updateMatch(matchId, match.toDto()) }
            .map { it.toModel() }
    }
    
    /**
     * Kończy mecz
     */
    suspend fun finishMatch(matchId: Int): Result<Match> {
        return request { apiService.finishMatch(matchId, FinishMatchRequest().toDto()) }
            .map { it.toModel() }
    }
    
    /**
     * Dodaje nowego zawodnika
     * Modern API authorization is provided by [BearerAuthInterceptor]. An encrypted PIN is
     * included only for a server that did not issue a token during court authorization.
     */
    suspend fun addPlayer(
        firstName: String,
        lastName: String,
        flagCode: String,
        category: String = "B1",
        courtId: String
    ): Result<Player> {
        return try {
            val playerRequest = mutableMapOf(
                "first_name" to firstName,
                "last_name" to lastName,
                "name" to "$firstName $lastName".trim(),  // backward compat
                "flag_code" to flagCode.uppercase(),
                "group_category" to category  // v1 używa "group_category"
            )
            
            val legacyPin = sessionStore.current()
                ?.takeIf { it.courtId == courtId && !it.hasValidToken() }
                ?.legacyPin
            if (!legacyPin.isNullOrBlank()) {
                playerRequest["kort_id"] = courtId
                playerRequest["pin"] = legacyPin
            }
            
            request { apiService.addPlayer(playerRequest) }
                .map { it.toModel() }
                .fold(
                onSuccess = { addPlayerResponse ->
                    if (addPlayerResponse.ok && addPlayerResponse.player != null) {
                        playersCache.clear()
                        Result.success(addPlayerResponse.player)
                    } else {
                        Result.failure(Exception(addPlayerResponse.error ?: "Error adding player"))
                    }
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun <T> request(maxAttempts: Int = 3, call: suspend () -> Response<T>): Result<T> {
        var lastFailure: Exception? = null
        repeat(maxAttempts) { attempt ->
            try {
                val response = call()
                if (response.code() == 401) {
                    sessionStore.clear()
                }
                if (response.isSuccessful || !response.shouldRetry()) {
                    return response.toResult()
                }
                lastFailure = Exception(response.toErrorMessage())
            } catch (e: Exception) {
                lastFailure = e
            }

            if (attempt < maxAttempts - 1) {
                delay(500L * (attempt + 1))
            }
        }

        return Result.failure(lastFailure ?: Exception("Network request failed"))
    }

    private fun <T> Response<T>.toResult(): Result<T> {
        return if (isSuccessful) {
            val body = body()
            if (body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("Empty response body"))
            }
        } else {
            Result.failure(Exception(toErrorMessage()))
        }
    }

    private fun Response<*>.shouldRetry(): Boolean {
        return code() in 500..599 || code() == 408 || code() == 429
    }

    private fun Response<*>.toErrorMessage(): String {
        if (code() == 401) {
            return "Authorization failed or the court session expired. Please authorize the court again."
        }
        return "Error: ${code()} - ${message()}"
    }

    private fun cacheKey(courtId: String?): String = courtId ?: "__all__"

    private fun currentScheduleTimeIso(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date())
    }
}
