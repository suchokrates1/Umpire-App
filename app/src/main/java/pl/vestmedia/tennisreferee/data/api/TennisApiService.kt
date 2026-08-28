package pl.vestmedia.tennisreferee.data.api

import pl.vestmedia.tennisreferee.data.api.dto.AddPlayerResponseDto
import pl.vestmedia.tennisreferee.data.api.dto.CourtAuthResponseDto
import pl.vestmedia.tennisreferee.data.api.dto.CourtPinRequestDto
import pl.vestmedia.tennisreferee.data.api.dto.CourtsResponseDto
import pl.vestmedia.tennisreferee.data.api.dto.DirectorAckResponseDto
import pl.vestmedia.tennisreferee.data.api.dto.DirectorCommandsResponseDto
import pl.vestmedia.tennisreferee.data.api.dto.FinishMatchRequestDto
import pl.vestmedia.tennisreferee.data.api.dto.HeartbeatResponseDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchEventDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchEventResponseDto
import pl.vestmedia.tennisreferee.data.api.dto.MatchStatisticsRequestDto
import pl.vestmedia.tennisreferee.data.api.dto.PlayersResponseDto
import pl.vestmedia.tennisreferee.data.api.dto.ScheduleSuggestionResponseDto
import pl.vestmedia.tennisreferee.data.api.dto.TournamentOptionDto
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
    suspend fun getCourts(@Query("tournament_id") tournamentId: Int? = null): Response<CourtsResponseDto>

    /**
     * Pobiera listę aktywnych turniejów
     */
    @GET("api/tournaments/active")
    suspend fun getActiveTournaments(): Response<List<TournamentOptionDto>>
    
    /**
     * Pobiera listę zawodników
     */
    @GET("api/players")
    suspend fun getPlayers(@Query("court_id") courtId: String? = null): Response<PlayersResponseDto>

    /**
     * Pobiera najbliższy zaplanowany mecz dla kortu.
     */
    @GET("api/courts/{kort_id}/suggested-match")
    suspend fun getSuggestedMatch(
        @Path("kort_id") courtId: String,
        @Query("tournament_id") tournamentId: Int? = null,
        @Query("at") at: String? = null
    ): Response<ScheduleSuggestionResponseDto>
    
    /**
     * Weryfikuje PIN dla kortu
     */
    @POST("api/courts/{kort_id}/authorize")
    suspend fun verifyCourtPin(
        @Path("kort_id") courtId: String,
        @Body pinRequest: CourtPinRequestDto
    ): Response<CourtAuthResponseDto>
    
    /**
     * Pobiera szczegóły meczu
     */
    @GET("api/matches/{matchId}")
    suspend fun getMatch(@Path("matchId") matchId: Int): Response<MatchDto>
    
    /**
     * Tworzy nowy mecz
     */
    @POST("api/matches")
    suspend fun createMatch(@Body match: MatchDto): Response<MatchDto>
    
    /**
     * Aktualizuje wynik meczu
     */
    @PUT("api/matches/{matchId}")
    suspend fun updateMatch(
        @Path("matchId") matchId: Int,
        @Body match: MatchDto
    ): Response<MatchDto>
    
    /**
     * Kończy mecz
     */
    @POST("api/matches/{matchId}/finish")
    suspend fun finishMatch(
        @Path("matchId") matchId: Int,
        @Body request: FinishMatchRequestDto
    ): Response<MatchDto>
    
    /**
     * Dodaje nowego zawodnika
     */
    @POST("api/players")
    suspend fun addPlayer(@Body playerRequest: Map<String, String>): Response<AddPlayerResponseDto>
    
    /**
     * Loguje zdarzenie meczowe do serwera
     */
    @POST("api/match-events")
    suspend fun logMatchEvent(@Body event: MatchEventDto): Response<MatchEventResponseDto>
    
    /**
     * Wysyła statystyki meczu do serwera
     */
    @POST("api/match-statistics")
    suspend fun sendMatchStatistics(@Body statistics: MatchStatisticsRequestDto): Response<Unit>

    /**
     * Heartbeat — stan baterii i status online (niezależnie od meczu)
     */
    @POST("api/umpire-heartbeat")
    suspend fun sendHeartbeat(@Body body: Map<String, String>): Response<HeartbeatResponseDto>

    @GET("api/umpire/commands")
    suspend fun pollDirectorCommands(
        @Query("match_id") matchId: Int? = null,
        @Query("client_match_uuid") clientMatchUuid: String? = null,
        @Query("wait_ms") waitMs: Int? = null,
        @Query("court_id") courtId: String? = null
    ): Response<DirectorCommandsResponseDto>

    @POST("api/umpire/commands/{command_id}/ack")
    suspend fun ackDirectorCommand(
        @Path("command_id") commandId: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<DirectorAckResponseDto>
}

