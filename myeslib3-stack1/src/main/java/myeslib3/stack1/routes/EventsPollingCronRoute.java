package myeslib3.stack1.routes;

import lombok.NonNull;
import org.apache.camel.builder.RouteBuilder;

import javax.inject.Named;

import static myeslib3.stack1.routes.StringHelper.camelizedCron;

public class EventsPollingCronRoute extends RouteBuilder {

	final String eventsChannelId;
	final String eventsCronPooling;

	public EventsPollingCronRoute(@NonNull String eventsChannelId,
																@NonNull @Named("events_cron_polling") String eventsCronPooling) {
		this.eventsChannelId = eventsChannelId;
		this.eventsCronPooling = eventsCronPooling;
  }

	@Override
	public void configure() throws Exception {

    fromF("quartz2://events/%s?cron=%s",
            eventsChannelId, camelizedCron(eventsCronPooling))
      .routeId("pool-events-cron" + eventsChannelId)
      .startupOrder(10)
      .threads(1)
      .log("fired")
      .toF("direct:pool-events-%s", eventsChannelId);

  }
}
