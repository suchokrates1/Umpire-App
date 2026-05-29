package pl.vestmedia.tennisreferee.ui.match

import android.content.Context
import android.view.View
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.domain.match.model.MatchState
import pl.vestmedia.tennisreferee.databinding.LayoutAnnouncementBinding

class AnnouncementController(
    private val context: Context,
    private val binding: LayoutAnnouncementBinding,
    private val getAnnouncementType: () -> String?,
    private val onContinue: () -> Unit,
    private val onSkipSideChange: () -> Unit,
    private val onButtonLogged: (String, String) -> Unit
) {
    fun bind() {
        binding.buttonAnnouncementContinue.setOnClickListener {
            onButtonLogged("AnnouncementContinue", getAnnouncementType().orEmpty())
            onContinue()
        }

        binding.buttonAnnouncementSkipSideChange.setOnClickListener {
            onButtonLogged("SkipSideChange", "")
            onSkipSideChange()
        }
    }

    fun render(state: MatchState) {
        val announcementType = getAnnouncementType()
        val (title, message, icon) = when (announcementType) {
            "side_change" -> Triple(
                context.getString(R.string.announce_side_change),
                context.getString(R.string.announce_side_change_msg),
                "\uD83D\uDD04"
            )
            "tiebreak" -> Triple(
                context.getString(R.string.announce_tiebreak),
                context.getString(
                    R.string.announce_tiebreak_msg,
                    state.matchConfig.gamesPerSet,
                    state.matchConfig.tiebreakPoints
                ),
                "\uD83C\uDFBE"
            )
            "super_tiebreak" -> Triple(
                context.getString(R.string.announce_super_tiebreak),
                context.getString(
                    R.string.announce_super_tiebreak_msg,
                    state.matchConfig.setsToWin - 1,
                    state.matchConfig.superTiebreakPoints
                ),
                "\uD83C\uDFC6"
            )
            "deciding_point" -> Triple(
                context.getString(R.string.deciding_point),
                context.getString(R.string.deciding_point_msg),
                "\u2757"
            )
            else -> Triple("", "", "")
        }

        binding.textAnnouncementIcon.text = icon
        binding.textAnnouncementTitle.text = title
        binding.textAnnouncementMessage.text = message
        binding.buttonAnnouncementSkipSideChange.visibility =
            if (announcementType == "side_change") View.VISIBLE else View.GONE
    }
}