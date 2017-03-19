package myeslib3.stack1;

import com.google.inject.AbstractModule;
import org.aeonbits.owner.ConfigCache;

import static myeslib3.stack1.infra.utils.ConfigHelper.overrideConfigPropsWithSystemVars;

public class Stack1Module extends AbstractModule{

	@Override
	protected void configure() {

		Stack1Config config =
						ConfigCache.getOrCreate(Stack1Config.class, System.getProperties(), System.getenv());
		bind(Stack1Config.class).toInstance(config);
		overrideConfigPropsWithSystemVars(binder(), config);

	}

}
