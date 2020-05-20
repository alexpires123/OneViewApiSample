package com.example.oneviewapisample.hotspot

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.annotation.RequiresApi
import com.android.dx.stock.ProxyBuilder
import java.lang.reflect.Method

@RequiresApi(Build.VERSION_CODES.M)
class WifiHotspotConfig private constructor(context: Context) {
    private val TAG = "WifiHotspotConfig"
    private var mContext: Context? = null
    private var mWifiManager: WifiManager? = null
    private var mConnectivityManager: ConnectivityManager? = null

//    fun WifiHotspotConfig(context: Context) {
//        mContext = context
//        mWifiManager =
//            mContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//        mConnectivityManager =
//            mContext.getSystemService(ConnectivityManager::class.java)
//    }

    init {

        mContext = context
        mWifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        mConnectivityManager = context.getSystemService(ConnectivityManager::class.java)
    }

    companion object : SingletonHolder<WifiHotspotConfig, Context>(::WifiHotspotConfig)



    /**
     * Set Hotspot configuration
     */
    fun setConfiguration(apConfig: WifiConfiguration?): Boolean {
        try {
            val setConfigMethod = mWifiManager!!.javaClass.getMethod("setWifiApConfiguration", WifiConfiguration::class.java)
            val status = setConfigMethod.invoke(mWifiManager, apConfig) as Boolean
            Log.i(
                TAG,
                "setWifiApConfiguration - success? $status"
            )
            return status
        } catch (e: Exception) {
            Log.e(TAG, "Error in configureHotspot")
            e.printStackTrace()
        }
        return false
    }


    fun getAuthType(apConfig: WifiConfiguration?): Int {
        try {
            val getAuthMethod = apConfig?.javaClass?.getMethod(
                "getAuthType")
            getAuthMethod?.isAccessible = true
            val authType = getAuthMethod?.invoke(apConfig) as Int
            Log.e(TAG, "getAuthType -$authType")
            return authType
        } catch (e: Exception) {
            Log.e(TAG, "Error in configureHotspot")
            e.printStackTrace()
        }
        return WifiConfiguration.KeyMgmt.NONE
    }


    fun getApBand(apConfig: WifiConfiguration?): Int {
        try {
            val apbandField = apConfig?.javaClass?.getField("apBand")
            return apbandField?.getInt(apConfig)!!
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
        return -1
    }


    fun setApBand(apConfig: WifiConfiguration?, apBandValue: Int): WifiConfiguration? {
        try {
            val apbandField = apConfig?.javaClass?.getField("apBand")
            apbandField?.setInt(apConfig, apBandValue)
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
        return apConfig
    }


    /**
     * Get current hotspot configuration
     */
    fun getConfiguration(): WifiConfiguration? {
        try {
            val getWifiApConfigurationMethod =
                mWifiManager!!.javaClass.getMethod("getWifiApConfiguration")
            return getWifiApConfigurationMethod.invoke(mWifiManager) as WifiConfiguration
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }


    fun isOn(): Boolean {

        //return mWifiManager.isWifiEnabled();
        try {
            val getWifiApState =
                mWifiManager!!.javaClass.getMethod("getWifiApState")
            return getWifiApState.invoke(mWifiManager) as Int == 13
        } catch (e: Exception) {
            e.printStackTrace()
        }
        //   return mWifiManager.getWifiApState() == 13;
        return false
    }


    /**
     * Turn hotspot on/off
     */
    fun turnOnOff(turnOn: Boolean, callback: OnStartTetheringCallBack?) {
        mWifiManager!!.isWifiEnabled = turnOn
        if (turnOn) {
            startTethering(callback)
        } else {
            stopTethering()
        }
    }


    /**
     * This enables tethering using the ssid/password defined in Settings App>Hotspot & tethering
     *
     */
    private fun startTethering(callback: OnStartTetheringCallBack?): Boolean {
        val outputDir = mContext!!.codeCacheDir
        val proxy = try {
            ProxyBuilder.forClass(OnStartTetheringCallbackClass())
                .dexCache(outputDir).handler { proxy, method, args ->
                    when (method.name) {
                        "onTetheringStarted" -> callback?.onTetheringStarted()
                        "onTetheringFailed" -> callback?.onTetheringFailed()
                        else -> ProxyBuilder.callSuper(proxy, method, *args)
                    }
                    null
                }.build()
        } catch (e: Exception) {
            Log.e(TAG, "Error in enableTethering ProxyBuilder")
            e.printStackTrace()
            return false
        }
        var method: Method? = null
        try {
            method = mConnectivityManager!!.javaClass.getDeclaredMethod(
                "startTethering",
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType, OnStartTetheringCallbackClass(),
                Handler::class.java
            )
            if (method == null) {
                Log.e(TAG, "startTetheringMethod is null")
            } else {
                method.invoke(mConnectivityManager, 0, true, proxy, null)
                Log.d(TAG, "startTethering invoked")
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error in enableTethering")
            e.printStackTrace()
        }
        return false
    }


    private fun stopTethering() {
        try {
            val method =
                mConnectivityManager!!.javaClass.getDeclaredMethod(
                    "stopTethering",
                    Int::class.javaPrimitiveType
                )
            if (method == null) {
                Log.e(TAG, "stopTetheringMethod is null")
            } else {
                method.invoke(mConnectivityManager, 0)
                Log.d(TAG, "stopTethering invoked")
            }
        } catch (e: Exception) {
            Log.e(TAG, "stopTethering error: $e")
            e.printStackTrace()
        }
    }


    /**
     * Get OnStartTetheringCallback class from system
     */
    private fun OnStartTetheringCallbackClass(): Class<*>? {
        try {
            return Class.forName("android.net.ConnectivityManager\$OnStartTetheringCallback")
        } catch (e: ClassNotFoundException) {
            Log.e(
                TAG,
                "OnStartTetheringCallbackClass error: $e"
            )
            e.printStackTrace()
        }
        return null
    }
}