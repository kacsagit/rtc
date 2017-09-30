package com.example.kata.rtc

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.PNCallback
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.enums.PNStatusCategory
import com.pubnub.api.models.consumer.PNPublishResult
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import org.json.JSONObject
import java.util.*


class KurentoActivity : AppCompatActivity() {
    private var mPubNub: PubNub? = null
    private var username: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kurento)
        initPubNub()
    }

    fun initPubNub() {
        val mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(baseContext)
        this.username = mSharedPreferences.getString(Constants.USER_NAME, UUID.randomUUID().toString())
        username="kata"
        val stdbyChannel = "12033"+Constants.STDBY_SUFFIX
        val pnConfiguration = PNConfiguration()
        pnConfiguration.subscribeKey = Constants.SUB_KEY
        pnConfiguration.publishKey = Constants.PUB_KEY
        pnConfiguration.secretKey="sec-c-MGExN2IxYTQtYmQ5Ny00NWJiLWJlYzAtZDg1Y2Q2MzVjNTY3"
        pnConfiguration.uuid=this.username
        pnConfiguration.isSecure = false

        this.mPubNub = PubNub(pnConfiguration)
        mPubNub?.addListener(object : SubscribeCallback() {
            override fun status(pubnub: PubNub?, status: PNStatus?) {


                if (status?.category == PNStatusCategory.PNUnexpectedDisconnectCategory) {
                    // This event happens when radio / connectivity is lost
                } else if (status?.category == PNStatusCategory.PNConnectedCategory) {

                    // Connect event. You can do stuff like publish, and know you'll get it.
                    // Or just use the connected event to confirm you are subscribed for
                    // UI / internal notifications, etc

                    if (status?.category == PNStatusCategory.PNConnectedCategory) {
                        pubnub?.publish()?.channel(stdbyChannel)?.message("hello!!")?.async(object : PNCallback<PNPublishResult>() {
                            override fun onResponse(result: PNPublishResult?, status: PNStatus) {
                                // Check whether request successfully completed or not.
                                if (!status.isError) {

                                   // val user = result.getString(Constants.JSON_CALL_USER)
                                    // Consider Accept/Reject call here
                                    val intent = Intent(this@KurentoActivity, VideoChatActivity::class.java)
                                    intent.putExtra(Constants.USER_NAME, username)
                                    intent.putExtra(Constants.JSON_CALL_USER, username)
                                    startActivity(intent)
                                    // Message successfully published to specified channel.
                                } else {

                                    // Handle message publish error. Check 'category' property to find out possible issue
                                    // because of which request did fail.
                                    //
                                    // Request can be resent using: [status retry];
                                }// Request processing failed.
                            }
                        })
                    }
                } else if (status?.category == PNStatusCategory.PNReconnectedCategory) {

                    // Happens as part of our regular operation. This event happens when
                    // radio / connectivity is lost, then regained.
                } else if (status?.category == PNStatusCategory.PNDecryptionErrorCategory) {

                    // Handle messsage decryption error. Probably client configured to
                    // encrypt messages and on live data feed it received plain text.
                }
            }

            override fun message(pubnub: PubNub?, message: PNMessageResult?) {
                // Handle new message stored in message.message
                if (message?.channel != null) {
                    // Message has been received on channel group stored in
                    // message.getChannel()
                } else {
                    // Message has been received on channel stored in
                    // message.getSubscription()
                }

                /*
                log the following items with your favorite logger
                    - message.getMessage()
                    - message.getSubscription()
                    - message.getTimetoken()
            */
            }

            override fun presence(pubnub: PubNub?, presence: PNPresenceEventResult?) {

            }
        })

        mPubNub?.subscribe()?.channels(Arrays.asList(stdbyChannel))?.execute()

    }

    fun makeCall(view: View) {
        val callNum = "erer"
        if (callNum.isEmpty() || callNum == this.username) {
            Toast.makeText(this, "Enter a valid number.", Toast.LENGTH_SHORT).show()
        }
        dispatchCall(callNum)
    }

    fun dispatchCall(callNum: String) {
        val callNumStdBy = callNum + Constants.STDBY_SUFFIX
        val jsonCall = JSONObject()
       /* try {
            jsonCall.put(Constants.JSON_CALL_USER, this.username)
            mPubNub?.publish(callNumStdBy, jsonCall, object : Callback() {
                override fun successCallback(channel: String?, message: Any?) {
                    Log.d("MA-dCall", "SUCCESS: " + message!!.toString())
                    val intent = Intent(this@KurentoActivity, VideoChatActivity::class.java)
                    intent.putExtra(Constants.USER_NAME, username)
                    intent.putExtra(Constants.CALL_USER, callNum)
                    startActivity(intent)
                }
            })
        } catch (e: JSONException) {
            e.printStackTrace()
        }*/

    }
}
