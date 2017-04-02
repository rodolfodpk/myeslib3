package myeslib3.stack1.command.impl;

import com.google.inject.Guice;
import com.google.inject.Injector;
import myeslib3.core.StateTransitionsTracker;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;
import myeslib3.core.functions.DependencyInjectionFn;
import myeslib3.core.functions.WriteModelStateTransitionFn;
import myeslib3.example1.core.aggregates.customer.*;
import myeslib3.examples.example1.runtime.CustomerModule;
import myeslib3.stack1.command.SnapshotReader;
import myeslib3.stack1.command.WriteModelRepository;
import org.apache.camel.com.github.benmanes.caffeine.cache.Cache;
import org.apache.camel.com.github.benmanes.caffeine.cache.Caffeine;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

public class Stack1SnapshotReaderTest {

	final Injector injector = Guice.createInjector(new CustomerModule());

    @Inject
    Supplier<Customer> supplier;
    @Inject
    DependencyInjectionFn<Customer> dependencyInjectionFn;
    @Inject
		WriteModelStateTransitionFn<Customer> writeModelStateTransitionFn;

    @Mock
    WriteModelRepository dao;

    Cache<String, List<UnitOfWork>> cache;

    @Before
    public void init() throws Exception {
        cache = Caffeine.newBuilder().build();
        injector.injectMembers(this);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void on_empty_history_then_returns_version_0() throws ExecutionException {

			String id = "customer#1";

			final SnapshotReader.Snapshot<Customer> expectedSnapshot =
							new SnapshotReader.Snapshot<>(supplier.get(), new Version(0L));

			final List<UnitOfWork> expectedHistory = new ArrayList<>();

			final StateTransitionsTracker<Customer> tracker = new StateTransitionsTracker<>(supplier.get(),
							writeModelStateTransitionFn, dependencyInjectionFn);

			when(dao.getAll(id)).thenReturn(expectedHistory);

			final Stack1SnapshotReader<Customer> reader = new Stack1SnapshotReader<>(cache, dao);

			assertThat(reader.getSnapshot(id, tracker)).isEqualTo(expectedSnapshot);

			verify(dao).getAll(id);

			verifyNoMoreInteractions(dao);

    }

    @Test
    public void on_empty_cache_then_returns_version_from_db() {

			final String id = "customer#1";
			final String name =  "customer#1 name";

			final Customer expectedInstance = new Customer(id, name, false, null, null, null);

			final SnapshotReader.Snapshot<Customer> expectedSnapshot =
								new SnapshotReader.Snapshot<>(expectedInstance, Version.create(1L));

			final CreateCustomerCmd command = new CreateCustomerCmd(name);

			final UnitOfWork newUow = new UnitOfWork(UUID.randomUUID(), id, UUID.randomUUID().toString(),
																				new Version(1L),
																				asList(new CustomerCreated(id, command.getName())),
																				LocalDateTime.now());

			final List<UnitOfWork> expectedHistory = Lists.newArrayList(newUow);

			when(dao.getAll(id)).thenReturn(expectedHistory);

			final StateTransitionsTracker<Customer> tracker = new StateTransitionsTracker<>(supplier.get(),
							writeModelStateTransitionFn, dependencyInjectionFn);

			final Stack1SnapshotReader<Customer> reader = new Stack1SnapshotReader<>(cache, dao);

			assertThat(reader.getSnapshot(id, tracker)).isEqualTo(expectedSnapshot);

			verify(dao).getAll(id);

			verifyNoMoreInteractions(dao);


    }

    @Test
    public void on_cache_then_hits_db_to_check_newer_version() {

			final String id = "customer#1";
			final String name =  "customer#1 name";

			final Customer expectedInstance = new Customer(id, name, false, null, null, null);

			final SnapshotReader.Snapshot<Customer> expectedSnapshot =
							new SnapshotReader.Snapshot<>(expectedInstance, Version.create(1L));

			final CreateCustomerCmd command = new CreateCustomerCmd(name);

			final UnitOfWork newUow = new UnitOfWork(UUID.randomUUID(), id, UUID.randomUUID().toString(),
							new Version(1L),
							asList(new CustomerCreated(id, command.getName())),
							LocalDateTime.now());

			final List<UnitOfWork> expectedHistory = Lists.newArrayList(newUow);

			when(dao.getAll(id)).thenReturn(expectedHistory);

			final StateTransitionsTracker<Customer> tracker = new StateTransitionsTracker<>(expectedInstance,
							writeModelStateTransitionFn, dependencyInjectionFn);

			final Stack1SnapshotReader<Customer> reader = new Stack1SnapshotReader<>(cache, dao);

			cache.put(id, expectedHistory);

			assertThat(reader.getSnapshot(id, tracker)).isEqualTo(expectedSnapshot);

			verify(dao).getAllAfterVersion(eq(id), eq(expectedSnapshot.getVersion()));

			verifyNoMoreInteractions(dao);

    }

    @Test
    public void on_both_cache_and_db_then_hits_db_to_compose_history() {

			final String id = "customer#1";
			final String name =  "customer#1 name";
			final String reason = "because yes";

			final Version cachedVersion = new Version(1L);
			final Version expectedVersion = new Version(2L);
			final LocalDateTime activated_on = LocalDateTime.now();

			final Customer expectedInstance =
							new Customer(id, name, true, activated_on, null, reason);

			final SnapshotReader.Snapshot<Customer> expectedSnapshot =
							new SnapshotReader.Snapshot<>(expectedInstance, expectedVersion);

			// cached history
			final CreateCustomerCmd command1 = new CreateCustomerCmd(name);
			final UnitOfWork newUow = new UnitOfWork(UUID.randomUUID(), id, UUID.randomUUID().toString(),
							cachedVersion,
							asList(new CustomerCreated(id, command1.getName())),
							LocalDateTime.now());
			final List<UnitOfWork> cachedHistory = Lists.newArrayList(newUow);

			// non cached history (on db)
			final ActivateCustomerCmd command2 = new ActivateCustomerCmd(reason);
			final UnitOfWork uow2 = new UnitOfWork(UUID.randomUUID(), id, UUID.randomUUID().toString(),
							expectedVersion,
							asList(new CustomerActivated(reason, activated_on)),
							activated_on);
			List<UnitOfWork> nonCachedHistory = Lists.newArrayList(uow2);

			// prepare

			when(dao.getAll(id)).thenReturn(cachedHistory);
			when(dao.getAllAfterVersion(id, cachedVersion)).thenReturn(nonCachedHistory);

			final StateTransitionsTracker<Customer> tracker = new StateTransitionsTracker<>(expectedInstance,
							writeModelStateTransitionFn, dependencyInjectionFn);

			final Stack1SnapshotReader<Customer> reader = new Stack1SnapshotReader<>(cache, dao);

			cache.put(id, cachedHistory);

			assertThat(reader.getSnapshot(id, tracker)).isEqualTo(expectedSnapshot);

			verify(dao).getAllAfterVersion(eq(id), eq(cachedVersion));

			verifyNoMoreInteractions(dao);

    }

}

