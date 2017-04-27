package myeslib3.stack1.query;

import lombok.extern.slf4j.Slf4j;
import myeslib3.core.data.Event;
import myeslib3.example1.aggregates.customer.events.CustomerActivated;
import myeslib3.example1.aggregates.customer.events.CustomerCreated;
import myeslib3.example1.aggregates.customer.events.CustomerDeactivated;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.CreateSqlObject;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

import static javaslang.API.*;
import static javaslang.Predicates.instanceOf;

@Slf4j
public abstract class Example1EventsProjectorDao implements EventsProjectorDao {

  @CreateSqlObject
  abstract CustomerDao customerDao();

  public void handle(String id, Event event) {

    Match(event).of(

      Case(instanceOf(CustomerCreated.class), (e) ->
        run(() -> customerDao().create(id, e.getName()))
      ),

      Case(instanceOf(CustomerActivated.class), (e) ->
        run(() -> customerDao().setActive(id, true))
      ),

      Case(instanceOf(CustomerDeactivated.class), (e) ->
        run(() -> customerDao().setActive(id, false))
      ),

      Case($(), e -> run(() -> log.warn("{} does not have handler", e)))

    );

  }

  static public abstract class CustomerDao {

    @SqlUpdate("INSERT INTO CustomerSummary(id, name) VALUES(:id, :name)")
    public abstract void create(@Bind("id") String id, @Bind("name") String name);

    @SqlUpdate("UPDATE CustomerSummary set is_active = :isActive where id = :id")
    public abstract void setActive(@Bind("id") String id, @Bind("isActive") boolean isActive);

  }

}
