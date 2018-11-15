package io.frjufvjn.lab.vertx_mybatis;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.frjufvjn.lab.vertx_mybatis.factory.VertxSqlConnectionFactory;
import io.frjufvjn.lab.vertx_mybatis.query.QueryModule;
import io.frjufvjn.lab.vertx_mybatis.query.QueryServices;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class TemplateDatabaseVerticle extends AbstractVerticle {

	private final Logger logger = LogManager.getLogger(TemplateDatabaseVerticle.class);
	private int HTTP_SERVER_PORT = 8080;
	private Injector injector = null;

	@Override
	public void start(Future<Void> startFuture) throws Exception {

		final Router router = Router.router(vertx);
		final Route svc = router.route(HttpMethod.GET, "/svc/:svcnm/:param1/:param2");

		injector = Guice.createInjector(new QueryModule());

		svc.handler(this::dbSearchHandler);



		/**
		 * @description Create HTTP Server
		 * */
		vertx.createHttpServer().requestHandler(router::accept).listen(HTTP_SERVER_PORT, res -> {
			if (res.failed()) {
				// res.cause().printStackTrace();
				startFuture.fail(res.cause());
			} else {
				startFuture.complete();
				logger.info("Server listening at: http://localhost:8080/");
			}
		});
	}



	/**
	 * @description DB Search Handler
	 * @param ctx
	 */
	private void dbSearchHandler(RoutingContext ctx) {

		HttpServerResponse response = ctx.response();
		Map<String, Object> reqData = new LinkedHashMap<String, Object>();

		String serviceName = ctx.request().getParam("svcnm");
		String param1 = ctx.request().getParam("param1");
		String param2 = ctx.request().getParam("param2");

		reqData.put("sqlName", serviceName);
		reqData.put("email", param1);
		reqData.put("password", param2);




		/**
		 * @description Async SQL Using Vertx SQL 
		 * */
		VertxSqlConnectionFactory.getClient().getConnection(conn -> {

			if (conn.failed()) {
				logger.error(conn.cause().getMessage());
			}

			try {

				final SQLConnection connection = conn.result();



				// Get Query Using MyBatis ORM Framework
				Map<String, Object> queryInfo = injector.getInstance(QueryServices.class).getQuery(reqData);



				// TODO 리팩토링 필요 (참고 : https://github.com/sczyh30/vertx-blueprint-todo-backend)
				// Vertx SQL Async & Non-Block (Prepared Statement)
				connection.queryWithParams(
						(String) queryInfo.get("sql"),
						(JsonArray)queryInfo.get("sqlParam"), res -> {

							try {
								if ( res.failed() ) {
									logger.error(res.cause().getMessage());
								}

								// just print result
								for (JsonArray line : res.result().getResults()) {
									logger.info(line.encode());
								}

								response.putHeader("Content-Type", "application/json");
								response.setStatusCode(200);
								response.end( res.result().getResults().toString() );

							} catch (Exception e) {
								response.putHeader("Content-Type", "application/json");
								response.setStatusCode(500);
								response.end("SQL Service Error");
								throw new RuntimeException(e);
							} finally {
								// and close the connection
								connection.close(done -> {
									if (done.failed()) {
										throw new RuntimeException(done.cause());
									}
								});
							}
						});
			} catch (Exception e2) {
				response.putHeader("Content-Type", "application/json");
				response.setStatusCode(500);
				response.end("SQL Service Error");
				throw new RuntimeException(e2);
			}
		});
	}
}