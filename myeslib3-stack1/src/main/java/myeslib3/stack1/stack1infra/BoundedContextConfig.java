package myeslib3.stack1.stack1infra;

import org.aeonbits.owner.Config;

public interface BoundedContextConfig {

	@Config.DefaultValue("0/30 * * * * ?")
	String events_cron_polling();

	@Config.DefaultValue("500")
	Integer events_max_rows_query();

	default String camelized(String cron) {
		return cron.replace(' ', '+');
	}

}
