package myeslib3.stack1.query.routes;

import javaslang.Tuple;
import javaslang.Tuple3;
import javaslang.collection.List;
import lombok.val;
import myeslib3.core.data.Event;
import myeslib3.example1.aggregates.customer.CustomerId;
import myeslib3.example1.aggregates.customer.commands.CreateCustomerCmd;
import myeslib3.example1.aggregates.customer.events.CustomerCreated;
import myeslib3.stack1.command.WriteModelRepository;
import myeslib3.stack1.stack1infra.BoundedContextConfig;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class EventsPollingRouteTest extends CamelTestSupport {

  final static String eventsChannelId = "channelExample1";

  @Produce(uri = "direct:start")
  protected ProducerTemplate template;

  @EndpointInject(uri = "mock:result")
  protected MockEndpoint resultEndpoint;

  @Mock
  BoundedContextConfig config;

  @Before
  public void init() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(config.events_max_rows_query()).thenReturn(10);
    when(config.events_backoff_failures_threshold()).thenReturn(3);
    when(config.events_backoff_iddle_threshold()).thenReturn(3);
    when(config.events_backoff_multiplier()).thenReturn(2);
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

    when(repoMock.getAllSince(eq(0L), eq(10))).thenReturn(tuplesList);

    resultEndpoint.expectedBodiesReceived(tuplesList);

    template.sendBody(true);

    resultEndpoint.assertIsSatisfied(100);

    verify(repoMock).getLastUowSequence();

    verify(repoMock).getAllSince(eq(0L), eq(10));

    verifyNoMoreInteractions(repoMock);

  }

  @Test
  public void when_failure_it_must_increase_failures() throws Exception {

    val repoMock = mock(WriteModelRepository.class); // , withSettings().verboseLogging());
    val route = new EventsPollingRoute(eventsChannelId, repoMock, config);
    context.addRoutes(route);

    when(repoMock.getLastUowSequence())
            .thenThrow(new RuntimeException("fail 1"), new RuntimeException("fail 2"), new RuntimeException("fail 2"))
            ;

    template.requestBody(true);

    verify(repoMock).getLastUowSequence();

    AssertionsForInterfaceTypes.assertThat(route.getFailures().get()).isEqualTo(1);

    verifyNoMoreInteractions(repoMock);

  }

  @Test
  public void when_idle_it_must_increase_idles() throws Exception {

    val repoMock = mock(WriteModelRepository.class); // , withSettings().verboseLogging());
    val route = new EventsPollingRoute(eventsChannelId, repoMock, config);
    context.addRoutes(route);

    when(repoMock.getLastUowSequence()).thenReturn(0L);

    when(repoMock.getAllSince(eq(0L), eq(10))).thenReturn(List.empty());

    template.requestBody(true);

    verify(repoMock).getLastUowSequence();

    verify(repoMock).getAllSince(eq(0L), eq(10));

    AssertionsForInterfaceTypes.assertThat(route.getIdles().get()).isEqualTo(1);

    verifyNoMoreInteractions(repoMock);

  }

  @Test
  public void after_3_failures_it_must_skip3_pools() throws Exception {

    val repoMock = mock(WriteModelRepository.class); //, withSettings().verboseLogging());
    val route = new EventsPollingRoute(eventsChannelId, repoMock, config);
    context.addRoutes(route);

    val uowId = "uow#1";
    val aggregateRootId = "id-1";
    val cmd1 = new CreateCustomerCmd(UUID.randomUUID(), new CustomerId("c1"), "customer1");
    val event1 = new CustomerCreated(cmd1.getTargetId(), cmd1.getName());

    final List<Tuple3<String, String, List<Event>>> tuplesList =
            List.of(Tuple.of(uowId, aggregateRootId, List.of(event1)));

    when(repoMock.getLastUowSequence()).thenReturn(0L, 0L, 0L, 0L, 0L, 0L, 0L);

    when(repoMock.getAllSince(eq(0L), eq(10)))
      .thenThrow(new RuntimeException("fail 1"), new RuntimeException("fail 2"), new RuntimeException("fail 2"))
      .thenReturn(tuplesList)

    ;

    resultEndpoint.expectedBodiesReceived(1, 2, 3, tuplesList);

    // force circuit breaker to open
    template.sendBody(1);
    template.sendBody(2);
    template.sendBody(3);

    // retry and skip 2 times and close the circuit breaker
    template.sendBody(4);
    template.sendBody(5);

    // this should work
    template.sendBody(6);

    resultEndpoint.assertIsSatisfied(1000);

    verify(repoMock, times(4)).getLastUowSequence();

    verify(repoMock, times(4)).getAllSince(eq(0L), eq(10));

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