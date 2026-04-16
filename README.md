# ColeFinder

**ColeFinder** es una aplicación Android nativa para ayudar a docentes a localizar centros educativos cercanos (colegios e institutos) mediante geolocalización y mapas.

## Stack tecnológico

- **Lenguaje**: [Kotlin 2.0.21](https://kotlinlang.org/) (K2).
- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) y [Material 3](https://m3.material.io/).
- **Arquitectura**: **MVVM** con estado unidireccional (`MapState` + `MapViewModel`). La lógica de datos pasa por repositorios; la UI no llama a Supabase directamente.
- **Backend**: [Supabase](https://supabase.com/) (PostgreSQL + PostGIS) y acceso vía RPC PostgREST (`nearby_colegios`).
- **Cliente remoto**: [supabase-kt](https://github.com/supabase-community/supabase-kt) (PostgREST) sobre Ktor.
- **Persistencia local**: [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore) (preferencias de UI, p. ej. hints de long press).
- **Inyección de dependencias**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android).
- **Mapas y ubicación**: [Maps Compose](https://github.com/googlemaps/android-maps-compose), Google Play Services Maps y Fused Location Provider.

## Estructura del código

El módulo es `:app`, namespace `es.colefinder`. Organización real del código fuente:

| Ruta (bajo `app/src/main/java/es/colefinder/`) | Contenido |
|------------------------------------------------|-----------|
| `ui/map/` | `MapScreen`, `MapViewModel`, `MapState`, filtros (`CentroFiltros`), detalle en pantalla |
| `data/` | Cliente Supabase (`Supabase.kt`), modelos (`Colegio`, `NearbyColegioDto`), repositorios |
| `data/network/` | Clasificación de errores de carga de colegios (`ColegiosLoadError`, `ColegiosLoadException`) |
| `di/` | Módulos Hilt (`NetworkModule`, `RepositoryModule`) |

Los modelos de dominio usados por el mapa viven en `data/model/`; no hay un paquete `domain/` separado.

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

## Depuración de conectividad (carga de colegios)

Si falla la RPC en algunas redes (p. ej. datos móviles), los fallos se registran con el tag **`SupabaseColegioRepo`** en nivel **Error**, con una categoría entre corchetes (`[DNS]`, `[TIMEOUT]`, `[SSL]`, etc.) y el detalle técnico en el mensaje. El `MapViewModel` también escribe en **`MapViewModel`** al fallar la carga.

Ejemplo de filtro en consola:

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
