package myeslib3.stack1;

import myeslib3.stack1.stack1infra.BoundedContextConfig;
import myeslib3.stack1.stack1infra.CamelConfig;
import myeslib3.stack1.stack1infra.DatabaseConfig;
import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.Config;

public interface Stack1Config extends CamelConfig, DatabaseConfig, BoundedContextConfig, Config, Accessible {

}
