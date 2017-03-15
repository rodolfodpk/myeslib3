package myeslib3.stack1.persistence;

import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Event;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;
import myeslib3.stack.persistence.SnapshotReader;

import java.util.List;
import java.util.stream.Collectors;

public class PostgresSnapshotReader<A extends AggregateRoot> implements SnapshotReader<A> {

    @Override
    public Snapshot<A> getSnapshot(String id) {
        return null; // TODO
    }

  public List<Event> flatMap(final List<UnitOfWork> unitOfWorks) {
    return unitOfWorks.stream().flatMap((unitOfWork) -> unitOfWork.getEvents().stream()).collect(Collectors.toList());
  }

  public Version lastVersion(final List<UnitOfWork> unitOfWorks) {
    return unitOfWorks.isEmpty() ? Version.create(0) : unitOfWorks.get(unitOfWorks.size()-1).getVersion();
  }

}
