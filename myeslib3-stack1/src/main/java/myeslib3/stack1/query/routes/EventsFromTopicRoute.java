package myeslib3.stack1.query.routes;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import myeslib3.core.data.UnitOfWork;
import myeslib3.stack1.Headers;
import myeslib3.stack1.command.WriteModelRepository;
import myeslib3.stack1.stack1infra.DatabaseConfig;
import org.apache.camel.builder.RouteBuilder;

import java.util.Optional;
import java.util.UUID;

@AllArgsConstructor
public class EventsFromTopicRoute extends RouteBuilder {

	@NonNull final String eventsChannelId;
	@NonNull final WriteModelRepository repo;
	@NonNull final DatabaseConfig dc;

	@Override
	public void configure() throws Exception {

		fromF("pgevent:%s?database=%s&channel=%s", "pgDatasource", dc.db_name(), eventsChannelId)
			.routeId("events-from-topic-" + eventsChannelId)
			.threads(10)
			.process(e -> {
				final String uowId = e.getIn().getHeader(Headers.UNIT_OF_WORK_ID, String.class);
				final Optional<UnitOfWork> unitOfWork = repo.get(UUID.fromString(uowId));
				e.getOut().setBody(unitOfWork.get(), UnitOfWork.class);
				e.getOut().setHeader(Headers.UNIT_OF_WORK_ID, uowId);
				e.getOut().setHeader(Headers.AGGREGATE_ROOT_ID, unitOfWork.get().getTargetId());
			})
			.toF("seda:%s-events-projector", eventsChannelId)
			.toF("seda:%s-events-monitor", eventsChannelId)
		;

	}

}
