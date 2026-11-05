package com.app.whispr.api


import com.app.whispr.data.GroupDetailResponseDto
import com.app.whispr.data.GroupMessagesDto
import com.app.whispr.data.GroupRequest
import com.app.whispr.data.GroupResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {

    //Get Groups
    @GET("/groups/")
    suspend fun getGroups(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<GroupResponseDto>


    //Create Groups
    @POST("/groups/")
    suspend fun createGroups(
        @Body groupRequest: GroupRequest
    ) : Response<GroupResponseDto>


    //Get Groups Details
    @GET("/groups/{slug}/")
    suspend fun getGroupDetails(
        @Path("slug") slug: String
    ) : Response<GroupDetailResponseDto>


    //Get Group Messages
    @GET("/groups/{slug}/messages")
    suspend fun getGroupMessages(
        @Path("slug") slug: String,
    ) : Response<GroupMessagesDto>
}
