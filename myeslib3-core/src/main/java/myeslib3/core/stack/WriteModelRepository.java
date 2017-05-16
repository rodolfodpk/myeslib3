package myeslib3.core.stack;

import myeslib3.core.UnitOfWork;
import myeslib3.core.Version;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WriteModelRepository  {

	void append(UnitOfWork unitOfWork) throws DbConcurrencyException;

	Optional<UnitOfWork> get(UUID uowId);

	List<ProjectionData> getAllSince(long sinceUowSequence, int maxResultSize);

	VersionedEvents getAll(String aggregateRootId);

	VersionedEvents getAllAfterVersion(String aggregateRootId, Version version);

  class DbConcurrencyException extends Exception {

    public DbConcurrencyException(String s) {
      super(s);
    }

  }
}
