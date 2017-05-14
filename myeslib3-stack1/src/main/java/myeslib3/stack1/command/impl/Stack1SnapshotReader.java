package myeslib3.stack1.command.impl;


import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
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
import static myeslib3.stack1.utils.EventsHelper.lastVersion;

@Slf4j
public class Stack1SnapshotReader<ID extends AggregateRootId, A extends AggregateRoot>
				implements SnapshotReader<ID, A> {

	private static final Logger logger = LoggerFactory.getLogger(Stack1SnapshotReader.class);

	final Cache<ID, Tuple2<Version, List<Event>>> cache;
	final WriteModelRepository dao;
  final Supplier<A> supplier;
  final Function<A, A> dependencyInjectionFn;
  final BiFunction<Event, A, A> stateTransitionFn;

	public Stack1SnapshotReader(@NonNull Cache<ID, Tuple2<Version, List<Event>>> cache,
															@NonNull WriteModelRepository dao,
															@NonNull Supplier<A> supplier,
															@NonNull Function<A, A> dependencyInjectionFn,
															@NonNull BiFunction<Event, A, A> stateTransitionFn) {
		this.cache = cache;
		this.dao = dao;
		this.supplier = supplier;
		this.dependencyInjectionFn = dependencyInjectionFn;
		this.stateTransitionFn = stateTransitionFn;
	}

	@Override
	public Snapshot<A> getSnapshot(ID id) {

		requireNonNull(id);

		logger.debug("id {} cache.getInstance(id)", id);

    final StateTransitionsTracker<A> tracker = new StateTransitionsTracker<>(supplier.get(),
            stateTransitionFn, dependencyInjectionFn);

		final AtomicBoolean wasDaoCalled = new AtomicBoolean(false);

		final Tuple2<Version, List<Event>> cachedUowList = cache.get(id, s -> {
			logger.debug("id {} cache.getInstance(id) does not contain anything for this id. Will have to search on dao",
							id);
			wasDaoCalled.set(true);
			return dao.getAll(id.getStringValue());
		});

		logger.debug("id {} wasDaoCalled ? {}", id, wasDaoCalled.get());

		if (wasDaoCalled.get()) {
			return new Snapshot<>(tracker.applyEvents(cachedUowList._2().toJavaList()).currentState(), cachedUowList._1());
		}

		final Version lastVersion = lastVersion(cachedUowList);

		logger.debug("id {} cached lastSnapshotData has version {}. will check if there any version beyond it",
						id, lastVersion);

		final Tuple2<Version, List<Event>> nonCachedUowList =
						dao.getAllAfterVersion(id.getStringValue(), lastVersion);

		logger.debug("id {} found {} pending transactions. Last version is now {}",
						id, nonCachedUowList._2().size(), lastVersion);

		final List<Event> cachedPlusNonCachedList = cachedUowList._2()
						.appendAll(nonCachedUowList._2());

		final Version finalVersion = nonCachedUowList._1();

		cache.put(id, Tuple.of(finalVersion, cachedPlusNonCachedList));

		return new Snapshot<>(tracker.applyEvents(cachedPlusNonCachedList.toJavaList()).currentState(), finalVersion);

	}

}
