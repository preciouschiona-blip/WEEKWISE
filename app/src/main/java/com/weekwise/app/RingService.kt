package com.weekwise.app

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator

/** Rings like an alarm clock until the person taps "I'm up" or snoozes. Stops by itself after 2 minutes. */
class RingService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        const val ACTION_STOP = "com.weekwise.app.STOP"
        const val ACTION_SNOOZE = "com.weekwise.app.SNOOZE"
        const val NOTIF_ID = 4242
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra("title") ?: "Alarm"
        val text = intent?.getStringExtra("text") ?: ""

        when (intent?.action) {
            ACTION_STOP -> {
                stopRinging()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                Schedule.snooze(this, title, text, 5)
                stopRinging()
                return START_NOT_STICKY
            }
        }

        Notify.ensureChannels(this)
        val piFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val open = PendingIntent.getActivity(this, 2, Intent(this, MainActivity::class.java), piFlags)
        val stop = PendingIntent.getService(
            this, 3,
            Intent(this, RingService::class.java).setAction(ACTION_STOP),
            piFlags
        )
        val snooze = PendingIntent.getService(
            this, 4,
            Intent(this, RingService::class.java)
                .setAction(ACTION_SNOOZE)
                .putExtra("title", title)
                .putExtra("text", text),
            piFlags
        )

        val n = Notification.Builder(this, Notify.CH_ALARM)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(if (text.isEmpty()) "Alarm" else text)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_ALARM)
            .addAction(Notification.Action.Builder(0, "I'm up", stop).build())
            .addAction(Notification.Action.Builder(0, "Snooze 5 min", snooze).build())
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIF_ID, n)
        }

        startSound()
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ stopRinging() }, 120_000L)
        return START_NOT_STICKY
    }

    private fun startSound() {
        releasePlayer()
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setDataSource(this, uri)
            mp.isLooping = true
            mp.prepare()
            mp.start()
            player = mp
        } catch (e: Exception) {
            // No sound available. The notification and vibration still work.
        }
        try {
            @Suppress("DEPRECATION")
            val v = getSystemService(VIBRATOR_SERVICE) as Vibrator
            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0))
            vibrator = v
        } catch (e: Exception) {
        }
    }

    private fun releasePlayer() {
        try {
            player?.stop()
        } catch (e: Exception) {
        }
        player?.release()
        player = null
    }

    private fun stopRinging() {
        handler.removeCallbacksAndMessages(null)
        releasePlayer()
        vibrator?.cancel()
        vibrator = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        releasePlayer()
        vibrator?.cancel()
        super.onDestroy()
    }
}
