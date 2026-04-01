package es.colefinder.data.repository

import es.colefinder.ui.map.ColegioConDistancia
import es.colefinder.ui.map.TipoCentroFiltro
import es.colefinder.ui.map.TitularidadFiltro

/**
 * Contrato de acceso a datos para centros educativos.
 *
 * La implementación actual ([SupabaseColegioRepository]) accede directamente a Supabase
 * mediante la RPC `nearby_colegios` usando la anon key.
 *
 * En el futuro, esta interfaz permite sustituir la implementación por una
 * cacheada (Room) sin modificar el ViewModel.
 */
interface ColegioRepository {

    /**
     * Devuelve los centros más cercanos a (lat, lon) respetando los filtros activos.
     *
     * @param lat Latitud del punto de referencia.
     * @param lon Longitud del punto de referencia.
     * @param limit Número máximo de resultados (por defecto 50).
     * @param titularidades Filtros de titularidad activos.
     * @param tipos Filtros de tipo de centro activos.
     * @return Lista ordenada por distancia con metadatos precalculados.
     * @throws NetworkException si hay error de comunicación con Supabase.
     * @throws Exception para cualquier otro error inesperado.
     */
    suspend fun fetchNearbyColegios(
        lat: Double,
        lon: Double,
        limit: Int = 50,
        titularidades: Set<TitularidadFiltro> = setOf(TitularidadFiltro.TODOS),
        tipos: Set<TipoCentroFiltro> = setOf(TipoCentroFiltro.TODOS)
    ): Result<List<ColegioConDistancia>>
}
