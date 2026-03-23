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
    val filtroSeleccionado: String = "Todos",
    val cameraPosition: CameraPositionState = CameraPositionState(
        position = CameraPosition.fromLatLngZoom(LatLng(40.4168, -3.7038), 6f)
    )
) {
    val colegiosConDistancia: List<ColegioConDistancia>
        get() = colegiosCercanos
}

data class SearchSuggestion(
    val title: String,
    val subtitle: String,
    val latLng: LatLng
)

data class ColegioConDistancia(
    val colegio: Colegio,
    val distanciaMetros: Double
)
