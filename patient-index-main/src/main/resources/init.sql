CREATE TABLE patient (
    id varchar(64) PRIMARY KEY,
    version_id int NOT NULL,
    last_updated timestamptz NOT NULL,
    identifiers jsonb NOT NULL,
    merged_into varchar(64) REFERENCES patient(id)
);
