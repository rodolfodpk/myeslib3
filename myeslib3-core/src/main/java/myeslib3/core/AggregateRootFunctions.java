package myeslib3.core;


import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Command;
import myeslib3.core.data.Event;
import myeslib3.core.functions.CommandHandlerFn;
import myeslib3.core.functions.StateTransitionFn;

public interface AggregateRootFunctions {

  CommandHandlerFn<AggregateRoot, Command> commandHandlerFn();

  StateTransitionFn<AggregateRoot, Event> stateTransitionFn();

}
