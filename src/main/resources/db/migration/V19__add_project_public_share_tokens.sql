ALTER TABLE projects ADD COLUMN public_share_token UUID;

UPDATE projects
SET public_share_token = gen_random_uuid()
WHERE public_share_token IS NULL;

ALTER TABLE projects ALTER COLUMN public_share_token SET NOT NULL;

CREATE UNIQUE INDEX uq_projects_public_share_token ON projects(public_share_token);
