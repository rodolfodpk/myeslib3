package myeslib3.example1.aggregates.customer;

import lombok.Value;
import lombok.experimental.Wither;
import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Event;
import myeslib3.example1.aggregates.customer.commands.DeactivateCustomerCmd;
import myeslib3.example1.aggregates.customer.events.CustomerActivated;
import myeslib3.example1.aggregates.customer.events.CustomerCreated;
import myeslib3.example1.aggregates.customer.events.CustomerDeactivated;
import myeslib3.example1.aggregates.customer.events.DeactivatedCmdScheduled;
import myeslib3.example1.services.SupplierHelperService;

import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.Validate.validState;

@Value
@Wither
public class Customer implements AggregateRoot {

  transient SupplierHelperService service;

  String id;
  String name;
  boolean isActive;
  String reason;

  List<Event> create(String targetId, String name) {

    validState(this.id == null,
            "customer already exists! customerId should be null");
    return asList(new CustomerCreated(targetId, name));
  }

  List<Event> activate(String reason) {

    return asList(new CustomerActivated(reason, service.now()),
            new DeactivatedCmdScheduled(
                    new DeactivateCustomerCmd(service.uuid(), id,
                            "just because I want automatic deactivation 1 day after activation"),
                    service.now().plusDays(1)));
  }

  List<Event> deactivate(String reason) {

    return asList(new CustomerDeactivated(reason, service.now()));
  }

  public static Customer create(String id, String name, boolean isActive, String reason) {
    return new Customer(null, id, name, isActive, reason);
  }

}