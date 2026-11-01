package com.weekwise.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** After a restart or an app update, put every alarm back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Notify.ensureChannels(ctx)
        Schedule.rearmAll(ctx)
    }
}
