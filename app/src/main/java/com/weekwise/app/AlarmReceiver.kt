package com.weekwise.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        // A snoozed alarm rings again with the same title.
        if (intent.getBooleanExtra("snooze", false)) {
            startRinging(
                ctx,
                intent.getStringExtra("title") ?: "Alarm",
                intent.getStringExtra("text") ?: ""
            )
            return
        }

        val id = intent.getStringExtra("id") ?: return
        val entry = Schedule.load(ctx).firstOrNull { it.id == id } ?: return

        if (entry.kind == "alarm") {
            startRinging(ctx, entry.title, entry.text)
        } else {
            Notify.post(ctx, entry)
        }

        // Weekly and daily entries arm their next time. One-time entries are finished.
        if (entry.date == null) Schedule.arm(ctx, entry)
    }

    private fun startRinging(ctx: Context, title: String, text: String) {
        val svc = Intent(ctx, RingService::class.java)
            .putExtra("title", title)
            .putExtra("text", text)
        ctx.startForegroundService(svc)
    }
}
