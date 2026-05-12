package es.colefinder.data.repository

import android.util.Log
import es.colefinder.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import es.colefinder.data.model.ColegioSearchDto
import es.colefinder.data.model.NearbyColegioDto
import es.colefinder.ui.map.ColegioConDistancia
import es.colefinder.ui.map.TipoCentroFiltro
import es.colefinder.ui.map.TitularidadFiltro
import es.colefinder.ui.map.clasificarTipoCentro
import es.colefinder.ui.map.toRpcArray
import es.colefinder.data.network.ColegiosLoadException
import es.colefinder.data.network.classifyColegiosLoadFailure
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SupabaseColegioRepo"

/**
 * Implementación de [ColegioRepository] sobre Supabase (anon key + RLS).
 *
 * Accede directamente a la RPC `nearby_colegios` de Supabase.
 * No usa service_role ni ninguna credencial privilegiada.
 *
 * Errores: se clasifican con [classifyColegiosLoadFailure] y se devuelven como
 * [Result.failure] envuelto en [ColegiosLoadException] (mensaje de usuario + categoría en logs).
 */
@Singleton
class SupabaseColegioRepository @Inject constructor(
    private val supabase: SupabaseClient
) : ColegioRepository {

    override suspend fun fetchNearbyColegios(
        lat: Double,
        lon: Double,
        limit: Int,
        titularidades: Set<TitularidadFiltro>,
        tipos: Set<TipoCentroFiltro>
    ): Result<List<ColegioConDistancia>> = withContext(Dispatchers.IO) {
        try {
            val arrayTitularidad = titularidades.toRpcArray()
            val arrayTipo = tipos.toRpcArray()

            val params = buildJsonObject {
                put("p_lat", lat)
                put("p_lon", lon)
                put("p_limit", 200)
                if (arrayTitularidad != null) putJsonArray("p_titularidades") {
                    arrayTitularidad.forEach { add(it) }
                }
                if (arrayTipo != null) putJsonArray("p_tipos") {
                    arrayTipo.forEach { add(it) }
                }
            }

            val dtos: List<NearbyColegioDto> = supabase.postgrest
                .rpc("nearby_colegios", params) {
                    schema = BuildConfig.SUPABASE_SCHEMA
                }
                .decodeList<NearbyColegioDto>()

            val resultado = dtos
                .distinctBy { it.id }
                .map { dto ->
                    ColegioConDistancia(
                        colegio = dto.toColegio(),
                        distanciaMetros = dto.distanciaMetros,
                        tipoCentroClasificado = clasificarTipoCentro(
                            nombre = dto.nombre,
                            tipo = dto.tipo ?: "",
                            descripcionEntidad = dto.descripcionEntidad,
                            tipoCentroNormalizado = dto.tipoCentroNormalizado
                        ),
                        titularidadNormalizada = dto.titularidadNormalizada
                    )
                }

            Log.d(TAG, "fetchNearbyColegios: ${resultado.size} centros cargados")
            Result.success(resultado)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val classified = classifyColegiosLoadFailure(e)
            Log.e(TAG, "fetchNearbyColegios [${classified.category}] ${classified.technicalDetail}", e)
            Result.failure(
                ColegiosLoadException(
                    category = classified.category,
                    userMessage = classified.userMessage,
                    message = classified.technicalDetail,
                    cause = e
                )
            )
        }
    }

    override suspend fun searchColegiosByName(
        query: String,
        limit: Int
    ): Result<List<ColegioSearchDto>> = withContext(Dispatchers.IO) {
        try {
            val sanitized = query.replace("%", "").replace("_", "").trim()
            if (sanitized.length < 2) return@withContext Result.success(emptyList())

            val results = supabase.postgrest
                .from("colegios")
                .select(columns = Columns.list("id", "nombre", "localidad", "latitud", "longitud")) {
                    filter { ilike("nombre", "%$sanitized%") }
                    limit(limit.toLong())
                }
                .decodeList<ColegioSearchDto>()

            Log.d(TAG, "searchColegiosByName '$sanitized': ${results.size} resultados")
            Result.success(results)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "searchColegiosByName: error", e)
            Result.failure(e)
        }
    }
}
