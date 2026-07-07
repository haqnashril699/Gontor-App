package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Task(
    val id: Long,
    val name: String,
    val category: String,
    val status: String, // "Belum Dimulai", "On Progress", "Selesai"
    val assignee: String,
    val deadline: String, // "YYYY-MM-DDTHH:MM" format
    val subtasks: List<Subtask>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class Subtask(
    val text: String,
    val done: Boolean
)

@JsonClass(generateAdapter = true)
data class TeamMember(
    val name: String,
    val nick: String,
    val ttl: String,
    val contact: String,
    val role: String,
    val hobby: String,
    val avatar: String? = ""
)

@JsonClass(generateAdapter = true)
data class Meeting(
    val episode: String? = "1",
    val time: String? = "19:00",
    val place: String? = "Sekretariat",
    val presenter: String? = "",
    val topic: String? = "",
    val notes: String? = "",
    val notify: Boolean? = false
)
