package com.example.oneviewapisample.wifi

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.oneviewapisample.R

class ConfigureWifiActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configure_wifi)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, WifiConfigFragment())
            .commit()
    }
}
