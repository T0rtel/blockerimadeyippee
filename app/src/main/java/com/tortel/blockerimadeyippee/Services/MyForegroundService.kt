package Services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tortel.testblocker3yay.R

class MyForegroundService : Service() {
    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Create a notification for the foreground service
        val notification = createNotification()
        startForeground(4, notification)

        return START_STICKY
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "timer_channeltwo")
            .setContentTitle("My App")
            .setContentText("Running in the background")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}