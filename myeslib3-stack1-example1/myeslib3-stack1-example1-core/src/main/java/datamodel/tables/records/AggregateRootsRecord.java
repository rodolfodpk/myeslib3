/*
 * This file is generated by jOOQ.
*/
package datamodel.tables.records;


import datamodel.tables.AggregateRoots;
import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Record4;
import org.jooq.Row4;
import org.jooq.impl.UpdatableRecordImpl;

import javax.annotation.Generated;
import java.sql.Timestamp;


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
public class AggregateRootsRecord extends UpdatableRecordImpl<AggregateRootsRecord> implements Record4<String, String, Long, Timestamp> {

    private static final long serialVersionUID = -424963112;

    /**
     * Setter for <code>example1db.aggregate_roots.ar_name</code>.
     */
    public AggregateRootsRecord setArName(String value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>example1db.aggregate_roots.ar_name</code>.
     */
    public String getArName() {
        return (String) get(0);
    }

    /**
     * Setter for <code>example1db.aggregate_roots.ar_id</code>.
     */
    public AggregateRootsRecord setArId(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>example1db.aggregate_roots.ar_id</code>.
     */
    public String getArId() {
        return (String) get(1);
    }

    /**
     * Setter for <code>example1db.aggregate_roots.version</code>.
     */
    public AggregateRootsRecord setVersion(Long value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>example1db.aggregate_roots.version</code>.
     */
    public Long getVersion() {
        return (Long) get(2);
    }

    /**
     * Setter for <code>example1db.aggregate_roots.last_updated_on</code>.
     */
    public AggregateRootsRecord setLastUpdatedOn(Timestamp value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>example1db.aggregate_roots.last_updated_on</code>.
     */
    public Timestamp getLastUpdatedOn() {
        return (Timestamp) get(3);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Record2<String, String> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record4 type implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Row4<String, String, Long, Timestamp> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Row4<String, String, Long, Timestamp> valuesRow() {
        return (Row4) super.valuesRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field1() {
        return AggregateRoots.AGGREGATE_ROOTS.AR_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field2() {
        return AggregateRoots.AGGREGATE_ROOTS.AR_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field3() {
        return AggregateRoots.AGGREGATE_ROOTS.VERSION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Timestamp> field4() {
        return AggregateRoots.AGGREGATE_ROOTS.LAST_UPDATED_ON;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value1() {
        return getArName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value2() {
        return getArId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value3() {
        return getVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timestamp value4() {
        return getLastUpdatedOn();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregateRootsRecord value1(String value) {
        setArName(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregateRootsRecord value2(String value) {
        setArId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregateRootsRecord value3(Long value) {
        setVersion(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregateRootsRecord value4(Timestamp value) {
        setLastUpdatedOn(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregateRootsRecord values(String value1, String value2, Long value3, Timestamp value4) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached AggregateRootsRecord
     */
    public AggregateRootsRecord() {
        super(AggregateRoots.AGGREGATE_ROOTS);
    }

    /**
     * Create a detached, initialised AggregateRootsRecord
     */
    public AggregateRootsRecord(String arName, String arId, Long version, Timestamp lastUpdatedOn) {
        super(AggregateRoots.AGGREGATE_ROOTS);

        set(0, arName);
        set(1, arId);
        set(2, version);
        set(3, lastUpdatedOn);
    }
}
