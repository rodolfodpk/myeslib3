package myeslib3.stack1;


import myeslib3.core.StateTransitionsTracker;
import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;
import myeslib3.stack.SnapshotReader;
import myeslib3.stack.WriteModelDao;
import org.apache.camel.com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static myeslib3.stack1.infra.utils.EventsHelper.flatMap;
import static myeslib3.stack1.infra.utils.EventsHelper.lastVersion;

public class Stack1SnapshotReader<A extends AggregateRoot> implements SnapshotReader<A> {

	private static final Logger logger = LoggerFactory.getLogger(Stack1SnapshotReader.class);

	private final Cache<String, List<UnitOfWork>> cache;
	private final WriteModelDao dao;

	public Stack1SnapshotReader(Cache<String, List<UnitOfWork>> cache,
															WriteModelDao dao) {
		checkNotNull(cache);
		checkNotNull(dao);

		this.cache = cache;
		this.dao = dao;
	}

	@Override
	public Snapshot<A> getSnapshot(String id, StateTransitionsTracker<A> tracker) {

		checkNotNull(id);
		checkNotNull(tracker);

		logger.debug("id {} cache.get(id)", id);

		final AtomicBoolean wasDaoCalled = new AtomicBoolean(false);

		final List<UnitOfWork> cachedUowList = cache.get(id, s -> {
			logger.debug("id {} cache.get(id) does not contain anything for this id. Will have to search on dao", id);
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
