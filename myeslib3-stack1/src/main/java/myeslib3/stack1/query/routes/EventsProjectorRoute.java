package myeslib3.stack1.query.routes;

import lombok.val;
import myeslib3.core.data.AggregateRootId;
import myeslib3.core.data.UnitOfWork;
import myeslib3.stack1.Headers;
import myeslib3.stack1.query.EventsDao;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.AbstractListAggregationStrategy;
import org.apache.camel.processor.aggregate.MemoryAggregationRepository;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class EventsProjectorRoute extends RouteBuilder {

	final String eventsChannelId;
	final EventsDao eventsdao;
  final int intervalInMilliseconds;

  @Inject
  public EventsProjectorRoute(String eventsChannelId, EventsDao eventsdao, int intervalInMilliseconds) {
    this.eventsChannelId = eventsChannelId;
    this.eventsdao = eventsdao;
    this.intervalInMilliseconds = intervalInMilliseconds;
  }

  @Override
	public void configure() throws Exception {

		fromF("seda:%s-events-projector", eventsChannelId)
			.routeId(eventsChannelId + "-events-projector")
			.aggregate(header(Headers.AGGREGATE_ROOT_ID), new Strategy())
						.completionInterval(intervalInMilliseconds).aggregationRepository(new MemoryAggregationRepository())
			.wireTap(String.format("direct:%s-events-projector-write", eventsChannelId))
			.log("${body}")
			;

		fromF("%s-events-projector-write", eventsChannelId)
			.routeId(eventsChannelId + "-events-projector-write")
			.threads(10)
			.process(e -> {
				final List<UnitOfWork> list = e.getIn().getBody(List.class);
        val events = list.stream().flatMap(uow -> uow.getEvents().stream()).collect(Collectors.toList());
        val id = e.getIn().getHeader(Headers.AGGREGATE_ROOT_ID, AggregateRootId.class);
        eventsdao.handle(id, events);
			})
			.log("${body}");

	}

	private class Strategy extends AbstractListAggregationStrategy<UnitOfWork> {
		@Override
		public UnitOfWork getValue(Exchange exchange) {
			return exchange.getIn().getBody(UnitOfWork.class);
		}
	}

}

/*
schemas: denormalized, snowflake, star, json, etc
 */