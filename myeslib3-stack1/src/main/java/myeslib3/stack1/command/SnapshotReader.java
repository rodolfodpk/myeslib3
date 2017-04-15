package myeslib3.stack1.command;

import myeslib3.core.data.AggregateRoot;

@FunctionalInterface
public interface SnapshotReader<A extends AggregateRoot> {

	Snapshot<A> getSnapshot(final String aggregateRootId);

}