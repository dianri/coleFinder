-- =============================================================================
-- Promoción incremental: staging → public SOLO filas fuente = 'mercadata'
-- =============================================================================
-- EJECUTAR SOLO cuando hayas validado staging y apruebes promoción a PROD.
-- NO sustituye promote_colegios_data.sql (sync completo); evita TRUNCATE en public.
--
-- Orden: DELETE previo mercadata en public + INSERT desde staging + ajuste secuencia id.
-- =============================================================================

BEGIN;

DELETE FROM public.colegios
WHERE fuente = 'mercadata';

INSERT INTO public.colegios (
    nombre,
    direccion,
    latitud,
    longitud,
    tipo,
    localidad,
    fuente,
    fuente_id,
    descripcion_entidad,
    descripcion,
    content_url,
    clase_vial,
    nombre_via,
    tipo_num,
    num,
    codigo_postal,
    provincia,
    barrio,
    cod_barrio,
    distrito,
    cod_distrito,
    telefono,
    fax,
    email,
    accesibilidad,
    categoria_madrid,
    tipo_centro_normalizado,
    titularidad_normalizada,
    location,
    es_dificil_desempeno,
    jornada_tipo,
    es_rural
)
SELECT
    nombre,
    direccion,
    latitud,
    longitud,
    tipo,
    localidad,
    fuente,
    fuente_id,
    descripcion_entidad,
    descripcion,
    content_url,
    clase_vial,
    nombre_via,
    tipo_num,
    num,
    codigo_postal,
    provincia,
    barrio,
    cod_barrio,
    distrito,
    cod_distrito,
    telefono,
    fax,
    email,
    accesibilidad,
    categoria_madrid,
    tipo_centro_normalizado,
    titularidad_normalizada,
    location,
    es_dificil_desempeno,
    jornada_tipo,
    es_rural
FROM staging.colegios
WHERE fuente = 'mercadata';

SELECT setval(
    pg_get_serial_sequence('public.colegios', 'id'),
    COALESCE((SELECT MAX(id) FROM public.colegios), 1)
);

COMMIT;
