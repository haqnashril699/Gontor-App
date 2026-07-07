package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

interface FirebaseApi {
    @GET("tasks.json")
    suspend fun getTasks(): Map<String, Task>?

    @PUT("tasks/{id}.json")
    suspend fun putTask(@Path("id") id: String, @Body task: Task): Task

    @DELETE("tasks/{id}.json")
    suspend fun deleteTask(@Path("id") id: String): Any?

    @GET("teamMembers.json")
    suspend fun getTeamMembers(): List<TeamMember>?

    @PUT("teamMembers.json")
    suspend fun putTeamMembers(@Body members: List<TeamMember>): List<TeamMember>

    @GET("categories.json")
    suspend fun getCategories(): List<String>?

    @PUT("categories.json")
    suspend fun putCategories(@Body categories: List<String>): List<String>

    @GET("meetings.json")
    suspend fun getMeetings(): Map<String, Meeting>?

    @PUT("meetings/{date}.json")
    suspend fun putMeeting(@Path("date") date: String, @Body meeting: Meeting): Meeting

    @GET("calendarNotes.json")
    suspend fun getCalendarNotes(): Map<String, String>?

    @PUT("calendarNotes.json")
    suspend fun putCalendarNotes(@Body notes: Map<String, String>): Map<String, String>
}

object FirebaseService {
    private const val BASE_URL = "https://gontorpayinventory-cloud-default-rtdb.asia-southeast1.firebasedatabase.app/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val api: FirebaseApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FirebaseApi::class.java)
    }
}
