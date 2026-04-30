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
    val telefono: String?,
    val esDificilDesempeno: Boolean = false,
    val esRural: Boolean = false,
    val jornadaTipo: JornadaTipo = JornadaTipo.DESCONOCIDA
)

enum class JornadaTipo(val label: String) {
    CONTINUA("Continua"),
    PARTIDA("Partida"),
    DESCONOCIDA("Desconocida")
}
