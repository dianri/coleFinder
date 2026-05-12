-- =============================================================================
-- Paso 0: UNIQUE (fuente, fuente_id) en staging.colegios (alineado con public)
-- =============================================================================
-- Idempotente: si el constraint ya existe, no falla.
-- Ejecutar ANTES de import_mercadata_to_staging_colegios.sql

DO $$
BEGIN
    -- Deduplicación previa (conservar la fila de menor id por par fuente/fuente_id)
    DELETE FROM staging.colegios c
    USING (
        SELECT id
        FROM (
            SELECT id,
                   ROW_NUMBER() OVER (
                       PARTITION BY fuente, fuente_id
                       ORDER BY id
                   ) AS rn
            FROM staging.colegios
        ) t
        WHERE t.rn > 1
    ) d
    WHERE c.id = d.id;

    ALTER TABLE staging.colegios
        ADD CONSTRAINT colegios_fuente_fuente_id_key UNIQUE (fuente, fuente_id);
EXCEPTION
    WHEN duplicate_object THEN
        NULL;
END;
$$;
