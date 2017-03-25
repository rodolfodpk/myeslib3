package myeslib3.stack1.command;

import lombok.Value;
import myeslib3.core.AggregateRoot;
import myeslib3.core.Version;
import myeslib3.core.command.WriteModelStateTracker;

@FunctionalInterface
public interface SnapshotReader<AGGREGATE_ROOT extends AggregateRoot> {

	Snapshot<AGGREGATE_ROOT> getSnapshot(final String id, WriteModelStateTracker<AGGREGATE_ROOT> tracker);

	@Value
	class Snapshot<AGGREGATE_ROOT> {
		final AGGREGATE_ROOT instance;
		final Version version;
	}
}