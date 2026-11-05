package com.app.whispr.data


import com.google.gson.annotations.SerializedName

data class GroupResponseDto(
    @SerializedName("count")
    var count: Int? = null,
    @SerializedName("next")
    var next: Any? = null,
    @SerializedName("previous")
    var previous: Any? = null,
    @SerializedName("results")
    var results: List<Result?>? = null
)


data class Result(
    @SerializedName("created_at")
    var createdAt: String? = null,
    @SerializedName("description")
    var description: String? = null,
    @SerializedName("name")
    var name: String? = null,
    @SerializedName("online_count")
    var onlineCount: Int? = null,
    @SerializedName("slug")
    var slug: String? = null
)
