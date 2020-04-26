package com.waveformhealth.room

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.twilio.video.*
import com.waveformhealth.MainActivity
import com.waveformhealth.R
import com.waveformhealth.databinding.FragmentRoomBinding

class RoomFragment : Fragment() {

    private var _binding: FragmentRoomBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "RoomActivityLog"
    }

    private lateinit var room: Room
    private lateinit var accessToken: String
    private var localAudioTracks = mutableListOf<LocalAudioTrack>()
    private var localVideoTracks = mutableListOf<LocalVideoTrack>()
    private var localDataTracks = mutableListOf<LocalDataTrack>()

    private val roomListener = object : Room.Listener {
        override fun onRecordingStopped(room: Room) {
            Log.i(TAG, "onRecordingStopped")
        }

        override fun onParticipantDisconnected(
            room: Room,
            remoteParticipant: RemoteParticipant
        ) {
            Log.i(TAG, "onParticipantDisconnected")
        }

        override fun onRecordingStarted(room: Room) {
            Log.i(TAG, "onRecordingStarted")
        }

        override fun onConnectFailure(room: Room, twilioException: TwilioException) {
            Log.e(TAG, "onConnectFailure: " + twilioException.localizedMessage)
        }

        override fun onReconnected(room: Room) {
            Log.i(TAG, "onReconnected")
        }

        override fun onParticipantConnected(room: Room, remoteParticipant: RemoteParticipant) {
            Log.i(TAG, "onParticipantConnected")
        }

        override fun onConnected(room: Room) {
            Log.i(TAG, "onConnected")

            context?.let {
                val localAudioTrack = LocalAudioTrack.create(it, true)
                val localDataTrack = LocalDataTrack.create(it)
                val cameraCapturer = CameraCapturer(it, CameraCapturer.CameraSource.FRONT_CAMERA)
                val localVideoTrack = LocalVideoTrack.create(it, true, cameraCapturer)

                localVideoTrack?.addRenderer(binding.videoView as VideoRenderer)

                localAudioTracks.add(localAudioTrack!!)
                localVideoTracks.add(localVideoTrack!!)
                localDataTracks.add(localDataTrack!!)
            }

        }

        override fun onDisconnected(room: Room, twilioException: TwilioException?) {
            Log.i(TAG, "onDisconnected")

            localAudioTracks[0].release()
            localVideoTracks[0].release()
            localDataTracks[0].release()

            activity?.supportFragmentManager?.popBackStack()
            (activity as MainActivity).toggleViews(visible = true)
        }

        override fun onReconnecting(room: Room, twilioException: TwilioException) {
            Log.i(TAG, "onReconnecting")
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let { bundle ->
            bundle.getString("accessToken")?.let { token ->
                accessToken = token

                val connectOptions = ConnectOptions.Builder(accessToken)
                    .audioTracks(localAudioTracks)
                    .videoTracks(localVideoTracks)
                    .dataTracks(localDataTracks)
                    .build()
                room = Video.connect(context!!, connectOptions, roomListener)
            }
        }

        initializeClickListeners()
    }

    private fun initializeClickListeners() {
        binding.roomDisconnectButton?.setOnClickListener {
            disconnectFromRoom()
        }

        binding.roomToggleCameraButton?.setOnClickListener {
            toggleCamera()
        }

        binding.roomToggleMicrophoneButton?.setOnClickListener {
            toggleMic()
        }
    }

    private fun disconnectFromRoom() {
        room.disconnect()
    }

    private fun toggleCamera() {
        when (localVideoTracks[0].isEnabled) {
            true -> {
                localVideoTracks[0].enable(false)
                binding.roomToggleCameraButton?.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_videocam_on, null))
                Toast.makeText(context, "Camera off", Toast.LENGTH_SHORT).show()
            }
            false -> {
                localVideoTracks[0].enable(true)
                binding.roomToggleCameraButton?.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_videocam_off, null))
                Toast.makeText(context, "Camera on", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleMic() {
        when (localAudioTracks[0].isEnabled) {
            true -> {
                localAudioTracks[0].enable(false)
                binding.roomToggleMicrophoneButton?.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_mic_on, null))
                Toast.makeText(context, "Microphone muted", Toast.LENGTH_SHORT).show()
            }
            false -> {
                localAudioTracks[0].enable(true)
                binding.roomToggleMicrophoneButton?.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_mic_off, null))
                Toast.makeText(context, "Microphone activated", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
