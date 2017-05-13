/*
 * This file is generated by jOOQ.
*/
package datamodel.tables;


import datamodel.Example1db;
import datamodel.Keys;
import datamodel.tables.records.AggregateRootsRecord;
import org.jooq.*;
import org.jooq.impl.TableImpl;

import javax.annotation.Generated;
import java.sql.Timestamp;
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
public class AggregateRoots extends TableImpl<AggregateRootsRecord> {

    private static final long serialVersionUID = 852795366;

    /**
     * The reference instance of <code>example1db.aggregate_roots</code>
     */
    public static final AggregateRoots AGGREGATE_ROOTS = new AggregateRoots();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<AggregateRootsRecord> getRecordType() {
        return AggregateRootsRecord.class;
    }

    /**
     * The column <code>example1db.aggregate_roots.ar_name</code>.
     */
    public final TableField<AggregateRootsRecord, String> AR_NAME = createField("ar_name", org.jooq.impl.SQLDataType.VARCHAR.length(36).nullable(false), this, "");

    /**
     * The column <code>example1db.aggregate_roots.ar_id</code>.
     */
    public final TableField<AggregateRootsRecord, String> AR_ID = createField("ar_id", org.jooq.impl.SQLDataType.VARCHAR.length(36).nullable(false), this, "");

    /**
     * The column <code>example1db.aggregate_roots.version</code>.
     */
    public final TableField<AggregateRootsRecord, Long> VERSION = createField("version", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>example1db.aggregate_roots.last_updated_on</code>.
     */
    public final TableField<AggregateRootsRecord, Timestamp> LAST_UPDATED_ON = createField("last_updated_on", org.jooq.impl.SQLDataType.TIMESTAMP.nullable(false).defaultValue(org.jooq.impl.DSL.inline("CURRENT_TIMESTAMP", org.jooq.impl.SQLDataType.TIMESTAMP)), this, "");

    /**
     * Create a <code>example1db.aggregate_roots</code> table reference
     */
    public AggregateRoots() {
        this("aggregate_roots", null);
    }

    /**
     * Create an aliased <code>example1db.aggregate_roots</code> table reference
     */
    public AggregateRoots(String alias) {
        this(alias, AGGREGATE_ROOTS);
    }

    private AggregateRoots(String alias, Table<AggregateRootsRecord> aliased) {
        this(alias, aliased, null);
    }

    private AggregateRoots(String alias, Table<AggregateRootsRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return Example1db.EXAMPLE1DB;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<AggregateRootsRecord> getPrimaryKey() {
        return Keys.KEY_AGGREGATE_ROOTS_PRIMARY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<AggregateRootsRecord>> getKeys() {
        return Arrays.<UniqueKey<AggregateRootsRecord>>asList(Keys.KEY_AGGREGATE_ROOTS_PRIMARY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregateRoots as(String alias) {
        return new AggregateRoots(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public AggregateRoots rename(String name) {
        return new AggregateRoots(name, null);
    }
}
