package myeslib3.core.data;

import lombok.NonNull;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Value
public class UnitOfWork {

	@NonNull private final UUID unitOfWorkId;
	@NonNull private final String aggregateRootId;
	@NonNull private final Version version;
	@NonNull private final List<Event> events;
	@NonNull private final LocalDateTime timestamp;

	public static UnitOfWork create(String aggregateRootId, Version version, List<Event> events) {
		return new UnitOfWork(UUID.randomUUID(), aggregateRootId, version, events, LocalDateTime.now());
	}

}
