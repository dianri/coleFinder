-- PLAYBOOK: Promoción de Datos de PRE (staging) a PROD (public)
-- Uso: Ejecutar en el Editor SQL de Supabase para sincronizar la tabla de colegios.

BEGIN;

-- 1. Limpiar producción (opcional: o usar un delta si se prefiere)
-- En este caso, hacemos una sincronización completa para asegurar paridad.
TRUNCATE TABLE public.colegios;

-- 2. Insertar desde staging preservando IDs y estructura
INSERT INTO public.colegios (
    id, nombre, direccion, localidad, provincia, tipo, latitud, longitud, 
    telefono, fuente, fuente_id, descripcion_entidad, location,
    tipo_centro_normalizado, titularidad_normalizada, 
    es_dificil_desempeno, jornada_tipo, es_rural
)
SELECT 
    id, nombre, direccion, localidad, provincia, tipo, latitud, longitud, 
    telefono, fuente, fuente_id, descripcion_entidad, location,
    tipo_centro_normalizado, titularidad_normalizada, 
    es_dificil_desempeno, jornada_tipo, es_rural
FROM staging.colegios;

-- 3. Actualizar la secuencia de IDs para evitar conflictos en futuras inserciones
SELECT setval(
    pg_get_serial_sequence('public.colegios', 'id'), 
    COALESCE(MAX(id), 1)
) FROM public.colegios;

-- 4. Verificación de conteo
DO $$
DECLARE
    count_staging INTEGER;
    count_public INTEGER;
BEGIN
    SELECT count(*) INTO count_staging FROM staging.colegios;
    SELECT count(*) INTO count_public FROM public.colegios;
    
    IF count_staging <> count_public THEN
        RAISE EXCEPTION 'Error de paridad: staging (%) vs public (%)', count_staging, count_public;
    ELSE
        RAISE NOTICE 'Promoción exitosa: % registros sincronizados.', count_public;
    END IF;
END $$;

COMMIT;
