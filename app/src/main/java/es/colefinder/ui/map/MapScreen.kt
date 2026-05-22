package es.colefinder.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.util.Log
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.location.Priority
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.delay
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import es.colefinder.data.model.Colegio
import es.colefinder.data.model.JornadaTipo
import es.colefinder.ui.utils.createNumberedMarkerBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class MapSheetValue { Collapsed, Intermediate, Expanded }

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class, ExperimentalFoundationApi::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // 1. Estados de Ubicación y Permisos (Arriba para que estén disponibles)
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val uiSettings = remember {
        MapUiSettings(
            myLocationButtonEnabled = false,
            compassEnabled = true,
            mapToolbarEnabled = false,
            zoomControlsEnabled = false
        )
    }

    var isFirstLoad by remember { mutableStateOf(true) }
    var pendingLocationRequest by remember { mutableStateOf(false) }

    val focusOnCurrentLocationAndLoadNearby = remember(fusedLocationClient, viewModel, context) {
        {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                    if (lastLoc != null) {
                        viewModel.updateUserLocation(LatLng(lastLoc.latitude, lastLoc.longitude))
                    } else {
                        // Fallback: getCurrentLocation para casos donde lastLocation es null
                        try {
                            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                .addOnSuccessListener { freshLoc: Location? ->
                                    if (freshLoc != null) {
                                        viewModel.updateUserLocation(LatLng(freshLoc.latitude, freshLoc.longitude))
                                    } else {
                                        Toast.makeText(context, "No se pudo obtener la ubicación. Activa el GPS.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Error al obtener ubicación fresh", Toast.LENGTH_SHORT).show()
                                }
                        } catch (e: SecurityException) {
                            Toast.makeText(context, "Permiso revocado inesperadamente", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.addOnFailureListener {
                    Toast.makeText(context, "Error al acceder a la última ubicación", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 2. Estados del BottomSheet Custom con AnchoredDraggable
    var headerHeight by remember { mutableStateOf(0f) }
    var filtersHeight by remember { mutableStateOf(0f) }
    var containerHeight by remember { mutableStateOf(0f) }

    val anchoredDraggableState: AnchoredDraggableState<MapSheetValue> = remember {
        AnchoredDraggableState(
            initialValue = MapSheetValue.Intermediate,
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = SpringSpec<Float>(),
            decayAnimationSpec = exponentialDecay<Float>()
        )
    }

    val navBarPx = WindowInsets.navigationBars.getBottom(density).toFloat()

    // Custom NestedScrollConnection para coordinar el scroll entre la lista y el panel
    val customNestedScrollConnection = remember(anchoredDraggableState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                // Solo consumimos hacia arriba si el panel NO ha llegado a su tope (Expanded)
                val expandedOffset = try { anchoredDraggableState.anchors.positionOf(MapSheetValue.Expanded) } catch (_: Exception) { Float.NaN }
                val currentOffset = try { anchoredDraggableState.requireOffset() } catch (_: Exception) { Float.NaN }

                return if (delta < 0 && source == NestedScrollSource.UserInput && !expandedOffset.isNaN() && currentOffset > expandedOffset + 0.5f) {
                    val consumed = anchoredDraggableState.dispatchRawDelta(delta)
                    Offset(0f, consumed)
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                return if (source == NestedScrollSource.UserInput) {
                    val consumedDelta = anchoredDraggableState.dispatchRawDelta(delta)
                    Offset(0f, consumedDelta)
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val velocity = available.y
                val expandedOffset = try { anchoredDraggableState.anchors.positionOf(MapSheetValue.Expanded) } catch (_: Exception) { Float.NaN }
                val currentOffset = try { anchoredDraggableState.requireOffset() } catch (_: Exception) { Float.NaN }

                // Solo asentar el panel hacia arriba si todavía tiene recorrido
                return if (velocity < 0 && !expandedOffset.isNaN() && currentOffset > expandedOffset + 0.5f) {
                    anchoredDraggableState.settle(velocity)
                    available
                } else {
                    Velocity.Zero
                }
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val velocity = available.y
                // Capturar la inercia restante (ej: cuando la lista llega al tope arriba) para cerrar/ajustar el panel
                anchoredDraggableState.settle(velocity)
                return available
            }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var searchBarHeight by remember { mutableStateOf(0f) }

    SideEffect {
        if (containerHeight > 0 && headerHeight > 0) {
            // hCollapsed: el tope del sheet queda a (altura total - el cabezal visible por encima de la nav bar)
            // Ya que aplicamos navigationBarsPadding() abajo, el headerHeight medido NO incluye la barra de navegación.
            val hCollapsed = containerHeight - headerHeight - navBarPx
            val hIntermediate = containerHeight - (headerHeight + filtersHeight) - navBarPx
            val hExpanded = searchBarHeight + with(density) { 24.dp.toPx() } // Debajo de la SearchBar + margen 24dp

            anchoredDraggableState.updateAnchors(
                DraggableAnchors {
                    MapSheetValue.Collapsed at hCollapsed
                    MapSheetValue.Intermediate at hIntermediate
                    MapSheetValue.Expanded at hExpanded
                }
            )
        }
    }
    



    // Manejo de permisos y carga inicial / acciones pendientes
    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            // Si el permiso se acaba de conceder O es la primera carga con permiso ya presente
            if (isFirstLoad || pendingLocationRequest) {
                focusOnCurrentLocationAndLoadNearby()
                isFirstLoad = false
                pendingLocationRequest = false
            }
        } else {
            // Sin permisos: Madrid por defecto + RPC. Tras un denegación previa,
            // shouldShowRationale suele ser true; no debe impedir initializeMap (regresión).
            if (isFirstLoad) {
                viewModel.initializeMap(null)
                isFirstLoad = false
            }
        }
    }

    // Un solo Toast por error de estado (evita spam en recomposiciones)
    LaunchedEffect(state.error) {
        val msg = state.error ?: return@LaunchedEffect
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        viewModel.clearError()
    }

    // Efecto para activar el hint al arrastrar el mapa
    LaunchedEffect(state.cameraPosition.isMoving) {
        if (state.cameraPosition.isMoving) {
            viewModel.onMapMoved()
        }
    }

    // Efecto para auto-ajustar cámara si los resultados son lejanos (Filtro 'Mi Ubicación' o Búsquedas)
    LaunchedEffect(state.isLoading, state.focusedRequestType) {
        if (!state.isLoading && state.focusedRequestType != FocusedRequestType.NONE && state.colegiosConDistancia.isNotEmpty()) {
            // Un pequeño delay ayuda a asegurar que los marcadores y la proyección ya están listos
            delay(500)
            
            val visibleBounds = state.cameraPosition.projection?.visibleRegion?.latLngBounds
            if (visibleBounds != null) {
                // Verificar si hay algún centro dentro del área visible actual (zoom 15)
                val anyVisible = state.colegiosConDistancia.any { 
                    visibleBounds.contains(LatLng(it.colegio.latitud, it.colegio.longitud)) 
                }
                
                // Si están todos fuera (lejos), encuadramos con auto-fit
                if (!anyVisible) {
                    val boundsBuilder = LatLngBounds.builder()
                    state.colegiosConDistancia.forEach { 
                        boundsBuilder.include(LatLng(it.colegio.latitud, it.colegio.longitud)) 
                    }
                    
                    // Incluimos también el punto de referencia del tipo de búsqueda
                    val refLocation = when (state.focusedRequestType) {
                        FocusedRequestType.MY_LOCATION -> state.userLocation
                        FocusedRequestType.POINT -> state.puntoReferencia
                        else -> state.cameraPosition.position.target
                    }
                    refLocation?.let { boundsBuilder.include(it) }
                    
                    try {
                        state.cameraPosition.animate(
                            CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 250) // Padding generoso
                        )
                        viewModel.setMostrarAvisoCentrosLejanos(true)
                    } catch (_: Exception) { /* Ignorar si falla el encuadre */ }
                }
            }
            viewModel.consumeFocusedRequest()
        }
    }

    // Auto-dismiss para el aviso de centros lejanos
    LaunchedEffect(state.showRemoteResultsWarning) {
        if (state.showRemoteResultsWarning) {
            delay(6000)
            viewModel.setMostrarAvisoCentrosLejanos(false)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().onSizeChanged { containerHeight = it.height.toFloat() }) {
        val fullHeight = constraints.maxHeight.toFloat()
        
        // Capa 1: Mapa
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = state.cameraPosition,
            onMapLongClick = { viewModel.onMapLongClick(it) },
            onMapClick = { viewModel.onMapClick() },
            properties = MapProperties(isMyLocationEnabled = locationPermissionsState.allPermissionsGranted),
            uiSettings = uiSettings
        ) {
            state.puntoReferencia?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "Punto de Referencia",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
            }
            state.colegiosConDistancia.forEachIndexed { index, colegioConDist ->
                val colegio = colegioConDist.colegio
                key(colegio.id) {
                    val numberLabel = (index + 1).toString()
                    val markerColor = colorParaTitularidad(colegio.tipo, colegioConDist.titularidadNormalizada)
                    val icon = remember(colegio.id, numberLabel, markerColor, state.puntoReferencia) {
                        createNumberedMarkerBitmap(context, numberLabel, markerColor)
                    }
                    Marker(
                        state = MarkerState(position = LatLng(colegio.latitud, colegio.longitud)),
                        title = colegio.nombre,
                        snippet = colegio.tipo,
                        icon = icon,
                        onClick = {
                            viewModel.onColegioClick(colegioConDist)
                            true
                        }
                    )
                }
            }
        }

        // Capa 2: Persistent Bottom Sheet
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    val y = runCatching { anchoredDraggableState.requireOffset() }.getOrDefault(fullHeight)
                    IntOffset(0, y.roundToInt())
                }
                .anchoredDraggable(anchoredDraggableState, Orientation.Vertical)
                .nestedScroll(customNestedScrollConnection)
                .zIndex(1f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            shadowElevation = 16.dp, // Elevación para sombra visual
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding() // Asegura que el contenido no quede tras la nav bar
            ) {
                // Cabecera: Manejador + Título
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { 
                            if (it.height > 0) headerHeight = it.height.toFloat() 
                        }
                        .clickable {
                            scope.launch {
                                val target = if (anchoredDraggableState.currentValue == MapSheetValue.Collapsed) 
                                    MapSheetValue.Intermediate else MapSheetValue.Collapsed
                                anchoredDraggableState.animateTo(target)
                            }
                        }
                        .padding(bottom = 8.dp), // Padding extra para el área táctil
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
                    }
                    Text(
                        text = "Centros cercanos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
                    )
                }

                // Sección Intermedia: Filtros
                Column(modifier = Modifier.onSizeChanged { filtersHeight = it.height.toFloat() }) {
                    FiltroChipRow(
                        titulo = "Titularidad",
                        opciones = TitularidadFiltro.entries,
                        seleccionados = state.filtrosTitularidad,
                        etiqueta = { it.label },
                        colorChip = { colorParaTitularidadFiltro(it) },
                        onSeleccionar = { viewModel.toggleFiltroTitularidad(it) }
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    FiltroChipRow(
                        titulo = "Tipo de centro",
                        opciones = TipoCentroFiltro.entries,
                        seleccionados = state.filtrosTipoCentro,
                        etiqueta = { it.label },
                        colorChip = { colorParaTipoCentroFiltro(it) },
                        onSeleccionar = { viewModel.toggleFiltroTipoCentro(it) }
                    )
                    Spacer(modifier = Modifier.height(12.dp)) // Espacio extra para evitar que asome el listado
                }

                // Sección Extendida: Lista
                Box(modifier = Modifier.weight(1f)) {
                    if (state.colegiosConDistancia.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No hay centros de este tipo cerca de ti",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val listState = rememberLazyListState()
                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                            items(
                                count = state.colegiosConDistancia.size,
                                key = { index -> state.colegiosConDistancia[index].colegio.id }
                            ) { index ->
                                val item = state.colegiosConDistancia[index]
                                val colegio = item.colegio
                                val distText = if (item.distanciaMetros >= 1000) "%.1f km".format(item.distanciaMetros / 1000) else "${item.distanciaMetros.toInt()} m"

                                val titularidadLabel = labelParaTitularidad(item.titularidadNormalizada, colegio.tipo)
                                val destacado = colegio.esDificilDesempeno || colegio.esRural
                                val itemBgColor = if (destacado) colorFondoDestacado() else Color.Transparent
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min)
                                        .background(itemBgColor)
                                        .clickable {
                                            scope.launch { anchoredDraggableState.animateTo(MapSheetValue.Intermediate) }
                                            viewModel.moverAColegio(LatLng(colegio.latitud, colegio.longitud), item)
                                        }
                                ) {
                                    if (destacado) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .width(4.dp)
                                                .fillMaxHeight()
                                                .background(Color(0xFFEF5350))
                                        )
                                    }

                                    ListItem(
                                        leadingContent = {
                                            Badge(containerColor = colorParaTitularidad(colegio.tipo, item.titularidadNormalizada)) {
                                                Text((index + 1).toString(), color = Color.White)
                                            }
                                        },
                                        headlineContent = { Text(colegio.nombre, fontWeight = FontWeight.Bold) },
                                        supportingContent = {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                val subtitulo = if (colegio.jornadaTipo != JornadaTipo.DESCONOCIDA) {
                                                    "${colegio.localidad} · $titularidadLabel · Jornada ${colegio.jornadaTipo.label}"
                                                } else {
                                                    "${colegio.localidad} · $titularidadLabel"
                                                }
                                                Text(
                                                    text = subtitulo,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (colegio.esDificilDesempeno || colegio.esRural) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 2.dp)) {
                                                        if (colegio.esRural) {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = colorFondoChipDestacado(),
                                                                contentColor = colorTextoRural()
                                                            ) {
                                                                Text(
                                                                    text = "Rural",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                        if (colegio.esDificilDesempeno) {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = colorFondoChipDestacado(),
                                                                contentColor = colorTextoDificil()
                                                            ) {
                                                                Text(
                                                                    text = "Difícil desempeño",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = "A $distText de ti",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Overlays: SearchBar
        SearchBar(
            query = searchQuery,
            onQueryChange = { 
                searchQuery = it
                viewModel.onSearchQueryChanged(it, context)
            },
            onSearch = {
                viewModel.buscarDireccion(searchQuery, context)
                searchActive = false
            },
            active = searchActive,
            onActiveChange = { 
                searchActive = it 
                if (!it) searchQuery = ""
            },
            placeholder = { Text("Buscar colegio o dirección...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchActive && searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                } else if (!searchActive) {
                    Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Perfil", modifier = Modifier.padding(end = 8.dp))
                }
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(if (searchActive) 0.dp else 16.dp)
                .fillMaxWidth()
                .onSizeChanged { searchBarHeight = it.height.toFloat() }
        ) {
            LazyColumn {
                items(state.suggestions.size) { index ->
                    val suggestion = state.suggestions[index]
                    val isColegio = suggestion.colegioId != null
                    ListItem(
                        headlineContent = { Text(suggestion.title) },
                        supportingContent = { Text(suggestion.subtitle) },
                        leadingContent = {
                            Icon(
                                if (isColegio) Icons.Default.School else Icons.Default.LocationOn,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.clickable {
                            searchQuery = suggestion.title
                            searchActive = false
                            viewModel.selectSuggestion(suggestion)
                        }
                    )
                }
            }
        }

        // Overlay: Detail Card
        AnimatedVisibility(
            visible = state.selectedColegioConDistancia != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .padding(bottom = 80.dp) // No obstruir el estado colapsado
                .zIndex(2f)
        ) {
            state.selectedColegioConDistancia?.let { colegioConDistancia ->
                val colegio = colegioConDistancia.colegio
                ColegioDetailCard(
                    colegioConDistancia = colegioConDistancia,
                    onClose = { viewModel.clearSelectedColegio() },
                    onNavigate = {
                        val uri = Uri.parse("google.navigation:q=${colegio.latitud},${colegio.longitud}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps")
                        try { context.startActivity(mapIntent) } catch (e: Exception) { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                    },
                    onCall = { colegio.telefono?.let { tel -> context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel"))) } },
                    onVerDetalle = {
                        val query = Uri.encode("${colegio.nombre}, ${colegio.direccion}, ${colegio.localidad}")
                        val uri = Uri.parse("geo:0,0?q=$query")
                        val mapIntent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps")
                        try { context.startActivity(mapIntent) } catch (e: Exception) { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                    }
                )
            }
        }

        if (state.isLoading) { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) }

        // Overlay: Location FAB
        val fabOffset by remember(fullHeight) {
            derivedStateOf {
                try {
                    anchoredDraggableState.requireOffset()
                } catch (e: Exception) {
                    fullHeight
                }
            }
        }
        FloatingActionButton(
            onClick = {
                pendingLocationRequest = true
                if (locationPermissionsState.allPermissionsGranted) {
                    focusOnCurrentLocationAndLoadNearby()
                    pendingLocationRequest = false
                } else {
                    locationPermissionsState.launchMultiplePermissionRequest()
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp)
                .zIndex(1f)
                .graphicsLayer {
                    translationY = fabOffset - with(density) { 80.dp.toPx() }
                },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = "Mi ubicación")
        }

        // Hint contextual para Long Press (Capa superior absoluta)
        LaunchedEffect(state.showLongPressHint) {
            if (state.showLongPressHint) {
                delay(4000)
                viewModel.onHintDismissed()
            }
        }

        AnimatedVisibility(
            visible = state.showLongPressHint,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(3f)
                .graphicsLayer {
                    // Posicionar justo encima del sheet (fabOffset es el tope del sheet)
                    // Restamos la altura aproximada del chip (48dp) + un margen de seguridad (16dp)
                    translationY = fabOffset - with(density) { 64.dp.toPx() }
                }
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .clickable { viewModel.onHintDismissed() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Mantén pulsado el mapa para buscar centros cerca",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 5. Aviso de Centros Lejanos (Capa superior)
        AnimatedVisibility(
            visible = state.showRemoteResultsWarning,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(4f)
                .padding(top = 100.dp) // Debajo del buscador
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "No hay centros cerca; mostramos los más próximos",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { viewModel.setMostrarAvisoCentrosLejanos(false) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ColegioDetailCard(
    colegioConDistancia: ColegioConDistancia,
    onClose: () -> Unit,
    onNavigate: () -> Unit,
    onCall: () -> Unit,
    onVerDetalle: () -> Unit
) {
    val colegio = colegioConDistancia.colegio
    val distText = if (colegioConDistancia.distanciaMetros >= 1000) "%.1f km".format(colegioConDistancia.distanciaMetros / 1000) else "${colegioConDistancia.distanciaMetros.toInt()} m"
    val titularidadColor = colorParaTitularidad(colegio.tipo, colegioConDistancia.titularidadNormalizada)
    val titularidadLabel = labelParaTitularidad(colegioConDistancia.titularidadNormalizada, colegio.tipo)
    val tipoCentroColor  = colorParaTipoCentroClasificado(colegioConDistancia.tipoCentroClasificado)
    val tipoCentroLabel  = colegioConDistancia.tipoCentroClasificado.label
    val hayTelefono      = !colegio.telefono.isNullOrBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = colegio.nombre,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Close, contentDescription = "Cerrar") }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Fila 1: Metadatos estándar + Localidad
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoChip(label = titularidadLabel, color = titularidadColor)
                    InfoChip(label = tipoCentroLabel, color = tipoCentroColor)
                    InfoChip(label = distText, color = MaterialTheme.colorScheme.primary)
                    if (colegio.localidad.isNotBlank()) {
                        InfoChip(label = colegio.localidad, color = MaterialTheme.colorScheme.secondary)
                    }
                    if (colegio.jornadaTipo != JornadaTipo.DESCONOCIDA) {
                        InfoChip(label = "Jornada ${colegio.jornadaTipo.label}", color = MaterialTheme.colorScheme.tertiary)
                    }
                }
                
                // Fila 2: Etiquetas extra (Solo si aplica)
                if (colegio.esDificilDesempeno || colegio.esRural) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (colegio.esRural) {
                            Surface(shape = RoundedCornerShape(50), color = colorFondoDestacado(), contentColor = colorTextoRural()) {
                                Text(text = "Rural", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (colegio.esDificilDesempeno) {
                            Surface(shape = RoundedCornerShape(50), color = colorFondoDestacado(), contentColor = colorTextoDificil()) {
                                Text(text = "Difícil desempeño", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(text = colegio.direccion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    if (colegio.localidad.isNotBlank()) { Text(text = colegio.localidad, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            if (hayTelefono) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Call, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = colegio.telefono!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigate, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Navegar")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hayTelefono) {
                    OutlinedButton(onClick = onCall, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Llamar")
                    }
                }
                OutlinedButton(onClick = onVerDetalle, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Detalle")
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.15f), contentColor = color) {
        Text(text = label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> FiltroChipRow(
    titulo: String,
    opciones: List<T>,
    seleccionados: Set<T>,
    etiqueta: (T) -> String,
    colorChip: (T) -> Color,
    onSeleccionar: (T) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            opciones.forEach { opcion ->
                val selected = opcion in seleccionados
                val color = colorChip(opcion)
                FilterChip(
                    selected = selected,
                    onClick = { onSeleccionar(opcion) },
                    label = { Text(etiqueta(opcion)) },
                    leadingIcon = if (selected) { { Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) } } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = color.copy(alpha = 0.12f),
                        labelColor = color,
                        selectedContainerColor = color,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = color.copy(alpha = 0.5f), selectedBorderColor = Color.Transparent)
                )
            }
        }
    }
}
