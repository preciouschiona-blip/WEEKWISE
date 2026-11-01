package com.weekwise.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object Notify {
    const val CH_REMIND = "reminders"
    const val CH_ALARM = "alarms"

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)

        val reminders = NotificationChannel(CH_REMIND, "Reminders", NotificationManager.IMPORTANCE_HIGH)
        reminders.description = "Class, call, task and planning reminders"
        reminders.enableVibration(true)

        val alarms = NotificationChannel(CH_ALARM, "Alarms", NotificationManager.IMPORTANCE_HIGH)
        alarms.description = "Wake-up and work routine alarms"
        alarms.setSound(null, null)
        alarms.enableVibration(false)

        nm.createNotificationChannels(listOf(reminders, alarms))
    }

    fun post(ctx: Context, e: Entry) {
        ensureChannels(ctx)
        val open = PendingIntent.getActivity(
            ctx,
            1,
            Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(ctx, CH_REMIND)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(e.title)
            .setContentText(e.text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.notify(e.id.hashCode(), n)
    }
}
