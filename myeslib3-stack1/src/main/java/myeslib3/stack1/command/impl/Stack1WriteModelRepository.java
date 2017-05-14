package myeslib3.stack1.command.impl;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.Tuple4;
import javaslang.collection.List;
import lombok.NonNull;
import myeslib3.core.data.Command;
import myeslib3.core.data.Event;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;
import myeslib3.stack1.command.UnitOfWorkData;
import myeslib3.stack1.command.WriteModelRepository;
import myeslib3.stack1.utils.jdbi.DbConcurrencyException;
import myeslib3.stack1.utils.jdbi.LocalDateTimeMapper;
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

  static final String getUowSql = "select * from units_of_work where uow_id = :uow_id ";

  static final String getAllAfterVersionSql = "select version, uow_events " +
                                                "from units_of_work where ar_id = :ar_id and ar_name = :ar_name " +
                                                " and version > :version " +
                                                "order by version";

  static final String selectAggRootSql =
          "select version from aggregate_roots where ar_id = :ar_id and ar_name = :ar_name";

  static final String insertAggRootSql = "insert into aggregate_roots (ar_id, ar_name, version, last_updated_on) " +
        "values (:ar_id, :ar_name, :new_version, :last_updated_on) ";

  static final String updateAggRootSql =
          "update aggregates_root set version_number = :new_version, last_updated_on = :last_updated_on " +
        "where ar_id = :ar_id and ar_name = :ar_name and version_number = :curr_version";

  static final String insertUowSql = "insert into units_of_work " +
        "(uow_id, uow_events, cmd_id, cmd_data, ar_id, ar_name, version, inserted_on) " +
        "values (:uow_id, :uow_events, :cmd_id, :cmd_data, :ar_id, :ar_name, :version, :inserted_on)";

  private final String aggregateRootName;
  private final Gson gson;
  private final DBI dbi;

  private final TypeToken<java.util.List<Event>> listTypeToken = new TypeToken<java.util.List<Event>>() {};

  public Stack1WriteModelRepository(String aggregateRootName, @NonNull Gson gson, @NonNull DBI dbi) {
    this.aggregateRootName = aggregateRootName;
    this.gson = gson;
    this.dbi = dbi;
    this.dbi.registerColumnMapper(new LocalDateTimeMapper());

  }

  @Override
  public Optional<UnitOfWork> get(UUID uowId) {

    final Tuple4<String, String, Long, String> uowTuple = dbi
      .withHandle(h -> h.createQuery(getUowSql)
              .bind("uow_id", uowId.toString())
              .map(new UnitOfWorkMapper()).first()
      );

    final Command command = gson.fromJson(uowTuple._2(), Command.class);
    final java.util.List<Event> events = gson.fromJson(uowTuple._4(), listTypeToken.getType());
    final UnitOfWork uow = new UnitOfWork(UUID.fromString(uowTuple._1()), command,  new Version(uowTuple._3()), events);

    return Optional.of(uow);

  }

  @Override
  public List<UnitOfWorkData> getAllSince(long sinceUowSequence, int maxResultSize) {

    logger.debug("will load a maximum of {} units of work since sequence {}", maxResultSize, sinceUowSequence);

    final java.util.List<Tuple4<String, Long, String, String>> eventsListAsJson = dbi
            .withHandle(new HandleCallback<java.util.List<Tuple4<String, Long, String, String>>>() {

                          final String sql = String.format("select uow_id, uow_seq_number, ar_id, uow_events " +
                                  "from units_of_work where uow_seq_number > %d order by uow_seq_number limit %d",
                                  sinceUowSequence, maxResultSize);

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
  public Tuple2<Version, List<Event>> getAll(String id) {
    return getAllAfterVersion(id, new Version(0L));
  }

  @Override
  public Tuple2<Version, List<Event>> getAllAfterVersion(@NonNull String id, @NonNull Version version) {

    logger.debug("will load {}", id);

    final java.util.List<Tuple2<Long, String>> eventsListAsJson = dbi
      .withHandle(h -> h.createQuery(getAllAfterVersionSql)
              .bind("ar_id", id.toString())
              .bind("version", version.getValueAsLong())
              .map(new EventsMapper()).list()
      );

    if (eventsListAsJson == null) {

      logger.debug("found none unit of work for id {} and version > {}",
              id.toString(), version.getValueAsLong());

      return new Tuple2<>(Version.create(0), List.empty());

    }

    logger.debug("found {} units of work for id {} and version > {}",
            eventsListAsJson.size(), id.toString(), version.getValueAsLong());

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

    logger.debug("appending uow to units_of_work with id {}", unitOfWork.getTargetId());

    dbi.inTransaction(TransactionIsolationLevel.SERIALIZABLE, (conn, status) -> {

      final Long currentVersion = conn.createQuery(selectAggRootSql)
              .bind("ar_id", unitOfWork.getTargetId().getStringValue())
              .bind("ar_name", aggregateRootName)
              .map(LongColumnMapper.WRAPPER).first();

      if ((currentVersion == null ? 0 : currentVersion) != unitOfWork.getVersion().getValueAsLong() - 1) {
        throw new DbConcurrencyException(
                String.format("ar_id = [%s], current_version = %d, new_version = %d",
                        unitOfWork.getTargetId().getStringValue(),
                        currentVersion, unitOfWork.getVersion().getValueAsLong()));
      }

      int result1;

      if (currentVersion == null) {

        result1 = conn.createStatement(insertAggRootSql)
                .bind("ar_id", unitOfWork.getTargetId().getStringValue())
                .bind("ar_name", aggregateRootName)
                .bind("new_version", unitOfWork.getVersion().getValueAsLong())
                .bind("last_updated_on", new Timestamp(Instant.now().getEpochSecond()))
                .execute();

      } else {

        result1 = conn.createStatement(updateAggRootSql)
                .bind("ar_id", unitOfWork.getTargetId().getStringValue())
                .bind("ar_name", aggregateRootName)
                .bind("new_version", unitOfWork.getVersion().getValueAsLong())
                .bind("curr_version", unitOfWork.getVersion().getValueAsLong() - 1)
                .bind("last_updated_on", new Timestamp(Instant.now().getEpochSecond()))
                .execute();
      }

      final String cmdAsJson = gson.toJson(unitOfWork.getCommand(), Command.class);
      final String eventsAsJson = gson.toJson(unitOfWork.getEvents(), listTypeToken.getType());

      int result2 = conn.createStatement(insertUowSql)
        .bind("uow_id", unitOfWork.getUnitOfWorkId().toString())
        .bind("uow_events", eventsAsJson)
        .bind("cmd_id", unitOfWork.getCommand().getCommandId().toString())
        .bind("cmd_data", cmdAsJson)
        .bind("ar_id", unitOfWork.getTargetId().getStringValue())
        .bind("ar_name", aggregateRootName)
        .bind("version", unitOfWork.getVersion().getValueAsLong())
        .bind("inserted_on", new Timestamp(Instant.now().getEpochSecond()))
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
                      unitOfWork.getTargetId().getStringValue(),
                      currentVersion, unitOfWork.getVersion().getValueAsLong()));

    }

    );

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
                    resultSet.getString("ar_id"),
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
