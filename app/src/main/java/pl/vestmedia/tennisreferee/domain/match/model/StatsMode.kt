package pl.vestmedia.tennisreferee.domain.match.model

import kotlinx.serialization.Serializable

/**
 * Tryb zbierania statystyk meczu
 * BASIC - uproszczony: tylko podwójne błędy, serwujący ma Win/Fault, odbierający ma Win
 * ADVANCED - pełny: asy, wymuszony/niewymuszony błąd, winnery, itd.
 */
@Serializable
enum class StatsMode {
    BASIC,
    ADVANCED
}
