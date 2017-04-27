package myeslib3.stack1.query.routes;

import javaslang.Tuple2;
import javaslang.collection.List;
import myeslib3.core.data.Event;
import myeslib3.stack1.command.WriteModelRepository;
import myeslib3.stack1.stack1infra.BoundedContextConfig;
import org.apache.camel.builder.RouteBuilder;

public class EventsPollingRoute extends RouteBuilder {

	final String eventsChannelId;
	final WriteModelRepository repo;
	final BoundedContextConfig stack1Config;

	public EventsPollingRoute(String eventsChannelId, WriteModelRepository repo, BoundedContextConfig stack1Config) {
		this.eventsChannelId = eventsChannelId;
		this.repo = repo;
    this.stack1Config = stack1Config;
  }

	@Override
	public void configure() throws Exception {

    fromF("quartz2://events/%s?cron=%s", eventsChannelId, stack1Config.camelized(stack1Config.events_cron_polling()))
      .routeId("events-" + eventsChannelId)
      .startupOrder(10)
      .threads(1)
      .log("fired")
      .hystrix()
        .hystrixConfiguration()
          .groupKey("events-" + eventsChannelId)
          .executionTimeoutInMilliseconds(5000).circuitBreakerSleepWindowInMilliseconds(10000)
        .end()
        .process(e -> {
          final List<Tuple2<String, List<Event>>> tuplesList = repo.getAllSince(0, 10);
          e.getOut().setBody(tuplesList, List.class);
        })
        .toF("seda:%s-events", eventsChannelId)
      .onFallback()
        .log("fallback - circuit breaker seems to be open")
        .stop()
      .end()
      .log("${body}");
  }
}
