package myeslib3.core.command;

import com.spencerwi.either.Result;
import myeslib3.core.AggregateRoot;
import myeslib3.core.Command;
import myeslib3.core.UnitOfWork;
import myeslib3.core.Version;

@FunctionalInterface
public interface CommandHandlerFn<AGGREGATE_ROOT extends AggregateRoot, COMMAND extends Command> {

  Result<UnitOfWork> handle(String commandId, COMMAND command,
                            String targetId, AGGREGATE_ROOT targetInstance, Version targetVersion,
                            WriteModelStateTransitionFn<AGGREGATE_ROOT> writeModelStateTransitionFn,
                            DependencyInjectionFn<AGGREGATE_ROOT> dependencyInjectionFn);

}
