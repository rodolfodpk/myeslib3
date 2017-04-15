package myeslib3.example1.aggregates.customer.events;

import lombok.Value;
import myeslib3.core.data.Event;

@Value
public class CustomerCreated implements Event {
  String id;
  String name;
}
