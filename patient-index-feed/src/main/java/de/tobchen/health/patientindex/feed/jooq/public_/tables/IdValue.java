/*
 * This file is generated by jOOQ.
 */
package de.tobchen.health.patientindex.feed.jooq.public_.tables;


import de.tobchen.health.patientindex.feed.jooq.public_.Keys;
import de.tobchen.health.patientindex.feed.jooq.public_.Public;
import de.tobchen.health.patientindex.feed.jooq.public_.tables.records.IdValueRecord;

import java.time.OffsetDateTime;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function2;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row2;
import org.jooq.Schema;
import org.jooq.SelectField;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class IdValue extends TableImpl<IdValueRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public.id_value</code>
     */
    public static final IdValue ID_VALUE = new IdValue();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<IdValueRecord> getRecordType() {
        return IdValueRecord.class;
    }

    /**
     * The column <code>public.id_value.id</code>.
     */
    public final TableField<IdValueRecord, String> ID = createField(DSL.name("id"), SQLDataType.VARCHAR(64).nullable(false), this, "");

    /**
     * The column <code>public.id_value.value_ts</code>.
     */
    public final TableField<IdValueRecord, OffsetDateTime> VALUE_TS = createField(DSL.name("value_ts"), SQLDataType.TIMESTAMPWITHTIMEZONE(6), this, "");

    private IdValue(Name alias, Table<IdValueRecord> aliased) {
        this(alias, aliased, null);
    }

    private IdValue(Name alias, Table<IdValueRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>public.id_value</code> table reference
     */
    public IdValue(String alias) {
        this(DSL.name(alias), ID_VALUE);
    }

    /**
     * Create an aliased <code>public.id_value</code> table reference
     */
    public IdValue(Name alias) {
        this(alias, ID_VALUE);
    }

    /**
     * Create a <code>public.id_value</code> table reference
     */
    public IdValue() {
        this(DSL.name("id_value"), null);
    }

    public <O extends Record> IdValue(Table<O> child, ForeignKey<O, IdValueRecord> key) {
        super(child, key, ID_VALUE);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Public.PUBLIC;
    }

    @Override
    public UniqueKey<IdValueRecord> getPrimaryKey() {
        return Keys.ID_VALUE_PKEY;
    }

    @Override
    public IdValue as(String alias) {
        return new IdValue(DSL.name(alias), this);
    }

    @Override
    public IdValue as(Name alias) {
        return new IdValue(alias, this);
    }

    @Override
    public IdValue as(Table<?> alias) {
        return new IdValue(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public IdValue rename(String name) {
        return new IdValue(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public IdValue rename(Name name) {
        return new IdValue(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public IdValue rename(Table<?> name) {
        return new IdValue(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<String, OffsetDateTime> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function2<? super String, ? super OffsetDateTime, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function2<? super String, ? super OffsetDateTime, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}