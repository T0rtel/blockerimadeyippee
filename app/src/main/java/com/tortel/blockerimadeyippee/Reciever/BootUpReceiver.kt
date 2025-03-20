package com.tortel.blockerimadeyippee.Reciever

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

import com.tortel.blockerimadeyippee.MainActivity



class BootUpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("RESTART DETECTOR","detected restart, islockdown : ${ MainActivity.isLocked} , lockdown timer : ${ MainActivity.isLocked}")

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            MainActivity.timeLeft = ""
            MainActivity.isLocked = false
        }

        Log.d("RESTART DETECTOR", "after restart :islockdown : ${MainActivity.isLocked} , lockdown timer : ${MainActivity.timeLeft}")

    }
}