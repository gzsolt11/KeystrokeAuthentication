package com.example.keystrokeauthentication

import android.app.Service
import android.content.*
import android.content.res.Resources
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager


class KeystrokeAuthenticationProvider( var activityContext:Context,var editText: EditText) {

    private val TRAINCOUNTER = 10
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
    private var trainCounterLeft = TRAINCOUNTER
    private var keyArray = ArrayList<TextView>()
    private var trainKeyArray = ArrayList<TextView>()
    private lateinit var keyboardPopUp:PopupWindow
    private lateinit var trainPopUp:PopupWindow
    private var isClicked = false
    private val sharedPreferences: SharedPreferences
    private var authenticationBroadcastReceiver: AuthenticationBroadcastReceiver
    private var isServiceRunning = false

    init{
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

        authenticationBroadcastReceiver = AuthenticationBroadcastReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction("StartBackgroundReceiver")
        LocalBroadcastManager.getInstance(activityContext).registerReceiver(authenticationBroadcastReceiver,intentFilter)

        startBackgroundProcess()

        setListenersToKeys()
        sharedPreferences = activityContext.getSharedPreferences("sharedPref", Service.MODE_PRIVATE)
    }


    private fun trainApplication(pressedTimestamps:ArrayList<Long>, releasedTimestamps:ArrayList<Long>){
        val intent = Intent("StartBackgroundReceiver")
        intent.putExtra("ID",2)
        intent.putExtra("pressedTimestamps", pressedTimestamps)
        intent.putExtra("releasedTimestamps", releasedTimestamps)
        LocalBroadcastManager.getInstance(activityContext).sendBroadcast(intent)
    }

    fun startBackgroundProcess(){
        val intent = Intent("StartBackgroundReceiver")
        intent.putExtra("ID",0)
        LocalBroadcastManager.getInstance(activityContext).sendBroadcast(intent)
        isServiceRunning = true
    }

    fun authenticate(): Boolean{
        if(isServiceRunning) {
            val authenticationResult = authenticationBroadcastReceiver.authenticate(authenticatePressedTimestamps, authenticateReleasedTimestamps)
            editText.setText("")
            authenticatePressedTimestamps = ArrayList()
            authenticateReleasedTimestamps = ArrayList()
            return authenticationResult
        }
        editText.setText("")
        return false
    }


    fun stopBackgroundProcess(){
        LocalBroadcastManager.getInstance(activityContext).unregisterReceiver(authenticationBroadcastReceiver)
        isServiceRunning = false
    }

    private fun ruleEditText(editText:EditText){
        layoutInflater = activityContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        keyboardPopUp = PopupWindow(layoutInflater.inflate(R.layout.keyboard, null,false), Resources.getSystem().displayMetrics.widthPixels,ViewGroup.LayoutParams.WRAP_CONTENT,true)

        inputMethodManager = activityContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(editText.rootView.findViewById(R.id.content), 0)
        editText.isFocusable = false
        editText.setOnClickListener{ view ->
            keyboardPopUp.showAtLocation(view.rootView,Gravity.BOTTOM,0,0)
        }
    }

    fun train(){
        pressedTimestamps = ArrayList()
        releasedTimeStamps = ArrayList()
        trainCounterLeft = TRAINCOUNTER

        trainPopUp = PopupWindow(layoutInflater.inflate(R.layout.trainingpincode, null,false), Resources.getSystem().displayMetrics.widthPixels,editText.rootView.height,true)
        trainPopUp.showAtLocation(editText.rootView,Gravity.TOP,0,0)

        trainPinTitle = trainPopUp.contentView.findViewById(R.id.trainPinCodeTitle)
        trainPinTitle.text = "Write your pincode " + trainCounterLeft + " times"

        trainEditText = trainPopUp.contentView.findViewById<EditText>(R.id.trainPinCodeEditText)
        inputMethodManager.hideSoftInputFromWindow(trainEditText.rootView.findViewById(R.id.content), 0)
        trainEditText.isFocusable = false

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
        trainkeyOk = trainPopUp.contentView.findViewById(R.id.buttonOK)
        trainkeyDel = trainPopUp.contentView.findViewById(R.id.buttonDelete)

        setListenersToTrainKeys()
    }


    private fun setListenersToKeys(){
        for(key in keyArray){
            key.setOnClickListener{ _ ->
                if(editText.text.length < 6) {
                    editText.setText(editText.text.toString() + "*")
                }
            }
            key.setOnTouchListener(object: View.OnTouchListener{
                override fun onTouch(view: View?, event: MotionEvent?): Boolean {
                    if(authenticateReleasedTimestamps.size < 6) {
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
            key.setOnTouchListener(object: View.OnTouchListener{
                override fun onTouch(view: View?, event: MotionEvent?): Boolean {
                    if(temporarReleasedTimeStamps.size < 6) {
                        if (event?.action == MotionEvent.ACTION_DOWN && !isClicked) {
                            temporarPressedTimestamps.add(event.eventTime)
                            isClicked = true
                        }
                        if (event?.action == MotionEvent.ACTION_UP  && isClicked) {
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
                pressedTimestamps.addAll(temporarPressedTimestamps)
                releasedTimeStamps.addAll(temporarReleasedTimeStamps)
                temporarPressedTimestamps = ArrayList()
                temporarReleasedTimeStamps = ArrayList()
                trainEditText.setText("")
                trainCounterLeft -= 1
                trainPinTitle.text = "Write your pincode " + trainCounterLeft + " times"
                if (trainCounterLeft == 0) {
                    trainPopUp.dismiss()
                    trainApplication(pressedTimestamps, releasedTimeStamps)
                }
            }
        }

        trainkeyDel.setOnClickListener{ _ ->
            trainEditText.setText("")
            temporarPressedTimestamps = ArrayList()
            temporarReleasedTimeStamps = ArrayList()
        }
    }
}