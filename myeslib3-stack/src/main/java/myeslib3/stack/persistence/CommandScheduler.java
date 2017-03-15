package myeslib3.stack.persistence;

import myeslib3.core.data.CommandScheduling;

@FunctionalInterface
public interface CommandScheduler {
  void schedule(String causeCommandId, CommandScheduling commandScheduling);
}
