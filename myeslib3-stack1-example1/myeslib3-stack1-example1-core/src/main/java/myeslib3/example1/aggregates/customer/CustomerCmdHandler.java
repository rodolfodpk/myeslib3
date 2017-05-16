package myeslib3.example1.aggregates.customer;

import javaslang.API;
import lombok.val;
import myeslib3.core.UnitOfWork;
import myeslib3.core.Version;
import myeslib3.core.model.AggregateRootCmdHandler;
import myeslib3.core.model.Command;
import myeslib3.core.model.Event;
import myeslib3.core.stack.StateTransitionsTracker;
import myeslib3.example1.aggregates.customer.commands.ActivateCustomerCmd;
import myeslib3.example1.aggregates.customer.commands.CreateActivateCustomerCmd;
import myeslib3.example1.aggregates.customer.commands.CreateCustomerCmd;
import myeslib3.example1.aggregates.customer.commands.DeactivateCustomerCmd;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static javaslang.API.*;
import static javaslang.Predicates.instanceOf;
import static myeslib3.core.UnitOfWork.of;

public class CustomerCmdHandler extends AggregateRootCmdHandler<Customer> {

  @Inject
  public CustomerCmdHandler(BiFunction<Event, Customer, Customer> stateTransitionFn, Function<Customer, Customer> dependencyInjectionFn) {
    super(stateTransitionFn, dependencyInjectionFn);
  }

  @Override
  public Optional<UnitOfWork> handle(Command cmd, Customer targetInstance, Version targetVersion) {

    final UnitOfWork uow = Match(cmd).of(

      API.Case(instanceOf(CreateCustomerCmd.class), (command) ->
        of(cmd, targetVersion.nextVersion(),
                targetInstance.create(command.getTargetId(), command.getName()))
      ),

      API.Case(instanceOf(ActivateCustomerCmd.class), (command) ->
        of(cmd, targetVersion.nextVersion(), targetInstance.activate(command.getReason()))),

      API.Case(instanceOf(DeactivateCustomerCmd.class), (command) ->
        of(cmd, targetVersion.nextVersion(), targetInstance.deactivate(command.getReason()))),

      API.Case(instanceOf(CreateActivateCustomerCmd.class), (command) -> {
        val tracker = new StateTransitionsTracker<Customer>(targetInstance,
                stateTransitionFn, dependencyInjectionFn);
        final List<Event> events = tracker
                .applyEvents(targetInstance.create((CustomerId) cmd.getTargetId(), command.getName()))
                .applyEvents(tracker.currentState().activate(command.getReason()))
                .getEvents();
        return of(cmd, targetVersion.nextVersion(), events);
      }),

      Case($(), o -> null)

    );

    return uow == null ? Optional.empty() : Optional.of(uow);

  }
}
