package io.frjufvjn.lab.vertx_mybatis.sql;

import java.util.List;
import java.util.Map;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.frjufvjn.lab.vertx_mybatis.factory.VertxSqlConnectionFactory;
import io.frjufvjn.lab.vertx_mybatis.query.QueryModule;
import io.frjufvjn.lab.vertx_mybatis.query.QueryServices;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;

public class SqlInnerServices {
	private static final Injector queryServices = Guice.createInjector(new QueryModule());

	public void sqlRead(JsonObject params, Handler<AsyncResult<List<JsonObject>>> aHandler) {
		try {
			Map<String, Object> queryInfo = queryServices.getInstance(QueryServices.class).getQuery(params);

			VertxSqlConnectionFactory.getClient().getConnection(conn -> {
				if (conn.failed()) aHandler.handle(Future.failedFuture(conn.cause()));

				try ( final SQLConnection connection = conn.result() ) {
					String sql = (String) queryInfo.get("sql");
					JsonArray param = (JsonArray)queryInfo.get("sqlParam");

					connection.queryWithParams(sql, param, ar -> {
						if(ar.succeeded()) {
							ar.result().getRows().toString();
							aHandler.handle(Future.succeededFuture(ar.result().getRows()));
						}
						else {
							aHandler.handle(Future.failedFuture(ar.cause()));
						}
					});
				}
			});

		} catch (Exception e) {
			aHandler.handle(Future.failedFuture(e));
		}
	}
}
