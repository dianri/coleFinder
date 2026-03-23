package es.colefinder.ui.map

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import es.colefinder.data.model.Colegio

data class MapState(
    val isLoading: Boolean = false,
    val colegiosCercanos: List<ColegioConDistancia> = emptyList(),
    val suggestions: List<SearchSuggestion> = emptyList(),
    val error: String? = null,
    val userLocation: LatLng? = null,
    val puntoReferencia: LatLng? = null,
    val selectedColegio: Colegio? = null,
    // Sets vacíos no son válidos: siempre contienen al menos TODOS u opciones concretas.
    val filtrosTitularidad: Set<TitularidadFiltro> = setOf(TitularidadFiltro.TODOS),
    val filtrosTipoCentro: Set<TipoCentroFiltro> = setOf(TipoCentroFiltro.TODOS),
    val cameraPosition: CameraPositionState = CameraPositionState(
        position = CameraPosition.fromLatLngZoom(LatLng(40.4168, -3.7038), 6f)
    )
) {
    /**
     * Lista final para marcadores y panel lateral.
     * Usa titularidadNormalizada (BD) cuando está disponible; fallback a tipo (texto libre).
     */
    val colegiosConDistancia: List<ColegioConDistancia>
        get() = colegiosCercanos.filter { item ->
            matchesTitularidadNormalizadaFiltros(
                titularidadNormalizada = item.titularidadNormalizada,
                tipoRaw                = item.colegio.tipo,
                filtros                = filtrosTitularidad
            ) &&
            item.tipoCentroClasificado.matchesFiltros(filtrosTipoCentro)
        }
}

data class SearchSuggestion(
    val title: String,
    val subtitle: String,
    val latLng: LatLng
)

data class ColegioConDistancia(
    val colegio: Colegio,
    val distanciaMetros: Double,
    val tipoCentroClasificado: TipoCentroClasificado = TipoCentroClasificado.OTROS,
    val titularidadNormalizada: String? = null
)
