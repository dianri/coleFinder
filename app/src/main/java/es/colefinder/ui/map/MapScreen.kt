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
import androidx.compose.material.icons.automirrored.filled.List
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import es.colefinder.data.model.Colegio
import es.colefinder.ui.utils.createNumberedMarkerBitmap
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

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
            mapToolbarEnabled = false
        )
    }

    // Carga inicial: usa ubicación real si el permiso ya está concedido; si falla en cualquier punto,
    // el ViewModel cae al fallback por defecto (Madrid). Se ejecuta exactamente una vez.
    LaunchedEffect(Unit) {
        if (locationPermissionsState.allPermissionsGranted) {
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        val latLng = location?.let {
                            com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude)
                        }
                        Log.d("MapScreen", "Startup: lastLocation=$latLng")
                        viewModel.initializeMap(latLng)
                    }
                    .addOnFailureListener { e ->
                        Log.w("MapScreen", "Startup: lastLocation falló (${e.message}), usando por defecto")
                        viewModel.initializeMap(null)
                    }
            } catch (e: Exception) {
                Log.w("MapScreen", "Startup: excepción obteniendo ubicación (${e.message}), usando por defecto")
                viewModel.initializeMap(null)
            }
        } else {
            Log.d("MapScreen", "Startup: sin permiso de ubicación, cargando por defecto")
            viewModel.initializeMap(null)
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = {
                        if (locationPermissionsState.allPermissionsGranted) {
                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                location?.let {
                                    viewModel.updateUserLocation(LatLng(it.latitude, it.longitude))
                                } ?: run {
                                    Toast.makeText(context, "No se pudo obtener la ubicación. Activa el GPS.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            locationPermissionsState.launchMultiplePermissionRequest()
                        }
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Mi ubicación")
                }
                
                FloatingActionButton(
                    onClick = { showBottomSheet = true }
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Ver lista")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = state.cameraPosition,
                onMapLongClick = { latLng ->
                    viewModel.onMapLongClick(latLng)
                },
                onMapClick = {
                    viewModel.clearSelectedColegio()
                },
                properties = MapProperties(
                    isMyLocationEnabled = locationPermissionsState.allPermissionsGranted
                ),
                uiSettings = uiSettings
            ) {
                // Marcador de Referencia
                state.puntoReferencia?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = "Punto de Referencia",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }

                // Colegios
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

            // SearchBar M3
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
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Perfil",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(if (searchActive) 0.dp else 16.dp)
                    .fillMaxWidth()
            ) {
                // Sugerencias combinadas: Geocoder + Colegios Locales
                LazyColumn {
                    // Sugerencias del Geocoder
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

                    // Sugerencias de colegios si el Geocoder está vacío o como complemento
                    if (searchQuery.isNotEmpty()) {
                        val filteredColegios = state.colegiosCercanos
                            .filter { it.colegio.nombre.contains(searchQuery, ignoreCase = true) }
                            .take(5)

                        items(filteredColegios.size) { index ->
                            val item = filteredColegios[index]
                            val colegio = item.colegio
                            ListItem(
                                headlineContent = { Text(colegio.nombre) },
                                supportingContent = { Text(colegio.localidad) },
                                leadingContent = { Icon(Icons.Default.Search, contentDescription = null) },
                                modifier = Modifier.clickable {
                                    searchQuery = colegio.nombre
                                    searchActive = false
                                    viewModel.moverAColegio(LatLng(colegio.latitud, colegio.longitud), item)
                                }
                            )
                        }
                    }
                }
            }

            // Card de Detalle
            AnimatedVisibility(
                visible = state.selectedColegioConDistancia != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .padding(bottom = 160.dp)
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
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                        },
                        onCall = {
                            colegio.telefono?.let { tel ->
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel"))
                                context.startActivity(intent)
                            }
                        },
                        onVerDetalle = {
                            val query = buildString {
                                append(colegio.nombre)
                                if (colegio.direccion.isNotBlank()) append(", ${colegio.direccion}")
                                if (colegio.localidad.isNotBlank()) append(", ${colegio.localidad}")
                            }
                            val uri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                .setPackage("com.google.android.apps.maps")
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                        }
                    )
                }
            }

            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            state.error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                BottomSheetContent(
                    state = state,
                    onFiltroTitularidadChange = { viewModel.toggleFiltroTitularidad(it) },
                    onFiltroTipoCentroChange  = { viewModel.toggleFiltroTipoCentro(it) },
                    onColegioClick = { colegioConDistancia ->
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) showBottomSheet = false
                        }
                        viewModel.moverAColegio(
                            LatLng(colegioConDistancia.colegio.latitud, colegioConDistancia.colegio.longitud),
                            colegioConDistancia
                        )
                    }
                )
            }
        }
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
    val distText = if (colegioConDistancia.distanciaMetros >= 1000)
        "%.1f km".format(colegioConDistancia.distanciaMetros / 1000)
    else
        "${colegioConDistancia.distanciaMetros.toInt()} m"

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

            // 1. CABECERA: nombre + favorito + cerrar
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
                IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. CHIPS: titularidad · tipo · distancia
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoChip(label = titularidadLabel, color = titularidadColor)
                InfoChip(label = tipoCentroLabel,  color = tipoCentroColor)
                InfoChip(label = distText,          color = ColorFiltroTodos)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. DIRECCIÓN
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = colegio.direccion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (colegio.localidad.isNotBlank()) {
                        Text(
                            text = colegio.localidad,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 4. TELÉFONO (solo si existe)
            if (hayTelefono) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = colegio.telefono!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. BOTONES DE ACCIÓN
            // Fila 1: acción primaria a ancho completo.
            Button(
                onClick = onNavigate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Navegar")
            }

            // Fila 2: acciones secundarias. Con teléfono → dos columnas; sin teléfono → Detalle ancho completo.
            Spacer(modifier = Modifier.height(8.dp))
            if (hayTelefono) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCall,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Llamar")
                    }
                    OutlinedButton(
                        onClick = onVerDetalle,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Detalle")
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onVerDetalle,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver detalle")
                }
            }
        }
    }
}

/** Chip informativo no interactivo con fondo semitransparente y texto coloreado. */
@Composable
private fun InfoChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f),
        contentColor = color
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetContent(
    state: MapState,
    onFiltroTitularidadChange: (TitularidadFiltro) -> Unit,
    onFiltroTipoCentroChange: (TipoCentroFiltro) -> Unit,
    onColegioClick: (ColegioConDistancia) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.filtrosTitularidad, state.filtrosTipoCentro) {
        listState.scrollToItem(0)
    }

    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Text(
            text = "Centros cercanos",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        FiltroChipRow(
            titulo = "Titularidad",
            opciones = TitularidadFiltro.entries,
            seleccionados = state.filtrosTitularidad,
            etiqueta = { it.label },
            colorChip = { colorParaTitularidadFiltro(it) },
            onSeleccionar = onFiltroTitularidadChange
        )

        Spacer(modifier = Modifier.height(8.dp))

        FiltroChipRow(
            titulo = "Tipo de centro",
            opciones = TipoCentroFiltro.entries,
            seleccionados = state.filtrosTipoCentro,
            etiqueta = { it.label },
            colorChip = { colorParaTipoCentroFiltro(it) },
            onSeleccionar = onFiltroTipoCentroChange
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (state.colegiosConDistancia.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay centros de este tipo cerca de ti",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(state = listState) {
                items(
                    count = state.colegiosConDistancia.size,
                    key = { index -> state.colegiosConDistancia[index].colegio.id }
                ) { index ->
                    val item = state.colegiosConDistancia[index]
                    val colegio = item.colegio
                    val dist = item.distanciaMetros
                    val distText = if (dist >= 1000) {
                        String.format("%.1f km", dist / 1000)
                    } else {
                        "${dist.toInt()} m"
                    }

                    ListItem(
                        leadingContent = {
                            Badge(containerColor = colorParaTitularidad(colegio.tipo, item.titularidadNormalizada)) {
                                Text((index + 1).toString(), color = Color.White)
                            }
                        },
                        headlineContent = {
                            Text(colegio.nombre, fontWeight = FontWeight.Bold)
                        },
                        supportingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("A $distText de ti")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    colegio.tipo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier.clickable { onColegioClick(item) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
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
        Text(
            text = titulo,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            opciones.forEach { opcion ->
                val selected = opcion in seleccionados
                val color = colorChip(opcion)
                FilterChip(
                    selected = selected,
                    onClick = { onSeleccionar(opcion) },
                    label = { Text(etiqueta(opcion)) },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                Icons.Default.Done,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = color.copy(alpha = 0.12f),
                        labelColor = color,
                        selectedContainerColor = color,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = color.copy(alpha = 0.5f),
                        selectedBorderColor = Color.Transparent
                    )
                )
            }
        }
    }
}
