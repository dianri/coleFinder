-- app_config: parámetros remotos extensibles (key / value)
-- Una fila por parámetro. Esquema según entorno: staging (PRE) | public (PROD)

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_type t
    JOIN pg_namespace n ON n.oid = t.typnamespace
    WHERE n.nspname = 'public' AND t.typname = 'app_update_type'
  ) THEN
    CREATE TYPE public.app_update_type AS ENUM ('FLEXIBLE', 'IMMEDIATE');
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS staging.app_config (
  id serial PRIMARY KEY,
  key text UNIQUE NOT NULL,
  value text NOT NULL,
  value_enum public.app_update_type,
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT staging_app_config_update_type_check
    CHECK (key <> 'update_type' OR value IN ('FLEXIBLE', 'IMMEDIATE')),
  CONSTRAINT staging_app_config_nearby_limit_check
    CHECK (key <> 'nearby_colegios_limit' OR value ~ '^[0-9]+$'),
  CONSTRAINT staging_app_config_min_version_check
    CHECK (key <> 'min_version_code' OR value ~ '^[0-9]+$'),
  CONSTRAINT staging_app_config_value_enum_usage CHECK (
    (key = 'update_type' AND value_enum IS NOT NULL)
    OR (key <> 'update_type' AND value_enum IS NULL)
  )
);

CREATE TABLE IF NOT EXISTS public.app_config (
  id serial PRIMARY KEY,
  key text UNIQUE NOT NULL,
  value text NOT NULL,
  value_enum public.app_update_type,
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT public_app_config_update_type_check
    CHECK (key <> 'update_type' OR value IN ('FLEXIBLE', 'IMMEDIATE')),
  CONSTRAINT public_app_config_nearby_limit_check
    CHECK (key <> 'nearby_colegios_limit' OR value ~ '^[0-9]+$'),
  CONSTRAINT public_app_config_min_version_check
    CHECK (key <> 'min_version_code' OR value ~ '^[0-9]+$'),
  CONSTRAINT public_app_config_value_enum_usage CHECK (
    (key = 'update_type' AND value_enum IS NOT NULL)
    OR (key <> 'update_type' AND value_enum IS NULL)
  )
);

ALTER TABLE staging.app_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.app_config ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS staging_app_config_select_anon ON staging.app_config;
CREATE POLICY staging_app_config_select_anon ON staging.app_config
  FOR SELECT TO anon USING (true);

DROP POLICY IF EXISTS public_app_config_select_anon ON public.app_config;
CREATE POLICY public_app_config_select_anon ON public.app_config
  FOR SELECT TO anon USING (true);

GRANT SELECT ON TABLE staging.app_config TO anon;
GRANT SELECT ON TABLE public.app_config TO anon;

INSERT INTO staging.app_config (key, value, value_enum) VALUES
  ('min_version_code', '1', NULL),
  ('update_type', 'FLEXIBLE', 'FLEXIBLE'),
  ('nearby_colegios_limit', '200', NULL)
ON CONFLICT (key) DO NOTHING;

INSERT INTO public.app_config (key, value, value_enum) VALUES
  ('min_version_code', '1', NULL),
  ('update_type', 'FLEXIBLE', 'FLEXIBLE'),
  ('nearby_colegios_limit', '200', NULL)
ON CONFLICT (key) DO NOTHING;

-- Trigger: al cambiar value_enum en update_type, actualiza value (la app lee value)
CREATE OR REPLACE FUNCTION public.app_config_sync_value_enum()
RETURNS trigger
LANGUAGE plpgsql
SET search_path TO public
AS $$
BEGIN
  IF NEW.key = 'update_type' THEN
    IF NEW.value_enum IS NOT NULL THEN
      NEW.value := NEW.value_enum::text;
    ELSIF NEW.value IN ('FLEXIBLE', 'IMMEDIATE') THEN
      NEW.value_enum := NEW.value::public.app_update_type;
    END IF;
  ELSE
    NEW.value_enum := NULL;
  END IF;
  NEW.updated_at := now();
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_staging_app_config_sync_value_enum ON staging.app_config;
CREATE TRIGGER trg_staging_app_config_sync_value_enum
  BEFORE INSERT OR UPDATE ON staging.app_config
  FOR EACH ROW EXECUTE FUNCTION public.app_config_sync_value_enum();

DROP TRIGGER IF EXISTS trg_public_app_config_sync_value_enum ON public.app_config;
CREATE TRIGGER trg_public_app_config_sync_value_enum
  BEFORE INSERT OR UPDATE ON public.app_config
  FOR EACH ROW EXECUTE FUNCTION public.app_config_sync_value_enum();

-- RLS + GRANT SELECT para anon (igual que colegios)

-- Edición en Supabase:
--   update_type → usar columna value_enum (dropdown), no value a mano
--   resto de claves → solo value (texto/número)
