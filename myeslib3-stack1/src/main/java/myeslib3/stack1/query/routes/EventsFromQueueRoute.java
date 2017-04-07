package myeslib3.stack1.query.routes;


import lombok.AllArgsConstructor;
import org.apache.camel.builder.RouteBuilder;

@AllArgsConstructor
public class EventsFromQueueRoute extends RouteBuilder {

	@Override
	public void configure() throws Exception {

	}

	// triggered by:
	// PUT - via rest
	// polling with cron +backoff multiplier
	// on startup
}
