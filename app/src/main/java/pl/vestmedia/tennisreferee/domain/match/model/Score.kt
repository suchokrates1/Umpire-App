package pl.vestmedia.tennisreferee.domain.match.model

/**
 * Model reprezentujący wynik meczu
 */
data class Score(
    val player1Sets: Int = 0,
    
    val player2Sets: Int = 0,
    
    val player1Games: Int = 0,
    
    val player2Games: Int = 0,
    
    val player1Points: Int = 0,
    
    val player2Points: Int = 0,
    
    val setsHistory: List<SetScore> = emptyList()
)

data class SetScore(
    val setNumber: Int,
    
    val player1Games: Int,
    
    val player2Games: Int,
    
    val tiebreakLoserPoints: Int? = null,  // For display like 5-4(7) — stores loser's tiebreak points
    
    val isSuperTiebreak: Boolean = false  // True when this "set" is actually a super tiebreak (stores points, not games)
)
