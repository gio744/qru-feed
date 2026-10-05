ALTER TABLE legal_releases
  ADD COLUMN IF NOT EXISTS package_payload jsonb,
  ADD COLUMN IF NOT EXISTS package_signature text,
  ADD COLUMN IF NOT EXISTS activated_at timestamptz,
  ADD COLUMN IF NOT EXISTS superseded_at timestamptz;

CREATE TABLE IF NOT EXISTS release_gate_results (
 id bigserial PRIMARY KEY,
 release_id uuid NOT NULL REFERENCES legal_releases(id),
 gate_name text NOT NULL,
 passed boolean NOT NULL,
 details jsonb NOT NULL DEFAULT '{}'::jsonb,
 checked_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_one_active_release
ON legal_releases ((status))
WHERE status='ACTIVE';
