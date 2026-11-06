package com.app.whispr.data.remote.dto

data class GroupRequest(
    val name: String,
    val slug: String,
    val description: String? = "",
    val email: String
)
