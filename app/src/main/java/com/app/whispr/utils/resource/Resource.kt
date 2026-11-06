package com.app.whispr.utils.resource

import com.app.whispr.utils.ErrorBean
import com.app.movieexplorer2.utils.resource.Status

data class Resource<out T>(
    val status: Status,
    val data: T?,
    val error: ErrorBean?
) {

    companion object {

        fun <T> success(
            data: T?
        ): Resource<T> {

            return Resource(
                status = Status.SUCCESS,
                data = data,
                error = null
            )
        }

        fun <T> error(
            error: ErrorBean
        ): Resource<T> {

            return Resource(
                status = Status.ERROR,
                data = null,
                error = error
            )
        }

        fun <T> loading(): Resource<T> {

            return Resource(
                status = Status.LOADING,
                data = null,
                error = null
            )
        }
    }
}