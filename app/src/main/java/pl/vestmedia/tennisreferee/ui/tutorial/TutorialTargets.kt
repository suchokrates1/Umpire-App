package pl.vestmedia.tennisreferee.ui.tutorial

import pl.vestmedia.tennisreferee.R

object TutorialTargets {
    fun viewId(target: String): Int? = when (target) {
        "swapSides" -> R.id.buttonSwapSides
        "chooseServer" -> R.id.buttonPlayer1Serves
        "winButton" -> R.id.buttonWinServerLeft
        "doubleFault" -> R.id.buttonFaultServerLeft
        "announcementContinue" -> R.id.buttonAnnouncementContinue
        "undo" -> R.id.buttonUndo
        "finish" -> R.id.buttonBack
        "done" -> R.id.buttonNextMatchSameSetup
        "configNext" -> R.id.buttonNext
        else -> null
    }
}
