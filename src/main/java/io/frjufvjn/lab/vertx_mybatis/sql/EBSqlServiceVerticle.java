package io.frjufvjn.lab.vertx_mybatis.sql;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.frjufvjn.lab.vertx_mybatis.Constants;
import io.frjufvjn.lab.vertx_mybatis.factory.VertxSqlConnectionFactory;
import io.frjufvjn.lab.vertx_mybatis.query.QueryModule;
import io.frjufvjn.lab.vertx_mybatis.query.QueryServices;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.sql.SQLConnection;

public class EBSqlServiceVerticle extends AbstractVerticle {
	private final Logger logger = LogManager.getLogger(EBSqlServiceVerticle.class);
	private Injector services = null;

	@Override
	public void start(Future<Void> startFuture) throws Exception {

		services = Guice.createInjector(new QueryModule());

		EventBus eb = vertx.eventBus();
		eb.consumer(Constants.EVENTBUS_SQL_VERTICLE_ADDR, msg -> {

			try {
				JsonObject params = (JsonObject) msg.body();
				Map<String, Object> queryInfo = services.getInstance(QueryServices.class).getQuery(params);

				/**
				 * @description This method is Asynchronous way even if there is a delay.
				 * */
				VertxSqlConnectionFactory.getClient().getConnection(conn -> {
					if (conn.failed()) msg.reply(Constants.EVENTBUS_SQL_VERTICLE_FAIL_SIGNAL);

					try ( final SQLConnection connection = conn.result() ) {
						String sql = (String) queryInfo.get("sql");
						JsonArray param = (JsonArray)queryInfo.get("sqlParam");

						connection.queryWithParams(sql, param, ar -> {
							if(ar.succeeded()) msg.reply(ar.result().getRows().toString() );
							else msg.reply(Constants.EVENTBUS_SQL_VERTICLE_FAIL_SIGNAL);
						});
					}
				});

				/**
				 * @description NOTE : There is no Asynchronous effect and it is sequential. 
				 * VertxSqlConnectionFactory.getClient().query((String)queryInfo.get("sql"), ar -> {
						if (ar.succeeded()) {
							msg.reply(ar.result().getRows().toString() );
						}
					});
				 * */

			} catch (Exception e) {
				msg.reply("fail");
			}

		}).completionHandler(ar -> {
			startFuture.complete();
			logger.info("EBSqlServiceVerticle EventBus Ready!! (address:"+Constants.EVENTBUS_SQL_VERTICLE_ADDR+")");
		});
	}
}
