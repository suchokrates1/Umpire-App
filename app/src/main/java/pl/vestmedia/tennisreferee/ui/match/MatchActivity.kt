package pl.vestmedia.tennisreferee.ui.match

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.databinding.ActivityMatchBinding
import pl.vestmedia.tennisreferee.databinding.LayoutScoreboardBinding
import pl.vestmedia.tennisreferee.databinding.LayoutServerSelectionBinding
import pl.vestmedia.tennisreferee.databinding.LayoutServeBinding
import pl.vestmedia.tennisreferee.databinding.LayoutRallyBinding
import pl.vestmedia.tennisreferee.databinding.LayoutBasicScoringBinding
import pl.vestmedia.tennisreferee.databinding.LayoutMatchFinishedBinding
import pl.vestmedia.tennisreferee.databinding.LayoutAnnouncementBinding
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.utils.AppLogger

/**
 * Match activity that drives the live scoring flow.
 */
class MatchActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMatchBinding
    private lateinit var scoreboardBinding: LayoutScoreboardBinding
    private lateinit var serverSelectionBinding: LayoutServerSelectionBinding
    private lateinit var serveBinding: LayoutServeBinding
    private lateinit var rallyBinding: LayoutRallyBinding
    private lateinit var basicScoringBinding: LayoutBasicScoringBinding
    private lateinit var matchFinishedBinding: LayoutMatchFinishedBinding
    private lateinit var announcementBinding: LayoutAnnouncementBinding
    private lateinit var serverSelectionViewBinder: ServerSelectionViewBinder
    private lateinit var scoreboardRenderer: ScoreboardRenderer
    private lateinit var courtSideNamesRenderer: CourtSideNamesRenderer
    private lateinit var scoringButtonsController: ScoringButtonsController
    private lateinit var announcementController: AnnouncementController
    private lateinit var matchFinishController: MatchFinishController
    private lateinit var courtSideSwapAnimator: CourtSideSwapAnimator
    private lateinit var matchDialogsController: MatchDialogsController
    private lateinit var matchTimerRenderer: MatchTimerRenderer
    private lateinit var matchToolbarRenderer: MatchToolbarRenderer
    private lateinit var matchViewSwitcher: MatchViewSwitcher
    private lateinit var activeMatchStore: ActiveMatchStore
    private var activeMatchUuid: String? = null
    private val viewModel: MatchViewModel by viewModels()
    
    companion object {
        private const val EXTRA_MATCH_UUID = "match_uuid"
        const val EXTRA_MATCH_STATE = "match_state"
        const val EXTRA_IS_DOUBLES = "is_doubles"
        const val EXTRA_TEAM1_COLOR = "team1_color"
        const val EXTRA_TEAM2_COLOR = "team2_color"
        private const val EXTRA_MATCH_RESULT_ACTION = "match_result_action"
        const val RESULT_NEXT_MATCH_SAME_SETUP = "next_match_same_setup"
        const val RESULT_NEXT_MATCH_NEW_SETUP = "next_match_new_setup"

        fun createIntent(context: Context, matchUuid: String, isDoubles: Boolean): Intent {
            return Intent(context, MatchActivity::class.java).apply {
                putExtra(EXTRA_MATCH_UUID, matchUuid)
                if (isDoubles) {
                    putExtra(EXTRA_IS_DOUBLES, true)
                    putExtra(EXTRA_TEAM1_COLOR, R.color.team1_color)
                    putExtra(EXTRA_TEAM2_COLOR, R.color.team2_color)
                }
            }
        }

        fun resultIntent(action: String): Intent {
            return Intent().putExtra(EXTRA_MATCH_RESULT_ACTION, action)
        }

        fun resultAction(data: Intent?): String? {
            return data?.getStringExtra(EXTRA_MATCH_RESULT_ACTION)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMatchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        activeMatchStore = ActiveMatchStore(this)
        
        // Inicjalizuj bindingi dla included layoutów
        scoreboardBinding = LayoutScoreboardBinding.bind(binding.layoutScoreboard.root)
        serverSelectionBinding = LayoutServerSelectionBinding.bind(binding.layoutServerSelection.root)
        serveBinding = LayoutServeBinding.bind(binding.layoutServe.root)
        rallyBinding = LayoutRallyBinding.bind(binding.layoutRally.root)
        basicScoringBinding = LayoutBasicScoringBinding.bind(binding.layoutBasicScoring.root)
        matchFinishedBinding = LayoutMatchFinishedBinding.bind(binding.layoutMatchFinished.root)
        announcementBinding = LayoutAnnouncementBinding.bind(binding.layoutAnnouncement.root)
        scoreboardRenderer = ScoreboardRenderer(this, scoreboardBinding)
        courtSideSwapAnimator = CourtSideSwapAnimator(serverSelectionBinding)
        matchTimerRenderer = MatchTimerRenderer(scoreboardBinding) { viewModel.matchState.value }
        matchToolbarRenderer = MatchToolbarRenderer(this)
        matchDialogsController = MatchDialogsController(
            activity = this,
            onUndoConfirmed = { viewModel.undoLastAction() },
            getMatchState = { viewModel.matchState.value },
            onFinishConfirmed = { request -> viewModel.finishMatchWithOutcome(request) },
            onExitConfirmed = { finish() },
            onBracketWarningCleared = { viewModel.clearBracketWarning() }
        )
        courtSideNamesRenderer = CourtSideNamesRenderer(
            context = this,
            serverSelectionBinding = serverSelectionBinding,
            serveBinding = serveBinding,
            rallyBinding = rallyBinding,
            basicScoringBinding = basicScoringBinding
        )
        matchFinishController = MatchFinishController(
            activity = this,
            binding = matchFinishedBinding,
            onNextMatch = { action -> finishWithResult(action) }
        )
        scoringButtonsController = ScoringButtonsController(
            context = this,
            serveBinding = serveBinding,
            rallyBinding = rallyBinding,
            basicScoringBinding = basicScoringBinding,
            getState = { viewModel.matchState.value },
            onAce = { viewModel.handleAce() },
            onFault = { viewModel.handleFault() },
            onFootFault = { viewModel.handleFootFault() },
            onBallInPlay = { viewModel.handleBallInPlay() },
            onWinner = { viewModel.handleWinner(it) },
            onForcedError = { viewModel.handleForcedError(it) },
            onUnforcedError = { viewModel.handleUnforcedError(it) },
            onBasicWin = { viewModel.handleBasicWin(it) },
            onBasicFault = { viewModel.handleBasicFault() },
            onButtonLogged = { action, detail -> AppLogger.button("Match", action, detail) }
        )
        announcementController = AnnouncementController(
            context = this,
            binding = announcementBinding,
            getAnnouncementType = { viewModel.pendingAnnouncementType },
            onContinue = { viewModel.continueFromAnnouncement() },
            onSkipSideChange = { viewModel.skipSideChange() },
            onButtonLogged = { action, detail -> AppLogger.button("Match", action, detail) }
        )
        serverSelectionViewBinder = ServerSelectionViewBinder(
            context = this,
            binding = serverSelectionBinding,
            getState = { viewModel.matchState.value },
            onServerSelected = { viewModel.setFirstServer(it) },
            onSwapSides = {
                courtSideSwapAnimator.animate()
                viewModel.swapSides()
            },
            onButtonLogged = { AppLogger.button("Match", it) }
        )
        matchViewSwitcher = MatchViewSwitcher(
            binding = binding,
            getState = { viewModel.matchState.value },
            renderAnnouncement = { announcementController.render(it) },
            renderServe = { state, animateSecondServeText ->
                scoringButtonsController.renderServeView(state, animateSecondServeText)
            },
            renderBasicScoring = { scoringButtonsController.renderBasicScoringView(it) },
            renderMatchFinished = { matchFinishController.render(it) },
            onViewShown = { view ->
                AppLogger.screen("Match:${view.name}")
                (application as TennisRefereeApp).healthCheckManager.currentScreen = "Match:${view.name}"
            }
        )
        
        val matchState = loadInitialMatchState()
        
        if (matchState == null) {
            finish()
            return
        }
        
        activeMatchUuid = matchState.clientMatchUuid
        if (viewModel.matchState.value == null) {
            viewModel.initializeMatch(matchState)
        }
        AppLogger.screen("Match", "court=${matchState.courtId} p1=${matchState.player1.getDisplayName()} p2=${matchState.player2.getDisplayName()}")
        (application as TennisRefereeApp).healthCheckManager.currentScreen = "Match"
        
        // Nie wyłączaj ekranu podczas meczu
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        setupObservers()
        setupListeners()
        
        // Obsługa przycisku wstecz - potwierdzenie podczas meczu
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val state = viewModel.matchState.value
                if (state?.isMatchFinished == true) {
                    finish()
                } else {
                    matchDialogsController.showExitConfirmation()
                }
            }
        })
    }
    
    private fun setupObservers() {
        viewModel.matchState.observe(this) { state ->
            activeMatchStore.save(state)
            scoreboardRenderer.render(state)
            courtSideNamesRenderer.render(state)
            serverSelectionViewBinder.render(state)
            if (viewModel.currentView.value == MatchView.SERVE) {
                scoringButtonsController.renderServeView(state, animateSecondServeText = true)
            }
            matchTimerRenderer.render(state)
            // Aktualizuj widok basic scoring gdy zmienia się stan (np. fault → 2. serwis)
            if (viewModel.currentView.value == MatchView.BASIC_SCORING) {
                scoringButtonsController.renderBasicScoringView(state)
            }
        }
        
        viewModel.currentView.observe(this) { view ->
            matchViewSwitcher.show(view)
        }
        
        viewModel.canUndo.observe(this) { canUndo ->
            binding.buttonUndo.isEnabled = canUndo
            binding.buttonUndo.alpha = if (canUndo) 1.0f else 0.5f
            // Przycisk zawsze widoczny, tylko enabled/disabled
        }
        
        viewModel.undoMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearUndoMessage()
            }
        }
        
        viewModel.bracketWarning.observe(this) { event ->
            event?.let { matchDialogsController.showBracketWarning(it) }
        }

        viewModel.syncStatus.observe(this) { status ->
            matchToolbarRenderer.renderSyncStatus(status)
        }
        
        // Match announcements — now handled as inline ANNOUNCEMENT view
        // (no more AlertDialog popups)
    }

    private fun loadInitialMatchState(): MatchState? {
        intent.getStringExtra(EXTRA_MATCH_UUID)?.let { matchUuid ->
            activeMatchStore.get(matchUuid)?.let { return it }
        }

        intent.extras?.classLoader = MatchState::class.java.classLoader
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_MATCH_STATE, MatchState::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_MATCH_STATE)
        }?.also { activeMatchStore.save(it) }
    }

    private fun finishWithResult(action: String) {
        activeMatchUuid?.let { activeMatchStore.clear(it) }
        setResult(RESULT_OK, resultIntent(action))
        finish()
    }
    
    private fun setupListeners() {
        serverSelectionViewBinder.bind()
        scoringButtonsController.bind()
        announcementController.bind()
        
        // Przycisk Cofnij z potwierdzeniem
        binding.buttonUndo.setOnClickListener {
            AppLogger.button("Match", "Undo")
            matchDialogsController.showUndoConfirmation()
        }
        
        // Przycisk zakończenia meczu z potwierdzeniem
        binding.buttonBack.setOnClickListener {
            AppLogger.button("Match", "FinishMatch")
            matchDialogsController.showFinishMatchConfirmation()
        }
        
    }
    
    override fun onDestroy() {
        super.onDestroy()
        matchTimerRenderer.clear()
    }
    

}

