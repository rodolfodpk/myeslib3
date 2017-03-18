package myeslib3.stack;

import myeslib3.core.StateTransitionsTracker;
import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Version;

@FunctionalInterface
public interface SnapshotReader<A extends AggregateRoot> {

	Snapshot<A> getSnapshot(final String id, StateTransitionsTracker<A> tracker);

	class Snapshot<A> {

		private final A instance;
		private final Version version;

		public Snapshot(A instance, Version version) {
			this.instance = instance;
			this.version = version;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Snapshot<?> snapshot = (Snapshot<?>) o;

			if (!instance.equals(snapshot.instance)) return false;
			return version.equals(snapshot.version);
		}

		@Override
		public int hashCode() {
			int result = instance.hashCode();
			result = 31 * result + version.hashCode();
			return result;
		}

		@Override
		public String toString() {
			return "Snapshot{" +
							"instance=" + instance +
							", version=" + version +
							'}';
		}

		public A getInstance() {
			return instance;
		}

		public Version getVersion() {
			return version;
		}
	}
}