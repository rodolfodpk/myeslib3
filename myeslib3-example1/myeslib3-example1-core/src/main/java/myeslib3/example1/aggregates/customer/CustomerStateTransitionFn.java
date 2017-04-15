package myeslib3.example1.aggregates.customer;

import myeslib3.core.data.Event;
import myeslib3.example1.aggregates.customer.events.CustomerActivated;
import myeslib3.example1.aggregates.customer.events.CustomerCreated;

import java.util.function.BiFunction;

import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;

public class CustomerStateTransitionFn implements BiFunction<Event, Customer, Customer> {

  @Override
  public Customer apply(Event event, Customer instance) {
    return Match(event).of(
            Case(instanceOf(CustomerCreated.class), (e) -> instance.withId(e.getId()).withName(e.getName())),
            Case(instanceOf(CustomerActivated.class), (e) -> instance.withReason(e.getReason()))
    );
  }
}
