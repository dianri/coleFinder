package es.colefinder.data.model

import kotlinx.serialization.Serializable

/**
 * DTO ligero para la busqueda de centros por nombre.
 * Solo incluye los campos necesarios para mostrar sugerencias y centrar el mapa.
 */
@Serializable
data class ColegioSearchDto(
    val id: Int,
    val nombre: String,
    val localidad: String? = null,
    val latitud: Double,
    val longitud: Double
)
