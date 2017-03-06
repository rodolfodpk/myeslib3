package myeslib3.core;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import myeslib3.core.data.AggregateRoot;
import myeslib3.core.data.Event;
import myeslib3.core.functions.DependencyInjectionFn;
import myeslib3.core.functions.StateTransitionFn;

public class StateTransitionsTracker<AggregateRoot, Event> {

  private final AggregateRoot originalInstance;
  final StateTransitionFn<AggregateRoot, Event> applyEventsFn;
  final DependencyInjectionFn<AggregateRoot> dependencyInjectionFn;
  final List<Tuple2<AggregateRoot, Event>> stateTransitions;

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
      AggregateRoot newInstance = applyEventsFn.apply(e, currentState());
      stateTransitions.add(new Tuple2<>(newInstance, e));
    });
    return this;
  }

  public List<Event> collectedEvents() {
    return stateTransitions.stream().map(t ->  t.event).collect(Collectors.toList());
  }

  public AggregateRoot currentState() {
    AggregateRoot current = stateTransitions.size() == 0 ?
            originalInstance : stateTransitions.get(stateTransitions.size()-1).arInstance;
    return dependencyInjectionFn.inject(current);
  }

  class Tuple2<AggregateRoot, E> {
    private final AggregateRoot arInstance;
    private final E event;
    Tuple2(AggregateRoot arInstance, E event) {
      this.arInstance = arInstance;
      this.event = event;
    }
  }
}
