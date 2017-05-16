package myeslib3.example1.aggregates.customer;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import myeslib3.core.model.Event;
import org.junit.jupiter.api.DisplayName;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@DisplayName("A Customer")
public class CustomerTest {

  final Injector injector = Guice.createInjector(new CustomerModule());

  @Inject
  Supplier<Customer> supplier;
  @Inject
  Function<Customer, Customer> dependencyInjectionFn;
  @Inject
  BiFunction<Event, Customer, Customer> stateTransitionFn;

}