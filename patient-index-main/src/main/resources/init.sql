CREATE TABLE patient (
    id varchar(64) PRIMARY KEY,
    last_updated timestamptz NOT NULL,
    identifiers jsonb NOT NULL,
    merged_into varchar(64) REFERENCES patient(id)
);

CREATE FUNCTION patient_set_last_updated() RETURNS trigger AS $patient_set_last_updated$
    BEGIN
        NEW.last_updated := CURRENT_TIMESTAMP;
        RETURN NEW;
    END;
$patient_set_last_updated$ LANGUAGE plpgsql;

CREATE TRIGGER patient_set_last_updated BEFORE INSERT OR UPDATE ON patient
    FOR EACH ROW EXECUTE FUNCTION patient_set_last_updated();
