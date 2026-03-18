package es.colefinder.ui.map

import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import es.colefinder.data.model.Colegio
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private var searchJob: Job? = null

    init {
        fetchColegios()
    }

    fun fetchColegios() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // We ensure we get a fresh list and deduplicate by ID
                val result = supabase.postgrest["colegios"]
                    .select()
                    .decodeList<Colegio>()
                    .distinctBy { it.id }
                
                _state.update { it.copy(colegios = result, isLoading = false) }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    fun onSearchQueryChanged(query: String, context: Context) {
        searchJob?.cancel()
        if (query.length < 3) {
            _state.update { it.copy(suggestions = emptyList()) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            try {
                val suggestions = withContext(Dispatchers.IO) {
                    val geocoder = Geocoder(context)
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(query, 5)?.mapNotNull { address ->
                        val title = address.thoroughfare ?: address.featureName ?: query
                        val subtitle = address.locality ?: address.adminArea ?: ""
                        SearchSuggestion(
                            title = title,
                            subtitle = subtitle,
                            latLng = LatLng(address.latitude, address.longitude)
                        )
                    } ?: emptyList()
                }
                _state.update { it.copy(suggestions = suggestions) }
            } catch (e: Exception) {
                _state.update { it.copy(suggestions = emptyList()) }
            }
        }
    }

    fun selectSuggestion(suggestion: SearchSuggestion) {
        _state.update { it.copy(suggestions = emptyList()) }
        viewModelScope.launch {
            _state.value.cameraPosition.animate(
                CameraUpdateFactory.newLatLngZoom(suggestion.latLng, 15f)
            )
        }
    }

    fun updateUserLocation(latLng: LatLng) {
        viewModelScope.launch {
            _state.update { it.copy(userLocation = latLng, puntoReferencia = null) }
            _state.value.cameraPosition.animate(
                CameraUpdateFactory.newLatLngZoom(latLng, 15f)
            )
            fetchColegios()
        }
    }

    fun onMapLongClick(latLng: LatLng) {
        _state.update { it.copy(puntoReferencia = latLng, selectedColegio = null) }
    }

    fun onColegioClick(colegio: Colegio) {
        _state.update { it.copy(selectedColegio = colegio) }
        viewModelScope.launch {
            _state.value.cameraPosition.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(colegio.latitud, colegio.longitud), 17f)
            )
        }
    }

    fun clearSelectedColegio() {
        _state.update { it.copy(selectedColegio = null) }
    }

    fun setFiltro(tipo: String) {
        _state.update { it.copy(filtroSeleccionado = tipo) }
    }

    fun buscarDireccion(query: String, context: Context) {
        if (query.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val latLng = withContext(Dispatchers.IO) {
                    val geocoder = Geocoder(context)
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(query, 1)?.firstOrNull()?.let {
                        LatLng(it.latitude, it.longitude)
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

    fun moverAColegio(latLng: LatLng, colegio: Colegio) {
        _state.update { it.copy(selectedColegio = colegio) }
        viewModelScope.launch {
            _state.value.cameraPosition.animate(
                CameraUpdateFactory.newLatLngZoom(latLng, 17f)
            )
        }
    }
}
