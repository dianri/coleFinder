package es.colefinder.ui.map

import android.content.Context
import android.location.Geocoder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import es.colefinder.data.model.Colegio
import es.colefinder.data.model.NearbyColegioDto
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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

private const val DEFAULT_LAT = 40.4168
private const val DEFAULT_LON = -3.7038
private const val TAG = "MapViewModel"

@HiltViewModel
class MapViewModel @Inject constructor(
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var initialized = false

    fun initializeMap(userLatLng: LatLng?) {
        if (initialized) {
            Log.d(TAG, "initializeMap: ya inicializado, ignorando llamada duplicada")
            return
        }
        initialized = true
        _state.update { it.copy(isLoading = true) }

        if (userLatLng != null) {
            Log.d(TAG, "initializeMap: ubicación disponible (${userLatLng.latitude}, ${userLatLng.longitude})")
            viewModelScope.launch {
                _state.update { it.copy(userLocation = userLatLng) }
                _state.value.cameraPosition.animate(
                    CameraUpdateFactory.newLatLngZoom(userLatLng, 15f)
                )
                loadNearbyColegios(userLatLng.latitude, userLatLng.longitude)
            }
        } else {
            Log.d(TAG, "initializeMap: sin ubicación, cargando posición por defecto (Madrid)")
            loadNearbyColegios(DEFAULT_LAT, DEFAULT_LON)
        }
    }

    /**
     * Carga los 50 centros más cercanos al punto dado.
     * No filtra por titularidad en la RPC: el filtrado de titularidad y tipo
     * se aplica en cliente desde MapState.colegiosConDistancia, soportando multiselección.
     */
    fun loadNearbyColegios(lat: Double, lon: Double) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val params = buildJsonObject {
                    put("p_lat", lat)
                    put("p_lon", lon)
                    put("p_limit", 50)
                }
                val dtos = supabase.postgrest
                    .rpc("nearby_colegios", params)
                    .decodeList<NearbyColegioDto>()
                val cercanos = dtos
                    .distinctBy { it.id }
                    .map { dto ->
                        ColegioConDistancia(
                            colegio = dto.toColegio(),
                            distanciaMetros = dto.distanciaMetros,
                            tipoCentroClasificado = clasificarTipoCentro(
                                nombre                = dto.nombre,
                                tipo                  = dto.tipo ?: "",
                                descripcionEntidad    = dto.descripcionEntidad,
                                tipoCentroNormalizado = dto.tipoCentroNormalizado
                            ),
                            titularidadNormalizada = dto.titularidadNormalizada
                        )
                    }
                _state.update { it.copy(colegiosCercanos = cercanos, isLoading = false) }
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
            loadNearbyColegios(suggestion.latLng.latitude, suggestion.latLng.longitude)
        }
    }

    fun updateUserLocation(latLng: LatLng) {
        viewModelScope.launch {
            _state.update { it.copy(userLocation = latLng, puntoReferencia = null) }
            _state.value.cameraPosition.animate(
                CameraUpdateFactory.newLatLngZoom(latLng, 15f)
            )
            loadNearbyColegios(latLng.latitude, latLng.longitude)
        }
    }

    fun onMapLongClick(latLng: LatLng) {
        _state.update { it.copy(puntoReferencia = latLng, selectedColegio = null) }
        loadNearbyColegios(latLng.latitude, latLng.longitude)
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

    /** Alterna una opción de titularidad respetando la lógica de "Todos". Sin recarga RPC. */
    fun toggleFiltroTitularidad(filtro: TitularidadFiltro) {
        _state.update { it.copy(
            filtrosTitularidad = toggleTitularidad(it.filtrosTitularidad, filtro)
        )}
    }

    /** Alterna una opción de tipo de centro respetando la lógica de "Todos". Sin recarga RPC. */
    fun toggleFiltroTipoCentro(filtro: TipoCentroFiltro) {
        _state.update { it.copy(
            filtrosTipoCentro = toggleTipoCentro(it.filtrosTipoCentro, filtro)
        )}
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
                    loadNearbyColegios(latLng.latitude, latLng.longitude)
                    // isLoading lo gestiona loadNearbyColegios desde aquí
                } else {
                    _state.update { it.copy(error = "Dirección no encontrada", isLoading = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Error al buscar: ${e.localizedMessage}", isLoading = false) }
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
