package myeslib3.core.functions;

import com.spencerwi.either.Result;
import myeslib3.core.data.Command;
import myeslib3.core.data.Event;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;

@FunctionalInterface
public interface CommandHandlerFn<AggregateRoot, C extends Command> {

  Result<UnitOfWork> handle(String commandId,
                            String aggregateRootId,
                            AggregateRoot aggregateRoot,
                            Version version,
                            C command,
                            StateTransitionFn<AggregateRoot, Event> stateTransitionFn,
                            DependencyInjectionFn<AggregateRoot> dependencyInjectionFn);
}
