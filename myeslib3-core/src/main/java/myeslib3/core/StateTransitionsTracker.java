package myeslib3.core;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Event;
import myeslib3.core.functions.DependencyInjectionFn;
import myeslib3.core.functions.StateTransitionFn;

public class StateTransitionsTracker<A extends AggregateRoot> {

  final A originalInstance;
  final StateTransitionFn<A> applyEventsFn;
  final DependencyInjectionFn<A> dependencyInjectionFn;
  final List<StateTransition<A>> stateTransitions;

  public StateTransitionsTracker(A originalInstance,
                                 StateTransitionFn<A> applyEventsFn,
                                 DependencyInjectionFn<A> dependencyInjectionFn) {
    this.originalInstance = originalInstance;
    this.applyEventsFn = applyEventsFn;
    this.dependencyInjectionFn = dependencyInjectionFn;
    this.stateTransitions = new ArrayList<>();
  }

  public StateTransitionsTracker<A> applyEvents(List<Event> events) {
    events.forEach(e -> {
      final A newInstance = applyEventsFn.apply(e, currentState());
      stateTransitions.add(new StateTransition<>(newInstance, e));
    });
    return this;
  }

  public List<Event> collectedEvents() {
    return stateTransitions.stream().map(t ->  t.afterThisEvent).collect(Collectors.toList());
  }

  public A currentState() {
    final A current = stateTransitions.size() == 0 ?
            originalInstance : stateTransitions.get(stateTransitions.size()-1).newInstance;
    return dependencyInjectionFn.inject(current);
  }

  class StateTransition<A> {
    private final A newInstance;
    private final Event afterThisEvent;
    StateTransition(A newInstance, Event afterThisEvent) {
      this.newInstance = newInstance;
      this.afterThisEvent = afterThisEvent;
    }
  }
}
