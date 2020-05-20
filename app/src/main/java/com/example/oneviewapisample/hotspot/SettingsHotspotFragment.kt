package com.example.oneviewapisample.hotspot

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager.SimplePreferenceComparisonCallback
import androidx.preference.SwitchPreference
import com.example.oneviewapisample.R


class SettingsHotspotFragment : PreferenceFragmentCompat() {
    var mTurnOnOff: SwitchPreference? = null
    var mEditSettings: Preference? = null
    var wifiHotspotConfig: WifiHotspotConfig? = null


    var mOnStartTetheringCallBack = object : OnStartTetheringCallBack() {
        override fun onTetheringStarted() {
            updateOnOff(true)
        }

        override fun onTetheringFailed() {
            updateOnOff(false)
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        setPreferencesFromResource(R.xml.wifi_hotspot, null)

        preferenceManager.preferenceComparisonCallback = SimplePreferenceComparisonCallback()

        mTurnOnOff = findPreference(KEY_ON_OFF)
        mEditSettings = findPreference(KEY_EDIT_SETTINGS)
        wifiHotspotConfig = WifiHotspotConfig.getInstance(this.requireContext())
        mEditSettings?.title = wifiHotspotConfig!!.getConfiguration()?.SSID
        mTurnOnOff?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->

                wifiHotspotConfig?.turnOnOff(newValue as Boolean, mOnStartTetheringCallBack)
                mTurnOnOff!!.isChecked
            }
        mEditSettings!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(
                this@SettingsHotspotFragment.activity,
                ConfigSettingsHotspotActivity::class.java
            )
            startActivityForResult(
                intent,
                REQUEST_EDIT_HOTSPOT_CODE
            )
            true
        }

        updateOnOff(wifiHotspotConfig!!.isOn())
    }



    @RequiresApi(Build.VERSION_CODES.M)
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EDIT_HOTSPOT_CODE && resultCode == Activity.RESULT_OK) {
            //update configuration in preferences
            mEditSettings?.setTitle(wifiHotspotConfig?.getConfiguration()?.SSID)
        }
    }



    private fun updateOnOff(isOn: Boolean) {
        this.activity?.runOnUiThread(java.lang.Runnable {
            mTurnOnOff!!.isChecked = isOn
            if (mTurnOnOff!!.isChecked) {
                mTurnOnOff!!.title = "ON"
            } else {
                mTurnOnOff!!.title = "OFF"
            }
        })
    }



    companion object {
        const val REQUEST_EDIT_HOTSPOT_CODE = 100
        private const val KEY_ON_OFF = "turn_on_off"
        private const val KEY_EDIT_SETTINGS = "edit_settings"
        @JvmStatic
        fun newInstance(): SettingsHotspotFragment {
            return SettingsHotspotFragment()
        }
    }
}