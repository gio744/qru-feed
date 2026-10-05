CREATE TABLE IF NOT EXISTS release_dossiers (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 release_id uuid NOT NULL REFERENCES legal_releases(id),
 attempt_no integer NOT NULL,
 actor_subject text NOT NULL,
 status text NOT NULL,
 snapshot jsonb NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE(release_id,attempt_no)
);

ALTER TABLE audit_events
  ADD COLUMN IF NOT EXISTS dossier_id uuid REFERENCES release_dossiers(id);

CREATE INDEX IF NOT EXISTS idx_dossier_release ON release_dossiers(release_id,attempt_no);
