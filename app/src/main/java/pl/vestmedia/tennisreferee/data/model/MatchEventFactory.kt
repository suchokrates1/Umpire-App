package pl.vestmedia.tennisreferee.data.model

object MatchEventFactory {
    fun create(
        state: MatchState,
        eventType: String,
        batteryLevel: Int?,
        isCharging: Boolean?,
        timestamp: Long = System.currentTimeMillis()
    ): MatchEvent {
        return MatchEvent(
            courtId = state.courtId,
            eventType = eventType,
            player1 = buildSidePlayerInfo(state, isPlayer1Side = true),
            player2 = buildSidePlayerInfo(state, isPlayer1Side = false),
            score = ScoreInfo(
                player1Sets = state.player1Sets,
                player2Sets = state.player2Sets,
                player1Games = state.player1Games,
                player2Games = state.player2Games,
                player1Points = state.player1Points,
                player2Points = state.player2Points,
                isTiebreak = state.isTiebreak,
                isSuperTiebreak = state.isSuperTiebreak,
                matchFinished = state.isMatchFinished,
                setsHistory = state.setsHistory.toList(),
                statsMode = state.statsMode.name
            ),
            stats = LiveStatsInfo(
                player1Aces = state.player1Stats.aces,
                player1DoubleFaults = state.player1Stats.doubleFaults,
                player1Winners = state.player1Stats.winners,
                player1UnforcedErrors = state.player1Stats.unforcedErrors,
                player1FirstServePct = state.player1Stats.getFirstServePercentage(),
                player2Aces = state.player2Stats.aces,
                player2DoubleFaults = state.player2Stats.doubleFaults,
                player2Winners = state.player2Stats.winners,
                player2UnforcedErrors = state.player2Stats.unforcedErrors,
                player2FirstServePct = state.player2Stats.getFirstServePercentage()
            ),
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            timestamp = timestamp
        )
    }

    private fun buildSidePlayerInfo(state: MatchState, isPlayer1Side: Boolean): PlayerInfo {
        val player = if (isPlayer1Side) state.player1 else state.player2
        val serving = if (isPlayer1Side) state.isPlayer1Serving else !state.isPlayer1Serving

        if (!state.isDoubles) {
            return PlayerInfo(
                name = player.getDisplayName(),
                fullName = player.getFullName(),
                flag = player.flag,
                isServing = serving
            )
        }

        val displayName = if (isPlayer1Side) state.getTeam1DisplayName() else state.getTeam2DisplayName()
        val fullName = if (isPlayer1Side) state.getTeam1FullName() else state.getTeam2FullName()

        return PlayerInfo(
            name = displayName,
            fullName = fullName,
            flag = player.flag,
            isServing = serving
        )
    }
}