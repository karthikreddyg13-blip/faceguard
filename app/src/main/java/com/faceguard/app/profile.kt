package com.faceguard.app

data class Profile(
    val id: Int = 0,
    val name: String,
    val relation: String,
    val isOwner: Boolean = false,
    val faceVector: String = "",
    val createdAt: Long = System.currentTimeMillis()
)