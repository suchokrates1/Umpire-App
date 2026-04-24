package pl.vestmedia.tennisreferee.data.repository

import pl.vestmedia.tennisreferee.data.api.RetrofitClient
import pl.vestmedia.tennisreferee.data.model.Court
import pl.vestmedia.tennisreferee.data.model.Match
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.data.model.CourtPinRequest
import pl.vestmedia.tennisreferee.data.model.CourtAuthResponse
import pl.vestmedia.tennisreferee.data.model.TournamentOption

/**
 * Repository obsługujące operacje na kortach i meczach
 */
class TennisRepository {
    
    private val apiService = RetrofitClient.apiService
    
    /**
     * Pobiera listę dostępnych kortów
     */
    suspend fun getCourts(tournamentId: Int? = null): Result<List<Court>> {
        return try {
            val response = apiService.getCourts(tournamentId)
            if (response.isSuccessful) {
                val courts = response.body()?.courts ?: emptyList()
                Result.success(courts)
            } else {
                Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pobiera listę aktywnych turniejów
     */
    suspend fun getActiveTournaments(): Result<List<TournamentOption>> {
        return try {
            val response = apiService.getActiveTournaments()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Pobiera listę zawodników
     */
    suspend fun getPlayers(courtId: String? = null): Result<List<Player>> {
        return try {
            val response = apiService.getPlayers(courtId)
            if (response.isSuccessful) {
                val players = response.body()?.players ?: emptyList()
                Result.success(players)
            } else {
                Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Weryfikuje PIN dla kortu
     */
    suspend fun verifyCourtPin(courtId: String, pin: String): Result<CourtAuthResponse> {
        return try {
            val response = apiService.verifyCourtPin(courtId, CourtPinRequest(pin))
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                if (authResponse.authorized) {
                    Result.success(authResponse)
                } else {
                    Result.failure(Exception(authResponse.error ?: "Authorization failed"))
                }
            } else {
                Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Pobiera szczegóły meczu
     */
    suspend fun getMatch(matchId: Int): Result<Match> {
        return try {
            val response = apiService.getMatch(matchId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Tworzy nowy mecz
     */
    suspend fun createMatch(match: Match): Result<Match> {
        return try {
            val response = apiService.createMatch(match)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Aktualizuje wynik meczu
     */
    suspend fun updateMatch(matchId: Int, match: Match): Result<Match> {
        return try {
            val response = apiService.updateMatch(matchId, match)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Kończy mecz
     */
    suspend fun finishMatch(matchId: Int): Result<Match> {
        return try {
            val response = apiService.finishMatch(matchId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
            
            val response = apiService.addPlayer(playerRequest)
            if (response.isSuccessful && response.body() != null) {
                val addPlayerResponse = response.body()!!
                if (addPlayerResponse.ok && addPlayerResponse.player != null) {
                    Result.success(addPlayerResponse.player)
                } else {
                    Result.failure(Exception(addPlayerResponse.error ?: "Error adding player"))
                }
            } else {
                Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
