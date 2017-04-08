package myeslib3.core;

import com.google.inject.Guice;
import com.google.inject.Injector;
import myeslib3.core.functions.DependencyInjectionFn;
import myeslib3.core.functions.StateTransitionFn;
import myeslib3.example1.core.aggregates.customer.Customer;
import myeslib3.example1.core.aggregates.customer.CustomerActivated;
import myeslib3.example1.core.aggregates.customer.CustomerCreated;
import myeslib3.examples.example1.runtime.CustomerModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("A StateTransitionsTrackerTest")
public class StateTransitionsTrackerTest {

  final Injector injector = Guice.createInjector(new CustomerModule());

  @Inject
  Supplier<Customer> supplier;
  @Inject
  DependencyInjectionFn<Customer> dependencyInjectionFn;
  @Inject
  StateTransitionFn<Customer> stateTransitionFn;

  StateTransitionsTracker<Customer> tracker;

  @BeforeEach
  void instantiate() {
    injector.injectMembers(this);
  }

  @Test
  public void can_be_instantiated() {
    injector.injectMembers(this);
    new StateTransitionsTracker<>(supplier.get(), stateTransitionFn, dependencyInjectionFn);
  }

  @Nested
  @DisplayName("when new")
  public class WhenIsNew {

    @BeforeEach
    void instantiate() {
      tracker = new StateTransitionsTracker<>(supplier.get(), stateTransitionFn, dependencyInjectionFn);
    }

    @Test
    void is_empty() {
      assertThat(tracker.isEmpty()).isTrue();
    }

    @Test
    void has_empty_state() {
      assertThat(tracker.currentState()).isEqualTo(supplier.get());
    }

    @Nested
    @DisplayName("when adding a create customer event")
    public class WhenAddingNewEvent {

      private CustomerCreated createdEvent = new CustomerCreated("c1","customer-1");
      private Customer expectedCustomer = new Customer("c1", "customer-1", false,
              null, null, null);

      @BeforeEach
      void apply_create_event() {
        tracker.applyEvents(Arrays.asList(createdEvent));
      }

      @Test
      void has_new_state() {
        assertThat(tracker.currentState()).isEqualTo(expectedCustomer);
      }

      @Test
      void has_only_create_event() {
        assertThat(tracker.collectedEvents()).contains(createdEvent);
        assertThat(tracker.collectedEvents().size()).isEqualTo(1);
      }

      @Nested
      @DisplayName("when adding an activate customer event")
      public class WhenAddingActivateEvent {

        private CustomerActivated customerActivated = new CustomerActivated("is ok", LocalDateTime.now());
        private Customer expectedCustomer = new Customer("c1", "customer-1", true,
                customerActivated.getDate(), null, customerActivated.getReason());

        @BeforeEach
        void apply_activate_event() {
          tracker.applyEvents(Arrays.asList(customerActivated));
        }

        @Test
        void has_new_state() {
          assertThat(tracker.currentState()).isEqualTo(expectedCustomer);
        }

        @Test
        void has_both_create_and_activated_evenst() {
          assertThat(tracker.collectedEvents().get(0)).isEqualTo(createdEvent);
          assertThat(tracker.collectedEvents().get(1)).isEqualTo(customerActivated);
          assertThat(tracker.collectedEvents().size()).isEqualTo(2);
        }

      }

    }

  }

}