package myeslib3.stack.persistence;

import lombok.Value;
import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Version;

@FunctionalInterface
public interface SnapshotReader<A extends AggregateRoot> {

  Snapshot<A> getSnapshot(final String id);
//
//  default List<Event> flatMap(final List<UnitOfWork> unitOfWorks) {
//    return unitOfWorks.stream().flatMap((unitOfWork) -> unitOfWork.getEvents().stream()).collect(Collectors.toList());
//  }
//
//  default Version lastVersion(final List<UnitOfWork> unitOfWorks) {
//    return unitOfWorks.isEmpty() ? Version.create(0) : unitOfWorks.get(unitOfWorks.size()-1).getVersion();
//  }

  @Value
  class Snapshot<A> {
    A instance;
    Version version;
  }
}