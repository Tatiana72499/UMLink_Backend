CREATE TABLE projects (
    id UUID PRIMARY KEY, name VARCHAR(120) NOT NULL, description VARCHAR(500),
    owner_name VARCHAR(120) NOT NULL, created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE diagrams (
    id UUID PRIMARY KEY, project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL, version BIGINT, created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE uml_classes (
    id UUID PRIMARY KEY, diagram_id UUID NOT NULL REFERENCES diagrams(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL, position_x DOUBLE PRECISION NOT NULL, position_y DOUBLE PRECISION NOT NULL, version BIGINT
);
CREATE TABLE uml_attributes (
    id UUID PRIMARY KEY, uml_class_id UUID NOT NULL REFERENCES uml_classes(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL, data_type VARCHAR(80) NOT NULL, visibility VARCHAR(20) NOT NULL
);
CREATE TABLE uml_relations (
    id UUID PRIMARY KEY, diagram_id UUID NOT NULL REFERENCES diagrams(id) ON DELETE CASCADE,
    source_class_id UUID NOT NULL REFERENCES uml_classes(id) ON DELETE CASCADE,
    target_class_id UUID NOT NULL REFERENCES uml_classes(id) ON DELETE CASCADE, type VARCHAR(30) NOT NULL
);
CREATE INDEX idx_diagrams_project_id ON diagrams(project_id);
CREATE INDEX idx_uml_classes_diagram_id ON uml_classes(diagram_id);
CREATE INDEX idx_uml_relations_diagram_id ON uml_relations(diagram_id);
