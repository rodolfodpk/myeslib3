package myeslib3.stack1.query;

import lombok.extern.slf4j.Slf4j;
import myeslib3.core.data.Event;
import myeslib3.example1.aggregates.customer.events.CustomerActivated;
import myeslib3.example1.aggregates.customer.events.CustomerCreated;
import myeslib3.example1.aggregates.customer.events.CustomerDeactivated;
import org.jooq.Configuration;
import org.jooq.impl.DSL;

import static javaslang.API.*;
import static javaslang.Predicates.instanceOf;

@Slf4j
public class Example1EventsProjector extends EventsProjectorDao {

  public Example1EventsProjector(String eventsChannelId, Configuration jooqCfg) {
    super(eventsChannelId, jooqCfg);
  }

  public void handle(Configuration ctx, String id, Event event) {

    Match(event).of(

      Case(instanceOf(CustomerCreated.class), (e) ->
              run(() -> DSL.using(ctx))
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
