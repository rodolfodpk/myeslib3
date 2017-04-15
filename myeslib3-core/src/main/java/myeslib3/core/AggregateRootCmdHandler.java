package myeslib3.core;

import lombok.AllArgsConstructor;
import myeslib3.core.data.*;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

@AllArgsConstructor
public abstract class AggregateRootCmdHandler<A extends AggregateRoot> {

  protected final BiFunction<Event, A, A> stateTransitionFn;
  protected final Function<A, A> dependencyInjectionFn;

  public abstract Optional<UnitOfWork> handle(Command cmd, A targetInstance, Version targetVersion);

}
