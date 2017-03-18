package myeslib3.core.functions;

import com.spencerwi.either.Result;
import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Command;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;

@FunctionalInterface
public interface CommandHandlerFn<A extends AggregateRoot, C extends Command> {

  Result<UnitOfWork> handle(String commandId, C command,
                            String targetId, A targetInstance, Version targetVersion,
                            StateTransitionFn<A> stateTransitionFn,
                            DependencyInjectionFn<A> dependencyInjectionFn);

}
