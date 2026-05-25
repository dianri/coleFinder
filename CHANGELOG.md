# Changelog

Todos los cambios notables de este proyecto se documentan aquí.  
Formato: [Keep a Changelog](https://keepachangelog.com/es/1.0.0/).  
Versionado: [Semantic Versioning](https://semver.org/lang/es/).

## [Unreleased]

## [1.1.2] - 2026-05-25

### Added
- Banner de actualización in-app: botón cerrar para posponer aviso de descarga
  o reinicio pendiente hasta la siguiente apertura de la app

### Changed
- Edge-to-edge Android 15: sustituir `enableEdgeToEdge()` (invoca APIs
  deprecadas en androidx.activity) por `WindowCompat.setDecorFitsSystemWindows`
  + cutout `ALWAYS`; `Scaffold` raíz con `contentWindowInsets =
  WindowInsets.systemBars`, mapa a pantalla completa e insets en overlays y sheet
- Actualizaciones in-app: reanuda flujo IMMEDIATE interrumpido al volver a
  primer plano (`onResume`)

## [1.1.1] - 2026-05-24

### Added
- Hint contextual cuando el permiso de ubicación está bloqueado permanentemente:
  banner con instrucciones para activarlo en Ajustes, se abre directamente al
  pulsarlo y se descarta solo tras 5 s

### Fixed
- Crash al arrastrar el mapa durante una animación de cámara
  (CancellationException no capturada en maps-compose 6.x)
- Auto-fit de cámara cuando el usuario está fuera de España: espera a que
  la animación inicial termine (polling isMoving, máx. 3 s) antes de
  evaluar los bounds visibles; consumeFocusedRequest() solo se llama cuando
  la proyección del mapa está disponible, evitando el bucle de reactivación
- Edge-to-edge Android 15: window insets correctos en SearchBar, FAB y bottom
  sheet (windowInsetsPadding, statusBarsPadding, navigationBarsPadding, imePadding)
- Setup staging: política RLS SELECT para authenticated en staging.colegios
  (alineada con GRANT) (interno)
- Migración app_config: RLS para anon, creación idempotente del enum y DROP
  TRIGGER antes de recrear triggers (interno)

### Changed
- Dependencias: maps-compose 6.1.2 → 6.6.0, play-services-maps 19.0.0 → 20.0.0
  (elimina APIs obsoletas setStatusBarColor/setNavigationBarColor en Android 15)
- AppConfigRepository: update_type remoto solo acepta FLEXIBLE o IMMEDIATE;
  resto → fallback FLEXIBLE (interno)

## [1.1.0] - 2026-05-22

### Added
- Actualización forzada configurable: tablas `app_config` en esquemas `staging` y `public`, `AppConfigRepository` y comprobación en arranque con Play In-App Updates (IMMEDIATE o FLEXIBLE según remoto)
- Scripts `supabase/setup/` para replicar backend en proyecto Supabase nuevo (esquemas `public`/`staging`, RPC, `app_config`, seed ~500 colegios del centro de Madrid)

### Changed
- README raíz y `supabase/setup/README.md`: guía de backend, Google Play, presentación TFM y troubleshooting
- Texto del hint de long press en el mapa: «Mantén pulsado el mapa para buscar centros cerca»
- `app_config.value_enum` (enum FLEXIBLE|IMMEDIATE) para editar `update_type` con selector en Supabase; `value` se sincroniza por trigger (interno)
- Config de actualización: una sola tabla `app_config` por entorno (PRE/PROD); eliminadas tablas redundantes `staging_app_config` (interno)
- `app_config` vuelve a formato key/value extensible; nuevo parámetro `nearby_colegios_limit` (POI en mapa)
- RPC `nearby_colegios` lee el límite desde `app_config` e ignora `p_limit` del cliente (apps en pruebas siguen funcionando)
- La app ya no envía `p_limit` en la carga de centros cercanos (interno)
- Una sola instancia de `AppUpdateManager` en `InAppUpdateManager` para flujos flexibles y forzados (interno)

## [1.0.1] - 2026-05-12

### Fixed
- Peticiones a Supabase (RPC cercanos): motor HTTP Ktor con OkHttp, timeouts explícitos y reintentos para reducir fallos intermitentes por conexión cerrada (p. ej. EOF tras keep-alive)
- OkHttp `readTimeout` en 25 s, alineado con `requestTimeout` de Ktor/Supabase (no mayor que el timeout global de la petición)

### Added
- Actualizaciones in-app flexibles (Play App Update): comprobación al volver a primer plano, flujo con Play y banner con "Actualizar" / "Reiniciar e instalar"
- Tests unitarios para CentroFiltros, MapViewModel y MapState (~88% cobertura lógica de negocio)
- Tests de ColegioRepository con Ktor MockEngine
- Reglas de Cursor para flujo de trabajo, tests y calidad de código
- Política de versionado semántico y nombres de APK por flavor

### Changed
- CI: artefacto `debug-apks` solo incluye APKs pre/debug y prod/debug (excluye prod/release con placeholders) (interno)
- Gradle/CI: eliminado `gradle/gradle-daemon-jvm.properties` (forzaba JDK 21 JetBrains vía foojay → 400 en CI); módulo `app` con `kotlin { jvmToolchain(17) }`; `setup-java@v4` con Temurin 17 (interno)
- OpenGrep: `.github/opengrep.yml` con packs `p/kotlin`, `p/android`, `p/secrets` y `p/owasp-top-ten`; `args: --no-color` para logs legibles en CI (interno)
- CI GitHub Actions: `permissions` solo `contents: read`; job Assemble incluye `assembleProdRelease` (interno)
- ProGuard/R8: eliminado keep global de `com.google.android.gms.**`; se confía en reglas de consumidor de los AAR (interno)
- JaCoCo: lista de exclusiones centralizada en `jacocoExcludes`; `jacocoFullReport` incluye `.exec` bajo `jacoco/**` además de unit test code coverage (interno)
- Activado isMinifyEnabled + isShrinkResources en release con reglas ProGuard
- versionName actualizado a formato semver "1.0.0"
- Eliminado versionNameSuffix del flavor pre — el flavor queda identificado por el nombre del APK
- JaCoCo: informes de cobertura unitaria, exclusiones UI/Compose y módulos no unit-testeables; CI publica informe HTML unificado `jacocoFullReport` (pre+prod) como artefacto (interno)
- CI GitHub Actions: actions con versión fija, caché Gradle explícita, Node 24 para JS actions, sin `--no-daemon` en Gradle (interno)
- Corrección de tipo_centro_normalizado en centros Mercadata: 3.842 institutos reclasificados como SECUNDARIA mediante heurística por nombre (IES, Instituto) y clasificación con bachillerato. PRIMARIA pasa de 25.801 a 22.078 centros.
- Aumentado el límite de centros cargados en el mapa de 50 a 200 centros más cercanos.

## [1.0.0] - 2026-05-05

### Added
- Mapa de colegios cercanos con Google Maps Compose
- Filtros por titularidad (Público, Concertado, Privado) y tipo de centro (Primaria, Secundaria, FP, Adultos, Especial, Otros)
- Búsqueda por nombre de colegio con sugerencias en tiempo real
- Búsqueda por dirección con Geocoder
- Long press en mapa para centrar búsqueda en un punto personalizado
- Clasificación automática de centros por siglas del nombre y descripción de entidad
- Integración con Supabase mediante RPC nearby_colegios
- Arquitectura MVVM con Hilt, Jetpack Compose y StateFlow
- Flavors pre (staging) y prod con Supabase separado
- DataStore para persistir preferencias de usuario (longPress discovery hint)
