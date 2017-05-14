package myeslib3.example1.utils.config;

import org.aeonbits.owner.Config;

public interface BoundedContextConfig {

	@Config.DefaultValue("0/30 * * * * ?")
	@Config.Key("events_cron_polling")
	String events_cron_polling();

	@Config.DefaultValue("500")
	@Config.Key("events_max_rows_query")
	Integer events_max_rows_query();

	@Config.DefaultValue("3")
	@Config.Key("events_backoff_iddle_threshold")
	Integer events_backoff_iddle_threshold();

	@Config.DefaultValue("3")
	@Config.Key("events_backoff_failures_threshold")
	Integer events_backoff_failures_threshold();

  @Config.DefaultValue("3")
	@Config.Key("events_backoff_multiplier")
  Integer events_backoff_multiplier();

}
