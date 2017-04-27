package myeslib3.stack1.query.routes;

import javaslang.Tuple2;
import javaslang.collection.List;
import myeslib3.core.data.Event;
import myeslib3.stack1.query.EventsProjectorDao;
import org.apache.camel.builder.RouteBuilder;

public class EventsSingleProjectionRoute extends RouteBuilder {

	final String eventsChannelId;
  final EventsProjectorDao eventsdao;
  final boolean multipleConsumers;

	public EventsSingleProjectionRoute(String eventsChannelId, EventsProjectorDao eventsdao, boolean multipleConsumers) {
		this.eventsChannelId = eventsChannelId;
    this.eventsdao = eventsdao;
    this.multipleConsumers = multipleConsumers;
  }

	@Override
	public void configure() throws Exception {

	  // single thread

    fromF("seda:%s-events?multipleConsumers=%b", eventsChannelId, multipleConsumers)
      .routeId("st-events-projector" + eventsChannelId)
      .threads(1)
      .process(e -> {
        final List<Tuple2<String, List<Event>>> tuple2List = e.getIn().getBody(List.class);
        eventsdao.handle(tuple2List);
      })
      .log("after st-events-projector: ${body}");

	}

}
