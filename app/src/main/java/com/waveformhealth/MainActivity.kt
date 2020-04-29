package com.waveformhealth

import android.Manifest
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
import com.twilio.video.CameraCapturer
import com.twilio.video.LocalVideoTrack
import com.twilio.video.VideoRenderer
import com.waveformhealth.databinding.ActivityMainBinding
import com.waveformhealth.model.Invite
import com.waveformhealth.repo.WaveformServiceRepository
import com.waveformhealth.room.RoomFragment
import kotlinx.android.synthetic.main.invite_contact_dialog.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivityLog"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var accessToken: String
    private lateinit var roomSid: String
    private lateinit var phoneNumber: String

    private var localVideoTrack: LocalVideoTrack? = null

    @Inject
    lateinit var waveformServiceRepository: WaveformServiceRepository

    private var granted = false
    private var previewShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        (applicationContext as WaveformHealthApp).appComp().inject(this)

        binding.startVisitButton.setOnClickListener {
            showAlertDialogButtonClicked()
        }
        checkPermissions(fromButton = false)
    }

    private fun inviteContact(phoneNumber: String) {
        val strippedPhoneNumber = phoneNumber.replace("-", "")
        val passCodeEncoded = Credentials.basic(BuildConfig.API_SECRET, "")
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                waveformServiceRepository.inviteContact(
                    passCodeEncoded,
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

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val phoneNumberReturn = customLayout.inviteContactPhoneNumberEditText.text.toString()
            if (phoneNumberReturn.isNotEmpty()) {
                if (android.util.Patterns.PHONE.matcher(phoneNumberReturn).matches()) {
                    phoneNumber = phoneNumberReturn
                    getAccessToken()
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

                    override fun onPermissionRationaleShouldBeShown(permissions: List<PermissionRequest>?, token: PermissionToken?) {
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
                val passCodeEncoded = Credentials.basic(BuildConfig.API_SECRET, "")
                val roomResponse = waveformServiceRepository.createRoom(passCodeEncoded)
                roomResponse?.let { serviceRoom ->
                    roomSid = serviceRoom.sid
                    Log.i(TAG, roomSid)
                    val tokenResponse =
                        waveformServiceRepository.requestToken(passCodeEncoded, roomSid)
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

                    supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, roomFragment).commit()
                    showFragment()
                }
            }
        } else {
            checkPermissions(true)
        }
    }

    private fun setUpPreviewCamera() {
        val cameraCapturer = CameraCapturer(this, CameraCapturer.CameraSource.FRONT_CAMERA)
        localVideoTrack = LocalVideoTrack.create(applicationContext, true, cameraCapturer)
        localVideoTrack?.addRenderer(binding.previewCamera as VideoRenderer)
        binding.previewCamera.mirror = true
    }

    override fun onPause() {
        super.onPause()
        localVideoTrack?.release()
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
