UPDATE uml_relations
SET source_cardinality = '1..*',
    target_cardinality = '1..*'
WHERE association_class_id IS NOT NULL
  AND (source_cardinality IS DISTINCT FROM '1..*'
       OR target_cardinality IS DISTINCT FROM '1..*');
