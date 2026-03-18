package es.colefinder.ui.map

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import es.colefinder.data.model.Colegio

data class MapState(
    val isLoading: Boolean = false,
    val colegios: List<Colegio> = emptyList(),
    val error: String? = null,
    val userLocation: LatLng? = null,
    val filtroSeleccionado: String = "Todos",
    val cameraPosition: CameraPositionState = CameraPositionState(
        position = CameraPosition.fromLatLngZoom(LatLng(40.4168, -3.7038), 6f)
    )
) {
    // Computed list of colegios with their distance to the user (or Madrid as fallback) and filtered by type
    val colegiosConDistancia: List<ColegioConDistancia>
        get() {
            val reference = userLocation ?: LatLng(40.4168, -3.7038)
            return colegios
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
        }
}

data class ColegioConDistancia(
    val colegio: Colegio,
    val distanciaMetros: Double
)
