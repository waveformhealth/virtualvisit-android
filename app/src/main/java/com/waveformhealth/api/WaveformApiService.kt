package com.waveformhealth.api

import com.waveformhealth.model.ServiceResponseCode
import com.waveformhealth.model.ServiceTokenResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface WaveformApiService {

    @GET("/v1/token/")
    fun getTokenFromService(): Call<ServiceTokenResponse>

    @POST("/v1/invite/")
    fun inviteContact(
        @Header("Authorization") auth: String,
        @Header("X-Access-Token") token: String
    ): Call<ServiceResponseCode>
    
}