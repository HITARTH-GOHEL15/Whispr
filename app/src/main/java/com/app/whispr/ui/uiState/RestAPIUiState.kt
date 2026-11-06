package com.app.whispr.ui.uiState

import com.app.whispr.data.remote.dto.GroupDetailResponseDto
import com.app.whispr.data.remote.dto.Message
import com.app.whispr.data.remote.dto.Result
import com.app.whispr.utils.ErrorBean

data class RestAPIUiState(
    val groups: List<Result?> = emptyList(),
    val groupMessages: List<Message?> = emptyList(),
    val groupDetail: GroupDetailResponseDto? = null,

    val groupDetailLoaded: Boolean = false,
    val groupsLoaded: Boolean = false,
    val groupMessagesLoaded: Boolean = false,

    val success: Boolean = false,
    var isLoading: Boolean = false,
    val error: ErrorBean? = null
)