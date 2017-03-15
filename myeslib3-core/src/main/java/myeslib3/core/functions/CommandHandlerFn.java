package myeslib3.core.functions;

import com.spencerwi.either.Result;
import myeslib3.core.data.*;

@FunctionalInterface
public interface CommandHandlerFn<A extends AggregateRoot, C extends Command> {

  Result<UnitOfWork> handle(String commandId, C command,
                            String targetId, A targetInstance, Version targetVersion,
                            StateTransitionFn<A> stateTransitionFn,
                            DependencyInjectionFn<A> dependencyInjectionFn);

}
