package myeslib3.stack1.infra;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.DefaultValue;

public interface DatabaseConfig {

  @DefaultValue("com.impossibl.postgres.jdbc.PGDriver")
  @Config.Key("database.driver")
  String db_driver();

  @DefaultValue("jdbc:pgsql://localhost:5432/dbname1?applicationName=MyApp&networkTimeout=10000")
  @Config.Key("database.url")
  String db_url();

  @DefaultValue("dbuser")
  @Config.Key("database.user")
  String db_user();

  @DefaultValue("dbuserpass")
  @Config.Key("database.password")
  String db_password();

  @DefaultValue("10")
  @Config.Key("database.pool.max.size")
  Integer db_max_pool_size();

  @DefaultValue("30000")
  @Config.Key("database.query.timeout.ms")
  Integer query_query_timeout_ms();

}
