package myeslib3.core.data;

import lombok.NonNull;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Value
public class UnitOfWork {

	@NonNull final UUID unitOfWorkId;
	@NonNull final Command command;
	@NonNull final Version version;
	@NonNull final List<Event> events;
	@NonNull final LocalDateTime timestamp;

	public static UnitOfWork create(Command command, Version version, List<Event> events) {
		return new UnitOfWork(UUID.randomUUID(), command, version, events, LocalDateTime.now());
	}

	public String getTargetId() {
		return command.getTargetId();
	}
}
