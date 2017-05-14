package myeslib3.core.data;

import lombok.NonNull;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
public class UnitOfWork {

  @NonNull
  final UUID unitOfWorkId;
  @NonNull
  final Command command;
  @NonNull
  final Version version;
  @NonNull
  final List<Event> events;
//	@NonNull final LocalDateTime timestamp;

  public static UnitOfWork of(Command command, Version version, List<Event> events) {
    return new UnitOfWork(UUID.randomUUID(), command, version, events);
  }

  public AggregateRootId getTargetId() {
    return command.getTargetId();
  }

}
