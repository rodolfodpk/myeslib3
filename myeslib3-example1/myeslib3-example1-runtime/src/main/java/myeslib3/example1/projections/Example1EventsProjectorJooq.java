package myeslib3.example1.projections;

import javaslang.Tuple;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import myeslib3.core.model.Event;
import myeslib3.core.model.EventsProjector;
import myeslib3.core.stack.ProjectionData;
import myeslib3.example1.aggregates.customer.events.CustomerActivated;
import myeslib3.example1.aggregates.customer.events.CustomerCreated;
import myeslib3.example1.aggregates.customer.events.CustomerDeactivated;
import org.jooq.Configuration;
import org.jooq.impl.DSL;

import java.util.List;

import static example1.datamodel.tables.CustomerSummary.CUSTOMER_SUMMARY;
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
  public void handle(final List<ProjectionData> uowList) { // TODO interface com apenas este e o de cima

    log.info("writing {} events for eventsChannelId {}", uowList.size(), eventsChannelId);

    DSL.using(jooqCfg)
      .transaction(ctx -> uowList.stream().flatMap(uowdata -> uowdata.getEvents().stream()
              .map(e -> Tuple.of(uowdata.getTargetId(), e)))
              .forEach(tuple -> handle(ctx, tuple._1(), tuple._2())));

  }


  public void handle(Configuration ctx, String id, Event event) {

    Match(event).of(

      Case(instanceOf(CustomerCreated.class), (e) ->
              run(() -> DSL.using(ctx).insertInto(CUSTOMER_SUMMARY)
                      .values(e.getId(), e.getName(), false))
      ),

      Case(instanceOf(CustomerActivated.class), (e) ->
              run(() -> DSL.using(ctx).update(CUSTOMER_SUMMARY)
                                      .set(CUSTOMER_SUMMARY.IS_ACTIVE, true))
      ),

      Case(instanceOf(CustomerDeactivated.class), (e) ->
              run(() -> DSL.using(ctx).update(CUSTOMER_SUMMARY)
                      .set(CUSTOMER_SUMMARY.IS_ACTIVE, false))
      ),

      Case($(), e -> run(() -> log.warn("{} does not have any event projection handler", e)))

    );

    // TODO update uow_last_seq for this event channel

  }

}
