package io.frjufvjn.lab.vertx_mybatis.sql;

import java.util.Map;

import com.google.inject.Guice;

import io.frjufvjn.lab.vertx_mybatis.common.ApiCommon;
import io.frjufvjn.lab.vertx_mybatis.factory.VertxSqlConnectionFactory;
import io.frjufvjn.lab.vertx_mybatis.query.QueryModule;
import io.frjufvjn.lab.vertx_mybatis.query.QueryServices;
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.RoutingContext;

public class SqlServiceImp extends ApiCommon implements SqlServices {

	@Override
	public void sqlApiRead(RoutingContext ctx) throws Exception {
		try {
			Map<String, Object> queryInfo = Guice.createInjector(new QueryModule()).getInstance(QueryServices.class).getQuery(ctx);
			VertxSqlConnectionFactory.getClient().getConnection(conn -> {		
				if (conn.failed()) serviceUnavailable(ctx);

				try ( final SQLConnection connection = conn.result() ) {
					String sql = (String) queryInfo.get("sql");
					JsonArray param = (JsonArray)queryInfo.get("sqlParam");

					connection.queryWithParams(sql, param, ar -> {
						if(ar.succeeded()) {
							sendReadResult(ctx, ar);
						} else {
							internalError(ctx, ar.cause().getMessage());
						}
					});
				}
			});
		} catch ( IllegalArgumentException iae ) {
			sendNotFoundSqlName(ctx);
		} catch ( IllegalStateException ise ) {
			internalError(ctx, ise);
		}
	}

	@Override
	public void sqlApiCreate(RoutingContext ctx) throws Exception {
		executeSqlCUD(ctx);
	}

	@Override
	public void sqlApiUpdate(RoutingContext ctx) throws Exception {
		executeSqlCUD(ctx);
	}

	@Override
	public void sqlApiDelete(RoutingContext ctx) throws Exception {
		executeSqlCUD(ctx);
	}

	private void executeSqlCUD(RoutingContext ctx) throws Exception {
		try {
			Map<String, Object> queryInfo = Guice.createInjector(new QueryModule()).getInstance(QueryServices.class).getQuery(ctx);
			VertxSqlConnectionFactory.getClient().getConnection(conn -> {
				if (conn.failed()) serviceUnavailable(ctx);

				try ( final SQLConnection connection = conn.result() ) {
					String sql = (String) queryInfo.get("sql");
					JsonArray param = (JsonArray)queryInfo.get("sqlParam");

					connection.updateWithParams(sql, param, ar -> {
						if (ar.succeeded()) {
							ok(ctx, ar.result().getUpdated() + ":" + ar.result().getKeys());
						} else {
							internalError(ctx, ar.cause().getMessage());
						}
					});
				}
			});
		} catch ( IllegalArgumentException iae ) {
			sendNotFoundSqlName(ctx);
		} catch ( IllegalStateException ise ) {
			internalError(ctx, ise);
		}
	}

	private void sendReadResult(RoutingContext ctx, AsyncResult<ResultSet> ar) {
		if ( ar.result().getRows().size() > 0 ) {
			ok(ctx, ar.result().getRows().toString());
		} else {
			noContent(ctx);
		}
	}

	private void sendNotFoundSqlName(RoutingContext ctx) {
		internalError(ctx, "The sqlName parameter is empty or the service can not be found in the service list (mybatis mapper).");
	}
}
