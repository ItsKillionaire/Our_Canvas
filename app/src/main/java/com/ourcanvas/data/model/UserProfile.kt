package com.ourcanvas.data.model

// Represents a user's profile, primarily for mood status.
data class UserProfile(
    val uid: String = "",
    val mood: String = "😊", // Default mood emoji
    var coupleId: String? = null
)
