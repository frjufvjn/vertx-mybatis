package io.frjufvjn.lab.vertx_mybatis.sql;

import java.util.List;
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

/**
 * @author PJW
 *
 */
public class SqlServiceImp extends ApiCommon implements SqlServices {

	/* (non-Javadoc)
	 * @see io.frjufvjn.lab.vertx_mybatis.sql.SqlServices#sqlApiRead(io.vertx.ext.web.RoutingContext)
	 */
	@Override
	public void sqlApiRead(RoutingContext ctx) throws Exception {
		executeSqlCommonRead(ctx);
	}

	/* (non-Javadoc)
	 * @see io.frjufvjn.lab.vertx_mybatis.sql.SqlServices#sqlApiCreate(io.vertx.ext.web.RoutingContext)
	 */
	@Override
	public void sqlApiCreate(RoutingContext ctx) throws Exception {
		executeSqlCommonCUD(ctx);
	}

	/* (non-Javadoc)
	 * @see io.frjufvjn.lab.vertx_mybatis.sql.SqlServices#sqlApiUpdate(io.vertx.ext.web.RoutingContext)
	 */
	@Override
	public void sqlApiUpdate(RoutingContext ctx) throws Exception {
		executeSqlCommonCUD(ctx);
	}

	/* (non-Javadoc)
	 * @see io.frjufvjn.lab.vertx_mybatis.sql.SqlServices#sqlApiDelete(io.vertx.ext.web.RoutingContext)
	 */
	@Override
	public void sqlApiDelete(RoutingContext ctx) throws Exception {
		executeSqlCommonCUD(ctx);
	}

	/* (non-Javadoc)
	 * @see io.frjufvjn.lab.vertx_mybatis.sql.SqlServices#sqlApiCreateBatch(io.vertx.ext.web.RoutingContext)
	 */
	@Override
	public void sqlApiCreateBatch(RoutingContext ctx) throws Exception {
		executeSqlCommonBatchCUD(ctx);
	}

	/* (non-Javadoc)
	 * @see io.frjufvjn.lab.vertx_mybatis.sql.SqlServices#sqlApiUpdateBatch(io.vertx.ext.web.RoutingContext)
	 */
	@Override
	public void sqlApiUpdateBatch(RoutingContext ctx) throws Exception {
		executeSqlCommonBatchCUD(ctx);
	}

	/* (non-Javadoc)
	 * @see io.frjufvjn.lab.vertx_mybatis.sql.SqlServices#sqlApiDeleteBatch(io.vertx.ext.web.RoutingContext)
	 */
	@Override
	public void sqlApiDeleteBatch(RoutingContext ctx) throws Exception {
		executeSqlCommonBatchCUD(ctx);
	}

	/**
	 * @param ctx
	 * @throws Exception
	 */
	private void executeSqlCommonRead(RoutingContext ctx) throws Exception {
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

	/**
	 * @param ctx
	 * @throws Exception
	 */
	private void executeSqlCommonCUD(RoutingContext ctx) throws Exception {
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

	/**
	 * @param ctx
	 * @throws Exception
	 */
	private void executeSqlCommonBatchCUD(RoutingContext ctx) throws Exception {
		try {
			Map<String, Object> queryInfo = Guice.createInjector(new QueryModule()).getInstance(QueryServices.class).getQueryWithoutParam(ctx);

			VertxSqlConnectionFactory.getClient().getConnection(conn -> {		
				if (conn.failed()) serviceUnavailable(ctx);

				try ( final SQLConnection connection = conn.result() ) {
					String sql = (String) queryInfo.get("sql");
					@SuppressWarnings("unchecked")
					List<JsonArray> batch = (List<JsonArray>) queryInfo.get("batchParam");

					connection.batchWithParams(sql, batch, ar -> {
						if (ar.succeeded()) {
							ok(ctx);
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

	/**
	 * @param ctx
	 * @param ar
	 */
	private void sendReadResult(RoutingContext ctx, AsyncResult<ResultSet> ar) {
		if ( ar.result().getRows().size() > 0 ) {
			ok(ctx, ar.result().getRows().toString());
		} else {
			noContent(ctx);
		}
	}

	/**
	 * @param ctx
	 */
	private void sendNotFoundSqlName(RoutingContext ctx) {
		internalError(ctx, "The sqlName parameter is empty or the service can not be found in the service list (mybatis mapper).");
	}
}
