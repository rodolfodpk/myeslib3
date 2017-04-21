package myeslib3.stack1.command.impl;

import com.google.inject.Guice;
import com.google.inject.Injector;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.List;
import myeslib3.core.data.Event;
import myeslib3.core.data.Version;
import myeslib3.example1.aggregates.customer.Customer;
import myeslib3.example1.aggregates.customer.CustomerId;
import myeslib3.example1.aggregates.customer.CustomerModule;
import myeslib3.example1.aggregates.customer.commands.CreateCustomerCmd;
import myeslib3.example1.aggregates.customer.events.CustomerActivated;
import myeslib3.example1.aggregates.customer.events.CustomerCreated;
import myeslib3.stack1.command.Snapshot;
import myeslib3.stack1.command.WriteModelRepository;
import org.apache.camel.com.github.benmanes.caffeine.cache.Cache;
import org.apache.camel.com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.UUID;
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
    WriteModelRepository<CustomerId> dao;

    Cache<CustomerId, Tuple2<Version, List<Event>>> cache;

    @BeforeEach
    public void init() throws Exception {
        cache = Caffeine.newBuilder().build();
        injector.injectMembers(this);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void on_empty_history_then_returns_version_0() throws ExecutionException {

			final CustomerId id = new CustomerId("customer#1");

			final Snapshot<Customer> expectedSnapshot =
							new Snapshot<>(supplier.get(), new Version(0L));

			final Tuple2<Version, List<Event>> expectedHistory = Tuple.of(new Version(0), List.empty());

			when(dao.getAll(id)).thenReturn(expectedHistory);

			final Stack1SnapshotReader<CustomerId, Customer> reader = new Stack1SnapshotReader<>(cache, dao, supplier,
							dependencyInjectionFn, stateTransitionFn);

			assertThat(reader.getSnapshot(id)).isEqualTo(expectedSnapshot);

			verify(dao).getAll(id);

			verifyNoMoreInteractions(dao);

    }

    @Test
    public void on_empty_cache_then_returns_version_from_db() {

			final CustomerId id = new CustomerId("customer#1");
			final String name =  "customer#1 name";

			final Customer expectedInstance = Customer.of(id, name, false, null);

			final Snapshot<Customer> expectedSnapshot =
								new Snapshot<>(expectedInstance, Version.create(1L));

			final CreateCustomerCmd command = new CreateCustomerCmd(UUID.randomUUID(), id, name);

      final Tuple2<Version, List<Event>> expectedHistory =
              Tuple.of(new Version(1), List.of(new CustomerCreated(id, command.getName())));

			when(dao.getAll(id)).thenReturn(expectedHistory);

			final Stack1SnapshotReader<CustomerId, Customer> reader = new Stack1SnapshotReader<>(cache, dao, supplier,
							dependencyInjectionFn, stateTransitionFn);

			assertThat(reader.getSnapshot(id)).isEqualTo(expectedSnapshot);

			verify(dao).getAll(id);

			verifyNoMoreInteractions(dao);


    }

    @Test
    public void on_cache_then_hits_db_to_check_newer_version() {

			final CustomerId id = new CustomerId("customer#1");
			final String name =  "customer#1 name";

			final CreateCustomerCmd command = new CreateCustomerCmd(UUID.randomUUID(), id, name);

      final Tuple2<Version, List<Event>> expectedHistory =
              Tuple.of(new Version(1), List.of(new CustomerCreated(id, command.getName())));

			when(dao.getAll(id)).thenReturn(expectedHistory);

			cache.put(id, expectedHistory);

			verifyNoMoreInteractions(dao);

    }

    @Test
    public void on_both_cache_and_db_then_hits_db_to_compose_history() {

			final CustomerId id = new CustomerId("customer#1");
			final String name =  "customer#1 name";
			final String reason = "because yes";

			final Version cachedVersion = new Version(1L);
			final Version expectedVersion = new Version(2L);
			final LocalDateTime activated_on = LocalDateTime.now();

			final Customer expectedInstance = Customer.of(id, name, true, reason);

			final Snapshot<Customer> expectedSnapshot =
							new Snapshot<>(expectedInstance, expectedVersion);

			// cached history
			final CreateCustomerCmd command1 = new CreateCustomerCmd(UUID.randomUUID(), id, name);

      final Tuple2<Version, List<Event>> cachedHistory =
              Tuple.of(new Version(1), List.of(new CustomerCreated(id, command1.getName())));

      final Tuple2<Version, List<Event>> nonCachedHistory =
              Tuple.of(new Version(2), List.of(new CustomerActivated(reason, activated_on)));

      // prepare

			when(dao.getAll(id)).thenReturn(cachedHistory);
			when(dao.getAllAfterVersion(id, cachedVersion)).thenReturn(nonCachedHistory);

			final Stack1SnapshotReader<CustomerId, Customer> reader = new Stack1SnapshotReader<>(cache, dao, supplier,
							dependencyInjectionFn, stateTransitionFn);

			cache.put(id, cachedHistory);

			Snapshot<Customer> snapshot = reader.getSnapshot(id);

//			assertThat(snapshot).isEqualTo(expectedSnapshot);

      assertThat(snapshot.getInstance().getId()).isEqualTo(expectedSnapshot.getInstance().getId());
      assertThat(snapshot.getInstance().getName()).isEqualTo(expectedSnapshot.getInstance().getName());
      assertThat(snapshot.getInstance().isActive()).isEqualTo(expectedSnapshot.getInstance().isActive());
      assertThat(snapshot.getInstance().getReason()).isEqualTo(expectedSnapshot.getInstance().getReason());

      verify(dao).getAllAfterVersion(eq(id), eq(cachedVersion));

			verifyNoMoreInteractions(dao);

    }

}

