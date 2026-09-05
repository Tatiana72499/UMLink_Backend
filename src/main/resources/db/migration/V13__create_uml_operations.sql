CREATE TABLE uml_operations (
    id UUID PRIMARY KEY,
    uml_class_id UUID NOT NULL REFERENCES uml_classes(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    return_type VARCHAR(80) NOT NULL,
    visibility VARCHAR(20) NOT NULL
);

CREATE TABLE uml_operation_parameters (
    id UUID PRIMARY KEY,
    uml_operation_id UUID NOT NULL REFERENCES uml_operations(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    data_type VARCHAR(80) NOT NULL,
    parameter_order INTEGER NOT NULL,
    CONSTRAINT uk_uml_operation_parameter_order UNIQUE (uml_operation_id, parameter_order)
);

CREATE INDEX idx_uml_operations_class_id ON uml_operations(uml_class_id);
CREATE INDEX idx_uml_operation_parameters_operation_id ON uml_operation_parameters(uml_operation_id);
