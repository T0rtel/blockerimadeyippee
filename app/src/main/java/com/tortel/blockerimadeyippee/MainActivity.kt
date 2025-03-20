package com.tortel.blockerimadeyippee

import Services.AppMonitorService
import Services.MyAccessibilityService.Companion.currentApp
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.tortel.blockerimadeyippee.Reciever.MyDeviceAdminReceiver
import com.tortel.blockerimadeyippee.ui.theme.BlockerIMadeYippeeTheme
import kotlinx.coroutines.Job
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var componentName: ComponentName
    private var countDownTimer: CountDownTimer? = null
    private var countdownJob: Job? = null

    companion object {
        var appControl = true
        var isLocked: Boolean = false
        var timeLeft: String = ""
        lateinit var bannerView : View
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("MissingInflatedId")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(this, MyDeviceAdminReceiver::class.java)

        createNotificationChannel()

        //startLOCKDOWN
        if (intent.getBooleanExtra("startlockdown", false)) {
            Log.d("LOCKDOWN","RECIEVED INTENT TO START LOCKDOWN")
            LockDownStarted(intent.getLongExtra("millisInFuture", LockDownTime))

            val noti = MakeNotification("WANNA SCROLL?", "${intent.getStringExtra("appnword")}", false, true)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(2, noti)

            Log.d("LOCKDOWN", "FINISHED LOCKDOWN SETUP, should be running now")
        }

        //ALL BUTTON EVENTS
        setupAllButtons()
    }

    private fun LockDownStarted(millisInFuture : Long) {
        Log.d("LOCKDOWN", "LockDown started function")
        if (isLocked == true) return

        isLocked = true
        timeLeft = ""
        startCountdown(millisInFuture)
        startOverlay()
    }
    fun LockDownStopped() {
        Log.d("LOCKDOWN", "lockdown DONE!")
        isLocked = false
        timeLeft = ""
        currentApp = ""

        try {
            windowManager.removeView(bannerView)
        }catch (e : Exception){
            Log.w("LOCKDOWN", "failed to remove banner view , cause : $e")
        }

    }

    private fun startCountdown(millisInFuture: Long) {
        Log.d("LOCKDOWN", "start countdown service")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = 1 // Unique ID for the notification
        var isfirsttime = true
        var notification : Any?

        countDownTimer = object : CountDownTimer(millisInFuture, 1000) {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = millisUntilFinished / 1000 % 60
                val timeleft = String.format("%02d:%02d", minutes, seconds)
                timeLeft = timeleft


                if (isfirsttime){
                    notification = MakeNotification("Time Left", timeLeft, true, true)
                }else{
                    notification = MakeNotification("Time Left", timeLeft, true, false)
                }

                notificationManager.notify(notificationId, notification as Notification)
                isfirsttime = false
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onFinish() {
                LockDownStopped()

                val notification = MakeNotification("Timer Finished", "Device unlocked!", false, true)
                notificationManager.notify(notificationId, notification)

                Log.d("LOCKDOWN", "Finished TIMER! isshown: ${bannerView.isShown} islocked: ${isLocked}")
            }
        }.start()


        //        val intent = Intent(this, CountDownService::class.java).apply {
//            putExtra("millisInFuture", millisInFuture)
//        }
//
        //startService(intent)



    }

    private fun startOverlay() {
        Log.d("LOCKDOWN", "start overlay")
        //windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
        }

        bannerView = LayoutInflater.from(this).inflate(R.layout.banner_layout, null)
        windowManager.addView(bannerView, layoutParams)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun MakeNotification(title: String, text: String, isOngoing : Boolean, makesSound : Boolean): Notification {
        var channel = ""
        if (makesSound){
            channel = "timer_channeltwo"
        }else{
            channel = "timer_channel"
        }
        val notification = Notification.Builder(this@MainActivity, channel)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(isOngoing)
            .build()
        return notification
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

            val channeltwo = NotificationChannel(
                "timer_channeltwo", // Channel ID
                "Status Notification", // Channel name
                NotificationManager.IMPORTANCE_HIGH // Importance level
            ).apply {
                description = "Shows Whether the app is working or not"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(channeltwo)
        }
    }

    private fun setupAllButtons(){
        val enableAccessibilityButton: Button = findViewById(R.id.enable_accessibility_button)
        enableAccessibilityButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        val enableAdminButton: Button = findViewById(R.id.ask_for_admin)
        //!devicePolicyManager.isAdminActive(componentName)
        enableAdminButton.setOnClickListener {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                ComponentName(this@MainActivity, MyDeviceAdminReceiver::class.java)
            )
            intent.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Enable admin permissions to use all features of this app."
            )
            startActivity(intent)
            Log.d(
                "MyAdmin",
                "Asked device for admin, current admin status: ${
                    devicePolicyManager.isAdminActive(
                        componentName
                    )
                }"
            )
        }

        val foregroundservice: Button = findViewById(R.id.foreground)
        foregroundservice.setOnClickListener {
            if (!Settings.canDrawOverlays(this@MainActivity)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                )
                startActivity(intent)

            }
            Log.d(
                "ForegroundService",
                "Asked for foreground service , status : ${Settings.canDrawOverlays(this@MainActivity)}"
            )
        }

        val feelingdownbtn: Button = findViewById(R.id.feeling_down)
        feelingdownbtn.setOnClickListener {
            LockDownStarted(Random.nextInt(15 , 101) * 60 * 1000L)
        }

        // Check if battery optimization is enabled
        if (isBatteryOptimizationEnabled(this@MainActivity)) {
            // Show a dialog to the user
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Disable Battery Optimization")
                .setMessage("To ensure the app works correctly, please disable battery optimization for this app.")
                .setPositiveButton("Go to Settings") { _, _ ->
                    // Open battery optimization settings
                    requestDisableBatteryOptimization(this@MainActivity)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
    fun requestDisableBatteryOptimization(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:${context.packageName}")
            context.startActivity(intent)
        }
    }

    private fun isBatteryOptimizationEnabled(context: Context): Boolean {
        val powerManager = ContextCompat.getSystemService(context, PowerManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            !powerManager?.isIgnoringBatteryOptimizations(context.packageName)!!
        } else {
            false // Battery optimization is not available below Android 6.0
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }


    override fun onPause() {
        super.onPause()

        if (isLocked) {
            startService(Intent(this, AppMonitorService::class.java))
        }

        /*
        if (bannerView.isVisible && !isLocked){
            windowManager.removeView(bannerView)
        }
         */




    }

    override fun onDestroy() {
        super.onDestroy()
        //startTimer()

        countdownJob?.cancel()

        if (isLocked) {
            startService(Intent(this, AppMonitorService::class.java))
        }
    }
}