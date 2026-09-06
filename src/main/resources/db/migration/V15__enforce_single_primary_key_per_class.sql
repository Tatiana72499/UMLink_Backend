WITH ranked_primary_keys AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY uml_class_id ORDER BY id) AS position
    FROM uml_attributes
    WHERE is_primary_key = TRUE
)
UPDATE uml_attributes
SET is_primary_key = FALSE
WHERE id IN (
    SELECT id
    FROM ranked_primary_keys
    WHERE position > 1
);

CREATE UNIQUE INDEX ux_uml_attributes_single_primary_key
    ON uml_attributes (uml_class_id)
    WHERE is_primary_key = TRUE;
