package myeslib3.core.data;

import java.util.UUID;

public interface Command {

  UUID getCommandId();

  String getTargetId();
}
