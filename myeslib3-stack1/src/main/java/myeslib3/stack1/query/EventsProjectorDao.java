package myeslib3.stack1.query;

import javaslang.Tuple2;
import javaslang.collection.List;
import myeslib3.core.data.Event;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.sqlobject.Transaction;

/**
 * http://manikandan-k.github.io/2015/05/10/Transactions_in_jdbi.html
 */
public interface EventsProjectorDao {

  @Transaction(TransactionIsolationLevel.SERIALIZABLE)
  default void handle(List<Tuple2<String, List<Event>>> tuple2List) {

    tuple2List.forEach(tuple2 ->
            tuple2._2().forEach(event ->
              handle(tuple2._1(), event)));

  }

  @Transaction(TransactionIsolationLevel.SERIALIZABLE)
  default void handle(String id, List<Event> events) {
    for (Event e : events) {
      handle(id, e);
    }
  }

  @Transaction(TransactionIsolationLevel.SERIALIZABLE)
  void handle(String id, Event event);

}
