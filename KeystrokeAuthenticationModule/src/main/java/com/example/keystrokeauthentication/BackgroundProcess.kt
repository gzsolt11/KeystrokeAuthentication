package com.example.keystrokeauthentication

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.io.FileReader

class BackgroundProcess: Service() {
    val TAG = "BACKGROUNDPROCESS"

    init{
        Log.v(TAG, "Service is running")
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Thread {
            while (true) {
                readFile()
            }
        }.start()

        // if the server kills the service it will be recreated
        return START_REDELIVER_INTENT
    }

    fun readFile(){
        var text:String
        val fileText = applicationContext.resources.openRawResource(R.raw.keylogs).bufferedReader().use { text = it.readText() }

        Log.v("READFILE",text)


    }
}