package myeslib3.stack1.query.routes;

import com.google.inject.Guice;
import com.google.inject.Injector;
import javaslang.collection.List;
import lombok.val;
import myeslib3.example1.aggregates.customer.CustomerId;
import myeslib3.example1.aggregates.customer.commands.CreateCustomerCmd;
import myeslib3.example1.aggregates.customer.events.CustomerCreated;
import myeslib3.stack1.command.UnitOfWorkData;
import myeslib3.stack1.query.EventsProjectorDao;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.processor.idempotent.MemoryIdempotentRepository;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class EventsProjectionRouteTest extends CamelTestSupport {

  final static String eventsChannelId = "channelExample1";

  static final Injector injector = Guice.createInjector();

  @Produce(uri = "direct:start")
  ProducerTemplate template;

  @EndpointInject(uri = "mock:result")
  MockEndpoint resultEndpoint;

  @Before
  public void init() throws Exception {
    injector.injectMembers(this);
  }

  @Test
  public void single_events_list() throws Exception {

    long completionInterval = 100;
    int completionSize = 1;

    val eventsProjectorMock = mock(EventsProjectorDao.class, withSettings().verboseLogging());
    when(eventsProjectorMock.getEventsChannelId()).thenReturn(eventsChannelId);
    val route = new EventsProjectionRoute(eventsProjectorMock , new MemoryIdempotentRepository(),
            false, completionInterval, completionSize);
    context.addRoutes(route);

    val uowId = "uow#1";
    val aggregateRootId = "id-1";
    val cmd1 = new CreateCustomerCmd(UUID.randomUUID(), new CustomerId("c1"), "customer1");
    val event1 = new CustomerCreated(cmd1.getTargetId(), cmd1.getName());

    final List<UnitOfWorkData> tuplesList =
            List.of(new UnitOfWorkData(uowId, 1L, aggregateRootId, List.of(event1)));

    resultEndpoint.expectedBodiesReceived(tuplesList.get(0));

    template.requestBody(tuplesList.get(0));

    resultEndpoint.assertIsSatisfied(1000);

    verify(eventsProjectorMock).handle(eq(tuplesList));

    verify(eventsProjectorMock, times(2)).getEventsChannelId();

    verifyNoMoreInteractions(eventsProjectorMock );

  }

  @Override
  protected RouteBuilder createRouteBuilder() throws Exception {

    return new RouteBuilder() {
      @Override
      public void configure() throws Exception {
      from("direct:start")
        .toF("seda:%s-events?multipleConsumers=%b", eventsChannelId, false)
        .log("*** from seda: ${body}")
        .to("mock:result");
      }
    };

  }

}