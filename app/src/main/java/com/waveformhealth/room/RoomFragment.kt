package com.waveformhealth.room

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.twilio.video.*
import com.waveformhealth.R
import com.waveformhealth.WaveformHealthApp
import com.waveformhealth.databinding.FragmentRoomBinding
import com.waveformhealth.repo.WaveformServiceRepository
import com.waveformhealth.util.Constants
import javax.inject.Inject

class RoomFragment : Fragment() {

    var onResult: (() -> Unit)? = null

    private var _binding: FragmentRoomBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "RoomActivityLog"
    }

    private lateinit var room: Room
    private lateinit var accessToken: String
    private lateinit var roomSid: String
    private lateinit var participant: RemoteParticipant
    private lateinit var cameraCapturer: Camera2Capturer

    private var localAudioTracks = mutableListOf<LocalAudioTrack>()
    private var localVideoTracks = mutableListOf<LocalVideoTrack>()
    private var localDataTracks = mutableListOf<LocalDataTrack>()

    private var remoteAudioTracks = mutableListOf<RemoteAudioTrack>()
    private var remoteVideoTracks = mutableListOf<RemoteVideoTrack>()
    private var remoteDataTracks = mutableListOf<RemoteDataTrack>()

    private val cameraIds = arrayListOf<String>()
    private var currentCameraId = ""

    @Inject
    lateinit var waveFormRepository: WaveformServiceRepository

    private val roomListener = object : Room.Listener {
        override fun onRecordingStopped(room: Room) {
            Log.i(TAG, "onRecordingStopped")
        }

        override fun onParticipantDisconnected(
            room: Room,
            remoteParticipant: RemoteParticipant
        ) {
            Log.i(TAG, "onParticipantDisconnected")

            if (room.remoteParticipants.size == 0) {
                largeLocalSmallRemote()
            }
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

            participant = remoteParticipant
            participant.setListener(remoteParticipantListener())
        }

        override fun onConnected(room: Room) {
            Log.i(TAG, "onConnected")

            if (room.remoteParticipants.size > 0) {
                room.remoteParticipants.forEach {
                    it.remoteAudioTracks[0].remoteAudioTrack?.let { remoteAudioTrack ->
                        remoteAudioTracks.add(remoteAudioTrack)
                    }

                    it.remoteVideoTracks[0].remoteVideoTrack?.let { remoteVideoTrack ->
                        remoteVideoTracks.add(remoteVideoTrack)
                    }

                    it.remoteDataTracks[0].remoteDataTrack?.let { remoteDataTrack ->
                        remoteDataTracks.add(remoteDataTrack)
                    }

                    it.setListener(remoteParticipantListener())
                }
            } else {
                largeLocalSmallRemote()
            }
        }

        override fun onDisconnected(room: Room, twilioException: TwilioException?) {
            Log.i(TAG, "onDisconnected")

            localAudioTracks[0].release()
            localVideoTracks[0].release()
            localDataTracks[0].release()

            activity?.supportFragmentManager?.popBackStack()
            onResult?.invoke()
        }

        override fun onReconnecting(room: Room, twilioException: TwilioException) {
            Log.i(TAG, "onReconnecting")
        }

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity?.applicationContext as WaveformHealthApp).appComp().inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.smallVideoViewLocal.mirror = true
        binding.largeVideoViewLocal.mirror = true
        binding.smallVideoViewRemote.mirror = true
        binding.largeVideoViewRemote.mirror = true

        arguments?.let { bundle ->
            bundle.getString("roomSid")?.let {
                roomSid = it
            }
            bundle.getString("accessToken")?.let { token ->
                accessToken = token

                context?.let {
                    val localAudioTrack = LocalAudioTrack.create(it, true)
                    val localDataTrack = LocalDataTrack.create(it)

                    cameraIds.addAll(
                        (it.getSystemService(Context.CAMERA_SERVICE) as CameraManager).cameraIdList
                    )
                    currentCameraId = cameraIds[Constants.Camera.FRONT_CAMERA]

                    cameraCapturer = Camera2Capturer(it, currentCameraId)

                    val videoFormat = VideoFormat(VideoDimensions.HD_S1080P_VIDEO_DIMENSIONS, 60)
                    val localVideoTrack = LocalVideoTrack.create(it, true, cameraCapturer, videoFormat)

                    localVideoTrack?.addSink(binding.smallVideoViewLocal)
                    localVideoTrack?.addSink(binding.largeVideoViewLocal)

                    localAudioTracks.add(localAudioTrack!!)
                    localVideoTracks.add(localVideoTrack!!)
                    localDataTracks.add(localDataTrack!!)

                    val connectOptions = ConnectOptions.Builder(accessToken)
                        .audioTracks(localAudioTracks)
                        .videoTracks(localVideoTracks)
                        .dataTracks(localDataTracks)
                        .build()

                    room = Video.connect(it, connectOptions, roomListener)
                }
            }
        }

        initializeClickListeners()
    }

    private fun initializeClickListeners() {
        binding.roomDisconnectButton.setOnClickListener {
            disconnectFromRoom()
        }

        binding.roomToggleCameraButton.setOnClickListener {
            toggleCamera()
        }

        binding.roomToggleMicrophoneButton.setOnClickListener {
            toggleMic()
        }

        binding.smallVideoViewLocal.setOnClickListener {
            largeLocalSmallRemote()
        }

        binding.smallVideoViewRemote.setOnClickListener {
            largeRemoteSmallLocal()
        }

        binding.roomSwichCamera.setOnClickListener {
            if (cameraIds.indexOf(currentCameraId) == Constants.Camera.REAR_CAMERA) {
                currentCameraId = cameraIds[Constants.Camera.FRONT_CAMERA]
                switchCamera(currentCameraId, true)
            } else {
                currentCameraId = cameraIds[Constants.Camera.REAR_CAMERA]
                switchCamera(currentCameraId, false)
            }
        }
    }

    private fun switchCamera(cameraId: String, mirror: Boolean) {
        cameraCapturer.switchCamera(cameraId)
        binding.smallVideoViewLocal.mirror = mirror
        binding.largeVideoViewLocal.mirror = mirror
    }

    private fun largeRemoteSmallLocal() {
        binding.largeVideoViewRemote.visibility = View.VISIBLE
        binding.smallVideoViewLocal.visibility = View.VISIBLE

        binding.smallVideoViewRemote.visibility = View.GONE
        binding.largeVideoViewLocal.visibility = View.GONE
    }

    private fun largeLocalSmallRemote() {
        binding.smallVideoViewLocal.visibility = View.GONE
        binding.largeVideoViewRemote.visibility = View.GONE

        binding.largeVideoViewLocal.visibility = View.VISIBLE
        binding.smallVideoViewRemote.visibility = View.VISIBLE
    }

    private fun remoteParticipantListener(): RemoteParticipant.Listener {
        return object : RemoteParticipant.Listener {
            override fun onDataTrackPublished(
                remoteParticipant: RemoteParticipant,
                remoteDataTrackPublication: RemoteDataTrackPublication
            ) {
                Log.i(TAG, "remoteParticipantListener: onDataTrackPublished")
            }

            override fun onAudioTrackEnabled(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication
            ) {
                Log.i(TAG, "remoteParticipantListener: onAudioTrackEnabled")
            }

            override fun onAudioTrackPublished(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication
            ) {
                Log.i(TAG, "remoteParticipantListener: onAudioTrackPublished")
            }

            override fun onVideoTrackPublished(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication
            ) {
                Log.i(TAG, "remoteParticipantListener: onVideoTrackPublished")

            }

            override fun onVideoTrackSubscribed(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication,
                remoteVideoTrack: RemoteVideoTrack
            ) {
                Log.i(TAG, "remoteParticipantListener: onVideoTrackSubscribed")
                remoteVideoTrack.addSink(binding.largeVideoViewRemote)
                remoteVideoTrack.addSink(binding.smallVideoViewRemote)
                largeRemoteSmallLocal()
                remoteVideoTracks.add(remoteVideoTrack)
            }

            override fun onVideoTrackEnabled(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication
            ) {
                Log.i(TAG, "remoteParticipantListener: onVideoTrackEnabled")
            }

            override fun onVideoTrackDisabled(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication
            ) {
                Log.i(TAG, "remoteParticipantListener: onVideoTrackDisabled")
            }

            override fun onVideoTrackUnsubscribed(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication,
                remoteVideoTrack: RemoteVideoTrack
            ) {
                Log.i(TAG, "remoteParticipantListener: onVideoTrackUnsubscribed")
            }

            override fun onDataTrackSubscriptionFailed(
                remoteParticipant: RemoteParticipant,
                remoteDataTrackPublication: RemoteDataTrackPublication,
                twilioException: TwilioException
            ) {
                Log.i(TAG, "remoteParticipantListener: onDataTrackSubscriptionFailed")
            }

            override fun onAudioTrackDisabled(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication
            ) {
                Log.i(TAG, "remoteParticipantListener: onAudioTrackDisabled")
            }

            override fun onDataTrackSubscribed(
                remoteParticipant: RemoteParticipant,
                remoteDataTrackPublication: RemoteDataTrackPublication,
                remoteDataTrack: RemoteDataTrack
            ) {
                Log.i(TAG, "remoteParticipantListener: onDataTrackSubscribed")
            }

            override fun onAudioTrackUnsubscribed(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication,
                remoteAudioTrack: RemoteAudioTrack
            ) {
                Log.i(TAG, "remoteParticipantListener: onAudioTrackUnsubscribed")
            }

            override fun onAudioTrackSubscribed(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication,
                remoteAudioTrack: RemoteAudioTrack
            ) {
                Log.i(TAG, "remoteParticipantListener: onAudioTrackSubscribed")
            }

            override fun onVideoTrackSubscriptionFailed(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication,
                twilioException: TwilioException
            ) {
                Log.i(TAG, "remoteParticipantListener: onVideoTrackSubscriptionFailed")
            }

            override fun onAudioTrackSubscriptionFailed(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication,
                twilioException: TwilioException
            ) {
                Log.i(TAG, "remoteParticipantListener: onAudioTrackSubscriptionFailed")
            }

            override fun onAudioTrackUnpublished(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication
            ) {
                Log.i(TAG, "remoteParticipantListener: onAudioTrackUnpublished")
            }

            override fun onVideoTrackUnpublished(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication
            ) {
                Log.i(TAG, "remoteParticipantListener: onVideoTrackUnpublished")
            }

            override fun onDataTrackUnsubscribed(
                remoteParticipant: RemoteParticipant,
                remoteDataTrackPublication: RemoteDataTrackPublication,
                remoteDataTrack: RemoteDataTrack
            ) {
                Log.i(TAG, "remoteParticipantListener: onDataTrackUnsubscribed")
            }

            override fun onDataTrackUnpublished(
                remoteParticipant: RemoteParticipant,
                remoteDataTrackPublication: RemoteDataTrackPublication
            ) {
                Log.i(TAG, "remoteParticipantListener: onDataTrackUnpublished")
            }

        }
    }

    private fun disconnectFromRoom() {
        room.disconnect()
    }

    private fun toggleCamera() {
        when (localVideoTracks[0].isEnabled) {
            true -> {
                localVideoTracks[0].enable(false)
                binding.roomToggleCameraButton.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_videocam_off,
                        null
                    )
                )
                Toast.makeText(context, "Camera off", Toast.LENGTH_SHORT).show()
            }
            false -> {
                localVideoTracks[0].enable(true)
                binding.roomToggleCameraButton.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_videocam_on,
                        null
                    )
                )
                Toast.makeText(context, "Camera on", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleMic() {
        when (localAudioTracks[0].isEnabled) {
            true -> {
                localAudioTracks[0].enable(false)
                binding.roomToggleMicrophoneButton.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_mic_off,
                        null
                    )
                )
                Toast.makeText(context, "Microphone muted", Toast.LENGTH_SHORT).show()
            }
            false -> {
                localAudioTracks[0].enable(true)
                binding.roomToggleMicrophoneButton.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_mic_on,
                        null
                    )
                )
                Toast.makeText(context, "Microphone activated", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
