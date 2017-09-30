package com.example.kata.rtc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.example.kata.rtc.adapters.ChatAdapter
import com.example.kata.rtc.adt.ChatMessage
import com.example.kata.rtc.pnwebrtc.PnPeer
import com.example.kata.rtc.pnwebrtc.PnRTCClient
import com.example.kata.rtc.pnwebrtc.PnRTCListener
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.activity_video_chat.*
import org.json.JSONException
import org.webrtc.*
import java.util.*


class VideoChatActivity : AppCompatActivity() {
    companion object {
        val VIDEO_TRACK_ID = "videoPN"
        val AUDIO_TRACK_ID = "audioPN"
        val LOCAL_MEDIA_STREAM_ID = "localStreamPN"
    }


    private var localVideoSource: VideoSource? = null
    private var pnRTCClient: PnRTCClient? = null
    private var username: String? = null
    lateinit private var eglBase: EglBase
    lateinit private var localRenderer: SurfaceViewRenderer
    lateinit private var remoteRenderer: SurfaceViewRenderer
    lateinit var mChatEditText: EditText
    lateinit var mChatList: ListView
    lateinit var mChatAdapter: ChatAdapter
    lateinit var mCallStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_chat)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val extras = intent.extras
        if (extras == null || !extras.containsKey(Constants.USER_NAME)) {
            val intent = Intent(this, KurentoActivity::class.java)
            startActivity(intent)
            Toast.makeText(this, "Need to pass username to VideoChatActivity in intent extras (Constants.USER_NAME).",
                    Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        this.username = extras.getString(Constants.USER_NAME, "")
        this.mChatList = findViewById<ListView>(R.id.list)
        this.mChatEditText = findViewById<EditText>(R.id.chat_input)
        this.mCallStatus = findViewById<TextView>(R.id.call_status)

        // Set up the List View for chatting
        val ll = LinkedList<ChatMessage>()
        mChatAdapter = ChatAdapter(this, ll)
        mChatList.adapter = mChatAdapter

        PeerConnectionFactory.initializeAndroidGlobals(
                this, // Context
                true, // Audio Enabled
                true, // Video Enabled
                true // Hardware Acceleration Enabled
        ) // Render EGL Context
        val options = PeerConnectionFactory.Options()

        val pcFactory = PeerConnectionFactory(options)

        this.pnRTCClient = PnRTCClient(Constants.PUB_KEY, Constants.SUB_KEY, this.username)

        // Returns the number of cams & front/back face device name
        val capturer = createCameraCapturer(Camera1Enumerator(false))

        // First create a Video Source, then we can make a Video Track
        localVideoSource = pcFactory.createVideoSource(capturer)
        val localVideoTrack = pcFactory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource)

        // First we create an AudioSource then we can create our AudioTrack
        val audioSource = pcFactory.createAudioSource(pnRTCClient?.audioConstraints())
        val localAudioTrack = pcFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource)

        capturer?.startCapture(1000, 1000, 30)

        val mediaStream = pcFactory.createLocalMediaStream(LOCAL_MEDIA_STREAM_ID)

        // Now we can add our tracks.
        mediaStream.addTrack(localVideoTrack)
        mediaStream.addTrack(localAudioTrack)


        initRenderers()

        // VideoChatActivity#onCreate()
        // First attach the RTC Listener so that callback events will be triggered
        this.pnRTCClient?.attachRTCListener(MyRTCListener())

        // Then attach your local media stream to the PnRTCClient.
        //  This will trigger the onLocalStream callback.
        this.pnRTCClient?.attachLocalMediaStream(mediaStream)

        // Listen on a channel. This is your "phone number," also set the max chat users.
        this.pnRTCClient?.listenOn("12033" + Constants.STDBY_SUFFIX)
        this.pnRTCClient?.setMaxConnections(1)

        // If Constants.CALL_USER is in the intent extras, auto-connect them.
        if (extras.containsKey(Constants.CALL_USER)) {
            val callUser = extras.getString(Constants.CALL_USER, "")
            connectToUser(callUser)
        }

    }


    private fun initRenderers() {
        eglBase = EglBase.create()
        localRenderer = SurfaceViewRenderer(this)
        remoteRenderer = SurfaceViewRenderer(this)

        localRenderer.init(eglBase.eglBaseContext, null)
        remoteRenderer.init(eglBase.eglBaseContext, null)


        remote_render_layout.setPosition(0, 0, 100, 100)

        localRenderer.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        localRenderer.setMirror(true)
        localRenderer.setZOrderMediaOverlay(true)

        remoteRenderer.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        remoteRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)


        local_render_layout.addView(localRenderer)
        remote_render_layout.addView(remoteRenderer)
    }


    fun connectToUser(user: String) {
        this.pnRTCClient?.connect("kata")
    }

    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames

        // First, try to find front facing camera

        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val videoCapturer = enumerator.createCapturer(deviceName, null)

                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }

        // Front facing camera not found, try something else

        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                val videoCapturer = enumerator.createCapturer(deviceName, null)

                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }

        return null
    }

    fun hangup(view: View) {
        this.pnRTCClient?.closeAllConnections()
        startActivity(Intent(this@VideoChatActivity, KurentoActivity::class.java))
    }

    private inner class MyRTCListener : PnRTCListener()// Override methods you plan on using
    {
        override fun onAddRemoteStream(remoteStream: MediaStream?, peer: PnPeer?) {
            super.onAddRemoteStream(remoteStream, peer) // Will log values
            this@VideoChatActivity.runOnUiThread(java.lang.Runnable {
                Toast.makeText(this@VideoChatActivity, "Connected to " + peer?.getId(), Toast.LENGTH_SHORT).show()
                try {
                    if (remoteStream?.videoTracks?.size === 0) return@Runnable
                    remoteStream!!.videoTracks[0].addRenderer(VideoRenderer(remoteRenderer))
                    remote_render_layout.setPosition(0, 0, 100, 100)
                    local_render_layout.setPosition(72, 65, 25, 25)
                    remote_render_layout.requestLayout()
                    local_render_layout.requestLayout()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
        }

        override fun onPeerConnectionClosed(peer: PnPeer?) {
            super.onPeerConnectionClosed(peer)
            val intent = Intent(this@VideoChatActivity, KurentoActivity::class.java)
            startActivity(intent)
            finish()
        }

        override fun onMessage(peer: PnPeer, message: Any) {
            if (message !is JsonObject) return  //Ignore if not JSONObject
            val user = message.get(Constants.JSON_MSG_UUID).asString
            val text = message.get(Constants.JSON_MSG).asString
            val time = message.get(Constants.JSON_TIME).asLong
            val chatMsg = ChatMessage(user, text, time)
            this@VideoChatActivity.runOnUiThread { mChatAdapter.addMessage(chatMsg) }


        }


        override fun onLocalStream(localStream: MediaStream?) {
            super.onLocalStream(localStream) // Will log values
            this@VideoChatActivity.runOnUiThread(java.lang.Runnable {
                if (localStream?.videoTracks?.size === 0) return@Runnable
                localStream!!.videoTracks[0].addRenderer(VideoRenderer(localRenderer))
            })
        }
    }


    fun sendMessage(view: View) {
        val message = mChatEditText.getText().toString()
        if (message == "") return  // Return if empty
        val chatMsg = ChatMessage(this.username, message, System.currentTimeMillis())
        val messageJSON = JsonObject()
        try {
            messageJSON.addProperty(Constants.JSON_MSG_UUID, chatMsg.sender)
            messageJSON.addProperty(Constants.JSON_MSG, chatMsg.message)
            messageJSON.addProperty(Constants.JSON_TIME, chatMsg.timeStamp)
            this.pnRTCClient?.transmitAll(messageJSON)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        // Hide keyboard when you send a message.
        val focusView = this.currentFocus
        if (focusView != null) {
            val inputManager = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
        mChatEditText.setText("")
    }


}
