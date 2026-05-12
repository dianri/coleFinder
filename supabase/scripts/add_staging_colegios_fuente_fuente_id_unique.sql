-- =============================================================================
-- Paso 0: UNIQUE (fuente, fuente_id) en staging.colegios (alineado con public)
-- =============================================================================
-- Idempotente: si el constraint ya existe, no falla.
-- Ejecutar ANTES de import_mercadata_to_staging_colegios.sql

DO $$
BEGIN
    ALTER TABLE staging.colegios
        ADD CONSTRAINT colegios_fuente_fuente_id_key UNIQUE (fuente, fuente_id);
EXCEPTION
    WHEN duplicate_object THEN
        NULL;
END;
$$;
