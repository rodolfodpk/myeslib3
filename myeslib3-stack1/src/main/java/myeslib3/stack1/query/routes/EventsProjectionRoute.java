package myeslib3.stack1.query.routes;

import javaslang.collection.List;
import lombok.NonNull;
import myeslib3.stack1.Headers;
import myeslib3.stack1.command.UnitOfWorkData;
import myeslib3.stack1.query.EventsProjector;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.AbstractListAggregationStrategy;
import org.apache.camel.processor.aggregate.MemoryAggregationRepository;
import org.apache.camel.spi.IdempotentRepository;

public class EventsProjectionRoute extends RouteBuilder {

  final EventsProjector eventsdao;
  final IdempotentRepository idempotentRepo;
  final boolean multipleConsumers;
  final long completionInterval;
  final int completionSize;

  public EventsProjectionRoute(@NonNull EventsProjector eventsdao,
                               @NonNull IdempotentRepository idempotentRepo,
                               boolean multipleConsumers, long completionInterval, int completionSize) {
    this.eventsdao = eventsdao;
    this.idempotentRepo = idempotentRepo;
    this.multipleConsumers = multipleConsumers;
    this.completionInterval = completionInterval;
    this.completionSize = completionSize;
  }

	@Override
	public void configure() throws Exception {

    fromF("seda:%s-events?multipleConsumers=%b", eventsdao.getEventsChannelId(), multipleConsumers)
      .routeId("st-events-projector" + eventsdao.getEventsChannelId())
      .threads(1)
      .process(e -> {
        final UnitOfWorkData uow = e.getIn().getBody(UnitOfWorkData.class);
        e.getOut().setHeader(Headers.UNIT_OF_WORK_ID, uow.getUowId());
        e.getOut().setBody(uow);
      })
      .idempotentConsumer(header(Headers.UNIT_OF_WORK_ID)).messageIdRepository(idempotentRepo)
      .aggregate(body()).aggregationStrategy(new Strategy())
        .completionInterval(completionInterval).completionSize(completionSize)
        .aggregationRepository(new MemoryAggregationRepository())
      .log("will process ${body.class.name}")
      .process(e -> {
        final List<UnitOfWorkData> list = List.ofAll(e.getIn().getBody(java.util.List.class));
        eventsdao.handle(list);
      })
      .log("after st-events-projector: ${body}");

    // TODO update last uow sequence

	}

  private class Strategy extends AbstractListAggregationStrategy<UnitOfWorkData> {
    @Override
    public UnitOfWorkData getValue(Exchange exchange) {
      return exchange.getIn().getBody(UnitOfWorkData.class);
    }
  }

}


