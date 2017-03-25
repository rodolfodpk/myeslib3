package myeslib3.stack1.stack1infra.utils;

import com.google.inject.Binder;
import com.google.inject.name.Names;
import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.Config;

import java.util.Properties;
import java.util.stream.Stream;

public class ConfigHelper {

  @SuppressWarnings({"unchecked", "deprecation"})
  public static void overrideConfigPropsWithSystemVars(
          final Binder binder, final Accessible config) {

    // publish configs into properties
    Properties properties = new Properties();
    config.fill(properties);
    Names.bindProperties(binder, properties);

    // now bind all configs
    Class[] interfaces = config.getClass().getInterfaces();
    Stream.of(interfaces)
            .filter((aClass) -> !aClass.equals(Config.class))
            .filter((aClass) -> !aClass.equals(Accessible.class))
            .forEach((aClass) -> {
              binder.bind(aClass).toInstance(config);
            });
  }

}
