package myeslib3.example1.aggregates.customer.events;

import lombok.Value;
import myeslib3.core.data.CommandScheduling;
import myeslib3.core.data.Event;
import myeslib3.example1.aggregates.customer.commands.DeactivateCustomerCmd;

import java.time.LocalDateTime;

@Value
public class DeactivatedCmdScheduled implements Event, CommandScheduling {

  DeactivateCustomerCmd scheduledCommand;
  LocalDateTime scheduledAt;

}
