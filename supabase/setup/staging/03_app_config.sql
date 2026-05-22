-- ColeFinder – Datos iniciales de app_config (PRE / staging)
-- Ejecutar después de staging/01_schema.sql
-- Ajusta min_version_code y update_type según tus necesidades

INSERT INTO staging.app_config (key, value, value_enum)
VALUES ('min_version_code', '1', NULL)
ON CONFLICT (key) DO UPDATE SET
  value = EXCLUDED.value,
  value_enum = EXCLUDED.value_enum,
  updated_at = now();

INSERT INTO staging.app_config (key, value, value_enum)
VALUES ('update_type', 'FLEXIBLE', 'FLEXIBLE'::public.app_update_type)
ON CONFLICT (key) DO UPDATE SET
  value = EXCLUDED.value,
  value_enum = EXCLUDED.value_enum,
  updated_at = now();

INSERT INTO staging.app_config (key, value, value_enum)
VALUES ('nearby_colegios_limit', '200', NULL)
ON CONFLICT (key) DO UPDATE SET
  value = EXCLUDED.value,
  value_enum = EXCLUDED.value_enum,
  updated_at = now();
