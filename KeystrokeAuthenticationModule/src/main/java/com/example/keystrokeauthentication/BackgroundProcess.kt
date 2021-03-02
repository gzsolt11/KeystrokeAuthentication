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
        readFile()
        // if the server kills the service it will be recreated
        return START_REDELIVER_INTENT
    }

    fun readFile(){
        try{
            var fileReader = FileReader("keylog.csv")

            do{
                var c = fileReader.read()
                Log.v(TAG,c.toString())
            }while(c != -1)
        } catch (ex: Exception){
            print(ex.message)
        }


    }
}