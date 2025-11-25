package pl.vestmedia.tennisreferee.ui.match

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.databinding.ActivityMatchBinding
import pl.vestmedia.tennisreferee.databinding.LayoutScoreboardBinding
import pl.vestmedia.tennisreferee.databinding.LayoutServerSelectionBinding
import pl.vestmedia.tennisreferee.databinding.LayoutServeBinding
import pl.vestmedia.tennisreferee.databinding.LayoutRallyBinding
import pl.vestmedia.tennisreferee.databinding.LayoutMatchFinishedBinding
import pl.vestmedia.tennisreferee.data.model.MatchState
import pl.vestmedia.tennisreferee.data.model.Player

/**
 * Activity zarządzające przebiegiem meczu
 */
class MatchActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMatchBinding
    private lateinit var scoreboardBinding: LayoutScoreboardBinding
    private lateinit var serverSelectionBinding: LayoutServerSelectionBinding
    private lateinit var serveBinding: LayoutServeBinding
    private lateinit var rallyBinding: LayoutRallyBinding
    private lateinit var matchFinishedBinding: LayoutMatchFinishedBinding
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
        matchFinishedBinding = LayoutMatchFinishedBinding.bind(binding.layoutMatchFinished.root)
        
        // Pobierz stan meczu z Intent
        val matchState = intent.getParcelableExtra<MatchState>(EXTRA_MATCH_STATE)
        
        if (matchState == null) {
            finish()
            return
        }
        
        viewModel.initializeMatch(matchState)
        
        setupObservers()
        setupListeners()
    }
    
    private fun setupObservers() {
        viewModel.matchState.observe(this) { state ->
            updateScoreboard(state)
            updatePlayerNames(state)
            updateServerSelectionButtons(state)
            updateServeView(state)
            updateTimer(state)
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
    }
    
    private fun setupListeners() {
        // Wybór serwującego
        serverSelectionBinding.buttonPlayer1Serves.setOnClickListener {
            viewModel.setFirstServer(true)
        }
        
        serverSelectionBinding.buttonPlayer2Serves.setOnClickListener {
            viewModel.setFirstServer(false)
        }
        
        // Zamiana stron
        serverSelectionBinding.buttonSwapSides.setOnClickListener {
            viewModel.swapSides()
        }
        
        // Serwis - lewa strona (Player 1)
        serveBinding.buttonAceLeft.setOnClickListener {
            viewModel.handleAce()
        }
        
        serveBinding.buttonFaultLeft.setOnClickListener {
            viewModel.handleFault()
        }
        
        // Serwis - prawa strona (Player 2)
        serveBinding.buttonAceRight.setOnClickListener {
            viewModel.handleAce()
        }
        
        serveBinding.buttonFaultRight.setOnClickListener {
            viewModel.handleFault()
        }
        
        // Ball in Play - wspólny przycisk dla obu graczy
        serveBinding.buttonBallInPlay.setOnClickListener {
            viewModel.handleBallInPlay()
        }
        
        // Rally - lewa strona (Player 1 lub Player 2 w zależności od sidesSwapped)
        rallyBinding.buttonWinnerLeft.setOnClickListener {
            val isPlayer1 = !(viewModel.matchState.value?.sidesSwapped ?: false)
            viewModel.handleWinner(isPlayer1)
        }
        
        rallyBinding.buttonForcedErrorLeft.setOnClickListener {
            val isPlayer1 = !(viewModel.matchState.value?.sidesSwapped ?: false)
            viewModel.handleForcedError(isPlayer1)
        }
        
        rallyBinding.buttonUnforcedErrorLeft.setOnClickListener {
            val isPlayer1 = !(viewModel.matchState.value?.sidesSwapped ?: false)
            viewModel.handleUnforcedError(isPlayer1)
        }
        
        // Rally - prawa strona (Player 2 lub Player 1 w zależności od sidesSwapped)
        rallyBinding.buttonWinnerRight.setOnClickListener {
            val isPlayer1 = viewModel.matchState.value?.sidesSwapped ?: false
            viewModel.handleWinner(isPlayer1)
        }
        
        rallyBinding.buttonForcedErrorRight.setOnClickListener {
            val isPlayer1 = viewModel.matchState.value?.sidesSwapped ?: false
            viewModel.handleForcedError(isPlayer1)
        }
        
        rallyBinding.buttonUnforcedErrorRight.setOnClickListener {
            val isPlayer1 = viewModel.matchState.value?.sidesSwapped ?: false
            viewModel.handleUnforcedError(isPlayer1)
        }
        
        // Przycisk Cofnij
        binding.buttonUndo.setOnClickListener {
            viewModel.undoLastAction()
        }
        
        // Przycisk powrotu
        binding.buttonBack.setOnClickListener {
            finish()
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
        
        serverSelectionBinding.buttonPlayer1Serves.text = getString(R.string.player_serves, state.player1.getDisplayName())
        serverSelectionBinding.buttonPlayer2Serves.text = getString(R.string.player_serves, state.player2.getDisplayName())
        
        // Aktualizuj nazwiska w widoku serwisu (bez flag) - uwzględnij zamianę stron
        serveBinding.textPlayerLeftName.text = leftPlayer.getDisplayName()
        serveBinding.textPlayerRightName.text = rightPlayer.getDisplayName()
        
        // Aktualizuj nazwiska w widoku rally (bez flag) - uwzględnij zamianę stron
        rallyBinding.textPlayerLeftName.text = leftPlayer.getDisplayName()
        rallyBinding.textPlayerRightName.text = rightPlayer.getDisplayName()
    }
    
    private fun updateScoreboard(state: MatchState) {
        // Flagi i nazwy graczy
        scoreboardBinding.textPlayer1Flag.text = getCountryFlag(state.player1.flag)
        scoreboardBinding.textPlayer2Flag.text = getCountryFlag(state.player2.flag)
        
        // Nazwiska bez ikony serwisu
        scoreboardBinding.textPlayer1Name.text = state.player1.getDisplayName()
        scoreboardBinding.textPlayer2Name.text = state.player2.getDisplayName()
        
        // Ikona serwisu przy punktach
        scoreboardBinding.textPlayer1ServerIcon.visibility = if (state.isPlayer1Serving) View.VISIBLE else View.GONE
        scoreboardBinding.textPlayer2ServerIcon.visibility = if (!state.isPlayer1Serving) View.VISIBLE else View.GONE
        
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
            // Pierwszy set zakończony
            val set1 = state.setsHistory[0]
            scoreboardBinding.textPlayer1Set1.text = set1.player1Games.toString()
            scoreboardBinding.textPlayer2Set1.text = set1.player2Games.toString()
        }
        
        // Set 2 - zawsze widoczny
        if (state.setsHistory.size == 1) {
            // Trwa drugi set
            scoreboardBinding.textPlayer1Set2.text = state.player1Games.toString()
            scoreboardBinding.textPlayer2Set2.text = state.player2Games.toString()
        } else if (state.setsHistory.size > 1) {
            // Drugi set zakończony
            val set2 = state.setsHistory[1]
            scoreboardBinding.textPlayer1Set2.text = set2.player1Games.toString()
            scoreboardBinding.textPlayer2Set2.text = set2.player2Games.toString()
        } else {
            // Przed drugim setem - pokaż 0
            scoreboardBinding.textPlayer1Set2.text = "0"
            scoreboardBinding.textPlayer2Set2.text = "0"
        }
        
        // Zaznacz aktywny set pomarańczowym tłem
        highlightActiveSet(currentSetIndex)
        
        // Tryb gry
        when {
            state.isSuperTiebreak -> {
                scoreboardBinding.textGameMode.text = "Super Tie-break (do 10, 2 przewagi)"
                scoreboardBinding.textGameMode.visibility = View.VISIBLE
            }
            state.isTiebreak -> {
                scoreboardBinding.textGameMode.text = "Tie-break (do 7, 2 przewagi)"
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
        
        serverSelectionBinding.buttonPlayer1Serves.text = "$leftPlayerName\n${getString(R.string.player_serves_short)}"
        serverSelectionBinding.buttonPlayer2Serves.text = "$rightPlayerName\n${getString(R.string.player_serves_short)}"
        
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
        // Ukryj wszystkie widoki z animacją slide out
        animateViewTransition(binding.layoutServerSelection.root, View.GONE)
        animateViewTransition(binding.layoutServe.root, View.GONE)
        animateViewTransition(binding.layoutRally.root, View.GONE)
        animateViewTransition(binding.layoutMatchFinished.root, View.GONE)
        
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
                
                MatchView.MATCH_FINISHED -> {
                    animateViewTransition(binding.layoutMatchFinished.root, View.VISIBLE)
                    
                    val winner = if (state.player1Sets > state.player2Sets) {
                        state.player1.getDisplayName()
                    } else {
                        state.player2.getDisplayName()
                    }
                    
                    matchFinishedBinding.textWinner.text = "Zwycięzca: $winner"
                    
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
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopTimerUpdates()
    }
}

