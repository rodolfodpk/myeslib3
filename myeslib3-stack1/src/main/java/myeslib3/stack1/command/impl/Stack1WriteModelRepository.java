package myeslib3.stack1.command.impl;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.List;
import myeslib3.core.data.Command;
import myeslib3.core.data.Event;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;
import myeslib3.stack1.command.WriteModelRepository;
import myeslib3.stack1.stack1infra.jdbi.DbConcurrencyException;
import myeslib3.stack1.stack1infra.jdbi.LocalDateTimeMapper;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.util.LongColumnMapper;
import org.skife.jdbi.v2.util.StringColumnMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class Stack1WriteModelRepository implements WriteModelRepository {

  static final Logger logger = LoggerFactory.getLogger(Stack1WriteModelRepository.class);

  private final DbMetadata dbMetadata;
  private final Gson gson;
  private final DBI dbi;

  private final String eventsChannelId;
  private final String selectAggRootSql;
  private final String insertAggRootSql;
  private final String updateAggRootSql;
  private final String insertUowSql;

  private final TypeToken<java.util.List<Event>> listTypeToken = new TypeToken<java.util.List<Event>>() {};

  public Stack1WriteModelRepository(String eventsChannelId, String aggregateRootId, Gson gson, DBI dbi) {

    requireNonNull(aggregateRootId);
    requireNonNull(eventsChannelId);
    requireNonNull(gson);
    requireNonNull(dbi);

    this.eventsChannelId = eventsChannelId;
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
            String.format("insert into %s (uow_id, uow_data, cmd_data, target_id, version, inserted_on) " +
                            "values (:uow_id, :uow_data, :cmd_data, :target_id, :version, :inserted_on)",
                    dbMetadata.unitOfWorkTable);

    logger.debug(updateAggRootSql);
    logger.debug(insertUowSql);

  }


  @Override
  public Optional<UnitOfWork> get(UUID uowId) {

    final String uowAsJson = dbi
      .withHandle(new HandleCallback<String>() {
        final String sql = String.format("select uow_data " +
                "from %s where uow_id = :uow_id ", dbMetadata.unitOfWorkTable);

        public String withHandle(Handle h) {
          return h.createQuery(sql)
                  .bind("uow_id", uowId.toString())
                  .map(StringColumnMapper.INSTANCE).first();
        }
      }
      );

    return uowAsJson == null ? Optional.empty() : Optional.of(gson.fromJson(uowAsJson, UnitOfWork.class));

  }

  @Override
  public Tuple2<Version, List<Event>> getAll(String id) {
    return getAllAfterVersion(id, new Version(0L));
  }

  @Override
  public Tuple2<Version, List<Event>> getAllAfterVersion(String id, Version version) {

    requireNonNull(id);
    requireNonNull(version);

    logger.debug("will load {} from {}", id, dbMetadata.aggregateRootTable);

    final java.util.List<Tuple2<Long, String>> eventsListAsJson = dbi
      .withHandle(new HandleCallback<java.util.List<Tuple2<Long, String>>>() {

        String sql = String.format("select version, uow_events " +
                "from %s where target_id = :id " +
                " and version > :version " +
                "order by version", dbMetadata.unitOfWorkTable);

        public java.util.List<Tuple2<Long, String>> withHandle(Handle h) {
          return h.createQuery(sql)
                  .bind("id", id.toString())
                  .bind("version", version.getVersion())
                  .map(new EventsMapper()).list();
        }
      }
      );

    if (eventsListAsJson == null) {

      logger.debug("found none unit of work for id {} and version > {} on {}",
              id.toString(), version.getVersion(), dbMetadata.unitOfWorkTable);

      return new Tuple2<>(Version.create(0), List.empty());

    }

    logger.debug("found {} units of work for id {} and version > {} on {}",
            eventsListAsJson.size(), id.toString(), version.getVersion(), dbMetadata.unitOfWorkTable);

    final ArrayList<Event> result = new ArrayList<>();
    Long finalVersion = 0L;

    for (Tuple2<Long, String> tuple : eventsListAsJson) {
      logger.debug("converting to List<Event> from {}", tuple);
      final List<Event> events = gson.fromJson(tuple._2(), listTypeToken.getType());
      logger.debug(events.toString());
      events.forEach(e -> result.add(e));
      finalVersion = tuple._1();
    }

    return Tuple.of(new Version(finalVersion), List.ofAll(result));

  }

  @Override
  public void append(final UnitOfWork unitOfWork) throws DbConcurrencyException {

    requireNonNull(unitOfWork);

    final String cmdAsJson = gson.toJson(unitOfWork.getCommand(), Command.class);
    final String eventsAsJson = gson.toJson(unitOfWork.getEvents(), listTypeToken.getType());

    logger.debug("appending uow to {} with id {}", dbMetadata.aggregateRootTable, unitOfWork.getTargetId());

    dbi.inTransaction(TransactionIsolationLevel.SERIALIZABLE, (conn, status) -> {

      final Long currentVersion = conn.createQuery(selectAggRootSql)
              .bind("id", unitOfWork.getTargetId())
              .map(LongColumnMapper.WRAPPER).first();

      if ((currentVersion == null ? 0 : currentVersion) != unitOfWork.getVersion().getVersion() - 1) {
        throw new DbConcurrencyException(
                String.format("id = [%s], current_version = %d, new_version = %d",
                        unitOfWork.getTargetId(),
                        currentVersion, unitOfWork.getVersion().getVersion()));
      }

      int result1;

      if (currentVersion == null) {

        result1 = conn.createStatement(insertAggRootSql)
                .bind("id", unitOfWork.getTargetId())
                .bind("new_version", unitOfWork.getVersion().getVersion())
                .bind("last_update", new Timestamp(Instant.now().getEpochSecond()))
                .execute();

      } else {

        result1 = conn.createStatement(updateAggRootSql)
                .bind("id", unitOfWork.getTargetId())
                .bind("new_version", unitOfWork.getVersion().getVersion())
                .bind("curr_version", unitOfWork.getVersion().getVersion() - 1)
                .bind("last_update", new Timestamp(Instant.now().getEpochSecond()))
                .execute();
      }

      int result2 = conn.createStatement(insertUowSql)
              .bind("uow_id", unitOfWork.getUnitOfWorkId().toString())
              .bind("uow_events", eventsAsJson)
              .bind("cmd_id", unitOfWork.getCommand().getCommandId())
              .bind("cmd_data", cmdAsJson)
              .bind("target_id", unitOfWork.getTargetId())
              .bind("version", unitOfWork.getVersion().getVersion())
              .bind("inserted_on", new Timestamp(Instant.now().getEpochSecond()))
              .execute();

      conn.createStatement(
              String.format(
                      "SELECT pg_notify('%s', '%s'); ",
                      eventsChannelId,
                      unitOfWork.getUnitOfWorkId().toString()))
              .execute();

      // TODO schedular commands emitidos aqui ?? SIM (e remove CommandScheduler)

//              uow.getEvents().stream()
//            .filter(event -> event instanceof CommandScheduling) // TODO tem que ter idempotency disto
//            .map(event -> (CommandScheduling) e)
//            .forEachOrdered(cs -> commandScheduler.schedule(commandId, cs));

      if (result1 + result2 == 2) {
        return true;
      }

      throw new DbConcurrencyException(
              String.format("id = [%s], current_version = %d, new_version = %d",
                      unitOfWork.getTargetId(),
                      currentVersion, unitOfWork.getVersion().getVersion()));

    }

    );

  }

  static class DbMetadata {

    final String aggregateRootTable;
    final String unitOfWorkTable;
    private final String aggregateRootName;

    public DbMetadata(String aggregateRootName) {
      this.aggregateRootName = aggregateRootName;
      this.aggregateRootTable = aggregateRootName.concat("_AR");
      this.unitOfWorkTable = aggregateRootName.concat("_UOW");
    }

  }

}

class EventsMapper implements ResultSetMapper<Tuple2<Long, String>> {
  @Override
  public Tuple2<Long, String> map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
    return Tuple.of(resultSet.getLong("version"), resultSet.getString("uow_events"));
  }
}