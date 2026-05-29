package pl.vestmedia.tennisreferee.ui.match

import android.app.Application
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
import pl.vestmedia.tennisreferee.domain.match.model.*
import pl.vestmedia.tennisreferee.domain.match.MatchActionReducer
import pl.vestmedia.tennisreferee.domain.match.MatchActionResult
import pl.vestmedia.tennisreferee.domain.match.MatchCommand
import pl.vestmedia.tennisreferee.domain.match.MatchFinishOutcomeApplier
import pl.vestmedia.tennisreferee.domain.match.MatchPointEvent
import pl.vestmedia.tennisreferee.domain.match.MatchProgressEvent
import pl.vestmedia.tennisreferee.domain.match.MatchProgressReducer
import pl.vestmedia.tennisreferee.domain.match.MatchProgressScreen
import pl.vestmedia.tennisreferee.domain.match.MatchUndoManager
import pl.vestmedia.tennisreferee.domain.match.MatchUndoResult
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

    private val _syncStatus = MutableLiveData<SyncStatus>(SyncStatus.IDLE)
    val syncStatus: LiveData<SyncStatus> = _syncStatus

    private val batteryInfoProvider = DeviceBatteryInfoProvider(application)
    private val syncDiagnosticsStore = SyncDiagnosticsStore(application)
    
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
            applyMatchCommand(state, MatchCommand.ToggleSides)
        }
        AppLogger.action("Match", "SideChangeSkipped")
        continueFromAnnouncement()
    }
    
    private val matchSyncCoordinator = MatchSyncCoordinator(
        apiService = RetrofitClient.apiService,
        matchHistoryRepository = (application as TennisRefereeApp).matchHistoryRepository,
        batteryInfoProvider = { batteryInfoProvider.current() },
        onSyncStatus = { status -> _syncStatus.postValue(status) },
        onBracketWarning = { warning, matchId -> _bracketWarning.postValue(BracketWarningEvent(warning, matchId)) },
        onSyncDiagnostics = { status, error -> syncDiagnosticsStore.record(status, error) }
    )
    
    /**
     * Inicjalizuje nowy mecz
     */
    fun initializeMatch(matchState: MatchState) {
        _matchState.value = matchState
    }
    
    /**
     * Ustawia który gracz serwuje pierwszy
     */
    fun setFirstServer(serverNumber: Int) {
        _matchState.value?.let { state ->
            applyMatchCommand(state, MatchCommand.StartMatch(serverNumber, System.currentTimeMillis()))
            _currentView.value = scoringViewFor(state)
            
            // Log match start event
            logMatchEvent("match_start")
        }
    }
    
    /**
     * Zamienia strony (gracze przechodzą na przeciwległe strony kortu)
     */
    fun swapSides() {
        _matchState.value?.let { state ->
            applyMatchCommand(state, MatchCommand.ToggleSides)
        }
    }
    
    /**
     * Zapisuje aktualny stan przed wykonaniem akcji
     */
    private fun saveStateBeforeAction(actionType: ActionType, description: String) {
        _matchState.value?.let { state ->
            MatchUndoManager.saveStateBeforeAction(state, actionType, description)
            _canUndo.value = true
        }
    }
    
    /**
     * Cofa ostatnią akcję
     */
    fun undoLastAction() {
        _matchState.value?.let { state ->
            when (val result = MatchUndoManager.undoLastAction(state)) {
                MatchUndoResult.NoAction -> {
                    _undoMessage.value = getApplication<Application>().getString(R.string.no_actions_to_undo)
                }
                is MatchUndoResult.Restored -> {
                    _canUndo.value = result.canUndo
                    _undoMessage.value = getApplication<Application>().getString(R.string.undo_action_format, result.description)
                    _matchState.value = state
                    _currentView.value = if (state.statsMode == StatsMode.BASIC) MatchView.BASIC_SCORING else MatchView.SERVE
                }
            }
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
            val serverName = serverName(state)
            saveStateBeforeAction(ActionType.ACE, str(R.string.undo_ace, serverName))
            applyMatchCommand(state, MatchCommand.Ace)
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
            } else {
                // Podwójny błąd
                val serverName = serverName(state)
                saveStateBeforeAction(ActionType.DOUBLE_FAULT, str(R.string.undo_double_fault, serverName))
            }
            applyMatchCommand(state, MatchCommand.Fault)
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
            } else {
                val serverName = serverName(state)
                saveStateBeforeAction(ActionType.FOOT_FAULT, str(R.string.undo_foot_fault_double, serverName))
            }
            applyMatchCommand(state, MatchCommand.FootFault)
        }
    }
    
    /**
     * Piłka w grze - przejście do widoku rozgrywki
     */
    fun handleBallInPlay() {
        _matchState.value?.let { state ->
            applyMatchCommand(state, MatchCommand.BallInPlay)
        }
    }
    
    /**
     * Obsługuje winner - uderzenie kończące wymianę
     */
    fun handleWinner(isPlayer1: Boolean) {
        _matchState.value?.let { state ->
            val playerName = playerName(state, isPlayer1)
            saveStateBeforeAction(ActionType.WINNER, str(R.string.undo_winner, playerName))
            applyMatchCommand(state, MatchCommand.Winner(isPlayer1))
        }
    }
    
    /**
     * Obsługuje wymuszone błędy
     */
    fun handleForcedError(isPlayer1: Boolean) {
        _matchState.value?.let { state ->
            val playerName = playerName(state, isPlayer1)
            saveStateBeforeAction(ActionType.FORCED_ERROR, str(R.string.undo_forced_error, playerName))
            applyMatchCommand(state, MatchCommand.ForcedError(isPlayer1))
        }
    }
    
    /**
     * Obsługuje niewymuszone błędy
     */
    fun handleUnforcedError(isPlayer1: Boolean) {
        _matchState.value?.let { state ->
            val playerName = playerName(state, isPlayer1)
            saveStateBeforeAction(ActionType.UNFORCED_ERROR, str(R.string.undo_unforced_error, playerName))
            applyMatchCommand(state, MatchCommand.UnforcedError(isPlayer1))
        }
    }
    
    // ===== BASIC MODE =====
    
    /**
     * Tryb BASIC: Gracz wygrywa punkt (serwujący lub odbierający)
     * W basic mode nie rozróżniamy ace/winner/forced/unforced — po prostu WIN.
     */
    fun handleBasicWin(isPlayer1: Boolean) {
        _matchState.value?.let { state ->
            val playerName = playerName(state, isPlayer1)
            saveStateBeforeAction(ActionType.WINNER, str(R.string.undo_win, playerName))
            applyMatchCommand(state, MatchCommand.BasicWin(isPlayer1))
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
            } else {
                // Podwójny błąd
                val serverName = serverName(state)
                saveStateBeforeAction(ActionType.DOUBLE_FAULT, str(R.string.undo_double_fault, serverName))
            }
            applyMatchCommand(state, MatchCommand.BasicFault)
        }
    }

    private fun applyMatchCommand(state: MatchState, command: MatchCommand) {
        val result = MatchActionReducer.reduce(state, command)

        result.pointWinner?.let { pointWinner ->
            val pointResult = MatchActionReducer.reduce(state, MatchCommand.PointWon(pointWinner))
            handlePointResult(state, pointResult)
        }
        handlePointResult(state, result)
        _matchState.value = state

        if (result.transitionToRally) {
            _currentView.value = MatchView.RALLY
        }
        if (result.pointWinner != null || result.pointScored) {
            checkGameAndSetStatus()
        }
    }

    private fun handlePointResult(state: MatchState, result: MatchActionResult) {
        result.pointEvents.forEach { event ->
            when (event) {
                MatchPointEvent.Point -> logMatchEvent("point")
                MatchPointEvent.ServeChange -> logMatchEvent("serve_change")
                MatchPointEvent.SideChange -> logMatchEvent("side_change")
            }
        }

        result.announcementType?.let { pendingAnnouncementType = it }
        if (result.showAnnouncementImmediately) {
            _matchState.value = state
            _currentView.value = MatchView.ANNOUNCEMENT
        }
    }

    private fun serverName(state: MatchState): String {
        return if (state.isDoubles) {
            state.getCurrentServerName()
        } else if (state.isPlayer1Serving) {
            state.player1.getDisplayName()
        } else {
            state.player2.getDisplayName()
        }
    }

    private fun playerName(state: MatchState, isPlayer1: Boolean): String {
        return if (isPlayer1) state.player1.getDisplayName() else state.player2.getDisplayName()
    }
    
    /**
     * Sprawdza czy gem/set został wygrany i aktualizuje stan
     */
    private fun checkGameAndSetStatus() {
        _matchState.value?.let { state ->
            val result = MatchProgressReducer.reduceAfterPoint(
                state = state,
                currentAnnouncementType = pendingAnnouncementType,
                nowMs = System.currentTimeMillis()
            )

            result.events.forEach { event ->
                when (event) {
                    MatchProgressEvent.Game -> logMatchEvent("game")
                    MatchProgressEvent.Set -> logMatchEvent("set")
                }
            }
            pendingAnnouncementType = result.announcementType

            if (result.publishState) {
                _matchState.value = state
            }

            if (result.finalizeMatch) {
                finalizeMatchOnServer(state)
            }

            _currentView.value = when (result.nextScreen) {
                MatchProgressScreen.Announcement -> MatchView.ANNOUNCEMENT
                MatchProgressScreen.MatchFinished -> MatchView.MATCH_FINISHED
                MatchProgressScreen.Scoring -> scoringViewFor(state)
            }

            if (result.syncMatch) {
                syncMatchWithServer()
            }
        }
    }

    private fun scoringViewFor(state: MatchState): MatchView {
        return if (state.statsMode == StatsMode.BASIC) MatchView.BASIC_SCORING else MatchView.SERVE
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
            matchSyncCoordinator.finalizeMatch(state)
        }
    }

    fun finishMatchWithOutcome(request: FinishMatchRequest) {
        _matchState.value?.let { state ->
            MatchFinishOutcomeApplier.apply(state, request, System.currentTimeMillis())
            _matchState.value = state
            _currentView.value = MatchView.MATCH_FINISHED
            viewModelScope.launch(Dispatchers.IO) {
                matchSyncCoordinator.finalizeMatch(state, request)
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
            matchSyncCoordinator.logMatchEvent(state, eventType)
        }
    }
    
    /**
     * Synchronizuje aktualny stan meczu z serwerem
     */
    fun syncMatchWithServer() {
        _matchState.value?.let { state ->
            viewModelScope.launch(Dispatchers.IO) {
                matchSyncCoordinator.syncMatch(state)
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
            viewModelScope.launch(Dispatchers.IO) {
                matchSyncCoordinator.finishMatch(state)
            }
        }
    }
    
    /**
     * Wysyła statystyki meczu do API
     */
    fun sendMatchStatistics() {
        _matchState.value?.let { state ->
            viewModelScope.launch(Dispatchers.IO) {
                matchSyncCoordinator.sendStatistics(state)
            }
        }
    }
}
