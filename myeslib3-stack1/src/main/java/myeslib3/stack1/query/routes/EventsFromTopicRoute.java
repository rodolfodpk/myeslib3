package myeslib3.stack1.query.routes;

import com.impossibl.postgres.jdbc.PGDataSource;
import lombok.AllArgsConstructor;
import myeslib3.stack1.stack1infra.DatabaseConfig;
import org.apache.camel.builder.RouteBuilder;

@AllArgsConstructor
public class EventsFromTopicRoute extends RouteBuilder {

	final String eventsChannelId;
	final PGDataSource ds;
	final DatabaseConfig dc;

	@Override
	public void configure() throws Exception {

		fromF("pgevent://%s:%s/%s/%s?user=%s&pass=%s",
						dc.db_host(), dc.db_port(), dc.db_name(), eventsChannelId, dc.db_user(), dc.db_password())
						.toF("seda:%s-events-projector");

	}

}
