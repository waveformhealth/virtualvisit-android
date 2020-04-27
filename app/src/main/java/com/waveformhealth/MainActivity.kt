package com.waveformhealth

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.waveformhealth.databinding.ActivityMainBinding
import com.waveformhealth.repo.WaveformServiceRepository
import com.waveformhealth.room.RoomFragment
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

    @Inject
    lateinit var waveformServiceRepository: WaveformServiceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        (applicationContext as WaveformHealthApp).appComp().inject(this)
    }

    fun joinRoom(view: View) {
        Log.i(TAG, "join room button pressed")

        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                val passCodeEncoded = Credentials.basic("5693651332:", "")
                val roomResponse = waveformServiceRepository.createRoom(passCodeEncoded)
                roomResponse?.let { serviceRoom ->
                    roomSid = serviceRoom.sid
                    Log.i(TAG, roomSid)
                    val roomIdEncoded = Credentials.basic(serviceRoom.sid, "")
                    val tokenResponse = waveformServiceRepository.requestToken(passCodeEncoded, roomIdEncoded)
                    tokenResponse?.let { serviceTokenResponse ->
                        accessToken = serviceTokenResponse.token
                    }
                }
            }

            withContext(Dispatchers.Main) {
                val bundle = Bundle()
                bundle.putString("accessToken", accessToken)
                bundle.putString("roomSid", roomSid)

                val roomFragment = RoomFragment()
                roomFragment.arguments = bundle

                supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, roomFragment).commit()
                toggleViews(visible = false)
            }
        }
    }

    fun toggleViews(visible: Boolean) {
        when (visible) {
            true -> {
                binding.testJoinRoom.visibility = View.VISIBLE
                binding.fragmentContainer?.visibility = View.GONE
            }
            false -> {
                binding.testJoinRoom.visibility = View.GONE
                binding.fragmentContainer?.visibility = View.VISIBLE
            }
        }
    }
}
