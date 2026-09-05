CREATE TABLE diagram_drawings (
    id UUID PRIMARY KEY,
    diagram_id UUID NOT NULL REFERENCES diagrams(id) ON DELETE CASCADE,
    svg_path TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_diagram_drawings_diagram_id ON diagram_drawings(diagram_id);
