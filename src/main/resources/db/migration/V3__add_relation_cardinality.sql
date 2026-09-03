ALTER TABLE uml_relations
    ADD COLUMN source_cardinality VARCHAR(20),
    ADD COLUMN target_cardinality VARCHAR(20);

UPDATE uml_relations
SET type = 'GENERALIZATION'
WHERE type = 'INHERITANCE';
