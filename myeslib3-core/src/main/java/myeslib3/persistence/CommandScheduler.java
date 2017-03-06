package myeslib3.persistence;

import myeslib3.core.data.CommandScheduling;

@FunctionalInterface
public interface CommandScheduler {
  void schedule(String causeCommandId , CommandScheduling commandScheduling);
}
