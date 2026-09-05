CREATE TABLE project_members (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_project_member UNIQUE (project_id, user_id),
    CONSTRAINT chk_project_member_role CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER'))
);

CREATE INDEX idx_project_members_user_id ON project_members(user_id);

INSERT INTO project_members (id, project_id, user_id, role, created_at)
SELECT gen_random_uuid(), id, owner_id, 'OWNER', created_at
FROM projects
WHERE owner_id IS NOT NULL
ON CONFLICT (project_id, user_id) DO NOTHING;
