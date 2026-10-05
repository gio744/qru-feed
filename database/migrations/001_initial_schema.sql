-- QRU Trânsito • Production Schema v1
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 external_subject text UNIQUE NOT NULL,
 display_name text NOT NULL,
 active boolean NOT NULL DEFAULT true,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE roles (id text PRIMARY KEY, description text NOT NULL);
CREATE TABLE user_roles (
 user_id uuid REFERENCES users(id), role_id text REFERENCES roles(id),
 PRIMARY KEY(user_id,role_id)
);

CREATE TABLE jurisdictions (
 id text PRIMARY KEY, parent_id text REFERENCES jurisdictions(id),
 name text NOT NULL, level text NOT NULL CHECK(level IN ('NATIONAL','STATE','MUNICIPAL'))
);
CREATE TABLE legal_norms (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), norm_type text NOT NULL,
 number text NOT NULL, year integer, title text NOT NULL, official_source_url text,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE legal_effect_versions (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), norm_id uuid NOT NULL REFERENCES legal_norms(id),
 version_no integer NOT NULL, valid_from timestamptz NOT NULL, valid_to timestamptz,
 status text NOT NULL, content_fingerprint text NOT NULL,
 UNIQUE(norm_id,version_no)
);
CREATE TABLE fichas (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), stable_key text UNIQUE NOT NULL,
 title text NOT NULL, jurisdiction_id text NOT NULL REFERENCES jurisdictions(id),
 status text NOT NULL DEFAULT 'DRAFT'
);
CREATE TABLE operational_bindings (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 ficha_id uuid NOT NULL REFERENCES fichas(id),
 effect_version_id uuid NOT NULL REFERENCES legal_effect_versions(id),
 provision_reference text NOT NULL, audited_excerpt text
);
CREATE TABLE legal_releases (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), version text UNIQUE NOT NULL,
 status text NOT NULL, fingerprint text NOT NULL, effective_from timestamptz,
 created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE cases (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), created_by uuid REFERENCES users(id),
 jurisdiction_id text REFERENCES jurisdictions(id), head_revision_id uuid,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE case_revisions (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), case_id uuid NOT NULL REFERENCES cases(id),
 actor_id uuid REFERENCES users(id), origin text NOT NULL, reason text,
 changeset jsonb NOT NULL, content_fingerprint text NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE case_revision_parents (
 revision_id uuid REFERENCES case_revisions(id), parent_revision_id uuid REFERENCES case_revisions(id),
 PRIMARY KEY(revision_id,parent_revision_id)
);
ALTER TABLE cases ADD CONSTRAINT cases_head_fk FOREIGN KEY(head_revision_id) REFERENCES case_revisions(id);
CREATE TABLE case_facts (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), revision_id uuid NOT NULL REFERENCES case_revisions(id),
 fact_key text NOT NULL, fact_value jsonb NOT NULL, confirmation_state text NOT NULL,
 provenance jsonb NOT NULL DEFAULT '{}'::jsonb
);
CREATE TABLE case_conflicts (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), case_id uuid NOT NULL REFERENCES cases(id),
 field_key text NOT NULL, local_revision_id uuid REFERENCES case_revisions(id),
 server_revision_id uuid REFERENCES case_revisions(id), legal_relevance boolean NOT NULL DEFAULT true,
 status text NOT NULL DEFAULT 'OPEN', resolution jsonb
);

CREATE TABLE idempotency_records (
 idempotency_key text PRIMARY KEY, actor_id uuid REFERENCES users(id),
 request_fingerprint text NOT NULL, operation_id uuid NOT NULL,
 result jsonb, status text NOT NULL, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE temporary_grants (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), jti text UNIQUE NOT NULL,
 actor_id uuid REFERENCES users(id), artifact_id text NOT NULL, action text NOT NULL,
 purpose text NOT NULL, session_id text NOT NULL, expires_at timestamptz NOT NULL,
 revoked_at timestamptz
);
CREATE TABLE consumed_jti (
 jti text PRIMARY KEY REFERENCES temporary_grants(jti),
 consumed_at timestamptz NOT NULL DEFAULT now(), operation_id uuid
);
CREATE TABLE audit_events (
 sequence bigserial PRIMARY KEY, event_type text NOT NULL, actor_id uuid REFERENCES users(id),
 entity_type text NOT NULL, entity_id text NOT NULL, event_data jsonb NOT NULL,
 previous_hash text, event_hash text NOT NULL, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_effect_dates ON legal_effect_versions(valid_from,valid_to);
CREATE INDEX idx_case_revision_case ON case_revisions(case_id,created_at);
CREATE INDEX idx_audit_entity ON audit_events(entity_type,entity_id,sequence);
