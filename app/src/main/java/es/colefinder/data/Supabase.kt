package es.colefinder.data

import es.colefinder.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.DEFAULT

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
        if (BuildConfig.DEBUG) {
            defaultLogLevel = io.github.jan.supabase.logging.LogLevel.DEBUG
        }
        install(Postgrest) {
            defaultSchema = BuildConfig.SUPABASE_SCHEMA
        }
    }
}