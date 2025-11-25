package pl.vestmedia.tennisreferee.data.api

import pl.vestmedia.tennisreferee.data.model.CourtsResponse
import pl.vestmedia.tennisreferee.data.model.Match
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.data.model.PlayersResponse
import pl.vestmedia.tennisreferee.data.model.CourtPinRequest
import pl.vestmedia.tennisreferee.data.model.CourtAuthResponse
import pl.vestmedia.tennisreferee.data.model.MatchEvent
import pl.vestmedia.tennisreferee.data.model.MatchEventResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * API interface dla komunikacji z serwerem score.vestmedia.pl
 */
interface TennisApiService {
    
    /**
     * Pobiera listę dostępnych kortów
     */
    @GET("api/courts")
    suspend fun getCourts(): Response<CourtsResponse>
    
    /**
     * Pobiera listę zawodników
     */
    @GET("api/players")
    suspend fun getPlayers(): Response<PlayersResponse>
    
    /**
     * Weryfikuje PIN dla kortu
     */
    @POST("api/courts/{kort_id}/authorize")
    suspend fun verifyCourtPin(
        @Path("kort_id") courtId: String,
        @Body pinRequest: CourtPinRequest
    ): Response<CourtAuthResponse>
    
    /**
     * Pobiera szczegóły meczu
     */
    @GET("api/matches/{matchId}")
    suspend fun getMatch(@Path("matchId") matchId: Int): Response<Match>
    
    /**
     * Tworzy nowy mecz
     */
    @POST("api/matches")
    suspend fun createMatch(@Body match: Match): Response<Match>
    
    /**
     * Aktualizuje wynik meczu
     */
    @PUT("api/matches/{matchId}")
    suspend fun updateMatch(
        @Path("matchId") matchId: Int,
        @Body match: Match
    ): Response<Match>
    
    /**
     * Kończy mecz
     */
    @POST("api/matches/{matchId}/finish")
    suspend fun finishMatch(@Path("matchId") matchId: Int): Response<Match>
    
    /**
     * Dodaje nowego zawodnika
     */
    @POST("api/players")
    suspend fun addPlayer(@Body playerRequest: Map<String, String>): Response<Player>
    
    /**
     * Loguje zdarzenie meczowe do serwera
     */
    @POST("api/match-events")
    suspend fun logMatchEvent(@Body event: MatchEvent): Response<MatchEventResponse>
}
