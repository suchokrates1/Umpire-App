package pl.vestmedia.tennisreferee.ui.match

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class DeviceBatteryInfoProvider(
    private val application: Application
) {
    fun current(): MatchBatteryInfo {
        val batteryStatus = application.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return MatchBatteryInfo(
            level = batteryStatus?.batteryLevel(),
            isCharging = batteryStatus?.isCharging()
        )
    }

    private fun Intent.batteryLevel(): Int? {
        val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level >= 0 && scale > 0) (level * 100 / scale) else null
    }

    private fun Intent.isCharging(): Boolean {
        val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }
}