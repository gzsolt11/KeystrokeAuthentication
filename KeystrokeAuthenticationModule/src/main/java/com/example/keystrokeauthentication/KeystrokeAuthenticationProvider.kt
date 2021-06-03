package com.example.keystrokeauthentication

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.slider.Slider
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries


class KeystrokeAuthenticationProvider(var activityContext: Context, var editText: EditText) {

    private val TRAINCOUNTER = 20
    private var key0: TextView
    private var key1: TextView
    private var key2: TextView
    private var key3: TextView
    private var key4: TextView
    private var key5: TextView
    private var key6: TextView
    private var key7: TextView
    private var key8: TextView
    private var key9: TextView
    private var keyOk: ImageView
    private var keyDel: ImageView
    private lateinit var trainkey0: TextView
    private lateinit var trainkey1: TextView
    private lateinit var trainkey2: TextView
    private lateinit var trainkey3: TextView
    private lateinit var trainkey4: TextView
    private lateinit var trainkey5: TextView
    private lateinit var trainkey6: TextView
    private lateinit var trainkey7: TextView
    private lateinit var trainkey8: TextView
    private lateinit var trainkey9: TextView
    private lateinit var trainkeyOk: ImageView
    private lateinit var trainkeyDel: ImageView
    private lateinit var trainEditText: EditText
    private lateinit var trainPinTitle: TextView
    private lateinit var inputMethodManager:InputMethodManager
    private lateinit var layoutInflater:LayoutInflater
    private  var temporarPressedTimestamps:ArrayList<Long> = ArrayList()
    private var temporarReleasedTimeStamps:ArrayList<Long> = ArrayList()
    private  var pressedTimestamps:ArrayList<Long> = ArrayList()
    private var releasedTimeStamps:ArrayList<Long> = ArrayList()
    private  var authenticatePressedTimestamps:ArrayList<Long> = ArrayList()
    private var authenticateReleasedTimestamps:ArrayList<Long> = ArrayList()
    private  var incorrectPressedTimestamps:ArrayList<Long> = ArrayList()
    private var incorrectReleasedTimestamps:ArrayList<Long> = ArrayList()
    private var featureModell:MutableList<Double> = ArrayList()
    private var trainCounterLeft = TRAINCOUNTER
    private var keyArray = ArrayList<TextView>()
    private var trainKeyArray = ArrayList<TextView>()
    private lateinit var keyboardPopUp:PopupWindow
    private lateinit var trainPopUp:PopupWindow
    private lateinit var deviationPopUp:PopupWindow
    private var isClicked = false
    private val sharedPreferences: SharedPreferences = activityContext.getSharedPreferences("sharedPref", Service.MODE_PRIVATE)
    private var authenticationBroadcastReceiver: AuthenticationBroadcastReceiver
    private var isServiceRunning = false
    private lateinit var graph:GraphView
    private var retry = false
    private var series: LineGraphSeries<DataPoint> = LineGraphSeries()
    private lateinit var keyboardLayout: ConstraintLayout
    private var threshold:Double = 0.0
    private var easyThreshold:Double = 0.0
    private var hardThreshold:Double = 0.0
    private var color = ""


    init{
        //activityContext.setTheme(R.style.Theme_AppCompat_Light_NoActionBar)
        ruleEditText(editText)

        key0 = keyboardPopUp.contentView.findViewById(R.id.buttonNumber0)
        key1 = keyboardPopUp.contentView.findViewById(R.id.buttonNumber1)
        key2 = keyboardPopUp.contentView.findViewById(R.id.buttonNumber2)
        key3 = keyboardPopUp.contentView.findViewById(R.id.buttonNumber3)
        key4 = keyboardPopUp.contentView.findViewById(R.id.buttonNumber4)
        key5 = keyboardPopUp.contentView.findViewById(R.id.buttonNumber5)
        key6 = keyboardPopUp.contentView.findViewById(R.id.buttonNumber6)
        key7 = keyboardPopUp.contentView.findViewById(R.id.buttonNumber7)
        key8 = keyboardPopUp.contentView.findViewById(R.id.buttonNumber8)
        key9 = keyboardPopUp.contentView.findViewById(R.id.buttonNumber9)
        keyArray.add(key0)
        keyArray.add(key1)
        keyArray.add(key2)
        keyArray.add(key3)
        keyArray.add(key4)
        keyArray.add(key5)
        keyArray.add(key6)
        keyArray.add(key7)
        keyArray.add(key8)
        keyArray.add(key9)
        keyOk = keyboardPopUp.contentView.findViewById(R.id.buttonOK)
        keyDel = keyboardPopUp.contentView.findViewById(R.id.buttonDelete)
        keyboardLayout = keyboardPopUp.contentView.findViewById(R.id.keyboardLayout)

        authenticationBroadcastReceiver = AuthenticationBroadcastReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction("StartBackgroundReceiver")
        LocalBroadcastManager.getInstance(activityContext).registerReceiver(
                authenticationBroadcastReceiver,
                intentFilter
        )

        if(sharedPreferences.contains("R")){
            setKeyboardColor(sharedPreferences.getInt("R",1), sharedPreferences.getInt("G",1),sharedPreferences.getInt("B",1))
        }

        startBackgroundProcess()

        setListenersToKeys()

    }


    private fun trainApplication(
            pressedTimestamps: ArrayList<Long>,
            releasedTimestamps: ArrayList<Long>
    ){
        val intent = Intent("StartBackgroundReceiver")
        intent.putExtra("ID", 2)
        intent.putExtra("pressedTimestamps", pressedTimestamps)
        intent.putExtra("releasedTimestamps", releasedTimestamps)
        LocalBroadcastManager.getInstance(activityContext).sendBroadcast(intent)
    }

    fun startBackgroundProcess(){
        val json = sharedPreferences.getString("trainData", null)
        if(json == null){
            Handler(Looper.getMainLooper()).postDelayed({
                train()
            }, 100)
        }
        val intent = Intent("StartBackgroundReceiver")
        intent.putExtra("ID", 0)
        LocalBroadcastManager.getInstance(activityContext).sendBroadcast(intent)
        isServiceRunning = true

        if(sharedPreferences.contains("R")){
            setKeyboardColor(sharedPreferences.getInt("R",1), sharedPreferences.getInt("G",1),sharedPreferences.getInt("B",1))
        }

    }

    fun authenticate(): Boolean{
        if(isServiceRunning) {
            val authenticationResult = authenticationBroadcastReceiver.authenticate(
                    authenticatePressedTimestamps,
                    authenticateReleasedTimestamps
            )
            editText.setText("")
            authenticatePressedTimestamps = ArrayList()
            authenticateReleasedTimestamps = ArrayList()
            return authenticationResult
        }
        editText.setText("")
        return false
    }


    fun stopBackgroundProcess(){
        LocalBroadcastManager.getInstance(activityContext).unregisterReceiver(
                authenticationBroadcastReceiver
        )
        isServiceRunning = false
    }

    private fun ruleEditText(editText: EditText){
        layoutInflater = activityContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        keyboardPopUp = PopupWindow(
                layoutInflater.inflate(R.layout.keyboard2, null, false),
                Resources.getSystem().displayMetrics.widthPixels,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        )

        inputMethodManager = activityContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(editText.rootView.findViewById(R.id.content), 0)
        editText.isFocusable = false
        editText.setOnClickListener{ view ->
            keyboardPopUp.showAtLocation(view.rootView, Gravity.BOTTOM, 0, 0)
        }
    }

    fun train(){
        pressedTimestamps = ArrayList()
        releasedTimeStamps = ArrayList()
        incorrectPressedTimestamps = ArrayList()
        incorrectReleasedTimestamps = ArrayList()
        featureModell = ArrayList()
        retry = false

        series = LineGraphSeries()

        trainCounterLeft = TRAINCOUNTER

        trainPopUp = PopupWindow(
                layoutInflater.inflate(R.layout.trainingpincode, null, false),
                Resources.getSystem().displayMetrics.widthPixels,
                editText.rootView.height,
                true
        )
        trainPopUp.showAtLocation(editText.rootView, Gravity.TOP, 0, 0)

        trainPinTitle = trainPopUp.contentView.findViewById(R.id.trainPinCodeTitle)
        trainPinTitle.text = "Write your pincode " + trainCounterLeft + " times"

        trainEditText = trainPopUp.contentView.findViewById<EditText>(R.id.trainPinCodeEditText)
        inputMethodManager.hideSoftInputFromWindow(
                trainEditText.rootView.findViewById(R.id.content),
                0
        )
        trainEditText.isFocusable = false

        graph = trainPopUp.contentView.findViewById(R.id.graphView)
        graph.gridLabelRenderer.gridColor = Color.BLACK
        graph.gridLabelRenderer.verticalLabelsColor = Color.BLACK
        graph.gridLabelRenderer.horizontalLabelsColor = Color.BLACK


        trainkey0 = trainPopUp.contentView.findViewById(R.id.buttonNumber0)
        trainkey1 = trainPopUp.contentView.findViewById(R.id.buttonNumber1)
        trainkey2 = trainPopUp.contentView.findViewById(R.id.buttonNumber2)
        trainkey3 = trainPopUp.contentView.findViewById(R.id.buttonNumber3)
        trainkey4 = trainPopUp.contentView.findViewById(R.id.buttonNumber4)
        trainkey5 = trainPopUp.contentView.findViewById(R.id.buttonNumber5)
        trainkey6 = trainPopUp.contentView.findViewById(R.id.buttonNumber6)
        trainkey7 = trainPopUp.contentView.findViewById(R.id.buttonNumber7)
        trainkey8 = trainPopUp.contentView.findViewById(R.id.buttonNumber8)
        trainkey9 = trainPopUp.contentView.findViewById(R.id.buttonNumber9)
        trainkeyOk = trainPopUp.contentView.findViewById(R.id.buttonOK)
        trainkeyDel = trainPopUp.contentView.findViewById(R.id.buttonDelete)
        trainKeyArray.add(trainkey0)
        trainKeyArray.add(trainkey1)
        trainKeyArray.add(trainkey2)
        trainKeyArray.add(trainkey3)
        trainKeyArray.add(trainkey4)
        trainKeyArray.add(trainkey5)
        trainKeyArray.add(trainkey6)
        trainKeyArray.add(trainkey7)
        trainKeyArray.add(trainkey8)
        trainKeyArray.add(trainkey9)

        if(sharedPreferences.contains("R")){
            setTrainKeyboardColor(sharedPreferences.getInt("R",1), sharedPreferences.getInt("G",1),sharedPreferences.getInt("B",1))
        }

        setListenersToTrainKeys()
    }


    private fun setListenersToKeys(){
        for(key in keyArray){
            key.setOnClickListener{ _ ->
                if(editText.text.length < 6) {
                    editText.setText(editText.text.toString() + "*")
                }
            }
            key.setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(view: View?, event: MotionEvent?): Boolean {
                    if (authenticateReleasedTimestamps.size < 6) {
                        if (event?.action == MotionEvent.ACTION_DOWN && !isClicked) {
                            isClicked = true
                            authenticatePressedTimestamps.add(event.eventTime)
                        }
                        if (event?.action == MotionEvent.ACTION_UP && isClicked) {
                            isClicked = false
                            authenticateReleasedTimestamps.add(event.eventTime)
                        }
                    }

                    return view?.onTouchEvent(event) ?: true
                }
            })
        }

        keyOk.setOnClickListener{ _ ->
            keyboardPopUp.dismiss()
        }


        keyDel.setOnClickListener{ _ ->
            editText.setText("")
            authenticatePressedTimestamps = ArrayList()
            authenticateReleasedTimestamps = ArrayList()
        }
    }

    private fun setListenersToTrainKeys(){
        for(key in trainKeyArray){
            key.setOnClickListener{ _ ->
                if(trainEditText.text.length < 6) {
                    trainEditText.setText(trainEditText.text.toString() + "*")
                }
            }
            key.setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(view: View?, event: MotionEvent?): Boolean {
                    if (temporarReleasedTimeStamps.size < 6) {
                        if (event?.action == MotionEvent.ACTION_DOWN && !isClicked) {
                            temporarPressedTimestamps.add(event.eventTime)
                            isClicked = true
                        }
                        if (event?.action == MotionEvent.ACTION_UP && isClicked) {
                            temporarReleasedTimeStamps.add(event.eventTime)
                            isClicked = false
                        }
                    }
                    return view?.onTouchEvent(event) ?: true
                }
            })
        }

        trainkeyOk.setOnClickListener{ _ ->
            if(trainEditText.text.length == 6) {

                var meanFeatures:MutableList<Double> = arrayListOf()
                var testFeatures:List<Int> = arrayListOf()
                var trainFeatures:ArrayList<List<Int>> = ArrayList<List<Int>>()

                if(releasedTimeStamps.size == 0){
                    trainFeatures = convertTimestampsToKeystrokes(
                            temporarPressedTimestamps,
                            temporarReleasedTimeStamps
                    )
                    meanFeatures = calculateMeanVector(trainFeatures)
                }else {
                    trainFeatures = convertTimestampsToKeystrokes(
                            pressedTimestamps,
                            releasedTimeStamps
                    )
                    testFeatures = convertTimestampsToKeystrokes(
                            temporarPressedTimestamps,
                            temporarReleasedTimeStamps
                    )[0]
                    meanFeatures = calculateMeanVector(trainFeatures)

                }


                var failCounter = 0
                if(TRAINCOUNTER-trainCounterLeft > 0) {
                    for (i in 0 until meanFeatures.size) {
                        if (testFeatures[i] > (meanFeatures[i] * 2) || testFeatures[i] < (meanFeatures[i] / 2)) {
                            failCounter = 1
                        }
                    }
                }

                if(failCounter == 1 && !retry){
                    incorrectReleasedTimestamps.addAll(temporarReleasedTimeStamps)
                    incorrectPressedTimestamps.addAll(temporarPressedTimestamps)
                }else{
                    if(retry){
                        pressedTimestamps.addAll(temporarPressedTimestamps)
                        releasedTimeStamps.addAll(temporarReleasedTimeStamps)
                        incorrectPressedTimestamps = ArrayList(
                                incorrectPressedTimestamps.subList(
                                        6,
                                        incorrectPressedTimestamps.size
                                )
                        )
                        incorrectReleasedTimestamps = ArrayList(
                                incorrectReleasedTimestamps.subList(
                                        6,
                                        incorrectReleasedTimestamps.size
                                )
                        )
                    } else {
                        pressedTimestamps.addAll(temporarPressedTimestamps)
                        releasedTimeStamps.addAll(temporarReleasedTimeStamps)
                    }
                }

                if((incorrectReleasedTimestamps.size/6) == ((releasedTimeStamps.size/6) + 3)){
                    var tempPressed = ArrayList<Long>()
                    var tempReleased = ArrayList<Long>()
                    tempPressed.addAll(incorrectPressedTimestamps)
                    tempReleased.addAll(incorrectReleasedTimestamps)
                    incorrectPressedTimestamps = pressedTimestamps
                    incorrectReleasedTimestamps = releasedTimeStamps
                    pressedTimestamps = tempPressed
                    releasedTimeStamps = tempReleased
                }


                temporarPressedTimestamps = ArrayList()
                temporarReleasedTimeStamps = ArrayList()
                trainEditText.setText("")
                trainCounterLeft -= 1
                trainPinTitle.text = "Write your pincode " + trainCounterLeft + " times"

                graph.removeAllSeries()
                graph.viewport.isXAxisBoundsManual = true
                graph.viewport.setMaxX(15.0)
                graph.gridLabelRenderer.isHumanRoundingX.and(false)


                for(feature in trainFeatures){
                    series = LineGraphSeries()
                    for(i in 0 until feature.size) {
                        series.appendData(
                                DataPoint(i.toDouble(), feature[i].toDouble()),
                                false,
                                100
                        )
                    }
                    graph.addSeries(series)
                }

                var incorrectFeatures = convertTimestampsToKeystrokes(
                        incorrectPressedTimestamps,
                        incorrectReleasedTimestamps
                )
                for(feature in incorrectFeatures){
                    series = LineGraphSeries()
                    series.color = Color.RED
                    for(i in 0 until feature.size) {
                        if(feature[i] > meanFeatures[i] * 2) {
                            series.appendData(
                                    DataPoint(
                                            i.toDouble(),
                                            meanFeatures[i] * 2.toDouble()
                                    ), true, 100
                            )
                        }else if(feature[i] < meanFeatures[i] / 2){
                            series.appendData(
                                    DataPoint(
                                            i.toDouble(),
                                            meanFeatures[i] / 2.toDouble()
                                    ), true, 100
                            )
                        }else{
                            series.appendData(
                                    DataPoint(i.toDouble(), feature[i].toDouble()),
                                    true,
                                    100
                            )
                        }
                    }
                    graph.addSeries(series)
                }


                if (trainCounterLeft == 0) {
                    Log.v("MERET", incorrectReleasedTimestamps.size.toString())
                    if(incorrectReleasedTimestamps.size > 0){
                        val alertDialog = AlertDialog.Builder(activityContext)
                                .setTitle("Néhány minta eltér a többitől!")
                                .setMessage("Szeretnéd újraírni a hibás mintákat a biztonság növelése érdekében vagy nem?")
                                .setCancelable(false)
                                .setPositiveButton("Igen") { dialog, which ->
                                    retry = true
                                    trainCounterLeft = incorrectReleasedTimestamps.size/6
                                    trainPinTitle.text = "Write your pincode " + trainCounterLeft + " times"
                                }
                                .setNegativeButton("Nem") { dialog, which ->
                                    pressedTimestamps.addAll(incorrectPressedTimestamps)
                                    releasedTimeStamps.addAll(incorrectReleasedTimestamps)
                                    trainPopUp.dismiss()
                                    trainApplication(pressedTimestamps, releasedTimeStamps)
                                }
                                .create()

                        alertDialog.window!!.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
                        alertDialog.show()
                    }else {
                        trainPopUp.dismiss()
                        trainApplication(pressedTimestamps, releasedTimeStamps)
                    }
                }
            }
        }

        trainkeyDel.setOnClickListener{ _ ->
            trainEditText.setText("")
            temporarPressedTimestamps = ArrayList()
            temporarReleasedTimeStamps = ArrayList()
        }
    }

    private fun convertTimestampsToKeystrokes(
            pressedTimestamps: ArrayList<Long>,
            releasedTimestamps: ArrayList<Long>
    ):ArrayList<List<Int>> {

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

    @SuppressLint("ClickableViewAccessibility")
    fun setKeyboardColor(){
        val colorPopUp = PopupWindow(
                layoutInflater.inflate(R.layout.colorpicker, null, false),
                Resources.getSystem().displayMetrics.widthPixels,
                editText.rootView.height,
                true
        )
        colorPopUp.showAtLocation(editText.rootView, Gravity.TOP, 0, 0)

        val colorPickerImage:ImageView = colorPopUp.contentView.findViewById(R.id.colorPicker)
        val colorPickerButton:Button = colorPopUp.contentView.findViewById(R.id.pickerButton)
        var bitmap: Bitmap
        var r:Int = 1
        var g:Int = 1
        var b:Int = 1


        colorPickerImage.setOnTouchListener { view, motionEvent ->
            if(motionEvent.action == MotionEvent.ACTION_DOWN || motionEvent.action == MotionEvent.ACTION_MOVE){
                //bitmap = Bitmap.createBitmap(colorPickerImage.width,colorPickerImage.height,Bitmap.Config.ARGB_8888)
                bitmap = (colorPickerImage.drawable as BitmapDrawable).bitmap
                Log.v("Meret",colorPickerImage.width.toString()+ " " + colorPickerImage.height)
                Log.v("MOZGAS", bitmap.width.toString() + " " +bitmap.height)
                Log.v("MOZGAS", motionEvent.x.toString() + " " +motionEvent.y)
                val pixel: Int = bitmap.getPixel(motionEvent.x.toInt(), motionEvent.y.toInt())

                r = Color.red(pixel)
                g = Color.green(pixel)
                b = Color.blue(pixel)

                colorPickerButton.setBackgroundColor(Color.rgb(r, g, b))

            }
            false
        }

        colorPickerButton.setOnClickListener{

            setKeyboardColor(r,g,b)

            val editor = sharedPreferences.edit()
            editor.putInt("R",r)
            editor.putInt("G",g)
            editor.putInt("B",b)

            editor.apply()
            Log.v("VANEBENNE",sharedPreferences.contains("R").toString())
            colorPopUp.dismiss()

        }
    }

    private fun setKeyboardColor(r:Int, g:Int, b:Int){
        for(element in keyArray){
            element.setBackgroundColor(Color.rgb(r, g, b))
        }
        keyDel.setBackgroundColor(Color.rgb(r, g, b))
        keyOk.setBackgroundColor(Color.rgb(r, g, b))
    }

    private fun setTrainKeyboardColor(r:Int, g:Int, b:Int){
        for (element in trainKeyArray) {
            element.setBackgroundColor(Color.rgb(r, g, b))
        }
        trainkeyDel.setBackgroundColor(Color.rgb(r, g, b))
        trainkeyOk.setBackgroundColor(Color.rgb(r, g, b))

    }

    fun setDeviationForThreshold(){
        getThresholds()
        if(threshold > 0.0) {
            val thresholdsAndRates: ArrayList<MutableList<Double>> = authenticationBroadcastReceiver.getRates()
            activityContext.setTheme(R.style.Theme_MaterialComponents)
            deviationPopUp = PopupWindow(
                    layoutInflater.inflate(R.layout.deviationsetter, null, false),
                    Resources.getSystem().displayMetrics.widthPixels,
                    editText.rootView.height,
                    true
            )
            deviationPopUp.showAtLocation(editText.rootView, Gravity.TOP, 0, 0)
            val slider = deviationPopUp.contentView.findViewById<Slider>(R.id.slider)

            if(easyThreshold > hardThreshold){
                val temp = easyThreshold
                easyThreshold = hardThreshold
                hardThreshold = temp
            }
            deviationPopUp.contentView.findViewById<Button>(R.id.deviationOkButton)
                    .setOnClickListener {
                        val sharedPreference =
                                activityContext.getSharedPreferences("THRESHOLD", Context.MODE_PRIVATE)
                        val editor = sharedPreference.edit()
                        editor.putFloat("threshold", (threshold + slider.value).toFloat())
                        editor.putFloat("baseThreshold", threshold.toFloat())
                        editor.putFloat("easyThreshold", easyThreshold.toFloat())
                        editor.putFloat("hardThreshold", hardThreshold.toFloat())
                        editor.apply()
                        //activityContext.setTheme(R.style.UsingKeystrokeAuthentication)
                        deviationPopUp.dismiss()
                    }


            deviationPopUp.contentView.findViewById<TextView>(R.id.actualValue).text =
                    String.format("%.5f", threshold)
            deviationPopUp.contentView.findViewById<TextView>(R.id.lowerBoundary).text =
                    String.format("%.5f", easyThreshold)
            deviationPopUp.contentView.findViewById<TextView>(R.id.upperBoundary).text =
                    String.format("%.5f", hardThreshold.toFloat())

            val graph2 = deviationPopUp.contentView.findViewById<GraphView>(R.id.graphView)

            var series3: LineGraphSeries<DataPoint> = LineGraphSeries()
            var series5: LineGraphSeries<DataPoint> = LineGraphSeries()
            var series4: LineGraphSeries<DataPoint> = LineGraphSeries()

            graph2.removeAllSeries()
            graph2.viewport.isXAxisBoundsManual = true
            graph2.viewport.setMaxY(1.0)
            graph2.gridLabelRenderer.gridColor = Color.BLACK
            graph2.gridLabelRenderer.verticalLabelsColor = Color.BLACK
            graph2.gridLabelRenderer.horizontalLabelsColor = Color.BLACK
            thresholdsAndRates[0].maxOrNull()?.let { graph2.viewport.setMaxX(it) }

            graph2.viewport.isYAxisBoundsManual = true
            graph2.viewport.isXAxisBoundsManual = true



            series3 = LineGraphSeries()
            series5 = LineGraphSeries()
            for(i in 0 until thresholdsAndRates[0].size){
                series3.appendData(DataPoint(thresholdsAndRates[0][i], thresholdsAndRates[1][i]), false, 100)
                series5.appendData(DataPoint(thresholdsAndRates[0][i], thresholdsAndRates[2][i]), false, 100)
            }

            series3.color = Color.RED

            series4.appendData(DataPoint(threshold, 0.0), false, 100)
            series4.appendData(DataPoint(threshold, 1.0), false, 100)
            series4.color = Color.GREEN

            graph2.addSeries(series3)
            graph2.addSeries(series5)
            graph2.addSeries(series4)


            slider.valueFrom =  easyThreshold.toFloat()
            slider.valueTo = hardThreshold.toFloat()+0.00001.toFloat()
            slider.value = threshold.toFloat()
            slider.addOnChangeListener(Slider.OnChangeListener { slider, value, fromUser ->
                deviationPopUp.contentView.findViewById<TextView>(R.id.actualValue).text =
                        String.format("%.5f", value)

                graph2.removeAllSeries()

                series4 = LineGraphSeries()
                series4.appendData(DataPoint(value.toDouble(),0.0), false, 100)
                series4.appendData(DataPoint( value.toDouble(),1.0), false, 100)
                series4.color = Color.GREEN

                graph2.addSeries(series3)
                graph2.addSeries(series4)
                graph2.addSeries(series5)
            })
        }

    }

    fun getThresholds() {
        val thresholdList = authenticationBroadcastReceiver.getThresholds()
        threshold = thresholdList[0]
        easyThreshold = thresholdList[1]
        hardThreshold = thresholdList[2]
        Log.v("KAPOTT",threshold.toString())
        Log.v("KAPOTT",easyThreshold.toString())
        Log.v("KAPOTT",hardThreshold.toString())
    }

    fun Help(){
        val helpPopUp = PopupWindow(
                layoutInflater.inflate(R.layout.help, null, false),
                Resources.getSystem().displayMetrics.widthPixels,
                editText.rootView.height,
                true
        )
        helpPopUp.showAtLocation(editText.rootView, Gravity.TOP, 0, 0)

        val closeButton:Button = helpPopUp.contentView.findViewById(R.id.closeHelpButton)

        closeButton.setOnClickListener{
            helpPopUp.dismiss()
        }
    }


}





