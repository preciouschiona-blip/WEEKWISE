package com.weekwise.app

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    lateinit var web: WebView
    private val reqContact = 11
    private val reqNotif = 12
    private val reqCal = 13
    private var notifFromButton = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notify.ensureChannels(this)

        web = WebView(this)
        setContentView(web)

        val s = web.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.allowContentAccess = false

        web.addJavascriptInterface(Bridge(this), "Android")
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.scheme == "file") return false
                try {
                    val i = if (uri.scheme == "tel") {
                        Intent(Intent.ACTION_DIAL, uri)
                    } else {
                        Intent(Intent.ACTION_VIEW, uri)
                    }
                    startActivity(i)
                } catch (e: Exception) {
                    // No app can open this link.
                }
                return true
            }
        }
        web.loadUrl("file:///android_asset/index.html")

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), reqNotif)
        }
    }

    fun js(code: String) {
        runOnUiThread { web.evaluateJavascript(code, null) }
    }

    // ---------- permissions ----------

    fun hasNotifications(): Boolean {
        val nm = getSystemService(NotificationManager::class.java)
        return nm.areNotificationsEnabled()
    }

    fun hasCalendar(): Boolean {
        return checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }

    fun askNotifications() {
        notifFromButton = true
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), reqNotif)
        } else {
            openNotificationSettings()
        }
    }

    fun askCalendar() {
        requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), reqCal)
    }

    private fun openNotificationSettings() {
        val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        startActivity(i)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val denied = grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED
        if (requestCode == reqNotif && denied && notifFromButton) {
            openNotificationSettings()
        }
        notifFromButton = false
        js("window.onPermissionResult&&window.onPermissionResult()")
    }

    // ---------- contacts ----------

    fun pickContact() {
        val i = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        startActivityForResult(i, reqContact)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != reqContact || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        val cols = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        contentResolver.query(uri, cols, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val name = c.getString(0) ?: ""
                val number = c.getString(1) ?: ""
                js("window.onContactPicked&&window.onContactPicked(" +
                        JSONObject.quote(name) + "," + JSONObject.quote(number) + ")")
            }
        }
    }

    // ---------- calendar ----------

    fun calendarEvents(start: Long, end: Long): String {
        val arr = JSONArray()
        if (!hasCalendar()) return arr.toString()
        val b = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(b, start)
        ContentUris.appendId(b, end)
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY
        )
        contentResolver.query(
            b.build(), projection, null, null,
            CalendarContract.Instances.BEGIN + " ASC"
        )?.use { c ->
            while (c.moveToNext()) {
                arr.put(
                    JSONObject()
                        .put("title", c.getString(0) ?: "")
                        .put("begin", c.getLong(1))
                        .put("end", c.getLong(2))
                        .put("allDay", c.getInt(3) == 1)
                )
            }
        }
        return arr.toString()
    }

    // ---------- back button ----------

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        web.evaluateJavascript("(window.__back&&window.__back())||false") { result ->
            if (result != "true") {
                super@MainActivity.onBackPressed()
            }
        }
    }
}
