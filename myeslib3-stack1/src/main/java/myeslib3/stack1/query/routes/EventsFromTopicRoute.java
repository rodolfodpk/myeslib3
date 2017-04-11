package myeslib3.stack1.query.routes;

import com.impossibl.postgres.jdbc.PGDataSource;
import lombok.AllArgsConstructor;
import myeslib3.core.data.UnitOfWork;
import myeslib3.stack1.command.WriteModelRepository;
import myeslib3.stack1.stack1infra.DatabaseConfig;
import org.apache.camel.builder.RouteBuilder;

import java.util.UUID;

@AllArgsConstructor
public class EventsFromTopicRoute extends RouteBuilder {

	private static final java.lang.String UNIT_OF_WORK_ID = "UNIT_OF_WORK_ID";
	final String eventsChannelId;
	final PGDataSource ds;
	final DatabaseConfig dc;
	final WriteModelRepository repo;

	@Override
	public void configure() throws Exception {

		fromF("pgevent://%s:%s/%s/%s?user=%s&pass=%s",
			dc.db_host(), dc.db_port(), dc.db_name(), eventsChannelId, dc.db_user(), dc.db_password())
			.routeId("events-serialize-topic-" + eventsChannelId)
			.process(e -> {
				final String uowId = e.getIn().getHeader(UNIT_OF_WORK_ID, String.class);
				final UnitOfWork unitOfWork = repo.get(UUID.fromString(uowId));
				e.getOut().setBody(unitOfWork, UnitOfWork.class);
			})
			.toF("seda:%s-events-projector", eventsChannelId)
			.toF("seda:%s-events-monitor", eventsChannelId)
		;

	}

}
