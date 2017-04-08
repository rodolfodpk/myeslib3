package myeslib3.stack1.command.routes;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import myeslib3.core.data.Command;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;
import myeslib3.core.functions.CommandHandlerFn;
import myeslib3.core.functions.DependencyInjectionFn;
import myeslib3.core.functions.StateTransitionFn;
import myeslib3.example1.core.aggregates.customer.*;
import myeslib3.examples.example1.runtime.CustomerModule;
import myeslib3.stack1.command.SnapshotReader;
import myeslib3.stack1.command.WriteModelRepository;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.com.github.benmanes.caffeine.cache.Cache;
import org.apache.camel.com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultProducerTemplate;
import org.apache.camel.processor.idempotent.MemoryIdempotentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class CommandPostSyncRouteTest {

	static final Injector injector = Guice.createInjector(new CustomerModule());
  static final DefaultCamelContext context = new DefaultCamelContext();

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
	WriteModelRepository writeModelRepository;


	@BeforeEach
	public void init() throws Exception {
		injector.injectMembers(this);
		MockitoAnnotations.initMocks(this);
		when(snapshotReader.getSnapshot(anyString(), any()))
						.thenReturn(new SnapshotReader.Snapshot<>(supplier.get(), new Version(0)));
		final CommandPostSyncRoute<Customer, CustomerCommand> route =
						new CommandPostSyncRoute<>(Customer.class, commandsList(),
										commandHandlerFn, supplier, dependencyInjectionFn, stateTransitionFn,
										snapshotReader, writeModelRepository, gson, new MemoryIdempotentRepository());
		context.addRoutes(route);
		context.start();
		//Thread.sleep(10000);
	}

	@AfterEach
	public void afterRun() throws Exception {
		context.stop();
	}

	@Test
	public void test1() {

    ProducerTemplate p = new DefaultProducerTemplate(context);

    CustomerCommand c = new CreateCustomerCmd("customer1");

    String asJson =  gson.toJson(c, Command.class);

    p.sendBody("direct:handle-customer-create_customer_cmd", asJson);


	}

	List<Class<?>> commandsList() {

		return Arrays.asList(CreateCustomerCmd.class, ActivateCustomerCmd.class,
						DeactivateCustomerCmd.class, CreateActivatedCustomerCmd.class);

	}

}
