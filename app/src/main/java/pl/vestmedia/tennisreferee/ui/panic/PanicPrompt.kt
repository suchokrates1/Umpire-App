package pl.vestmedia.tennisreferee.ui.panic

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.lifecycle.lifecycleScope
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pl.vestmedia.tennisreferee.R
import pl.vestmedia.tennisreferee.TennisRefereeApp
import pl.vestmedia.tennisreferee.data.api.dto.PanicLineDto
import pl.vestmedia.tennisreferee.data.api.dto.PanicRequestDto
import pl.vestmedia.tennisreferee.data.auth.CourtSessionProvider
import pl.vestmedia.tennisreferee.databinding.DialogPanicBinding

object PanicPrompt {
    /**
     * Puts the SOS item in this screen's menu, after the screen's own items. Every screen
     * an umpire can stand on calls this once in onCreate; the screen's own menu handling
     * stays as it is.
     */
    fun install(activity: AppCompatActivity) {
        activity.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_panic, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId != R.id.action_panic) return false
                show(activity)
                return true
            }
        })
    }

    fun show(activity: AppCompatActivity) {
        val binding = DialogPanicBinding.inflate(activity.layoutInflater)
        val dialog = AlertDialog.Builder(activity)
            .setView(binding.root)
            .create()
        dialog.setCanceledOnTouchOutside(false)
        var token: String? = null
        var pollJob: Job? = null
        val connectivity = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                activity.runOnUiThread { showOffline(binding, !isOnline(activity)) }
            }

            override fun onLost(network: Network) {
                activity.runOnUiThread { showOffline(binding, true) }
            }
        }

        fun poll() {
            val current = token ?: return
            pollJob?.cancel()
            pollJob = activity.lifecycleScope.launch {
                while (isActive && dialog.isShowing) {
                    delay(3000)
                    if (!isOnline(activity)) {
                        showOffline(binding, true)
                        continue
                    }
                    showOffline(binding, false)
                    try {
                        val response = (activity.application as TennisRefereeApp)
                            .container.apiService.getPanicThread(current)
                        if (response.isSuccessful) {
                            render(activity, binding, response.body()?.messages.orEmpty())
                        }
                    } catch (_: IOException) {
                        showOffline(binding, true)
                    }
                }
            }
        }

        binding.buttonClose.setOnClickListener { dialog.dismiss() }
        binding.buttonWifi.setOnClickListener {
            activity.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
        binding.buttonSend.setOnClickListener {
            if (!isOnline(activity)) {
                showOffline(binding, true)
                return@setOnClickListener
            }
            val note = binding.noteInput.text?.toString().orEmpty()
            activity.lifecycleScope.launch {
                binding.buttonSend.isEnabled = false
                binding.waitingRow.visibility = View.VISIBLE
                binding.progress.visibility = View.VISIBLE
                binding.textWaiting.setText(R.string.panic_waiting)
                try {
                    val app = activity.application as TennisRefereeApp
                    val current = token
                    if (current == null) {
                        val response = app.container.apiService.sendPanic(
                            PanicRequestDto(courtId = courtId(activity), note = note.trim().ifEmpty { null })
                        )
                        when (response.code()) {
                            200 -> {
                                token = response.body()?.threadToken
                                binding.noteInput.text?.clear()
                                if (token != null) poll() else binding.waitingRow.visibility = View.GONE
                            }
                            429 -> {
                                binding.progress.visibility = View.GONE
                                binding.textWaiting.setText(R.string.panic_cooldown)
                            }
                            else -> {
                                binding.progress.visibility = View.GONE
                                binding.textWaiting.setText(R.string.panic_failed)
                            }
                        }
                    } else if (note.isBlank()) {
                        binding.waitingRow.visibility = View.GONE
                    } else {
                        val response = app.container.apiService.sendPanicFollowUp(
                            current,
                            PanicRequestDto(note = note.trim())
                        )
                        if (response.isSuccessful) {
                            binding.noteInput.text?.clear()
                            render(activity, binding, response.body()?.messages.orEmpty())
                        } else {
                            binding.progress.visibility = View.GONE
                            binding.textWaiting.setText(R.string.panic_failed)
                        }
                    }
                } catch (_: IOException) {
                    showOffline(binding, true)
                    binding.waitingRow.visibility = View.GONE
                } finally {
                    binding.buttonSend.isEnabled = true
                }
            }
        }

        dialog.setOnDismissListener {
            pollJob?.cancel()
            runCatching { connectivity.unregisterNetworkCallback(networkCallback) }
        }
        dialog.setOnShowListener {
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            runCatching {
                connectivity.registerNetworkCallback(
                    NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build(),
                    networkCallback
                )
            }
        }
        showOffline(binding, !isOnline(activity))
        dialog.show()
    }

    private fun courtId(activity: AppCompatActivity): String? {
        val app = activity.application as TennisRefereeApp
        return CourtSessionProvider.get().current()?.courtId?.trim()?.takeIf { it.isNotEmpty() }
            ?: app.healthCheckManager.courtId?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun isOnline(activity: AppCompatActivity): Boolean {
        val connectivity = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivity.activeNetwork ?: return false
        val caps = connectivity.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showOffline(binding: DialogPanicBinding, offline: Boolean) {
        binding.offlineBox.visibility = if (offline) View.VISIBLE else View.GONE
    }

    private fun render(activity: AppCompatActivity, binding: DialogPanicBinding, messages: List<PanicLineDto>) {
        binding.transcript.removeAllViews()
        if (messages.isEmpty()) {
            binding.transcriptScroll.visibility = View.GONE
            return
        }
        binding.transcriptScroll.visibility = View.VISIBLE
        val waiting = messages.last().direction != "desk"
        binding.waitingRow.visibility = if (waiting) View.VISIBLE else View.GONE
        binding.progress.visibility = if (waiting) View.VISIBLE else View.GONE
        if (waiting) binding.textWaiting.setText(R.string.panic_waiting)
        for (line in messages) {
            val view = TextView(activity)
            val who = if (line.direction == "desk") R.string.panic_desk else R.string.panic_you
            view.text = activity.getString(R.string.panic_transcript_line, activity.getString(who), line.text)
            view.textSize = 16f
            val gap = (8 * activity.resources.displayMetrics.density).toInt()
            view.setPadding(0, gap, 0, gap)
            binding.transcript.addView(view)
        }
    }
}
