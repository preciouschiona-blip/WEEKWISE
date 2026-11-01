package com.weekwise.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.JavascriptInterface
import org.json.JSONObject

/** The functions the web screens can call. Only local app pages are ever loaded, so this is safe to expose. */
class Bridge(private val act: MainActivity) {

    @JavascriptInterface
    fun syncSchedule(json: String) {
        try {
            Schedule.sync(act, json)
        } catch (e: Exception) {
            // A bad list leaves the previous alarms in place.
        }
    }

    @JavascriptInterface
    fun pickContact() {
        act.runOnUiThread { act.pickContact() }
    }

    @JavascriptInterface
    fun requestNotifications() {
        act.runOnUiThread { act.askNotifications() }
    }

    @JavascriptInterface
    fun requestCalendar() {
        act.runOnUiThread { act.askCalendar() }
    }

    @JavascriptInterface
    fun permissionStatus(): String {
        return JSONObject()
            .put("notifications", act.hasNotifications())
            .put("calendar", act.hasCalendar())
            .toString()
    }

    @JavascriptInterface
    fun getCalendarEvents(start: String, end: String): String {
        return act.calendarEvents(start.toLong(), end.toLong())
    }

    @JavascriptInterface
    fun copyText(text: String) {
        act.runOnUiThread {
            val cm = act.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("WeekWise", text))
        }
    }
}
