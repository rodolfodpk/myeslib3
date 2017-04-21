package myeslib3.stack1.command.routes;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.val;
import myeslib3.core.data.Command;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;
import myeslib3.example1.Example1Module;
import myeslib3.example1.aggregates.customer.Customer;
import myeslib3.example1.aggregates.customer.CustomerCmdHandler;
import myeslib3.example1.aggregates.customer.CustomerId;
import myeslib3.example1.aggregates.customer.CustomerModule;
import myeslib3.example1.aggregates.customer.commands.CreateCustomerCmd;
import myeslib3.example1.aggregates.customer.events.CustomerCreated;
import myeslib3.stack1.command.Snapshot;
import myeslib3.stack1.command.SnapshotReader;
import myeslib3.stack1.command.WriteModelRepository;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.processor.idempotent.MemoryIdempotentRepository;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class CommandSyncRouteTest extends CamelTestSupport {

	static final Injector injector = Guice.createInjector(new CustomerModule(), new Example1Module());

  @EndpointInject(uri = "mock:result")
  protected MockEndpoint resultEndpoint;

  @Produce(uri = "direct:start")
  protected ProducerTemplate template;

  @Inject
	Supplier<Customer> supplier;
	@Inject
  CustomerCmdHandler commandHandlerFn;
	@Inject
	Gson gson;

	@Mock
	SnapshotReader<CustomerId, Customer> snapshotReader;
	@Mock
	WriteModelRepository<CustomerId> writeModelRepository;

	@Before
	public void init() throws Exception {
    injector.injectMembers(this);
    MockitoAnnotations.initMocks(this);
    val route = new CommandSyncRoute<>(Customer.class, snapshotReader, commandHandlerFn, writeModelRepository, gson,
            new MemoryIdempotentRepository());
    context.addRoutes(route);
	}

	@After
	public void afterRun() throws Exception {
	}

	@Test
	public void vald_command_must_return_valid_unit_of_work() throws InterruptedException {

    val customerId = new CustomerId("customer#1");

    when(snapshotReader.getSnapshot(eq(customerId)))
            .thenReturn(new Snapshot<>(supplier.get(), new Version(0)));

    val createCustomerCmd = new CreateCustomerCmd(UUID.randomUUID(), customerId, "customer1");

    val asJson = gson.toJson(createCustomerCmd, Command.class);

    template.requestBody(asJson);

    verify(snapshotReader).getSnapshot(eq(customerId));

    val expectedEvent = new CustomerCreated(customerId, "customer1");
    val expectedUow = UnitOfWork.create(createCustomerCmd, new Version(1), Arrays.asList(expectedEvent));

    ArgumentCaptor<UnitOfWork> argument = ArgumentCaptor.forClass(UnitOfWork.class);

    verify(writeModelRepository).append(argument.capture());

    val resultingUow = argument.getValue();

    assertEquals(resultingUow.getCommand(), expectedUow.getCommand());
    assertEquals(resultingUow.getEvents(), expectedUow.getEvents());
    assertEquals(resultingUow.getVersion(), expectedUow.getVersion());

    verifyNoMoreInteractions(snapshotReader, writeModelRepository);

    val expectedBody = gson.toJson(resultingUow);

    resultEndpoint.expectedMessageCount(1);

    val receivedBody = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);

    assertEquals(expectedBody, receivedBody);

    resultEndpoint.assertIsSatisfied();

  }

  @Test
  public void invalid_command_must_return_error() throws InterruptedException {

    val customerId = new CustomerId("customer#1");
    val currentCustomer = Customer.of(customerId, "customer1", false, null);

    when(snapshotReader.getSnapshot(eq(customerId)))
            .thenReturn(new Snapshot<>(currentCustomer, new Version(1)));

    val createCustomerCmd = new CreateCustomerCmd(UUID.randomUUID(), customerId, "customer1");

    val asJson = gson.toJson(createCustomerCmd, Command.class);

    template.requestBody(asJson);

    verify(snapshotReader).getSnapshot(eq(customerId));

    verifyNoMoreInteractions(snapshotReader, writeModelRepository);

    val expectedBody = gson.toJson(Arrays.asList("customer already created"), List.class);

    resultEndpoint.expectedMessageCount(1);

    val receivedBody = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);

    assertEquals(expectedBody, receivedBody);

    resultEndpoint.assertIsSatisfied();

  }

  @Override
  protected RouteBuilder createRouteBuilder() {
    return new RouteBuilder() {
      public void configure() {
        from("direct:start")
                .streamCaching()
        .to("direct:handle-cmd-customer")
//        .log("** final result ${body}")
        .to("mock:result");
      }
    };
  }

}
