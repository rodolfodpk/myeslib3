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

import java.util.List;
import java.util.stream.Collectors;

public class MultiThreadEventsProjectorRoute extends RouteBuilder {

	final String eventsChannelId;
	final EventsDao eventsdao;
  final int intervalInMilliseconds;
  final int threadPoolSize;

  public MultiThreadEventsProjectorRoute(String eventsChannelId, EventsDao eventsdao,
                                         int intervalInMilliseconds, int threadPoolSize) {
    this.eventsChannelId = eventsChannelId;
    this.eventsdao = eventsdao;
    this.intervalInMilliseconds = intervalInMilliseconds;
    this.threadPoolSize = threadPoolSize;
  }

  @Override
	public void configure() throws Exception {

		fromF("seda:%s-events?multipleConsumers=true", eventsChannelId)
			.routeId(eventsChannelId + "-mt-events-projector")
			.aggregate(header(Headers.AGGREGATE_ROOT_ID), new Strategy())
						.completionInterval(intervalInMilliseconds).aggregationRepository(new MemoryAggregationRepository())
			.wireTap(String.format("direct:%s-mt-events-projector-write", eventsChannelId))
			.log("${body}")
			;

		fromF("%s-mt-events-projector-write", eventsChannelId)
			.routeId(eventsChannelId + "-mt-events-projector-write")
			.threads(threadPoolSize)
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