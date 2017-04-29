package myeslib3.stack1.query.routes;

import javaslang.Tuple3;
import javaslang.collection.List;
import lombok.NonNull;
import myeslib3.core.data.Event;
import myeslib3.stack1.Headers;
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
        final Tuple3<String, String, List<Event>> uow = e.getIn().getBody(Tuple3.class);
        e.getOut().setHeader(Headers.UNIT_OF_WORK_ID, uow._1());
        e.getOut().setBody(uow, Tuple3.class);
      })
      .idempotentConsumer(header(Headers.UNIT_OF_WORK_ID)).messageIdRepository(idempotentRepo)
      .log("will process ${body.class.name}")
      .process(e -> {
        final Tuple3<String, String, List<Event>> uow = e.getIn().getBody(Tuple3.class);
        eventsdao.handle(uow._2(), uow._3());
      })
      .log("after st-events-projector: ${body}");

    // TODO update last uow sequence

	}

}
