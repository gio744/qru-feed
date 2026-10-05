CREATE TABLE IF NOT EXISTS release_impact_reviews (
 release_id uuid PRIMARY KEY REFERENCES legal_releases(id),
 reviewed_by uuid REFERENCES users(id),
 passed boolean NOT NULL,
 details jsonb NOT NULL DEFAULT '{}'::jsonb,
 reviewed_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS release_regression_results (
 id bigserial PRIMARY KEY,
 release_id uuid NOT NULL REFERENCES legal_releases(id),
 test_id text NOT NULL,
 critical boolean NOT NULL DEFAULT true,
 passed boolean NOT NULL,
 details jsonb NOT NULL DEFAULT '{}'::jsonb,
 checked_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE(release_id,test_id)
);
