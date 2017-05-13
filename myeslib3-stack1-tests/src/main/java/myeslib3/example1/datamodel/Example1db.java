/*
 * This file is generated by jOOQ.
*/
package myeslib3.example1.datamodel;


import myeslib3.example1.datamodel.tables.AggregateRoots;
import myeslib3.example1.datamodel.tables.Idempotency;
import myeslib3.example1.datamodel.tables.SchemaVersion;
import myeslib3.example1.datamodel.tables.UnitsOfWork;
import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;

import javax.annotation.Generated;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.9.2"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Example1db extends SchemaImpl {

    private static final long serialVersionUID = 1162443304;

    /**
     * The reference instance of <code>example1db</code>
     */
    public static final Example1db EXAMPLE1DB = new Example1db();

    /**
     * The table <code>example1db.aggregate_roots</code>.
     */
    public final AggregateRoots AGGREGATE_ROOTS = myeslib3.example1.datamodel.tables.AggregateRoots.AGGREGATE_ROOTS;

    /**
     * The table <code>example1db.idempotency</code>.
     */
    public final Idempotency IDEMPOTENCY = myeslib3.example1.datamodel.tables.Idempotency.IDEMPOTENCY;

    /**
     * The table <code>example1db.schema_version</code>.
     */
    public final SchemaVersion SCHEMA_VERSION = myeslib3.example1.datamodel.tables.SchemaVersion.SCHEMA_VERSION;

    /**
     * The table <code>example1db.units_of_work</code>.
     */
    public final UnitsOfWork UNITS_OF_WORK = myeslib3.example1.datamodel.tables.UnitsOfWork.UNITS_OF_WORK;

    /**
     * No further instances allowed
     */
    private Example1db() {
        super("example1db", null);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        List result = new ArrayList();
        result.addAll(getTables0());
        return result;
    }

    private final List<Table<?>> getTables0() {
        return Arrays.<Table<?>>asList(
            AggregateRoots.AGGREGATE_ROOTS,
            Idempotency.IDEMPOTENCY,
            SchemaVersion.SCHEMA_VERSION,
            UnitsOfWork.UNITS_OF_WORK);
    }
}
