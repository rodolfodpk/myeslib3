package myeslib3.persistence;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Value;
import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Event;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;

public interface SnapshotReader<K, A extends AggregateRoot> {

  Snapshot<A> getSnapshot(final K id);

  default List<Event> flatMap(final List<UnitOfWork> unitOfWorks) {
    return unitOfWorks.stream().flatMap((unitOfWork) -> unitOfWork.getEvents().stream()).collect(Collectors.toList());
  }

  default Version lastVersion(List<UnitOfWork> unitOfWorks) {
    return unitOfWorks.isEmpty() ? Version.create(0) : unitOfWorks.get(unitOfWorks.size()-1).getVersion();
  }

  @Value
  class Snapshot<A> {
    A instance;
    Version version;
  }
}