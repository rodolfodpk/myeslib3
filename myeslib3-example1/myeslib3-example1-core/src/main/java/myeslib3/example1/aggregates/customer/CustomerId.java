package myeslib3.example1.aggregates.customer;

import lombok.Value;
import myeslib3.core.data.AggregateRootId;

@Value
public class CustomerId implements AggregateRootId {
  String stringValue;

}
