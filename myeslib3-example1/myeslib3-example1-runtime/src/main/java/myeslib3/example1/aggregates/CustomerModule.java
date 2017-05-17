package myeslib3.example1.aggregates;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import myeslib3.core.model.AggregateRootCmdHandler;
import myeslib3.core.model.Event;
import myeslib3.example1.aggregates.customer.Customer;
import myeslib3.example1.aggregates.customer.CustomerCmdHandler;
import myeslib3.example1.aggregates.customer.CustomerStateTransitionFn;
import myeslib3.example1.services.SampleServiceImpl;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class CustomerModule extends AbstractModule {

  @Override
  protected void configure() {

  }

  @Provides
  @Singleton
  Supplier<Customer> supplier() {
    return () -> new Customer(null, null,  null, false, null);
  }

  @Provides
  @Singleton
  Function<Customer, Customer> dependencyInjectionFn(final SampleServiceImpl service) {
    return (c) -> c.withService(service);
  }

  @Provides
  @Singleton
  BiFunction<Event, Customer, Customer> stateTransitionFn(final SampleServiceImpl service) {
    return new CustomerStateTransitionFn();
  }

  @Provides
  @Singleton
  AggregateRootCmdHandler<Customer> cmdHandler(final BiFunction<Event, Customer, Customer> stateTransFn,
                                               final Function<Customer, Customer> depInjectionFn) {
    return new CustomerCmdHandler(stateTransFn, depInjectionFn);
  }

}
