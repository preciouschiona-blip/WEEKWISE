package com.weekwise.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import java.util.Calendar

/** One alarm or reminder. days uses Sunday = 0 ... Saturday = 6 (same as JavaScript). */
data class Entry(
    val id: String,
    val title: String,
    val text: String,
    val kind: String,
    val hour: Int,
    val minute: Int,
    val days: List<Int>,
    val date: String?
)

object Schedule {
    private const val PREFS = "weekwise"
    private const val KEY = "schedule"
    const val ACTION_FIRE = "com.weekwise.app.FIRE"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun parse(json: String): List<Entry> {
        val out = ArrayList<Entry>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val time = o.optString("time", "00:00").split(":")
            val daysArr = o.optJSONArray("days")
            val days = ArrayList<Int>()
            if (daysArr != null) {
                for (j in 0 until daysArr.length()) days.add(daysArr.getInt(j))
            }
            val date: String? = if (o.isNull("date")) null else o.optString("date")
            out.add(
                Entry(
                    id = o.getString("id"),
                    title = o.optString("title"),
                    text = o.optString("text"),
                    kind = o.optString("kind", "reminder"),
                    hour = time[0].toInt(),
                    minute = time[1].toInt(),
                    days = days,
                    date = date
                )
            )
        }
        return out
    }

    fun load(ctx: Context): List<Entry> {
        return try {
            parse(prefs(ctx).getString(KEY, "[]") ?: "[]")
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Replace everything: cancel the old alarms, store the new list, arm the next time of each. */
    fun sync(ctx: Context, json: String) {
        val fresh = parse(json)
        load(ctx).forEach { disarm(ctx, it.id) }
        prefs(ctx).edit().putString(KEY, json).apply()
        fresh.forEach { arm(ctx, it) }
    }

    fun rearmAll(ctx: Context) {
        load(ctx).forEach { arm(ctx, it) }
    }

    fun nextTrigger(e: Entry, now: Long): Long? {
        val dateStr = e.date
        if (dateStr != null) {
            val p = dateStr.split("-")
            val c = Calendar.getInstance()
            c.set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt(), e.hour, e.minute, 0)
            c.set(Calendar.MILLISECOND, 0)
            return if (c.timeInMillis > now) c.timeInMillis else null
        }
        if (e.days.isEmpty()) return null
        for (i in 0..7) {
            val d = Calendar.getInstance()
            d.timeInMillis = now
            d.add(Calendar.DAY_OF_YEAR, i)
            d.set(Calendar.HOUR_OF_DAY, e.hour)
            d.set(Calendar.MINUTE, e.minute)
            d.set(Calendar.SECOND, 0)
            d.set(Calendar.MILLISECOND, 0)
            val dow = d.get(Calendar.DAY_OF_WEEK) - 1
            if (e.days.contains(dow) && d.timeInMillis > now) return d.timeInMillis
        }
        return null
    }

    private fun pending(ctx: Context, id: String): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java).setAction(ACTION_FIRE).putExtra("id", id)
        return PendingIntent.getBroadcast(
            ctx,
            id.hashCode(),
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showIntent(ctx: Context): PendingIntent {
        return PendingIntent.getActivity(
            ctx,
            1,
            Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun arm(ctx: Context, e: Entry) {
        val t = nextTrigger(e, System.currentTimeMillis()) ?: return
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val p = pending(ctx, e.id)
        val canExact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (e.kind == "alarm" || !canExact) {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(t, showIntent(ctx)), p)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, p)
        }
    }

    fun disarm(ctx: Context, id: String) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(ctx, id))
    }

    fun snooze(ctx: Context, title: String, text: String, minutes: Int) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(ctx, AlarmReceiver::class.java)
            .setAction(ACTION_FIRE)
            .putExtra("snooze", true)
            .putExtra("title", title)
            .putExtra("text", text)
        val p = PendingIntent.getBroadcast(
            ctx,
            777001,
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val at = System.currentTimeMillis() + minutes * 60_000L
        am.setAlarmClock(AlarmManager.AlarmClockInfo(at, showIntent(ctx)), p)
    }
}
