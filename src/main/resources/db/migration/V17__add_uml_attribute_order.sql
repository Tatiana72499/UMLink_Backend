ALTER TABLE uml_attributes ADD COLUMN attribute_order INTEGER NOT NULL DEFAULT 0;

WITH ordered_attributes AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY uml_class_id ORDER BY id) - 1 AS row_order
    FROM uml_attributes
)
UPDATE uml_attributes attribute
SET attribute_order = ordered_attributes.row_order
FROM ordered_attributes
WHERE attribute.id = ordered_attributes.id;
