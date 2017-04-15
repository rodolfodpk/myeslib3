package myeslib3.stack1.query.routes;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import myeslib3.core.data.UnitOfWork;
import myeslib3.stack1.Headers;
import myeslib3.stack1.query.EventsProjector;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.AbstractListAggregationStrategy;
import org.apache.camel.processor.aggregate.MemoryAggregationRepository;

import java.util.List;

@AllArgsConstructor
public class EventsProjectorRoute extends RouteBuilder {

	@NonNull final String eventsChannelId;
	@NonNull final EventsProjector eventsProjector;
	@NonNull final int intervalInMilliseconds;

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
				for (UnitOfWork uow: list) {
					eventsProjector.apply(uow);
				}
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