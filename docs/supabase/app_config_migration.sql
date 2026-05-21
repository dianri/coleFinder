-- app_config: parámetros remotos extensibles (key / value)
-- Una fila por parámetro. Esquema según entorno: staging (PRE) | public (PROD)

CREATE TABLE IF NOT EXISTS staging.app_config (
  id serial PRIMARY KEY,
  key text UNIQUE NOT NULL,
  value text NOT NULL,
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT staging_app_config_update_type_check
    CHECK (key <> 'update_type' OR value IN ('FLEXIBLE', 'IMMEDIATE')),
  CONSTRAINT staging_app_config_nearby_limit_check
    CHECK (key <> 'nearby_colegios_limit' OR value ~ '^[0-9]+$'),
  CONSTRAINT staging_app_config_min_version_check
    CHECK (key <> 'min_version_code' OR value ~ '^[0-9]+$')
);

CREATE TABLE IF NOT EXISTS public.app_config (
  id serial PRIMARY KEY,
  key text UNIQUE NOT NULL,
  value text NOT NULL,
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT public_app_config_update_type_check
    CHECK (key <> 'update_type' OR value IN ('FLEXIBLE', 'IMMEDIATE')),
  CONSTRAINT public_app_config_nearby_limit_check
    CHECK (key <> 'nearby_colegios_limit' OR value ~ '^[0-9]+$'),
  CONSTRAINT public_app_config_min_version_check
    CHECK (key <> 'min_version_code' OR value ~ '^[0-9]+$')
);

INSERT INTO staging.app_config (key, value) VALUES
  ('min_version_code', '1'),
  ('update_type', 'FLEXIBLE'),
  ('nearby_colegios_limit', '200')
ON CONFLICT (key) DO NOTHING;

INSERT INTO public.app_config (key, value) VALUES
  ('min_version_code', '1'),
  ('update_type', 'FLEXIBLE'),
  ('nearby_colegios_limit', '200')
ON CONFLICT (key) DO NOTHING;

-- RLS + GRANT SELECT para anon (igual que colegios)

-- nearby_colegios lee nearby_colegios_limit desde app_config del mismo esquema.
-- El parámetro p_limit de la RPC se mantiene por compatibilidad con apps antiguas pero se ignora.
