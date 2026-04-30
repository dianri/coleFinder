# Database Environment Workflow: Staging & Production

Este documento define la arquitectura Multi-Entorno de ColeFinder y los procedimientos operativos para la gestión de datos y lógica en Supabase.

## 1. Arquitectura de Entornos

ColeFinder utiliza **un único proyecto de Supabase** con aislamiento por esquemas de base de datos (`Postgres Schemas`).

| Entorno | Android Flavor | Schema DB | Acceso API | Notas |
|---|---|---|---|---|
| **PRE (Staging)** | `pre` | `staging` | `anon` key + `Accept-Profile: staging` | Datos de prueba y validación |
| **PROD (Production)** | `prod` | `public` | `anon` key + `Accept-Profile: public` | Datos reales servidos a usuarios |

## 2. Flujo de Trabajo y Promoción

### A. Gestión de Datos (`colegios`)

Los datos **nunca** se editan directamente en el esquema `public`.

1. **Ingesta en PRE**: Cargar nuevos datos o realizar correcciones en la tabla `staging.colegios`.
2. **Validación**: Verificar en la App Android (flavor `pre`) que los datos aparecen correctamente, están bien geolocalizados y los filtros funcionan.
3. **Promoción**: Una vez validados, sincronizar `staging` -> `public` usando el playbook de promoción.
4. **Resguardo**: El esquema `import` contiene las fuentes originales para auditoría o re-importación.

### B. Gestión de Lógica (`RPC`)

Las funciones SQL (como `nearby_colegios`) deben validarse en `staging` antes de actualizar `public`.

1. **Desarrollo**: Crear o modificar la función en `staging.nearby_colegios`.
2. **Prueba**: Cambiar el `search_path` de la función a `staging, public, extensions` durante el desarrollo.
3. **Promoción**: Copiar el DDL validado a `public.nearby_colegios`.
4. **Limpieza**: Asegurarse de que no queden firmas de funciones obsoletas (mismo nombre, distintos parámetros).

## 3. Seguridad y Secretos

### Reglas Críticas
- **`secrets.properties`**: Este archivo contiene las llaves reales y **NUNCA** debe ser incluido en el control de versiones. (Verificado en `.gitignore`).
- **`secrets.defaults.properties`**: Contiene solo placeholders. Es el único archivo de configuración de secretos que se versiona.
- **Grants**: El rol `anon` debe tener permisos explícitos de `USAGE` sobre el esquema `staging` y `SELECT`/`EXECUTE` sobre sus objetos.

## 4. Troubleshooting: Refresh de Cache

PostgREST (la API de Supabase) cachea la estructura de la base de datos. Si tras una promoción los cambios no se ven en la API:

1. Ejecutar en el Editor SQL:
   ```sql
   NOTIFY pgrst, 'reload schema';
   ```
2. O reiniciar el contenedor de la API (si es auto-hosted):
   ```bash
   docker compose restart rest
   ```

---
> [!IMPORTANT]
> Todas las peticiones RPC desde Android en el entorno PRE deben forzar explícitamente el esquema `staging` en el constructor de la petición para garantizar que se envíe la cabecera `Content-Profile`.
