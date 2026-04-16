package es.colefinder.data

import es.colefinder.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlin.time.Duration.Companion.seconds

/**
 * Singleton del cliente Supabase.
 * Las credenciales se leen de BuildConfig, generadas por Secrets Gradle Plugin
 * a partir de secrets.properties (local, NO versionado).
 *
 * Rol usado: `anon` (publishable key). La tabla colegios está protegida por RLS.
 * NUNCA se debe usar aquí service_role ni ninguna clave admin.
 */
object Supabase {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        // supabase-kt aplica este valor como requestTimeoutMillis en Ktor (por defecto 10s).
        // Redes móviles lentas (DNS/TLS) suelen beneficiarse de un margen algo mayor.
        requestTimeout = 25.seconds
        if (BuildConfig.DEBUG) {
            defaultLogLevel = io.github.jan.supabase.logging.LogLevel.DEBUG
        }
        install(Postgrest) {
            defaultSchema = BuildConfig.SUPABASE_SCHEMA
        }
    }
}