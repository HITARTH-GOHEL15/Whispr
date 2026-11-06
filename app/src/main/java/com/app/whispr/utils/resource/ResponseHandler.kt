package com.app.whispr.utils.resource

import android.content.Context
import com.app.whispr.utils.ErrorBean
import com.app.whispr.utils.ErrorUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.Response
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResponseHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val errorUtils: ErrorUtils
) {

    companion object {
        const val TAG = "ResponseHandler"
    }

    fun <T> handleResponse(
        response: Response<T>,
        isAutoHandleError: Boolean = true
    ): Resource<T> {

        if (response.isSuccessful) {

            val responseBody = response.body()

            return if (response.code() == 204) {

                Resource.success(null)

            } else {

                if (responseBody != null) {

                    Resource.success(responseBody)

                } else {

                    Resource.error(
                        errorUtils.handleError(
                            IllegalStateException(
                                "Unexpected error"
                            ),
                            ""
                        )
                    )
                }
            }
        }

        return when (response.code()) {

            400, 422 -> {

                handleClientError(
                    response,
                    isAutoHandleError
                )
            }

            401 -> {

                Resource.error(
                    ErrorBean(
                        errorCode = 401,
                        errorMessage = "Unauthorized"
                    )
                )
            }

            403 -> {

                Resource.error(
                    ErrorBean(
                        errorCode = 403,
                        errorMessage = "Access denied"
                    )
                )
            }

            404 -> {

                Resource.error(
                    ErrorBean(
                        errorCode = 404,
                        errorMessage = "data not found"
                    )
                )
            }

            408 -> {

                Resource.error(
                    errorUtils.handleError(
                        SocketTimeoutException(),
                        ""
                    )
                )
            }

            500, 502, 503, 504 -> {

                Resource.error(
                    ErrorBean(
                        errorCode = response.code(),
                        errorMessage = "Something Went Wrong"
                    )
                )
            }

            else -> {

                Resource.error(
                    errorUtils.handleError(
                        null,
                        ""
                    )
                )
            }
        }
    }

    private fun <T> handleClientError(
        response: Response<T>,
        isAutoHandleError: Boolean
    ): Resource<T> {

        val errorBody = response.errorBody()?.string()

        if (!errorBody.isNullOrEmpty()) {

            return if (isAutoHandleError) {

                Resource.error(
                    errorUtils.handleMapError(
                        errorBody
                    )
                )

            } else {

                Resource.error(
                    ErrorBean(
                        errorCode = response.code(),
                        error = errorBody
                    )
                )
            }
        }

        return Resource.error(
            errorUtils.handleError(
                null,
                ""
            )
        )
    }

    fun <T> handleException(
        e: Throwable?
    ): Resource<T> {

        return Resource.error(
            errorUtils.handleError(
                e,
                ""
            )
        )
    }
}
