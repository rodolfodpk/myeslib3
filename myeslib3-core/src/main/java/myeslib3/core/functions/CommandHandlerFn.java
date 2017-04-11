package myeslib3.core.functions;

import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Command;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;

import java.util.Optional;

@FunctionalInterface
public interface CommandHandlerFn<A extends AggregateRoot, C extends Command> {

  Optional<UnitOfWork> handleCommand(C command,
                                     String targetId, A targetInstance, Version targetVersion,
                                     StateTransitionFn<A> stateTransitionFn,
                                     DependencyInjectionFn<A> dependencyInjectionFn);

}
