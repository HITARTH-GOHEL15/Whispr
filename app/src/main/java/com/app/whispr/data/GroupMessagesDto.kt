package com.app.whispr.data


import com.google.gson.annotations.SerializedName


data class GroupMessagesDto(
    @SerializedName("has_more")
    var hasMore: Boolean? = null,
    @SerializedName("messages")
    var messages: List<Message?>? = null
)

data class Message(
    @SerializedName("attachment")
    var attachment: Attachment? = null,
    @SerializedName("client_id")
    var clientId: String? = null,
    @SerializedName("content")
    var content: String? = null,
    @SerializedName("created_at")
    var createdAt: String? = null,
    @SerializedName("id")
    var id: Int? = null,
    @SerializedName("message_type")
    var messageType: String? = null,
    @SerializedName("sender")
    var sender: Sender? = null,
    @SerializedName("sticker")
    var sticker: Sticker? = null
)

data class Attachment(
    @SerializedName("created_at")
    var createdAt: String? = null,
    @SerializedName("duration_ms")
    var durationMs: Any? = null,
    @SerializedName("height")
    var height: Int? = null,
    @SerializedName("id")
    var id: String? = null,
    @SerializedName("is_voice_note")
    var isVoiceNote: Boolean? = null,
    @SerializedName("kind")
    var kind: String? = null,
    @SerializedName("mime_type")
    var mimeType: String? = null,
    @SerializedName("name")
    var name: String? = null,
    @SerializedName("size")
    var size: Int? = null,
    @SerializedName("thumbnail_url")
    var thumbnailUrl: String? = null,
    @SerializedName("url")
    var url: String? = null,
    @SerializedName("width")
    var width: Int? = null
)

data class Sender(
    @SerializedName("email")
    var email: String? = null,
    @SerializedName("name")
    var name: String? = null
)

data class Sticker(
    @SerializedName("emoji")
    var emoji: String? = null,
    @SerializedName("id")
    var id: Int? = null,
    @SerializedName("pack_id")
    var packId: Int? = null,
    @SerializedName("url")
    var url: String? = null
)
