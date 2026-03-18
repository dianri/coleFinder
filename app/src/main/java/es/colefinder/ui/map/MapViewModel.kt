package es.colefinder.ui.map

import android.content.Context
import android.location.Geocoder
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import es.colefinder.data.model.Colegio
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    init {
        fetchColegios()
    }

    fun fetchColegios() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val colegios = supabase.postgrest["colegios"]
                    .select()
                    .decodeList<Colegio>()
                _state.update { it.copy(colegios = colegios, isLoading = false) }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    fun updateUserLocation(latLng: LatLng) {
        viewModelScope.launch {
            _state.value.cameraPosition.animate(
                CameraUpdateFactory.newLatLngZoom(latLng, 15f)
            )
            // After moving to user location, search nearby colleges
            // In a real app we would filter by distance, for now we just refresh
            fetchColegios()
        }
    }

    fun buscarDireccion(query: String, context: Context) {
        if (query.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val latLng = withContext(Dispatchers.IO) {
                    val geocoder = Geocoder(context)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocationName(query, 1)?.firstOrNull()?.let {
                            LatLng(it.latitude, it.longitude)
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocationName(query, 1)?.firstOrNull()?.let {
                            LatLng(it.latitude, it.longitude)
                        }
                    }
                }

                if (latLng != null) {
                    _state.value.cameraPosition.animate(
                        CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                    )
                } else {
                    _state.update { it.copy(error = "Dirección no encontrada") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Error al buscar: ${e.localizedMessage}") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
