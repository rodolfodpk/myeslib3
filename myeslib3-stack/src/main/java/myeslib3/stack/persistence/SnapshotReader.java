package myeslib3.stack.persistence;

import lombok.Value;
import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Version;

@FunctionalInterface
public interface SnapshotReader<A extends AggregateRoot> {

  Snapshot<A> getSnapshot(final String id);

  @Value
  class Snapshot<A> {
    A instance;
    Version version;
  }
}