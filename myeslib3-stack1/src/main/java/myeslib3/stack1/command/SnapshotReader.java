package myeslib3.stack1.command;

import lombok.Value;
import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Version;

@FunctionalInterface
public interface SnapshotReader<A extends AggregateRoot> {

	Snapshot<A> getSnapshot(final String aggregateRootId);

	@Value
	class Snapshot<A> {
		final A instance;
		final Version version;

    public Version nextVersion() {
    	return version.nextVersion();
    }
  }
}