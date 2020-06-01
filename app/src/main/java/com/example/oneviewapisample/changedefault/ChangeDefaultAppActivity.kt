package com.example.oneviewapisample.changedefault

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PatternMatcher
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.core.content.getSystemService
import com.example.oneviewapisample.R
import kotlinx.android.synthetic.main.activity_change_default_app.*
import java.lang.reflect.Method

class ChangeDefaultAppActivity : AppCompatActivity() {

    lateinit var changeDefaultApp : ChangeDefaultApplication
    val TAG: String = "ChangeDefaultApp"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_default_app)

        //create web implicit intent
        val webIntent: Intent = Uri.parse("https://www.android.com").let { webpage ->
            Intent(Intent.ACTION_VIEW, webpage)
        }

        changeDefaultApp = ChangeDefaultApplication(this.applicationContext)

        btDefault.setOnClickListener { loadInDefaultApplication(webIntent) }
        btSetDefaultApp.setOnClickListener { showAppsList(webIntent) }
    }


    /**
     * Start default app for implicit intent
     * */
    fun loadInDefaultApplication(intent:Intent) {
        if(intent.resolveActivity(packageManager) != null ) {
            startActivity(intent)
        }

    }


    /**
     * Choose default app dialog
     * */
    fun showAppsList(intent:Intent) {
        val listView = ListView(this)

        val alertDialog: AlertDialog = AlertDialog.Builder(this)
            .setView(listView)
            .setTitle("Applications")
            .setNegativeButton("Cancel", null)
            .create()

        val values = mutableListOf<String>()
        for(info in changeDefaultApp.getAllActivities(intent)) {
            values.add(info.activityInfo.packageName)
        }

        val adapter: ArrayAdapter<String> = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1, android.R.id.text1, values
        )
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            changeDefaultApp.changeDefaultApp(values[position], intent)
            alertDialog.dismiss()
            Toast.makeText(this,"Check load default button if default app was changed", Toast.LENGTH_SHORT).show()
        }
        alertDialog.show()

    }

}
