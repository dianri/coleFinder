package es.colefinder.ui.map

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import es.colefinder.data.model.Colegio

data class MapState(
    val isLoading: Boolean = false,
    val colegios: List<Colegio> = emptyList(),
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
    // Computed list of up to 50 colegios with their distance to the reference point (or user location/Madrid as fallback)
    // We apply distinctBy here as well just to be 100% safe against UI duplication bugs
    val colegiosConDistancia: List<ColegioConDistancia>
        get() {
            val reference = puntoReferencia ?: userLocation ?: LatLng(40.4168, -3.7038)
            
            return colegios
                .distinctBy { it.id }
                .filter { colegio ->
                    filtroSeleccionado == "Todos" || colegio.tipo.contains(filtroSeleccionado, ignoreCase = true)
                }
                .map { colegio ->
                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(
                        reference.latitude, reference.longitude,
                        colegio.latitud, colegio.longitud,
                        results
                    )
                    ColegioConDistancia(colegio, results[0].toDouble())
                }
                .sortedBy { it.distanciaMetros }
                .take(50)
        }
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
