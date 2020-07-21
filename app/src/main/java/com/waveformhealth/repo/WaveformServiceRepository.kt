package com.waveformhealth.repo

import com.waveformhealth.api.WaveformApiService
import com.waveformhealth.model.*
import javax.inject.Inject
import javax.inject.Singleton

class WaveformServiceRepository @Inject constructor(
    private val waveformApiService: WaveformApiService
) {
    fun createRoom(): ServiceRoom? =
        waveformApiService.createRoom().execute().body()

    fun requestToken(roomId: String): ServiceTokenResponse? =
        waveformApiService.getTokenFromService(roomId).execute().body()

    fun inviteContact(invite: Invite) {
        waveformApiService.inviteContact(invite).execute().body()
    }

}