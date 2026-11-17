package com.app.whispr.ui.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieexplorer2.utils.resource.Status
import com.app.whispr.data.repository.WhisprRepository
import com.app.whispr.ui.uiState.RestAPIUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import kotlin.collections.orEmpty

@HiltViewModel
class RestApiViewModel @Inject constructor(
    private val repository: WhisprRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(RestAPIUiState())

    val uiState: StateFlow<RestAPIUiState> =
        _uiState.asStateFlow()

    private fun updateLoadingState() {

        val state = _uiState.value

        if (
            state.groupsLoaded &&
            state.groupMessagesLoaded &&
            state.groupDetailLoaded
        ) {

            _uiState.value =
                state.copy(
                    isLoading = false
                )
        }
    }

    fun getGroups() {

        repository.getGroups()
            .onEach { state ->

                when (state.status) {

                    Status.LOADING -> {
                        // Keep existing loading state
                    }

                    Status.SUCCESS -> {

                        Log.d("GROUP_DEBUG", "SUCCESS received")
                        Log.d("GROUP_DEBUG", "Response: ${state.data}")
                        Log.d("GROUP_DEBUG", "Results: ${state.data?.results}")

                        val groups = state.data?.results.orEmpty()

                        Log.d("GROUP_DEBUG", "Groups size: ${groups.size}")

                        _uiState.value = _uiState.value.copy(
                            groups = groups,
                            groupsLoaded = true
                        )

                        updateLoadingState()
                    }

                    Status.ERROR -> {

                        _uiState.value =
                            _uiState.value.copy(
                                groupsLoaded = true,
                                error = state.error
                            )

                        updateLoadingState()
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun getGroupMessages(slug: String) {

        repository.getGroupMessages(slug)
            .onEach { state ->
                when(state.status) {
                    Status.SUCCESS -> {
                        val groupMessages = state.data
                            ?.messages
                            .orEmpty()

                        _uiState.value =
                            _uiState.value.copy(
                                groupMessages = groupMessages,
                                groupMessagesLoaded = true
                            )

                        updateLoadingState()
                    }
                    Status.ERROR -> {
                        _uiState.value =
                            _uiState.value.copy(
                                groupMessagesLoaded = true,
                                error = state.error
                            )
                    }
                    Status.LOADING -> {

                    }
                }
            }.launchIn(viewModelScope)
    }

     fun getGroupDetail(slug: String) {
        repository.getGroupDetails(slug)
            .onEach { state ->
                when(state.status) {
                    Status.SUCCESS -> {
                        val groupDetail = state.data

                        _uiState.value =
                            _uiState.value.copy(
                                groupDetail = groupDetail,
                                groupDetailLoaded = true
                            )

                        updateLoadingState()
                    }
                    Status.ERROR -> {
                        _uiState.value =
                            _uiState.value.copy(
                                groupDetailLoaded = true,
                                error = state.error
                            )
                    }
                    Status.LOADING -> {

                    }
                }
            }.launchIn(viewModelScope)
    }

    fun createGroup(
        name: String,
        email: String,
        description: String,
        slug: String
    ) {
        repository.createGroup(
            name = name,
            email = email,
            description = description,
            slug = slug
        )
            .onEach { result ->

                when (result.status) {

                    Status.LOADING -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            error = null,
                            success = false
                        )
                    }

                    Status.SUCCESS -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            success = true,
                            error = null
                        )
                    }

                    Status.ERROR -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            success = false,
                            error = result.error
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}

