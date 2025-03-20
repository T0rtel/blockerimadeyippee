package Services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.tortel.blockerimadeyippee.MainActivity
import com.tortel.blockerimadeyippee.MainActivity.Companion.bannerView
import com.tortel.blockerimadeyippee.MainActivity.Companion.isLocked
import com.tortel.blockerimadeyippee.MainActivity.Companion.timeLeft
import com.tortel.blockerimadeyippee.R

class CountDownService : Service() {

    private lateinit var notificationManager: NotificationManager
    private val notificationId = 1 // Unique ID for the notification
    private var countDownTimer: CountDownTimer? = null

    val defaultmillis : Long = 15 * 1000

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("CountDownService", "Service started , islocked : ${isLocked}")
        if (isLocked == false) return START_STICKY
        val millisInFuture = intent?.getLongExtra("millisInFuture", defaultmillis)
            ?: (defaultmillis) // Default to 15 minutes
        //val context = applicationContext

        // Start the countdown
        startCountdown(millisInFuture)

//        // Start the service in the foreground
//        val notification = createNotification("Timer Started", "Lockdown timer is running", true)
//        //startForeground(1, notification)
//        ServiceCompat.startForeground(this, 1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)

        return START_STICKY
    }

    private fun startCountdown(millisInFuture: Long) {
        Log.d("COUNTDOWN", "Starting countdown")
        countDownTimer = object : CountDownTimer(millisInFuture, 1000) {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = millisUntilFinished / 1000 % 60
                val timeLeft = String.format("%02d:%02d", minutes, seconds)
                MainActivity.timeLeft = timeLeft

                // Update the notification
                val notification = createNotification("Time Left", timeLeft, true)
                notificationManager.notify(notificationId, notification)
                //startForeground(1, notification)
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onFinish() {
                Log.d("LOCKDOWN", "Finished TIMER! isshown: ${bannerView.isShown} islocked: ${isLocked}")
                //val intent = Intent("com.tortel.testblocker3yay.TIMER_FINISHED")
                //sendBroadcast(intent)

                isLocked = false
                timeLeft = ""
                if (bannerView.isShown){
                    (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(bannerView)
                }


                // Notify that the timer has finished
                val notification = createNotification("Timer Finished", "Device unlocked!", false)
                notificationManager.notify(1, notification)

                stopForeground(STOP_FOREGROUND_REMOVE)

                Log.d("CountDownService", "Device unlocked!")
            }
        }.start()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(title: String, text: String, isOngoing: Boolean): Notification {
        return Notification.Builder(this, "timer_channel")
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(isOngoing)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "timer_channel", // Channel ID
                "Timer Notifications", // Channel name
                NotificationManager.IMPORTANCE_LOW // Importance level
            ).apply {
                description = "Shows the remaining time for the lock"
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    /*
    override fun onDestroy() {
        Log.d("CountDownService", "Service destroyed")
        super.onDestroy()
        //countDownTimer?.cancel() // Stop the countdown timer
    }
     */

}