package com.monitor.health.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.monitor.health.Constant
import com.monitor.health.R
import com.monitor.health.utility.PreferenceHelper
import io.livekit.android.LiveKit
import io.livekit.android.RoomOptions
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.RemoteVideoTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * LiveKit video call screen for calls started from the admin/web side (see
 * CallInvitationController on the backend) — replaces the Twilio-based
 * VideoActivity for this flow. Fetches its own join token from
 * /api/patient/livekit/token using this device's stored Passport bearer
 * token, same as the web and Flutter sides.
 *
 * NOTE: written against the LiveKit Android SDK's documented public API
 * (io.livekit:livekit-android) but not compiled locally — verify against
 * the SDK version pinned in app/build.gradle before shipping.
 */
class LiveKitCallActivity : AppCompatActivity() {

    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var room: Room? = null

    private lateinit var remoteVideo: TextureViewRenderer
    private lateinit var statusText: TextView

    private var callerName: String? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.CAMERA] == true && result[Manifest.permission.RECORD_AUDIO] == true) {
            connect()
        } else {
            statusText.text = "Camera and microphone permissions are required."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_livekit_call)

        callerName = intent.getStringExtra("caller_name")

        remoteVideo = findViewById(R.id.remote_video)
        statusText = findViewById(R.id.call_status)
        statusText.text = if (callerName != null) "Calling $callerName…" else "Connecting…"

        findViewById<ImageButton>(R.id.decline_button).setOnClickListener { hangUp() }

        if (hasPermissions()) {
            connect()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    private fun hasPermissions(): Boolean {
        val cam = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val mic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        return cam && mic
    }

    private fun connect() {
        activityScope.launch {
            try {
                val (url, token) = withContext(Dispatchers.IO) { fetchToken() }

                val newRoom = LiveKit.create(
                    applicationContext,
                    RoomOptions(adaptiveStream = true, dynacast = true),
                )
                room = newRoom
                newRoom.initVideoRenderer(remoteVideo)

                activityScope.launch {
                    newRoom.events.collect { event ->
                        when (event) {
                            is RoomEvent.TrackSubscribed -> {
                                val track = event.track
                                if (track is RemoteVideoTrack) {
                                    track.addRenderer(remoteVideo)
                                    statusText.text = ""
                                }
                            }
                            is RoomEvent.Disconnected -> finish()
                            else -> {}
                        }
                    }
                }

                newRoom.connect(url, token)
                newRoom.localParticipant.setMicrophoneEnabled(true)
                newRoom.localParticipant.setCameraEnabled(true)
            } catch (e: Exception) {
                Log.e("LiveKitCallActivity", "connect error", e)
                statusText.text = "Could not connect to the call."
            }
        }
    }

    private fun fetchToken(): Pair<String, String> {
        val authToken = PreferenceHelper.getInstance(this).getString(Constant.AUTH_TOKEN, null)
            ?: throw IllegalStateException("No auth token stored.")

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(Constant.BASE_URL + "api/patient/livekit/token")
            .post(ByteArray(0).toRequestBody(null))
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw IllegalStateException("Empty response.")
            if (!response.isSuccessful) throw IllegalStateException("Server returned ${response.code}: $body")
            val data = JSONObject(body).getJSONObject("data")
            return Pair(data.getString("url"), data.getString("token"))
        }
    }

    private fun hangUp() {
        room?.disconnect()
        room = null
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        room?.disconnect()
        activityScope.cancel()
    }
}
