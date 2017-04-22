package myeslib3.stack1.query.routes;

import myeslib3.core.data.UnitOfWork;
import myeslib3.stack1.Headers;
import myeslib3.stack1.command.WriteModelRepository;
import myeslib3.stack1.stack1infra.DatabaseConfig;
import org.apache.camel.builder.RouteBuilder;

import java.util.Optional;
import java.util.UUID;

public class EventsFromTopicRoute extends RouteBuilder {

	final String eventsChannelId;
	final WriteModelRepository repo;
	final DatabaseConfig dc;

	public EventsFromTopicRoute(String eventsChannelId, WriteModelRepository repo, DatabaseConfig dc) {
		this.eventsChannelId = eventsChannelId;
		this.repo = repo;
		this.dc = dc;
	}

	@Override
	public void configure() throws Exception {
		fromF("pgevent://%s:%s/%s/%s?user=%s&pass=%s",
						dc.db_host(), dc.db_port(), dc.db_name(), eventsChannelId, dc.db_user(), dc.db_password())
			.routeId("events-from-topic-" + eventsChannelId)
			.threads(10)
			.process(e -> {
				final String uowId = e.getIn().getBody(String.class);
				final Optional<UnitOfWork> unitOfWork = repo.get(UUID.fromString(uowId));
				e.getOut().setBody(unitOfWork.get(), UnitOfWork.class);
				e.getOut().setHeader(Headers.UNIT_OF_WORK_ID, uowId);
				e.getOut().setHeader(Headers.AGGREGATE_ROOT_ID, unitOfWork.get().getTargetId());
			})
			.log("from db -> ${body}")
			.toF("seda:%s-events", eventsChannelId)
		;

	}

}
