package es.colefinder.data

import es.colefinder.BuildConfig
import es.colefinder.data.repository.SupabaseColegioRepository
import es.colefinder.ui.map.TipoCentroFiltro
import es.colefinder.ui.map.TitularidadFiltro
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun MockRequestHandleScope.jsonOk(body: String) = respond(
    content = body,
    status = HttpStatusCode.OK,
    headers = headersOf(HttpHeaders.ContentType, "application/json")
)

/**
 * Tests de integración ligera del cliente: [SupabaseColegioRepository] (implementación real de
 * [es.colefinder.data.repository.ColegioRepository]) con [MockEngine] de Ktor.
 */
class ColegioRepositoryTest {

    private companion object {
        const val TEST_SUPABASE_URL = "https://api.test.supabase.co"
        const val TEST_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test-anon-key"

        /** JSON alineado con la RPC PostgREST `nearby_colegios` y [es.colefinder.data.model.NearbyColegioDto]. */
        val JSON_TWO_NEARBY = """
            [
              {
                "id": 101,
                "nombre": "CEIP Número Uno",
                "direccion": "Calle Mayor 10",
                "latitud": 40.4165,
                "longitud": -3.7026,
                "tipo": "Centro público",
                "localidad": "Madrid",
                "provincia": "Madrid",
                "telefono": "915551010",
                "descripcion_entidad": null,
                "tipo_centro_normalizado": "PRIMARIA",
                "titularidad_normalizada": "PUBLICO",
                "es_dificil_desempeno": false,
                "jornada_tipo": "continua",
                "es_rural": false,
                "distancia_metros": 125.5
              },
              {
                "id": 202,
                "nombre": "IES Dos de Mayo",
                "direccion": "Avenida de América 2",
                "latitud": 40.4381,
                "longitud": -3.6762,
                "tipo": "Concertado",
                "localidad": "Madrid",
                "provincia": "Madrid",
                "telefono": null,
                "tipo_centro_normalizado": "SECUNDARIA",
                "titularidad_normalizada": "CONCERTADO",
                "es_dificil_desempeno": false,
                "jornada_tipo": "partida",
                "es_rural": false,
                "distancia_metros": 890.0
              }
            ]
        """.trimIndent()

        val JSON_SEARCH_ONE = """
            [
              {
                "id": 501,
                "nombre": "IES Madrid Búsqueda",
                "localidad": "Madrid",
                "latitud": 40.42,
                "longitud": -3.70
              }
            ]
        """.trimIndent()
    }

    private fun supabaseWithEngine(engine: MockEngine) = createSupabaseClient(TEST_SUPABASE_URL, TEST_ANON_KEY) {
        httpEngine = engine
        install(Postgrest) {
            defaultSchema = BuildConfig.SUPABASE_SCHEMA
        }
    }

    private fun repository(engine: MockEngine) = SupabaseColegioRepository(supabaseWithEngine(engine))

    private fun isNearbyRpc(path: String): Boolean = "nearby_colegios" in path

    /** Select sobre tabla `colegios` (no confundir con `/rpc/...`). */
    private fun isColegiosRest(path: String): Boolean =
        "/colegios" in path && "rpc" !in path

    @Test
    fun fetchNearbyColegios_respuestaExitosa_devuelveListaColegios() = runBlocking {
        // Given
        val engine = MockEngine { request ->
            when {
                isNearbyRpc(request.url.encodedPath) -> jsonOk(JSON_TWO_NEARBY)
                else -> error("Unexpected request: ${request.method} ${request.url}")
            }
        }
        val repo = repository(engine)
        // When
        val result = repo.fetchNearbyColegios(
            lat = 40.4168,
            lon = -3.7038,
            titularidades = setOf(TitularidadFiltro.TODOS),
            tipos = setOf(TipoCentroFiltro.TODOS)
        )
        // Then
        assertTrue(result.isSuccess)
        val list = result.getOrNull()!!
        assertEquals(2, list.size)
        val first = list[0].colegio
        val second = list[1].colegio
        assertEquals(101, first.id)
        assertEquals("CEIP Número Uno", first.nombre)
        assertEquals(40.4165, first.latitud, 1e-9)
        assertEquals(-3.7026, first.longitud, 1e-9)
        assertEquals(202, second.id)
        assertEquals("IES Dos de Mayo", second.nombre)
        assertEquals(40.4381, second.latitud, 1e-9)
        assertEquals(-3.6762, second.longitud, 1e-9)
    }

    @Test
    fun fetchNearbyColegios_respuestaVacia_devuelveListaVacia() = runBlocking {
        // Given
        val engine = MockEngine { request ->
            when {
                isNearbyRpc(request.url.encodedPath) -> jsonOk("[]")
                else -> error("Unexpected request: ${request.method} ${request.url}")
            }
        }
        val repo = repository(engine)
        // When
        val result = repo.fetchNearbyColegios(
            lat = 40.0,
            lon = -3.0,
            titularidades = setOf(TitularidadFiltro.TODOS),
            tipos = setOf(TipoCentroFiltro.TODOS)
        )
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull().isNullOrEmpty())
    }

    @Test
    fun fetchNearbyColegios_errorDeRed_devuelveFailure() = runBlocking {
        // Given
        val engine = MockEngine { request ->
            when {
                isNearbyRpc(request.url.encodedPath) -> throw IOException("Sin red simulada")
                else -> error("Unexpected request: ${request.method} ${request.url}")
            }
        }
        val repo = repository(engine)
        // When
        val result = repo.fetchNearbyColegios(
            lat = 40.0,
            lon = -3.0,
            titularidades = setOf(TitularidadFiltro.TODOS),
            tipos = setOf(TipoCentroFiltro.TODOS)
        )
        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun searchColegiosByName_conQuery_devuelveColegiosCoincidentes() = runBlocking {
        // Given
        val engine = MockEngine { request ->
            when {
                isColegiosRest(request.url.encodedPath) -> jsonOk(JSON_SEARCH_ONE)
                else -> error("Unexpected request: ${request.method} ${request.url}")
            }
        }
        val repo = repository(engine)
        // When
        val result = repo.searchColegiosByName(query = "Madrid", limit = 8)
        // Then
        assertTrue(result.isSuccess)
        val list = result.getOrNull()!!
        assertEquals(1, list.size)
        assertTrue(list[0].nombre.contains("Madrid", ignoreCase = true))
    }

    @Test
    fun searchColegiosByName_errorServidor_devuelveFailure() = runBlocking {
        // Given
        val engine = MockEngine { request ->
            when {
                isColegiosRest(request.url.encodedPath) -> respond(
                    content = """{"message":"internal error"}""",
                    status = HttpStatusCode.InternalServerError,
                    headers = headersOf(HttpHeaders.ContentType, listOf("application/json"))
                )
                else -> error("Unexpected request: ${request.method} ${request.url}")
            }
        }
        val repo = repository(engine)
        // When
        val result = repo.searchColegiosByName(query = "test", limit = 8)
        // Then
        assertTrue(result.isFailure)
    }
}
