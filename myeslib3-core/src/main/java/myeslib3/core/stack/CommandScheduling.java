package myeslib3.core.stack;

import myeslib3.core.model.Command;

import java.time.LocalDateTime;

public interface CommandScheduling {

  Command getScheduledCommand();
  LocalDateTime getScheduledAt();

}
