package com.example.agoralivestream

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas


class MainActivity : AppCompatActivity() {

    // Fill the App ID of your project generated on Agora Console.
    private val appId = "dc897096a4464644891172bab14ac5d3"

    // Fill the channel name.
    private val channelName = "TestLiveStream"

    // Fill the temp token generated on Agora Console.
    private val token = "006dc897096a4464644891172bab14ac5d3IABsjLs8+6HCdMZLgbIdwqVndZd1K7aM+vFYawFJgRbsyQKyqsUAAAAAEACmdyT/dFXjYwEA6AN0VeNj"

    // An integer that identifies the local user.
    private val uid = 0
    private var isJoined = false

    private var agoraEngine: RtcEngine? = null

    //SurfaceView to render local video in a Container.
    private var localSurfaceView: SurfaceView? = null

    //SurfaceView to render Remote video in a Container.
    private var remoteSurfaceView: SurfaceView? = null

    // A toggle switch to change the User role.
    private var audienceRole: Switch? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        audienceRole =  findViewById(R.id.switch1);
        // If all the permissions are granted, initialize the RtcEngine object and join a channel.
        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
        }
        setupVideoSDKEngine();
    }



    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    private fun checkSelfPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                REQUESTED_PERMISSIONS[0]
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                REQUESTED_PERMISSIONS[1]
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            false
        } else true
    }
    fun showMessage(message: String?) {
        runOnUiThread {
            Toast.makeText(
                applicationContext,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupVideoSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            // By default, the video module is disabled, call enableVideo to enable it.
            agoraEngine!!.enableVideo()
        } catch (e: Exception) {
            showMessage(e.toString())
        }
    }
    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Listen for the remote host joining the channel to get the uid of the host.
        override fun onUserJoined(uid: Int, elapsed: Int) {
            showMessage("Remote user joined $uid")
            if (!audienceRole!!.isChecked) return
            // Set the remote video view
            runOnUiThread { setupRemoteVideo(uid) }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            isJoined = true
            showMessage("Joined Channel $channel")
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            showMessage("Remote user offline $uid $reason")
            runOnUiThread { remoteSurfaceView!!.visibility = View.GONE }
        }
    }
    private fun setupRemoteVideo(uid: Int) {
        val container = findViewById<FrameLayout>(R.id.remote_video_view_container)
        remoteSurfaceView = SurfaceView(baseContext)
        remoteSurfaceView!!.setZOrderMediaOverlay(true)
        container.addView(remoteSurfaceView)
        agoraEngine!!.setupRemoteVideo(
            VideoCanvas(
                remoteSurfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                uid
            )
        )
        // Display RemoteSurfaceView.
        remoteSurfaceView!!.setVisibility(View.VISIBLE)
    }

    private fun setupLocalVideo() {
        val container = findViewById<FrameLayout>(R.id.local_video_view_container)
        // Create a SurfaceView object and add it as a child to the FrameLayout.
        localSurfaceView = SurfaceView(baseContext)
        container.addView(localSurfaceView)
        // Pass the SurfaceView object to Agora so that it renders the local video.
        agoraEngine!!.setupLocalVideo(
            VideoCanvas(
                localSurfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                0
            )
        )
    }
    fun joinChannel(view: View?) {
        if (checkSelfPermission()) {
            val options = ChannelMediaOptions()
            // For Live Streaming, set the channel profile as LIVE_BROADCASTING.
            options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
            // Set the client role as BROADCASTER or AUDIENCE according to the scenario.
            if (audienceRole!!.isChecked) { //Audience
                options.clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
            } else { //Host
                options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                // Display LocalSurfaceView.
                setupLocalVideo()
                localSurfaceView!!.visibility = View.VISIBLE
                // Start local preview.
                agoraEngine!!.startPreview()
            }
            audienceRole!!.isEnabled = false // Disable the switch
            // Join the channel with a temp token.
            // You need to specify the user ID yourself, and ensure that it is unique in the channel.
            agoraEngine!!.joinChannel(token, channelName, uid, options)
        } else {
            Toast.makeText(applicationContext, "Permissions was not granted", Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun leaveChannel(view: View?) {
        if (!isJoined) {
            showMessage("Join a channel first")
        } else {
            agoraEngine!!.leaveChannel()
            showMessage("You left the channel")
            // Stop remote video rendering.
            if (remoteSurfaceView != null) remoteSurfaceView!!.visibility = View.GONE
            // Stop local video rendering.
            if (localSurfaceView != null) localSurfaceView!!.visibility = View.GONE
            isJoined = false
        }
        audienceRole!!.isEnabled = true // Enable the switch
    }
    override fun onDestroy() {
        super.onDestroy()
        agoraEngine!!.stopPreview()
        agoraEngine!!.leaveChannel()

        // Destroy the engine in a sub-thread to avoid congestion
        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }


}