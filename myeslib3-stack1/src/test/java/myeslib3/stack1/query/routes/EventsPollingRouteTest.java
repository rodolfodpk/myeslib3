package myeslib3.stack1.query.routes;

import javaslang.Tuple;
import javaslang.Tuple3;
import javaslang.collection.List;
import lombok.val;
import myeslib3.core.data.Event;
import myeslib3.example1.aggregates.customer.CustomerId;
import myeslib3.example1.aggregates.customer.commands.CreateCustomerCmd;
import myeslib3.example1.aggregates.customer.events.CustomerCreated;
import myeslib3.stack1.Stack1Config;
import myeslib3.stack1.command.WriteModelRepository;
import myeslib3.stack1.stack1infra.BoundedContextConfig;
import org.aeonbits.owner.ConfigFactory;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class EventsPollingRouteTest extends CamelTestSupport {

  final static String eventsChannelId = "channelExample1";

  @Produce(uri = "direct:start")
  protected ProducerTemplate template;

  @EndpointInject(uri = "mock:result")
  protected MockEndpoint resultEndpoint;

  final BoundedContextConfig config = ConfigFactory.create(Stack1Config.class, new HashMap<>());

  @Before
  public void init() throws Exception {
  }

  @Test
  public void single_events_list() throws Exception {

    val repoMock = mock(WriteModelRepository.class, withSettings().verboseLogging());
    val route = new EventsPollingRoute(eventsChannelId, repoMock, config);
    context.addRoutes(route);

    val uowId = "uow#1";
    val aggregateRootId = "id-1";
    val cmd1 = new CreateCustomerCmd(UUID.randomUUID(), new CustomerId("c1"), "customer1");
    val event1 = new CustomerCreated(cmd1.getTargetId(), cmd1.getName());

    final List<Tuple3<String, String, List<Event>>> tuplesList =
            List.of(Tuple.of(uowId, aggregateRootId, List.of(event1)));

    when(repoMock.getLastUowSequence()).thenReturn(0L);

    when(repoMock.getAllSince(eq(0L), eq(config.events_max_rows_query()))).thenReturn(tuplesList);

    resultEndpoint.expectedBodiesReceived(tuplesList);

    template.sendBody(tuplesList);

    resultEndpoint.assertIsSatisfied(1000);

    verify(repoMock).getLastUowSequence();

    verify(repoMock).getAllSince(eq(0L), eq(config.events_max_rows_query()));

    verifyNoMoreInteractions(repoMock);

  }

  @Override
  protected RouteBuilder createRouteBuilder() throws Exception {

    return new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        from("direct:start")
                .toF("direct:pool-events-%s", eventsChannelId)
                .log("*** from pooling: ${header.aggregate_root_id} - ${body}")
                .to("mock:result");
      }
    };

  }

}