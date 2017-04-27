package myeslib3.stack1.query.routes;

import javaslang.Tuple2;
import javaslang.collection.List;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import myeslib3.core.data.Event;
import myeslib3.stack1.Headers;
import myeslib3.stack1.query.EventsProjectorDao;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.AbstractListAggregationStrategy;
import org.apache.camel.processor.aggregate.MemoryAggregationRepository;

import java.util.stream.Collectors;

@Slf4j
public class EventsMultiProjectionRoute extends RouteBuilder {

	final String eventsChannelId;
  final EventsProjectorDao eventsdao;
  final int intervalInMilliseconds;
  final int threadPoolSize;
  final boolean multipleConsumers;

	public EventsMultiProjectionRoute(String eventsChannelId, EventsProjectorDao eventsdao,
                                    int intervalInMilliseconds, int threadPoolSize, boolean multipleConsumers) {
		this.eventsChannelId = eventsChannelId;
    this.eventsdao = eventsdao;
    this.intervalInMilliseconds = intervalInMilliseconds;
    this.threadPoolSize = threadPoolSize;
    this.multipleConsumers = multipleConsumers;
  }

	@Override
	public void configure() throws Exception {

    fromF("seda:%s-events?multipleConsumers=%b", eventsChannelId, multipleConsumers)
      .routeId("mt-events-projector" + eventsChannelId)
      .threads(threadPoolSize)
      .log("received: ${body}")
      .split(bodyAs(List.class))
      .process(e -> {
        final Tuple2<String, List<Event>> tuple = e.getIn().getBody(Tuple2.class);
        e.getOut().setHeader(Headers.AGGREGATE_ROOT_ID, tuple._1());
        e.getOut().setBody(tuple._2());
      })
      .aggregate(header(Headers.AGGREGATE_ROOT_ID), new Strategy())
      .completionTimeout(intervalInMilliseconds).aggregationRepository(new MemoryAggregationRepository())
      .process(e -> {
        final java.util.List<List<Event>> aggregatedEvents = e.getIn().getBody(java.util.List.class);
        final java.util.List<Event> flatMappedList = aggregatedEvents.stream().flatMap(l -> l.toJavaStream())
                .collect(Collectors.toList());
        val aggregateRootId = e.getIn().getHeader(Headers.AGGREGATE_ROOT_ID, String.class);
        eventsdao.handle(aggregateRootId, List.ofAll(flatMappedList));
        e.getOut().setBody(flatMappedList);
      })
      .log("delivered: ${body}")
      .setBody(constant("gogo"))
	    ;
	}

  private class Strategy extends AbstractListAggregationStrategy<List> {
    @Override
    public List getValue(Exchange exchange) {
      return exchange.getIn().getBody(List.class);
    }
  }

}
