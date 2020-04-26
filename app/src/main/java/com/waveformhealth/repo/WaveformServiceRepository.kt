package com.waveformhealth.repo

import com.waveformhealth.api.WaveformApiService
import com.waveformhealth.model.ServiceResponseCode
import com.waveformhealth.model.ServiceRoom
import com.waveformhealth.model.ServiceTokenResponse
import javax.inject.Inject
import javax.inject.Singleton

class WaveformServiceRepository @Inject constructor(
    private val waveformApiService: WaveformApiService
) {
    fun createRoom(passcode: String): ServiceRoom? =
        waveformApiService.createRoom(passcode).execute().body()

    fun requestToken(passcode: String, roomId: String): ServiceTokenResponse? =
        waveformApiService.getTokenFromService(passcode, roomId).execute().body()

    fun inviteContact(passcode: String, phoneNumber: String): ServiceResponseCode? =
        waveformApiService.inviteContact(passcode, phoneNumber).execute().body()
}