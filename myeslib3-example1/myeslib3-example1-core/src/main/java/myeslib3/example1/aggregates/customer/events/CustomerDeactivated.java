package myeslib3.example1.aggregates.customer.events;

import lombok.Value;
import myeslib3.core.model.Event;

import java.time.LocalDateTime;

@Value
public class CustomerDeactivated implements Event {
  String reason;
  LocalDateTime when;
}
