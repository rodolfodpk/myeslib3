package myeslib3.stack1.command.impl;


import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.List;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import myeslib3.core.StateTransitionsTracker;
import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.AggregateRootId;
import myeslib3.core.data.Event;
import myeslib3.core.data.Version;
import myeslib3.stack1.command.Snapshot;
import myeslib3.stack1.command.SnapshotReader;
import myeslib3.stack1.command.WriteModelRepository;
import org.apache.camel.com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static myeslib3.stack1.stack1infra.utils.EventsHelper.lastVersion;

@AllArgsConstructor
public class Stack1SnapshotReader<ID extends AggregateRootId, A extends AggregateRoot> implements SnapshotReader<ID, A> {

	private static final Logger logger = LoggerFactory.getLogger(Stack1SnapshotReader.class);

	@NonNull final Cache<ID, Tuple2<Version, List<Event>>> cache;
	@NonNull final WriteModelRepository dao;
  @NonNull final Supplier<A> supplier;
  @NonNull final Function<A, A> dependencyInjectionFn;
  @NonNull final BiFunction<Event, A, A> stateTransitionFn;

  @Override
	public Snapshot<A> getSnapshot(ID aggregateRootId) {

		requireNonNull(aggregateRootId);

		logger.debug("id {} cache.getInstance(id)", aggregateRootId);

    final StateTransitionsTracker<A> tracker = new StateTransitionsTracker<>(supplier.get(),
            stateTransitionFn, dependencyInjectionFn);

		final AtomicBoolean wasDaoCalled = new AtomicBoolean(false);

		final Tuple2<Version, List<Event>> cachedUowList = cache.get(aggregateRootId, s -> {
			logger.debug("id {} cache.getInstance(id) does not contain anything for this id. Will have to search on dao",
							aggregateRootId);
			wasDaoCalled.set(true);
			return dao.getAll(aggregateRootId);
		});

		logger.debug("id {} wasDaoCalled ? {}", aggregateRootId, wasDaoCalled.get());

		if (wasDaoCalled.get()) {
			return new Snapshot<>(tracker.applyEvents(cachedUowList._2().toJavaList()).currentState(), cachedUowList._1());
		}

		final Version lastVersion = lastVersion(cachedUowList);

		logger.debug("id {} cached lastSnapshotData has version {}. will check if there any version beyond it",
            aggregateRootId, lastVersion);

		final Tuple2<Version, List<Event>> nonCachedUowList =
						dao.getAllAfterVersion(aggregateRootId, lastVersion);

		logger.debug("id {} found {} pending transactions. Last version is now {}",
            aggregateRootId, nonCachedUowList._2().size(), lastVersion);

		final List<Event> cachedPlusNonCachedList = cachedUowList._2()
						.appendAll(nonCachedUowList._2());

		final Version finalVersion = nonCachedUowList._1();

		cache.put(aggregateRootId, Tuple.of(finalVersion, cachedPlusNonCachedList));

		return new Snapshot<>(tracker.applyEvents(cachedPlusNonCachedList.toJavaList()).currentState(), finalVersion);

	}

}
