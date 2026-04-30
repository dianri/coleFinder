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
    @SerialName("fuente_id")           val fuenteId: Long? = null,
    val telefono: String? = null,
    @SerialName("descripcion_entidad")     val descripcionEntidad: String? = null,
    @SerialName("tipo_centro_normalizado") val tipoCentroNormalizado: String? = null,
    @SerialName("titularidad_normalizada") val titularidadNormalizada: String? = null,
    @SerialName("es_dificil_desempeno")    val esDificilDesempeno: Boolean = false,
    @SerialName("jornada_tipo")            val jornadaTipo: String = "desconocida",
    @SerialName("es_rural")                val esRural: Boolean = false,
    @SerialName("distancia_metros")        val distanciaMetros: Double
) {
    fun toColegio(): Colegio {
        return Colegio(
            id = id,
            nombre = nombre,
            direccion = direccion ?: "",
            localidad = localidad ?: "",
            tipo = tipo ?: "",
            latitud = latitud,
            longitud = longitud,
            telefono = telefono,
            esDificilDesempeno = esDificilDesempeno,
            esRural = esRural,
            jornadaTipo = parseJornadaTipo(jornadaTipo)
        )
    }

    private fun parseJornadaTipo(value: String): JornadaTipo {
        return when (value.lowercase()) {
            "continua" -> JornadaTipo.CONTINUA
            "partida" -> JornadaTipo.PARTIDA
            else -> JornadaTipo.DESCONOCIDA
        }
    }
}
