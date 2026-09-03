CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
ALTER TABLE projects ADD COLUMN owner_id UUID REFERENCES app_users(id);
CREATE INDEX idx_projects_owner_id ON projects(owner_id);
