# ColeFinder

**ColeFinder** es una aplicación Android nativa para ayudar a docentes a localizar centros educativos cercanos (colegios e institutos) mediante geolocalización y mapas.

![CI](https://github.com/dianri/coleFinder/actions/workflows/ci.yml/badge.svg?branch=main)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-4285F4?logo=android&logoColor=white)
![License](https://img.shields.io/badge/license-AGPL--3.0-blue)
[![Presentación](https://img.shields.io/badge/Presentación-Gamma-6C47FF?logoColor=white)](https://gamma.app/docs/ColeFinder-yj5kcsgludb2gd4)

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
| `update/` | Sistema de actualizaciones in-app: `InAppUpdateManager` (gestiona una única instancia de `AppUpdateManager`, soporta flujos FLEXIBLE e IMMEDIATE), `InAppUpdateUiState` (estado UI del banner) |

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

> Necesitas un proyecto Supabase activo con el backend configurado.
> Ver [`supabase/setup/README.md`](./supabase/setup/README.md).

## Backend (Supabase)

El backend es un proyecto [Supabase](https://supabase.com) con PostGIS.
La carpeta [`supabase/setup/`](./supabase/setup/) contiene scripts SQL
idempotentes para levantar el backend desde cero en cualquier proyecto
Supabase nuevo.

**Para replicar el entorno:** sigue las instrucciones de
[`supabase/setup/README.md`](./supabase/setup/README.md). En resumen:

1. Crea un proyecto en [supabase.com](https://supabase.com) y activa la extensión **PostGIS** (*Database → Extensions*).
2. Si usas supabase.com (hosted), expone el esquema `staging` en **INTEGRATIONS → Data API → Settings → Exposed Schemas** antes de ejecutar ningún script.
3. Ejecuta los 8 scripts de `supabase/setup/` en el SQL Editor en el orden indicado en el README de esa carpeta.
4. Rellena `secrets.properties` con la URL base y la anon key del proyecto (ver sección anterior).

> **Datos de muestra:** los scripts incluyen ~500 centros del centro de
> Madrid como muestra funcional. La app publicada en Google Play conecta
> al backend de producción con el dataset nacional completo.

> Los scripts de `supabase/scripts/` son de mantenimiento interno
> (imports, promotes). No son necesarios para levantar el entorno
> desde cero.

## Compilación

Hay dos variantes de debug; el task `compileDebugKotlin` es ambiguo. Ejemplos:

```bash
./gradlew :app:compilePreDebugKotlin
./gradlew :app:compileProdDebugKotlin
./gradlew :app:assemblePreDebug
./gradlew :app:assembleProdDebug
```

## Tests

Las pruebas unitarias usan **JUnit 4**, **MockK** y **kotlinx-coroutines-test**.

```bash
# Ejecutar tests unitarios (flavor pre)
./gradlew :app:testPreDebugUnitTest

# Ejecutar con informe de cobertura Jacoco (pre + prod)
./gradlew :app:jacocoFullReport
```

El informe HTML se genera en:
`app/build/reports/jacoco/jacocoFullReport/html/index.html`

## CI/CD

El workflow de GitHub Actions (`.github/workflows/ci.yml`) se ejecuta en cada push y pull request a `develop` y `main`:

| Job | Qué hace |
|-----|----------|
| **Assemble Debug** | Compila ambos flavors (pre + prod) |
| **Unit Tests** | Ejecuta tests y genera cobertura Jacoco |

Los artefactos de cobertura se guardan 14 días en cada ejecución de CI.

## Funcionalidades principales

- **Mapa interactivo** con marcadores codificados por color (titularidad) y numerados.
- **Búsqueda híbrida**: la SearchBar busca simultáneamente por nombre de centro (Supabase) y por ubicación/dirección (Geocoder). Los resultados de centros aparecen primero con icono de escuela; las ubicaciones con icono de pin.
- **Filtros** por titularidad (Público, Concertado, Privado) y tipo de centro (Primaria, Secundaria, FP, Adultos, Especial, Otros), aplicados en servidor y con fallback en cliente.
- **Detalle del centro** con navegación a Google Maps y llamada.
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

## Despliegue

La app está publicada en **Google Play** (flavor `prod`, esquema `public`):

> 🔗 **[ColeFinder en Google Play](https://play.google.com/store/apps/details?id=es.colefinder)**

La versión publicada conecta al backend de producción con el dataset
nacional completo de centros educativos de toda España.

## Presentación del proyecto

Presentación del TFM realizada con Gamma:

> 🔗 **[Ver presentación](https://gamma.app/docs/ColeFinder-yj5kcsgludb2gd4)**

## Flujo de ramas

| Rama | Propósito |
|------|-----------|
| `main` | Código de producción estable |
| `develop` | Rama de integración principal |
| `feature/*` | Nuevas funcionalidades |

Las PRs van siempre de `feature/*` → `develop` → `main`.

## Licencia

Copyright (C) 2026 Diego Angel Fernandez Garcia

Este proyecto está licenciado bajo la **GNU Affero General Public License v3.0 (AGPL-3.0)**.

Esto significa que:
- Puedes ver, estudiar y modificar el código libremente.
- Si distribuyes una versión modificada o la usas en un servicio, **debes publicar el código fuente** bajo la misma licencia.
- No está permitido publicar esta aplicación o una derivada en tiendas de apps (Google Play, etc.) sin publicar también el código fuente completo.

Ver el archivo [LICENSE](./LICENSE) para el texto completo.

---

Gracias por contribuir o probar ColeFinder.
