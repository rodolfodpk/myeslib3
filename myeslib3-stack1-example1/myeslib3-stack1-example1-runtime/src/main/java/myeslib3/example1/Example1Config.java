package myeslib3.example1;

import myeslib3.example1.utils.config.BoundedContextConfig;
import myeslib3.example1.utils.config.CamelConfig;
import myeslib3.example1.utils.config.DatabaseConfig;
import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.Config;

public interface Example1Config extends CamelConfig, DatabaseConfig, BoundedContextConfig, Config, Accessible {

}
