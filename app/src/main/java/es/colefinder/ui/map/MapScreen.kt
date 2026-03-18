package es.colefinder.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
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

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ColeFinder - Mapa de Colegios") })
        },
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
                    Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación")
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
                properties = MapProperties(
                    isMyLocationEnabled = locationPermissionsState.allPermissionsGranted
                )
            ) {
                // Iteramos sobre la lista ordenada y filtrada para asignar la numeración
                state.colegiosConDistancia.forEachIndexed { index, colegioConDist ->
                    val colegio = colegioConDist.colegio
                    val numberLabel = (index + 1).toString()
                    val markerColor = if (colegio.tipo.contains("Público", true)) Color(0xFF4CAF50) else Color(0xFF2196F3)
                    
                    // Generamos el icono numerado dinámico
                    val icon = remember(colegio.id, numberLabel, markerColor) {
                        createNumberedMarkerBitmap(context, numberLabel, markerColor)
                    }

                    Marker(
                        state = MarkerState(position = LatLng(colegio.latitud, colegio.longitud)),
                        title = colegio.nombre,
                        snippet = colegio.tipo,
                        icon = icon
                    )
                }
            }

            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = {
                    viewModel.buscarDireccion(searchQuery, context)
                    searchActive = false
                },
                active = searchActive,
                onActiveChange = { searchActive = it },
                placeholder = { Text("Buscar dirección...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {}

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
                    onFilterChange = { viewModel.setFiltro(it) },
                    onColegioClick = { latLng ->
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                        viewModel.moverAColegio(latLng)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetContent(
    state: MapState,
    onFilterChange: (String) -> Unit,
    onColegioClick: (LatLng) -> Unit
) {
    val listState = rememberLazyListState()
    val categorias = listOf("Todos", "Público", "Concertado")

    LaunchedEffect(state.filtroSeleccionado) {
        listState.scrollToItem(0)
    }

    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Text(
            text = "Colegios cercanos",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            categorias.forEach { categoria ->
                val selected = state.filtroSeleccionado == categoria
                FilterChip(
                    selected = selected,
                    onClick = { onFilterChange(categoria) },
                    label = { Text(categoria) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                    } else null,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        if (state.colegiosConDistancia.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay colegios de este tipo cerca de ti",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(state = listState) {
                items(state.colegiosConDistancia.size) { index ->
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
                            Badge(
                                containerColor = if (colegio.tipo.contains("Público", true)) Color(0xFF4CAF50) else Color(0xFF2196F3)
                            ) {
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
                                Text(colegio.tipo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier.clickable {
                            onColegioClick(LatLng(colegio.latitud, colegio.longitud))
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}
