package myeslib3.core.command;

import myeslib3.core.Command;

import java.time.LocalDateTime;

public interface CommandScheduling {

	Command scheduledCommand();

	LocalDateTime scheduledAt();

}
