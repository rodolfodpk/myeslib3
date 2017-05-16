package myeslib3.stack1;

import javaslang.Tuple;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import myeslib3.core.model.Event;
import myeslib3.core.model.EventsProjector;
import myeslib3.core.stack.ProjectionData;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.sqlobject.Transaction;

import java.util.List;

// http://manikandan-k.github.io/2015/05/10/Transactions_in_jdbi.html

@Slf4j
public abstract class EventsProjectorJdbi implements EventsProjector {

  @Getter
  private final String eventsChannelId;
  private final DBI dbi;

  public EventsProjectorJdbi(String eventsChannelId, final DBI dbi) {
    this.eventsChannelId = eventsChannelId;
    this.dbi = dbi;
  }

  public Long getLastUowSeq() {
    return null; // TODO
  }

  @Transaction(TransactionIsolationLevel.SERIALIZABLE)
  public void handle(final List<ProjectionData> uowList) {

    log.info("writing events for eventsChannelId {}", eventsChannelId);

    uowList.stream().flatMap(uowdata -> uowdata.getEvents()
              .stream().map(e -> Tuple.of(uowdata.getTargetId(), e)))
              .forEach(tuple -> handle(tuple._1(), tuple._2()));

  }

  @Transaction(TransactionIsolationLevel.SERIALIZABLE)
  public abstract void handle(final String id, final Event event);

}
