package myeslib3.stack1.query.routes;

import lombok.AllArgsConstructor;
import myeslib3.core.UnitOfWork;
import myeslib3.stack1.query.EventsProjector;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.AbstractListAggregationStrategy;
import org.apache.camel.processor.aggregate.MemoryAggregationRepository;

import java.util.List;

@AllArgsConstructor
public class EventsProjectorRoute extends RouteBuilder {

	static final String AGGREGATE_ROOT_ID = "aggregateRootId";

	final String eventsChannelId;
	final EventsProjector eventsProjector;
	final int intervalInMilliseconds;

	@Override
	public void configure() throws Exception {

		fromF("seda:%s-events-projector", eventsChannelId)
			.routeId(eventsChannelId + "-events-projector")
			.setHeader(AGGREGATE_ROOT_ID, simple("${body.getAggregateRootId()"))
			.aggregate(header(AGGREGATE_ROOT_ID), new Strategy())
						.completionInterval(intervalInMilliseconds).aggregationRepository(new MemoryAggregationRepository())
			.wireTap(String.format("direct:%s-events-projector-write", eventsChannelId))
			.log("${body}")
			;

		fromF("%s-events-projector-write", eventsChannelId)
			.routeId(eventsChannelId + "-events-projector-write")
			.threads(10)
			.process(e -> {
				final String aggregateRootId = e.getIn().getHeader(AGGREGATE_ROOT_ID, String.class);
				final List<UnitOfWork> list = e.getIn().getBody(List.class);
				eventsProjector.apply(aggregateRootId, list);
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