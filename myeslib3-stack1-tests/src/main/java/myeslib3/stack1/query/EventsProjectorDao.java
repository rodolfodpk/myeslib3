package myeslib3.stack1.query;

import javaslang.Tuple;
import javaslang.collection.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import myeslib3.core.data.Event;
import myeslib3.stack1.command.UnitOfWorkData;
import org.jooq.Configuration;
import org.jooq.impl.DSL;

@Slf4j
public abstract class EventsProjectorDao {

  @Getter
  private final String eventsChannelId;
  private final Configuration jooqCfg;

  public EventsProjectorDao(String eventsChannelId, final Configuration jooqCfg) {
    this.eventsChannelId = eventsChannelId;
    this.jooqCfg = jooqCfg;
  }

  public Long getLastUowSeq() {
    return null; // TODO
  }

  public void handle(final List<UnitOfWorkData> uowList) {

    log.info("writing events for eventsChannelId {}", eventsChannelId);

    DSL.using(jooqCfg)
      .transaction(ctx -> uowList.flatMap(uowdata -> uowdata.getEvents()
              .map(e -> Tuple.of(uowdata.getTargetId(), e)))
              .forEach(tuple -> handle(ctx, tuple._1(), tuple._2())));

  }

  abstract void handle(final Configuration ctx, final String id, final Event event);

}
