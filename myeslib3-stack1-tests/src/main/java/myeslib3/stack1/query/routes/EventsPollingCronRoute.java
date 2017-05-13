package myeslib3.stack1.query.routes;

import lombok.NonNull;
import myeslib3.stack1.stack1infra.BoundedContextConfig;
import org.apache.camel.builder.RouteBuilder;

public class EventsPollingCronRoute extends RouteBuilder {

	final String eventsChannelId;
	final BoundedContextConfig config;

	public EventsPollingCronRoute(@NonNull String eventsChannelId, @NonNull BoundedContextConfig config) {
		this.eventsChannelId = eventsChannelId;
    this.config = config;
  }

	@Override
	public void configure() throws Exception {

    fromF("quartz2://events/%s?cron=%s",
            eventsChannelId, config.camelized(config.events_cron_polling()))
      .routeId("pool-events-cron" + eventsChannelId)
      .startupOrder(10)
      .threads(1)
      .log("fired")
      .toF("direct:pool-events-%s", eventsChannelId);

  }
}
