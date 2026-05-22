package com.weatherbar.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import kotlin.math.roundToInt

class WeatherService : Service() {

    private val CHANNEL_ID = "weather_channel"
    private val NOTIFICATION_ID = 1
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("⏳ Yükleniyor...", "Hava durumu alınıyor"))

        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val intervalMs = prefs.getInt(MainActivity.KEY_INTERVAL, 30) * 60 * 1000L

        updateRunnable = object : Runnable {
            override fun run() {
                Thread {
                    val result = WeatherFetcher(this@WeatherService).fetchWeather()
                    val unit = prefs.getString(MainActivity.KEY_UNIT, "C") ?: "C"
                    handler.post {
                        if (result != null) {
                            val temp = result.temperature.roundToInt()
                            val title = "${result.icon} $temp°$unit"
                            val sub = result.description
                            updateNotification(title, sub)
                            prefs.edit()
                                .putString("last_weather_text", title)
                                .putString("last_weather_desc", sub)
                                .apply()
                        } else {
                            updateNotification("⚠️ Veri alınamadı", "Lütfen ayarları kontrol edin")
                        }
                    }
                }.start()
                handler.postDelayed(this, intervalMs)
            }
        }
        handler.post(updateRunnable!!)
        return START_STICKY
    }

    override fun onDestroy() {
        updateRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Hava Durumu", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Anlık hava durumu bildirim çubuğu"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, buildNotification(title, content))
    }
}
