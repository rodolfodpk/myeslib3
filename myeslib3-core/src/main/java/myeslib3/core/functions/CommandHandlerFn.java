package myeslib3.core.functions;

import com.spencerwi.either.Result;
import myeslib3.core.StateTransitionsTracker;
import myeslib3.core.data.*;

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
