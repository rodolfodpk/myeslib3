package myeslib3.core;

import lombok.NonNull;
import lombok.Value;
import myeslib3.core.model.AggregateRootId;
import myeslib3.core.model.Command;
import myeslib3.core.model.Event;

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

  public static UnitOfWork of(Command command, Version version, List<Event> events) {
    return new UnitOfWork(UUID.randomUUID(), command, version, events);
  }

  public AggregateRootId getTargetId() {
    return command.getTargetId();
  }

}
