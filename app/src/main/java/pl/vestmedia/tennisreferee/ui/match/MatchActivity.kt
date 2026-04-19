package pl.vestmedia.tennisreferee.ui.match

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
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
import pl.vestmedia.tennisreferee.data.model.MatchConfig
import pl.vestmedia.tennisreferee.data.model.StatsMode
import pl.vestmedia.tennisreferee.data.model.Player
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.utils.AppLogger
import pl.vestmedia.tennisreferee.ui.playerselection.PlayerSelectionActivity

/**
 * Activity zarządzające przebiegiem meczu
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
        // Wybór serwującego
        serverSelectionBinding.buttonPlayer1Serves.setOnClickListener {
            AppLogger.button("Match", "Player1Serves")
            viewModel.setFirstServer(true)
        }
        
        serverSelectionBinding.buttonPlayer2Serves.setOnClickListener {
            AppLogger.button("Match", "Player2Serves")
            viewModel.setFirstServer(false)
        }
        
        // Zamiana stron z animacją
        serverSelectionBinding.buttonSwapSides.setOnClickListener {
            AppLogger.button("Match", "SwapSides")
            animateSwapSides()
            viewModel.swapSides()
        }
        
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
     * Animuje zmianę wyniku z delikatnym efektem scale
     */
    private fun animateScoreChange(view: View, newText: String) {
        if (view is android.widget.TextView) {
            view.text = newText
        }
        
        view.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(150)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(150)
                    .start()
            }
            .start()
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
                if (view.visibility == View.VISIBLE) {
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
    
    /**
     * Konwertuje kod kraju (ISO) na emoji flagi
     */
    private fun getCountryFlag(countryCode: String?): String {
        if (countryCode.isNullOrEmpty() || countryCode.length != 2) return ""
        
        // Konwertuj na wielkie litery
        val upperCode = countryCode.uppercase()
        
        val firstChar = Character.codePointAt(upperCode, 0) - 0x41 + 0x1F1E6
        val secondChar = Character.codePointAt(upperCode, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    }
    
    private fun updatePlayerNames(state: MatchState) {
        // Aktualizuj nazwiska na przyciskach wyboru serwującego
        val leftPlayer = if (state.sidesSwapped) state.player2 else state.player1
        val rightPlayer = if (state.sidesSwapped) state.player1 else state.player2
        
        // Dla debla użyj nazw zespołów
        if (state.isDoubles) {
            val team1Name = state.getTeam1DisplayName()
            val team2Name = state.getTeam2DisplayName()
            
            serverSelectionBinding.buttonPlayer1Serves.text = getString(R.string.team_serves, team1Name)
            serverSelectionBinding.buttonPlayer2Serves.text = getString(R.string.team_serves, team2Name)
            
            // W widoku serwisu pokaż aktualnego serwującego
            serveBinding.textPlayerLeftName.text = if (!state.sidesSwapped) {
                state.getCurrentServerName()
            } else {
                if (state.currentServer == 2 || state.currentServer == 4) state.getCurrentServerName() else ""
            }
            serveBinding.textPlayerRightName.text = if (state.sidesSwapped) {
                state.getCurrentServerName()
            } else {
                if (state.currentServer == 2 || state.currentServer == 4) state.getCurrentServerName() else ""
            }
            
            // Rally używa nazw zespołów
            rallyBinding.textPlayerLeftName.text = if (!state.sidesSwapped) team1Name else team2Name
            rallyBinding.textPlayerRightName.text = if (!state.sidesSwapped) team2Name else team1Name
            
            // Basic scoring - nazwy zespołów
            basicScoringBinding.textPlayerLeftName.text = if (!state.sidesSwapped) team1Name else team2Name
            basicScoringBinding.textPlayerRightName.text = if (!state.sidesSwapped) team2Name else team1Name
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
            val styledText = android.text.SpannableString(fullText)
            val firstServeEnd = fullText.indexOf(">")
            if (firstServeEnd > 0) {
                styledText.setSpan(
                    android.text.style.ForegroundColorSpan(android.graphics.Color.GRAY),
                    0, firstServeEnd,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                // Pogrub "2nd serve" po strzałce
                styledText.setSpan(
                    android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                    firstServeEnd + 1, fullText.length,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                // Powiększ "2nd serve"
                styledText.setSpan(
                    android.text.style.RelativeSizeSpan(1.3f),
                    firstServeEnd + 1, fullText.length,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
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
        // Flagi i nazwy graczy/zespołów
        if (state.isDoubles) {
            // Dla debla pokaż nazwy zespołów zamiast pojedynczych graczy
            scoreboardBinding.textPlayer1Flag.text = "👥"  // Ikona zespołu
            scoreboardBinding.textPlayer2Flag.text = "👥"
            
            scoreboardBinding.textPlayer1Name.text = state.getTeam1DisplayName()
            scoreboardBinding.textPlayer2Name.text = state.getTeam2DisplayName()
        } else {
            // Singiel - flagi i nazwiska
            scoreboardBinding.textPlayer1Flag.text = getCountryFlag(state.player1.flag)
            scoreboardBinding.textPlayer2Flag.text = getCountryFlag(state.player2.flag)
            
            scoreboardBinding.textPlayer1Name.text = state.player1.getDisplayName()
            scoreboardBinding.textPlayer2Name.text = state.player2.getDisplayName()
        }
        
        // Ikona serwisu (piłka tenisowa) po lewej od punktów
        scoreboardBinding.imagePlayer1ServerIcon.visibility = if (state.isPlayer1Serving) View.VISIBLE else View.INVISIBLE
        scoreboardBinding.imagePlayer2ServerIcon.visibility = if (!state.isPlayer1Serving) View.VISIBLE else View.INVISIBLE
        
        // Punkty z animacją
        animateScoreChange(scoreboardBinding.textPlayer1Points, state.getPlayer1PointsDisplay())
        animateScoreChange(scoreboardBinding.textPlayer2Points, state.getPlayer2PointsDisplay())
        
        // Określ aktywny set (0 = Set 1, 1 = Set 2)
        val currentSetIndex = state.setsHistory.size
        
        // Set 1 - zawsze widoczny
        if (state.setsHistory.isEmpty()) {
            // Trwa pierwszy set
            scoreboardBinding.textPlayer1Set1.text = state.player1Games.toString()
            scoreboardBinding.textPlayer2Set1.text = state.player2Games.toString()
        } else {
            // Pierwszy set zakończony — pokaż z wynikiem tiebreak jeśli był
            val set1 = state.setsHistory[0]
            val tbSuffix1 = if (set1.tiebreakLoserPoints != null) "(${set1.tiebreakLoserPoints})" else ""
            scoreboardBinding.textPlayer1Set1.text = "${set1.player1Games}${ if (set1.player1Games < set1.player2Games) tbSuffix1 else "" }"
            scoreboardBinding.textPlayer2Set1.text = "${set1.player2Games}${ if (set1.player2Games < set1.player1Games) tbSuffix1 else "" }"
        }
        
        // Set 2 - zawsze widoczny
        if (state.setsHistory.size == 1) {
            // Trwa drugi set
            scoreboardBinding.textPlayer1Set2.text = state.player1Games.toString()
            scoreboardBinding.textPlayer2Set2.text = state.player2Games.toString()
        } else if (state.setsHistory.size > 1) {
            // Drugi set zakończony — pokaż z wynikiem tiebreak jeśli był
            val set2 = state.setsHistory[1]
            val tbSuffix2 = if (set2.tiebreakLoserPoints != null) "(${set2.tiebreakLoserPoints})" else ""
            scoreboardBinding.textPlayer1Set2.text = "${set2.player1Games}${ if (set2.player1Games < set2.player2Games) tbSuffix2 else "" }"
            scoreboardBinding.textPlayer2Set2.text = "${set2.player2Games}${ if (set2.player2Games < set2.player1Games) tbSuffix2 else "" }"
        } else {
            // Przed drugim setem - pokaż 0
            scoreboardBinding.textPlayer1Set2.text = getString(R.string.zero_score)
            scoreboardBinding.textPlayer2Set2.text = getString(R.string.zero_score)
        }
        
        // Zaznacz aktywny set pomarańczowym tłem
        highlightActiveSet(currentSetIndex)
        
        // Tryb gry
        when {
            state.isSuperTiebreak -> {
                scoreboardBinding.textGameMode.text = getString(R.string.super_tiebreak_mode, state.matchConfig.superTiebreakPoints)
                scoreboardBinding.textGameMode.visibility = View.VISIBLE
            }
            state.isTiebreak -> {
                scoreboardBinding.textGameMode.text = getString(R.string.tiebreak_mode, state.matchConfig.tiebreakPoints)
                scoreboardBinding.textGameMode.visibility = View.VISIBLE
            }
            else -> {
                scoreboardBinding.textGameMode.text = ""
                scoreboardBinding.textGameMode.visibility = View.GONE
            }
        }
    }
    
    private var lastActiveSet = 0
    
    private fun highlightActiveSet(setIndex: Int) {
        val accentColor = resources.getColor(R.color.accent, theme)
        val transparentColor = android.graphics.Color.TRANSPARENT
        
        // Jeśli set się zmienił, animuj przejście
        if (setIndex != lastActiveSet && lastActiveSet < 2) {
            // Animuj wygaszenie starego seta
            when (lastActiveSet) {
                0 -> {
                    scoreboardBinding.backgroundPlayer1Set1.animate().alpha(0f).setDuration(200).start()
                    scoreboardBinding.backgroundPlayer2Set1.animate().alpha(0f).setDuration(200).start()
                }
                1 -> {
                    scoreboardBinding.backgroundPlayer1Set2.animate().alpha(0f).setDuration(200).start()
                    scoreboardBinding.backgroundPlayer2Set2.animate().alpha(0f).setDuration(200).start()
                }
            }
        }
        
        // Ustaw tła
        when (setIndex) {
            0 -> {
                // Aktywny Set 1
                scoreboardBinding.backgroundPlayer1Set1.setBackgroundColor(accentColor)
                scoreboardBinding.backgroundPlayer2Set1.setBackgroundColor(accentColor)
                scoreboardBinding.backgroundPlayer1Set1.alpha = 0.3f
                scoreboardBinding.backgroundPlayer2Set1.alpha = 0.3f
                
                scoreboardBinding.backgroundPlayer1Set2.setBackgroundColor(transparentColor)
                scoreboardBinding.backgroundPlayer2Set2.setBackgroundColor(transparentColor)
                
                // Animuj pojawienie się jeśli to zmiana
                if (setIndex != lastActiveSet) {
                    scoreboardBinding.backgroundPlayer1Set1.alpha = 0f
                    scoreboardBinding.backgroundPlayer2Set1.alpha = 0f
                    scoreboardBinding.backgroundPlayer1Set1.animate().alpha(0.3f).setDuration(300).start()
                    scoreboardBinding.backgroundPlayer2Set1.animate().alpha(0.3f).setDuration(300).start()
                }
            }
            1 -> {
                // Aktywny Set 2
                scoreboardBinding.backgroundPlayer1Set1.setBackgroundColor(transparentColor)
                scoreboardBinding.backgroundPlayer2Set1.setBackgroundColor(transparentColor)
                
                scoreboardBinding.backgroundPlayer1Set2.setBackgroundColor(accentColor)
                scoreboardBinding.backgroundPlayer2Set2.setBackgroundColor(accentColor)
                scoreboardBinding.backgroundPlayer1Set2.alpha = 0.3f
                scoreboardBinding.backgroundPlayer2Set2.alpha = 0.3f
                
                // Animuj pojawienie się jeśli to zmiana
                if (setIndex != lastActiveSet) {
                    scoreboardBinding.backgroundPlayer1Set2.alpha = 0f
                    scoreboardBinding.backgroundPlayer2Set2.alpha = 0f
                    scoreboardBinding.backgroundPlayer1Set2.animate().alpha(0.3f).setDuration(300).start()
                    scoreboardBinding.backgroundPlayer2Set2.animate().alpha(0.3f).setDuration(300).start()
                }
            }
            else -> {
                // Mecz zakończony - bez zaznaczenia
                scoreboardBinding.backgroundPlayer1Set1.setBackgroundColor(transparentColor)
                scoreboardBinding.backgroundPlayer2Set1.setBackgroundColor(transparentColor)
                scoreboardBinding.backgroundPlayer1Set2.setBackgroundColor(transparentColor)
                scoreboardBinding.backgroundPlayer2Set2.setBackgroundColor(transparentColor)
            }
        }
        
        lastActiveSet = setIndex
    }
    
    private fun updateServerSelectionButtons(state: MatchState) {
        // Zaktualizuj teksty przycisków wyboru serwującego w zależności od sidesSwapped
        val leftPlayerName = if (!state.sidesSwapped) {
            state.player1.getDisplayName()
        } else {
            state.player2.getDisplayName()
        }
        
        val rightPlayerName = if (!state.sidesSwapped) {
            state.player2.getDisplayName()
        } else {
            state.player1.getDisplayName()
        }
        
        serverSelectionBinding.buttonPlayer1Serves.text = getString(R.string.player_serves, leftPlayerName)
        serverSelectionBinding.buttonPlayer2Serves.text = getString(R.string.player_serves, rightPlayerName)
        
        // Zastosuj kolory drużyn dla debla
        val isDoublesMatch = intent.getBooleanExtra(EXTRA_IS_DOUBLES, false)
        if (isDoublesMatch) {
            val team1Color = intent.getIntExtra(EXTRA_TEAM1_COLOR, 0)
            val team2Color = intent.getIntExtra(EXTRA_TEAM2_COLOR, 0)
            
            if (team1Color != 0 && team2Color != 0) {
                serverSelectionBinding.buttonPlayer1Serves.setBackgroundColor(
                    androidx.core.content.ContextCompat.getColor(this, team1Color)
                )
                serverSelectionBinding.buttonPlayer2Serves.setBackgroundColor(
                    androidx.core.content.ContextCompat.getColor(this, team2Color)
                )
            }
        }
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
            val styledText = android.text.SpannableString(fullText)
            // Znajdź pozycję pierwszego serwisu do pokolorowania na szaro
            val firstServeEnd = fullText.indexOf(">")
            if (firstServeEnd > 0) {
                styledText.setSpan(
                    android.text.style.ForegroundColorSpan(android.graphics.Color.GRAY),
                    0, firstServeEnd,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
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
                        val styledText = android.text.SpannableString(fullText)
                        // Znajdź pozycję pierwszego serwisu do pokolorowania na szaro
                        val firstServeEnd = fullText.indexOf(">")
                        if (firstServeEnd > 0) {
                            styledText.setSpan(
                                android.text.style.ForegroundColorSpan(android.graphics.Color.GRAY),
                                0, firstServeEnd,
                                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
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
                    matchFinishedBinding.textWinner.setTextColor(android.graphics.Color.parseColor("#FFD700")) // Gold highlight
                    
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
        
        matchFinishedBinding.textFirstServePctPlayer1.text = "${state.player1Stats.getFirstServePercentage()}%"
        matchFinishedBinding.textFirstServePctPlayer2.text = "${state.player2Stats.getFirstServePercentage()}%"
        
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
    
    /**
     * Formatuje czas trwania na format HH:MM:SS lub MM:SS
     */
    private fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = (durationMs / (1000 * 60 * 60))
        
        return if (hours > 0) {
            String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
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
        val button1 = serverSelectionBinding.buttonPlayer1Serves
        val button2 = serverSelectionBinding.buttonPlayer2Serves
        
        // Fade out oba przyciski
        val fadeOut1 = ObjectAnimator.ofFloat(button1, "alpha", 1f, 0f)
        val fadeOut2 = ObjectAnimator.ofFloat(button2, "alpha", 1f, 0f)
        
        fadeOut1.duration = 150
        fadeOut2.duration = 150
        
        // Po fade out - zaktualizuj i fade in
        val fadeIn1 = ObjectAnimator.ofFloat(button1, "alpha", 0f, 1f)
        val fadeIn2 = ObjectAnimator.ofFloat(button2, "alpha", 0f, 1f)
        
        fadeIn1.duration = 150
        fadeIn2.duration = 150
        
        // Sekwencja: fade out -> fade in
        val animatorSet = AnimatorSet()
        animatorSet.play(fadeOut1).with(fadeOut2)
        animatorSet.play(fadeIn1).after(fadeOut1)
        animatorSet.play(fadeIn2).after(fadeOut2)
        
        animatorSet.start()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopTimerUpdates()
    }
    

}

