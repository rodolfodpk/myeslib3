package myeslib3.stack1.command;

import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.AggregateRootId;

@FunctionalInterface
public interface SnapshotReader<ID extends AggregateRootId, A extends AggregateRoot> {

	Snapshot<A> getSnapshot(final ID aggregateRootId);

}