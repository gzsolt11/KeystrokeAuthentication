package com.example.keystrokeauthentication

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class BackgroundProcess: Service() {
    val TAG = "BACKGROUNDPROCESS"
    val TESTDATASIZE = 5
    val TRAINDATASIZE = 5
    var dataFrame = HashMap<Int, ArrayList<List<Int>>>()
    var positiveTrainData: MutableList<List<Int>> = ArrayList()
    var positiveTestData: MutableList<List<Int>> = ArrayList()
    var negativeTestData: MutableList<List<Int>> = ArrayList()
    var positiveTestScores: MutableList<Double> = ArrayList()
    var negativeTestScores: MutableList<Double> = ArrayList()
    var threshold: Double = 0.0
    lateinit var pressedTimestamps: ArrayList<Long>
    lateinit var releasedTimestamps: ArrayList<Long>
    lateinit var authenticationPressedTimestamps: ArrayList<Long>
    lateinit var authenticationReleasedTimestamps: ArrayList<Long>


    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        var extra = intent?.extras?.get("ID")
        if (extra == 0) {
            Log.v("VALASZ", "START")
            if(isApplicationTrained()) {
                Thread {
                    dataFrame = readFile()

                    val datas: ArrayList<MutableList<List<Int>>> = splitDataByUser(-1)

                    positiveTrainData = datas[0]
                    positiveTestData = datas[1]
                    negativeTestData = datas[2]
                    Log.v("TANITOTT TRAIN:",positiveTrainData.toString())
                    Log.v("TANITOTT TEST:",positiveTestData.toString())

                    positiveTestScores = manhattanScaledDistances(positiveTrainData, positiveTestData)
                    negativeTestScores = manhattanScaledDistances(positiveTrainData, negativeTestData)

                    val scores: ArrayList<MutableList<Double>> = ArrayList()
                    scores.add(positiveTestScores)
                    scores.add(negativeTestScores)

                    positiveTestScores = scores[0]
                    negativeTestScores = scores[1]

                    threshold = getThreshold(positiveTestScores, negativeTestScores)
                    Log.v("TANUL Threshold ", threshold.toString())
                }.start()
            }
        }
        if (extra == 1) {
            Log.v("VALASZ", "autentikacio")
            if(isApplicationTrained()) {
                if(positiveTrainData.size != 0){
                    Thread {
                        authenticationPressedTimestamps = intent?.extras?.get("pressedTimestamps") as ArrayList<Long>
                        authenticationReleasedTimestamps = intent?.extras?.get("releasedTimestamps") as ArrayList<Long>
                        val authenticationKeystrokes = convertTimestampsToKeystrokes(authenticationPressedTimestamps, authenticationReleasedTimestamps)
                        Log.v("AUTHENTICATION", authenticationKeystrokes.toString())
                        val authenticateResult = authenticate(authenticationKeystrokes)

                        val authIntent = Intent("SendResultToAuthenticationProvider")
                        authIntent.putExtra("authenticationResult",authenticateResult)
                        LocalBroadcastManager.getInstance(this.baseContext).sendBroadcast(authIntent)

                        Log.v("TANULAS EREDMENY", authenticateResult.toString())
                    }.start()
                }else{
                    Toast.makeText(applicationContext, "The service is not running!", Toast.LENGTH_SHORT).show()
                }
            }else{
                Toast.makeText(applicationContext, "The application is not trained!", Toast.LENGTH_SHORT).show()
            }

        }
        if (extra == 2) {
            Log.v("VALASZ", "TRAIN")
            Thread {
                pressedTimestamps = intent?.extras?.get("pressedTimestamps") as ArrayList<Long>
                releasedTimestamps = intent?.extras?.get("releasedTimestamps") as ArrayList<Long>
                Log.v("GOT LISTS", pressedTimestamps.toString())
                Log.v("GOT LISTS", releasedTimestamps.toString())

                var inputTrainData = convertTimestampsToKeystrokes(pressedTimestamps, releasedTimestamps)
                saveTrainDataToLocalSP(inputTrainData)

            }.start()
        }
        // if the server kills the service it will be recreated
        return START_REDELIVER_INTENT
    }

    fun readFile(): HashMap<Int, ArrayList<List<Int>>> {
        var dataFrame = HashMap<Int, ArrayList<List<Int>>>()
        applicationContext.resources.openRawResource(R.raw.keylogs).bufferedReader().use {
            var text2 = it.lineSequence()
            text2.iterator().forEach { it2 ->
                var elements: MutableList<String> = it2.split(",") as MutableList<String>
                if (elements[0].toIntOrNull() != null) {
                    var userId = elements[elements.size - 1].toInt()
                    elements.removeLast()
                    //Log.v("READIN",elements.toString())
                    var arrayList = dataFrame.get(userId)
                    if (arrayList == null) {
                        arrayList = ArrayList()
                    }
                    arrayList.add(elements.map { it -> it.toInt() })
                    dataFrame.put(userId, arrayList)
                }
            }
        }

        val sharedPreferences: SharedPreferences = getSharedPreferences("sharedPref", MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("trainData",null)
        if(json == null){
            Toast.makeText(applicationContext, "Application is not trained yet!", Toast.LENGTH_SHORT).show()
        }else{
            val type = object: TypeToken<ArrayList<List<Int>>>(){}.type
            val trainData:ArrayList<List<Int>> = gson.fromJson(json,type)
            Log.v("TANULNA",trainData.toString())
            dataFrame.put(-1, trainData)
        }


        return dataFrame
    }

    fun splitDataByUser(userId: Int): ArrayList<MutableList<List<Int>>> {
        val data = dataFrame[userId]
        val datas: ArrayList<MutableList<List<Int>>> = ArrayList()
        if (data != null) {
            var positiveTrainData = data.subList(0, TRAINDATASIZE)
            var positiveTestData = data.subList(5, TESTDATASIZE + TRAINDATASIZE)
            var negativeTestData: MutableList<List<Int>> = ArrayList<List<Int>>()

            for (key in dataFrame.keys) {
                if (key != userId) {
                    if (dataFrame[key] != null) {
                        negativeTestData.add(dataFrame[key]!![0])
                    }
                }
                if (negativeTestData.size == TESTDATASIZE) {
                    break
                }
            }

            datas.add(positiveTrainData)
            datas.add(positiveTestData)
            datas.add(negativeTestData)
        }
        return datas

    }

    fun manhattanScaledDistances(positiveTrainData: MutableList<List<Int>>, testData: MutableList<List<Int>>): MutableList<Double> {
        val meanVector = calculateMeanVector(positiveTrainData)
        val madVector = calculateMadVector(positiveTrainData, meanVector)

        //Log.v("TANULAS",positiveTrainData.toString())
        //Log.v("TANULAS",meanVector.toString())
        //Log.v("TANULAS",madVector.toString())

        var testScores: MutableList<Double> = ArrayList()

        for (i in 0 until testData.size) {
            var score = 0.0
            for (j in 0 until meanVector.size) {
                score += abs(testData[i][j] - meanVector[j]) / madVector[j]
            }
            testScores.add(score)
        }
        testScores = testScores.map { it -> 1 / (1 + it) }.toMutableList()

        return testScores
    }

    // atlag szamolasa minden oszlopra
    fun calculateMeanVector(positiveTrainData: MutableList<List<Int>>): MutableList<Double> {
        val meanVector: MutableList<Double> = ArrayList()
        var sum = 0.0
        for (i in 0..positiveTrainData[0].size - 1) {
            sum = 0.0
            for (k in 0 until positiveTrainData.size) {
                sum += positiveTrainData[k][i]
            }
            meanVector.add(sum / positiveTrainData.size)
        }
        return meanVector
    }

    // atlagtol valo elteres atlaga
    fun calculateMadVector(positiveTrainData: MutableList<List<Int>>, meanVector: MutableList<Double>): MutableList<Double> {
        val madVector: MutableList<Double> = ArrayList()
        var sum = 0.0
        for (i in 0 until positiveTrainData[0].size) {
            sum = 0.0
            for (k in 0 until positiveTrainData.size) {
                sum += abs(positiveTrainData[k][i] - meanVector[i])

            }
            madVector.add(sum / positiveTrainData.size)
        }
        return madVector
    }

    fun getThreshold(positiveTestScores: MutableList<Double>, negativeTestScores: MutableList<Double>): Double {
        val allScores = ArrayList(positiveTestScores)
        allScores.addAll(negativeTestScores)
        val minScore = allScores.reduce { acc, next -> min(acc, next) }
        val maxScore = allScores.reduce { acc, next -> max(acc, next) }
        val unit: Double = (maxScore - minScore) / (TESTDATASIZE + TRAINDATASIZE)

        val thresholds: MutableList<Double> = ArrayList()
        var threshold = minScore

        for (i in 0 until (TESTDATASIZE + TRAINDATASIZE)) {
            thresholds.add(threshold)
            threshold += unit
        }
        Log.v("THESHOLDS",thresholds.toString())

        var savedThreshold = 0.0
        var savedfn = positiveTestScores.size
        for (thresholdElement in thresholds) {
            val fp = negativeTestScores.filter { it -> it >= thresholdElement }.size
            val fn = positiveTestScores.filter { it -> it < thresholdElement }.size

            if ((fp == 0) and (fn < savedfn)) {
                savedThreshold = thresholdElement
                savedfn = fn
            }
        }
        return savedThreshold
    }

    fun authenticate(keylog: MutableList<List<Int>>): Boolean {
        val userScore = manhattanScaledDistances(positiveTrainData, keylog)
        Log.v("USERSCORE",userScore.toString())
        val count = userScore.filter { element -> element >= threshold }.size
        if (count == keylog.size && keylog.size != 0) {
            return true
        }
        return false
    }

    fun convertTimestampsToKeystrokes(pressedTimestamps: ArrayList<Long>, releasedTimestamps: ArrayList<Long>):ArrayList<List<Int>> {

        Log.v("TANUL", pressedTimestamps.toString())
        Log.v("TANUL", releasedTimestamps.toString())
        var inputtedTrainData: ArrayList<List<Int>> = ArrayList()
        var i = 0
        while (i < pressedTimestamps.size) {
            Log.v("TANUL", i.toString())
            val H1 = (releasedTimestamps[i] - pressedTimestamps[i]).toInt()
            val H2 = (releasedTimestamps[i + 1] - pressedTimestamps[i + 1]).toInt()
            val H3 = (releasedTimestamps[i + 2] - pressedTimestamps[i + 2]).toInt()
            val H4 = (releasedTimestamps[i + 3] - pressedTimestamps[i + 3]).toInt()
            val H5 = (releasedTimestamps[i + 4] - pressedTimestamps[i + 4]).toInt()
            val H6 = (releasedTimestamps[i + 5] - pressedTimestamps[i + 5]).toInt()

            val PP1 = (pressedTimestamps[i + 1] - pressedTimestamps[i]).toInt()
            val PP2 = (pressedTimestamps[i + 2] - pressedTimestamps[i + 1]).toInt()
            val PP3 = (pressedTimestamps[i + 3] - pressedTimestamps[i + 2]).toInt()
            val PP4 = (pressedTimestamps[i + 4] - pressedTimestamps[i + 3]).toInt()
            val PP5 = (pressedTimestamps[i + 5] - pressedTimestamps[i + 4]).toInt()

            val RP1 = (pressedTimestamps[i + 1] - releasedTimestamps[i]).toInt()
            val RP2 = (pressedTimestamps[i + 2] - releasedTimestamps[i + 1]).toInt()
            val RP3 = (pressedTimestamps[i + 3] - releasedTimestamps[i + 2]).toInt()
            val RP4 = (pressedTimestamps[i + 4] - releasedTimestamps[i + 3]).toInt()
            val RP5 = (pressedTimestamps[i + 5] - releasedTimestamps[i + 4]).toInt()

            inputtedTrainData.add(listOf(H1, H2, H3, H4, H5, H6, PP1, PP2, PP3, PP4, PP5, RP1, RP2, RP3, RP4, RP5))

            i += 6
        }

        return inputtedTrainData
    }

    private fun saveTrainDataToLocalSP(trainData: ArrayList<List<Int>>) {

        val sharedPreferences: SharedPreferences = getSharedPreferences("sharedPref", MODE_PRIVATE)
        Log.v("TANITOTT ADAT:",trainData.toString())
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(trainData)
        editor.putString("trainData",json)
        editor.commit()

    }

    private fun isApplicationTrained():Boolean{
        val sharedPreferences: SharedPreferences = getSharedPreferences("sharedPref", MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("trainData",null)
        if(json == null){
            Toast.makeText(applicationContext, "Application is not trained yet!", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

}