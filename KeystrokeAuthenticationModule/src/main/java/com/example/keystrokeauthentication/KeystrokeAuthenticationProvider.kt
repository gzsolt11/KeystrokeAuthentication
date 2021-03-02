package com.example.keystrokeauthentication

import android.content.Context
import android.content.Intent
import android.util.Log

class KeystrokeAuthenticationProvider(var context: Context) {


    public fun makeLog(message:String){
        Log.v("KeystrokeAuthentication",message)
    }

    fun startBackgroundProcess(){
        var intent = Intent(context, BackgroundProcess::class.java).also {
            context.startService(it)
        }
        Log.v("KeystrokeAuthentication","background process started")
    }

    fun stopBackgroundProcess(){
        var intent = Intent(context, BackgroundProcess::class.java).also {
            context.stopService(it)
        }
        Log.v("KeystrokeAuthentication","background process stoppedBA")
    }
}