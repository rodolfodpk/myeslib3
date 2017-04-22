package myeslib3.stack1.query.routes;

import lombok.val;
import myeslib3.core.data.AggregateRootId;
import myeslib3.core.data.UnitOfWork;
import myeslib3.stack1.Headers;
import myeslib3.stack1.query.EventsDao;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.MemoryAggregationRepository;

import java.util.List;
import java.util.stream.Collectors;

public class SingleThreadEventsProjectorRoute extends RouteBuilder {

	final String eventsChannelId;
	final EventsDao eventsdao;
  final int intervalInMilliseconds;

  public SingleThreadEventsProjectorRoute(String eventsChannelId, EventsDao eventsdao, int intervalInMilliseconds) {
    this.eventsChannelId = eventsChannelId;
    this.eventsdao = eventsdao;
    this.intervalInMilliseconds = intervalInMilliseconds;
  }

  @Override
	public void configure() throws Exception {

		fromF("seda:%s-events?multipleConsumers=true", eventsChannelId)
			.routeId(eventsChannelId + "-st-events-projector")
			.aggregate(body())
						.completionInterval(intervalInMilliseconds).aggregationRepository(new MemoryAggregationRepository())
			.wireTap(String.format("direct:%s-st-events-projector-write", eventsChannelId))
			.log("${body}")
			;

		fromF("%s-st-events-projector-write", eventsChannelId)
			.routeId(eventsChannelId + "-st-events-projector-write")
			.process(e -> {
				final List<UnitOfWork> list = e.getIn().getBody(List.class);
        val events = list.stream().flatMap(uow -> uow.getEvents().stream()).collect(Collectors.toList());
        val id = e.getIn().getHeader(Headers.AGGREGATE_ROOT_ID, AggregateRootId.class);
        eventsdao.handle(id, events);
			})
			.log("${body}");

	}

}
