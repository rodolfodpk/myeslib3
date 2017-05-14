package myeslib3.stack1.stack1infra.idempotency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

public class MyIdempotencyDao {

	Logger logger = LoggerFactory.getLogger(MyIdempotencyDao.class);

	private final String entityId;
	private final DataSource dataSource;

	public MyIdempotencyDao(String entityId, DataSource dataSource) {
		this.entityId = entityId;
		this.dataSource = dataSource;
	}

//    public boolean put(final String key) {
//      final AtomicInteger updateCount = new AtomicInteger(0);
//        try {
//            DSL.using(dataSource, SQLDialect.MYSQL)
//                    .transaction(ctx -> {
//                        Record1<String> record = DSL.using(ctx)
//                                .select(IDEMPOTENCY.ENTITY_KEY)
//                                .from(IDEMPOTENCY)
//                                .where(IDEMPOTENCY.ENTITY_ID.eq(entityId).and(IDEMPOTENCY.ENTITY_KEY.eq(key)))
//                                .fetchAny();
//                        if (record!=null) return;
//                        int _updateCount = DSL.using(ctx)
//                                .insertInto(IDEMPOTENCY)
//                                .columns(IDEMPOTENCY.ENTITY_ID, IDEMPOTENCY.ENTITY_KEY)
//                                .values(entityId, key)
//                                .execute();
//                        updateCount.set(_updateCount);
//                    });
//        } catch (Exception e) {
//            //e.printStackTrace();
//            return false;
//        }
//        //logger.info("* put key=[{}] success={}", key, success.getInstance());
//        return updateCount.getInstance()==1;
//    }
//
//    public String getInstance(final String key) {
//        AtomicReference<String> value = new AtomicReference<>(null);
//        DSL.using(dataSource, SQLDialect.MYSQL)
//                .transaction(ctx -> {
//                    Record1<String> record = DSL.using(ctx)
//                            .select(IDEMPOTENCY.ENTITY_KEY)
//                            .from(IDEMPOTENCY)
//                            .where(IDEMPOTENCY.ENTITY_ID.eq(entityId).and(IDEMPOTENCY.ENTITY_KEY.eq(key)))
//                            .fetchAny();
//                    if (record!= null) {
//                        value.set(record.getInstance(IDEMPOTENCY.ENTITY_KEY));
//                    }
//                });
//            //logger.info("getInstance key={} success={}", key, key.equals(value.getInstance()));
//            return value.getInstance();
//    }
//
//    public boolean delete(final String key) {
//        final AtomicInteger updateCount = new AtomicInteger();
//        DSL.using(dataSource, SQLDialect.MYSQL)
//                .transaction(ctx -> {
//                    int _updateCount = DSL.using(ctx)
//                            .deleteFrom(IDEMPOTENCY)
//                            .where(IDEMPOTENCY.ENTITY_ID.eq(entityId).and(IDEMPOTENCY.ENTITY_KEY.eq(key)))
//                            .execute();
//                    updateCount.set(_updateCount);
//                });
//        //logger.info("delete key={} success={}", key, updateCount.getInstance()==1);
//        return updateCount.getInstance()==1;
//    }
}
