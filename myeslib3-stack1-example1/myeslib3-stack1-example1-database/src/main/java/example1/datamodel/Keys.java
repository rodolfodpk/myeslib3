/*
 * This file is generated by jOOQ.
*/
package example1.datamodel;


import example1.datamodel.tables.AggregateRoots;
import example1.datamodel.tables.CustomerSummary;
import example1.datamodel.tables.Idempotency;
import example1.datamodel.tables.SchemaVersion;
import example1.datamodel.tables.UnitsOfWork;
import example1.datamodel.tables.records.AggregateRootsRecord;
import example1.datamodel.tables.records.CustomerSummaryRecord;
import example1.datamodel.tables.records.IdempotencyRecord;
import example1.datamodel.tables.records.SchemaVersionRecord;
import example1.datamodel.tables.records.UnitsOfWorkRecord;

import javax.annotation.Generated;

import org.jooq.Identity;
import org.jooq.UniqueKey;
import org.jooq.impl.AbstractKeys;


/**
 * A class modelling foreign key relationships between tables of the <code>example1db</code> 
 * schema
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.9.2"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // IDENTITY definitions
    // -------------------------------------------------------------------------

    public static final Identity<UnitsOfWorkRecord, Long> IDENTITY_UNITS_OF_WORK = Identities0.IDENTITY_UNITS_OF_WORK;

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<AggregateRootsRecord> KEY_AGGREGATE_ROOTS_PRIMARY = UniqueKeys0.KEY_AGGREGATE_ROOTS_PRIMARY;
    public static final UniqueKey<CustomerSummaryRecord> KEY_CUSTOMER_SUMMARY_PRIMARY = UniqueKeys0.KEY_CUSTOMER_SUMMARY_PRIMARY;
    public static final UniqueKey<IdempotencyRecord> KEY_IDEMPOTENCY_PRIMARY = UniqueKeys0.KEY_IDEMPOTENCY_PRIMARY;
    public static final UniqueKey<SchemaVersionRecord> KEY_SCHEMA_VERSION_PRIMARY = UniqueKeys0.KEY_SCHEMA_VERSION_PRIMARY;
    public static final UniqueKey<UnitsOfWorkRecord> KEY_UNITS_OF_WORK_PRIMARY = UniqueKeys0.KEY_UNITS_OF_WORK_PRIMARY;
    public static final UniqueKey<UnitsOfWorkRecord> KEY_UNITS_OF_WORK_UOW_ID = UniqueKeys0.KEY_UNITS_OF_WORK_UOW_ID;
    public static final UniqueKey<UnitsOfWorkRecord> KEY_UNITS_OF_WORK_CMD_ID = UniqueKeys0.KEY_UNITS_OF_WORK_CMD_ID;

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------


    // -------------------------------------------------------------------------
    // [#1459] distribute members to avoid static initialisers > 64kb
    // -------------------------------------------------------------------------

    private static class Identities0 extends AbstractKeys {
        public static Identity<UnitsOfWorkRecord, Long> IDENTITY_UNITS_OF_WORK = createIdentity(UnitsOfWork.UNITS_OF_WORK, UnitsOfWork.UNITS_OF_WORK.UOW_SEQ_NUMBER);
    }

    private static class UniqueKeys0 extends AbstractKeys {
        public static final UniqueKey<AggregateRootsRecord> KEY_AGGREGATE_ROOTS_PRIMARY = createUniqueKey(AggregateRoots.AGGREGATE_ROOTS, "KEY_aggregate_roots_PRIMARY", AggregateRoots.AGGREGATE_ROOTS.AR_NAME, AggregateRoots.AGGREGATE_ROOTS.AR_ID);
        public static final UniqueKey<CustomerSummaryRecord> KEY_CUSTOMER_SUMMARY_PRIMARY = createUniqueKey(CustomerSummary.CUSTOMER_SUMMARY, "KEY_customer_summary_PRIMARY", CustomerSummary.CUSTOMER_SUMMARY.ID);
        public static final UniqueKey<IdempotencyRecord> KEY_IDEMPOTENCY_PRIMARY = createUniqueKey(Idempotency.IDEMPOTENCY, "KEY_idempotency_PRIMARY", Idempotency.IDEMPOTENCY.SLOT_NAME, Idempotency.IDEMPOTENCY.SLOT_ID);
        public static final UniqueKey<SchemaVersionRecord> KEY_SCHEMA_VERSION_PRIMARY = createUniqueKey(SchemaVersion.SCHEMA_VERSION, "KEY_schema_version_PRIMARY", SchemaVersion.SCHEMA_VERSION.INSTALLED_RANK);
        public static final UniqueKey<UnitsOfWorkRecord> KEY_UNITS_OF_WORK_PRIMARY = createUniqueKey(UnitsOfWork.UNITS_OF_WORK, "KEY_units_of_work_PRIMARY", UnitsOfWork.UNITS_OF_WORK.UOW_SEQ_NUMBER, UnitsOfWork.UNITS_OF_WORK.AR_NAME);
        public static final UniqueKey<UnitsOfWorkRecord> KEY_UNITS_OF_WORK_UOW_ID = createUniqueKey(UnitsOfWork.UNITS_OF_WORK, "KEY_units_of_work_uow_id", UnitsOfWork.UNITS_OF_WORK.UOW_ID, UnitsOfWork.UNITS_OF_WORK.AR_NAME);
        public static final UniqueKey<UnitsOfWorkRecord> KEY_UNITS_OF_WORK_CMD_ID = createUniqueKey(UnitsOfWork.UNITS_OF_WORK, "KEY_units_of_work_cmd_id", UnitsOfWork.UNITS_OF_WORK.CMD_ID, UnitsOfWork.UNITS_OF_WORK.AR_NAME);
    }
}
