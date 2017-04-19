package myeslib3.example1.aggregates.customer.commands;

import lombok.Value;
import lombok.experimental.Wither;
import myeslib3.core.data.Command;

import java.util.UUID;

@Value @Wither
public class DeactivateCustomerCmd implements Command {
  UUID commandId;
  String targetId;
  String reason;
}
