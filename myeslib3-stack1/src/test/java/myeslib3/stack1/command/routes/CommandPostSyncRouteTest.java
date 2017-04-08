package myeslib3.stack1.command.routes;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.functions.CommandHandlerFn;
import myeslib3.core.functions.DependencyInjectionFn;
import myeslib3.core.functions.StateTransitionFn;
import myeslib3.example1.core.aggregates.customer.*;
import myeslib3.examples.example1.runtime.CustomerModule;
import myeslib3.stack1.command.SnapshotReader;
import myeslib3.stack1.command.WriteModelRepository;
import org.apache.camel.com.github.benmanes.caffeine.cache.Cache;
import org.apache.camel.com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.processor.idempotent.MemoryIdempotentRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class CommandPostSyncRouteTest {

	@Inject
	Supplier<Customer> supplier;
	@Inject
	DependencyInjectionFn<Customer> dependencyInjectionFn;
	@Inject
  StateTransitionFn<Customer> stateTransitionFn;
	@Inject
	CommandHandlerFn<Customer, CustomerCommand> commandHandlerFn;
	@Inject
	Gson gson;

	@Mock
	SnapshotReader<Customer> snapshotReader;
	@Mock
	WriteModelRepository dao;

	Cache<String, List<UnitOfWork>> cache;
	DefaultCamelContext context;

	@Before
	public void init() throws Exception {
		cache = Caffeine.newBuilder().build();
		context = new DefaultCamelContext();
		Injector injector = Guice.createInjector(new CustomerModule());
		injector.injectMembers(this);
		MockitoAnnotations.initMocks(this);
		CommandPostSyncRoute<Customer, CustomerCommand> route =
						new CommandPostSyncRoute<>(Customer.class, commandsList(),
										commandHandlerFn, supplier, dependencyInjectionFn, stateTransitionFn,
										snapshotReader, dao, gson, new MemoryIdempotentRepository());

		context.start();
	}

	@AfterEach
	public void afterRun() throws Exception {
		context.stop();
	}

	@Test
	public void test1() {
		// TODO Retrofit client for apply Customer commands
	}

	List<Class<?>> commandsList() {

		return Arrays.asList(CreateCustomerCmd.class, ActivateCustomerCmd.class,
						DeactivateCustomerCmd.class, CreateActivatedCustomerCmd.class);

	}

}
