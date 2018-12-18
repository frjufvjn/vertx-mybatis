package io.frjufvjn.lab.vertx_mybatis;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.frjufvjn.lab.vertx_mybatis.common.ApiRequestCommon;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.impl.MessageImpl;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Log4j2LogDelegateFactory;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class MainVerticle extends ApiRequestCommon {

	private final Logger logger = LogManager.getLogger(MainVerticle.class);
	private Properties properties;
	private LocalMap<String,JsonObject> wsSessions = null;
	private JWTAuth jwtProvider;

	@Override
	public void start(Future<Void> startFuture) throws Exception {

		// log4j2 delegate
		System.setProperty("vertx.logger-delegate-factory-class-name", Log4j2LogDelegateFactory.class.getName());

		logger.info("hashCode : {}, context's hashcode : {}", vertx.hashCode(), vertx.getOrCreateContext().hashCode() );

		// Server Config File Load
		properties = new Properties();
		try (InputStream inputStream = getClass().getResourceAsStream("/config/app.properties")) {
			properties.load(inputStream);
		}



		/**
		 * @description HTTP Router Setting
		 * */
		final Router router = Router.router(vertx);
		router.route().handler(StaticHandler.create().setCachingEnabled(false));



		/**
		 * @description Create HTTP Server (RequestHandler & WebSocketHandler)
		 */
		final HttpServer httpServer = vertx.createHttpServer();
		int httpServerPort = Integer.parseInt(properties.getProperty("HTTP_SERVER_PORT", "8080"));
		wsSessions = vertx.sharedData().getLocalMap(Constants.WEBSOCKET_CHANNEL);

		// RequestHandler
		httpServer.requestHandler(router::accept);

		// WebSocketHandler
		httpServer.websocketHandler(this::wsHandler);

		// http listen
		httpServer.listen(httpServerPort, res -> {
			if (res.failed()) {
				// res.cause().printStackTrace();
				startFuture.fail(res.cause());
			} else {
				startFuture.complete();
				logger.info("Server listening at: http://localhost:{}, hashcode:{}", httpServerPort, vertx.getOrCreateContext().hashCode());
			}
		});



		/**
		 * @description CORS Enable Setup
		 * */
		enableCorsSupport(router);



		/**
		 * @description Service Router protected by JWT
		 * */
		serviceRouter(router);



		/**
		 * @description SubVerticle Deploy
		 * */
		subVerticleDeploy();



		/**
		 * @description Deploy EventBus SQL Verticle
		 * */
		eventBusSqlDeploy();



		/**
		 * @description BinLogClientVerticle Deploy
		 * */
		binlogDeploy();



		/**
		 * @description MySQL BinLog Event Consumer & Client Send
		 * */
		binlogConsumer();



		/**
		 * @description jsVerticle.js Deploy
		 * */
		jsVerticleDeploy();



		/**
		 * @description periodicJob
		 */
		periodicJob();



		/**
		 * @description prototype http router for Test
		 * */
		prototypeHttpRouter(router);
	}



	/**
	 * @description API Service Router
	 * @param router
	 */
	private void serviceRouter(final Router router) {

		/**
		 * @description Create a JWT Auth Provider
		 * */
		jwtProvider = JWTAuth.create(vertx, 
				new JWTAuthOptions().setKeyStore(
						new KeyStoreOptions()
						.setType("jceks")
						.setPath("config/keystore.jceks")
						.setPassword("secret")
						)
				);



		/**
		 * @description protect the API
		 * */
		router.route("/api/*")
		.handler(JWTAuthHandler.create(jwtProvider, "/api/newToken"))
		.handler(BodyHandler.create());



		/**
		 * @description this route is excluded from the auth handler
		 * */
		router.route(HttpMethod.POST, "/api/newToken").handler(this::generateJwtToken);



		/**
		 * @description SQL API Service Router Defined
		 * */
		router.post("/api/create").handler(this::apiCreate);
		router.post("/api/read").handler(this::apiRead);
		router.post("/api/update").handler(this::apiUpdate);
		router.post("/api/delete").handler(this::apiDelete);
		router.post("/api/create/multi").handler(this::apiCreateBatch);
		router.post("/api/update/multi").handler(this::apiUpdateBatch);
		router.post("/api/delete/multi").handler(this::apiDeleteBatch);
	}


	/**
	 * @description JWT Token Generate
	 * @param ctx
	 */
	private void generateJwtToken(RoutingContext ctx) {

		String username = ctx.getBodyAsJson().getString("username");
		String password = ctx.getBodyAsJson().getString("password");
		JsonObject sqlParams = new JsonObject()
				.put("sqlName", "sql_user_authentication")
				.put("user_id", username)
				;

		// Verify username and password from db
		ctx.vertx().eventBus().send(Constants.EVENTBUS_SQL_VERTICLE_ADDR, sqlParams, reply -> {
			if (reply.succeeded()) {
				String res = ((String) reply.result().body());
				if ( Constants.EVENTBUS_SQL_VERTICLE_FAIL_SIGNAL.equals(res) ) {
					ctx.response().setStatusCode(500)
					.putHeader("content-type", "application/json")
					.end( new JsonObject().put("error", "Authentication Server Error").encodePrettily() );
				} else {
					if (new JsonArray(res).size() > 0) {
						String comparePasswd = new JsonArray(res).getJsonObject(0).getString("password", "");
						if (password.equals(comparePasswd)) {

							// Send to client Generated JWT token
							ctx.response().putHeader("Content-Type", "text/plain")
							.end(jwtProvider.generateToken(
									new JsonObject(),
									new JWTOptions()
									.setExpiresInMinutes(30)
									));
						} else {
							ctx.response().setStatusCode(401)
							.putHeader("content-type", "application/json")
							.end(new JsonObject().put("error", "Unauthorized").encodePrettily());
						}
					} else {
						ctx.response().setStatusCode(401)
						.putHeader("content-type", "application/json")
						.end(new JsonObject().put("error", "Unauthorized").encodePrettily());
					}
				}
			}
		});
	}

	/**
	 * @description Enable CORS support for web router.
	 * @param router router instance
	 */
	private void enableCorsSupport(Router router) {
		Set<String> allowHeaders = new HashSet<>();
		allowHeaders.add("x-requested-with");
		allowHeaders.add("Access-Control-Allow-Origin");
		allowHeaders.add("origin");
		allowHeaders.add("Content-Type");
		allowHeaders.add("accept");
		// CORS support
		router.route().handler(CorsHandler.create("*")
				.allowedHeaders(allowHeaders)
				.allowedMethod(HttpMethod.GET)
				.allowedMethod(HttpMethod.POST)
				//				.allowedMethod(HttpMethod.DELETE)
				//				.allowedMethod(HttpMethod.PATCH)
				//				.allowedMethod(HttpMethod.PUT)
				);
	}



	/**
	 * @description sub verticle deploy
	 */
	private void subVerticleDeploy() {
		vertx.deployVerticle(Constants.VERTICLE_SUB, deploy -> {
			if (deploy.succeeded()) {
				logger.info("SubVerticle deploy successfully ID: " + deploy.result());
			} else {
				deploy.cause().printStackTrace();
				vertx.close();
			}
		});
	}



	/**
	 * @description EventBus Sql Verticle Deploy
	 */
	private void eventBusSqlDeploy() {
		vertx.deployVerticle(Constants.VERTICLE_EVENTBUS_SQL, dep -> {
			if (dep.succeeded()) {
				logger.info("{} deploy success", Constants.VERTICLE_EVENTBUS_SQL);
			}
		});
	}



	/**
	 * @description Consume EventBus message from MySQL Binlog Verticle
	 */
	private void binlogConsumer() {
		EventBus eb = vertx.eventBus();
		eb.consumer("msg.mysql.live.select", msg -> {
			String testHandlerID = ((JsonObject) msg.body()).getString("user-key");

			eb.send(testHandlerID, ((JsonObject) msg.body()).getString("data") , ar -> {
				// msg.reply("success");
			});

		}).completionHandler(ar -> {
			if (ar.succeeded()) logger.info("MySQL BinLog Consumer Ready Complete!!!");
		});
	}



	/**
	 * @description MySQL Binlog Verticle Deploy
	 */
	private void binlogDeploy() {
		vertx.fileSystem().readFile("config/pubsub-mysql-server.json", ar -> { // server connection config read
			if (ar.succeeded()) {
				JsonObject config = new JsonObject(ar.result().getString(0, ar.result().length(), "UTF-8"));
				logger.info("use-binlog : {}", config.getBoolean("use-binlog"));
				if (config.getBoolean("use-binlog")) {
					vertx.fileSystem().readFile(config.getString("password-config-path"), secConfig -> { // secret config read
						if ( secConfig.succeeded() ) {
							JsonObject secOpt = new JsonObject(secConfig.result().getString(0, secConfig.result().length(), "UTF-8"));
							config.mergeIn(secOpt);

							vertx.deployVerticle("io.frjufvjn.lab.vertx_mybatis.mysqlBinlog.BinLogClientVerticle", 
									new DeploymentOptions().setConfig(config),
									deploy -> {
										if (deploy.succeeded()) {
											logger.info("BinLogClientTestVerticle deploy successfully ID: " + deploy.result());
										} else {
											deploy.cause().printStackTrace();
											vertx.close();
										}
									});
						} else {
							logger.warn("BinLogClientVerticle secret config read Failed !!!");
						}
					});
				}
			}
		});
	}



	/**
	 * @description Websocket Handler
	 * @param ws
	 */
	private void wsHandler(ServerWebSocket ws) {

		ws.closeHandler(ch -> {
			logger.info("Close Handler event, remove wsSession map...");
			wsSessions.remove(ws.textHandlerID());
		});

		ws.frameHandler(wsFrame -> {
			if (logger.isDebugEnabled() ) logger.debug("frameHandler's Remote Address : {}, id : {}", ws.remoteAddress().toString(), ws.textHandlerID());

			if (ws.path().equals("/channel")) {
				if ( wsFrame.isText() ) {
					if (logger.isDebugEnabled() ) logger.debug(wsFrame.textData());
					JsonObject clientRegiMsg = new JsonObject(wsFrame.textData());

					if ( !wsSessions.containsKey(ws.textHandlerID()) ) {
						LocalMap<String,JsonArray> pubsubServices = vertx.sharedData().getLocalMap("ws.pubsubServices");
						long chkSvcCount = pubsubServices.get("table").stream()
								.filter(f -> clientRegiMsg.getString("service-name").equals(((JsonObject) f).getString("service-name")) )
								.count();

						if (chkSvcCount > 0) {
							wsSessions.put(ws.textHandlerID(), clientRegiMsg);
							ws.writeFinalTextFrame("Service Registration Success");
						} else {
							ws.writeFinalTextFrame("Request Service Not Available");
							ws.reject();
							logger.warn("{} is Rejected (Request Service Not Available)", ws.textHandlerID());
						}
					}
				}

				if ( wsFrame.isClose() ) {
					logger.info("event close frame " + wsFrame.closeReason());
				}
			} else {
				ws.reject();
			}
		});

		ws.exceptionHandler(t -> {
			logger.error(t.getCause().getMessage());
		});

	}




	/**
	 * @description 주기적 배치작업 실행
	 * 	- 혹시라도 남아 있을 csv파일 삭제
	 * 	- 03:0x & 03:3x 2번 실행
	 * 	- 파일의 수정시간이 6시간 지난 파일 대상으로 삭제
	 */
	private void periodicJob() {
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



	/**
	 * @description prototype http router for Test
	 * @param router
	 */
	private void prototypeHttpRouter(final Router router) {
		router.route(HttpMethod.GET, "/proxy/:userid/:fetchrow/:startdt/:enddt/:phone/:custno").handler(this::proxyMultiRequest);
		router.route(HttpMethod.GET, "/proxy-test").handler(this::proxyMultiRequestTutorial);
		router.route(HttpMethod.GET, "/jseb/:message").handler(this::jsEbTest);
	}



	/**
	 * @description
	 *              <li>Multiple Asynchronous Reactive Process Per One HTTP Request
	 *              <li>URI Patern : "/proxy/:fetchrow/:startdt/:enddt/:phone/:custno"
	 *              <li>Test : curl http://localhost:18080/proxy/testuserid/50000/20180701000000/20180730235959/no/no
	 * @param ctx
	 */
	@SuppressWarnings("rawtypes")
	private void proxyMultiRequest(RoutingContext ctx) {

		logger.info("proxyMultiRequest invoke : " + vertx.getOrCreateContext().hashCode());

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
				logger.error(ar.cause().getMessage());
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

		logger.info("proxyMultiRequest invoke : " + vertx.getOrCreateContext().hashCode());

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
		java.text.SimpleDateFormat format = new java.text.SimpleDateFormat(pattern);
		return format.format(date);
	}



	/**
	 * @description jsVerticle.js Deploy
	 * 	- fat.jar실행시, "src/main/js/jsVerticle.js"
	 * */
	private void jsVerticleDeploy() {
		//		vertx.deployVerticle("src/main/js/jsVerticle.js", deploy -> {
		//			if (deploy.succeeded()) {
		//				logger.info("jsVerticle deploy successfully ID: " + deploy.result());
		//			} else {
		//				deploy.cause().printStackTrace();
		//				vertx.close();
		//			}
		//		});
	}


	/**
	 * @description test
	 * @param ctx
	 */
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
}
