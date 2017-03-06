package myeslib3.core.data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Value;

@Value
public class UnitOfWork {

  UUID unitOfWorkId;
  String aggregateRootId;
  String commandId;
  Command command;
  Version version;
  List<Event> events;
  LocalDateTime timestamp;

  public static UnitOfWork create(String aggregateRootId, String commandId, Command command, Version version,
                                  List<Event> events) {
    return new UnitOfWork(UUID.randomUUID(), aggregateRootId, commandId, command, version, events, LocalDateTime.now());
  }

  public static UnitOfWork create(String aggregateRootId, String commandId, Command command, Version version,
                                  List<Event> events, LocalDateTime timestamp) {
    return new UnitOfWork(UUID.randomUUID(), aggregateRootId, commandId, command, version, events, timestamp);
  }

}
