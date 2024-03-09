CREATE TYPE message_status AS ENUM ('queued', 'sent', 'failed');

CREATE TABLE message (
    id serial PRIMARY KEY,
    patient_id varchar(64) NOT NULL,
    patient_last_updated timestamptz NOT NULL,
    patient_merged_into_id varchar(64),
    recorded_at timestamptz NOT NULL,
    status message_status NOT NULL
);

CREATE TABLE id_value (
    id varchar(64) PRIMARY KEY,
    value_ts timestamptz
);
