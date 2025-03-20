package com.tortel.blockerimadeyippee.Reciever

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Admin permissions enabled", Toast.LENGTH_SHORT).show()
        Log.d("MyAdmin","Admin weehee!")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Admin permissions disabled", Toast.LENGTH_SHORT).show()
        Log.d("MyAdmin","Admin disabled nooo :(")
    }
}