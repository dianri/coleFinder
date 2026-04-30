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
import es.colefinder.data.network.ColegiosLoadException
import es.colefinder.data.repository.ColegioRepository
import es.colefinder.data.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

private const val DEFAULT_LAT = 40.4168
private const val DEFAULT_LON = -3.7038
private const val TAG = "MapViewModel"

@HiltViewModel
class MapViewModel @Inject constructor(
    private val colegioRepository: ColegioRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var nearbyLoadJob: Job? = null
    private val nearbyLoadGeneration = AtomicInteger(0)
    private var initialized = false
    private var pendingColegioSelection: Int? = null

    init {
        viewModelScope.launch {
            userPreferencesRepository.userPreferencesFlow.collect { prefs ->
                _state.update { it.copy(
                    hasDiscoveredLongPress = prefs.hasDiscoveredLongPress,
                    longPressHintCount = prefs.longPressHintCount
                )}
            }
        }
    }

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
                _state.update { it.copy(userLocation = userLatLng, focusedRequestType = FocusedRequestType.MY_LOCATION) }
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
     * Carga los centros más cercanos al punto dado delegando en [ColegioRepository].
     * El acceso a Supabase ocurre en el repositorio, no aquí.
     *
     * Solo una petición cercana está activa: cada llamada cancela la anterior y usa un
     * contador de generación para ignorar respuestas obsoletas si una respuesta llega
     * fuera de orden (p. ej. toggles rápidos de filtros).
     *
     * La generación se incrementa **antes** de [Job.cancel] y del nuevo [launch], para que
     * cualquier continuación rezagada de una petición anterior vea de inmediato un
     * `generation != nearbyLoadGeneration` aunque el incremento dentro del nuevo job aún no
     * se haya ejecutado (orden en el hilo principal entre cancel y el primer suspend).
     */
    fun loadNearbyColegios(lat: Double, lon: Double) {
        val generation = nearbyLoadGeneration.incrementAndGet()
        nearbyLoadJob?.cancel()
        nearbyLoadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val titularidades = _state.value.filtrosTitularidad
            val tipos = _state.value.filtrosTipoCentro
            val result = colegioRepository.fetchNearbyColegios(
                lat = lat,
                lon = lon,
                titularidades = titularidades,
                tipos = tipos
            )

            ensureActive()
            if (generation != nearbyLoadGeneration.get()) return@launch

            result.fold(
                onSuccess = { cercanos ->
                    if (generation != nearbyLoadGeneration.get()) return@fold
                    _state.update { it.copy(colegiosCercanos = cercanos, isLoading = false) }
                    pendingColegioSelection?.let { pendingId ->
                        pendingColegioSelection = null
                        cercanos.find { it.colegio.id == pendingId }?.let { onColegioClick(it) }
                    }
                },
                onFailure = { error ->
                    if (generation != nearbyLoadGeneration.get()) return@fold
                    pendingColegioSelection = null
                    val userMsg = (error as? ColegiosLoadException)?.userMessage
                        ?: error.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "Error de conexión"
                    Log.e(TAG, "loadNearbyColegios: $userMsg", error)
                    _state.update { it.copy(error = userMsg, isLoading = false) }
                }
            )
        }
    }

    fun onSearchQueryChanged(query: String, context: Context) {
        searchJob?.cancel()
        if (query.length < 3) {
            _state.update { it.copy(suggestions = emptyList()) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)

            val geocoderDeferred = async(Dispatchers.IO) {
                try {
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
                } catch (_: Exception) {
                    emptyList()
                }
            }

            val colegiosDeferred = async(Dispatchers.IO) {
                colegioRepository.searchColegiosByName(query)
                    .getOrDefault(emptyList())
                    .map { dto ->
                        SearchSuggestion(
                            title = dto.nombre,
                            subtitle = dto.localidad ?: "",
                            latLng = LatLng(dto.latitud, dto.longitud),
                            colegioId = dto.id
                        )
                    }
            }

            val colegios = colegiosDeferred.await()
            val locations = geocoderDeferred.await()
            _state.update { it.copy(suggestions = colegios + locations) }
        }
    }

    fun selectSuggestion(suggestion: SearchSuggestion) {
        _state.update { it.copy(suggestions = emptyList()) }
        viewModelScope.launch {
            _state.update {
                it.copy(
                    puntoReferencia = suggestion.latLng,
                    focusedRequestType = FocusedRequestType.SEARCH
                )
            }
            _state.value.cameraPosition.animate(
                CameraUpdateFactory.newLatLngZoom(suggestion.latLng, 15f)
            )
            if (suggestion.colegioId != null) {
                pendingColegioSelection = suggestion.colegioId
            }
            loadNearbyColegios(suggestion.latLng.latitude, suggestion.latLng.longitude)
        }
    }

    fun updateUserLocation(latLng: LatLng) {
        viewModelScope.launch {
            _state.update { it.copy(userLocation = latLng, puntoReferencia = null, focusedRequestType = FocusedRequestType.MY_LOCATION) }
            _state.value.cameraPosition.animate(
                CameraUpdateFactory.newLatLngZoom(latLng, 15f)
            )
            loadNearbyColegios(latLng.latitude, latLng.longitude)
        }
    }

    fun consumeFocusedRequest() {
        _state.update { it.copy(focusedRequestType = FocusedRequestType.NONE) }
    }

    /** Tras mostrar el error en UI (p. ej. Toast), limpia el estado para evitar repeticiones. */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun setMostrarAvisoCentrosLejanos(show: Boolean) {
        _state.update { it.copy(showRemoteResultsWarning = show) }
    }

    fun onMapLongClick(latLng: LatLng) {
        _state.update { it.copy(puntoReferencia = latLng, selectedColegioConDistancia = null, focusedRequestType = FocusedRequestType.POINT) }
        loadNearbyColegios(latLng.latitude, latLng.longitude)

        if (!_state.value.hasDiscoveredLongPress) {
            viewModelScope.launch {
                userPreferencesRepository.updateHasDiscoveredLongPress(true)
            }
        }
    }

    fun onMapClick() {
        clearSelectedColegio()
        val currentState = _state.value
        if (!currentState.hasDiscoveredLongPress) {
            _state.update { it.copy(showLongPressHint = true) }
            viewModelScope.launch {
                userPreferencesRepository.incrementHintCount()
            }
        }
    }

    fun onMapMoved() {
        val currentState = _state.value
        if (!currentState.hasDiscoveredLongPress && !currentState.showLongPressHint) {
            _state.update { it.copy(showLongPressHint = true) }
            viewModelScope.launch {
                userPreferencesRepository.incrementHintCount()
            }
        }
    }

    fun onHintDismissed() {
        _state.update { it.copy(showLongPressHint = false) }
    }

    fun onColegioClick(colegioConDistancia: ColegioConDistancia) {
        _state.update { it.copy(selectedColegioConDistancia = colegioConDistancia) }
        viewModelScope.launch {
            _state.value.cameraPosition.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(colegioConDistancia.colegio.latitud, colegioConDistancia.colegio.longitud), 17f
                )
            )
        }
    }

    fun clearSelectedColegio() {
        _state.update { it.copy(selectedColegioConDistancia = null) }
    }

    fun toggleFavorito(colegioId: Int) {
        _state.update { state ->
            val nuevos = if (colegioId in state.favoritosIds) {
                state.favoritosIds - colegioId
            } else {
                state.favoritosIds + colegioId
            }
            state.copy(favoritosIds = nuevos)
        }
    }

    fun toggleFiltroTitularidad(filtro: TitularidadFiltro) {
        _state.update { it.copy(
            filtrosTitularidad = toggleTitularidad(it.filtrosTitularidad, filtro)
        )}
        reloadWithCurrentFilters()
    }

    fun toggleFiltroTipoCentro(filtro: TipoCentroFiltro) {
        _state.update { it.copy(
            filtrosTipoCentro = toggleTipoCentro(it.filtrosTipoCentro, filtro)
        )}
        reloadWithCurrentFilters()
    }

    private fun reloadWithCurrentFilters() {
        val punto = _state.value.puntoReferencia ?: _state.value.userLocation
        val lat = punto?.latitude ?: DEFAULT_LAT
        val lon = punto?.longitude ?: DEFAULT_LON
        loadNearbyColegios(lat, lon)
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
                    _state.update {
                        it.copy(
                            puntoReferencia = latLng,
                            focusedRequestType = FocusedRequestType.SEARCH
                        )
                    }
                    _state.value.cameraPosition.animate(
                        CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                    )
                    loadNearbyColegios(latLng.latitude, latLng.longitude)
                } else {
                    _state.update { it.copy(error = "Dirección no encontrada", isLoading = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Error al buscar: ${e.localizedMessage}", isLoading = false) }
            }
        }
    }

    fun moverAColegio(latLng: LatLng, colegioConDistancia: ColegioConDistancia) {
        _state.update { it.copy(selectedColegioConDistancia = colegioConDistancia) }
        viewModelScope.launch {
            _state.value.cameraPosition.animate(
                CameraUpdateFactory.newLatLngZoom(latLng, 17f)
            )
        }
    }
}
