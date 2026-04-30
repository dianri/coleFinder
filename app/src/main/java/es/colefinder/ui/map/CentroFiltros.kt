package es.colefinder.ui.map

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme

// ── Titularidad ───────────────────────────────────────────────────────────────

enum class TitularidadFiltro(val label: String) {
    TODOS("Todos"),
    PUBLICO("Público"),
    CONCERTADO("Concertado"),
    PRIVADO("Privado");

    /**
     * Valor para el parámetro p_titularidad de la RPC.
     * Debe coincidir exactamente con titularidad_normalizada en BD: PUBLICO, CONCERTADO, PRIVADO.
     * Devuelve null when no hay restricción (TODOS).
     */
    fun toRpcParam(): String? = when (this) {
        TODOS      -> null
        PUBLICO    -> "PUBLICO"
        CONCERTADO -> "CONCERTADO"
        PRIVADO    -> "PRIVADO"
    }
}

// ── Tipo de centro ────────────────────────────────────────────────────────────

enum class TipoCentroFiltro(val label: String) {
    TODOS("Todos"),
    PRIMARIA("Primaria"),
    SECUNDARIA("Secundaria"),
    FP("FP"),
    ADULTOS("Adultos"),
    ESPECIAL("Especial"),
    OTROS("Otros");

    /**
     * Valor para el parámetro p_tipo de la RPC.
     * Debe coincidir exactamente con tipo_centro_normalizado en BD.
     * Devuelve null when no hay restricción (TODOS).
     */
    fun toRpcParam(): String? = when (this) {
        TODOS      -> null
        PRIMARIA   -> "PRIMARIA"
        SECUNDARIA -> "SECUNDARIA"
        FP         -> "FP"
        ADULTOS    -> "ADULTOS"
        ESPECIAL   -> "ESPECIAL"
        OTROS      -> "OTROS"
    }
}

enum class TipoCentroClasificado { PRIMARIA, SECUNDARIA, FP, ADULTOS, ESPECIAL, OTROS }

/** Etiqueta legible para mostrar en la card de detalle. */
val TipoCentroClasificado.label: String get() = when (this) {
    TipoCentroClasificado.PRIMARIA   -> "Primaria"
    TipoCentroClasificado.SECUNDARIA -> "Secundaria"
    TipoCentroClasificado.FP         -> "FP"
    TipoCentroClasificado.ADULTOS    -> "Adultos"
    TipoCentroClasificado.ESPECIAL   -> "Especial"
    TipoCentroClasificado.OTROS      -> "Otros"
}

/**
 * Convierte el valor normalizado de BD (tipo_centro_normalizado) al enum local.
 * Si el valor es nulo o desconocido devuelve null para que el llamador aplique fallback.
 */
fun parseTipoCentroNormalizado(value: String?): TipoCentroClasificado? = when (value?.uppercase()) {
    "PRIMARIA"   -> TipoCentroClasificado.PRIMARIA
    "SECUNDARIA" -> TipoCentroClasificado.SECUNDARIA
    "FP"         -> TipoCentroClasificado.FP
    "ADULTOS"    -> TipoCentroClasificado.ADULTOS
    "ESPECIAL"   -> TipoCentroClasificado.ESPECIAL
    "OTROS"      -> TipoCentroClasificado.OTROS
    else         -> null
}

/**
 * Clasifica un centro en dos capas:
 *  1. tipo_centro_normalizado: valor de BD, fuente preferente.
 *  2. descripcion_entidad: campo estructurado fiable cuando está disponible.
 *  3. Siglas al inicio del nombre: CEIP, IES, CIFP, etc.
 *  4. Si hay duda → OTROS.
 */
fun clasificarTipoCentro(
    nombre: String,
    tipo: String,
    descripcionEntidad: String?,
    tipoCentroNormalizado: String? = null
): TipoCentroClasificado =
    parseTipoCentroNormalizado(tipoCentroNormalizado)
        ?: clasificarPorDescripcion(descripcionEntidad)
        ?: clasificarPorNombre(nombre)

/**
 * Clasifica usando descripcion_entidad.
 * Devuelve null si el campo no contiene información útil (vacío, email, muy corto).
 */
private fun clasificarPorDescripcion(descripcion: String?): TipoCentroClasificado? {
    if (!esMeaningfulDescripcion(descripcion)) return null
    val d = descripcion!!.lowercase()
    return when {
        d.contains("primaria")                                      -> TipoCentroClasificado.PRIMARIA
        d.contains("secundaria") || d.contains("bachillerato")      -> TipoCentroClasificado.SECUNDARIA
        d.contains("formación profesional") ||
            d.contains("formacion profesional") ||
            d.contains("ciclos formativos")                         -> TipoCentroClasificado.FP
        d.contains("permanente") || d.contains("adultos")           -> TipoCentroClasificado.ADULTOS
        d.contains("especial")                                      -> TipoCentroClasificado.ESPECIAL
        else                                                        -> null
    }
}

/** Devuelve false si el texto es nulo, vacío, un email o demasiado corto para ser útil. */
private fun esMeaningfulDescripcion(descripcion: String?): Boolean {
    if (descripcion.isNullOrBlank()) return false
    if (descripcion.contains('@')) return false
    return descripcion.length >= 10
}

/**
 * Clasifica por las siglas al inicio del nombre.
 * Solo usamos siglas inequívocas; el resto va a OTROS.
 * No clasificamos por la palabra "Colegio" sola porque en Madrid es un nombre propio.
 */
private fun clasificarPorNombre(nombre: String): TipoCentroClasificado {
    val n = nombre.trim().lowercase()
    return when {
        n.startsWith("ceip ") || n.startsWith("ceip.") ||
            n.startsWith("cra ") || n.startsWith("cra.") ||
            n.startsWith("cep ")                                    -> TipoCentroClasificado.PRIMARIA

        n.startsWith("ies ") || n.startsWith("ies.") ||
            n.startsWith("i.e.s") || n.startsWith("ieso ") ||
            n.startsWith("instituto ")                              -> TipoCentroClasificado.SECUNDARIA

        n.startsWith("cifp ") || n.startsWith("cifp.") ||
            n.startsWith("cipfp ")                                  -> TipoCentroClasificado.FP

        n.startsWith("cepa ") || n.startsWith("cepa.")             -> TipoCentroClasificado.ADULTOS

        n.startsWith("cee ") || n.startsWith("cee.")               -> TipoCentroClasificado.ESPECIAL

        else                                                        -> TipoCentroClasificado.OTROS
    }
}

fun TipoCentroClasificado.matchesFiltro(filtro: TipoCentroFiltro): Boolean = when (filtro) {
    TipoCentroFiltro.TODOS      -> true
    TipoCentroFiltro.PRIMARIA   -> this == TipoCentroClasificado.PRIMARIA
    TipoCentroFiltro.SECUNDARIA -> this == TipoCentroClasificado.SECUNDARIA
    TipoCentroFiltro.FP         -> this == TipoCentroClasificado.FP
    TipoCentroFiltro.ADULTOS    -> this == TipoCentroClasificado.ADULTOS
    TipoCentroFiltro.ESPECIAL   -> this == TipoCentroClasificado.ESPECIAL
    TipoCentroFiltro.OTROS      -> this == TipoCentroClasificado.OTROS
}

/** Devuelve true si la clasificación coincide con cualquiera de los filtros del set. */
fun TipoCentroClasificado.matchesFiltros(filtros: Set<TipoCentroFiltro>): Boolean =
    TipoCentroFiltro.TODOS in filtros || filtros.any { this.matchesFiltro(it) }

/** Devuelve true si el campo tipo del centro coincide con alguna titularidad del set. */
fun String.matchesTitularidadFiltros(filtros: Set<TitularidadFiltro>): Boolean =
    TitularidadFiltro.TODOS in filtros || filtros.any { filtro ->
        when (filtro) {
            TitularidadFiltro.TODOS      -> true
            TitularidadFiltro.PUBLICO    -> contains("Público",    ignoreCase = true)
            TitularidadFiltro.CONCERTADO -> contains("Concertado", ignoreCase = true)
            TitularidadFiltro.PRIVADO    -> contains("Privado",    ignoreCase = true)
        }
    }

/**
 * Matching de titularidad usando el valor normalizado de BD.
 * Más fiable que matchesTitularidadFiltros porque no depende del texto libre.
 * Si el valor normalizado es nulo, delega al texto libre como fallback.
 */
fun matchesTitularidadNormalizadaFiltros(
    titularidadNormalizada: String?,
    tipoRaw: String,
    filtros: Set<TitularidadFiltro>
): Boolean {
    if (TitularidadFiltro.TODOS in filtros) return true
    val norm = titularidadNormalizada?.uppercase()
    return if (norm != null) {
        filtros.any { filtro ->
            when (filtro) {
                TitularidadFiltro.TODOS      -> true
                TitularidadFiltro.PUBLICO    -> norm == "PUBLICO"
                TitularidadFiltro.CONCERTADO -> norm == "CONCERTADO"
                TitularidadFiltro.PRIVADO    -> norm == "PRIVADO"
            }
        }
    } else {
        tipoRaw.matchesTitularidadFiltros(filtros)
    }
}

// ── Lógica de toggle para multiselección ─────────────────────────────────────

/**
 * Genera el array JSON para el parámetro p_titularidades de la RPC.
 * Si el set contiene TODOS o está vacío, devuelve null (sin restricción en BD).
 * Si tiene opciones concretas, devuelve un JsonArray con sus valores normalizados.
 */
@JvmName("titularidadToRpcArray")
fun Set<TitularidadFiltro>.toRpcArray(): JsonArray? {
    val concretos = this.filter { it != TitularidadFiltro.TODOS }
    if (concretos.isEmpty()) return null
    return buildJsonArray { concretos.forEach { add(it.toRpcParam()!!) } }
}

/**
 * Genera el array JSON para el parámetro p_tipos de la RPC.
 * Si el set contiene TODOS o está vacío, devuelve null (sin restricción en BD).
 * Si tiene opciones concretas, devuelve un JsonArray con sus valores normalizados.
 */
@JvmName("tipoCentroToRpcArray")
fun Set<TipoCentroFiltro>.toRpcArray(): JsonArray? {
    val concretos = this.filter { it != TipoCentroFiltro.TODOS }
    if (concretos.isEmpty()) return null
    return buildJsonArray { concretos.forEach { add(it.toRpcParam()!!) } }
}

/**
 * Reglas de toggle:
 * - Si se pulsa TODOS → activa solo TODOS.
 * - Si TODOS está activo y se pulsa otro → activa solo ese.
 * - Si el valor ya estaba activo → se desactiva; si queda vacío → activa TODOS.
 * - Si el valor no estaba activo → se añade al set (sin TODOS).
 */
fun toggleTitularidad(
    current: Set<TitularidadFiltro>,
    toggled: TitularidadFiltro
): Set<TitularidadFiltro> = toggleFiltro(current, toggled, TitularidadFiltro.TODOS)

fun toggleTipoCentro(
    current: Set<TipoCentroFiltro>,
    toggled: TipoCentroFiltro
): Set<TipoCentroFiltro> = toggleFiltro(current, toggled, TipoCentroFiltro.TODOS)

private fun <T> toggleFiltro(current: Set<T>, toggled: T, todosValue: T): Set<T> {
    if (toggled == todosValue) return setOf(todosValue)
    val sinTodos = current - todosValue
    val nuevo = if (toggled in sinTodos) sinTodos - toggled else sinTodos + toggled
    return if (nuevo.isEmpty()) setOf(todosValue) else nuevo
}

// ── Colores (fuente única de verdad para chips Y marcadores) ──────────────────

val ColorFiltroTodos    = Color(0xFF546E7A) // blue-grey neutro
val ColorPublico        = Color(0xFF4CAF50) // verde
val ColorConcertado     = Color(0xFF2196F3) // azul
val ColorPrivado        = Color(0xFFE65100) // naranja oscuro

val ColorChipPrimaria   = Color(0xFFF57F17) // ámbar oscuro
val ColorChipSecundaria = Color(0xFFC62828) // granate
val ColorChipFP         = Color(0xFF00897B) // teal
val ColorChipAdultos    = Color(0xFF6A1B9A) // púrpura
val ColorChipEspecial   = Color(0xFFBF360C) // naranja quemado
val ColorChipOtros      = Color(0xFF757575) // gris

@Composable
fun colorFondoDestacado(): Color =
    if (isSystemInDarkTheme()) Color(0xFF332020) // Fondo oscuro muy sutil, ligeramente granate
    else Color(0xFFFFEBEE)

@Composable
fun colorFondoChipDestacado(): Color =
    if (isSystemInDarkTheme()) Color(0xFF4A2A2A) // Un poco más de contraste para el chip
    else Color(0xFFFFCDD2)

@Composable
fun colorTextoRural(): Color =
    if (isSystemInDarkTheme()) Color(0xFFA5D6A7) // Verde pastel brillante para dark mode
    else Color(0xFF1B5E20)

@Composable
fun colorTextoDificil(): Color =
    if (isSystemInDarkTheme()) Color(0xFFEF9A9A) // Rojo suave brillante para dark mode
    else Color(0xFFB71C1C)

/**
 * Color de marcador/badge.
 * Usa titularidadNormalizada de BD como fuente preferente; cae a tipo (texto libre) si es nulo.
 */
fun colorParaTitularidad(tipo: String, titularidadNormalizada: String? = null): Color {
    val norm = titularidadNormalizada?.uppercase()
    return when {
        norm == "PUBLICO"                              -> ColorPublico
        norm == "CONCERTADO"                           -> ColorConcertado
        norm == "PRIVADO"                              -> ColorPrivado
        tipo.contains("Público",    ignoreCase = true) -> ColorPublico
        tipo.contains("Concertado", ignoreCase = true) -> ColorConcertado
        tipo.contains("Privado",    ignoreCase = true) -> ColorPrivado
        else                                           -> ColorConcertado
    }
}

/** Color del chip de tipo para la card de detalle. Reutiliza colorParaTipoCentroFiltro. */
fun colorParaTipoCentroClasificado(tipo: TipoCentroClasificado): Color = when (tipo) {
    TipoCentroClasificado.PRIMARIA   -> ColorChipPrimaria
    TipoCentroClasificado.SECUNDARIA -> ColorChipSecundaria
    TipoCentroClasificado.FP         -> ColorChipFP
    TipoCentroClasificado.ADULTOS    -> ColorChipAdultos
    TipoCentroClasificado.ESPECIAL   -> ColorChipEspecial
    TipoCentroClasificado.OTROS      -> ColorChipOtros
}

/** Etiqueta de titularidad legible a partir del campo normalizado de BD o del tipo libre. */
fun labelParaTitularidad(titularidadNormalizada: String?, tipoRaw: String): String =
    when (titularidadNormalizada?.uppercase()) {
        "PUBLICO"    -> "Público"
        "CONCERTADO" -> "Concertado"
        "PRIVADO"    -> "Privado"
        else         -> tipoRaw.ifBlank { "Desconocido" }
    }

fun colorParaTitularidadFiltro(filtro: TitularidadFiltro): Color = when (filtro) {
    TitularidadFiltro.TODOS      -> ColorFiltroTodos
    TitularidadFiltro.PUBLICO    -> ColorPublico
    TitularidadFiltro.CONCERTADO -> ColorConcertado
    TitularidadFiltro.PRIVADO    -> ColorPrivado
}

fun colorParaTipoCentroFiltro(filtro: TipoCentroFiltro): Color = when (filtro) {
    TipoCentroFiltro.TODOS      -> ColorFiltroTodos
    TipoCentroFiltro.PRIMARIA   -> ColorChipPrimaria
    TipoCentroFiltro.SECUNDARIA -> ColorChipSecundaria
    TipoCentroFiltro.FP         -> ColorChipFP
    TipoCentroFiltro.ADULTOS    -> ColorChipAdultos
    TipoCentroFiltro.ESPECIAL   -> ColorChipEspecial
    TipoCentroFiltro.OTROS      -> ColorChipOtros
}
