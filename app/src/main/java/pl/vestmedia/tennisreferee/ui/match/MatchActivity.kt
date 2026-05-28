package pl.vestmedia.tennisreferee.ui.match

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.databinding.ActivityMatchBinding
import pl.vestmedia.tennisreferee.databinding.LayoutScoreboardBinding
import pl.vestmedia.tennisreferee.databinding.LayoutServerSelectionBinding
import pl.vestmedia.tennisreferee.databinding.LayoutServeBinding
import pl.vestmedia.tennisreferee.databinding.LayoutRallyBinding
import pl.vestmedia.tennisreferee.databinding.LayoutBasicScoringBinding
import pl.vestmedia.tennisreferee.databinding.LayoutMatchFinishedBinding
import pl.vestmedia.tennisreferee.databinding.LayoutAnnouncementBinding
import pl.vestmedia.tennisreferee.data.model.MatchState
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.utils.AppLogger
import java.util.Locale

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
    private val viewModel: MatchViewModel by viewModels()
    
    private val timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    
    companion object {
        const val EXTRA_MATCH_STATE = "match_state"
        const val EXTRA_IS_DOUBLES = "is_doubles"
        const val EXTRA_TEAM1_COLOR = "team1_color"
        const val EXTRA_TEAM2_COLOR = "team2_color"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMatchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Inicjalizuj bindingi dla included layoutów
        scoreboardBinding = LayoutScoreboardBinding.bind(binding.layoutScoreboard.root)
        serverSelectionBinding = LayoutServerSelectionBinding.bind(binding.layoutServerSelection.root)
        serveBinding = LayoutServeBinding.bind(binding.layoutServe.root)
        rallyBinding = LayoutRallyBinding.bind(binding.layoutRally.root)
        basicScoringBinding = LayoutBasicScoringBinding.bind(binding.layoutBasicScoring.root)
        matchFinishedBinding = LayoutMatchFinishedBinding.bind(binding.layoutMatchFinished.root)
        announcementBinding = LayoutAnnouncementBinding.bind(binding.layoutAnnouncement.root)
        scoreboardRenderer = ScoreboardRenderer(this, scoreboardBinding)
        courtSideNamesRenderer = CourtSideNamesRenderer(
            context = this,
            serverSelectionBinding = serverSelectionBinding,
            serveBinding = serveBinding,
            rallyBinding = rallyBinding,
            basicScoringBinding = basicScoringBinding
        )
        matchFinishController = MatchFinishController(this, matchFinishedBinding)
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
                animateSwapSides()
                viewModel.swapSides()
            },
            onButtonLogged = { AppLogger.button("Match", it) }
        )
        
        intent.extras?.classLoader = MatchState::class.java.classLoader

        // Pobierz stan meczu z Intent
        val matchState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_MATCH_STATE, MatchState::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_MATCH_STATE)
        }
        
        if (matchState == null) {
            finish()
            return
        }
        
        viewModel.initializeMatch(matchState)
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
                    AlertDialog.Builder(this@MatchActivity)
                        .setTitle(R.string.confirm_exit_title)
                        .setMessage(R.string.confirm_exit_message)
                        .setPositiveButton(R.string.yes) { _, _ -> finish() }
                        .setNegativeButton(R.string.no, null)
                        .show()
                }
            }
        })
    }
    
    private fun setupObservers() {
        viewModel.matchState.observe(this) { state ->
            updateScoreboard(state)
            updatePlayerNames(state)
            updateServerSelectionButtons(state)
            updateServeView(state)
            updateTimer(state)
            // Aktualizuj widok basic scoring gdy zmienia się stan (np. fault → 2. serwis)
            if (viewModel.currentView.value == MatchView.BASIC_SCORING) {
                updateBasicScoringView(state)
            }
        }
        
        viewModel.currentView.observe(this) { view ->
            showView(view)
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
            event?.let { showBracketWarningDialog(it) }
        }

        viewModel.syncStatus.observe(this) { status ->
            supportActionBar?.subtitle = when (status) {
                SyncStatus.IDLE -> null
                SyncStatus.SYNCING -> getString(R.string.sync_status_syncing)
                SyncStatus.SYNCED -> getString(R.string.sync_status_synced)
                SyncStatus.FAILED -> getString(R.string.sync_status_failed)
                SyncStatus.OFFLINE -> getString(R.string.sync_status_offline)
            }
        }
        
        // Match announcements — now handled as inline ANNOUNCEMENT view
        // (no more AlertDialog popups)
    }
    
    private fun showBracketWarningDialog(event: MatchViewModel.BracketWarningEvent) {
        val (title, message) = when (event.type) {
            "different_groups" -> Pair(
                getString(R.string.bracket_warning_title),
                getString(R.string.bracket_warning_different_groups)
            )
            "no_bracket" -> Pair(
                getString(R.string.bracket_warning_title),
                getString(R.string.bracket_warning_friendly)
            )
            else -> return
        }
        
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.ok) { _, _ ->
                viewModel.clearBracketWarning()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun setupListeners() {
        serverSelectionViewBinder.bind()
        scoringButtonsController.bind()
        announcementController.bind()
        
        // Przycisk Cofnij z potwierdzeniem
        binding.buttonUndo.setOnClickListener {
            AppLogger.button("Match", "Undo")
            showUndoConfirmation()
        }
        
        // Przycisk zakończenia meczu z potwierdzeniem
        binding.buttonBack.setOnClickListener {
            AppLogger.button("Match", "FinishMatch")
            showFinishMatchConfirmation()
        }
        
    }
    
    /**
     * Animuje przejście między widokami z efektem slide/fade
     */
    private fun animateViewTransition(view: View, newVisibility: Int) {
        when (newVisibility) {
            View.VISIBLE -> {
                // Slide in z prawej strony (fade in)
                view.alpha = 0f
                view.translationX = 100f
                view.visibility = View.VISIBLE
                view.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(300)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }
            View.GONE -> {
                if (view.isVisible) {
                    // Slide out w lewo (fade out)
                    view.animate()
                        .alpha(0f)
                        .translationX(-100f)
                        .setDuration(200)
                        .setInterpolator(android.view.animation.AccelerateInterpolator())
                        .withEndAction {
                            view.visibility = View.GONE
                            view.alpha = 1f
                            view.translationX = 0f
                        }
                        .start()
                } else {
                    view.visibility = View.GONE
                }
            }
        }
    }
    
    private fun updatePlayerNames(state: MatchState) {
        courtSideNamesRenderer.render(state)
    }
    
    private fun updateBasicScoringView(state: MatchState) {
        scoringButtonsController.renderBasicScoringView(state)
    }
    
    private fun updateScoreboard(state: MatchState) {
        scoreboardRenderer.render(state)
    }
    
    private fun updateServerSelectionButtons(state: MatchState) {
        serverSelectionViewBinder.render(state)
    }

    private fun updateServeView(state: MatchState) {
        // Aktualizuj tylko jeśli widok SERVE jest aktywny
        if (viewModel.currentView.value != MatchView.SERVE) return
        scoringButtonsController.renderServeView(state, animateSecondServeText = true)
    }
    
    private fun showView(view: MatchView) {
        AppLogger.screen("Match:${view.name}")
        (application as TennisRefereeApp).healthCheckManager.currentScreen = "Match:${view.name}"
        // Ukryj wszystkie widoki z animacją slide out
        animateViewTransition(binding.layoutServerSelection.root, View.GONE)
        animateViewTransition(binding.layoutServe.root, View.GONE)
        animateViewTransition(binding.layoutRally.root, View.GONE)
        animateViewTransition(binding.layoutBasicScoring.root, View.GONE)
        animateViewTransition(binding.layoutMatchFinished.root, View.GONE)
        animateViewTransition(binding.layoutAnnouncement.root, View.GONE)
        
        // Scoreboard widoczny wszędzie oprócz wyboru serwującego
        binding.layoutScoreboard.root.visibility = if (view == MatchView.SERVER_SELECTION) {
            View.GONE
        } else {
            View.VISIBLE
        }
        
        viewModel.matchState.value?.let { state ->
            when (view) {
                MatchView.SERVER_SELECTION -> {
                    animateViewTransition(binding.layoutServerSelection.root, View.VISIBLE)
                }
                
                MatchView.ANNOUNCEMENT -> {
                    announcementController.render(state)
                    animateViewTransition(binding.layoutAnnouncement.root, View.VISIBLE)
                }
                
                MatchView.SERVE -> {
                    animateViewTransition(binding.layoutServe.root, View.VISIBLE)
                    scoringButtonsController.renderServeView(state, animateSecondServeText = false)
                }
                
                MatchView.RALLY -> {
                    animateViewTransition(binding.layoutRally.root, View.VISIBLE)
                    // Nazwiska są już ustawione w updatePlayerNames()
                }
                
                MatchView.BASIC_SCORING -> {
                    animateViewTransition(binding.layoutBasicScoring.root, View.VISIBLE)
                    updateBasicScoringView(state)
                }
                
                MatchView.MATCH_FINISHED -> {
                    animateViewTransition(binding.layoutMatchFinished.root, View.VISIBLE)
                    matchFinishController.render(state)
                }
            }
        }
    }
    
    /**
     * Aktualizuje wyświetlacz timera meczu
     */
    private fun updateTimer(state: MatchState) {
        if (state.matchStartTime > 0 && !state.isMatchFinished) {
            scoreboardBinding.textMatchTimer.visibility = View.VISIBLE
            startTimerUpdates()
        } else if (state.isMatchFinished) {
            stopTimerUpdates()
            // Pokaż końcowy czas meczu
            scoreboardBinding.textMatchTimer.text = formatDuration(state.matchDuration)
            scoreboardBinding.textMatchTimer.visibility = View.VISIBLE
        }
    }
    
    /**
     * Rozpoczyna okresowe aktualizacje timera
     */
    private fun startTimerUpdates() {
        if (timerRunnable != null) return // już działa
        
        timerRunnable = object : Runnable {
            override fun run() {
                viewModel.matchState.value?.let { state ->
                    if (state.matchStartTime > 0 && !state.isMatchFinished) {
                        val elapsed = System.currentTimeMillis() - state.matchStartTime
                        scoreboardBinding.textMatchTimer.text = formatDuration(elapsed)
                        timerHandler.postDelayed(this, 1000) // Aktualizuj co sekundę
                    }
                }
            }
        }
        timerHandler.post(timerRunnable!!)
    }
    
    /**
     * Zatrzymuje okresowe aktualizacje timera
     */
    private fun stopTimerUpdates() {
        timerRunnable?.let {
            timerHandler.removeCallbacks(it)
            timerRunnable = null
        }
    }

    /**
     * Formatuje czas trwania na format HH:MM:SS lub MM:SS
     */
    private fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = (durationMs / (1000 * 60 * 60))
        
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
    
    /**
     * Pokazuje dialog potwierdzenia cofnięcia akcji
     */
    private fun showUndoConfirmation() {
        AppLogger.dialog("UndoConfirm", "show")
        AlertDialog.Builder(this)
            .setTitle(R.string.undo)
            .setMessage(R.string.confirm_undo)
            .setPositiveButton(R.string.yes) { _, _ ->
                AppLogger.dialog("UndoConfirm", "YES")
                viewModel.undoLastAction()
            }
            .setNegativeButton(R.string.no) { _, _ ->
                AppLogger.dialog("UndoConfirm", "NO")
            }
            .show()
    }
    
    /**
     * Pokazuje dialog potwierdzenia zakończenia meczu
     */
    private fun showFinishMatchConfirmation() {
        AppLogger.dialog("FinishMatchConfirm", "show")
        AlertDialog.Builder(this)
            .setTitle(R.string.finish_match)
            .setMessage(R.string.confirm_finish_match)
            .setPositiveButton(R.string.yes) { _, _ ->
                AppLogger.dialog("FinishMatchConfirm", "YES")
                finish()
            }
            .setNegativeButton(R.string.no) { _, _ ->
                AppLogger.dialog("FinishMatchConfirm", "NO")
            }
            .show()
    }
    
    /**
     * Łagodna animacja zamiany stron - fade out + slide
     */
    private fun animateSwapSides() {
        val animatedButtons = listOf(
            serverSelectionBinding.buttonPlayer1Serves,
            serverSelectionBinding.buttonPlayer2Serves,
            serverSelectionBinding.buttonPlayer3Serves,
            serverSelectionBinding.buttonPlayer4Serves
        ).filter { it.isVisible }

        val fadeOut = animatedButtons.map { button ->
            ObjectAnimator.ofFloat(button, "alpha", 1f, 0f).apply {
                duration = 150
            }
        }

        val fadeIn = animatedButtons.map { button ->
            ObjectAnimator.ofFloat(button, "alpha", 0f, 1f).apply {
                duration = 150
            }
        }

        AnimatorSet().apply {
            playTogether(fadeOut)
            playTogether(fadeIn)
            fadeIn.forEach { play(it).after(fadeOut.first()) }
            start()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopTimerUpdates()
    }
    

}

