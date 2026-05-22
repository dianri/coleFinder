# Configuración del entorno Supabase

Instrucciones para replicar el backend de ColeFinder en un proyecto Supabase nuevo, con los dos esquemas que usa la app:

| Esquema | Flavor Android | Uso |
|:---|:---|:---|
| **`staging`** | `pre` | Desarrollo y pruebas (cabecera `Content-Profile: staging`) |
| **`public`** | `prod` | Producción |

## Estructura de carpetas

```text
supabase/setup/
├── README.md
├── public/
│   ├── 01_schema.sql
│   ├── 02_functions.sql
│   ├── 03_app_config.sql
│   └── 04_seed_madrid.sql
└── staging/
    ├── 01_schema.sql
    ├── 02_functions.sql
    ├── 03_app_config.sql
    └── 04_seed_madrid.sql
```

## 1. Requisitos previos

- Cuenta en [Supabase](https://supabase.com) (o instancia self-hosted con PostgREST).
- **PostGIS activado** (*Database → Extensions*).
- Rol `anon` con acceso a PostgREST.

No incluyas credenciales reales en este repositorio.

## ⚠️ Configuración previa en supabase.com (hosted)

Antes de ejecutar ningún script, el esquema `staging` debe estar
expuesto en la API. Por defecto supabase.com solo expone `public`
y `graphql_public`; sin este paso la app en flavor **PRE** fallará
con error `406 Not Acceptable`.

**Pasos:**
1. Supabase Studio → icono **Configuración** (⚙️ esquina inferior izquierda)
2. Menú **INTEGRATIONS** → clic en **Data API**
3. Pestaña central **Settings**
4. Apartado **Exposed Schemas** → añadir `staging` (junto a `public`)
5. Guardar y esperar ~10 segundos a que reinicie la API

> **Self-hosted:** añade `staging` a `PGRST_DB_SCHEMAS` en `config.toml`:
> ```toml
> db-extra-search-path = "public, staging"
> ```

---

## 🌐 URL y claves del proyecto

Rellena `secrets.properties` (copia desde `secrets.defaults.properties`)
con los valores de tu proyecto Supabase:

```text
URL base del proyecto — SIN /rest/v1 ni ningún sufijo
SUPABASE_URL_PRE=https://<tu-proyecto>.supabase.co
SUPABASE_URL_PROD=https://<tu-proyecto>.supabase.co

Anon key (pública) — Settings → API → Project API keys → anon public
SUPABASE_ANON_KEY_PRE=<anon-key>
SUPABASE_ANON_KEY_PROD=<anon-key>

MAPS_API_KEY=<tu-google-maps-api-key>
```

> ⚠️ La URL debe ser **solo el dominio base**. Si incluyes `/rest/v1`
> la app crasheará al arrancar con `IllegalStateException`.

---

## 2. Orden de ejecución

Ejecuta los scripts en el **SQL Editor** en este orden (primero objetos compartidos en `public`, luego `staging`):

| Paso | Archivo | Qué hace |
|:---|:---|:---|
| 1 | `public/01_schema.sql` | Extensiones, enum `app_update_type`, tablas y RLS de **public**, triggers de `location` y `app_config` |
| 2 | `staging/01_schema.sql` | Esquema **staging**, tablas, índices `idx_staging_*`, RLS y trigger `app_config` (reutiliza función de `public`) |
| 3 | `public/02_functions.sql` | RPC `public.nearby_colegios` + `GRANT EXECUTE` |
| 4 | `staging/02_functions.sql` | RPC `staging.nearby_colegios` + `GRANT EXECUTE` |
| 5 | `public/03_app_config.sql` | Configuración remota inicial en **public** |
| 6 | `staging/03_app_config.sql` | Configuración remota inicial en **staging** |
| 7 | `public/04_seed_madrid.sql` | ~500 centros de Madrid en **public** (trigger rellena `location`) |
| 8 | `staging/04_seed_madrid.sql` | Mismos centros en **staging** + `UPDATE` de `location` al final |

Todos los scripts son **idempotentes** (`IF NOT EXISTS`, `CREATE OR REPLACE`, `ON CONFLICT`, etc.).

### Diferencias entre esquemas (como en tu BD actual)

- **public**: `colegios` con PK en `id`; trigger `tr_colegios_location_update`; política RLS `colegios` para `anon` y `authenticated`.
- **staging**: `colegios` sin PK (solo `UNIQUE (fuente, fuente_id)`); **sin** trigger de `location` en tabla (el seed hace un `UPDATE` explícito); RPC y `app_config` en el esquema `staging`.

## 3. Configurar `secrets.properties` en Android

1. Copia `secrets.defaults.properties` → **`secrets.properties`** (no versionado).
2. Rellena URL y anon key de tu proyecto:

```properties
SUPABASE_URL_PRE=https://<tu-proyecto>.supabase.co
SUPABASE_ANON_KEY_PRE=<anon-key>
SUPABASE_URL_PROD=https://<tu-proyecto-o-mismo-proyecto>.supabase.co
SUPABASE_ANON_KEY_PROD=<anon-key>
MAPS_API_KEY=<tu-clave-maps>
```

- **PRE** (`assemblePreDebug`): `BuildConfig.SUPABASE_SCHEMA = staging`
- **PROD** (`assembleProdRelease`): `BuildConfig.SUPABASE_SCHEMA = public`

## 4. Verificar que todo funciona

**Staging (PRE):**

```sql
SELECT key, value, value_enum FROM staging.app_config ORDER BY key;

SELECT id, nombre, distancia_metros
FROM staging.nearby_colegios(40.4168, -3.7038, 50, NULL, NULL)
LIMIT 5;
```

**Public (PROD):**

```sql
SELECT key, value, value_enum FROM public.app_config ORDER BY key;

SELECT id, nombre, distancia_metros
FROM public.nearby_colegios(40.4168, -3.7038, 50, NULL, NULL)
LIMIT 5;
```

En la app PRE, PostgREST debe recibir `Content-Profile: staging` en las llamadas RPC (ya configurado en el cliente según flavor).

## 5. Nota sobre los datos

Los ficheros `04_seed_madrid.sql` contienen ~500 centros educativos
del **centro de Madrid capital** (ordenados por proximidad a Puerta
del Sol), extraídos de fuentes públicas del Ministerio de Educación
y de la Comunidad de Madrid.

Esta muestra es suficiente para probar todas las funcionalidades de
la app: mapa, búsqueda, filtros y detalle de centro.

> **La app publicada en Google Play** conecta al backend de producción,
> que incluye el dataset nacional completo con miles de centros de toda
> España.

## 6. Solución de problemas frecuentes

| Error | Causa | Solución |
|:---|:---|:---|
| `406 Not Acceptable — Invalid schema: staging` | El esquema `staging` no está expuesto en la API | Sigue el paso **⚠️ Configuración previa** de este README |
| `IllegalStateException: url should not contain rest/v1` | La URL en `secrets.properties` incluye el sufijo `/rest/v1` | Usa solo el dominio base: `https://<proyecto>.supabase.co` |
| `42601: INSERT has more target columns than expressions` | Versión antigua del seed con 15 valores por fila en vez de 16 | Usa los archivos actuales del repositorio (ya corregido) |
| La RPC devuelve menos centros de los esperados | `nearby_colegios_limit` en `app_config` es bajo | Ejecuta: `UPDATE public.app_config SET value='200' WHERE key='nearby_colegios_limit';` |
