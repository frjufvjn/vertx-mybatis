package io.frjufvjn.lab.vertx_mybatis;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;

import io.frjufvjn.lab.vertx_mybatis.factory.VertxSqlConnectionFactory;
import io.frjufvjn.lab.vertx_mybatis.query.QueryGetter;
import io.frjufvjn.lab.vertx_mybatis.query.QueryServices;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.impl.MessageImpl;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.streams.Pump;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.SQLRowStream;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class MainVerticle extends AbstractVerticle {

	Logger logger = LoggerFactory.getLogger(MainVerticle.class);
	private Injector queryService = null;
	private Properties properties;
	private ResultSet cacheResult = null;

	@SuppressWarnings("static-access")
	@Override
	public void start(Future<Void> startFuture) throws Exception {

		logger.info("hashCode : " + vertx.factory.context().hashCode());

		properties = new Properties();
		try (InputStream inputStream = getClass().getResourceAsStream("/config/app.properties")) {
			properties.load(inputStream);
		}

		/**
		 * @description
		 * [기능 명세 체크]
		 * - 1. 서버에 개발환경 세팅할 
		 * 	- > 로컬레파지토리로 메이븐 pom.xml 별도 설정 - > java -jar xxx.fat.jar 로 실행가능하도록... 
		 * 		: pom.xml에서 Launcher로 설정된 verticle을 이 클래스명으로 바꿔주기만 하면 되고 프로젝트 홈에서 
		 * 		java -jar target/vertx-mybatis-1.0.0-fat.jar로 실행하면됨. 
		 * - 2. 리팩토링
		 * 	: mybatis injection 
		 * - 3. 리팩토링 : properties 로 static한 정보 분리
		 * - 4. 페이징 파라미터 추가 
		 * - 5. 기존 스프링mvc controller에서 호출하기 resttemplete 사용, 의존성추가 없이 바로 사용 가능 요청 완료후 파일 삭제 
		 * - 6. front 호출 작성 async await 
		 * - skip 7. RONUM 단위의 partial response가 아닌 index를 활용할 수 있는 날짜시간기준으로 제공할 수 있는 옵션 
		 * - 8. vertx sql client connection option 고려
		 * 		https://vertx.io/docs/vertx-jdbc-client/java/#_configuration 
		 * - 9. 데이터 암/복호화 함수 적용 
		 * - 10. JUL Logger 설정
		 * - 11. 혹시나 남아있는 임시파일 삭제 배치 적용 
		 * - 12. 서버에 빌드 설정 : 
		 * 		별도의 메이븐 디렉토리 패스를 잡기
		 * 		별도의 로컬 레파지토리 구성 
		 * 		build.sh & run.sh 작성 (run.sh은 start/stop/status/instances갯수 아규먼트 받아서 실행)
		 */

		final Router router = Router.router(vertx);
		final Route svc = router.route(HttpMethod.GET, "/svc/:svcnm/:param1/:param2");



		/**
		 * @description Query Getter & Binding Value Setter Using Mybatis
		 * */
		queryService = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(QueryServices.class).to(QueryGetter.class).in(Scopes.SINGLETON);
			}
		});




		/**
		 * @description Multi Async Request Proxy
		 * 	<li> test url : http://localhost:18080/proxy
		 * */
		router.route(HttpMethod.GET, "/proxy/:userid/:fetchrow/:startdt/:enddt/:phone/:custno").handler(this::proxyMultiRequest);
		router.route(HttpMethod.GET, "/proxy-test").handler(this::proxyMultiRequestTutorial);

		router.route(HttpMethod.GET, "/jseb/:message").handler(this::jsEbTest);

		svc.handler(this::dbSearchHandler);



		router.route(HttpMethod.GET, "/select-test/:param1").handler(this::selectTest);

		// [Test Case] sleep test
		router.route(HttpMethod.GET, "/sleep/:svcnm/:c1").handler(this::mysqlSleepQueryTest);

		// [Test Case] download
		router.route(HttpMethod.GET, "/download").handler(this::download);

		// [Test Case] exel export (simple sqlClient)
		// http://localhost:8080/excel
		router.route(HttpMethod.GET, "/excel").handler(this::excelExportPoi);

		// [Test Case] query stream (queryStream & POI)
		// http://localhost:8080/stream
		router.route(HttpMethod.GET, "/stream").handler(this::queryStreamPoi);


		LocalMap<String,JsonObject> sessions = vertx.sharedData().getLocalMap("ws.channel");

		/**
		 * @description Create HTTP Server (RequestHandler & WebSocketHandler)
		 */
		final HttpServer httpServer = vertx.createHttpServer();
		int httpServerPort = Integer.parseInt(properties.getProperty("HTTP_SERVER_PORT", "8080"));

		// RequestHandler
		httpServer.requestHandler(router::accept);

		// WebSocketHandler
		httpServer.websocketHandler(ws -> {
			ws.closeHandler(ch -> {
				logger.info("Close Handler : " + ch);
				sessions.remove(ws.textHandlerID());
			});

			ws.frameHandler(wsFrame -> {
				logger.info("frameHandler's Remote Address : " + ws.remoteAddress().toString() + " id : " + ws.textHandlerID());

				if (ws.path().equals("/channel")) {
					if ( wsFrame.isText() ) {
						logger.info(wsFrame.textData());

						if ( !sessions.containsKey(ws.textHandlerID()) ) {
							sessions.put(ws.textHandlerID(), new JsonObject(wsFrame.textData()));
						}

						ws.writeFinalTextFrame("Echo React");
					}

					if ( wsFrame.isClose() ) {
						logger.info("close " + wsFrame.closeReason());
					}
				} else {
					ws.reject();
				}

				EventBus eb = vertx.eventBus();
				eb.consumer("msg.jsverticle", e -> {
					String testHandlerID = e.body().toString();
					vertx.eventBus().send(testHandlerID, "SERVER SENT...");
				});
			});

			ws.exceptionHandler(t -> {
				logger.error(t.getCause().getMessage());
			});
		});

		httpServer.listen(httpServerPort, res -> {
			if (res.failed()) {
				// res.cause().printStackTrace();
				startFuture.fail(res.cause());
			} else {
				startFuture.complete();
				logger.info("Server listening at: http://localhost:" + httpServerPort);
			}
		});



		/**
		 * @description SubVerticle Deploy
		 * */
		vertx.deployVerticle("io.frjufvjn.lab.vertx_mybatis.SubVerticle", deploy -> {
			if (deploy.succeeded()) {
				logger.info("SubVerticle deploy successfully ID: " + deploy.result());
			} else {
				deploy.cause().printStackTrace();
				vertx.close();
			}
		});

		
		
		/**
		 * @description BinLogClientTestVerticle Deploy
		 * */
		vertx.fileSystem().readFile("C:/workspace_spring/common-secreet-data/mysql-local.json", f -> {
			if ( f.succeeded() ) {
				JsonObject opt = new JsonObject(f.result().getString(0, f.result().length(), "UTF-8"));

				vertx.deployVerticle("io.frjufvjn.lab.vertx_mybatis.mysqlBinlog.BinLogClientVerticle", 
						new DeploymentOptions().setConfig(opt),
						deploy -> {
							if (deploy.succeeded()) {
								logger.info("BinLogClientTestVerticle deploy successfully ID: " + deploy.result());
							} else {
								deploy.cause().printStackTrace();
								vertx.close();
							}
						});
			}
		});




		/**
		 * @description jsVerticle.js Deploy
		 * 	- fat.jar실행시, "src/main/js/jsVerticle.js"
		 * */
		//		 vertx.deployVerticle("src/main/js/jsVerticle.js", deploy -> {
		//		 	if (deploy.succeeded()) {
		//		 		logger.info("jsVerticle deploy successfully ID: " + deploy.result());
		//		 	} else {
		//		 		deploy.cause().printStackTrace();
		//		 		vertx.close();
		//		 	}
		//		 });



		/**
		 * @description 주기적 배치작업 실행
		 * 	- 혹시라도 남아 있을 csv파일 삭제
		 * 	- 03:0x & 03:3x 2번 실행
		 * 	- 파일의 수정시간이 6시간 지난 파일 대상으로 삭제
		 */
		vertx.setPeriodic(TimeUnit.MINUTES.toMillis(10) , id -> {
			// Multi Instances일때 구분되는 id인듯...
			if (id == 0) {
				String systime = getSysDateString(Calendar.getInstance().getTime(), "yyyyMMddkkmmss");

				String tm_ddk = systime.substring(8, 11);

				if ( "030".equals(tm_ddk) || "033".equals(tm_ddk) ) {
					logger.info(">> [Periodic Job] id:" + id + " ............");
					logger.info("Temp File Delete : " + tm_ddk);

					String targetPath = properties.getProperty("EXPORT_TARGET_PATH", "");
					File dirFile = new File(targetPath);
					File[] fileList = dirFile.listFiles();
					int allowInterval = 1*60*60*6; // 최종수정시간이 6 시간지난 파일 대상

					long now = System.currentTimeMillis();
					long fileTime = 0L;
					for (File file : fileList) {
						if( file.isFile() ) {
							fileTime = file.lastModified();
							long elapse = (now - fileTime)/1000;
							System.out.println(file.getName() + " : " + elapse);
							if( elapse > allowInterval ) {
								file.delete();
							}
						}
					}
				}
			}
		});
	}


	private void jsEbTest(RoutingContext ctx) {
		HttpServerResponse response = ctx.response();

		EventBus eb = vertx.eventBus();
		String passMessage = ctx.request().getParam("message");

		eb.send("msg.jsverticle.test", passMessage, reply -> {
			if (reply.succeeded()) {
				logger.info("received reply : " + reply.result().body());
				response.end("jsVerticle Reply Message : [" + reply.result().body() + "]");
			}
		});
	}



	/**
	 * @description
	 *              <li>Multiple Asynchronous Reactive Process Per One HTTP Request
	 *              <li>URI Patern : "/proxy/:fetchrow/:startdt/:enddt/:phone/:custno"
	 *              <li>Test : curl http://localhost:18080/proxy/testuserid/50000/20180701000000/20180730235959/no/no
	 * @param ctx
	 */
	@SuppressWarnings({ "static-access", "rawtypes" })
	private void proxyMultiRequest(RoutingContext ctx) {

		int thisHashCode = vertx.factory.context().hashCode();
		logger.info("proxyMultiRequest invoke : " + thisHashCode);

		long starttime = System.currentTimeMillis();

		HttpServerResponse response = ctx.response();

		int maxFetchRow = Integer.parseInt(properties.getProperty("MAX_FETCH_ROW", "50000"));
		int rowCntPerPage = Integer.parseInt(properties.getProperty("ROW_COUNT_PER_PAGE", "10000"));
		int fetchrow = Integer.parseInt(ctx.request().getParam("fetchrow"));

		MultiMap params = ctx.request().params();
		Set<String> names = params.names();

		if (fetchrow > maxFetchRow) {
			response.setStatusCode(403).end("Requested fetch row number exceeded.");
			return;
		}

		EventBus eb = vertx.eventBus();

		List<Future> futureList = new ArrayList<>();
		int occursCnt = (fetchrow / rowCntPerPage) + (fetchrow % rowCntPerPage > 0 ? 1 : 0);

		// send multiple eventbus message
		for (int i = 0; i < occursCnt; i++)
		{
			int start = (i * rowCntPerPage) + 1;
			int end = fetchrow < rowCntPerPage ? fetchrow : (i + 1) * rowCntPerPage;
			if (logger.isDebugEnabled())
				logger.debug("start:" + start + " end:" + end);

			Future<Message<Object>> fut = Future.future();
			futureList.add(fut);

			JsonObject sendMessage = new JsonObject();

			for (String key : names) {
				if (!"fetchrow".equals(key)) 
					sendMessage.put(key, params.get(key));
			}

			sendMessage
			.put("startrow", start)
			.put("endrow", end)
			.put("idx", i);

			eb.send("req.multi.service", sendMessage, fut.completer());
		}

		// all reply message async await handle
		StringBuffer resultSet = new StringBuffer();
		CompositeFuture.all(futureList).setHandler(ar -> {
			if (ar.succeeded()) {
				ar.result().list().forEach((result) -> {
					resultSet.append(((MessageImpl) result).body().toString());
				});

				if (logger.isDebugEnabled()) {
					logger.debug(resultSet.toString());
				}
				logger.info("service elapsed : " + (System.currentTimeMillis() - starttime) + "ms");

				// All succeeded
			} else {
				// All completed and at least one failed
			}

			response.end(resultSet.toString());
			futureList.clear();

		});
	}

	/**
	 * @description Multiple Asynchronous Reactive Process Per One HTTP Request Turorial
	 * @param ctx
	 */
	@SuppressWarnings("rawtypes")
	private void proxyMultiRequestTutorial(RoutingContext ctx) {

		@SuppressWarnings("static-access")
		int thisHashCode = vertx.factory.context().hashCode();
		logger.info("proxyMultiRequest invoke : " + thisHashCode);

		long start = System.currentTimeMillis();

		HttpServerResponse response = ctx.response();

		EventBus eb = vertx.eventBus();

		int occursCount = 5;

		List<Future> futureList = new ArrayList<>();

		// send multiple eventbus message
		for (int i = 0; i < occursCount; i++) {
			Future<Message<Object>> fut = Future.future();
			futureList.add(fut);
			eb.send("req.multi.service", "worker-react", fut.completer());
		}

		// async await all reply message
		StringBuffer resultSet = new StringBuffer();
		CompositeFuture.all(futureList).setHandler(ar -> {
			if (ar.succeeded()) {
				ar.result().list().forEach((result) -> {
					resultSet.append(((MessageImpl) result).body().toString());
				});

				if (logger.isDebugEnabled()) {
					logger.debug(resultSet.toString());
					logger.debug("service elapsed : " + (System.currentTimeMillis() - start) + "ms");
				}

				// All succeeded
				response.end("NORMAL-END");
			} else {
				// All completed and at least one failed
				response.end("ERROR-END");
			}

			futureList.clear();

		});
	}


	/**
	 * @description Get System Date
	 * @param date
	 * @param pattern
	 * @return
	 */
	private String getSysDateString(Date date, String pattern) {
		String result = null;
		java.text.SimpleDateFormat format = new java.text.SimpleDateFormat(pattern);
		result = format.format(date);
		return result;
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
				queryInfo = queryService.getInstance(QueryServices.class).getQuery(reqData);
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
		 */
		VertxSqlConnectionFactory.getClient().getConnection(conn -> {

			if (conn.failed()) {
				logger.error(conn.cause().getMessage());
			}

			try {

				final SQLConnection connection = conn.result();

				// Get Query Using MyBatis ORM Framework
				Map<String, Object> queryInfo = queryService.getInstance(QueryServices.class).getQuery(reqData);

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
