package com.waveformhealth.api

import com.waveformhealth.model.*
import retrofit2.Call
import retrofit2.http.*

interface WaveformApiService {

    @GET("/token")
    fun getTokenFromService(
        @Query("room") roomId: String
    ): Call<ServiceTokenResponse>

    @POST("/invitation")
    fun inviteContact(
        @Body invite: Invite
    ): Call<Void>

    @POST("/room")
    fun createRoom(): Call<ServiceRoom>

}