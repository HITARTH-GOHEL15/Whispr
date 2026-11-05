package com.app.whispr.data

data class GroupRequest(
    val name: String,
    val slug: String?  = "",
    val description: String? = "",
    val email: String
)
