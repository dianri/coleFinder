package es.colefinder.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class SupabaseHeaderTest {

    @Test
    fun `test rpc call sends correct schema headers`() = runBlocking {
        var capturedUrl: String? = null
        var capturedHeaders: Headers? = null

        val mockEngine = MockEngine { request ->
            capturedUrl = request.url.toString()
            capturedHeaders = request.headers
            respond(
                content = "[]",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = createSupabaseClient("https://api.example.com", "fake-key") {
            httpEngine = mockEngine
            install(Postgrest) {
                defaultSchema = "staging"
            }
        }

        // Simular la llamada del repositorio
        client.postgrest
            .rpc("nearby_colegios") {
                schema = "staging"
            }
            .decodeList<kotlinx.serialization.json.JsonElement>()

        println("URL: $capturedUrl")
        capturedHeaders?.forEach { name, values ->
            println("Header: $name = ${values.joinToString()}")
        }

        assertNotNull(capturedHeaders)
        // PostgREST RPC via POST typically requires Content-Profile for the execution schema
        // and Accept-Profile for the return schema. supabase-kt should send both or at least one.
        val hasContentProfile = capturedHeaders?.contains("Content-Profile") ?: false
        val hasAcceptProfile = capturedHeaders?.contains("Accept-Profile") ?: false

        assertTrue("Should contain either Content-Profile or Accept-Profile with 'staging'", 
            (capturedHeaders?.get("Content-Profile") == "staging") || (capturedHeaders?.get("Accept-Profile") == "staging")
        )
    }
}
