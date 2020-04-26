package com.waveformhealth.model

data class ServiceSecretKey(
    val algorithm: String,
    val key: ArrayList<Int>
)