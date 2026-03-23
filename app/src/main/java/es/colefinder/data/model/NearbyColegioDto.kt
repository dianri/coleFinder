package es.colefinder.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NearbyColegioDto(
    val id: Int,
    val nombre: String,
    val direccion: String? = null,
    val latitud: Double,
    val longitud: Double,
    val tipo: String? = null,
    val localidad: String? = null,
    val provincia: String? = null,
    val fuente: String? = null,
    @SerialName("fuente_id")        val fuenteId: Long? = null,
    val telefono: String? = null,
    @SerialName("distancia_metros") val distanciaMetros: Double
) {
    fun toColegio() = Colegio(
        id        = id,
        nombre    = nombre,
        direccion = direccion ?: "",
        latitud   = latitud,
        longitud  = longitud,
        tipo      = tipo ?: "",
        localidad = localidad ?: "",
        telefono  = telefono
    )
}
