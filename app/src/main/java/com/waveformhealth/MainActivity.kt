package com.waveformhealth

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.twilio.video.Camera2Capturer
import com.twilio.video.LocalVideoTrack
import com.twilio.video.VideoDimensions
import com.twilio.video.VideoFormat
import com.waveformhealth.databinding.ActivityMainBinding
import com.waveformhealth.model.Invite
import com.waveformhealth.repo.WaveformServiceRepository
import com.waveformhealth.room.RoomFragment
import com.waveformhealth.util.Constants
import kotlinx.android.synthetic.main.invite_contact_dialog.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivityLog"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var accessToken: String
    private lateinit var roomSid: String
    private lateinit var phoneNumber: String
    private lateinit var camera2Capturer: Camera2Capturer

    private var localVideoTrack: LocalVideoTrack? = null

    @Inject
    lateinit var waveformServiceRepository: WaveformServiceRepository

    private var granted = false
    private var previewShowing = false

    private val cameraIds = arrayListOf<String>()
    private var currentCameraId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        (applicationContext as WaveformHealthApp).appComp().inject(this)

        binding.startVisitButton.setOnClickListener {
            showAlertDialogButtonClicked()
        }

        binding.previewSwitchCamera.setOnClickListener {
            if (cameraIds.indexOf(currentCameraId) == Constants.Camera.REAR_CAMERA) {
                currentCameraId = cameraIds[Constants.Camera.FRONT_CAMERA]
                switchCamera(currentCameraId, true)
            } else {
                currentCameraId = cameraIds[Constants.Camera.REAR_CAMERA]
                switchCamera(currentCameraId, false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume")
        checkPermissions(fromButton = false)
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "onPause")
        localVideoTrack?.release()
        if (::camera2Capturer.isInitialized) {
            camera2Capturer.dispose()
        }
        granted = false
        cameraIds.clear()
        currentCameraId = ""
    }

    private fun switchCamera(cameraId: String, mirror: Boolean) {
        camera2Capturer.switchCamera(cameraId)
        binding.previewCamera.mirror = mirror
    }

    private fun inviteContact(phoneNumber: String) {
        val strippedPhoneNumber = phoneNumber.replace("-", "")
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                waveformServiceRepository.inviteContact(
                    Invite(roomSid, strippedPhoneNumber)
                )
                checkPermissions(fromButton = true)
            }
        }
    }

    private fun showAlertDialogButtonClicked() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this, R.style.dialogTheme)
        builder.setTitle("Invite contact")
        val customLayout = layoutInflater.inflate(R.layout.invite_contact_dialog, null)
        builder.setView(customLayout)
        builder.setPositiveButton("Invite and start Visit") { dialog, which ->
        }

        val dialog: AlertDialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(R.color.colorPrimary))
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val phoneNumberReturn = customLayout.inviteContactPhoneNumberEditText.text.toString()
            if (phoneNumberReturn.isNotEmpty()) {
                if (android.util.Patterns.PHONE.matcher(phoneNumberReturn).matches()) {
                    phoneNumber = phoneNumberReturn
                    getAccessToken()
                    binding.joiningRoomProgressCircle.visibility = View.VISIBLE
                    dialog.dismiss()
                } else {
                    customLayout.inviteContactPhoneNumberTextInput.error = "Enter a valid phone number"
                }
            } else {
                customLayout.inviteContactPhoneNumberTextInput.error = "Enter a phone number"
            }
        }
    }

    private fun checkPermissions(fromButton: Boolean) {
        if (!granted) {
            Dexter.withContext(this)
                .withPermissions(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                ).withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.let {
                            granted = it.areAllPermissionsGranted()
                            if (!previewShowing) {
                                setUpPreviewCamera()
                                previewShowing = false
                            }
                            if (granted && fromButton) {
                                joinRoom()
                            }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: List<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        token?.continuePermissionRequest()
                    }
                }).check()
        } else {
            joinRoom()
        }
    }

    private fun getAccessToken() {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                val roomResponse = waveformServiceRepository.createRoom()
                roomResponse?.let { serviceRoom ->
                    roomSid = serviceRoom.sid
                    Log.i(TAG, roomSid)
                    val tokenResponse =
                        waveformServiceRepository.requestToken(roomSid)
                    tokenResponse?.let { serviceTokenResponse ->
                        accessToken = serviceTokenResponse.token
                        inviteContact(phoneNumber)
                    }
                }
            }
        }
    }

    private fun joinRoom() {
        Log.i(TAG, "join room button pressed")
        if (granted) {
            GlobalScope.launch {
                withContext(Dispatchers.Main) {
                    val bundle = Bundle()
                    bundle.putString("accessToken", accessToken)
                    bundle.putString("roomSid", roomSid)

                    val roomFragment = RoomFragment()
                    roomFragment.arguments = bundle
                    roomFragment.onResult = {
                        hideFragment()
                        setUpPreviewCamera()
                    }

                    binding.joiningRoomProgressCircle.visibility = View.GONE
                    supportFragmentManager.beginTransaction().replace(
                        R.id.fragmentContainer,
                        roomFragment
                    ).commit()
                    showFragment()
                    localVideoTrack?.release()
                    camera2Capturer.dispose()
                    binding.previewSwitchCamera.visibility = View.GONE
                }
            }
        } else {
            checkPermissions(true)
        }
    }

    private fun setUpPreviewCamera() {
        cameraIds.addAll(
            (getSystemService(Context.CAMERA_SERVICE) as CameraManager).cameraIdList
        )
        currentCameraId = cameraIds[Constants.Camera.FRONT_CAMERA]

        camera2Capturer = Camera2Capturer(applicationContext, currentCameraId)
        val videoFormat = VideoFormat(VideoDimensions.HD_S1080P_VIDEO_DIMENSIONS, 60)

        localVideoTrack = LocalVideoTrack.create(applicationContext, true, camera2Capturer, videoFormat)
        localVideoTrack?.addSink(binding.previewCamera)

        binding.previewCamera.mirror = true
    }

    private fun hideFragment() {
        binding.startVisitButton.visibility = View.VISIBLE
        binding.previewCamera.visibility = View.VISIBLE
        binding.fragmentContainer.visibility = View.GONE
    }

    private fun showFragment() {
        binding.startVisitButton.visibility = View.GONE
        binding.previewCamera.visibility = View.GONE
        binding.fragmentContainer.visibility = View.VISIBLE
    }
}
