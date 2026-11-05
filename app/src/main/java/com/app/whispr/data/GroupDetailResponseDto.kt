package com.app.whispr.data


import com.google.gson.annotations.SerializedName

data class GroupDetailResponseDto(
    @SerializedName("created_at")
    var createdAt: String? = null,
    @SerializedName("description")
    var description: String? = null,
    @SerializedName("name")
    var name: String? = null,
    @SerializedName("online_count")
    var onlineCount: Int? = null,
    @SerializedName("online_members")
    var onlineMembers: List<OnlineMember?>? = null,
    @SerializedName("slug")
    var slug: String? = null,
    @SerializedName("websocket_url")
    var websocketUrl: String? = null
)

data class OnlineMember(
    @SerializedName("email")
    var email: String? = null,
    @SerializedName("joined_at")
    var joinedAt: String? = null,
    @SerializedName("last_seen_at")
    var lastSeenAt: String? = null,
    @SerializedName("name")
    var name: String? = null,
    @SerializedName("online")
    var online: Boolean? = null
)
