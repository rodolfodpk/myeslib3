package myeslib3.stack1.command.impl;

import com.google.inject.Guice;
import com.google.inject.Injector;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.List;
import myeslib3.core.data.Event;
import myeslib3.core.data.Version;
import myeslib3.example1.aggregates.customer.Customer;
import myeslib3.example1.aggregates.customer.CustomerModule;
import myeslib3.stack1.command.SnapshotReader;
import myeslib3.stack1.command.WriteModelRepository;
import org.apache.camel.com.github.benmanes.caffeine.cache.Cache;
import org.apache.camel.com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.inject.Inject;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("A SnapshotReader")
public class Stack1SnapshotReaderTest {

	final Injector injector = Guice.createInjector(new CustomerModule());

    @Inject
    Supplier<Customer> supplier;
    @Inject
		Function<Customer, Customer> dependencyInjectionFn;
    @Inject
		BiFunction<Event, Customer, Customer> stateTransitionFn;

    @Mock
    WriteModelRepository dao;

    Cache<String, Tuple2<Version, List<Event>>> cache;

    @BeforeEach
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

			final Tuple2<Version, List<Event>> expectedHistory = Tuple.of(new Version(0), List.empty());

			when(dao.getAll(id)).thenReturn(expectedHistory);

			final Stack1SnapshotReader<Customer> reader = new Stack1SnapshotReader<>(cache, dao, supplier,
							dependencyInjectionFn, stateTransitionFn);

			assertThat(reader.getSnapshot(id)).isEqualTo(expectedSnapshot);

			verify(dao).getAll(id);

			verifyNoMoreInteractions(dao);

    }
//
//    @Test
//    public void on_empty_cache_then_returns_version_from_db() {
//
//			final String id = "customer#1";
//			final String name =  "customer#1 name";
//
//			final Customer expectedInstance = new Customer(id, name, false, null, null, null);
//
//			final SnapshotReader.Snapshot<Customer> expectedSnapshot =
//								new SnapshotReader.Snapshot<>(expectedInstance, Version.create(1L));
//
//			final CreateCustomerCmd command = new CreateCustomerCmd(name);
//
//			final UnitOfWork newUow = new UnitOfWork(UUID.randomUUID(), id,
//																				new Version(1L),
//																				asList(new CustomerCreated(id, command.getName())),
//																				LocalDateTime.now());
//
//			final List<UnitOfWork> expectedHistory = Lists.newArrayList(newUow);
//
//			when(dao.getAll(id)).thenReturn(expectedHistory);
//
//			final Stack1SnapshotReader<Customer> reader = new Stack1SnapshotReader<>(cache, dao, supplier,
//							dependencyInjectionFn, stateTransitionFn);
//
//			assertThat(reader.getSnapshot(id)).isEqualTo(expectedSnapshot);
//
//			verify(dao).getAll(id);
//
//			verifyNoMoreInteractions(dao);
//
//
//    }
//
//    @Test
//    public void on_cache_then_hits_db_to_check_newer_version() {
//
//			final String id = "customer#1";
//			final String name =  "customer#1 name";
//
//			final CreateCustomerCmd command = new CreateCustomerCmd(name);
//
//			final UnitOfWork newUow = new UnitOfWork(UUID.randomUUID(), id,
//							new Version(1L),
//							asList(new CustomerCreated(id, command.getName())),
//							LocalDateTime.now());
//
//			final List<UnitOfWork> expectedHistory = Lists.newArrayList(newUow);
//
//			when(dao.getAll(id)).thenReturn(expectedHistory);
//
//			cache.put(id, expectedHistory);
//
//			verifyNoMoreInteractions(dao);
//
//    }
//
//    @Test
//    public void on_both_cache_and_db_then_hits_db_to_compose_history() {
//
//			final String id = "customer#1";
//			final String name =  "customer#1 name";
//			final String reason = "because yes";
//
//			final Version cachedVersion = new Version(1L);
//			final Version expectedVersion = new Version(2L);
//			final LocalDateTime activated_on = LocalDateTime.now();
//
//			final Customer expectedInstance =
//							new Customer(id, name, true, activated_on, null, reason);
//
//			final SnapshotReader.Snapshot<Customer> expectedSnapshot =
//							new SnapshotReader.Snapshot<>(expectedInstance, expectedVersion);
//
//			// cached history
//			final CreateCustomerCmd command1 = new CreateCustomerCmd(name);
//			final UnitOfWork newUow = new UnitOfWork(UUID.randomUUID(), id,
//							cachedVersion,
//							asList(new CustomerCreated(id, command1.getName())),
//							LocalDateTime.now());
//			final List<UnitOfWork> cachedHistory = Lists.newArrayList(newUow);
//
//			// non cached history (on db)
//			final UnitOfWork uow2 = new UnitOfWork(UUID.randomUUID(), id,
//							expectedVersion,
//							asList(new CustomerActivated(reason, activated_on)),
//							activated_on);
//			final List<UnitOfWork> nonCachedHistory = Lists.newArrayList(uow2);
//
//			// prepare
//
//			when(dao.getAll(id)).thenReturn(cachedHistory);
//			when(dao.getAllAfterVersion(id, cachedVersion)).thenReturn(nonCachedHistory);
//
//			final Stack1SnapshotReader<Customer> reader = new Stack1SnapshotReader<>(cache, dao, supplier,
//							dependencyInjectionFn, stateTransitionFn);
//
//			cache.put(id, cachedHistory);
//
//			assertThat(reader.getSnapshot(id)).isEqualTo(expectedSnapshot);
//
//			verify(dao).getAllAfterVersion(eq(id), eq(cachedVersion));
//
//			verifyNoMoreInteractions(dao);
//
//    }

}

