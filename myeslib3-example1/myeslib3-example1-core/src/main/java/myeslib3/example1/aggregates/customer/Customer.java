package myeslib3.example1.aggregates.customer;

import lombok.Value;
import lombok.experimental.Wither;
import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Event;
import myeslib3.example1.aggregates.customer.events.CustomerActivated;
import myeslib3.example1.aggregates.customer.events.CustomerCreated;
import myeslib3.example1.aggregates.customer.events.CustomerDeactivated;
import myeslib3.example1.services.SampleService;

import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.Validate.validState;

@Value
@Wither
public class Customer implements AggregateRoot {

  transient SampleService service;

  CustomerId id;
  String name;
  boolean isActive;
  String reason;

  List<Event> create(CustomerId id, String name) {

    validState(this.id == null,
            "customer already exists! customerId should be null");

    return asList(new CustomerCreated(id, name));
  }

  List<Event> activate(String reason) {

    return asList(new CustomerActivated(reason, service.now()));
  }

  List<Event> deactivate(String reason) {

    return asList(new CustomerDeactivated(reason, service.now()));
  }

  public static Customer create(CustomerId id, String name, boolean isActive, String reason) {
    return new Customer(null, id, name, isActive, reason);
  }

}
