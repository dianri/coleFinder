package es.colefinder.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Nota(
    val id: Int,
    @SerialName("created_at")
    val createdAt: String,
    val titulo: String
)
