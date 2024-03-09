/*
 * This file is generated by jOOQ.
 */
package de.tobchen.health.patientindex.feed.jooq.public_;


import de.tobchen.health.patientindex.feed.jooq.DefaultCatalog;
import de.tobchen.health.patientindex.feed.jooq.public_.tables.IdValue;
import de.tobchen.health.patientindex.feed.jooq.public_.tables.Message;

import java.util.Arrays;
import java.util.List;

import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class Public extends SchemaImpl {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public</code>
     */
    public static final Public PUBLIC = new Public();

    /**
     * The table <code>public.id_value</code>.
     */
    public final IdValue ID_VALUE = IdValue.ID_VALUE;

    /**
     * The table <code>public.message</code>.
     */
    public final Message MESSAGE = Message.MESSAGE;

    /**
     * No further instances allowed
     */
    private Public() {
        super("public", null);
    }


    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        return Arrays.asList(
            IdValue.ID_VALUE,
            Message.MESSAGE
        );
    }
}
