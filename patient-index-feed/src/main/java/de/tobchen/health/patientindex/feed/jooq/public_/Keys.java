/*
 * This file is generated by jOOQ.
 */
package de.tobchen.health.patientindex.feed.jooq.public_;


import de.tobchen.health.patientindex.feed.jooq.public_.tables.IdValue;
import de.tobchen.health.patientindex.feed.jooq.public_.tables.Message;
import de.tobchen.health.patientindex.feed.jooq.public_.tables.records.IdValueRecord;
import de.tobchen.health.patientindex.feed.jooq.public_.tables.records.MessageRecord;

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

    public static final UniqueKey<IdValueRecord> ID_VALUE_PKEY = Internal.createUniqueKey(IdValue.ID_VALUE, DSL.name("id_value_pkey"), new TableField[] { IdValue.ID_VALUE.ID }, true);
    public static final UniqueKey<MessageRecord> MESSAGE_PKEY = Internal.createUniqueKey(Message.MESSAGE, DSL.name("message_pkey"), new TableField[] { Message.MESSAGE.ID }, true);
}