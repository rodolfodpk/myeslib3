package myeslib3.stack1.query.routes;

import com.google.inject.Guice;
import com.google.inject.Injector;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.List;
import lombok.val;
import myeslib3.core.data.Event;
import myeslib3.example1.aggregates.customer.CustomerId;
import myeslib3.example1.aggregates.customer.commands.CreateCustomerCmd;
import myeslib3.example1.aggregates.customer.events.CustomerCreated;
import myeslib3.stack1.Headers;
import myeslib3.stack1.query.EventsProjectorDao;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.mockito.Mockito.verify;

public class EventsMultiProjectionRouteTest extends CamelTestSupport {

  final static String eventsChannelId = "channelExample1";

  static final Injector injector = Guice.createInjector();

  @Produce(uri = "direct:start")
  protected ProducerTemplate template;

  @EndpointInject(uri = "mock:result")
  protected MockEndpoint resultEndpoint;

  @Mock
  EventsProjectorDao dao;

  @Before
  public void init() throws Exception {
    injector.injectMembers(this);
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Disabled
  public void two_aggregates_events() throws Exception {

    val route = new EventsMultiProjectionRoute(eventsChannelId, dao, 10, 10, false);
    context.addRoutes(route);

    val aggregateRootId1 = "id-1";
    val cmd1 = new CreateCustomerCmd(UUID.randomUUID(), new CustomerId("c1"), "customer1");
    val event1 = new CustomerCreated(cmd1.getTargetId(), cmd1.getName());

    val aggregateRootId2 = "id-2";
    val cmd2 = new CreateCustomerCmd(UUID.randomUUID(), new CustomerId("c2"), "customer2");
    val event2 = new CustomerCreated(cmd2.getTargetId(), cmd2.getName());

    final List<Tuple2<String, List<Event>>> tuplesList =
            List.of(Tuple.of(aggregateRootId1, List.of(event1)), Tuple.of(aggregateRootId2, List.of(event2)));

    template.requestBody(tuplesList);

    Thread.sleep(50000);

//    resultEndpoint.assertExchangeReceived(2);

    resultEndpoint.assertIsSatisfied(10000);

    assertEquals(List.of(event1), resultEndpoint.getExchanges().get(0).getIn().getBody());
    assertEquals(aggregateRootId1, resultEndpoint.getExchanges().get(0).getIn().getHeader(Headers.AGGREGATE_ROOT_ID));

    assertEquals(List.of(event2), resultEndpoint.getExchanges().get(1).getIn().getBody());
    assertEquals(aggregateRootId2, resultEndpoint.getExchanges().get(1).getIn().getHeader(Headers.AGGREGATE_ROOT_ID));

    verify(dao).handle(aggregateRootId1, List.of(event1));
    verify(dao).handle(aggregateRootId2, List.of(event2));

  }

  @Override
  protected RouteBuilder createRouteBuilder() throws Exception {

    return new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        from("direct:start")
          .log("*** to seda: ${header.aggregate_root_id} - ${body}")
          .toF("seda:%s-events?multipleConsumers=%b", eventsChannelId, false)
          .log("*** from seda: ${header.aggregate_root_id} - ${body}")
          .to("mock:result");
      }
    };

  }

}