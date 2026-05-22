package es.colefinder.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AppConfigRowDto(
    val key: String,
    val value: String,
)
