package Services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.tortel.blockerimadeyippee.MainActivity


class AppMonitorService : Service() {


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
       Log.d("MONITORSERVICE","detected cant leave app, islockdown : ${MainActivity.isLocked} , lockdown timer : '${MainActivity.timeLeft}'")
        if (MainActivity.timeLeft == "00:00" || MainActivity.timeLeft == "" || MainActivity.timeLeft == null){
            MainActivity.isLocked = false
            MainActivity.timeLeft = ""

            stopSelf()
        }
        // open the app again
        val appIntent = Intent(this, MainActivity::class.java)
        appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(appIntent)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}