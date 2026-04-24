package pl.vestmedia.tennisreferee.ui.match

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.data.api.RetrofitClient
import pl.vestmedia.tennisreferee.data.model.*
import pl.vestmedia.tennisreferee.utils.AppLogger

/**
 * ViewModel zarządzający logiką meczu tenisowego
 */
class MatchViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _matchState = MutableLiveData<MatchState>()
    val matchState: LiveData<MatchState> = _matchState
    
    private val _currentView = MutableLiveData<MatchView>(MatchView.SERVER_SELECTION)
    val currentView: LiveData<MatchView> = _currentView
    
    private val _canUndo = MutableLiveData<Boolean>(false)
    val canUndo: LiveData<Boolean> = _canUndo
    
    private val _undoMessage = MutableLiveData<String?>()
    val undoMessage: LiveData<String?> = _undoMessage
    
    private val _bracketWarning = MutableLiveData<BracketWarningEvent?>()
    val bracketWarning: LiveData<BracketWarningEvent?> = _bracketWarning
    
    data class BracketWarningEvent(val type: String, val matchId: Int)
    
    private fun getBatteryLevel(): Int? {
        val batteryStatus = getApplication<Application>().registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return batteryStatus?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) (level * 100 / scale) else null
        }
    }
    
    private fun isBatteryCharging(): Boolean {
        val batteryStatus = getApplication<Application>().registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }
    
    /** Helper: get localized string from resources */
    private fun str(resId: Int, vararg args: Any): String =
        getApplication<Application>().getString(resId, *args)
    
    // Pending announcement type — set before switching to ANNOUNCEMENT view
    var pendingAnnouncementType: String? = null
        private set
    
    /**
     * Called when the umpire taps "Continue" on the announcement card.
     * Transitions to the appropriate scoring view.
     */
    fun continueFromAnnouncement() {
        pendingAnnouncementType = null
        val state = _matchState.value ?: return
        _currentView.value = if (state.statsMode == StatsMode.BASIC) MatchView.BASIC_SCORING else MatchView.SERVE
    }
    
    /**
     * Called when the umpire taps "Don't change sides" – reverses the swap and continues.
     */
    fun skipSideChange() {
        _matchState.value?.let { state ->
            state.sidesSwapped = !state.sidesSwapped
            _matchState.value = state
        }
        AppLogger.action("Match", "SideChangeSkipped")
        continueFromAnnouncement()
    }
    
    private val apiService = RetrofitClient.apiService
    private val matchHistoryRepository = (application as TennisRefereeApp).matchHistoryRepository
    
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
            state.matchStartTime = state.manualStartTime ?: System.currentTimeMillis()
            _matchState.value = state
            
            // W trybie basic przejdź do uproszczonego widoku
            if (state.statsMode == StatsMode.BASIC) {
                _currentView.value = MatchView.BASIC_SCORING
            } else {
                _currentView.value = MatchView.SERVE
            }
            
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
                previousSidesSwapped = state.sidesSwapped,
                previousTotalGamesPlayed = state.totalGamesPlayed,
                previousCurrentServer = state.currentServer,
                previousIsMatchFinished = state.isMatchFinished,
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
                _undoMessage.value = getApplication<Application>().getString(R.string.no_actions_to_undo)
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
            state.sidesSwapped = lastAction.previousSidesSwapped
            state.totalGamesPlayed = lastAction.previousTotalGamesPlayed
            state.currentServer = lastAction.previousCurrentServer
            state.isMatchFinished = lastAction.previousIsMatchFinished
            
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
            _undoMessage.value = getApplication<Application>().getString(R.string.undo_action_format, lastAction.description)
            _matchState.value = state
            _currentView.value = if (state.statsMode == StatsMode.BASIC) MatchView.BASIC_SCORING else MatchView.SERVE
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
            saveStateBeforeAction(ActionType.ACE, str(R.string.undo_ace, serverName))
            
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
                saveStateBeforeAction(ActionType.FAULT, str(R.string.undo_fault_first))
                
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
                saveStateBeforeAction(ActionType.DOUBLE_FAULT, str(R.string.undo_double_fault, serverName))
                
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
     * Obsługuje FOOT FAULT - błąd stopy przy serwisie
     * Traktowany identycznie jak zwykły fault (ITF Rules of Tennis, Rule 18)
     */
    fun handleFootFault() {
        _matchState.value?.let { state ->
            if (state.isFirstServe) {
                saveStateBeforeAction(ActionType.FOOT_FAULT, str(R.string.undo_foot_fault_first))
                
                if (state.isPlayer1Serving) {
                    state.player1Stats.firstServesTotal++
                } else {
                    state.player2Stats.firstServesTotal++
                }
                state.isFirstServe = false
                _matchState.value = state
            } else {
                val serverName = if (state.isPlayer1Serving) state.player1.getDisplayName() else state.player2.getDisplayName()
                saveStateBeforeAction(ActionType.FOOT_FAULT, str(R.string.undo_foot_fault_double, serverName))
                
                if (state.isPlayer1Serving) {
                    state.player1Stats.doubleFaults++
                    state.player1Stats.secondServesTotal++
                    addPoint(false)
                } else {
                    state.player2Stats.doubleFaults++
                    state.player2Stats.secondServesTotal++
                    addPoint(true)
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
            saveStateBeforeAction(ActionType.WINNER, str(R.string.undo_winner, playerName))
            
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
            saveStateBeforeAction(ActionType.FORCED_ERROR, str(R.string.undo_forced_error, playerName))
            
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
            saveStateBeforeAction(ActionType.UNFORCED_ERROR, str(R.string.undo_unforced_error, playerName))
            
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
    
    // ===== BASIC MODE =====
    
    /**
     * Tryb BASIC: Gracz wygrywa punkt (serwujący lub odbierający)
     * W basic mode nie rozróżniamy ace/winner/forced/unforced — po prostu WIN.
     */
    fun handleBasicWin(isPlayer1: Boolean) {
        _matchState.value?.let { state ->
            val playerName = if (isPlayer1) state.player1.getDisplayName() else state.player2.getDisplayName()
            saveStateBeforeAction(ActionType.WINNER, str(R.string.undo_win, playerName))
            
            if (isPlayer1) {
                state.player1Stats.winners++
            } else {
                state.player2Stats.winners++
            }
            
            // Statystyki serwisu — serwis wpadł (1. lub 2.)
            if (state.isFirstServe) {
                // 1. serwis wpadł
                if (state.isPlayer1Serving) {
                    state.player1Stats.firstServesIn++
                    state.player1Stats.firstServesTotal++
                } else {
                    state.player2Stats.firstServesIn++
                    state.player2Stats.firstServesTotal++
                }
            } else {
                // 2. serwis wpadł (1. był fault — już policzony)
                if (state.isPlayer1Serving) {
                    state.player1Stats.secondServesIn++
                    state.player1Stats.secondServesTotal++
                } else {
                    state.player2Stats.secondServesIn++
                    state.player2Stats.secondServesTotal++
                }
            }
            
            addPoint(isPlayer1)
            state.isFirstServe = true
            _matchState.value = state
            checkGameAndSetStatus()
        }
    }
    
    /**
     * Tryb BASIC: Fault serwującego
     * 1. serwis → przejście na 2. serwis
     * 2. serwis → podwójny błąd, punkt dla odbierającego
     */
    fun handleBasicFault() {
        _matchState.value?.let { state ->
            if (state.isFirstServe) {
                saveStateBeforeAction(ActionType.FAULT, str(R.string.undo_fault_first))
                // 1. serwis nieudany — policz próbę
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
                saveStateBeforeAction(ActionType.DOUBLE_FAULT, str(R.string.undo_double_fault, serverName))
                
                if (state.isPlayer1Serving) {
                    state.player1Stats.doubleFaults++
                    state.player1Stats.secondServesTotal++
                    addPoint(false)
                } else {
                    state.player2Stats.doubleFaults++
                    state.player2Stats.secondServesTotal++
                    addPoint(true)
                }
                state.isFirstServe = true
                _matchState.value = state
                checkGameAndSetStatus()
            }
        }
    }
    
    /**
     * Rotuje serwującego w deblu (1 -> 2 -> 3 -> 4 -> 1)
     */
    private fun rotateDoublesServer(state: MatchState) {
        state.currentServer = when (state.currentServer) {
            1 -> 2  // Gracz 1 (Zespół A) -> Gracz 2 (Zespół B)
            2 -> 3  // Gracz 2 (Zespół B) -> Gracz 3 (Partner 1, Zespół A)
            3 -> 4  // Gracz 3 (Zespół A) -> Gracz 4 (Zespół B)
            4 -> 1  // Gracz 4 (Zespół B) -> Gracz 1 (Zespół A)
            else -> 1
        }
        
        // Ustaw isPlayer1Serving na podstawie currentServer
        // 1,3 = Zespół A (player1Serving = true)
        // 2,4 = Zespół B (player1Serving = false)
        state.isPlayer1Serving = (state.currentServer == 1 || state.currentServer == 3)
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
                    if (state.isDoubles) {
                        // W deblu rotuj serwującego zgodnie z kolejnością
                        rotateDoublesServer(state)
                    } else {
                        // W singlu zwykła zmiana
                        state.isPlayer1Serving = !state.isPlayer1Serving
                    }
                    logMatchEvent("serve_change")
                }
                
                // Zmiana stron co 6 punktów
                if (totalPoints > 0 && totalPoints % 6 == 0) {
                    state.sidesSwapped = !state.sidesSwapped
                    logMatchEvent("side_change")
                    pendingAnnouncementType = "side_change"
                    _matchState.value = state
                    _currentView.value = MatchView.ANNOUNCEMENT
                    return
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
                    val wasSuperTiebreak = state.isSuperTiebreak
                    
                    // Oblicz punkty przegranego tiebreak (do wyświetlenia np. 5-4(7))
                    val tiebreakLoserPts = if (player1Won) state.player2Points else state.player1Points
                    
                    if (wasSuperTiebreak) {
                        // Super tiebreak: zapisz rzeczywiste punkty (np. 10:5)
                        // Nie dodajemy gemu — super TB nie ma gemów
                        if (player1Won) state.player1Sets++ else state.player2Sets++
                        
                        state.setsHistory.add(
                            SetScore(
                                setNumber = state.setsHistory.size + 1,
                                player1Games = state.player1Points,
                                player2Games = state.player2Points,
                                tiebreakLoserPoints = tiebreakLoserPts,
                                isSuperTiebreak = true
                            )
                        )
                    } else {
                        // Zwykły tiebreak: dodaj gem zwycięzcy (aby było 5:4 zamiast 4:4)
                        if (player1Won) state.player1Games++ else state.player2Games++
                        if (player1Won) state.player1Sets++ else state.player2Sets++
                        
                        state.setsHistory.add(
                            SetScore(
                                setNumber = state.setsHistory.size + 1,
                                player1Games = state.player1Games,
                                player2Games = state.player2Games,
                                tiebreakLoserPoints = tiebreakLoserPts
                            )
                        )
                    }
                    
                    // Resetuj gemy i punkty
                    state.player1Games = 0
                    state.player2Games = 0
                    state.isTiebreak = false
                    state.isSuperTiebreak = false
                    
                    // Zmiana stron i reset gemów po tiebreaku
                    state.sidesSwapped = !state.sidesSwapped
                    state.totalGamesPlayed = 0
                    pendingAnnouncementType = "side_change"
                    
                    // Sprawdź czy mecz się skończył (szczególnie ważne dla Super TB)
                    if (state.shouldEndMatch()) {
                        state.isMatchFinished = true
                        state.matchDuration = System.currentTimeMillis() - state.matchStartTime
                        _matchState.value = state
                        
                        // Sekwencyjne zakończenie meczu
                        finalizeMatchOnServer(state)
                        
                        _currentView.value = MatchView.MATCH_FINISHED
                        return
                    }
                    
                    // Sprawdź czy należy rozpocząć super tiebreak po wygranym tiebreaku
                    // (gdy sety są np. 1:1 przy setsToWin=2)
                    val setsToWinTB = state.matchConfig.setsToWin
                    if (state.player1Sets == (setsToWinTB - 1) && state.player2Sets == (setsToWinTB - 1)) {
                        state.isSuperTiebreak = true
                        pendingAnnouncementType = "super_tiebreak"
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
                        pendingAnnouncementType = "side_change"
                    }
                }
                
                // Reset punktów
                state.player1Points = 0
                state.player2Points = 0
                
                // Zmiana serwującego po gemie
                if (state.isDoubles) {
                    // W deblu rotacja serwisów: 1 -> 2 -> 3 -> 4 -> 1
                    rotateDoublesServer(state)
                } else {
                    // W singlu zwykła zmiana
                    state.isPlayer1Serving = !state.isPlayer1Serving
                }
                
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
                        
                        // Sekwencyjne zakończenie meczu
                        finalizeMatchOnServer(state)
                        
                        _currentView.value = MatchView.MATCH_FINISHED
                        return
                    }
                    
                    // Sprawdź czy należy rozpocząć super tiebreak (remis w setach, np. 1:1 przy setsToWin=2)
                    val setsToWin = state.matchConfig.setsToWin
                    if (state.player1Sets == (setsToWin - 1) && state.player2Sets == (setsToWin - 1)) {
                        state.isSuperTiebreak = true
                        pendingAnnouncementType = "super_tiebreak"
                    }
                    
                    // Resetuj gemy i licznik rozegranych gemów na nowy set
                    state.player1Games = 0
                    state.player2Games = 0
                    state.totalGamesPlayed = 0
                }
                
                // Sprawdź czy należy rozpocząć tiebreak (6:6)
                if (state.shouldStartTiebreak() && !state.isSuperTiebreak) {
                    state.isTiebreak = true
                    pendingAnnouncementType = "tiebreak"
                }
                
                _matchState.value = state
                
                // Jeśli jest ogłoszenie, pokaż kartę ogłoszenia zamiast od razu wracać do gry
                if (pendingAnnouncementType != null) {
                    _currentView.value = MatchView.ANNOUNCEMENT
                } else {
                    _currentView.value = if (state.statsMode == StatsMode.BASIC) MatchView.BASIC_SCORING else MatchView.SERVE
                }
                
                // Synchronizuj wynik z serwerem po każdym gemie
                syncMatchWithServer()
            } else {
                // Gem trwa dalej
                
                // W trybie no-advantage, przy 40-40 (3-3) pokaż ogłoszenie "deciding point"
                if (state.noAdvantage && state.player1Points == 3 && state.player2Points == 3
                    && !state.isTiebreak && !state.isSuperTiebreak) {
                    pendingAnnouncementType = "deciding_point"
                    _matchState.value = state
                    _currentView.value = MatchView.ANNOUNCEMENT
                } else {
                    _currentView.value = if (state.statsMode == StatsMode.BASIC) MatchView.BASIC_SCORING else MatchView.SERVE
                }
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
            MatchView.BASIC_SCORING -> _currentView.value = MatchView.SERVER_SELECTION
            else -> {}
        }
    }
    
    /**
     * Sekwencyjne zakończenie meczu na serwerze.
     * Kolejność: sync → match_end event → finish → statistics → local save
     */
    private fun finalizeMatchOnServer(state: MatchState) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Wyślij aktualny stan meczu z sets_history (PUT /matches/{id})
                state.matchId?.let { matchId ->
                    try {
                        val match = state.toMatch()
                        apiService.updateMatch(matchId, match)
                    } catch (e: Exception) {
                        AppLogger.error("finalizeMatch", "sync final state: ${e.message}")
                    }
                }

                // 2. Wyślij event match_end (POST /match-events) z pełnym sets_history
                try {
                    val event = MatchEvent(
                        courtId = state.courtId,
                        eventType = "match_end",
                        player1 = PlayerInfo(
                            name = state.player1.getDisplayName(),
                            fullName = state.player1.getFullName(),
                            flag = state.player1.flag,
                            isServing = state.isPlayer1Serving
                        ),
                        player2 = PlayerInfo(
                            name = state.player2.getDisplayName(),
                            fullName = state.player2.getFullName(),
                            flag = state.player2.flag,
                            isServing = !state.isPlayer1Serving
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
                        batteryLevel = getBatteryLevel(),
                        isCharging = isBatteryCharging()
                    )
                    apiService.logMatchEvent(event)
                } catch (e: Exception) {
                    AppLogger.error("finalizeMatch", "match_end event: ${e.message}")
                }

                // 3. Zakończ mecz na serwerze (POST /matches/{id}/finish)
                state.matchId?.let { matchId ->
                    try {
                        apiService.finishMatch(matchId)
                    } catch (e: Exception) {
                        AppLogger.error("finalizeMatch", "finish: ${e.message}")
                    }
                }

                // 4. Wyślij statystyki (POST /match-statistics)
                val statisticsRequest = state.toMatchStatisticsRequest()
                if (statisticsRequest != null) {
                    try {
                        apiService.sendMatchStatistics(statisticsRequest)
                    } catch (e: Exception) {
                        AppLogger.error("finalizeMatch", "statistics: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                AppLogger.error("finalizeMatch", "overall: ${e.message}")
            }

            // 5. Zapisz lokalnie do Room DB
            try {
                matchHistoryRepository.saveMatch(state)
            } catch (e: Exception) {
                AppLogger.error("finalizeMatch", "local save: ${e.message}")
            }
        }
    }

    /**
     * Loguje zdarzenie meczowe do serwera
     */
    private fun logMatchEvent(eventType: String) {
        val state = _matchState.value ?: return
        
        AppLogger.action("Match", eventType, "score=${state.player1Points}-${state.player2Points} games=${state.player1Games}-${state.player2Games} sets=${state.player1Sets}-${state.player2Sets}")
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val event = MatchEvent(
                    courtId = state.courtId,
                    eventType = eventType,
                    player1 = PlayerInfo(
                        name = state.player1.getDisplayName(),
                        fullName = state.player1.getFullName(),
                        flag = state.player1.flag,
                        isServing = state.isPlayer1Serving
                    ),
                    player2 = PlayerInfo(
                        name = state.player2.getDisplayName(),
                        fullName = state.player2.getFullName(),
                        flag = state.player2.flag,
                        isServing = !state.isPlayer1Serving
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
                    batteryLevel = getBatteryLevel(),
                    isCharging = isBatteryCharging()
                )
                
                val response = apiService.logMatchEvent(event)
                if (!response.isSuccessful) {
                    AppLogger.error("logMatchEvent", "HTTP ${response.code()} for $eventType")
                }
            } catch (e: Exception) {
                AppLogger.error("logMatchEvent", "$eventType: ${e.message}")
                // Nie przerywamy działania aplikacji przy błędzie logowania
            }
        }
    }
    
    /**
     * Zapisuje zakończony mecz do bazy danych
     */
    private fun saveMatchToDatabase(state: MatchState) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                matchHistoryRepository.saveMatch(state)
            } catch (e: Exception) {
                AppLogger.error("saveMatchToDatabase", e)
                // Nie przerywamy działania aplikacji przy błędzie zapisu
            }
        }
    }
    
    /**
     * Synchronizuje aktualny stan meczu z serwerem
     */
    fun syncMatchWithServer() {
        _matchState.value?.let { state ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    if (state.matchId == null) {
                        // Pierwszy sync - utwórz mecz na serwerze
                        val match = state.toMatch()
                        val response = apiService.createMatch(match)
                        
                        if (response.isSuccessful && response.body() != null) {
                            val created = response.body()!!
                            state.matchId = created.id
                            AppLogger.api("createMatch", "OK id=${state.matchId} phase=${created.phase}")
                            
                            // Handle bracket warning
                            created.bracketWarning?.let { warning ->
                                _bracketWarning.postValue(BracketWarningEvent(warning, created.id))
                            }
                        } else {
                            AppLogger.api("createMatch", "FAIL ${response.code()}")
                        }
                    } else {
                        // Aktualizuj istniejący mecz
                        val match = state.toMatch()
                        val response = apiService.updateMatch(state.matchId!!, match)
                        
                        if (!response.isSuccessful) {
                            AppLogger.api("updateMatch", "FAIL ${response.code()}")
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.error("syncMatchWithServer", e)
                    // Nie przerywamy działania aplikacji przy błędzie synchronizacji
                }
            }
        }
    }
    
    fun clearBracketWarning() {
        _bracketWarning.value = null
    }

    /**
     * Kończy mecz na serwerze
     */
    fun finishMatchOnServer() {
        _matchState.value?.let { state ->
            state.matchId?.let { matchId ->
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val response = apiService.finishMatch(matchId)
                        if (!response.isSuccessful) {
                            AppLogger.api("finishMatch", "FAIL ${response.code()}")
                        }
                    } catch (e: Exception) {
                        AppLogger.error("finishMatchOnServer", e)
                    }
                }
            }
        }
    }
    
    /**
     * Wysyła statystyki meczu do API
     */
    fun sendMatchStatistics() {
        _matchState.value?.let { state ->
            val statisticsRequest = state.toMatchStatisticsRequest()
            if (statisticsRequest != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val response = apiService.sendMatchStatistics(statisticsRequest)
                        if (response.isSuccessful) {
                            AppLogger.api("sendStatistics", "OK")
                        } else {
                            AppLogger.api("sendStatistics", "FAIL ${response.code()}")
                        }
                    } catch (e: Exception) {
                        AppLogger.error("sendMatchStatistics", e)
                    }
                }
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
    BASIC_SCORING,     // Uproszczony widok (Win/Fault)
    ANNOUNCEMENT,      // Ogłoszenie (zmiana stron, tiebreak, super tiebreak)
    MATCH_FINISHED     // Koniec meczu
}
