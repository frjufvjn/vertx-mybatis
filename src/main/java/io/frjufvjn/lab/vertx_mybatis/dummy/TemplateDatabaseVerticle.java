package io.frjufvjn.lab.vertx_mybatis.dummy;

import java.io.File;
import java.io.FileOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.frjufvjn.lab.vertx_mybatis.factory.VertxSqlConnectionFactory;
import io.frjufvjn.lab.vertx_mybatis.query.QueryModule;
import io.frjufvjn.lab.vertx_mybatis.query.QueryServices;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.Pump;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.SQLRowStream;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class TemplateDatabaseVerticle extends AbstractVerticle {

	private final Logger logger = LogManager.getLogger(TemplateDatabaseVerticle.class);
	private int HTTP_SERVER_PORT = 8080;
	private Injector injector = null;
	private ResultSet cacheResult = null;

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
	
	
	
	
	
	
	
	
	
	
	
	
	

	private void selectTest(RoutingContext ctx) {
		HttpServerResponse response = ctx.response();
		String query = "select * from test1 where c1 = ?";

		VertxSqlConnectionFactory.getClient().queryWithParams(query, 
				new JsonArray().add(Integer.valueOf(ctx.request().getParam("param1"))), res -> {
					if ( res.succeeded() ) {

						if ( this.cacheResult == null ) this.cacheResult = res.result();

						if ( this.cacheResult.equals(res.result()) ) {
							response.end("equal");
						} else {
							this.cacheResult = res.result();
							response.end("not-equal");
						}
					}
				});
	}



	/**
	 * @description [Test Case]
	 * @param ctx
	 */
	@SuppressWarnings("static-access")
	private void mysqlSleepQueryTest(RoutingContext ctx) {

		int thisHashCode = vertx.factory.context().hashCode();

		HttpServerResponse response = ctx.response();
		String serviceName = ctx.request().getParam("svcnm");
		Map<String, Object> reqData = new LinkedHashMap<String, Object>();
		reqData.put("sqlName", serviceName);
		reqData.put("c1", Integer.valueOf(ctx.request().getParam("c1")));

		VertxSqlConnectionFactory.getClient().getConnection(conn -> {
			if (conn.failed()) {
				logger.error(conn.cause().getMessage());
				response.setStatusCode(500).end();
				return;
			}

			final SQLConnection connection = conn.result();

			// Get Query Using MyBatis ORM Framework
			Map<String, Object> queryInfo = null;
			try {
				queryInfo = injector.getInstance(QueryServices.class).getQuery(reqData);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Query Stream With Param
			connection.queryWithParams((String) queryInfo.get("sql"), (JsonArray) queryInfo.get("sqlParam"), res -> {
				// just print result
				// for (JsonArray line : res.result().getResults()) {
				// logger.info(line.encode());
				// }

				System.out.println("END " + thisHashCode);

				response.putHeader("Content-Type", "application/json");
				response.setStatusCode(200);
				response.end(res.result().getResults().toString()); // res.result().getResults().toString() );
			});

			connection.close();
		});
	}

	/**
	 * @description [Test Case] DB Search Handler
	 * @param ctx
	 */
	private void dbSearchHandler2(RoutingContext ctx) {

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
		 */
		VertxSqlConnectionFactory.getClient().getConnection(conn -> {

			if (conn.failed()) {
				logger.error(conn.cause().getMessage());
			}

			try {

				final SQLConnection connection = conn.result();

				// Get Query Using MyBatis ORM Framework
				Map<String, Object> queryInfo = injector.getInstance(QueryServices.class).getQuery(reqData);

				connection.queryWithParams((String) queryInfo.get("sql"), (JsonArray) queryInfo.get("sqlParam"),
						res -> {

							try {
								if (res.failed()) {
									logger.error(res.cause().getMessage());
								}

								// just print result
								for (JsonArray line : res.result().getResults()) {
									logger.info(line.encode());
								}

								response.putHeader("Content-Type", "application/json");
								response.setStatusCode(200);
								response.end(res.result().getResults().toString());

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

	/**
	 * @description [Test Case] exel export (simple sqlClient POI)
	 * @param ctx
	 */
	private void excelExportPoi(RoutingContext ctx) {
		HttpServerResponse response = ctx.response();

		final JDBCClient oracleDbClient = JDBCClient.createShared(vertx,
				new JsonObject().put("url", "jdbc:oracle:thin:@127.0.0.1:3590:rnd") // ISPDS
				.put("driver_class", "oracle.jdbc.driver.OracleDriver").put("max_pool_size", 30)
				.put("user", "username").put("password", "password"));

		oracleDbClient.getConnection(conn -> {
			if (conn.failed()) {
				System.err.println(conn.cause().getMessage());
				return;
			}

			// "SELECT ROWNUM, ACCTNUM, NAME, RANK FROM LIST1"
			String sql = "select ROWNUM, A.* from (  " + "     select * from T_SEL_EXEC" + "     union all"
					+ "     select * from T_SEL_EXEC" + "     union all" + "     select * from T_SEL_EXEC"
					+ "     union all" + "     select * from T_SEL_EXEC" + "     union all"
					+ "     select * from T_SEL_EXEC" + "     union all" + "     select * from T_SEL_EXEC"
					+ "     union all" + "     select * from T_SEL_EXEC" + "     union all"
					+ "     select * from T_SEL_EXEC" + "     union all" + "     select * from T_SEL_EXEC"
					+ "     union all" + "     select * from T_SEL_EXEC" + "     union all"
					+ "     select * from T_SEL_EXEC" + " ) A";

			final SQLConnection connection = conn.result();
			// query some data
			connection.query(sql, res -> {
				if (res.failed()) {
					logger.error(res.cause().getMessage());
				}

				try (	FileOutputStream output_file = new FileOutputStream(new File("C:/dev/POI_XLS_JDBC.xls"));
						HSSFWorkbook new_workbook = new HSSFWorkbook(); // create a blank workbook object
						) {

					/* Create Workbook and Worksheet objects */
					HSSFSheet sheet = new_workbook.createSheet("excel_contents"); // create a worksheet with caption
					// score_details

					int rownum = 0;
					for (JsonObject line : res.result().getRows()) {
						// logger.info(line.encode());

						Row row = sheet.createRow(rownum++);
						List<String> columns = res.result().getColumnNames();

						int cellnum = 0;
						for (String col : columns) {
							Cell cell = row.createCell(cellnum++);
							if (line.getValue(col) instanceof java.math.BigInteger) {
								cell.setCellValue(line.getInteger(col, 0));
							} else {
								cell.setCellValue(line.getString(col, ""));
							}
						}
					}

					new_workbook.write(output_file); // write excel document to output stream

					response.putHeader("Content-Type", "application/json");
					response.setStatusCode(200);
					response.end("END");

				} catch (Exception e) {
					e.printStackTrace();
					response.putHeader("Content-Type", "application/json");
					response.setStatusCode(500);
					response.end("ERROR");
				} finally {
					// and close the connection
					connection.close(done -> {
						if (done.failed()) {
							throw new RuntimeException(done.cause());
						}
					});
				}
			});
		});
	}

	/**
	 * @description [Test Case] query stream (queryStream & POI)
	 * @param ctx
	 */
	private void queryStreamPoi(RoutingContext ctx) {

		HttpServerResponse response = ctx.response();

		final JDBCClient oracleDbClient = JDBCClient.createShared(vertx,
				new JsonObject().put("url", "jdbc:oracle:thin:@127.0.0.1:3590:rnd") // ISPDS
				.put("driver_class", "oracle.jdbc.driver.OracleDriver").put("max_pool_size", 30)
				.put("user", "username").put("password", "password"));

		oracleDbClient.getConnection(conn -> {
			if (conn.failed()) {
				System.err.println(conn.cause().getMessage());
				return;
			}

			// "SELECT ROWNUM, ACCTNUM, NAME, RANK FROM LIST1"
			String sql = "select ROWNUM, A.* from (  " + "     select * from T_SEL_EXEC" + "     union all"
					+ "     select * from T_SEL_EXEC" + "     union all" + "     select * from T_SEL_EXEC"
					+ "     union all" + "     select * from T_SEL_EXEC" + "     union all"
					+ "     select * from T_SEL_EXEC" + "     union all" + "     select * from T_SEL_EXEC"
					+ "     union all" + "     select * from T_SEL_EXEC" + "     union all"
					+ "     select * from T_SEL_EXEC" + "     union all" + "     select * from T_SEL_EXEC"
					+ "     union all" + "     select * from T_SEL_EXEC" + "     union all"
					+ "     select * from T_SEL_EXEC" + " ) A";

			final SQLConnection connection = conn.result();

			// query some data
			connection.queryStream(sql, res -> {
				if (res.failed()) {
					logger.error(res.cause().getMessage());
				}

				/* Create Workbook and Worksheet objects */
				HSSFWorkbook new_workbook = new HSSFWorkbook(); // create a blank workbook object
				HSSFSheet sheet = new_workbook.createSheet("excel_contents"); // create a worksheet with caption
				// score_details

				SQLRowStream sqlRowStream = res.result();
				sqlRowStream.resultSetClosedHandler(v -> {
					// will ask to restart the stream with the new result set if any
					sqlRowStream.moreResults();
				}).handler(row -> {

					// System.out.println(row.getValue(0)
					// +":"+row.getValue(1)+":"+row.getValue(2)+":"+row.getValue(3)+":"+row.getValue(4));
					int rownum = (row.getInteger(0)) - 1;
					Row sRow = sheet.createRow(rownum);
					List<String> columns = res.result().columns();

					int cellnum = 0;
					for (String col : columns) {
						int uCellnum = cellnum;
						Cell cell = sRow.createCell(cellnum++);

						if (row.getValue(uCellnum) instanceof java.math.BigInteger) {
							cell.setCellValue(row.getInteger(uCellnum));
						} else {
							cell.setCellValue(row.getString(uCellnum));
						}
					}
				}).endHandler(v -> {
					logger.info("END");

					connection.close(done -> {
						if (done.failed()) {
							throw new RuntimeException(done.cause());
						}
					});

					response.putHeader("Content-Type", "application/json");
					response.setStatusCode(200);
					response.end("END");

					FileOutputStream output_file = null;
					try {
						output_file = new FileOutputStream(new File("C:/dev/POI_XLS_JDBC.xls")); // create XLS file
						new_workbook.write(output_file); // write excel document to output stream
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						if (output_file != null) {
							try {
								output_file.close();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}

						if (new_workbook != null) {
							try {
								new_workbook.close();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}

				}).exceptionHandler(e -> {
					e.getCause().printStackTrace();
					response.putHeader("Content-Type", "application/json");
					response.setStatusCode(500);
					response.end("ERROR");
				});
			});

		});
	}


	/**
	 * @description [Test Case]
	 * @param ctx
	 */
	private void download(RoutingContext routingContext) {
		HttpServerResponse response = routingContext.response();
		String filePath = "D:/source/wtWeb_SCE_Uplus.tar";
		String fileName = "wtWeb_SCE_Uplus.tar";

		vertx.fileSystem().open(filePath, new OpenOptions(), readEvent -> {

			if (readEvent.failed()) {
				response.setStatusCode(500).end();
				return;
			}

			AsyncFile asyncFile = readEvent.result();

			response.setChunked(true);
			response.putHeader("Content-Type", "application/octet-stream; charset=utf-8");
			response.putHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

			Pump pump = Pump.pump(asyncFile, routingContext.response());

			pump.start();

			asyncFile.endHandler(aVoid -> {
				asyncFile.close();
				response.end();
			});
		});
	}
}