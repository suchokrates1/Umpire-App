package pl.vestmedia.tennisreferee.ui.match

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.vestmedia.tennisreferee.data.api.RetrofitClient
import pl.vestmedia.tennisreferee.data.model.*

/**
 * ViewModel zarządzający logiką meczu tenisowego
 */
class MatchViewModel : ViewModel() {
    
    private val _matchState = MutableLiveData<MatchState>()
    val matchState: LiveData<MatchState> = _matchState
    
    private val _currentView = MutableLiveData<MatchView>(MatchView.SERVER_SELECTION)
    val currentView: LiveData<MatchView> = _currentView
    
    private val _canUndo = MutableLiveData<Boolean>(false)
    val canUndo: LiveData<Boolean> = _canUndo
    
    private val _undoMessage = MutableLiveData<String?>()
    val undoMessage: LiveData<String?> = _undoMessage
    
    private val apiService = RetrofitClient.apiService
    
    /**
     * Inicjalizuje nowy mecz
     */
    fun initializeMatch(matchState: MatchState) {
        _matchState.value = matchState
    }
    
    /**
     * Ustawia który gracz serwuje pierwszy
     */
    fun setFirstServer(isPlayer1: Boolean) {
        _matchState.value?.let { state ->
            state.isPlayer1Serving = isPlayer1
            state.matchStartTime = System.currentTimeMillis()
            _matchState.value = state
            _currentView.value = MatchView.SERVE
            
            // Log match start event
            logMatchEvent("match_start")
        }
    }
    
    /**
     * Zamienia strony (gracze przechodzą na przeciwległe strony kortu)
     */
    fun swapSides() {
        _matchState.value?.let { state ->
            state.sidesSwapped = !state.sidesSwapped
            _matchState.value = state
        }
    }
    
    /**
     * Zapisuje aktualny stan przed wykonaniem akcji
     */
    private fun saveStateBeforeAction(actionType: ActionType, description: String) {
        _matchState.value?.let { state ->
            val action = MatchAction(
                actionType = actionType,
                previousPlayer1Points = state.player1Points,
                previousPlayer2Points = state.player2Points,
                previousPlayer1Games = state.player1Games,
                previousPlayer2Games = state.player2Games,
                previousPlayer1Sets = state.player1Sets,
                previousPlayer2Sets = state.player2Sets,
                previousIsPlayer1Serving = state.isPlayer1Serving,
                previousIsFirstServe = state.isFirstServe,
                previousIsTiebreak = state.isTiebreak,
                previousIsSuperTiebreak = state.isSuperTiebreak,
                previousSetsHistorySize = state.setsHistory.size,
                previousPlayer1Stats = state.player1Stats.copy(),
                previousPlayer2Stats = state.player2Stats.copy(),
                description = description
            )
            state.actionsHistory.add(action)
            _canUndo.value = true
        }
    }
    
    /**
     * Cofa ostatnią akcję
     */
    fun undoLastAction() {
        _matchState.value?.let { state ->
            if (state.actionsHistory.isEmpty()) {
                _undoMessage.value = "Brak akcji do cofnięcia"
                return
            }
            
            val lastAction = state.actionsHistory.removeAt(state.actionsHistory.size - 1)
            
            // Przywróć stan
            state.player1Points = lastAction.previousPlayer1Points
            state.player2Points = lastAction.previousPlayer2Points
            state.player1Games = lastAction.previousPlayer1Games
            state.player2Games = lastAction.previousPlayer2Games
            state.player1Sets = lastAction.previousPlayer1Sets
            state.player2Sets = lastAction.previousPlayer2Sets
            state.isPlayer1Serving = lastAction.previousIsPlayer1Serving
            state.isFirstServe = lastAction.previousIsFirstServe
            state.isTiebreak = lastAction.previousIsTiebreak
            state.isSuperTiebreak = lastAction.previousIsSuperTiebreak
            
            // Przywróć historię setów
            while (state.setsHistory.size > lastAction.previousSetsHistorySize) {
                state.setsHistory.removeAt(state.setsHistory.size - 1)
            }
            
            // Przywróć statystyki
            state.player1Stats.aces = lastAction.previousPlayer1Stats.aces
            state.player1Stats.doubleFaults = lastAction.previousPlayer1Stats.doubleFaults
            state.player1Stats.winners = lastAction.previousPlayer1Stats.winners
            state.player1Stats.forcedErrors = lastAction.previousPlayer1Stats.forcedErrors
            state.player1Stats.unforcedErrors = lastAction.previousPlayer1Stats.unforcedErrors
            state.player1Stats.firstServesIn = lastAction.previousPlayer1Stats.firstServesIn
            state.player1Stats.firstServesTotal = lastAction.previousPlayer1Stats.firstServesTotal
            state.player1Stats.secondServesIn = lastAction.previousPlayer1Stats.secondServesIn
            state.player1Stats.secondServesTotal = lastAction.previousPlayer1Stats.secondServesTotal
            
            state.player2Stats.aces = lastAction.previousPlayer2Stats.aces
            state.player2Stats.doubleFaults = lastAction.previousPlayer2Stats.doubleFaults
            state.player2Stats.winners = lastAction.previousPlayer2Stats.winners
            state.player2Stats.forcedErrors = lastAction.previousPlayer2Stats.forcedErrors
            state.player2Stats.unforcedErrors = lastAction.previousPlayer2Stats.unforcedErrors
            state.player2Stats.firstServesIn = lastAction.previousPlayer2Stats.firstServesIn
            state.player2Stats.firstServesTotal = lastAction.previousPlayer2Stats.firstServesTotal
            state.player2Stats.secondServesIn = lastAction.previousPlayer2Stats.secondServesIn
            state.player2Stats.secondServesTotal = lastAction.previousPlayer2Stats.secondServesTotal
            
            _canUndo.value = state.actionsHistory.isNotEmpty()
            _undoMessage.value = "Cofnięto: ${lastAction.description}"
            _matchState.value = state
            _currentView.value = MatchView.SERVE
        }
    }
    
    /**
     * Czyści komunikat o cofnięciu
     */
    fun clearUndoMessage() {
        _undoMessage.value = null
    }
    
    /**
     * Obsługuje ACE - punkt bezpośrednio z serwisu
     */
    fun handleAce() {
        _matchState.value?.let { state ->
            val serverName = if (state.isPlayer1Serving) state.player1.getDisplayName() else state.player2.getDisplayName()
            saveStateBeforeAction(ActionType.ACE, "ACE - $serverName")
            
            if (state.isPlayer1Serving) {
                state.player1Stats.aces++
                state.player1Stats.firstServesIn++
                state.player1Stats.firstServesTotal++
                addPoint(true)
            } else {
                state.player2Stats.aces++
                state.player2Stats.firstServesIn++
                state.player2Stats.firstServesTotal++
                addPoint(false)
            }
            state.isFirstServe = true
            _matchState.value = state
            checkGameAndSetStatus()
        }
    }
    
    /**
     * Obsługuje FAULT - nieudany serwis
     */
    fun handleFault() {
        _matchState.value?.let { state ->
            if (state.isFirstServe) {
                // Pierwszy serwis nieudany - przejdź na 2. serwis
                saveStateBeforeAction(ActionType.FAULT, "Fault - First Serve")
                
                if (state.isPlayer1Serving) {
                    state.player1Stats.firstServesTotal++
                } else {
                    state.player2Stats.firstServesTotal++
                }
                state.isFirstServe = false
                _matchState.value = state
            } else {
                // Podwójny błąd
                val serverName = if (state.isPlayer1Serving) state.player1.getDisplayName() else state.player2.getDisplayName()
                saveStateBeforeAction(ActionType.DOUBLE_FAULT, "Double Fault - $serverName")
                
                if (state.isPlayer1Serving) {
                    state.player1Stats.doubleFaults++
                    state.player1Stats.secondServesTotal++
                    addPoint(false) // Punkt dla przeciwnika
                } else {
                    state.player2Stats.doubleFaults++
                    state.player2Stats.secondServesTotal++
                    addPoint(true) // Punkt dla przeciwnika
                }
                state.isFirstServe = true
                _matchState.value = state
                checkGameAndSetStatus()
            }
        }
    }
    
    /**
     * Piłka w grze - przejście do widoku rozgrywki
     */
    fun handleBallInPlay() {
        _matchState.value?.let { state ->
            if (state.isFirstServe) {
                if (state.isPlayer1Serving) {
                    state.player1Stats.firstServesIn++
                    state.player1Stats.firstServesTotal++
                } else {
                    state.player2Stats.firstServesIn++
                    state.player2Stats.firstServesTotal++
                }
            } else {
                if (state.isPlayer1Serving) {
                    state.player1Stats.secondServesIn++
                    state.player1Stats.secondServesTotal++
                } else {
                    state.player2Stats.secondServesIn++
                    state.player2Stats.secondServesTotal++
                }
            }
            state.isFirstServe = true
            _matchState.value = state
            _currentView.value = MatchView.RALLY
        }
    }
    
    /**
     * Obsługuje winner - uderzenie kończące wymianę
     */
    fun handleWinner(isPlayer1: Boolean) {
        _matchState.value?.let { state ->
            val playerName = if (isPlayer1) state.player1.getDisplayName() else state.player2.getDisplayName()
            saveStateBeforeAction(ActionType.WINNER, "Winner - $playerName")
            
            if (isPlayer1) {
                state.player1Stats.winners++
            } else {
                state.player2Stats.winners++
            }
            addPoint(isPlayer1)
            _matchState.value = state
            checkGameAndSetStatus()
        }
    }
    
    /**
     * Obsługuje wymuszone błędy
     */
    fun handleForcedError(isPlayer1: Boolean) {
        _matchState.value?.let { state ->
            val playerName = if (isPlayer1) state.player1.getDisplayName() else state.player2.getDisplayName()
            saveStateBeforeAction(ActionType.FORCED_ERROR, "Forced Error - $playerName")
            
            if (isPlayer1) {
                state.player1Stats.forcedErrors++
                addPoint(false) // Punkt dla przeciwnika
            } else {
                state.player2Stats.forcedErrors++
                addPoint(true) // Punkt dla przeciwnika
            }
            _matchState.value = state
            checkGameAndSetStatus()
        }
    }
    
    /**
     * Obsługuje niewymuszone błędy
     */
    fun handleUnforcedError(isPlayer1: Boolean) {
        _matchState.value?.let { state ->
            val playerName = if (isPlayer1) state.player1.getDisplayName() else state.player2.getDisplayName()
            saveStateBeforeAction(ActionType.UNFORCED_ERROR, "Unforced Error - $playerName")
            
            if (isPlayer1) {
                state.player1Stats.unforcedErrors++
                addPoint(false) // Punkt dla przeciwnika
            } else {
                state.player2Stats.unforcedErrors++
                addPoint(true) // Punkt dla przeciwnika
            }
            _matchState.value = state
            checkGameAndSetStatus()
        }
    }
    
    /**
     * Dodaje punkt dla gracza
     */
    private fun addPoint(isPlayer1: Boolean) {
        _matchState.value?.let { state ->
            if (isPlayer1) {
                state.player1Points++
            } else {
                state.player2Points++
            }
            
            // Loguj punkt do serwera
            logMatchEvent("point")
            
            // W tiebreak i super tiebreak: zmiana serwisu co 2 punkty
            if (state.isTiebreak || state.isSuperTiebreak) {
                val totalPoints = state.player1Points + state.player2Points
                
                // Zmiana serwisu co 2 punkty (po 1,3,5,7,9... punkcie)
                if (totalPoints % 2 == 1) {
                    state.isPlayer1Serving = !state.isPlayer1Serving
                    logMatchEvent("serve_change")
                }
                
                // Zmiana stron co 6 punktów
                if (totalPoints > 0 && totalPoints % 6 == 0) {
                    state.sidesSwapped = !state.sidesSwapped
                    logMatchEvent("side_change")
                }
            }
        }
    }
    
    /**
     * Sprawdza czy gem/set został wygrany i aktualizuje stan
     */
    private fun checkGameAndSetStatus() {
        _matchState.value?.let { state ->
            if (state.isGameWon()) {
                // Ktoś wygrał gema
                val player1Won = state.player1Points > state.player2Points
                
                if (state.isTiebreak || state.isSuperTiebreak) {
                    // Dodaj gem zwycięzcy tiebreak (aby było 7:6 zamiast 6:6)
                    if (player1Won) {
                        state.player1Games++
                    } else {
                        state.player2Games++
                    }
                    
                    // Wygrany tiebreak = wygrany set
                    if (player1Won) {
                        state.player1Sets++
                    } else {
                        state.player2Sets++
                    }
                    
                    // Zapisz wynik seta (teraz będzie 7:6)
                    state.setsHistory.add(
                        SetScore(
                            setNumber = state.setsHistory.size + 1,
                            player1Games = state.player1Games,
                            player2Games = state.player2Games
                        )
                    )
                    
                    // Resetuj gemy i punkty
                    state.player1Games = 0
                    state.player2Games = 0
                    state.isTiebreak = false
                    state.isSuperTiebreak = false
                    
                    // Sprawdź czy mecz się skończył (szczególnie ważne dla Super TB)
                    if (state.shouldEndMatch()) {
                        state.isMatchFinished = true
                        state.matchDuration = System.currentTimeMillis() - state.matchStartTime
                        _matchState.value = state
                        _currentView.value = MatchView.MATCH_FINISHED
                        return
                    }
                } else {
                    // Normalny gem
                    if (player1Won) {
                        state.player1Games++
                    } else {
                        state.player2Games++
                    }
                    
                    // Zwiększ liczbę rozegranych gemów
                    state.totalGamesPlayed++
                    
                    // Automatyczna zmiana stron co nieparzyste gemy (1, 3, 5, 7...)
                    if (state.totalGamesPlayed % 2 == 1) {
                        state.sidesSwapped = !state.sidesSwapped
                    }
                }
                
                // Reset punktów
                state.player1Points = 0
                state.player2Points = 0
                
                // Zmiana serwującego po gemie
                state.isPlayer1Serving = !state.isPlayer1Serving
                
                // Log game won event
                logMatchEvent("game")
                
                // Sprawdź czy set został wygrany
                if (state.isSetWon()) {
                    val setWinner = if (state.player1Games > state.player2Games) 1 else 2
                    
                    if (setWinner == 1) {
                        state.player1Sets++
                    } else {
                        state.player2Sets++
                    }
                    
                    // Zapisz wynik seta
                    state.setsHistory.add(
                        SetScore(
                            setNumber = state.setsHistory.size + 1,
                            player1Games = state.player1Games,
                            player2Games = state.player2Games
                        )
                    )
                    
                    // Log set won event
                    logMatchEvent("set")
                    
                    // Sprawdź czy mecz się skończył
                    if (state.shouldEndMatch()) {
                        state.isMatchFinished = true
                        state.matchDuration = System.currentTimeMillis() - state.matchStartTime
                        _matchState.value = state
                        
                        // Log match end event
                        logMatchEvent("match_end")
                        
                        _currentView.value = MatchView.MATCH_FINISHED
                        return
                    }
                    
                    // Sprawdź czy należy rozpocząć super tiebreak (1:1 w setach)
                    if (state.player1Sets == 1 && state.player2Sets == 1) {
                        state.isSuperTiebreak = true
                    }
                    
                    // Resetuj gemy i licznik rozegranych gemów na nowy set
                    state.player1Games = 0
                    state.player2Games = 0
                    state.totalGamesPlayed = 0
                    
                    // Automatyczna zmiana stron po zakończeniu seta
                    state.sidesSwapped = !state.sidesSwapped
                }
                
                // Sprawdź czy należy rozpocząć tiebreak (6:6)
                if (state.shouldStartTiebreak() && !state.isSuperTiebreak) {
                    state.isTiebreak = true
                }
                
                _matchState.value = state
                _currentView.value = MatchView.SERVE
            } else {
                // Gem trwa dalej
                _currentView.value = MatchView.SERVE
            }
        }
    }
    
    /**
     * Powrót do poprzedniego widoku
     */
    fun goBack() {
        when (_currentView.value) {
            MatchView.SERVE -> _currentView.value = MatchView.SERVER_SELECTION
            MatchView.RALLY -> _currentView.value = MatchView.SERVE
            else -> {}
        }
    }
    
    /**
     * Loguje zdarzenie meczowe do serwera
     */
    private fun logMatchEvent(eventType: String) {
        val state = _matchState.value ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val event = MatchEvent(
                    courtId = state.courtId,
                    eventType = eventType,
                    player1 = PlayerInfo(
                        name = state.player1.getDisplayName(),
                        flag = state.player1.flag,
                        isServing = state.isPlayer1Serving && !state.sidesSwapped || 
                                   !state.isPlayer1Serving && state.sidesSwapped
                    ),
                    player2 = PlayerInfo(
                        name = state.player2.getDisplayName(),
                        flag = state.player2.flag,
                        isServing = !state.isPlayer1Serving && !state.sidesSwapped || 
                                   state.isPlayer1Serving && state.sidesSwapped
                    ),
                    score = ScoreInfo(
                        player1Sets = state.player1Sets,
                        player2Sets = state.player2Sets,
                        player1Games = state.player1Games,
                        player2Games = state.player2Games,
                        player1Points = state.player1Points,
                        player2Points = state.player2Points,
                        isTiebreak = state.isTiebreak,
                        isSuperTiebreak = state.isSuperTiebreak,
                        matchFinished = state.isMatchFinished
                    )
                )
                
                val response = apiService.logMatchEvent(event)
                if (!response.isSuccessful) {
                    println("Failed to log match event: ${response.code()}")
                }
            } catch (e: Exception) {
                println("Error logging match event: ${e.message}")
                // Nie przerywamy działania aplikacji przy błędzie logowania
            }
        }
    }
}

/**
 * Enum definiujący różne widoki w trakcie meczu
 */
enum class MatchView {
    SERVER_SELECTION,  // Wybór pierwszego serwującego
    SERVE,             // Widok serwisu (Ace/Fault/Ball in play)
    RALLY,             // Widok wymiany (Winner/Forced/Unforced)
    MATCH_FINISHED     // Koniec meczu
}
