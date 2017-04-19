package myeslib3.example1.aggregates.customer;

import lombok.val;
import myeslib3.core.AggregateRootCmdHandler;
import myeslib3.core.StateTransitionsTracker;
import myeslib3.core.data.Command;
import myeslib3.core.data.Event;
import myeslib3.core.data.UnitOfWork;
import myeslib3.core.data.Version;
import myeslib3.example1.aggregates.customer.commands.ActivateCustomerCmd;
import myeslib3.example1.aggregates.customer.commands.CreateActivateCustomerCmd;
import myeslib3.example1.aggregates.customer.commands.CreateCustomerCmd;
import myeslib3.example1.aggregates.customer.commands.DeactivateCustomerCmd;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;
import static myeslib3.core.data.UnitOfWork.create;
import static org.apache.commons.lang3.Validate.validState;

public class CustomerCmdHandler extends AggregateRootCmdHandler<Customer> {

  @Inject
  public CustomerCmdHandler(BiFunction<Event, Customer, Customer> stateTransitionFn, Function<Customer, Customer> dependencyInjectionFn) {
    super(stateTransitionFn, dependencyInjectionFn);
  }

  @Override
  public Optional<UnitOfWork> handle(Command cmd, Customer targetInstance, Version targetVersion) {

    final UnitOfWork uow = Match(cmd).of(

            Case(instanceOf(CreateCustomerCmd.class), (command) -> {
              validState(targetVersion.equals(Version.create(0)),
                      "before create the instance must be version= 0");
              return create(cmd, targetVersion.nextVersion(),
                      targetInstance.create(command.getTargetId(), command.getName()));
            }),

            Case(instanceOf(ActivateCustomerCmd.class), (command) ->
              create(cmd, targetVersion.nextVersion(), targetInstance.activate(command.getReason()))),

            Case(instanceOf(DeactivateCustomerCmd.class), (command) ->
              create(cmd, targetVersion.nextVersion(), targetInstance.deactivate(command.getReason()))),

            Case(instanceOf(CreateActivateCustomerCmd.class), (command) -> {
              val tracker = new StateTransitionsTracker<Customer>(targetInstance,
                      stateTransitionFn, dependencyInjectionFn);
              final List<Event> events = tracker
                      .applyEvents(targetInstance.create(cmd.getTargetId(), command.getName()))
                      .applyEvents(tracker.currentState().activate(command.getReason()))
                      .getEvents();
              return create(cmd, targetVersion.nextVersion(), events);
            })
    );

    return uow == null ? Optional.empty() : Optional.of(uow);

  }
}
