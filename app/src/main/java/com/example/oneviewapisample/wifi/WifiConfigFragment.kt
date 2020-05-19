package com.example.oneviewapisample.wifi

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.example.oneviewapisample.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext



/**
 * Config wifi
 */
class WifiConfigFragment : PreferenceFragmentCompat() {

    private val KEY_WIFI_LIST: CharSequence = "wifi_list"
    private val KEY_WIFI_ENABLE: CharSequence = "wifi_enable"
    private val KEY_WIFI_ADD: CharSequence = "wifi_add"

    lateinit var wifiManager : WifiManager

    var mSwitchWifiOnOff : SwitchPreferenceCompat? = null
    var mWifiNetworksCategory : CollapsibleCategory?= null
    var mAddNew : Preference ?= null

    val wifiScanReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            Log.i("test", "wifiScanReceiver")
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success) {
                scanSuccess()
            }
        }
    }




    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStart() {
        super.onStart()

        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        activity?.registerReceiver(wifiScanReceiver, intentFilter)
    }

    override fun onStop() {
        super.onStop()
        activity?.unregisterReceiver(wifiScanReceiver)
    }



    fun scanSuccess() {
        val results = wifiManager.scanResults
        Log.i("test", "scanSuccess")

        mWifiNetworksCategory?.removeAll()

        for (scanResult in results) {
            Log.i("test", scanResult.SSID)

            var newPreference = Preference(activity)
            newPreference.title = scanResult.SSID
            newPreference.setOnPreferenceClickListener { showConnectToDialog(scanResult) }

            mWifiNetworksCategory?.addPreference(newPreference)

        }

        mSwitchWifiOnOff?.summary = "Connected to: "+wifiManager.connectionInfo.ssid

    }



    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.configure_wifi, rootKey)

        mSwitchWifiOnOff = findPreference(KEY_WIFI_ENABLE) as SwitchPreferenceCompat?
        mWifiNetworksCategory = findPreference(KEY_WIFI_LIST) as CollapsibleCategory?
        mAddNew = findPreference(KEY_WIFI_ADD) as Preference?

        //init preferences state:
        preferenceScreen.removePreference(mWifiNetworksCategory)
        preferenceScreen.removePreference(mAddNew)

        wifiManager = activity?.applicationContext!!.getSystemService(WifiManager::class.java)
        //check wifi status
        if (wifiManager.isWifiEnabled) {
            mSwitchWifiOnOff?.summary = "Loading list..."
            preferenceScreen.addPreference(mWifiNetworksCategory)
            preferenceScreen.addPreference(mAddNew)

            wifiManager.startScan()
        } else {
            mSwitchWifiOnOff?.summary=""
            mWifiNetworksCategory?.removeAll()
            preferenceScreen.removePreference(mWifiNetworksCategory)
            preferenceScreen.removePreference(mAddNew)
        }

    }



    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == null) {
            return super.onPreferenceTreeClick(preference)
        }
        when (preference.key) {
            KEY_WIFI_ENABLE -> {
                val isChecked = (preference as SwitchPreferenceCompat).isChecked
                wifiManager.setWifiEnabled(isChecked)
                if (isChecked) {
                    mSwitchWifiOnOff?.summary = "Loading list..."
                    preferenceScreen.addPreference(mWifiNetworksCategory)
                    preferenceScreen.addPreference(mAddNew)

                    wifiManager.startScan()
                } else {
                    mSwitchWifiOnOff?.summary=""
                    mWifiNetworksCategory?.removeAll()
                    preferenceScreen.removePreference(mWifiNetworksCategory)
                    preferenceScreen.removePreference(mAddNew)
                }

                return true
            }
            KEY_WIFI_ADD -> {
                showAddNewDialog()
                return true
            }


        }
        return super.onPreferenceTreeClick(preference)
    }


    /**
     * Add new network dialog
     * */
    fun showAddNewDialog() {
        val view = LayoutInflater.from(this.requireContext()).inflate(R.layout.configure_wifi_dialog, null)
        var chShowpassword = view.findViewById<CheckBox>(R.id.chShowpassword)
        var edName = view.findViewById<EditText>(R.id.edName)
        var securitySpinner = view.findViewById<Spinner>(R.id.securitySpinner)
        var edPassword = view.findViewById<EditText>(R.id.edPassword)
        edPassword.setText("rqoi4366")
        var txPassword = view.findViewById<TextView>(R.id.txPassword)

        val alertDialog: AlertDialog = AlertDialog.Builder(this.requireContext())
            .setTitle("Add new Wi-Fi network")
            .setView(view)
            .setPositiveButton("OK" ){dialog, which ->
                addNewNetworkConfiguration(edName.text.toString(), edPassword.text.toString(), securitySpinner.selectedItemPosition)
            }
            .setNegativeButton("Cancel", null)
            .create()



        chShowpassword.setOnClickListener {
            if (chShowpassword.isChecked) {
                edPassword.transformationMethod = null
            } else {
                edPassword.transformationMethod = PasswordTransformationMethod()
            }
        }


        val adapter = ArrayAdapter.createFromResource(
            this.requireContext().applicationContext,
            R.array.security_type_array,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        securitySpinner.adapter = adapter
        securitySpinner.setSelection(1)


        securitySpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                if (position == 0 || position == 2) {
                    chShowpassword.visibility = View.GONE
                    edPassword.visibility = View.GONE
                    txPassword.visibility = View.GONE
                } else {
                    chShowpassword.visibility = View.VISIBLE
                    edPassword.visibility = View.VISIBLE
                    txPassword.visibility = View.VISIBLE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        })


        alertDialog.show()


    }


    /**
     * Add new network configuration to wifimanager
     * */
    private fun addNewNetworkConfiguration(name: String, password: String, indexSecurityType: Int) {
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(
                this.requireContext(),
                "Name cannot be empty.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(
                this.requireContext(),
                "Password cannot be empty.",
                Toast.LENGTH_LONG
            ).show()
            return
        } else if (indexSecurityType == 1 && password.length < 8) {
            Toast.makeText(
                this.requireContext(),
                "Password must have at least 8 characters.",
                Toast.LENGTH_LONG
            ).show()
            return
        }


        val apConfig = WifiConfiguration()
        apConfig.SSID = "\"" + name + "\""
        apConfig.preSharedKey =  "\"" + password +  "\""
        apConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
        apConfig.status = WifiConfiguration.Status.ENABLED


        if (indexSecurityType == 1) {
            apConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
        } else if (indexSecurityType == 2) {
            apConfig.wepTxKeyIndex = 0;
            apConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            apConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
        } else {
            apConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
        }

        val ret = wifiManager.addNetwork(apConfig)

        connectTo(apConfig)

    }



    /**
     * Show a dialog to connect to a already existing wifi
     * */
    fun showConnectToDialog(ssid : ScanResult) : Boolean {

        if (ssid.capabilities.contains("WPA")) {
            val view = LayoutInflater.from(this.requireContext()).inflate(R.layout.configure_wifi_dialog2, null)
            var chShowpassword = view.findViewById<CheckBox>(R.id.chShowpassword)
            var edPassword = view.findViewById<EditText>(R.id.edPassword)
            edPassword.setText("rqoi4366")

            chShowpassword.setOnClickListener {
                if (chShowpassword.isChecked) {
                    edPassword.transformationMethod = null
                } else {
                    edPassword.transformationMethod = PasswordTransformationMethod()
                }
            }

            val alertDialog: AlertDialog = AlertDialog.Builder(this.requireContext())
                .setTitle("Connect to "+ ssid.SSID)
                .setView(view)
                .setPositiveButton("OK" ){dialog, which ->
                    connectTo(ssid, edPassword.text.toString())

                }
                .setNegativeButton("Cancel", null)
                .create()

            alertDialog.show()
        } else {
            connectTo(ssid, null)
        }




        return true

    }



    /**
     * Connect to an existing wifi
     * */
    private fun connectTo(scanResult: ScanResult, password: String?) {
        //check if is already configured
        val list = wifiManager.configuredNetworks
        var config : WifiConfiguration? = null
        for (i in list) {
            Log.i("test", "add new configuration: i="+i.SSID)
            if (i.SSID != null && i.SSID.equals(scanResult.SSID)) {
                config = i

                break
            }
        }

        if (config == null) {
            config = WifiConfiguration()

            config.SSID = "\"" + scanResult.SSID + "\""
            config.BSSID = scanResult.BSSID

            if (scanResult.capabilities.contains("WPA") && password != null) {
                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

                config.preSharedKey = "\"" + password + "\""
            } else {
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                config.allowedAuthAlgorithms.clear();
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            }

            wifiManager.addNetwork(config)
        } else if (scanResult.capabilities.contains("WPA") && password != null) {
            config.preSharedKey = "\"" + password + "\""
        }


        connectTo(config)

    }


    /**
     * Connect to a specific wifi configuration if found
     * */
    fun connectTo(config: WifiConfiguration) {
        val list = wifiManager.configuredNetworks
        for (i in list) {
            Log.i("test", "add new configuration: i="+i.SSID)
            if (i.SSID != null && i.SSID.equals("\"" + config.SSID + "\"")) {
                wifiManager.disconnect()
                wifiManager.enableNetwork(i.networkId, true)
                wifiManager.reconnect()

                break
            }
        }

        wifiManager.startScan()
    }



}
