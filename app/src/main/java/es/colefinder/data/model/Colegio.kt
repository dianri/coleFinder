package es.colefinder.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Colegio(
    val id: Int,
    val nombre: String,
    val direccion: String,
    val latitud: Double,
    val longitud: Double,
    val tipo: String,
    val localidad: String,
    val telefono: String? = null,
    val esDificilDesempeno: Boolean = false
)
