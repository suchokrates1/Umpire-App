package pl.vestmedia.tennisreferee.ui.match

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
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
import pl.vestmedia.tennisreferee.ui.playerselection.PlayerSelectionActivity
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
    private val viewModel: MatchViewModel by viewModels()
    
    // Śledzenie stanu serwisu do animacji przejścia na 2. serwis
    private var wasFirstServe: Boolean = true
    
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
        
        // Serwis - lewa strona (Player 1)
        serveBinding.buttonAceLeft.setOnClickListener {
            AppLogger.button("Match", "Ace", "side=left")
            viewModel.handleAce()
        }
        
        serveBinding.buttonFaultLeft.setOnClickListener {
            AppLogger.button("Match", "Fault", "side=left")
            viewModel.handleFault()
        }
        
        // Foot Fault - lewa strona
        serveBinding.buttonFootFaultLeft.setOnClickListener {
            AppLogger.button("Match", "FootFault", "side=left")
            viewModel.handleFootFault()
        }
        
        // Serwis - prawa strona (Player 2)
        serveBinding.buttonAceRight.setOnClickListener {
            AppLogger.button("Match", "Ace", "side=right")
            viewModel.handleAce()
        }
        
        serveBinding.buttonFaultRight.setOnClickListener {
            AppLogger.button("Match", "Fault", "side=right")
            viewModel.handleFault()
        }
        
        // Foot Fault - prawa strona
        serveBinding.buttonFootFaultRight.setOnClickListener {
            AppLogger.button("Match", "FootFault", "side=right")
            viewModel.handleFootFault()
        }
        
        // Ball in Play - wspólny przycisk dla obu graczy
        serveBinding.buttonBallInPlay.setOnClickListener {
            AppLogger.button("Match", "BallInPlay")
            viewModel.handleBallInPlay()
        }
        
        // Rally - lewa strona (Player 1 lub Player 2 w zależności od sidesSwapped)
        rallyBinding.buttonWinnerLeft.setOnClickListener {
            val isPlayer1 = !(viewModel.matchState.value?.sidesSwapped ?: false)
            AppLogger.button("Match", "Winner", "side=left isP1=$isPlayer1")
            viewModel.handleWinner(isPlayer1)
        }
        
        rallyBinding.buttonForcedErrorLeft.setOnClickListener {
            val isPlayer1 = !(viewModel.matchState.value?.sidesSwapped ?: false)
            AppLogger.button("Match", "ForcedError", "side=left isP1=$isPlayer1")
            viewModel.handleForcedError(isPlayer1)
        }
        
        rallyBinding.buttonUnforcedErrorLeft.setOnClickListener {
            val isPlayer1 = !(viewModel.matchState.value?.sidesSwapped ?: false)
            AppLogger.button("Match", "UnforcedError", "side=left isP1=$isPlayer1")
            viewModel.handleUnforcedError(isPlayer1)
        }
        
        // Rally - prawa strona (Player 2 lub Player 1 w zależności od sidesSwapped)
        rallyBinding.buttonWinnerRight.setOnClickListener {
            val isPlayer1 = viewModel.matchState.value?.sidesSwapped ?: false
            AppLogger.button("Match", "Winner", "side=right isP1=$isPlayer1")
            viewModel.handleWinner(isPlayer1)
        }
        
        rallyBinding.buttonForcedErrorRight.setOnClickListener {
            val isPlayer1 = viewModel.matchState.value?.sidesSwapped ?: false
            AppLogger.button("Match", "ForcedError", "side=right isP1=$isPlayer1")
            viewModel.handleForcedError(isPlayer1)
        }
        
        rallyBinding.buttonUnforcedErrorRight.setOnClickListener {
            val isPlayer1 = viewModel.matchState.value?.sidesSwapped ?: false
            AppLogger.button("Match", "UnforcedError", "side=right isP1=$isPlayer1")
            viewModel.handleUnforcedError(isPlayer1)
        }
        
        // ===== BASIC MODE Listeners =====
        // Win buttons - serwujący (lewa strona)
        basicScoringBinding.buttonWinServerLeft.setOnClickListener {
            val isPlayer1 = !(viewModel.matchState.value?.sidesSwapped ?: false)
            AppLogger.button("Match", "BasicWin", "side=serverLeft isP1=$isPlayer1")
            viewModel.handleBasicWin(isPlayer1)
        }
        basicScoringBinding.buttonFaultServerLeft.setOnClickListener {
            AppLogger.button("Match", "BasicFault", "side=left")
            viewModel.handleBasicFault()
        }
        // Win buttons - odbierający (lewa strona)
        basicScoringBinding.buttonWinReceiverLeft.setOnClickListener {
            val isPlayer1 = !(viewModel.matchState.value?.sidesSwapped ?: false)
            AppLogger.button("Match", "BasicWin", "side=receiverLeft isP1=$isPlayer1")
            viewModel.handleBasicWin(isPlayer1)
        }
        // Win buttons - serwujący (prawa strona)
        basicScoringBinding.buttonWinServerRight.setOnClickListener {
            val isPlayer1 = viewModel.matchState.value?.sidesSwapped ?: false
            AppLogger.button("Match", "BasicWin", "side=serverRight isP1=$isPlayer1")
            viewModel.handleBasicWin(isPlayer1)
        }
        basicScoringBinding.buttonFaultServerRight.setOnClickListener {
            AppLogger.button("Match", "BasicFault", "side=right")
            viewModel.handleBasicFault()
        }
        // Win buttons - odbierający (prawa strona)
        basicScoringBinding.buttonWinReceiverRight.setOnClickListener {
            val isPlayer1 = viewModel.matchState.value?.sidesSwapped ?: false
            AppLogger.button("Match", "BasicWin", "side=receiverRight isP1=$isPlayer1")
            viewModel.handleBasicWin(isPlayer1)
        }
        
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
        
        // Przycisk "Dalej" na karcie ogłoszenia
        announcementBinding.buttonAnnouncementContinue.setOnClickListener {
            AppLogger.button("Match", "AnnouncementContinue", viewModel.pendingAnnouncementType ?: "")
            viewModel.continueFromAnnouncement()
        }
        
        // Przycisk "Nie zmieniaj stron" – cofnij swap i kontynuuj
        announcementBinding.buttonAnnouncementSkipSideChange.setOnClickListener {
            AppLogger.button("Match", "SkipSideChange")
            viewModel.skipSideChange()
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
        // Aktualizuj nazwiska na przyciskach wyboru serwującego
        val leftPlayer = if (state.sidesSwapped) state.player2 else state.player1
        val rightPlayer = if (state.sidesSwapped) state.player1 else state.player2
        
        // Dla debla użyj nazw zespołów
        if (state.isDoubles) {
            val leftTeamLabel = if (!state.sidesSwapped) state.getTeam1ServerAwareDisplayName() else state.getTeam2ServerAwareDisplayName()
            val rightTeamLabel = if (!state.sidesSwapped) state.getTeam2ServerAwareDisplayName() else state.getTeam1ServerAwareDisplayName()

            serveBinding.textPlayerLeftName.text = leftTeamLabel
            serveBinding.textPlayerRightName.text = rightTeamLabel
            rallyBinding.textPlayerLeftName.text = leftTeamLabel
            rallyBinding.textPlayerRightName.text = rightTeamLabel
            basicScoringBinding.textPlayerLeftName.text = leftTeamLabel
            basicScoringBinding.textPlayerRightName.text = rightTeamLabel
        } else {
            // Singiel - normalna logika
            serverSelectionBinding.buttonPlayer1Serves.text = getString(R.string.player_serves, state.player1.getDisplayName())
            serverSelectionBinding.buttonPlayer2Serves.text = getString(R.string.player_serves, state.player2.getDisplayName())
            
            // Aktualizuj nazwiska w widoku serwisu (bez flag) - uwzględnij zamianę stron
            serveBinding.textPlayerLeftName.text = leftPlayer.getDisplayName()
            serveBinding.textPlayerRightName.text = rightPlayer.getDisplayName()
            
            // Aktualizuj nazwiska w widoku rally (bez flag) - uwzględnij zamianę stron
            rallyBinding.textPlayerLeftName.text = leftPlayer.getDisplayName()
            rallyBinding.textPlayerRightName.text = rightPlayer.getDisplayName()
            
            // Aktualizuj nazwiska w widoku basic scoring - uwzględnij zamianę stron
            basicScoringBinding.textPlayerLeftName.text = leftPlayer.getDisplayName()
            basicScoringBinding.textPlayerRightName.text = rightPlayer.getDisplayName()
        }
    }
    
    private fun updateBasicScoringView(state: MatchState) {
        // Określ, po której stronie ekranu jest serwujący
        val serverOnLeft = (state.isPlayer1Serving && !state.sidesSwapped) ||
                          (!state.isPlayer1Serving && state.sidesSwapped)
        
        if (serverOnLeft) {
            // Serwujący po lewej - pokaż WIN+FAULT po lewej, tylko WIN po prawej
            basicScoringBinding.layoutServerLeft.visibility = View.VISIBLE
            basicScoringBinding.layoutReceiverLeft.visibility = View.GONE
            basicScoringBinding.layoutServerRight.visibility = View.GONE
            basicScoringBinding.layoutReceiverRight.visibility = View.VISIBLE
        } else {
            // Serwujący po prawej - tylko WIN po lewej, WIN+FAULT po prawej
            basicScoringBinding.layoutServerLeft.visibility = View.GONE
            basicScoringBinding.layoutReceiverLeft.visibility = View.VISIBLE
            basicScoringBinding.layoutServerRight.visibility = View.VISIBLE
            basicScoringBinding.layoutReceiverRight.visibility = View.GONE
        }
        
        // Dynamiczny tekst i kolor przycisku w zależności od serwisu
        val isFirst = state.isFirstServe
        if (isFirst) {
            // 1. serwis — przycisk "2. SERWIS" (pomarańczowy)
            basicScoringBinding.buttonFaultServerLeft.text = getString(R.string.second_serve_button)
            basicScoringBinding.buttonFaultServerRight.text = getString(R.string.second_serve_button)
            basicScoringBinding.buttonFaultServerLeft.setBackgroundColor(0xFFFF9800.toInt())
            basicScoringBinding.buttonFaultServerRight.setBackgroundColor(0xFFFF9800.toInt())
        } else {
            // 2. serwis — przycisk "2x FAULT" (czerwony)
            basicScoringBinding.buttonFaultServerLeft.text = getString(R.string.double_fault_button)
            basicScoringBinding.buttonFaultServerRight.text = getString(R.string.double_fault_button)
            basicScoringBinding.buttonFaultServerLeft.setBackgroundColor(0xFFF44336.toInt())
            basicScoringBinding.buttonFaultServerRight.setBackgroundColor(0xFFF44336.toInt())
        }
        
        // Aktualizuj informację o serwisie z animacją przejścia
        val shouldAnimate = wasFirstServe && !isFirst
        wasFirstServe = isFirst
        
        if (isFirst) {
            basicScoringBinding.textServeInfo.text = getString(R.string.first_serve)
        } else {
            val fullText = getString(R.string.second_serve)
            val styledText = SpannableString(fullText)
            val firstServeEnd = fullText.indexOf(">")
            if (firstServeEnd > 0) {
                styledText.setSpan(
                    ForegroundColorSpan(Color.GRAY),
                    0, firstServeEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                // Pogrub "2nd serve" po strzałce
                styledText.setSpan(
                    StyleSpan(Typeface.BOLD),
                    firstServeEnd + 1, fullText.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                // Powiększ "2nd serve"
                styledText.setSpan(
                    RelativeSizeSpan(1.3f),
                    firstServeEnd + 1, fullText.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            basicScoringBinding.textServeInfo.text = styledText
        }
        
        // Animacja slide przejścia na 2. serwis
        if (shouldAnimate) {
            animateServeTransition()
        }
    }
    
    /**
     * Animacja przejścia na 2. serwis — slide down + scale pulse na pasku serwisu
     */
    private fun animateServeTransition() {
        val serveInfoView = basicScoringBinding.textServeInfo
        
        // Slide z góry + fade in
        serveInfoView.translationY = -80f
        serveInfoView.alpha = 0f
        serveInfoView.scaleX = 0.8f
        serveInfoView.scaleY = 0.8f
        
        val slideDown = ObjectAnimator.ofFloat(serveInfoView, "translationY", -80f, 0f)
        val fadeIn = ObjectAnimator.ofFloat(serveInfoView, "alpha", 0f, 1f)
        val scaleX = ObjectAnimator.ofFloat(serveInfoView, "scaleX", 0.8f, 1.15f, 1f)
        val scaleY = ObjectAnimator.ofFloat(serveInfoView, "scaleY", 0.8f, 1.15f, 1f)
        
        AnimatorSet().apply {
            playTogether(slideDown, fadeIn, scaleX, scaleY)
            duration = 450
            interpolator = android.view.animation.OvershootInterpolator(1.2f)
            start()
        }
        
        // Krótki flash tła na czerwono, potem powrót
        val originalColor = basicScoringBinding.textServeInfo.currentTextColor
        basicScoringBinding.textServeInfo.setTextColor(0xFFF44336.toInt())
        serveInfoView.postDelayed({
            basicScoringBinding.textServeInfo.setTextColor(originalColor)
        }, 600)
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
        
        // Pokaż przyciski po stronie serwującego (uwzględnij zamianę stron)
        val serverOnLeft = (state.isPlayer1Serving && !state.sidesSwapped) || 
                          (!state.isPlayer1Serving && state.sidesSwapped)
        
        if (serverOnLeft) {
            serveBinding.layoutServeLeft.visibility = View.VISIBLE
            serveBinding.layoutServeRight.visibility = View.GONE
        } else {
            serveBinding.layoutServeLeft.visibility = View.GONE
            serveBinding.layoutServeRight.visibility = View.VISIBLE
        }
        
        // Aktualizuj text w zależności od pierwszego/drugiego serwisu z animacją
        if (state.isFirstServe) {
            serveBinding.textServeInfo.text = getString(R.string.first_serve)
        } else {
            // Przy 2 serwisie pokaż: "1. serwis > 2. serwis" z szarym pierwszym
            val fullText = getString(R.string.second_serve)
            val styledText = SpannableString(fullText)
            // Znajdź pozycję pierwszego serwisu do pokolorowania na szaro
            val firstServeEnd = fullText.indexOf(">")
            if (firstServeEnd > 0) {
                styledText.setSpan(
                    ForegroundColorSpan(Color.GRAY),
                    0, firstServeEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            serveBinding.textServeInfo.text = styledText
            
            // Animacja zmiany
            serveBinding.textServeInfo.alpha = 0f
            serveBinding.textServeInfo.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
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
                    val (title, message, icon) = when (viewModel.pendingAnnouncementType) {
                        "side_change" -> Triple(
                            getString(R.string.announce_side_change),
                            getString(R.string.announce_side_change_msg),
                            "\uD83D\uDD04"  // 🔄
                        )
                        "tiebreak" -> Triple(
                            getString(R.string.announce_tiebreak),
                            getString(R.string.announce_tiebreak_msg, state.matchConfig.gamesPerSet, state.matchConfig.tiebreakPoints),
                            "\uD83C\uDFBE"  // 🎾
                        )
                        "super_tiebreak" -> Triple(
                            getString(R.string.announce_super_tiebreak),
                            getString(R.string.announce_super_tiebreak_msg, state.matchConfig.setsToWin - 1, state.matchConfig.superTiebreakPoints),
                            "\uD83C\uDFC6"  // 🏆
                        )
                        "deciding_point" -> Triple(
                            getString(R.string.deciding_point),
                            getString(R.string.deciding_point_msg),
                            "\u2757"  // ❗
                        )
                        else -> Triple("", "", "")
                    }
                    announcementBinding.textAnnouncementIcon.text = icon
                    announcementBinding.textAnnouncementTitle.text = title
                    announcementBinding.textAnnouncementMessage.text = message
                    announcementBinding.buttonAnnouncementSkipSideChange.visibility =
                        if (viewModel.pendingAnnouncementType == "side_change") View.VISIBLE else View.GONE
                    animateViewTransition(binding.layoutAnnouncement.root, View.VISIBLE)
                }
                
                MatchView.SERVE -> {
                    animateViewTransition(binding.layoutServe.root, View.VISIBLE)
                    
                    // Nazwiska są już ustawione w updatePlayerNames()
                    
                    // Pokaż przyciski po stronie serwującego (uwzględnij zamianę stron)
                    val serverOnLeft = (state.isPlayer1Serving && !state.sidesSwapped) || 
                                      (!state.isPlayer1Serving && state.sidesSwapped)
                    
                    if (serverOnLeft) {
                        serveBinding.layoutServeLeft.visibility = View.VISIBLE
                        serveBinding.layoutServeRight.visibility = View.GONE
                    } else {
                        serveBinding.layoutServeLeft.visibility = View.GONE
                        serveBinding.layoutServeRight.visibility = View.VISIBLE
                    }
                    
                    // Aktualizuj text w zależności od pierwszego/drugiego serwisu
                    if (state.isFirstServe) {
                        serveBinding.textServeInfo.text = getString(R.string.first_serve)
                    } else {
                        // Przy 2 serwisie pokaż: "1. serwis > 2. serwis" z szarym pierwszym
                        val fullText = getString(R.string.second_serve)
                        val styledText = SpannableString(fullText)
                        // Znajdź pozycję pierwszego serwisu do pokolorowania na szaro
                        val firstServeEnd = fullText.indexOf(">")
                        if (firstServeEnd > 0) {
                            styledText.setSpan(
                                ForegroundColorSpan(Color.GRAY),
                                0, firstServeEnd,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                        serveBinding.textServeInfo.text = styledText
                    }
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
                    
                    val winner = if (state.player1Sets > state.player2Sets) {
                        state.player1.getDisplayName()
                    } else {
                        state.player2.getDisplayName()
                    }
                    
                    matchFinishedBinding.textWinner.text = getString(R.string.winner_label, winner)
                    
                    // Wyświetl statystyki
                    updateMatchStatistics(state)
                }
            }
        }
    }
    
    private fun updateMatchStatistics(state: MatchState) {
        // Ustaw nazwy graczy w nagłówkach tabeli
        matchFinishedBinding.headerPlayer1Name.text = state.player1.getDisplayName()
        matchFinishedBinding.headerPlayer2Name.text = state.player2.getDisplayName()
        
        // Wypełnij statystyki
        matchFinishedBinding.textAcesPlayer1.text = state.player1Stats.aces.toString()
        matchFinishedBinding.textAcesPlayer2.text = state.player2Stats.aces.toString()
        
        matchFinishedBinding.textDoubleFaultsPlayer1.text = state.player1Stats.doubleFaults.toString()
        matchFinishedBinding.textDoubleFaultsPlayer2.text = state.player2Stats.doubleFaults.toString()
        
        matchFinishedBinding.textWinnersPlayer1.text = state.player1Stats.winners.toString()
        matchFinishedBinding.textWinnersPlayer2.text = state.player2Stats.winners.toString()
        
        matchFinishedBinding.textUnforcedErrorsPlayer1.text = state.player1Stats.unforcedErrors.toString()
        matchFinishedBinding.textUnforcedErrorsPlayer2.text = state.player2Stats.unforcedErrors.toString()
        
        matchFinishedBinding.textFirstServePctPlayer1.text = formatPercentage(state.player1Stats.getFirstServePercentage())
        matchFinishedBinding.textFirstServePctPlayer2.text = formatPercentage(state.player2Stats.getFirstServePercentage())
        
        // Przycisk: Następny mecz z tym samym setupem (domyślny)
        matchFinishedBinding.buttonNextMatchSameSetup.setOnClickListener {
            AppLogger.button("Match", "NextMatchSameSetup", "court=${state.courtId}")
            AppLogger.navigate("Match", "PlayerSelection", "sameSetup=true")
            val intent = Intent(this, PlayerSelectionActivity::class.java).apply {
                putExtra(PlayerSelectionActivity.EXTRA_COURT_ID, state.courtId)
                putExtra(PlayerSelectionActivity.EXTRA_COURT_NAME, state.courtName)
                putExtra(PlayerSelectionActivity.EXTRA_MATCH_CONFIG, state.matchConfig)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            finish()
        }
        
        // Przycisk: Nowy mecz z innymi ustawieniami
        matchFinishedBinding.buttonNextMatchNewSetup.setOnClickListener {
            AppLogger.button("Match", "NextMatchNewSetup", "court=${state.courtId}")
            AppLogger.navigate("Match", "PlayerSelection", "sameSetup=false")
            val intent = Intent(this, PlayerSelectionActivity::class.java).apply {
                putExtra(PlayerSelectionActivity.EXTRA_COURT_ID, state.courtId)
                putExtra(PlayerSelectionActivity.EXTRA_COURT_NAME, state.courtName)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            finish()
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

    private fun formatPercentage(value: Int): String {
        return getString(R.string.percentage_value, value)
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

