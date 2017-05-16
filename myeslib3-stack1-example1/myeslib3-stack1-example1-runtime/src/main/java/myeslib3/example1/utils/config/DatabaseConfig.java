//package myeslib3.example1.utils.config;
//
//import org.aeonbits.owner.Config;
//import org.aeonbits.owner.Config.DefaultValue;
//
//public interface DatabaseConfig {
//
//  @DefaultValue("com.mysql.jdbc.Driver")
//  @Config.Key("database.driver")
//  String db_driver();
//
//  @DefaultValue("jdbc:mysql://localhost:3306/example1db?serverTimezone=UTC")
//  @Config.Key("database.url")
//  String db_url();
//
//  @DefaultValue("root")
//  @Config.Key("database.user")
//  String db_user();
//
//  @DefaultValue("my-secret-pw")
//  @Config.Key("database.password")
//  String db_password();
//
//  @DefaultValue("10")
//  @Config.Key("database.pool.max.size")
//  Integer db_max_pool_size();
//
//  @DefaultValue("30000")
//  @Config.Key("database.query.timeout.ms")
//  Integer query_query_timeout_ms();
//
//  @DefaultValue("dbname1")
//  @Config.Key("database.name")
//  String db_name();
//
//  @DefaultValue("localhost")
//  @Config.Key("database.host")
//  String db_host();
//
//  @DefaultValue("3306")
//  @Config.Key("database.port")
//  int db_port();
//
//}
