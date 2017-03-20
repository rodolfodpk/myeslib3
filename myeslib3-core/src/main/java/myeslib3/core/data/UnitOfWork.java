package myeslib3.core.data;

import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Value
public class UnitOfWork {

	private final UUID unitOfWorkId;
	private final String aggregateRootId;
	private final String commandId;
	private final Version version;
	private final List<Event> events;
	private final LocalDateTime timestamp;

	public static UnitOfWork create(String aggregateRootId, String commandId, Command command, Version version,
																	List<Event> events) {
		return new UnitOfWork(UUID.randomUUID(), aggregateRootId, commandId, version, events, LocalDateTime.now());
	}

	public static UnitOfWork create(String aggregateRootId, String commandId, Command command, Version version,
																	List<Event> events, LocalDateTime timestamp) {
		return new UnitOfWork(UUID.randomUUID(), aggregateRootId, commandId, version, events, timestamp);
	}

}
