package myeslib3.stack1.command.impl;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.Tuple4;
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

  public Stack1WriteModelRepository(String eventsChannelId, String aggregateRootName, Gson gson, DBI dbi) {

    requireNonNull(aggregateRootName);
    requireNonNull(eventsChannelId);
    requireNonNull(gson);
    requireNonNull(dbi);

    this.eventsChannelId = eventsChannelId;
    this.dbMetadata = new DbMetadata(aggregateRootName);
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
            String.format("insert into %s (uow_id, uow_events, cmd_id, cmd_data, target_id, version, inserted_on) " +
                            "values (:uow_id, :uow_events, :cmd_id, :cmd_data, :target_id, :version, :inserted_on)",
                    dbMetadata.unitOfWorkTable);

    logger.debug(updateAggRootSql);
    logger.debug(insertUowSql);

  }


  @Override
  public Optional<UnitOfWork> get(UUID uowId) {

    final Tuple4<String, String, Long, String> uowTuple = dbi
      .withHandle(new HandleCallback<Tuple4<String, String, Long, String>>() {
        final String sql = String.format("select * from %s where uow_id = :uow_id ", dbMetadata.unitOfWorkTable);
        public Tuple4<String, String, Long, String> withHandle(Handle h) {
          return h.createQuery(sql)
                  .bind("uow_id", uowId.toString())
                  .map(new UnitOfWorkMapper()).first();
        }
      }
      );

    final Command command = gson.fromJson(uowTuple._2(), Command.class);
    final java.util.List<Event> events = gson.fromJson(uowTuple._4(), listTypeToken.getType());
    final UnitOfWork uow = new UnitOfWork(UUID.fromString(uowTuple._1()), command,  new Version(uowTuple._3()), events);

    return uowTuple == null ? Optional.empty() : Optional.of(uow);

  }

  @Override
  public List<UnitOfWorkData> getAllSince(long sinceUowSequence, int maxResultSize) {

    logger.debug("will load a maximum of {} units of work since sequence {}", maxResultSize, sinceUowSequence);

    final java.util.List<Tuple4<String, Long, String, String>> eventsListAsJson = dbi
            .withHandle(new HandleCallback<java.util.List<Tuple4<String, Long, String, String>>>() {

                          String sql = String.format("select uow_id, uow_seq_number, target_id, uow_events " +
                                  "from %s where uow_seq_number > %d order by uow_seq_number limit %d",
                                  dbMetadata.unitOfWorkTable, sinceUowSequence, maxResultSize);

                          public java.util.List<Tuple4<String, Long, String, String>> withHandle(Handle h) {
                            return h.createQuery(sql)
                                    .bind("uow_seq_number", sinceUowSequence)
                                    .map(new ListOfEventsMapper()).list();
                          }
                        }
            );

    if (eventsListAsJson == null) {

      logger.info("Found none unit of work since sequence {}", sinceUowSequence);

      return List.empty();

    }

    logger.info("Found {} units of work since sequence {}", eventsListAsJson.size(), sinceUowSequence);

    final ArrayList<UnitOfWorkData> result = new ArrayList<>();

    for (Tuple4<String, Long, String, String> tuple : eventsListAsJson) {
      logger.info("converting to List<Event> from {}", tuple);
      final List<Event> events = gson.fromJson(tuple._4(), listTypeToken.getType());
      logger.debug(events.toString());
      events.forEach(e -> result.add(new UnitOfWorkData(tuple._1(), tuple._2(), tuple._3(), events)));
    }

    return List.ofAll(result);

  }

  @Override
  public Long getLastUowSequence() {
    return null; // TODO
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
                  .bind("version", version.getValueAsLong())
                  .map(new EventsMapper()).list();
        }
      }
      );

    if (eventsListAsJson == null) {

      logger.debug("found none unit of work for id {} and version > {} on {}",
              id.toString(), version.getValueAsLong(), dbMetadata.unitOfWorkTable);

      return new Tuple2<>(Version.create(0), List.empty());

    }

    logger.debug("found {} units of work for id {} and version > {} on {}",
            eventsListAsJson.size(), id.toString(), version.getValueAsLong(), dbMetadata.unitOfWorkTable);

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

    logger.debug("appending uow to {} with id {}", dbMetadata.aggregateRootTable, unitOfWork.getTargetId());

    dbi.inTransaction(TransactionIsolationLevel.SERIALIZABLE, (conn, status) -> {

      final Long currentVersion = conn.createQuery(selectAggRootSql)
              .bind("id", unitOfWork.getTargetId().getStringValue())
              .map(LongColumnMapper.WRAPPER).first();

      if ((currentVersion == null ? 0 : currentVersion) != unitOfWork.getVersion().getValueAsLong() - 1) {
        throw new DbConcurrencyException(
                String.format("id = [%s], current_version = %d, new_version = %d",
                        unitOfWork.getTargetId().getStringValue(),
                        currentVersion, unitOfWork.getVersion().getValueAsLong()));
      }

      int result1;

      if (currentVersion == null) {

        result1 = conn.createStatement(insertAggRootSql)
                .bind("id", unitOfWork.getTargetId().getStringValue())
                .bind("new_version", unitOfWork.getVersion().getValueAsLong())
                .bind("last_update", new Timestamp(Instant.now().getEpochSecond()))
                .execute();

      } else {

        result1 = conn.createStatement(updateAggRootSql)
                .bind("id", unitOfWork.getTargetId().getStringValue())
                .bind("new_version", unitOfWork.getVersion().getValueAsLong())
                .bind("curr_version", unitOfWork.getVersion().getValueAsLong() - 1)
                .bind("last_update", new Timestamp(Instant.now().getEpochSecond()))
                .execute();
      }

      final String cmdAsJson = gson.toJson(unitOfWork.getCommand(), Command.class);
      final String eventsAsJson = gson.toJson(unitOfWork.getEvents(), listTypeToken.getType());

      int result2 = conn.createStatement(insertUowSql)
        .bind("uow_id", unitOfWork.getUnitOfWorkId().toString())
        .bind("uow_events", eventsAsJson)
        .bind("cmd_id", unitOfWork.getCommand().getCommandId().toString())
        .bind("cmd_data", cmdAsJson)
        .bind("target_id", unitOfWork.getTargetId().getStringValue())
        .bind("version", unitOfWork.getVersion().getValueAsLong())
        .bind("inserted_on", new Timestamp(Instant.now().getEpochSecond()))
        .execute();

//      val notify = String.format(
//              "SELECT pg_notify('%s', '%s'); ",
//            //   "NOTIFY '%s', '%s' ; ",
//              eventsChannelId,
//              unitOfWork.getUnitOfWorkId().toString());
//
//      int result3 = conn.createStatement(notify).execute();

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
                      unitOfWork.getTargetId().getStringValue(),
                      currentVersion, unitOfWork.getVersion().getValueAsLong()));

    }

    );

  }

  static class DbMetadata {

    final String aggregateRootTable;
    final String unitOfWorkTable;

    public DbMetadata(String aggregateRootName) {
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

class ListOfEventsMapper implements ResultSetMapper<Tuple4<String, Long, String, String>> {
  @Override
  public Tuple4<String, Long, String, String> map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
    return Tuple.of(resultSet.getString("uow_id"),
                    resultSet.getLong("uow_seq_number"),
                    resultSet.getString("target_id"),
                    resultSet.getString("uow_events"));
  }
}

class UnitOfWorkMapper implements ResultSetMapper<Tuple4<String, String, Long, String>> {
  @Override
  public Tuple4<String, String, Long, String> map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
    return Tuple.of(resultSet.getString("uow_id"),
            resultSet.getString("cmd_data"),
            resultSet.getLong("version"),
            resultSet.getString("uow_events"));

  }
}
