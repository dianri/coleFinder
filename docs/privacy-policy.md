# Política de Privacidad de ColeFinder

**Última actualización:** 6 de mayo de 2026

## 1. Introducción

ColeFinder es una aplicación Android desarrollada por Diego Ángel Fernández García que ayuda a docentes a localizar centros educativos cercanos mediante geolocalización y mapas. Esta política de privacidad explica qué datos se recopilan, cómo se usan y qué derechos tienes como usuario.

## 2. Datos que recopilamos

### 2.1 Datos de ubicación

ColeFinder solicita acceso a la **ubicación del dispositivo** (latitud y longitud) para las siguientes funciones:

- Mostrar los centros educativos más cercanos a tu posición actual.
- Permitir explorar colegios en cualquier punto del mapa mediante pulsación prolongada.

Las coordenadas GPS se transmiten al servidor de base de datos (Supabase) exclusivamente para realizar la consulta de centros cercanos. **No se almacenan ni se asocian a ningún perfil de usuario.**

### 2.2 Preferencias de la aplicación

La app guarda localmente en tu dispositivo preferencias mínimas de interfaz (por ejemplo, si ya has visto los consejos de uso). Estos datos **nunca se envían a ningún servidor externo**.

## 3. Datos que NO recopilamos

ColeFinder **no recopila** ninguno de los siguientes datos:

- Nombre, correo electrónico u otros datos personales identificativos.
- Historial de búsquedas o centros visitados.
- Datos de pago o financieros.
- Información del dispositivo más allá de lo necesario para el funcionamiento de Google Maps y Supabase.

La aplicación **no requiere registro ni inicio de sesión**.

## 4. Cómo usamos los datos

| Dato | Finalidad | Se almacena en servidor |
|------|-----------|------------------------|
| Ubicación (lat/lon) | Buscar centros educativos cercanos | No |
| Preferencias de UI | Recordar configuración local | No (solo en el dispositivo) |

## 5. Servicios de terceros

ColeFinder utiliza los siguientes servicios externos:

- **Supabase** (supabase.com): base de datos con la información pública de centros educativos. Recibe las coordenadas del usuario para devolver los centros cercanos. Consulta su [política de privacidad](https://supabase.com/privacy).
- **Google Maps Platform**: para la visualización del mapa y los marcadores de centros. Consulta la [política de privacidad de Google](https://policies.google.com/privacy).
- **Google Play Services**: requerido para el proveedor de ubicación fusionada (Fused Location Provider).

## 6. Permisos solicitados

| Permiso | Motivo |
|---------|--------|
| `ACCESS_FINE_LOCATION` | Obtener la ubicación precisa para buscar centros cercanos |
| `ACCESS_COARSE_LOCATION` | Ubicación aproximada como alternativa |
| `INTERNET` | Consultar la base de datos de centros y cargar el mapa |

## 7. Menores de edad

ColeFinder está dirigida a **docentes y adultos**. No está diseñada para ser utilizada por menores de 13 años y no recopila intencionadamente datos de menores.

## 8. Seguridad de los datos

Las comunicaciones entre la aplicación y el servidor de Supabase se realizan mediante **HTTPS con cifrado TLS**. Las coordenadas transmitidas no se persisten en ningún servidor y se usan únicamente para resolver la consulta en tiempo real.

## 9. Retención de datos

ColeFinder **no almacena datos de usuarios en servidores externos**. Las preferencias locales se eliminan automáticamente al desinstalar la aplicación.

## 10. Tus derechos

Dado que ColeFinder no almacena datos personales identificativos en ningún servidor, no es posible solicitar acceso, rectificación ni eliminación de datos. Si tienes alguna duda, puedes contactar con el desarrollador a través del correo indicado a continuación.

## 11. Cambios en esta política

Cualquier cambio en esta política de privacidad se publicará en esta misma URL con la fecha de actualización correspondiente. Se recomienda revisarla periódicamente.

## 12. Contacto

Si tienes preguntas sobre esta política de privacidad, puedes contactar con el desarrollador:

**Diego Ángel Fernández García**  
Correo electrónico: [dianrie@hotmail.com](mailto:dianrie@hotmail.com)    
GitHub: [https://github.com/dianri/coleFinder](https://github.com/dianri/coleFinder)
