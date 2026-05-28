package pl.vestmedia.tennisreferee.ui.match

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.View
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.data.model.MatchState
import pl.vestmedia.tennisreferee.databinding.LayoutBasicScoringBinding
import pl.vestmedia.tennisreferee.databinding.LayoutRallyBinding
import pl.vestmedia.tennisreferee.databinding.LayoutServeBinding

class ScoringButtonsController(
    private val context: Context,
    private val serveBinding: LayoutServeBinding,
    private val rallyBinding: LayoutRallyBinding,
    private val basicScoringBinding: LayoutBasicScoringBinding,
    private val getState: () -> MatchState?,
    private val onAce: () -> Unit,
    private val onFault: () -> Unit,
    private val onFootFault: () -> Unit,
    private val onBallInPlay: () -> Unit,
    private val onWinner: (Boolean) -> Unit,
    private val onForcedError: (Boolean) -> Unit,
    private val onUnforcedError: (Boolean) -> Unit,
    private val onBasicWin: (Boolean) -> Unit,
    private val onBasicFault: () -> Unit,
    private val onButtonLogged: (String, String) -> Unit
) {
    private var wasFirstServe: Boolean = true

    fun bind() {
        serveBinding.buttonAceLeft.setOnClickListener {
            onButtonLogged("Ace", "side=left")
            onAce()
        }
        serveBinding.buttonFaultLeft.setOnClickListener {
            onButtonLogged("Fault", "side=left")
            onFault()
        }
        serveBinding.buttonFootFaultLeft.setOnClickListener {
            onButtonLogged("FootFault", "side=left")
            onFootFault()
        }
        serveBinding.buttonAceRight.setOnClickListener {
            onButtonLogged("Ace", "side=right")
            onAce()
        }
        serveBinding.buttonFaultRight.setOnClickListener {
            onButtonLogged("Fault", "side=right")
            onFault()
        }
        serveBinding.buttonFootFaultRight.setOnClickListener {
            onButtonLogged("FootFault", "side=right")
            onFootFault()
        }
        serveBinding.buttonBallInPlay.setOnClickListener {
            onButtonLogged("BallInPlay", "")
            onBallInPlay()
        }

        rallyBinding.buttonWinnerLeft.setOnClickListener {
            val isPlayer1 = isLeftPlayer1()
            onButtonLogged("Winner", "side=left isP1=$isPlayer1")
            onWinner(isPlayer1)
        }
        rallyBinding.buttonForcedErrorLeft.setOnClickListener {
            val isPlayer1 = isLeftPlayer1()
            onButtonLogged("ForcedError", "side=left isP1=$isPlayer1")
            onForcedError(isPlayer1)
        }
        rallyBinding.buttonUnforcedErrorLeft.setOnClickListener {
            val isPlayer1 = isLeftPlayer1()
            onButtonLogged("UnforcedError", "side=left isP1=$isPlayer1")
            onUnforcedError(isPlayer1)
        }
        rallyBinding.buttonWinnerRight.setOnClickListener {
            val isPlayer1 = isRightPlayer1()
            onButtonLogged("Winner", "side=right isP1=$isPlayer1")
            onWinner(isPlayer1)
        }
        rallyBinding.buttonForcedErrorRight.setOnClickListener {
            val isPlayer1 = isRightPlayer1()
            onButtonLogged("ForcedError", "side=right isP1=$isPlayer1")
            onForcedError(isPlayer1)
        }
        rallyBinding.buttonUnforcedErrorRight.setOnClickListener {
            val isPlayer1 = isRightPlayer1()
            onButtonLogged("UnforcedError", "side=right isP1=$isPlayer1")
            onUnforcedError(isPlayer1)
        }

        basicScoringBinding.buttonWinServerLeft.setOnClickListener {
            val isPlayer1 = isLeftPlayer1()
            onButtonLogged("BasicWin", "side=serverLeft isP1=$isPlayer1")
            onBasicWin(isPlayer1)
        }
        basicScoringBinding.buttonFaultServerLeft.setOnClickListener {
            onButtonLogged("BasicFault", "side=left")
            onBasicFault()
        }
        basicScoringBinding.buttonWinReceiverLeft.setOnClickListener {
            val isPlayer1 = isLeftPlayer1()
            onButtonLogged("BasicWin", "side=receiverLeft isP1=$isPlayer1")
            onBasicWin(isPlayer1)
        }
        basicScoringBinding.buttonWinServerRight.setOnClickListener {
            val isPlayer1 = isRightPlayer1()
            onButtonLogged("BasicWin", "side=serverRight isP1=$isPlayer1")
            onBasicWin(isPlayer1)
        }
        basicScoringBinding.buttonFaultServerRight.setOnClickListener {
            onButtonLogged("BasicFault", "side=right")
            onBasicFault()
        }
        basicScoringBinding.buttonWinReceiverRight.setOnClickListener {
            val isPlayer1 = isRightPlayer1()
            onButtonLogged("BasicWin", "side=receiverRight isP1=$isPlayer1")
            onBasicWin(isPlayer1)
        }
    }

    fun renderServeView(state: MatchState, animateSecondServeText: Boolean) {
        val serverOnLeft = isServerOnLeft(state)
        serveBinding.layoutServeLeft.visibility = if (serverOnLeft) View.VISIBLE else View.GONE
        serveBinding.layoutServeRight.visibility = if (serverOnLeft) View.GONE else View.VISIBLE

        if (state.isFirstServe) {
            serveBinding.textServeInfo.text = context.getString(R.string.first_serve)
            return
        }

        serveBinding.textServeInfo.text = styledSecondServeText(includeStrongSecondServe = false)
        if (animateSecondServeText) {
            serveBinding.textServeInfo.alpha = 0f
            serveBinding.textServeInfo.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }

    fun renderBasicScoringView(state: MatchState) {
        val serverOnLeft = isServerOnLeft(state)
        basicScoringBinding.layoutServerLeft.visibility = if (serverOnLeft) View.VISIBLE else View.GONE
        basicScoringBinding.layoutReceiverLeft.visibility = if (serverOnLeft) View.GONE else View.VISIBLE
        basicScoringBinding.layoutServerRight.visibility = if (serverOnLeft) View.GONE else View.VISIBLE
        basicScoringBinding.layoutReceiverRight.visibility = if (serverOnLeft) View.VISIBLE else View.GONE

        if (state.isFirstServe) {
            basicScoringBinding.buttonFaultServerLeft.text = context.getString(R.string.second_serve_button)
            basicScoringBinding.buttonFaultServerRight.text = context.getString(R.string.second_serve_button)
            basicScoringBinding.buttonFaultServerLeft.setBackgroundColor(0xFFFF9800.toInt())
            basicScoringBinding.buttonFaultServerRight.setBackgroundColor(0xFFFF9800.toInt())
        } else {
            basicScoringBinding.buttonFaultServerLeft.text = context.getString(R.string.double_fault_button)
            basicScoringBinding.buttonFaultServerRight.text = context.getString(R.string.double_fault_button)
            basicScoringBinding.buttonFaultServerLeft.setBackgroundColor(0xFFF44336.toInt())
            basicScoringBinding.buttonFaultServerRight.setBackgroundColor(0xFFF44336.toInt())
        }

        val shouldAnimate = wasFirstServe && !state.isFirstServe
        wasFirstServe = state.isFirstServe

        basicScoringBinding.textServeInfo.text = if (state.isFirstServe) {
            context.getString(R.string.first_serve)
        } else {
            styledSecondServeText(includeStrongSecondServe = true)
        }

        if (shouldAnimate) {
            animateServeTransition()
        }
    }

    private fun styledSecondServeText(includeStrongSecondServe: Boolean): SpannableString {
        val fullText = context.getString(R.string.second_serve)
        val styledText = SpannableString(fullText)
        val firstServeEnd = fullText.indexOf(">")
        if (firstServeEnd > 0) {
            styledText.setSpan(
                ForegroundColorSpan(Color.GRAY),
                0,
                firstServeEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            if (includeStrongSecondServe) {
                styledText.setSpan(
                    StyleSpan(Typeface.BOLD),
                    firstServeEnd + 1,
                    fullText.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                styledText.setSpan(
                    RelativeSizeSpan(1.3f),
                    firstServeEnd + 1,
                    fullText.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        return styledText
    }

    private fun animateServeTransition() {
        val serveInfoView = basicScoringBinding.textServeInfo

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

        val originalColor = basicScoringBinding.textServeInfo.currentTextColor
        basicScoringBinding.textServeInfo.setTextColor(0xFFF44336.toInt())
        serveInfoView.postDelayed({
            basicScoringBinding.textServeInfo.setTextColor(originalColor)
        }, 600)
    }

    private fun isLeftPlayer1(): Boolean {
        return !(getState()?.sidesSwapped ?: false)
    }

    private fun isRightPlayer1(): Boolean {
        return getState()?.sidesSwapped ?: false
    }

    private fun isServerOnLeft(state: MatchState): Boolean {
        return (state.isPlayer1Serving && !state.sidesSwapped) ||
            (!state.isPlayer1Serving && state.sidesSwapped)
    }
}