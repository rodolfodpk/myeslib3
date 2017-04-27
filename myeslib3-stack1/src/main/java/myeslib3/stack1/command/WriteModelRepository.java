package myeslib3.stack1.command;

import javaslang.Tuple2;
import javaslang.collection.List;
import myeslib3.core.data.Event;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;

import java.util.Optional;
import java.util.UUID;

public interface WriteModelRepository  {

	void append(UnitOfWork unitOfWork);

	Optional<UnitOfWork> get(UUID uowId);

	List<Tuple2<String, List<Event>>> getAllSince(long sinceUowSequence, int maxResultSize);

	Tuple2<Version, List<Event>> getAll(String aggregateRootId);

	Tuple2<Version, List<Event>> getAllAfterVersion(String aggregateRootId, Version version);

}
