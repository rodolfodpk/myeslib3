package myeslib3.example1.aggregates.customer.events;

import lombok.Value;
import myeslib3.core.model.Event;
import myeslib3.example1.aggregates.customer.CustomerId;

@Value
public class CustomerCreated implements Event {
  CustomerId id;
  String name;
}
