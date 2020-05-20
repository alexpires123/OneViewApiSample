package com.example.oneviewapisample.hotspot

import android.app.Activity
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiConfiguration.KeyMgmt
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.oneviewapisample.R
import kotlinx.android.synthetic.main.wifihotspot_settings_dialog.*

class ConfigSettingsHotspotActivity : AppCompatActivity() {

    var mWifiHotspotConfig: WifiHotspotConfig? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wifihotspot_settings_dialog)

        cbShowPassword?.setOnClickListener(View.OnClickListener {
            if (cbShowPassword.isChecked) {
                edPassword?.transformationMethod = null
            } else {
                edPassword?.transformationMethod = PasswordTransformationMethod()
            }
        })
        
        //set spinners

        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.security_type_array2,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spSecurity.adapter = adapter
        spSecurity.setSelection(1)

        val adapterApBand = ArrayAdapter.createFromResource(
            this,
            R.array.apband_array,
            android.R.layout.simple_spinner_item
        )
        adapterApBand.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spApBand.adapter = adapterApBand
        spApBand.setSelection(0)

        btSave.setOnClickListener(View.OnClickListener {
            if (saveConfiguration(
                    edHotspotName.getText().toString(), edPassword.getText().toString(),
                    spSecurity.getSelectedItemPosition(), spApBand.getSelectedItemPosition()
                )
            ) {
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                Toast.makeText(
                    this@ConfigSettingsHotspotActivity,
                    "Not possible to change Hotspot Settings.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        btCancel.setOnClickListener(View.OnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        })
        fillConfiguration()
    }

    /**
     * Fill fields with current configuration *
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun fillConfiguration() {
        mWifiHotspotConfig = WifiHotspotConfig.getInstance(this)
        val wifiHotspotConfig = mWifiHotspotConfig!!.getConfiguration()
        edHotspotName!!.setText(wifiHotspotConfig?.SSID)

        val authType = mWifiHotspotConfig!!.getAuthType(wifiHotspotConfig)

        if (authType == 1 || authType == 4) {
            spSecurity!!.setSelection(1)
            edPassword!!.setText(wifiHotspotConfig?.preSharedKey)
        } else {
            spSecurity!!.setSelection(0)
        }

        spApBand!!.setSelection(mWifiHotspotConfig!!.getApBand(wifiHotspotConfig))  // 0 - AP_BAND_2GHZ, 1 - AP_BAND_5GHZ

    }

    /**
     * Save new ap configuration
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun saveConfiguration(
        name: String,
        password: String,
        indexSecurityType: Int,
        indexAPBand: Int
    ): Boolean {
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(
                this@ConfigSettingsHotspotActivity,
                "Hotspot name cannot be empty.",
                Toast.LENGTH_LONG
            ).show()
            return false
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(
                this@ConfigSettingsHotspotActivity,
                "Password cannot be empty.",
                Toast.LENGTH_LONG
            ).show()
            return false
        } else if (password.length < 8) {
            Toast.makeText(
                this@ConfigSettingsHotspotActivity,
                "Password must have at least 8 characters.",
                Toast.LENGTH_LONG
            ).show()
            return false
        }
        var apConfig = mWifiHotspotConfig!!.getConfiguration()
        apConfig?.SSID = name
        apConfig?.preSharedKey = password
        apConfig?.allowedAuthAlgorithms?.set(WifiConfiguration.AuthAlgorithm.OPEN)
        apConfig?.status = WifiConfiguration.Status.ENABLED
        if (indexSecurityType == 1) {
            apConfig?.allowedKeyManagement?.set(4)
        } else {
            apConfig?.allowedKeyManagement?.set(0)
        }
        apConfig = mWifiHotspotConfig!!.setApBand(apConfig, indexAPBand)

        return mWifiHotspotConfig!!.setConfiguration(apConfig)
    }
}