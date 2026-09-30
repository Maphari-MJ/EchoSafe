package com.echosafe.data.model

import com.google.firebase.Timestamp

data class User(
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val fcmToken: String = "",
    val age: Int = 0,
    val gender: String = "",
    val createdAt: Timestamp = Timestamp.now()
)
