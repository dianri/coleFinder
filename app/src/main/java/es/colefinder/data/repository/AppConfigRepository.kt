package es.colefinder.data.repository

import android.util.Log
import es.colefinder.BuildConfig
import es.colefinder.data.model.AppConfigKeys
import es.colefinder.data.model.AppConfigRowDto
import es.colefinder.data.model.AppUpdateConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AppConfigRepository"

private val DEFAULT_UPDATE_CONFIG = AppUpdateConfig(
    minVersionCode = 0,
    updateType = "FLEXIBLE",
)

/**
 * Parámetros remotos en `app_config` (columnas `key` / `value`).
 * Esquema según flavor: PRE → staging; PROD → public.
 */
@Singleton
class AppConfigRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {

    private companion object {
        const val TABLE_NAME = "app_config"
    }

    suspend fun getConfigMap(): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val rows: List<AppConfigRowDto> = supabase.postgrest
                .from(TABLE_NAME)
                .select(columns = Columns.list("key", "value"))
                .decodeList()

            rows.associate { it.key to it.value }
        } catch (e: Exception) {
            Log.w(TAG, "getConfigMap failed", e)
            emptyMap()
        }
    }

    suspend fun getUpdateConfig(): AppUpdateConfig = withContext(Dispatchers.IO) {
        try {
            val byKey = getConfigMap()
            val minVersionCode = byKey[AppConfigKeys.MIN_VERSION_CODE]?.toIntOrNull() ?: 0
            val updateType = byKey[AppConfigKeys.UPDATE_TYPE]
                ?.trim()
                ?.uppercase()
                ?.takeIf { it == "FLEXIBLE" || it == "IMMEDIATE" }
                ?: "FLEXIBLE"

            AppUpdateConfig(
                minVersionCode = minVersionCode,
                updateType = updateType,
            ).also {
                Log.d(
                    TAG,
                    "getUpdateConfig schema=${BuildConfig.SUPABASE_SCHEMA} → min=${it.minVersionCode} type=${it.updateType}",
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "getUpdateConfig failed, using safe defaults", e)
            DEFAULT_UPDATE_CONFIG
        }
    }
}
