/*
 * This file is generated by jOOQ.
 */
package de.tobchen.health.patientindex.main.jooq.public_;


import de.tobchen.health.patientindex.main.jooq.public_.tables.Patient;
import de.tobchen.health.patientindex.main.jooq.public_.tables.records.PatientRecord;

import org.jooq.ForeignKey;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;


/**
 * A class modelling foreign key relationships and constraints of tables in
 * public.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class Keys {

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<PatientRecord> PATIENT_PKEY = Internal.createUniqueKey(Patient.PATIENT, DSL.name("patient_pkey"), new TableField[] { Patient.PATIENT.ID }, true);

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------

    public static final ForeignKey<PatientRecord, PatientRecord> PATIENT__PATIENT_MERGED_INTO_FKEY = Internal.createForeignKey(Patient.PATIENT, DSL.name("patient_merged_into_fkey"), new TableField[] { Patient.PATIENT.MERGED_INTO }, Keys.PATIENT_PKEY, new TableField[] { Patient.PATIENT.ID }, true);
}
