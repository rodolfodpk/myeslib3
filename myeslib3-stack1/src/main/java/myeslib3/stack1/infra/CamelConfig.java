package myeslib3.stack1.infra;

import org.aeonbits.owner.Config;

public interface CamelConfig {

	@Config.DefaultValue("camel-context-1")
	String camel_ctx_name();

	@Config.DefaultValue("false")
	boolean camel_tracer_enabled();

}
