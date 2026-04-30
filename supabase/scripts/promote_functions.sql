-- PLAYBOOK: Promoción de Funciones SQL de PRE (staging) a PROD (public)
-- Uso: Copiar la definición validada en staging y aplicarla en public.

BEGIN;

-- 1. Definición de la función (EJEMPLO: nearby_colegios)
-- Sustituir por la versión actual validada en staging.
CREATE OR REPLACE FUNCTION public.nearby_colegios(
    p_lat           double precision,
    p_lon           double precision,
    p_limit         integer DEFAULT 50,
    p_titularidades text[]  DEFAULT NULL,
    p_tipos         text[]  DEFAULT NULL
)
RETURNS TABLE (
    id integer,
    nombre text,
    direccion text,
    localidad text,
    provincia text,
    tipo text,
    latitud double precision,
    longitud double precision,
    telefono text,
    descripcion_entidad text,
    tipo_centro_normalizado text,
    titularidad_normalizada text,
    distancia_metros double precision,
    es_dificil_desempeno boolean,
    jornada_tipo text,
    es_rural boolean
) 
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, extensions
AS $$
BEGIN
    RETURN QUERY
    SELECT 
        c.id, c.nombre, c.direccion, c.localidad, c.provincia, c.tipo, 
        c.latitud, c.longitud, c.telefono, c.descripcion_entidad,
        c.tipo_centro_normalizado, c.titularidad_normalizada,
        ST_Distance(c.location, ST_SetSRID(ST_MakePoint(p_lon, p_lat), 4326)::geography) as distancia_metros,
        c.es_dificil_desempeno, c.jornada_tipo, c.es_rural
    FROM public.colegios c
    WHERE 
        (p_titularidades IS NULL OR c.titularidad_normalizada = ANY(p_titularidades))
        AND (p_tipos IS NULL OR c.tipo_centro_normalizado = ANY(p_tipos))
    ORDER BY c.location <-> ST_SetSRID(ST_MakePoint(p_lon, p_lat), 4326)::geography
    LIMIT p_limit;
END;
$$;

-- 2. Asegurar permisos para la API
ALTER FUNCTION public.nearby_colegios OWNER TO postgres;
GRANT EXECUTE ON FUNCTION public.nearby_colegios TO anon;
GRANT EXECUTE ON FUNCTION public.nearby_colegios TO authenticated;

-- 3. Refrescar cache de PostgREST
NOTIFY pgrst, 'reload schema';

COMMIT;

-- VERIFICACIÓN
SELECT pronamespace::regnamespace as schema, proname, pg_get_function_arguments(oid) 
FROM pg_proc 
WHERE proname = 'nearby_colegios';
