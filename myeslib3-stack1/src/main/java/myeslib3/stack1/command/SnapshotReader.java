package myeslib3.stack1.command;

import lombok.Value;
import myeslib3.core.StateTransitionsTracker;
import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Version;

@FunctionalInterface
public interface SnapshotReader<AGGREGATE_ROOT extends AggregateRoot> {

	Snapshot<AGGREGATE_ROOT> getSnapshot(final String id, StateTransitionsTracker<AGGREGATE_ROOT> tracker);

	@Value
	class Snapshot<AGGREGATE_ROOT> {
		final AGGREGATE_ROOT instance;
		final Version version;
	}
}