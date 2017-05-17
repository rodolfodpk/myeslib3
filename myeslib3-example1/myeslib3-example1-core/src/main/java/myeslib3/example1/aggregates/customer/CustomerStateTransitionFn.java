package myeslib3.example1.aggregates.customer;

import javaslang.Function2;
import myeslib3.core.model.Event;
import myeslib3.example1.aggregates.customer.events.CustomerActivated;
import myeslib3.example1.aggregates.customer.events.CustomerCreated;
import myeslib3.example1.aggregates.customer.events.CustomerDeactivated;

import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;

public class CustomerStateTransitionFn implements Function2<Event, Customer, Customer> {

  @Override
  public Customer apply(Event event, Customer instance) {

    return Match(event).of(

      Case(instanceOf(CustomerCreated.class),
              (e) -> instance.withId(e.getId()).withName(e.getName())),
      Case(instanceOf(CustomerActivated.class),
              (e) -> instance.withReason(e.getReason()).withActive(true)),
      Case(instanceOf(CustomerDeactivated.class),
              (e) -> instance.withReason(e.getReason()).withActive(false))

    );
  }
}
