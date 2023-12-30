# Patient Index

Very, very, *very* limited patient indexing.

## Configuration

Configuration is done in the *application.yaml* file.

### General

|Entry|Explanation|Default|
|---|---|---|
|`patient-index.production-mode`|Set `true` for production use|`null`|

### IHE

**Note:** OIDs are not persisted in the database. Changing them may have retroactive effects.

|Entry|Explanation|Default|
|---|---|---|
|`patient-index.ihe.pid-oid`|OID for internal patient ID|`null`|
|`patient-index.ihe.app-oid`|OID for sending application|`null`|
|`patient-index.ihe.facility-oid`|OID for sending facility|`null`|

#### Patient Identity Feed

|Entry|Explanation|Default|
|---|---|---|
|`patient-index.ihe.patient-identity-feed.receiver.app-oid`|OID for receiving application|`null`|
|`patient-index.ihe.patient-identity-feed.receiver.facility-oid`|OID for receiving facility|`null`|

## Interfaces

### FHIR R5

FHIR R5 is the main way of interfacing with Patient Index, with */fhir/r5* being the base url. See */fhir/r5/metadata* for the conformance statement.

#### Patient Resources

In addition to the basic `create` interaction, Patient Index allows Update as Create for Patient resources.

Only patient identifiers with system data are stored in Patient Index. Sending bare identifiers, names, or any other data has no effect and won't be recovered in `read` or `search` interactions.

Search by identifier is only possible with both system and value.

### IHE Transactions

Patient Index partially implements some IHE actors. Transactions are implemented as seemed fit.

#### ITI-8: Patient Identity Feed

Since Patient Index stores no demographic data, only internal patient identifiers are actually sent. As these identifiers don't change, only `A01` and `A40` messages are sent.

## Development

### (F)AQ

**Why Spring Boot 2.7?**

As of 2023, HAPI FHIR is depending on `javax.servlet`, so until that's upgraded, this project's dependencies won't be upgraded.
