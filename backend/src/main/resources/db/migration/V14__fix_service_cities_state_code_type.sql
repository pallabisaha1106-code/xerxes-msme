-- V13 declared state_code as CHARACTER(2) (bpchar), but ServiceCity.stateCode
-- is a plain Hibernate-mapped String with no columnDefinition override, so
-- schema validation expects VARCHAR. Fixing forward with a new migration
-- rather than editing V13, since V13 has already run against local dev
-- databases and Flyway migrations aren't meant to be edited once applied.
ALTER TABLE service_cities ALTER COLUMN state_code TYPE VARCHAR(2);
