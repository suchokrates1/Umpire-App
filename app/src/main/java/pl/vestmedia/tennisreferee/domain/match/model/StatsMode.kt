package pl.vestmedia.tennisreferee.domain.match.model

/**
 * Tryb zbierania statystyk meczu
 * BASIC - uproszczony: tylko podwójne błędy, serwujący ma Win/Fault, odbierający ma Win
 * ADVANCED - pełny: asy, wymuszony/niewymuszony błąd, winnery, itd.
 */
enum class StatsMode {
    BASIC,
    ADVANCED
}
