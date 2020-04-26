package com.waveformhealth.model

data class ServiceTokenResponse(
    val accountSid: String,
    val algorithm: String,
    val expiration: String,
    val grants: ServiceGrant,
    val id : String,
    val identity: String,
    val issuer: String,
    val secretKey: ServiceSecretKey
)