-- ColeFinder – Funciones RPC (PRE / staging)
-- Ejecutar después de staging/01_schema.sql

CREATE OR REPLACE FUNCTION staging.nearby_colegios(
  p_lat double precision,
  p_lon double precision,
  p_limit integer DEFAULT 50,
  p_titularidades text[] DEFAULT NULL::text[],
  p_tipos text[] DEFAULT NULL::text[]
)
RETURNS TABLE(
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
SET search_path TO staging, public, extensions
AS $function$
DECLARE
  v_limit integer;
BEGIN
  SELECT COALESCE(NULLIF(trim(value), '')::integer, 200)
  INTO v_limit
  FROM staging.app_config
  WHERE key = 'nearby_colegios_limit'
  LIMIT 1;

  IF v_limit IS NULL OR v_limit < 1 THEN
    v_limit := 200;
  END IF;
  v_limit := LEAST(v_limit, 500);

  RETURN QUERY
  SELECT
    c.id,
    c.nombre,
    c.direccion,
    c.localidad,
    c.provincia,
    c.tipo,
    c.latitud,
    c.longitud,
    c.telefono,
    c.descripcion_entidad,
    c.tipo_centro_normalizado,
    c.titularidad_normalizada,
    ST_DistanceSphere(
      ST_MakePoint(p_lon, p_lat),
      ST_MakePoint(c.longitud, c.latitud)
    ) AS distancia_metros,
    c.es_dificil_desempeno,
    c.jornada_tipo,
    c.es_rural
  FROM staging.colegios c
  WHERE
    (p_titularidades IS NULL OR c.titularidad_normalizada = ANY(p_titularidades))
    AND (p_tipos IS NULL OR c.tipo_centro_normalizado = ANY(p_tipos))
  ORDER BY
    c.location <-> ST_SetSRID(ST_MakePoint(p_lon, p_lat), 4326)::geography
  LIMIT v_limit;
END;
$function$;

COMMENT ON FUNCTION staging.nearby_colegios(double precision, double precision, integer, text[], text[]) IS
  'RPC PRE: N centros más cercanos; límite desde staging.app_config.nearby_colegios_limit.';

GRANT EXECUTE ON FUNCTION staging.nearby_colegios(
  double precision,
  double precision,
  integer,
  text[],
  text[]
) TO anon, authenticated;
