package com.example.oneviewapisample

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageDataObserver
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.Resources.NotFoundException
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.math.abs


class MainActivity : AppCompatActivity() {

    private companion object {
        const val FILE_HOME = "/mnt/sdcard/"
        const val PACKAGE_INSTALLED_ACTION ="com.example.oneviewsample.SESSION_API_PACKAGE_INSTALLED"
    }



    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        //Configure buttons click
        clearDataBtn.setOnClickListener { showInstalledApps() }
        closeAllRunningAppsBtn.setOnClickListener { closeAllRunningApps() }
        clearClipboardBtn.setOnClickListener { clearClipboard() }
        changeTimezoneBtn.setOnClickListener { changeTimezone() }
        showLogcatBtn.setOnClickListener { showLogcat() }
        deleteFilesBtn.setOnClickListener { showFiles() }
        installApkBtn.setOnClickListener { showFiles(false) }
        getDeviceNameBtn.setOnClickListener { showDeviceName() }
        setDeviceNameBtn.setOnClickListener { showSetDeviceNameDialog() }
        setAutoTimeOff.setOnClickListener { showSetAutoTimeOff() }
        btSendKeyboardEvents.setOnClickListener { executeKeyEvent(KeyEvent.KEYCODE_HOME, true) }

    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    fun executeKeyEvent(keyCode: Int, isLongPress: Boolean)  {

        val commandToRun = StringBuilder("input keyevent ")
        if (isLongPress) {
            commandToRun.append("--longpress ")
        }
        commandToRun.append(keyCode)

        Log.i("Test", "command: "+commandToRun)
        try {

            Runtime.getRuntime().exec(commandToRun.toString())
        } catch (e: IOException) {
            e.printStackTrace()

        }

    }


    /**
     * Install apk from a given path
     */
    private fun installApk(packagePath: String): Boolean {
        val packageInstaller = packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)

        return try {
            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)
            val out = session.openWrite("package", 0, -1)
            File(packagePath).inputStream().copyTo(out)
            session.fsync(out)
            out.close()

            // Create an install status receiver.
            val intent = Intent(this, MainActivity::class.java)
            intent.action = PACKAGE_INSTALLED_ACTION
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
            val statusReceiver = pendingIntent.intentSender
            session.commit(statusReceiver)
            true
        } catch (e: IOException) {
            Toast.makeText(
                this, "Install failed!", Toast.LENGTH_SHORT
            ).show()
            e.printStackTrace()
            false
        }
    }

    /**
     * Intent receive after apk installation
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val extras = intent?.extras
        if (PACKAGE_INSTALLED_ACTION == intent?.action) {
            val status = extras?.getInt(PackageInstaller.EXTRA_STATUS)
            val message = extras?.getString(PackageInstaller.EXTRA_STATUS_MESSAGE)
            when (status) {
                PackageInstaller.STATUS_PENDING_USER_ACTION-> {
                    // This test app isn't privileged, so the user has to confirm the install.
                    val confirmIntent = extras.get(Intent.EXTRA_INTENT) as Intent
                    startActivity(confirmIntent)
                }
                PackageInstaller.STATUS_SUCCESS-> {
                    Toast.makeText(this, "Install succeeded!", Toast.LENGTH_SHORT).show()
                }
                PackageInstaller.STATUS_FAILURE,
                PackageInstaller.STATUS_FAILURE_ABORTED,
                PackageInstaller.STATUS_FAILURE_BLOCKED,
                PackageInstaller.STATUS_FAILURE_CONFLICT,
                PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
                PackageInstaller.STATUS_FAILURE_INVALID,
                PackageInstaller.STATUS_FAILURE_STORAGE-> {
                    Toast.makeText(
                        this, "Install failed! $status, $message",
                        Toast.LENGTH_SHORT
                    ).show()
                }else->
                    Toast.makeText(this, "Unrecognized status received from installer: $status",
                            Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Show a popup with logcat
     */
    private fun showLogcat() {
        val textView = TextView(this)
        textView.maxLines = 15
        textView.minLines = 15
        textView.setLines(15)

        val alertDialog: AlertDialog = AlertDialog.Builder(this)
            .setView(textView)
            .setTitle("Logcat")
            .setNegativeButton("Close", null)
            .setPositiveButton("Update", null)
            .create()

        alertDialog.setOnShowListener { dialog ->
            val button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                textView.text = getLogcat()
            }
        }

        textView.text = getLogcat()
        alertDialog.show()
    }

    /**
     * Get the last 15 lines from logcat
     */
    private fun getLogcat(): String {
        try {
            val command = arrayOf( "logcat", "-t", "15" )
            val process = Runtime.getRuntime().exec(command)
            val bufferedReader = BufferedReader(
                InputStreamReader(process.inputStream)
            )
            val log = StringBuilder()
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                log.append(line + "\n")
            }
            return log.toString()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ""
    }

    /**
     * Clear clipboard
     */
    private fun clearClipboard() {
        val clipService = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("", "")
        clipService.setPrimaryClip(clipData)
        Toast.makeText(this, "Clipboard cleared!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Function to show all installed apps
     */
    private fun showInstalledApps() {
        val listView = ListView(this)

        val alertDialog: AlertDialog = AlertDialog.Builder(this)
            .setView(listView)
            .setTitle("Installed Packages")
            .setNegativeButton("Cancel", null)
            .create()


//        val intent = Intent(Intent.ACTION_MAIN, null)
//        intent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
//        val apps = packageManager.queryIntentActivities(intent, PackageManager.GET_META_DATA)
//        val values = mutableListOf<String>()
//        for (app in apps) {
//            app.activityInfo.packageName?.let {
//                if(packageName != it) {
//                    values.add(it)
//                }
//            }
//        }

        val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        val values = mutableListOf<String>()
        for (pi in packages) {
            pi.packageName?.let {
                if(packageName != it) {
                    values.add(it)
                }
            }
        }

        val adapter: ArrayAdapter<String> = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1, android.R.id.text1, values
        )
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            clearAppData(values[position])
            alertDialog.dismiss()
        }
        alertDialog.show()
    }



    /**
     * Function to show and select timezones
     */
    private fun changeTimezone() {
        val listView = ListView(this)

        val alertDialog: AlertDialog = AlertDialog.Builder(this)
            .setView(listView)
            .setTitle("Timezones")
            .setNegativeButton("Cancel", null)
            .create()

        val values = mutableListOf<String>()

        val timezones: MutableList<TimeZone> = ArrayList()
        val ids = TimeZone.getAvailableIDs()
        for (id in ids) {
            timezones.add(TimeZone.getTimeZone(id))
        }
        timezones.sortWith(Comparator { s1, s2 -> (s1?.rawOffset ?: 0) - (s2?.rawOffset ?: 0) })
        for (t in timezones) {
            values.add((formatTimeZone(t) + t.id))
        }

        val adapter: ArrayAdapter<String> = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1, android.R.id.text1, values
        )
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            setTimezone(timezones[position].id)

            Toast.makeText(this@MainActivity,
                "Timezone set to ${values[position]}.",
                Toast.LENGTH_SHORT)
                .show()

            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    /**
     * Set the device timezone
     */
    private fun setTimezone(timezoneId: String) {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setTimeZone(timezoneId)
    }


    /**
     * Format name of a timezone
     */
    private fun formatTimeZone(tz: TimeZone): String? {
        val hours: Long = TimeUnit.MILLISECONDS.toHours(tz.rawOffset.toLong())
        var minutes: Long = (TimeUnit.MILLISECONDS.toMinutes(tz.rawOffset.toLong())
                - TimeUnit.HOURS.toMinutes(hours))
        minutes = abs(minutes)
        return when {
            hours > 0 -> {
                String.format("(GMT+%d:%02d) %s", hours, minutes, tz.id)
            }
            hours < 0 -> {
                String.format("(GMT%d:%02d) %s", hours, minutes, tz.id)
            }
            else -> {
                String.format("(GMT) %s", tz.id)
            }
        }
    }

    /**
     * Close all running apps
     */
    private fun closeAllRunningApps() {
        val packages: List<ApplicationInfo> = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        var count = 0
        for(pkgInfo in packages) {
            if(pkgInfo.flags and ApplicationInfo.FLAG_STOPPED == 0 &&
                pkgInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0 &&
                        pkgInfo.packageName != packageName) {
                if(closeApplication(pkgInfo.packageName)) {
                    count++
                }
            }
        }

        Toast.makeText(this@MainActivity,
            "$count apps closed!",
            Toast.LENGTH_SHORT)
            .show()
    }

    /**
     * Close an application by package name
     */
    private fun closeApplication(packageName: String): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        try {
            val m: Method = activityManager.javaClass.getDeclaredMethod(
                "forceStopPackage",
                String::class.java
            )
            m.isAccessible = true
            m.invoke(activityManager, packageName)
            return true
        } catch (e: Exception) {
            Toast.makeText(this, "Close Application $packageName Error!", Toast.LENGTH_SHORT).show()
        }

        return false
    }

    /**
     * Function used to clear an app data. It only works running as a system app.
     */
    private fun clearAppData(packageName: String) {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        try {
            val m: Method = am.javaClass.getDeclaredMethod("clearApplicationUserData", String::class.java, IPackageDataObserver::class.java)
            m.isAccessible = true
            val res = m.invoke(am, packageName, object: IPackageDataObserver.Stub() {
                override fun onRemoveCompleted(mPackageName: String?, succeeded: Boolean) {
                    runOnUiThread {
                        if (succeeded) {
                            Toast.makeText(this@MainActivity,
                                "Clear Application User Data Success!",
                                Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            Toast.makeText(this@MainActivity,
                                "Clear Application User Data Error!",
                                Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            } ) as Boolean
            if(!res) {
                Toast.makeText(this, "Clear Application User Data Error!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Clear Application User Data Error!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Show files from sdcard
     */
    private fun showFiles(deleteFiles: Boolean = true) {
        val listView = ListView(this)

        val alertDialog: AlertDialog = AlertDialog.Builder(this)
            .setView(listView)
            .setTitle("Delete Files")
            .setNegativeButton("Close", null)
            .setPositiveButton("Home", null)
            .create()

        val values = getFiles()
        val adapter: ArrayAdapter<String> = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1, android.R.id.text1, values
        )
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val file = File(adapter.getItem(position) ?: FILE_HOME)
            if(!file.isDirectory) {
                if(deleteFiles) {
                    deleteFile(file)
                } else {
                    installApk(file.path)
                }
                alertDialog.dismiss()
            } else {
                adapter.clear()
                adapter.addAll(getFiles(file.path))
                adapter.notifyDataSetChanged()
            }
        }

        alertDialog.setOnShowListener { dialog ->
            val positiveBtn = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            positiveBtn.setOnClickListener {
                adapter.clear()
                adapter.addAll(getFiles())
                adapter.notifyDataSetChanged()
            }
        }

        alertDialog.show()
    }

    /**
     * Delete the given file
     */
    private fun deleteFile(file: File) {
        try {
            file.delete()
            Toast.makeText(this, "File delete success!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "File delete error!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Get files from specific path
     */
    private fun getFiles(path: String = FILE_HOME): List<String> {
        val list = mutableListOf<String>()
        val rootFile = File(path)
        rootFile.listFiles()?.let {
            for (file in it) {
                list.add(file.path)
            }
        }
        return list
    }



    private fun getDeviceName() : String {
        try {
            val command = "settings get global device_name"
            val process = Runtime.getRuntime().exec(command)
            val bufferedReader = BufferedReader(
                InputStreamReader(process.inputStream)
            )

            return bufferedReader.readLine()

        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ""


     //   return Settings.System.getString(applicationContext.contentResolver,Settings.Global.DEVICE_NAME);
    }


    private fun showDeviceName() {
        Toast.makeText(this, "Current device name: " + getDeviceName(), Toast.LENGTH_SHORT).show()
    }


    /**
     * Change device's name
     * */

    private fun showSetDeviceNameDialog() {
        val editText = EditText(this)
        val deviceName = getDeviceName();
        Log.i("Test", "Device name: "+ deviceName)

        editText.setText(deviceName)

        val alertDialog: AlertDialog = AlertDialog.Builder(this)
            .setView(editText)
            .setTitle("Change device Name")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Ok"){dialog, which ->

                //Tv settings does this but doesn't work here
//                val res = Settings.System.putString(applicationContext.contentResolver,Settings.Global.DEVICE_NAME , editText.text.toString())
//                val btAdapter = BluetoothAdapter.getDefaultAdapter()
//                if (btAdapter != null) {
//                    btAdapter.name = editText.text.toString()
//                }

                //works; change also in Settings->Device Preferences-> About-> Device name
                val command = "settings put global device_name " + editText.text.toString();
                try {
                    Runtime.getRuntime().exec(command)
                } catch (e: IOException) {
                    e.printStackTrace()
                }


            }
            .create()

        alertDialog.show()
    }



    private fun showSetAutoTimeOff() {
        val editText = EditText(this)
        editText.inputType = InputType.TYPE_CLASS_DATETIME

        val alertDialog: AlertDialog = AlertDialog.Builder(this)
            .setView(editText)
            .setTitle("Auto time off (minutes)")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Ok"){dialog, which ->

                val timeOffTime  =  TimeUnit.MINUTES.toMillis(editText.text.toString().toLong()).toInt()

                Settings.System.putInt(applicationContext.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, timeOffTime)

            }
            .create()

        alertDialog.show()
    }


}

