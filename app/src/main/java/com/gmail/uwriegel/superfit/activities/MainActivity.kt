package com.gmail.uwriegel.superfit.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.SoundEffectConstants
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import com.gmail.uwriegel.superfit.R
import com.gmail.uwriegel.superfit.sensor.SensorService
import com.gmail.uwriegel.superfit.tracking.DataSource
import com.gmail.uwriegel.superfit.tracking.exportToGpx
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*

// TODO: DATABase ändern: wenn track gespeichert, kennzeichnen, damit löschbar
// TODO: In NavigationBar Möglichkeit, alle Herzfrequenz- und Radsensoren auswählbar machen, Auswahl speichern und kontrollieren
// TODO: Ausgewähltes Ant+-Gerät direkt ansprechen
// TODO: TEst
class MainActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        initializeNavigationDrawer()

        val webSettings = mainWebView.settings
        webSettings.javaScriptEnabled = true
        // webSettings.domStorageEnabled = true
        WebView.setWebContentsDebuggingEnabled(true)
        // CORS allowed
        webSettings.allowUniversalAccessFromFileURLs = true
        mainWebView.webChromeClient = WebChromeClient()

        mainWebView.isHapticFeedbackEnabled = true
        mainWebView.addJavascriptInterface(object {
            @JavascriptInterface
            fun doHapticFeedback() = doAsync { uiThread { mainWebView.playSoundEffect(SoundEffectConstants.CLICK) } }

            @JavascriptInterface
            fun start() = doAsync { uiThread {
                val startIntent = Intent(this@MainActivity, SensorService::class.java)
                startIntent.action = SensorService.START
                startService(startIntent)

                display()
            } }

            @JavascriptInterface
            fun stop() = doAsync { uiThread {
                finish()
            } }
        }, "Native")

        if (checkPermissions())
            initialize()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else
            super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MainActivity.REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS -> {
                val perms = HashMap<String, Int>()
                // Initial
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED)
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED)
                // Fill with results
                for ((index, value) in permissions.withIndex())
                    perms.put(value, grantResults[index])
                // Check for ACCESS_FINE_LOCATION
                if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == PackageManager.PERMISSION_GRANTED
                        && perms[Manifest.permission.WRITE_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED)
                    // All Permissions Granted
                    initialize()
                else
                    // Permission Denied
                    Toast.makeText(this, "Some Permission is Denied", Toast.LENGTH_SHORT).show()
            }
            else ->
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CREATE_REQUEST_CODE) {
                if (data != null) {
                    val dataSource = DataSource(this@MainActivity)
                    val trackPoints = dataSource.getTrackPoints(currentTrackNumber)
                    val outputStream = getContentResolver().openOutputStream(data.data)
                    exportToGpx(outputStream, trackPoints)
                    outputStream.close()
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun initialize() {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val started = manager.getRunningServices(Integer.MAX_VALUE).firstOrNull({ SensorService::class.java.name == it.service.className } )!= null
        if (started)
            display()

        mainWebView.loadUrl("file:///android_asset/main.html")
    }

    private fun display() {
        val intent = Intent(this, DisplayActivity::class.java)
        startActivity(intent)
    }

    private fun checkPermissions(): Boolean {
        val permissionsList = ArrayList<String>()
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            permissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            permissionsList.add(Manifest.permission.READ_EXTERNAL_STORAGE)

        val permissions = permissionsList.toTypedArray()
        if (permissions.count() > 0) {
            requestPermissions(permissions, MainActivity.REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS)
            return false
        }
        return true
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initializeNavigationDrawer() {
        val navigationHeader = nav_view.getHeaderView(0)
        val navWebView = navigationHeader.findViewById<WebView>(R.id.navView)
        val navViewSettings = navWebView.settings
        navViewSettings.javaScriptEnabled = true
        navWebView.webChromeClient = WebChromeClient()
        navWebView.addJavascriptInterface(object {
            @JavascriptInterface
            fun doHapticFeedback() = doAsync { uiThread { mainWebView.playSoundEffect(SoundEffectConstants.CLICK) } }

            @JavascriptInterface
            fun fillTracks() = doAsync { uiThread {
                val dataSource = DataSource(this@MainActivity)
                val tracks = dataSource.getTracks().toList()

                val gson = Gson()
                val json = gson.toJson(tracks)

                navWebView.evaluateJavascript("onTracks($json)", null)
            } }

            @Suppress("DEPRECATION")
            @JavascriptInterface
            fun onTrackSelected(trackNumber: Long) {
                val dataSource = DataSource(this@MainActivity)
                val track = dataSource.getTrack(trackNumber)
                if (track != null)
                {
                    val date = Date(track.time)
                    val name = "${date.year + 1900}-${date.month + 1}-${date.date}-${date.hours}-${date.minutes}.gpx"

                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.type = "text/xml"
                    intent.putExtra(Intent.EXTRA_TITLE, name)
                    currentTrackNumber = trackNumber
                    startActivityForResult(intent, CREATE_REQUEST_CODE)
                }
            }

        }, "Native")
        navWebView.isHapticFeedbackEnabled = true
        navWebView.loadUrl("file:///android_asset/navigationbar.html")
    }

    private var currentTrackNumber = -1L

    companion object {
        val REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 1000
        private val CREATE_REQUEST_CODE = 40
    }
}
