package myeslib3.core.functions;

import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Command;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;

@FunctionalInterface
public interface CommandHandlerFn<AGGREGATE_ROOT extends AggregateRoot, COMMAND extends Command> {

  UnitOfWork handle(String commandId, COMMAND command,
                    String targetId, AGGREGATE_ROOT targetInstance, Version targetVersion,
                    WriteModelStateTransitionFn<AGGREGATE_ROOT> writeModelStateTransitionFn,
                    DependencyInjectionFn<AGGREGATE_ROOT> dependencyInjectionFn);

}
