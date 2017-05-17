package myeslib3.example1;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.val;
import myeslib3.core.model.AggregateRootId;
import myeslib3.core.model.Command;
import myeslib3.core.model.Event;
import myeslib3.example1.aggregates.customer.CustomerId;
import myeslib3.example1.aggregates.customer.commands.ActivateCustomerCmd;
import myeslib3.example1.aggregates.customer.commands.CreateActivateCustomerCmd;
import myeslib3.example1.aggregates.customer.commands.CreateCustomerCmd;
import myeslib3.example1.aggregates.customer.commands.DeactivateCustomerCmd;
import myeslib3.example1.aggregates.customer.events.CustomerActivated;
import myeslib3.example1.aggregates.customer.events.CustomerCreated;
import myeslib3.example1.aggregates.customer.events.CustomerDeactivated;
import myeslib3.example1.aggregates.customer.events.DeactivatedCmdScheduled;
import myeslib3.example1.services.SampleService;
import myeslib3.example1.services.SampleServiceImpl;
import myeslib3.example1.utils.gson.RuntimeTypeAdapterFactory;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;

import java.util.Properties;

public class Example1Module extends AbstractModule {

  @Override
  protected void configure() {

    bind(SampleService.class).to(SampleServiceImpl.class).asEagerSingleton();

    final Config config = ConfigFactory.load();
    final Properties props =  new Properties();

    config.entrySet().forEach(e -> {
      final String key = e.getKey().replace("myeslib3-stack1.", "");
      final String value = e.getValue().render().replace("\"", "");
//      System.out.println(key + "=" + value);
      props.put(key, value);
    });

    Names.bindProperties(binder(), props);

  }

  @Provides
  @Singleton
  Gson gson() {

    RuntimeTypeAdapterFactory<AggregateRootId> rtaIds  = RuntimeTypeAdapterFactory.of(AggregateRootId.class)
            .registerSubtype(CustomerId.class);

    RuntimeTypeAdapterFactory<Command> rtaCommands  = RuntimeTypeAdapterFactory.of(Command.class)
            .registerSubtype(CreateCustomerCmd.class)
            .registerSubtype(ActivateCustomerCmd.class)
            .registerSubtype(DeactivateCustomerCmd.class)
            .registerSubtype(CreateActivateCustomerCmd.class);

    RuntimeTypeAdapterFactory<Event> rtaEvents = RuntimeTypeAdapterFactory.of(Event.class)
            .registerSubtype(CustomerCreated.class)
            .registerSubtype(CustomerActivated.class)
            .registerSubtype(CustomerDeactivated.class)
            .registerSubtype(DeactivatedCmdScheduled.class);

    val gsonBuilder = new GsonBuilder();

    gsonBuilder.setPrettyPrinting();
    gsonBuilder.registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory());
    gsonBuilder.registerTypeAdapterFactory(rtaIds);
    gsonBuilder.registerTypeAdapterFactory(rtaCommands);
    gsonBuilder.registerTypeAdapterFactory(rtaEvents);

    gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES); // snake case

    val gson = gsonBuilder.create();

    return gson;
  }

}
