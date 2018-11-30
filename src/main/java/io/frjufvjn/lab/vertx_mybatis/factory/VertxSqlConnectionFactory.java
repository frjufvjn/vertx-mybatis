package io.frjufvjn.lab.vertx_mybatis.factory;

import java.io.IOException;
import java.util.Properties;

import org.apache.ibatis.io.Resources;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

public class VertxSqlConnectionFactory {

	private static Vertx vertx;
	private static JDBCClient jdbcClient;
	private static Properties prop = new Properties();

	static {
		try {
			prop.load(Resources.getResourceAsReader("config/db.properties"));

			vertx = Vertx.vertx();
			jdbcClient = JDBCClient.createShared(vertx, new JsonObject()
					.put("url", prop.getProperty("db.url"))
					.put("driver_class", prop.getProperty("db.driver"))
					.put("max_pool_size", Integer.parseInt(prop.getProperty("db.max_pool_size")))
					.put("user", prop.getProperty("db.username"))
					.put("password", prop.getProperty("db.password"))
					.put("max_idle_time", Integer.parseInt(prop.getProperty("db.max_idle_time")))
					.put("min_pool_size", Integer.parseInt(prop.getProperty("db.min_pool_size")))
					.put("acquire_retry_attempts", Integer.parseInt(prop.getProperty("db.acquire_retry_attempts")))
					);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static JDBCClient getClient() {
		return jdbcClient;
	}
}
