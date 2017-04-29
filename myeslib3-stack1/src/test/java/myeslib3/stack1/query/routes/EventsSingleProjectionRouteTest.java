package myeslib3.stack1.query.routes;

import com.google.inject.Guice;
import com.google.inject.Injector;
import javaslang.Tuple;
import javaslang.Tuple3;
import javaslang.collection.List;
import lombok.val;
import myeslib3.core.data.Event;
import myeslib3.example1.aggregates.customer.CustomerId;
import myeslib3.example1.aggregates.customer.commands.CreateCustomerCmd;
import myeslib3.example1.aggregates.customer.events.CustomerCreated;
import myeslib3.stack1.command.WriteModelRepository;
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

import static myeslib3.stack1.command.WriteModelRepository.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class EventsSingleProjectionRouteTest extends CamelTestSupport {

  final static String eventsChannelId = "channelExample1";

  static final Injector injector = Guice.createInjector();

  @Produce(uri = "direct:start")
  protected ProducerTemplate template;

  @EndpointInject(uri = "mock:result")
  protected MockEndpoint resultEndpoint;

  @Before
  public void init() throws Exception {
    injector.injectMembers(this);
  }

  @Test
  public void single_events_list() throws Exception {

    val daoMock= mock(EventsProjectorDao.class, withSettings().verboseLogging());
    val route = new EventsSingleProjectionRoute(eventsChannelId, daoMock, new MemoryIdempotentRepository(), false);
    context.addRoutes(route);

    val uowId = "uow#1";
    val aggregateRootId = "id-1";
    val cmd1 = new CreateCustomerCmd(UUID.randomUUID(), new CustomerId("c1"), "customer1");
    val event1 = new CustomerCreated(cmd1.getTargetId(), cmd1.getName());

    final List<WriteModelRepository.UnitOfWorkData> tuplesList =
            List.of(new UnitOfWorkData(uowId, 1L, aggregateRootId, List.of(event1)));

    resultEndpoint.expectedBodiesReceived(tuplesList);

    template.requestBody(tuplesList);

    resultEndpoint.assertIsSatisfied(1000);

    verify(daoMock).handle(eq(aggregateRootId), eq(List.of(event1)));

    verifyNoMoreInteractions(daoMock);

  }

  @Override
  protected RouteBuilder createRouteBuilder() throws Exception {

    return new RouteBuilder() {
      @Override
      public void configure() throws Exception {
      from("direct:start")
        .toF("seda:%s-events?multipleConsumers=%b", eventsChannelId, false)
        .log("*** from seda: ${header.aggregate_root_id} - ${body}")
        .to("mock:result");
      }
    };

  }

}