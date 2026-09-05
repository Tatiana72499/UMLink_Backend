CREATE TABLE diagram_activity_events (
    id UUID PRIMARY KEY,
    diagram_id UUID NOT NULL REFERENCES diagrams(id) ON DELETE CASCADE,
    actor_id UUID NOT NULL REFERENCES app_users(id),
    actor_name VARCHAR(120) NOT NULL,
    action VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_diagram_activity_events_diagram_created_at
    ON diagram_activity_events (diagram_id, created_at DESC);
