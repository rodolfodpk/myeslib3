package myeslib3.stack1.command.impl;


import lombok.AllArgsConstructor;
import lombok.NonNull;
import myeslib3.core.AggregateRoot;
import myeslib3.core.UnitOfWork;
import myeslib3.core.Version;
import myeslib3.core.command.WriteModelStateTracker;
import myeslib3.stack1.command.SnapshotReader;
import myeslib3.stack1.command.WriteModelRepository;
import org.apache.camel.com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static myeslib3.stack1.stack1infra.utils.EventsHelper.flatMap;
import static myeslib3.stack1.stack1infra.utils.EventsHelper.lastVersion;

@AllArgsConstructor
public class Stack1SnapshotReader<AGGREGATE_ROOT extends AggregateRoot> implements SnapshotReader<AGGREGATE_ROOT> {

	private static final Logger logger = LoggerFactory.getLogger(Stack1SnapshotReader.class);

	@NonNull Cache<String, List<UnitOfWork>> cache;
	@NonNull WriteModelRepository dao;

	@Override
	public Snapshot<AGGREGATE_ROOT> getSnapshot(String id, WriteModelStateTracker<AGGREGATE_ROOT> tracker) {

		requireNonNull(id);
		requireNonNull(tracker);

		logger.debug("id {} cache.getInstance(id)", id);

		final AtomicBoolean wasDaoCalled = new AtomicBoolean(false);

		final List<UnitOfWork> cachedUowList = cache.get(id, s -> {
			logger.debug("id {} cache.getInstance(id) does not contain anything for this id. Will have to search on dao", id);
			wasDaoCalled.set(true);
			return dao.getAll(id);
		});

		logger.debug("id {} wasDaoCalled ? {}", id, wasDaoCalled.get());

		if (wasDaoCalled.get()) {
			return new Snapshot<>(tracker.applyEvents(flatMap(cachedUowList)).currentState(), lastVersion(cachedUowList));
		}

		final Version lastVersion = lastVersion(cachedUowList);

		logger.debug("id {} cached lastSnapshotData has version {}. will check if there any version beyond it",
						id, lastVersion);

		final List<UnitOfWork> nonCachedUowList = dao.getAllAfterVersion(id, lastVersion);

		logger.debug("id {} found {} pending transactions. Last version is now {}",
						id, nonCachedUowList.size(), lastVersion);

		final List<UnitOfWork> cachedPlusNonCachedUowList = Stream.of(cachedUowList, nonCachedUowList)
						.flatMap(x -> x.stream()).collect(Collectors.toList());

		final Version finalVersion = lastVersion(cachedPlusNonCachedUowList);

		cache.put(id, cachedPlusNonCachedUowList);

		return new Snapshot<>(tracker.applyEvents(flatMap(cachedPlusNonCachedUowList)).currentState(), finalVersion);
	}

}
