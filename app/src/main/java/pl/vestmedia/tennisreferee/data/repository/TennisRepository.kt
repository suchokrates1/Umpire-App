package pl.vestmedia.tennisreferee.data.repository

import pl.vestmedia.tennisreferee.data.api.RetrofitClient
import pl.vestmedia.tennisreferee.data.model.Court
import pl.vestmedia.tennisreferee.data.model.Match
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.data.model.CourtPinRequest
import pl.vestmedia.tennisreferee.data.model.CourtAuthResponse
import pl.vestmedia.tennisreferee.data.model.TournamentOption
import retrofit2.Response

/**
 * Repository obsługujące operacje na kortach i meczach
 */
class TennisRepository {
    
    private val apiService = RetrofitClient.apiService
    private val playersCache = mutableMapOf<String?, List<Player>>()
    
    /**
     * Pobiera listę dostępnych kortów
     */
    suspend fun getCourts(tournamentId: Int? = null): Result<List<Court>> {
        return request { apiService.getCourts(tournamentId) }
            .map { it.courts }
    }

    /**
     * Pobiera listę aktywnych turniejów
     */
    suspend fun getActiveTournaments(): Result<List<TournamentOption>> {
        return request { apiService.getActiveTournaments() }
    }
    
    /**
     * Pobiera listę zawodników
     */
    suspend fun getPlayers(courtId: String? = null, forceRefresh: Boolean = false): Result<List<Player>> {
        if (!forceRefresh) {
            playersCache[courtId]?.let { return Result.success(it) }
        }

        return request { apiService.getPlayers(courtId) }
            .map { it.players }
            .onSuccess { playersCache[courtId] = it }
    }
    
    /**
     * Weryfikuje PIN dla kortu
     */
    suspend fun verifyCourtPin(courtId: String, pin: String): Result<CourtAuthResponse> {
        return request { apiService.verifyCourtPin(courtId, CourtPinRequest(pin)) }
            .fold(
                onSuccess = { authResponse ->
                if (authResponse.authorized) {
                    Result.success(authResponse)
                } else {
                    Result.failure(Exception(authResponse.error ?: "Authorization failed"))
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
    }
    
    /**
     * Tworzy nowy mecz
     */
    suspend fun createMatch(match: Match): Result<Match> {
        return request { apiService.createMatch(match) }
    }
    
    /**
     * Aktualizuje wynik meczu
     */
    suspend fun updateMatch(matchId: Int, match: Match): Result<Match> {
        return request { apiService.updateMatch(matchId, match) }
    }
    
    /**
     * Kończy mecz
     */
    suspend fun finishMatch(matchId: Int): Result<Match> {
        return request { apiService.finishMatch(matchId) }
    }
    
    /**
     * Dodaje nowego zawodnika
     * API format v1: { "name": "Nowak", "flag_code": "PL", "group_category": "B1", "kort_id": "1", "pin": "1234" }
     */
    suspend fun addPlayer(firstName: String, lastName: String, flagCode: String, category: String = "B1", courtId: String = "", courtPin: String = ""): Result<Player> {
        return try {
            val playerRequest = mutableMapOf(
                "first_name" to firstName,
                "last_name" to lastName,
                "name" to "$firstName $lastName".trim(),  // backward compat
                "flag_code" to flagCode.uppercase(),
                "group_category" to category  // v1 używa "group_category"
            )
            
            // Dodaj autoryzację kortu (wymagane!)
            if (courtId.isNotEmpty() && courtPin.isNotEmpty()) {
                playerRequest["kort_id"] = courtId
                playerRequest["pin"] = courtPin
            }
            
            request { apiService.addPlayer(playerRequest) }.fold(
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

    private suspend fun <T> request(call: suspend () -> Response<T>): Result<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Result.failure(Exception(response.toErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun Response<*>.toErrorMessage(): String {
        return "Error: ${code()} - ${message()}"
    }
}
