package myeslib3.stack1.query.routes;

import javaslang.Tuple3;
import lombok.NonNull;
import myeslib3.stack1.Headers;
import myeslib3.stack1.command.WriteModelRepository.UnitOfWorkData;
import myeslib3.stack1.query.EventsProjectorDao;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.IdempotentRepository;

public class EventsSingleProjectionRoute extends RouteBuilder {

	final String eventsChannelId;
  final EventsProjectorDao eventsdao;
  final IdempotentRepository idempotentRepo;
  final boolean multipleConsumers;

	public EventsSingleProjectionRoute(@NonNull String eventsChannelId, @NonNull EventsProjectorDao eventsdao,
                                     @NonNull IdempotentRepository idempotentRepo, boolean multipleConsumers) {
		this.eventsChannelId = eventsChannelId;
    this.eventsdao = eventsdao;
    this.idempotentRepo = idempotentRepo;
    this.multipleConsumers = multipleConsumers;
  }

	@Override
	public void configure() throws Exception {

    fromF("seda:%s-events?multipleConsumers=%b", eventsChannelId, multipleConsumers)
      .routeId("st-events-projector" + eventsChannelId)
      .threads(1)
      .split(body())
      .log("after split ${body.class.name}")
      .process(e -> {
        final UnitOfWorkData uow = e.getIn().getBody(UnitOfWorkData.class);
        e.getOut().setHeader(Headers.UNIT_OF_WORK_ID, uow.getUowId());
        e.getOut().setBody(uow, Tuple3.class);
      })
      .idempotentConsumer(header(Headers.UNIT_OF_WORK_ID)).messageIdRepository(idempotentRepo)
      .log("will process ${body.class.name}")
      .process(e -> {
        final UnitOfWorkData uow = e.getIn().getBody(UnitOfWorkData.class);
        eventsdao.handle(uow.getTargetId(), uow.getEvents());
      })
      .log("after st-events-projector: ${body}");

    // TODO update last uow sequence

	}

}
