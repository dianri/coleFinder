# ColeFinder

**ColeFinder** es una aplicación Android nativa para ayudar a docentes a localizar centros educativos cercanos (colegios e institutos) mediante geolocalización y mapas.

## Stack tecnológico

- **Lenguaje**: [Kotlin 2.0.21](https://kotlinlang.org/) (K2).
- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) y [Material 3](https://m3.material.io/).
- **Arquitectura**: **MVVM** con estado unidireccional (`MapState` + `MapViewModel`). La lógica de datos pasa por repositorios; la UI no llama a Supabase directamente.
- **Backend**: [Supabase](https://supabase.com/) (PostgreSQL + PostGIS). Acceso vía RPC PostgREST (`nearby_colegios`) y consultas directas (`from("colegios")`).
- **Cliente remoto**: [supabase-kt](https://github.com/supabase-community/supabase-kt) 3.0.3 (PostgREST) sobre Ktor.
- **Persistencia local**: [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore) (preferencias de UI, p. ej. hints de long press).
- **Inyección de dependencias**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android).
- **Mapas y ubicación**: [Maps Compose](https://github.com/googlemaps/android-maps-compose), Google Play Services Maps y Fused Location Provider.

## Estructura del código

El módulo es `:app`, namespace `es.colefinder`. Organización real del código fuente:

| Ruta (bajo `app/src/main/java/es/colefinder/`) | Contenido |
|------------------------------------------------|-----------|
| `ui/map/` | `MapScreen`, `MapViewModel`, `MapState`, filtros (`CentroFiltros`), detalle en pantalla |
| `data/` | Cliente Supabase (`Supabase.kt`), repositorios (`ColegioRepository`, `SupabaseColegioRepository`, `UserPreferencesRepository`) |
| `data/model/` | Modelos de dominio (`Colegio`) y DTOs (`NearbyColegioDto`, `ColegioSearchDto`) |
| `data/network/` | Clasificación de errores de red (`ColegiosLoadError`, `ColegiosLoadException`) |
| `di/` | Módulos Hilt (`NetworkModule`, `RepositoryModule`) |

No hay un paquete `domain/` separado.

## Product flavors (PRE / PROD)

Hay dos sabores en la dimensión `env`:

| Flavor | `applicationId` | Esquema Postgres (`BuildConfig.SUPABASE_SCHEMA`) | Uso típico |
|--------|-------------------|---------------------------------------------------|------------|
| **pre** | `es.colefinder.pre` | `staging` | Staging / validación contra BD de pruebas |
| **prod** | `es.colefinder` | `public` | Producción |

Las URLs y claves anon se inyectan por flavor en `BuildConfig` desde secretos (ver siguiente sección).

## Configuración de secretos

1. Copia `secrets.defaults.properties` como **`secrets.properties`** en la **raíz del proyecto** (el archivo real no se versiona; está en `.gitignore`).
2. Rellena al menos:

```properties
SUPABASE_URL_PRE=https://<tu-proyecto>.supabase.co
SUPABASE_ANON_KEY_PRE=<anon-key-staging>
SUPABASE_URL_PROD=https://<tu-proyecto-o-custom>.supabase.co
SUPABASE_ANON_KEY_PROD=<anon-key-prod>
MAPS_API_KEY=<tu-clave-maps>
```

- **Supabase**: URL base del proyecto (sin sufijos `/rest/v1`; el SDK los resuelve). Puedes usar el host `*.supabase.co` o un dominio custom si tu infraestructura lo expone así.
- **Google Maps**: la clave se inyecta en el manifest mediante el [Secrets Gradle Plugin](https://developers.google.com/maps/documentation/android-sdk/secrets-gradle-plugin); el placeholder en `AndroidManifest.xml` es `${MAPS_API_KEY}`. No hace falta pegar la clave a mano en el XML si `secrets.properties` está bien configurado.

## Compilación

Hay dos variantes de debug; el task `compileDebugKotlin` es ambiguo. Ejemplos:

```bash
./gradlew :app:compilePreDebugKotlin
./gradlew :app:compileProdDebugKotlin
./gradlew :app:assemblePreDebug
./gradlew :app:assembleProdDebug
```

## Funcionalidades principales

- **Mapa interactivo** con marcadores codificados por color (titularidad) y numerados.
- **Búsqueda híbrida**: la SearchBar busca simultáneamente por nombre de centro (Supabase) y por ubicación/dirección (Geocoder). Los resultados de centros aparecen primero con icono de escuela; las ubicaciones con icono de pin.
- **Filtros** por titularidad (Público, Concertado, Privado) y tipo de centro (Primaria, Secundaria, FP, Adultos, Especial, Otros), aplicados en servidor y con fallback en cliente.
- **Detalle del centro** con navegación a Google Maps, llamada y favoritos (in-memory).
- **Long press** en el mapa para explorar centros en cualquier punto.

## Depuración de conectividad

Los fallos de carga se clasifican automáticamente (`DNS`, `TIMEOUT`, `SSL`, `HTTP_CLIENT`, `HTTP_SERVER`, etc.) y se registran con el tag **`SupabaseColegioRepo`** en nivel **Error**. El usuario ve un mensaje legible; Logcat muestra la categoría y el detalle técnico.

```bash
adb logcat -s SupabaseColegioRepo:E MapViewModel:E
```

## Clonar el repositorio

```bash
git clone git@github.com:dianri/coleFinder.git
cd coleFinder
```

Crea `secrets.properties` como se indica arriba antes de compilar.

---

Gracias por contribuir o probar ColeFinder.
