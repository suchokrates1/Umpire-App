package pl.vestmedia.tennisreferee.data.repository

import pl.vestmedia.tennisreferee.data.api.RetrofitClient
import pl.vestmedia.tennisreferee.data.model.Court
import pl.vestmedia.tennisreferee.data.model.Match
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.data.model.CourtPinRequest
import pl.vestmedia.tennisreferee.data.model.CourtAuthResponse

/**
 * Repository obsługujące operacje na kortach i meczach
 */
class TennisRepository {
    
    private val apiService = RetrofitClient.apiService
    
    /**
     * Pobiera listę dostępnych kortów
     */
    suspend fun getCourts(): Result<List<Court>> {
        return try {
            val response = apiService.getCourts()
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
     * Pobiera listę zawodników
     */
    suspend fun getPlayers(): Result<List<Player>> {
        return try {
            val response = apiService.getPlayers()
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
     */
    suspend fun addPlayer(courtId: String, name: String, flagCode: String): Result<Player> {
        return try {
            val flagUrl = "https://flagcdn.com/w80/${flagCode.lowercase()}.png"
            val pin = "1111" // TODO: Pobierz PIN z sesji/storage
            
            val playerRequest = mapOf(
                "kort_id" to courtId,
                "pin" to pin,
                "name" to name,
                "flag_code" to flagCode.uppercase(),
                "flag_url" to flagUrl
            )
            
            val response = apiService.addPlayer(playerRequest)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
