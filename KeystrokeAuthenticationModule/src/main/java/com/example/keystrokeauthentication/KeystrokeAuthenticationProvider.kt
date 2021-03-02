package com.example.keystrokeauthentication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class KeystrokeAuthenticationProvider: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    public fun makeLog(message:String){
        Log.v("KeystrokeAuthentication",message)
        startBackgroundProcess()
        stopBackgroundProcess()
    }

    fun startBackgroundProcess(){
        var intent = Intent(this, BackgroundProcess::class.java).also {
            startService(it)
        }
        Log.v("KeystrokeAuthentication","background process started")
    }

    fun stopBackgroundProcess(){
        var intent = Intent(this, BackgroundProcess::class.java).also {
            stopService(it)
        }
        Log.v("KeystrokeAuthentication","background process stopped")
    }
}