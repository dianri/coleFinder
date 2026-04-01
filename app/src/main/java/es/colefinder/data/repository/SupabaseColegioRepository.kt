package es.colefinder.data.repository

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import es.colefinder.data.model.NearbyColegioDto
import es.colefinder.ui.map.ColegioConDistancia
import es.colefinder.ui.map.TipoCentroFiltro
import es.colefinder.ui.map.TitularidadFiltro
import es.colefinder.ui.map.clasificarTipoCentro
import es.colefinder.ui.map.toRpcArray
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SupabaseColegioRepo"

/**
 * Implementación de [ColegioRepository] sobre Supabase (anon key + RLS).
 *
 * Accede directamente a la RPC `nearby_colegios` de Supabase.
 * No usa service_role ni ninguna credencial privilegiada.
 *
 * Manejo de errores:
 * - IOException → error de red/conectividad.
 * - cualquier otra Exception → error inesperado de la API.
 * Ambos casos devueltos como [Result.failure] para que el ViewModel decida cómo presentarlos.
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
                put("p_limit", limit)
                if (arrayTitularidad != null) putJsonArray("p_titularidades") {
                    arrayTitularidad.forEach { add(it) }
                }
                if (arrayTipo != null) putJsonArray("p_tipos") {
                    arrayTipo.forEach { add(it) }
                }
            }

            val dtos = supabase.postgrest
                .rpc("nearby_colegios", params)
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

        } catch (e: IOException) {
            Log.e(TAG, "fetchNearbyColegios: error de red", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "fetchNearbyColegios: error inesperado", e)
            Result.failure(e)
        }
    }
}
