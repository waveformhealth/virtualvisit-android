package com.waveformhealth.api

import com.waveformhealth.model.*
import retrofit2.Call
import retrofit2.http.*

interface WaveformApiService {

    @GET("/v1/token/")
    fun getTokenFromService(
        @Header("Authorization") passcode: String,
        @Query("room") roomId: String
    ): Call<ServiceTokenResponse>

    @POST("/v1/invitation/")
    fun inviteContact(
        @Header("Authorization") passcode: String,
        @Body invite: Invite
    ): Call<Void>

    @POST("/v1/room/")
    fun createRoom(
        @Header("Authorization") passcode: String
    ): Call<ServiceRoom>

}