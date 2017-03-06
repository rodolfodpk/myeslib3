package myeslib3.core.data;

import java.time.LocalDateTime;

public interface CommandScheduling {

  Command scheduledCommand();

  LocalDateTime scheduledAt();

}
