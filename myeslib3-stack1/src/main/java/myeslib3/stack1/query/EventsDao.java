package myeslib3.stack1.query;

import myeslib3.core.data.AggregateRootId;
import myeslib3.core.data.Event;
import org.skife.jdbi.v2.sqlobject.Transaction;

import java.util.List;

/**
 * http://manikandan-k.github.io/2015/05/10/Transactions_in_jdbi.html
 */
public interface EventsDao {

  @Transaction
  void handle(AggregateRootId id, List<Event> events);

}
