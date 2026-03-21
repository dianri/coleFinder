# ColeFinder - Continuation Rules
# Archivo de Guía para el Desarrollo y Mantenimiento del Proyecto

## 📌 Stack Tecnológico (PRIORIDAD MÁXIMA)

### Configuración Esencial
| Tecnología | Versión | Notas Importantes |
|------------|---------|-------------------|
| **Kotlin** | 2.0.21 | Target SDK 36 con KSP activado |
| **Jetpack Compose** | BOM 2024.09.00 | Material Design 3 (MD3) |
| **Google Maps SDK** | 6.1.2 | `maps-compose` v6.1.2 |
| **Hilt** | 2.51.1 | Inyección de dependencias |
| **Supabase SDK** | 3.0.3 | `postgrest-kt` de `io.github.jan-tennert.supabase` |

### ⚠️ CONFIGURACIÓN CRÍTICA: Host de Supabase Personalizado
```kotlin
// ¡NO USAR las URLs estándar de supabase.co!
// Tu backend está alojado en Proxmox:
val SUPABASE_URL = "https://api.dianrie.com"
val SUPABASE_ANON_KEY = "...tuna" // Desde tu proyecto en Proxmox

// Configuración del cliente en Supabase.kt
fun provideSupabaseClient(): Provider<SupabaseClient> = provider {
    SupabaseClient(SUPABASE_URL, SUPABASE_ANON_KEY)
}
```

### Dependencias en `build.gradle.kts`
```kotlin
implementation("io.github.jan-tennert.supabase:postgrest-kt:3.0.3")
implementation("com.google.maps:maps-compose:6.1.2")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
implementation("androidx.room:room-runtime:2.6.1") // Para favoritos offline
```

---

## 🗄️ Modelo de Datos (Single Source of Truth)

### Entidad `Colegio`
```kotlin
@Serializable
data class Colegio(
    val id: Int,
    val nombre: String,
    val direccion: String,
    val localidad: String,
    val tipo: String, // Ej: Público, Privado, Concertado
    val latitud: Double,
    val longitud: Double,
    val telefono: String? // Opcional para Intents
)
```

---

## 🏗️ Arquitectura Implementada

### Data Layer
- **SupabaseClient**: Centralizado mediante Hilt en `NetworkModule`
- **Repository**: Maneja la lógica de fetch, filtros y ordenamiento

### UI Layer (MVVM)
- **MapState**: Estado principal que gestiona:
  - `isLoading: Boolean`
  - `colegiosConDistancia: List<Colegio>` (Únicos por ID)
  - `userLocation: Pair<Double, Double>?`
  - `filtroSeleccionado: String?`
  - `cameraPositionState: CameraPositionState`

- **MapViewModel**:
  - Ejecuta Fetch desde Supabase
  - Gestiona Geocoder con *debounce* de 300ms
  - Lógica de ordenación por distancia

### ⚠️ DEDUPPLICACIÓN OBLIGATORIA
```kotlin
// Antes de actualizar el estado
val colegiosUnicos = colegios.filter { !idsYaVistos.contains(it.id) }
    .distinctBy { it.id } // ¡SIEMPRE!
```

---

## 🎨 Componentes de Interfaz y UX

### Buscador (TopAppBar)
- `SearchBar` de Material 3
- Expansible con sugerencias de direcciones
- Integrazione con Geocoder nativo

### Mapa (`GoogleMap.compose`)
```kotlin
GoogleMap(
    mapToolbarEnabled = false,          // Oculta toolbar de Google
    myLocationButtonEnabled = false,    // Oculta botón "Mi ubicación"
    compassEnabled = true               // Brújula visible
) {
    // Marcadores numerados 1-50
    markerOptions {
        position = position
        title = "Col. $index"
        // Configuración de marcador personalizado
    }
}
```

### FABs
- **Mi Ubicación**: Botón personalizado (inferior derecha)
- **Lista**: Botón para ver lista de colegios

### Ficha de Detalle
- `ColegioDetailCard`
- Botones para:
  - Intent de Navegación (`google.navigation`)
  - Intent de Llamada (`ACTION_DIAL`)
- Se oculta al pulsar zona vacía del mapa

---

## 📜 Reglas de Desarrollo para la IA

### Código
- ✅ Generar siempre Kotlin moderno (Kotlin 2.0+)
- ✅ Usar corutinas y Flow para asincronía
- ❌ No usar XML para layouts (solo Compose)
- ✅ Usar `@Inject` para dependencias de Hilt
- ✅ **Kotlin Null Safety (Strict)**: NUNCA encadenes métodos tras un operador seguro (ej. `obj?.doA().doB()`). Usa siempre `.let { it.doA().doB() }` para asegurar que el receptor no sea null durante toda la cadena.
- ✅ **String Cleaning**: Para limpiar strings nullables, usa siempre `telefono?.filter { it.isDigit() } ?: ""` en lugar de múltiples `.replace()` o Regex innecesarios.
- ✅ **Uso de Utilidades**: Antes de implementar lógica de validación de teléfonos o limpieza de strings, verifica si existe `es.colefinder.utils.PhoneValidator.kt`.

### UI/UX
- ✅ Priorizar componentes oficiales de Material 3
- ✅ Evitar redundancias visuales
- ✅ Mantener interfaz limpia y minimalista

### Arquitectura
- ✅ Toda lógica de datos en `MapState`
- ✅ Unidireccionalidad del flujo de datos
- ✅ Separación clara entre UI y lógica

### Rendimiento
- ✅ Implementar `try-catch` en lanzamientos de Intents
- ✅ Evitar cálculos pesados en el Main Thread
- ✅ Usar `Dispatchers.IO` para operaciones de red/BD

### Consumo
- ✅ Priorizar Geocoder nativo (gratuito)
- ❌ Evitar llamadas repetidas a APIs externas costosas

---

## 🎯 Próximos Objetivos (Priorizados)

1. **Detalle Avanzado (UX)**: Implementar Intents de "Cómo llegar" y "Llamar"
2. **Persistencia (Room)**: Crear BD local para favoritos y modo offline
3. **Sincronización**: Lógica de actualización selectiva entre Room y Supabase
4. **Rutas**: Dibujar trayectorias en el mapa hacia el centro seleccionado

---

## 📝 Ejemplos de Código Comunes

### Intent de Navegación
```kotlin
fun openNavigationDestination(latLng: LatLng) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${latLng.latitude},${latLng.longitude}"))
    startActivity(intent)
}
```

### Intent de Llamada
```kotlin
fun openDialIntent(telefono: String?) {
  // Usar siempre la utilidad para limpiar el número antes del Intent
  val numeroLimpio = extraerDigitos(telefono)
  if (validarTelefono(numeroLimpio)) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numeroLimpio"))
    startActivity(intent)
  }
}
```
## 🛠️ Utilidades del Proyecto

### Validación de Teléfonos (`PhoneValidator.kt`)
Ubicación: `app/src/main/java/es/colefinder/utils/PhoneValidator.kt`
- `validarTelefono(String?)`: Retorna `Boolean`. Verifica formato español (>= 9 dígitos).
- `normalizarTelefono(String?)`: Retorna `String?`. Elimina espacios/guiones usando `.let`.
- `extraerDigitos(String?)`: Retorna `String`. Extrae solo números usando `.filter { it.isDigit() }`.

### Cálculo de Distancia
```kotlin
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371000.0 // metros
    // Implementación de la fórmula de Haversine
}