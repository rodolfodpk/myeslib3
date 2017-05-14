package myeslib3.example1.projections;

import javaslang.Tuple;
import javaslang.collection.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import myeslib3.core.data.Event;
import myeslib3.example1.aggregates.customer.events.CustomerActivated;
import myeslib3.example1.aggregates.customer.events.CustomerCreated;
import myeslib3.example1.aggregates.customer.events.CustomerDeactivated;
import myeslib3.stack1.command.UnitOfWorkData;
import myeslib3.stack1.query.EventsProjector;
import org.jooq.Configuration;
import org.jooq.impl.DSL;

import static javaslang.API.*;
import static javaslang.Predicates.instanceOf;

@Slf4j
public class Example1EventsProjectorJooq implements EventsProjector {

  @Getter
  private final String eventsChannelId;
  private final Configuration jooqCfg;

  public Example1EventsProjectorJooq(String eventsChannelId, final Configuration jooqCfg) {
    this.eventsChannelId = eventsChannelId;
    this.jooqCfg = jooqCfg;
  }

  @Override
  public Long getLastUowSeq() {
    return null; // TODO
  }

  @Override
  public void handle(final List<UnitOfWorkData> uowList) { // TODO interface com apenas este e o de cima

    log.info("writing events for eventsChannelId {}", eventsChannelId);

    DSL.using(jooqCfg)
      .transaction(ctx -> uowList.flatMap(uowdata -> uowdata.getEvents()
              .map(e -> Tuple.of(uowdata.getTargetId(), e)))
              .forEach(tuple -> handle(ctx, tuple._1(), tuple._2())));

  }


  public void handle(Configuration ctx, String id, Event event) {

    Match(event).of(

            Case(instanceOf(CustomerCreated.class), (e) ->
                    run(() -> DSL.using(ctx)) // TODO
            ),

            Case(instanceOf(CustomerActivated.class), (e) ->
                    run(() -> DSL.using(ctx))
            ),

            Case(instanceOf(CustomerDeactivated.class), (e) ->
                    run(() -> DSL.using(ctx))
            ),

            Case($(), e -> run(() -> log.warn("{} does not have any event projection handler", e)))

    );

  }

}
