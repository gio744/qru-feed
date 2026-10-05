INSERT INTO roles(id,description) VALUES
 ('ADMIN','Administrator'),('EDITOR','Legal content editor'),('REVIEWER','Legal reviewer'),('VIEWER','Read-only user')
ON CONFLICT DO NOTHING;

INSERT INTO jurisdictions(id,parent_id,name,level) VALUES
 ('BR',NULL,'Brasil','NATIONAL')
ON CONFLICT DO NOTHING;

-- Technical bootstrap release only; not substantive legal content.
INSERT INTO legal_releases(version,status,fingerprint,effective_from)
VALUES ('bootstrap-0','ACTIVE','BOOTSTRAP-NO-LEGAL-CONTENT',now())
ON CONFLICT(version) DO NOTHING;
