ALTER TABLE projects ADD COLUMN version BIGINT;
UPDATE projects SET version = 0 WHERE version IS NULL;
ALTER TABLE projects ALTER COLUMN version SET NOT NULL;

UPDATE diagrams SET version = 0 WHERE version IS NULL;
ALTER TABLE diagrams ALTER COLUMN version SET NOT NULL;
