package myeslib3.stack1;

import myeslib3.stack1.infra.CamelConfig;
import myeslib3.stack1.infra.DatabaseConfig;
import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.Config;

public interface Stack1Config extends CamelConfig, DatabaseConfig, Config, Accessible {

}
