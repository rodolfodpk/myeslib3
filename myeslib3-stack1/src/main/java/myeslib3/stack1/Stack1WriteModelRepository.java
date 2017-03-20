package myeslib3.stack1;

import com.google.gson.Gson;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;
import myeslib3.stack.WriteModelRepository;
import myeslib3.stack1.infra.jdbi.DbConcurrencyException;
import myeslib3.stack1.infra.jdbi.LocalDateTimeMapper;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.util.LongColumnMapper;
import org.skife.jdbi.v2.util.StringColumnMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class Stack1WriteModelRepository implements WriteModelRepository {

	static final Logger logger = LoggerFactory.getLogger(Stack1WriteModelRepository.class);

	private final DbMetadata dbMetadata;
	private final Gson gson;
	private final DBI dbi;

	private final String selectAggRootSql;
	private final String insertAggRootSql;
	private final String updateAggRootSql;
	private final String insertUowSql;

	public Stack1WriteModelRepository(String aggregateRootId, Gson gson, DBI dbi) {

		requireNonNull(aggregateRootId);
		requireNonNull(gson);
		requireNonNull(dbi);

		this.dbMetadata = new DbMetadata(aggregateRootId);
		this.gson = gson;
		this.dbi = dbi;
		this.dbi.registerColumnMapper(new LocalDateTimeMapper());

		this.selectAggRootSql =
						String.format("select version from %s where id = :id", dbMetadata.aggregateRootTable);

		this.insertAggRootSql =
						String.format("insert into %s (id, version, last_update) " +
										"values (:id, :new_version, :last_update) ", dbMetadata.aggregateRootTable);

		this.updateAggRootSql =
						String.format("update %s set version_number = :new_version, last_update = :last_update " +
														"where id = :id and version_number = :curr_version",
										dbMetadata.aggregateRootTable);

		this.insertUowSql =
						String.format("insert into %s (uow_id, uow_data, target_id, version, inserted_on) " +
														"values (:uow_id, :uow_data, :target_id, :version, :inserted_on)",
										dbMetadata.unitOfWorkTable);

		logger.debug(updateAggRootSql);
		logger.debug(insertUowSql);

	}

	@Override
	public List<UnitOfWork> getAll(String id) {
		return getAllAfterVersion(id, new Version(0L));
	}

	@Override
	public List<UnitOfWork> getAllAfterVersion(String id, Version version) {

		requireNonNull(id);
		requireNonNull(version);

		final List<UnitOfWork> arh = new ArrayList<>();

		logger.debug("will load {} from {}", id, dbMetadata.aggregateRootTable);

		final List<String> unitsOfWork = dbi
						.withHandle(new HandleCallback<List<String>>() {

													String sql = String.format("select uow_data " +
																	"from %s where target_id = :id " +
																	" and version > :version " +
																	"order by version", dbMetadata.unitOfWorkTable);

													public List<String> withHandle(Handle h) {
														return h.createQuery(sql)
																		.bind("id", id.toString())
																		.bind("version", version.getVersion())
																		.map(StringColumnMapper.INSTANCE).list();
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

		for (String uowAsJson : unitsOfWork) {
			logger.debug("converting to uow from {}", uowAsJson);
			final UnitOfWork uow = gson.fromJson(uowAsJson, UnitOfWork.class);
			logger.debug(uow.toString());
			arh.add(uow);
		}

		return Collections.unmodifiableList(arh);
	}

	@Override
	public void append(UnitOfWork unitOfWork) throws DbConcurrencyException {

		requireNonNull(unitOfWork);
		requireNonNull(unitOfWork.getAggregateRootId());
		requireNonNull(unitOfWork.getCommand());
		requireNonNull(unitOfWork.getCommandId());

		final String uowAsJson = gson.toJson(unitOfWork, UnitOfWork.class);

		logger.debug("appending uow to {} with id {}", dbMetadata.aggregateRootTable, unitOfWork.getAggregateRootId());

		dbi.inTransaction(TransactionIsolationLevel.SERIALIZABLE, (conn, status) -> {

							Long currentVersion = conn.createQuery(selectAggRootSql)
											.bind("id", unitOfWork.getAggregateRootId())
											.map(LongColumnMapper.WRAPPER).first() ;

							if ((currentVersion == null ? 0 : currentVersion) != unitOfWork.getVersion().getVersion() -1) {
								throw new DbConcurrencyException(
												String.format("id = [%s], current_version = %d, new_version = %d",
																unitOfWork.getAggregateRootId(),
																currentVersion, unitOfWork.getVersion().getVersion()));
							}

							int result1 ;

							if (currentVersion == null ) {

								result1 = conn.createStatement(insertAggRootSql)
												.bind("id", unitOfWork.getAggregateRootId())
												.bind("new_version", unitOfWork.getVersion().getVersion())
												.bind("last_update", new Timestamp(Instant.now().getEpochSecond()))
												.execute();

							} else {

								result1 = conn.createStatement(updateAggRootSql)
												.bind("id", unitOfWork.getAggregateRootId())
												.bind("new_version", unitOfWork.getVersion().getVersion())
												.bind("curr_version", unitOfWork.getVersion().getVersion() - 1)
												.bind("last_update", new Timestamp(Instant.now().getEpochSecond()))
												.execute() ;
							}

							int result2 = conn.createStatement(insertUowSql)
											.bind("uow_id", unitOfWork.getUnitOfWorkId().toString())
											.bind("uow_data", uowAsJson)
											.bind("target_id", unitOfWork.getAggregateRootId())
											.bind("version", unitOfWork.getVersion().getVersion())
											.bind("inserted_on", new Timestamp(Instant.now().getEpochSecond()))
											.execute();

							conn.createStatement(
											String.format(
															"SELECT pg_notify('testchannel', '%s'); ", unitOfWork.getUnitOfWorkId().toString()))
															.execute();

							if (result1 + result2 == 2) {
								return true;
							}

							throw new DbConcurrencyException(
											String.format("id = [%s], current_version = %d, new_version = %d",
															unitOfWork.getAggregateRootId(),
															currentVersion, unitOfWork.getVersion().getVersion()));

						}

		);

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
