package myeslib3.stack1.command.routes;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import myeslib3.core.data.Command;
import myeslib3.core.data.Event;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;
import myeslib3.example1.aggregates.customer.Customer;
import myeslib3.example1.aggregates.customer.CustomerCmdHandler;
import myeslib3.example1.aggregates.customer.CustomerModule;
import myeslib3.example1.aggregates.customer.commands.ActivateCustomerCmd;
import myeslib3.example1.aggregates.customer.commands.CreateActivateCustomerCmd;
import myeslib3.example1.aggregates.customer.commands.CreateCustomerCmd;
import myeslib3.example1.aggregates.customer.commands.DeactivateCustomerCmd;
import myeslib3.example1.aggregates.customer.events.CustomerCreated;
import myeslib3.stack1.command.SnapshotReader;
import myeslib3.stack1.command.WriteModelRepository;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.processor.idempotent.MemoryIdempotentRepository;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.inject.Inject;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static myeslib3.stack1.Headers.AGGREGATE_ROOT_ID;
import static myeslib3.stack1.Headers.COMMAND_ID;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class CommandSyncRouteTest extends CamelTestSupport {

	static final Injector injector = Guice.createInjector(new CustomerModule());
  static final DefaultCamelContext context = new DefaultCamelContext();

  @Produce(uri = "direct://handle-create_customer_cmd")
  protected ProducerTemplate template;

  @Inject
	Supplier<Customer> supplier;
	@Inject
	Function<Customer, Customer> dependencyInjectionFn;
	@Inject
  BiFunction<Event, Customer, Customer> stateTransitionFn;
	@Inject
  CustomerCmdHandler commandHandlerFn;
	@Inject
	Gson gson;

	@Mock
	SnapshotReader<Customer> snapshotReader;
	@Mock
	WriteModelRepository writeModelRepository;

	@Before
	public void init() throws Exception {
	}

	@After
	public void afterRun() throws Exception {
	}

	@Test
	public void test1() {

    String customerId = "1";
	  String commandId = "1";

    when(snapshotReader.getSnapshot(anyString()))
            .thenReturn(new SnapshotReader.Snapshot<>(supplier.get(), new Version(0)));

    CreateCustomerCmd c = new CreateCustomerCmd(UUID.randomUUID(), "c1", "customer1");

    String asJson =  gson.toJson(c, Command.class);

    Map<String, Object> headers = new HashMap<>();
    headers.put(AGGREGATE_ROOT_ID, customerId);
    headers.put(COMMAND_ID, commandId);

    template.requestBodyAndHeaders(asJson, headers);

    verify(snapshotReader).getSnapshot(eq(customerId));

    CustomerCreated expectedEvent = new CustomerCreated(customerId, "customer1");
    UnitOfWork result = UnitOfWork.create(c, new Version(1), Arrays.asList(expectedEvent));

    ArgumentCaptor<UnitOfWork> argument = ArgumentCaptor.forClass(UnitOfWork.class);

    verify(writeModelRepository).append(argument.capture());

    assertEquals(argument.getValue().getCommand(), result.getCommand());
    assertEquals(argument.getValue().getEvents(), result.getEvents());
    assertEquals(argument.getValue().getVersion(), result.getVersion());

    verifyNoMoreInteractions(snapshotReader, writeModelRepository);

    //System.out.println(uowAsJson);
  }

  @Override
  protected RouteBuilder createRouteBuilder() {
    injector.injectMembers(this);
    MockitoAnnotations.initMocks(this);
    final CommandSyncRoute<Customer> route =
            new CommandSyncRoute<>(Customer.class, snapshotReader, commandHandlerFn, writeModelRepository, gson,
                    new MemoryIdempotentRepository());
    return route;
  }

	List<Class<?>> commandsList() {

		return Arrays.asList(CreateCustomerCmd.class, ActivateCustomerCmd.class,
						DeactivateCustomerCmd.class, CreateActivateCustomerCmd.class);

	}

}
