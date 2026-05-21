package es.colefinder.data

import es.colefinder.data.repository.AppConfigRepository
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AppConfigRepositoryTest {

    @Test
    fun getUpdateConfig_parsesKeyValueRowsFromSupabase() = runBlocking {
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            assert(path.contains("app_config")) { "path=$path" }
            respond(
                content = """
                    [
                      {"key": "min_version_code", "value": "10"},
                      {"key": "update_type", "value": "IMMEDIATE"},
                      {"key": "nearby_colegios_limit", "value": "150"}
                    ]
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = createSupabaseClient(
            supabaseUrl = "https://api.test.supabase.co",
            supabaseKey = "test-key",
        ) {
            httpEngine = engine
            install(Postgrest) { defaultSchema = "public" }
        }
        val repo = AppConfigRepository(client)

        val config = repo.getUpdateConfig()

        assertEquals(10, config.minVersionCode)
        assertEquals("IMMEDIATE", config.updateType)
    }

    @Test
    fun getUpdateConfig_onNetworkError_returnsSafeDefaults() = runBlocking {
        val engine = MockEngine {
            respond(
                content = "error",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = createSupabaseClient(
            supabaseUrl = "https://api.test.supabase.co",
            supabaseKey = "test-key",
        ) {
            httpEngine = engine
            install(Postgrest) { defaultSchema = "public" }
        }
        val repo = AppConfigRepository(client)

        val config = repo.getUpdateConfig()

        assertEquals(0, config.minVersionCode)
        assertEquals("FLEXIBLE", config.updateType)
    }
}
