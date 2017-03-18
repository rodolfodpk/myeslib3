package myeslib3.stack1;

import com.google.gson.Gson;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;
import myeslib3.stack.WriteModelDao;
import myeslib3.stack1.infra.jdbi.DbConcurrencyException;
import myeslib3.stack1.infra.jdbi.LocalDateTimeMapper;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class Stack1WriteModelDao implements WriteModelDao {

	static final Logger logger = LoggerFactory.getLogger(Stack1WriteModelDao.class);

	private final DbMetadata dbMetadata;
	private final Gson gson;
	private final DBI dbi;

	public Stack1WriteModelDao(String aggregateRootId, Gson gson, DBI dbi) {
		checkNotNull(aggregateRootId);
		checkNotNull(gson);
		checkNotNull(dbi);
		this.dbMetadata = new DbMetadata(aggregateRootId);
		this.gson = gson;
		this.dbi = dbi;
		this.dbi.registerColumnMapper(new LocalDateTimeMapper());
	}

	@Override
	public List<UnitOfWork> getAll(String id) {
		return getAllAfterVersion(id, new Version(0L));
	}

	@Override
	public List<UnitOfWork> getAllAfterVersion(String id, Version version) {

		final List<UnitOfWork> arh = new ArrayList<>();

		logger.debug("will load {} from {}", id.toString(), dbMetadata.aggregateRootTable);

		final List<UowRecord> unitsOfWork = dbi
						.withHandle(new HandleCallback<List<UowRecord>>() {

													String sql = String.format("select id, version, uow_data, seq_number " +
																	"from %s where id = :id " +
																	" and version > :version " +
																	"order by version", dbMetadata.unitOfWorkTable);

													public List<UowRecord> withHandle(Handle h) {
														return h.createQuery(sql)
																		.bind("id", id.toString())
																		.bind("version", version.getVersion())
																		.map(new UowRecordMapper()).list();
													}
												}
						);

		if (unitsOfWork == null) {

			logger.debug("found none unit of work for id {} and version > {} on {}",
							id.toString(), version.getVersion(), dbMetadata.unitOfWorkTable);

			return new ArrayList<>();

		}

		logger.debug("found {} units of work for id {} and version > {} on {}",
						unitsOfWork.size(), id.toString(), version.getVersion(), dbMetadata.unitOfWorkTable);

		for (UowRecord r : unitsOfWork) {
			logger.debug("converting to uow from {}", r.uowData);
			final UnitOfWork uow = gson.fromJson(r.uowData, UnitOfWork.class);
			logger.debug(uow.toString());
			arh.add(uow);
		}

		return Collections.unmodifiableList(arh);
	}

	@Override
	public void append(UnitOfWork unitOfWork) throws DbConcurrencyException {

		checkNotNull(unitOfWork);
		checkNotNull(unitOfWork.getAggregateRootId());
		checkNotNull(unitOfWork.getCommand());
		checkNotNull(unitOfWork.getCommandId());

		final String selectAggRootSql =
						String.format("select id, version from %s where id = :id", dbMetadata.aggregateRootTable);

		final String insertAggRootSql =
						String.format("insert into %s (id, version, last_update) " +
										"values (:id, :new_version, :last_update) ", dbMetadata.aggregateRootTable);

		final String updateAggRootSql =
						String.format("update %s set version_number = :new_version, last_update = :last_update " +
														"where id = :id and version_number = :curr_version",
										dbMetadata.aggregateRootTable);

		final String insertUowSql =
						String.format("insert into %s (id, uow_data, version, inserted_on) " +
														"values (:id, :uow_data, :version, :inserted_on)",
										dbMetadata.unitOfWorkTable);

		final String uowAsJson = gson.toJson(unitOfWork, UnitOfWork.class);

		logger.debug(updateAggRootSql);
		logger.debug(insertUowSql);

		logger.debug("appending uow to {} with id {}", dbMetadata.aggregateRootTable, unitOfWork.getAggregateRootId());

		dbi.inTransaction(TransactionIsolationLevel.READ_COMMITTED, (conn, status) -> {

			boolean alreadyExists = conn.createStatement(selectAggRootSql)
							.bind("id", unitOfWork.getAggregateRootId())
							.execute() == 1;

			if (!alreadyExists && unitOfWork.getVersion().getVersion() != 1L) {
				throw new DbConcurrencyException(
								String.format("first version must be =1. id = [%s] version = %d",
												unitOfWork.getAggregateRootId(), unitOfWork.getVersion().getVersion()));
			}

			int result1 = alreadyExists ?
							conn.createStatement(updateAggRootSql)
											.bind("id", unitOfWork.getAggregateRootId())
											.bind("new_version", unitOfWork.getVersion())
											.bind("curr_version", unitOfWork.getVersion().getVersion() - 1)
											.bind("last_update", LocalDateTime.now())
											.execute()
							: conn.createStatement(insertAggRootSql)
							.bind("id", unitOfWork.getAggregateRootId())
							.bind("version", unitOfWork.getVersion())
							.bind("last_update", LocalDateTime.now())
							.execute();

			if (result1 != 1) {
				throw new DbConcurrencyException(
								String.format("does not match the last version for id = %s and last version %d",
												unitOfWork.getAggregateRootId(), unitOfWork.getVersion().getVersion() - 1));
			}

			int result2 = conn.createStatement(insertUowSql)
							.bind("id", unitOfWork.getAggregateRootId())
							.bind("uow_data", uowAsJson)
							.bind("version", unitOfWork.getVersion())
							.bind("inserted_on", unitOfWork.getTimestamp())
							.execute();

			int result = result1 + result2;

			if (result != 2) {
				throw new DbConcurrencyException(
								String.format("does not match the last version for id = %s and last version %d",
												unitOfWork.getAggregateRootId(), unitOfWork.getVersion().getVersion() - 1));
			}

			// TODO notify topic events

			return result == 2;
		}

		);

	}

	static class UowRecord {

		final String id;
		final Long version;
		final String uowData;
		final Long seqNumber;

		public UowRecord(String id, Long version, String uowData, Long seqNumber) {
			this.id = id;
			this.version = version;
			this.uowData = uowData;
			this.seqNumber = seqNumber;
		}

	}

	static class UowRecordMapper implements ResultSetMapper<UowRecord> {
		@Override
		public UowRecord map(int index, ResultSet r, StatementContext ctx)
						throws SQLException {
			String id = r.getString("id");
			Long version = r.getBigDecimal("version").longValue();
			String uowData = r.getString("uow_data");
			BigDecimal bdSeqNumber = r.getBigDecimal("seq_number");
			Long seqNumber = bdSeqNumber == null ? null : bdSeqNumber.longValue();
			return new UowRecord(id, version, uowData, seqNumber);
		}
	}

	static class DbMetadata {

		private final String aggregateRootName;
		final String aggregateRootTable;
		final String unitOfWorkTable;
//    final String commandTable;

		public DbMetadata(String aggregateRootName) {
			this.aggregateRootName = aggregateRootName;
			this.aggregateRootTable = aggregateRootName.concat("_AR");
			this.unitOfWorkTable = aggregateRootName.concat("_UOW");
//        this.commandTable = aggregateRootName.concat("_CMD");
		}

	}

}
