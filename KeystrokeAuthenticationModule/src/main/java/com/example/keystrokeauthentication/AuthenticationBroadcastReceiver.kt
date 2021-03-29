package com.example.keystrokeauthentication


import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class AuthenticationBroadcastReceiver : BroadcastReceiver() {

    private val TAG = "AuthenticationBroadcastReceiver"
    private val TESTDATASIZE = 5
    private val TRAINDATASIZE = 5
    private var dataFrame = HashMap<Int, ArrayList<List<Int>>>()
    private var positiveTrainData: MutableList<List<Int>> = ArrayList()
    private var positiveTestData: MutableList<List<Int>> = ArrayList()
    private var negativeTestData: MutableList<List<Int>> = ArrayList()
    private var positiveTestScores: MutableList<Double> = ArrayList()
    private var negativeTestScores: MutableList<Double> = ArrayList()
    private var threshold: Double = 0.0
    private lateinit var pressedTimestamps: ArrayList<Long>
    private lateinit var releasedTimestamps: ArrayList<Long>
    private var broadcastContext: Context? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        val extra = intent?.extras?.get("ID")
        broadcastContext = context
        if(extra == 0){
            Log.v(TAG, "START")
            if(isApplicationTrained()) {
                Thread {
                    startingService()
                }.start()
            }
        }
        if(extra == 2){
            Log.v(TAG, "TRAIN")
            Thread {
                pressedTimestamps = intent.extras?.get("pressedTimestamps") as ArrayList<Long>
                releasedTimestamps = intent.extras?.get("releasedTimestamps") as ArrayList<Long>
                val inputTrainData = convertTimestampsToKeystrokes(pressedTimestamps, releasedTimestamps)
                saveTrainDataToLocalSP(inputTrainData)
                startingService()
            }.start()
        }
    }

    private fun startingService(){
        dataFrame = readFile()
        val datas: ArrayList<MutableList<List<Int>>> = splitDataByUser(-1)

        positiveTrainData = datas[0]
        positiveTestData = datas[1]
        negativeTestData = datas[2]

        positiveTestScores = manhattanScaledDistances(positiveTrainData, positiveTestData)
        negativeTestScores = manhattanScaledDistances(positiveTrainData, negativeTestData)

        val scores: ArrayList<MutableList<Double>> = ArrayList()
        scores.add(positiveTestScores)
        scores.add(negativeTestScores)

        positiveTestScores = scores[0]
        negativeTestScores = scores[1]

        threshold = getThreshold(positiveTestScores, negativeTestScores)
        Log.v(TAG, "Threshold: $threshold")
    }

    fun authenticate( authenticationPressedTimestamps:ArrayList<Long>, authenticationReleasedTimestamps:ArrayList<Long>):Boolean{
        if (isApplicationTrained()) {
            if(authenticationPressedTimestamps.size == 6) {
                if (positiveTrainData.size != 0) {
                    val authenticationKeystrokes = convertTimestampsToKeystrokes(authenticationPressedTimestamps, authenticationReleasedTimestamps)
                    return authenticateStrokes(authenticationKeystrokes)
                }
                Toast.makeText(broadcastContext, "The service is not running!", Toast.LENGTH_SHORT).show()
            } else{
                Toast.makeText(broadcastContext, "PIN is wrong!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(broadcastContext, "The application is not trained!", Toast.LENGTH_SHORT).show()
        }
        return false
    }

    private fun readFile(): HashMap<Int, ArrayList<List<Int>>> {
        val dataFrame = HashMap<Int, ArrayList<List<Int>>>()
        broadcastContext!!.resources.openRawResource(R.raw.keylogs).bufferedReader().use {
            val text2 = it.lineSequence()
            text2.iterator().forEach { it2 ->
                val elements: MutableList<String> = it2.split(",") as MutableList<String>
                if (elements[0].toIntOrNull() != null) {
                    val userId = elements[elements.size - 1].toInt()
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

        val sharedPreferences: SharedPreferences = broadcastContext!!.getSharedPreferences("sharedPref", Service.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("trainData", null)
        if(json == null){
            Toast.makeText(broadcastContext!!, "Application is not trained yet!", Toast.LENGTH_SHORT).show()
        }else{
            val type = object: TypeToken<ArrayList<List<Int>>>(){}.type
            val trainData:ArrayList<List<Int>> = gson.fromJson(json, type)
            dataFrame[-1] = trainData
        }
        return dataFrame
    }

    private fun splitDataByUser(userId: Int): ArrayList<MutableList<List<Int>>> {
        val data = dataFrame[userId]
        val datas: ArrayList<MutableList<List<Int>>> = ArrayList()
        if (data != null) {
            val positiveTrainData = data.subList(0, TRAINDATASIZE)
            val positiveTestData = data.subList(5, TESTDATASIZE + TRAINDATASIZE)
            val negativeTestData: MutableList<List<Int>> = ArrayList<List<Int>>()

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

    private fun manhattanScaledDistances(positiveTrainData: MutableList<List<Int>>, testData: MutableList<List<Int>>): MutableList<Double> {
        val meanVector = calculateMeanVector(positiveTrainData)
        val madVector = calculateMadVector(positiveTrainData, meanVector)
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
    private fun calculateMeanVector(positiveTrainData: MutableList<List<Int>>): MutableList<Double> {
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
    private fun calculateMadVector(positiveTrainData: MutableList<List<Int>>, meanVector: MutableList<Double>): MutableList<Double> {
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

    private fun getThreshold(positiveTestScores: MutableList<Double>, negativeTestScores: MutableList<Double>): Double {
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


    private fun isApplicationTrained():Boolean{
        val sharedPreferences: SharedPreferences = broadcastContext!!.getSharedPreferences("sharedPref", Service.MODE_PRIVATE)
        val json = sharedPreferences.getString("trainData", null)
        if(json == null){
            Toast.makeText(broadcastContext, "Application is not trained yet!", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun convertTimestampsToKeystrokes(pressedTimestamps: ArrayList<Long>, releasedTimestamps: ArrayList<Long>):ArrayList<List<Int>> {

        Log.v(TAG, "TANUL $pressedTimestamps.toString()")
        Log.v(TAG, "TANUL $releasedTimestamps.toString()")
        val inputtedTrainData: ArrayList<List<Int>> = ArrayList()
        var i = 0
        while (i < pressedTimestamps.size) {
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

        val sharedPreferences: SharedPreferences = broadcastContext!!.getSharedPreferences("sharedPref", Service.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(trainData)
        editor.putString("trainData", json)
        editor.apply()

    }

    private fun authenticateStrokes(keylog: MutableList<List<Int>>): Boolean {
        val userScore = manhattanScaledDistances(positiveTrainData, keylog)
        Log.v(TAG,"TRAINEDTRESHOLD: $threshold.toString()")
        Log.v(TAG, "USERSCORE: $userScore.toString()")
        val count = userScore.filter { element -> element >= threshold }.size
        if (count == keylog.size && keylog.size != 0) {
            return true
        }
        return false
    }
}