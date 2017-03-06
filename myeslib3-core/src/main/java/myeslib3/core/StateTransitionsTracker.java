package myeslib3.core;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import myeslib3.core.functions.DependencyInjectionFn;
import myeslib3.core.functions.StateTransitionFn;

public class StateTransitionsTracker<AggregateRoot, Event> {

  final AggregateRoot originalInstance;
  final StateTransitionFn<AggregateRoot, Event> applyEventsFn;
  final DependencyInjectionFn<AggregateRoot> dependencyInjectionFn;
  final List<StateTransition<AggregateRoot, Event>> stateTransitions;

  public StateTransitionsTracker(AggregateRoot originalInstance,
                                 StateTransitionFn<AggregateRoot, Event> applyEventsFn,
                                 DependencyInjectionFn<AggregateRoot> dependencyInjectionFn) {
    this.originalInstance = originalInstance;
    this.applyEventsFn = applyEventsFn;
    this.dependencyInjectionFn = dependencyInjectionFn;
    this.stateTransitions = new ArrayList<>();
  }

  public StateTransitionsTracker<AggregateRoot, Event> applyEvents(List<Event> events) {
    events.forEach(e -> {
      final AggregateRoot newInstance = applyEventsFn.apply(e, currentState());
      stateTransitions.add(new StateTransition<>(newInstance, e));
    });
    return this;
  }

  public List<Event> collectedEvents() {
    return stateTransitions.stream().map(t ->  t.afterThisEvent).collect(Collectors.toList());
  }

  public AggregateRoot currentState() {
    final AggregateRoot current = stateTransitions.size() == 0 ?
            originalInstance : stateTransitions.get(stateTransitions.size()-1).newInstance;
    return dependencyInjectionFn.inject(current);
  }

  class StateTransition<AggregateRoot, E> {
    private final AggregateRoot newInstance;
    private final E afterThisEvent;
    StateTransition(AggregateRoot newInstance, E afterThisEvent) {
      this.newInstance = newInstance;
      this.afterThisEvent = afterThisEvent;
    }
  }
}
