package myeslib3.example1.aggregates.customer.commands;

import lombok.Value;
import lombok.experimental.Wither;
import myeslib3.core.data.Command;
import myeslib3.example1.aggregates.customer.CustomerId;

import java.util.UUID;

@Value @Wither
public class CreateCustomerCmd implements Command {
  UUID commandId;
  CustomerId targetId;
  String name;
}
