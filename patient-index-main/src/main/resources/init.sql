CREATE TABLE patients (
    id varchar(64) PRIMARY KEY,
    last_updated timestamptz NOT NULL,
    identifiers jsonb NOT NULL,
    merged_into varchar(64) REFERENCES patients(id)
);


