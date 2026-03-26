package es.colefinder.ui.map

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import es.colefinder.data.model.Colegio
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

    // 1. Estados del BottomSheet Custom con AnchoredDraggable
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
    val snackbarHostState = remember { SnackbarHostState() }

    // Custom NestedScrollConnection para delegar a AnchoredDraggable manual si nestedScrollConnection falla
    val customNestedScrollConnection = remember(anchoredDraggableState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                return if (delta < 0 && source == NestedScrollSource.UserInput) {
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
                return if (velocity < 0) {
                    anchoredDraggableState.settle(velocity)
                    available
                } else {
                    Velocity.Zero
                }
            }
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val velocity = available.y
                anchoredDraggableState.settle(velocity)
                return available
            }
        }
    }

    SideEffect {
        if (containerHeight > 0 && headerHeight > 0) {
            // hCollapsed: el tope del sheet queda a (altura total - el cabezal visible por encima de la nav bar)
            // Ya que aplicamos navigationBarsPadding() abajo, el headerHeight medido NO incluye la barra de navegación.
            val hCollapsed = containerHeight - headerHeight - navBarPx
            val hIntermediate = containerHeight - (headerHeight + filtersHeight) - navBarPx
            val hExpanded = with(density) { 80.dp.toPx() } // Espacio para la SearchBar

            anchoredDraggableState.updateAnchors(
                DraggableAnchors {
                    MapSheetValue.Collapsed at hCollapsed
                    MapSheetValue.Intermediate at hIntermediate
                    MapSheetValue.Expanded at hExpanded
                }
            )
        }
    }
    
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }

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

    LaunchedEffect(Unit) {
        if (locationPermissionsState.allPermissionsGranted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    val latLng = location?.let { LatLng(it.latitude, it.longitude) }
                    viewModel.initializeMap(latLng)
                }.addOnFailureListener { viewModel.initializeMap(null) }
            } catch (e: Exception) { viewModel.initializeMap(null) }
        } else {
            viewModel.initializeMap(null)
        }
    }

    // Efecto para mostrar el hint via Snackbar
    LaunchedEffect(state.showLongPressHint) {
        if (state.showLongPressHint) {
            snackbarHostState.showSnackbar(
                message = "Mantén pulsado el mapa para buscar centros cerca de ese punto"
            )
            viewModel.onHintDismissed()
        }
    }

    // Efecto para activar el hint al arrastrar el mapa
    LaunchedEffect(state.cameraPosition.isMoving) {
        if (state.cameraPosition.isMoving) {
            viewModel.onMapMoved()
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
                .nestedScroll(customNestedScrollConnection),
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

                                ListItem(
                                    leadingContent = {
                                        Badge(containerColor = colorParaTitularidad(colegio.tipo, item.titularidadNormalizada)) {
                                            Text((index + 1).toString(), color = Color.White)
                                        }
                                    },
                                    headlineContent = { Text(colegio.nombre, fontWeight = FontWeight.Bold) },
                                    supportingContent = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("A $distText de ti")
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(colegio.tipo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    modifier = Modifier.clickable {
                                        scope.launch { anchoredDraggableState.animateTo(MapSheetValue.Intermediate) }
                                        viewModel.moverAColegio(LatLng(colegio.latitud, colegio.longitud), item)
                                    }
                                )
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
        ) {
            LazyColumn {
                items(state.suggestions.size) { index ->
                    val suggestion = state.suggestions[index]
                    ListItem(
                        headlineContent = { Text(suggestion.title) },
                        supportingContent = { Text(suggestion.subtitle) },
                        leadingContent = { Icon(Icons.Default.LocationOn, contentDescription = null) },
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
        ) {
            state.selectedColegioConDistancia?.let { colegioConDistancia ->
                val colegio = colegioConDistancia.colegio
                ColegioDetailCard(
                    colegioConDistancia = colegioConDistancia,
                    isFavorito = colegio.id in state.favoritosIds,
                    onClose = { viewModel.clearSelectedColegio() },
                    onFavorito = { viewModel.toggleFavorito(colegio.id) },
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
        state.error?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }

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
                if (locationPermissionsState.allPermissionsGranted) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                        loc?.let { viewModel.updateUserLocation(LatLng(it.latitude, it.longitude)) }
                        ?: Toast.makeText(context, "Activa el GPS", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    locationPermissionsState.launchMultiplePermissionRequest()
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp)
                .graphicsLayer {
                    translationY = fabOffset - with(density) { 80.dp.toPx() }
                }
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = "Mi ubicación")
        }

        // Host para el hint contextual
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp) // Encima del sheet colapsado
        )
    }
}

@Composable
fun ColegioDetailCard(
    colegioConDistancia: ColegioConDistancia,
    isFavorito: Boolean,
    onClose: () -> Unit,
    onFavorito: () -> Unit,
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
                IconButton(onClick = onFavorito, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = if (isFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorito) "Quitar de favoritos" else "Añadir a favoritos",
                        tint = if (isFavorito) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Close, contentDescription = "Cerrar") }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(label = titularidadLabel, color = titularidadColor)
                InfoChip(label = tipoCentroLabel,  color = tipoCentroColor)
                InfoChip(label = distText, color = ColorFiltroTodos)
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
