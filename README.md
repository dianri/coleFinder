# ColeFinder

ColeFinder es una aplicación Android desarrollada con Jetpack Compose y Clean Architecture cuyo objetivo es ayudarte a localizar colegios e institutos cercanos en un mapa.

## Estado del proyecto

Actualmente en desarrollo. La primera versión muestra solo el mapa. Próximamente se añadirá barra de búsqueda y puntos de interés.

## Características

- Jetpack Compose para el UI moderno.
- Arquitectura Clean Architecture con separación en capas: `presentation`, `domain`, y `data`.
- Soporte para Google Maps.

## Estructura del proyecto

es.colefinder/
│
├── presentation/ # UI y pantallas (Compose)
├── domain/ # Lógica de negocio y casos de uso
├── data/ # Fuentes de datos, repositorios y modelos de red

text

## Requisitos

- Android Studio (Electric Eel o superior recomendado)
- Android SDK 30 o superior
- Kotlin como lenguaje principal
- Clave de API de Google Maps configurada en `AndroidManifest.xml`

## Instalación y ejecución

1. Clona este repositorio:
git@github.com:dianri/coleFinder.git

text

2. Abre el proyecto en Android Studio.

3. Añade tu clave de API de Google Maps en `app/src/main/AndroidManifest.xml`:
<meta-data android:name="com.google.android.geo.API_KEY" android:value="TU_API_KEY_AQUI"/>

text

4. Compila y ejecuta la aplicación en un emulador o dispositivo físico.

## Licencia

[MIT](LICENSE)

---

¡Gracias por contribuir o probar ColeFinder!