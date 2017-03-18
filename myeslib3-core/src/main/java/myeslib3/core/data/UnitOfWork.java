package myeslib3.core.data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class UnitOfWork {

  private final UUID unitOfWorkId;
  private final String aggregateRootId;
  private final String commandId;
  private final Command command;
  private final Version version;
  private final List<Event> events;
  private final LocalDateTime timestamp;

  public UnitOfWork(UUID unitOfWorkId, String aggregateRootId, String commandId, Command command, Version version, List<Event> events, LocalDateTime timestamp) {
    this.unitOfWorkId = unitOfWorkId;
    this.aggregateRootId = aggregateRootId;
    this.commandId = commandId;
    this.command = command;
    this.version = version;
    this.events = events;
    this.timestamp = timestamp;
  }

  public static UnitOfWork create(String aggregateRootId, String commandId, Command command, Version version,
                                  List<Event> events) {
    return new UnitOfWork(UUID.randomUUID(), aggregateRootId, commandId, command, version, events, LocalDateTime.now());
  }

  public static UnitOfWork create(String aggregateRootId, String commandId, Command command, Version version,
                                  List<Event> events, LocalDateTime timestamp) {
    return new UnitOfWork(UUID.randomUUID(), aggregateRootId, commandId, command, version, events, timestamp);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UnitOfWork that = (UnitOfWork) o;

    if (!unitOfWorkId.equals(that.unitOfWorkId)) return false;
    if (!aggregateRootId.equals(that.aggregateRootId)) return false;
    if (!commandId.equals(that.commandId)) return false;
    if (!command.equals(that.command)) return false;
    if (!version.equals(that.version)) return false;
    if (!events.equals(that.events)) return false;
    return timestamp.equals(that.timestamp);
  }

  @Override
  public int hashCode() {
    int result = unitOfWorkId.hashCode();
    result = 31 * result + aggregateRootId.hashCode();
    result = 31 * result + commandId.hashCode();
    result = 31 * result + command.hashCode();
    result = 31 * result + version.hashCode();
    result = 31 * result + events.hashCode();
    result = 31 * result + timestamp.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "UnitOfWork{" +
            "unitOfWorkId=" + unitOfWorkId +
            ", aggregateRootId='" + aggregateRootId + '\'' +
            ", commandId='" + commandId + '\'' +
            ", command=" + command +
            ", version=" + version +
            ", events=" + events +
            ", timestamp=" + timestamp +
            '}';
  }

  public UUID getUnitOfWorkId() {
    return unitOfWorkId;
  }

  public String getAggregateRootId() {
    return aggregateRootId;
  }

  public String getCommandId() {
    return commandId;
  }

  public Command getCommand() {
    return command;
  }

  public Version getVersion() {
    return version;
  }

  public List<Event> getEvents() {
    return events;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }
}
