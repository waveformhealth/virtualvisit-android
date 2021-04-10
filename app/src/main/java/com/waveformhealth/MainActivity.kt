package com.waveformhealth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

    private var previewShowing = false

    private val cameraIds = arrayListOf<String>()
    private var currentCameraId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        (applicationContext as WaveformHealthApp).appComp().inject(this)

        binding.startVisitButton.setOnClickListener {
            if (checkPermissionsGranted()) {
                if (!previewShowing) {
                    setUpPreviewCamera()
                }
                showAlertDialogButtonClicked()
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                    Constants.PermissionRequests.CAMERA_PREVIEW_AND_DIALOG_REQUEST
                )
            }
        }

        binding.previewSwitchCamera.setOnClickListener {
            if (checkPermissionsGranted()) {
                if (!previewShowing) {
                    setUpPreviewCamera()
                }
                switchCamera()
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                    Constants.PermissionRequests.CAMERA_PREVIEW_AND_SWITCH_REQUEST
                )
            }
        }

        if (checkPermissionsGranted()) {
            if (!previewShowing) {
                setUpPreviewCamera()
            }
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                Constants.PermissionRequests.CAMERA_PREVIEW_REQUEST
            )
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume")
        if (checkPermissionsGranted()) {
            if (!previewShowing) {
                setUpPreviewCamera()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "onPause")
        if (::camera2Capturer.isInitialized) {
            localVideoTrack?.release()
            camera2Capturer.dispose()
            cameraIds.clear()
            currentCameraId = ""
            previewShowing = false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            Constants.PermissionRequests.CAMERA_PREVIEW_AND_DIALOG_REQUEST -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED
                ) {
                    if (!previewShowing) {
                        setUpPreviewCamera()
                    }
                    showAlertDialogButtonClicked()
                } else {
                    checkPermissionsResult(grantResults)
                }
                return
            }

            Constants.PermissionRequests.CAMERA_PREVIEW_AND_SWITCH_REQUEST -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED
                ) {
                    if (!previewShowing) {
                        setUpPreviewCamera()
                    }
                    switchCamera()
                } else {
                    checkPermissionsResult(grantResults)
                }
                return
            }

            Constants.PermissionRequests.CAMERA_PREVIEW_REQUEST -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED
                ) {
                    if (!previewShowing) {
                        setUpPreviewCamera()
                    }
                } else {
                    checkPermissionsResult(grantResults)
                }
                return
            }

            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun checkPermissionsGranted(): Boolean {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun checkPermissionsResult(grantResults: IntArray) {
        if (grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED &&
                grantResults[1] == PackageManager.PERMISSION_DENIED
            ) {
                Toast.makeText(
                    applicationContext,
                    "Camera and Audio permissions required to use Virtual Visit",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(
                    applicationContext,
                    "Camera permission required to use Virtual Visit",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (grantResults[1] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(
                    applicationContext,
                    "Audio permission required to use Virtual Visit",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                applicationContext,
                "Camera and Audio permissions required to use Virtual Visit",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun switchCamera() {
        if (cameraIds.indexOf(currentCameraId) == Constants.Camera.REAR_CAMERA) {
            currentCameraId = cameraIds[Constants.Camera.FRONT_CAMERA]
            camera2Capturer.switchCamera(currentCameraId)
            binding.previewCamera.mirror = true
        } else {
            currentCameraId = cameraIds[Constants.Camera.REAR_CAMERA]
            camera2Capturer.switchCamera(currentCameraId)
            binding.previewCamera.mirror = false
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

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(resources.getColor(R.color.colorPrimary))
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val phoneNumberReturn = customLayout.inviteContactPhoneNumberEditText.text.toString()
            if (phoneNumberReturn.isNotEmpty()) {
                if (android.util.Patterns.PHONE.matcher(phoneNumberReturn).matches()) {
                    phoneNumber = phoneNumberReturn
                    getAccessToken()
                    binding.joiningRoomProgressCircle.visibility = View.VISIBLE
                    dialog.dismiss()
                } else {
                    customLayout.inviteContactPhoneNumberTextInput.error =
                        "Enter a valid phone number"
                }
            } else {
                customLayout.inviteContactPhoneNumberTextInput.error = "Enter a phone number"
            }
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

    private fun inviteContact(phoneNumber: String) {
        val strippedPhoneNumber = phoneNumber.replace("-", "")
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                waveformServiceRepository.inviteContact(
                    Invite(roomSid, strippedPhoneNumber)
                )
                joinRoom()
            }
        }
    }

    private fun joinRoom() {
        Log.i(TAG, "join room button pressed")
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
    }

    private fun setUpPreviewCamera() {
        cameraIds.addAll(
            (getSystemService(Context.CAMERA_SERVICE) as CameraManager).cameraIdList
        )
        currentCameraId = cameraIds[Constants.Camera.FRONT_CAMERA]

        camera2Capturer = Camera2Capturer(applicationContext, currentCameraId)
        val videoFormat = VideoFormat(VideoDimensions.HD_S1080P_VIDEO_DIMENSIONS, 60)

        localVideoTrack =
            LocalVideoTrack.create(applicationContext, true, camera2Capturer, videoFormat)
        localVideoTrack?.addSink(binding.previewCamera)

        binding.previewCamera.mirror = true

        previewShowing = true
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
