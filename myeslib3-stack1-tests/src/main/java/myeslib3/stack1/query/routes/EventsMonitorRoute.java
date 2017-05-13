package myeslib3.stack1.query.routes;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import myeslib3.core.functions.SagaEventMonitoringFn;
import org.apache.camel.builder.RouteBuilder;

@AllArgsConstructor
public class EventsMonitorRoute extends RouteBuilder {

  private static final String TARGET_ENDPOINT = "TARGET_ENDPOINT";

  @NonNull final String eventsChannelId;
  @NonNull final SagaEventMonitoringFn sagaEventMonitoringFn;
  @NonNull final Gson gson;

  @Override
  public void configure() throws Exception {

//    fromF("seda:%s-events?multipleConsumers=true", eventsChannelId)
//      .routeId(eventsChannelId + "-events-monitor")
//      .process( e-> {
//        final UnitOfWork unitOfWork = e.getIn().getBody(UnitOfWork.class);
//        e.getOut().setBody(unitOfWork.getEvents(), List.class);
//        e.getOut().setHeaders(e.getIn().getHeaders());
//      })
//      .split(body())
//      .process(e -> {
//        final Event event = e.getIn().getBody(Event.class);
//        final Optional<SagaEventMonitoringFn.CommandMessage> command = sagaEventMonitoringFn.apply(event);
//        e.getOut().setBody(command.isPresent() ? command.get() : null);
//        e.getOut().setHeaders(e.getIn().getHeaders());
//      })
//      .filter(bodyAs(SagaEventMonitoringFn.CommandMessage.class).isNotNull())
//      .process(e -> {
//        final SagaEventMonitoringFn.CommandMessage msg = e.getIn().getBody(SagaEventMonitoringFn.CommandMessage.class);
//        final String cmdAsJson = gson.toJson(msg.getCommand(), Command.class);
//        final String targetEndpoint = String.format("direct:handle-%s", commandId(msg.getCommandClass()));
//        e.getOut().setBody(cmdAsJson, String.class);
//        e.getOut().setHeaders(e.getIn().getHeaders());
//        e.getOut().setHeader(TARGET_ENDPOINT, targetEndpoint);
//      })
//      .recipientList(header(TARGET_ENDPOINT))
//      ;
  }

}
