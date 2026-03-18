package es.colefinder.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object Supabase {
    private const val SUPABASE_URL = "https://api.dianrie.com"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiYW5vbiIsImlzcyI6InN1cGFiYXNlIiwiaWF0IjoxNzczNjM0OTcxLCJleHAiOjE5MzEzMTQ5NzF9.Z_yApwia-JIHkKnxoVKOhE_SSmC4LcsFP1T59Bu4Iyk"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Postgrest)
    }
}