package myeslib3.core.stack;

import myeslib3.core.model.AggregateRoot;
import myeslib3.core.model.AggregateRootId;

@FunctionalInterface
public interface SnapshotReader<ID extends AggregateRootId, A extends AggregateRoot> {

	Snapshot<A> getSnapshot(final ID id);

}