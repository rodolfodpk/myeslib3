package myeslib3.stack1.command;

import javaslang.Tuple2;
import javaslang.collection.List;
import myeslib3.core.data.AggregateRootId;
import myeslib3.core.data.Event;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;

import java.util.Optional;
import java.util.UUID;

public interface WriteModelRepository<ID extends AggregateRootId>  {

	void append(UnitOfWork unitOfWork);

	Optional<UnitOfWork> get(UUID uowId);

	Tuple2<Version, List<Event>> getAll(ID id);

	Tuple2<Version, List<Event>> getAllAfterVersion(ID id, Version version);

}
