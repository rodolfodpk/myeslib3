package myeslib3.stack1.command;

import myeslib3.core.command.CommandScheduling;

@FunctionalInterface
public interface CommandScheduler {

	void schedule(String causeCommandId, CommandScheduling commandScheduling);

}
