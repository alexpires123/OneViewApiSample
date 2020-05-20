package com.example.oneviewapisample.hotspot

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.oneviewapisample.R
import com.example.oneviewapisample.hotspot.SettingsHotspotFragment.Companion.newInstance

class SettingsHotspotActivity : AppCompatActivity() {
    protected fun createSettingsFragment(): Fragment {
        return newInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_hotspot)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_browse_fragment, newInstance())
            .commit()
    }
}