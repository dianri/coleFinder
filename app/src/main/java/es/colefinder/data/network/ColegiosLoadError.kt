package es.colefinder.data.network

import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.NotFoundRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.github.jan.supabase.exceptions.UnknownRestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Categoría de fallo al cargar colegios (logs / diagnóstico).
 * No expuesta directamente al usuario.
 */
enum class ColegiosLoadCategory {
    DNS,
    TIMEOUT,
    SSL,
    CONNECTION_REFUSED,
    HTTP_CLIENT,
    HTTP_SERVER,
    NETWORK_GENERIC,
    UNKNOWN
}

data class ClassifiedColegiosLoadFailure(
    val category: ColegiosLoadCategory,
    /** Texto para Logcat (incluye pistas técnicas). */
    val technicalDetail: String,
    /** Mensaje corto y estable para mostrar en UI. */
    val userMessage: String
)

/**
 * Excepción envuelta en [Result.failure] desde [es.colefinder.data.repository.SupabaseColegioRepository]
 * para transportar mensaje de usuario sin perder la causa original en la cadena.
 */
class ColegiosLoadException(
    val category: ColegiosLoadCategory,
    val userMessage: String,
    message: String,
    cause: Throwable
) : Exception(message, cause)

private fun Throwable.asSequenceWithSelf(): Sequence<Throwable> =
    generateSequence(this) { it.cause }

/**
 * Clasifica errores de la RPC / red. [HttpRequestException] de supabase-kt no encadena [cause];
 * en ese caso se usan heurísticas sobre el mensaje.
 */
fun classifyColegiosLoadFailure(throwable: Throwable): ClassifiedColegiosLoadFailure {
    for (t in throwable.asSequenceWithSelf()) {
        when (t) {
            is HttpRequestTimeoutException -> {
                return ClassifiedColegiosLoadFailure(
                    category = ColegiosLoadCategory.TIMEOUT,
                    technicalDetail = "HttpRequestTimeoutException: ${t.message}",
                    userMessage = "La conexión tardó demasiado. Prueba otra red o más tarde."
                )
            }
            is UnknownHostException -> {
                return ClassifiedColegiosLoadFailure(
                    category = ColegiosLoadCategory.DNS,
                    technicalDetail = "UnknownHostException: ${t.message}",
                    userMessage = "No se pudo contactar con el servidor. Revisa datos móviles o DNS."
                )
            }
            is SocketTimeoutException -> {
                return ClassifiedColegiosLoadFailure(
                    category = ColegiosLoadCategory.TIMEOUT,
                    technicalDetail = "SocketTimeoutException: ${t.message}",
                    userMessage = "Tiempo de espera agotado. Prueba otra red."
                )
            }
            is SSLException -> {
                return ClassifiedColegiosLoadFailure(
                    category = ColegiosLoadCategory.SSL,
                    technicalDetail = "SSLException: ${t.message}",
                    userMessage = "Error de seguridad en la conexión (SSL). Revisa fecha del dispositivo o la red."
                )
            }
            is ConnectException -> {
                return ClassifiedColegiosLoadFailure(
                    category = ColegiosLoadCategory.CONNECTION_REFUSED,
                    technicalDetail = "ConnectException: ${t.message}",
                    userMessage = "No se pudo conectar al servidor. Comprueba la red."
                )
            }
        }
    }

    when (val e = throwable) {
        is UnauthorizedRestException, is BadRequestRestException -> {
            return ClassifiedColegiosLoadFailure(
                category = ColegiosLoadCategory.HTTP_CLIENT,
                technicalDetail = "Rest ${e::class.simpleName} status=${e.statusCode} error=${e.error} desc=${e.description}",
                userMessage = "No se pudo cargar la lista de centros (error del cliente)."
            )
        }
        is NotFoundRestException -> {
            return ClassifiedColegiosLoadFailure(
                category = ColegiosLoadCategory.HTTP_CLIENT,
                technicalDetail = "NotFoundRestException status=${e.statusCode} error=${e.error}",
                userMessage = "Servicio no disponible en el servidor. Comprueba la configuración."
            )
        }
        is UnknownRestException -> {
            val kind = if (e.statusCode in 500..599) ColegiosLoadCategory.HTTP_SERVER else ColegiosLoadCategory.HTTP_CLIENT
            return ClassifiedColegiosLoadFailure(
                category = kind,
                technicalDetail = "UnknownRestException status=${e.statusCode} error=${e.error} desc=${e.description}",
                userMessage = if (kind == ColegiosLoadCategory.HTTP_SERVER) {
                    "El servidor no respondió bien. Inténtalo más tarde."
                } else {
                    "No se pudo completar la solicitud al servidor."
                }
            )
        }
        is RestException -> {
            return ClassifiedColegiosLoadFailure(
                category = ColegiosLoadCategory.HTTP_CLIENT,
                technicalDetail = "RestException status=${e.statusCode} error=${e.error} desc=${e.description}",
                userMessage = "No se pudo cargar la lista de centros."
            )
        }
        is HttpRequestException -> {
            val msg = e.message.orEmpty()
            val fromMessage = classifyFromHttpRequestMessage(msg)
            if (fromMessage != null) return fromMessage.copy(
                technicalDetail = "HttpRequestException: $msg"
            )
            return ClassifiedColegiosLoadFailure(
                category = ColegiosLoadCategory.NETWORK_GENERIC,
                technicalDetail = "HttpRequestException: $msg",
                userMessage = "Problema de red al cargar centros. Prueba Wi‑Fi o datos."
            )
        }
        is IOException -> {
            return ClassifiedColegiosLoadFailure(
                category = ColegiosLoadCategory.NETWORK_GENERIC,
                technicalDetail = "IOException: ${e.message}",
                userMessage = "Error de red al cargar centros."
            )
        }
    }

    return ClassifiedColegiosLoadFailure(
        category = ColegiosLoadCategory.UNKNOWN,
        technicalDetail = "${throwable::class.simpleName}: ${throwable.message}",
        userMessage = "No se pudieron cargar los centros. Inténtalo de nuevo."
    )
}

private fun classifyFromHttpRequestMessage(message: String): ClassifiedColegiosLoadFailure? {
    val m = message.lowercase()
    return when {
        "timeout" in m || "timed out" in m ->
            ClassifiedColegiosLoadFailure(
                ColegiosLoadCategory.TIMEOUT,
                message,
                "La conexión tardó demasiado. Prueba otra red o más tarde."
            )
        "unable to resolve host" in m || "unknownhost" in m ->
            ClassifiedColegiosLoadFailure(
                ColegiosLoadCategory.DNS,
                message,
                "No se pudo contactar con el servidor. Revisa datos móviles o DNS."
            )
        "ssl" in m || "certificate" in m || "cert " in m ->
            ClassifiedColegiosLoadFailure(
                ColegiosLoadCategory.SSL,
                message,
                "Error de seguridad en la conexión (SSL)."
            )
        "connection refused" in m || "failed to connect" in m || "econnrefused" in m ->
            ClassifiedColegiosLoadFailure(
                ColegiosLoadCategory.CONNECTION_REFUSED,
                message,
                "No se pudo conectar al servidor."
            )
        "network is unreachable" in m || "no route to host" in m ->
            ClassifiedColegiosLoadFailure(
                ColegiosLoadCategory.NETWORK_GENERIC,
                message,
                "Red no disponible. Comprueba datos o Wi‑Fi."
            )
        else -> null
    }
}
