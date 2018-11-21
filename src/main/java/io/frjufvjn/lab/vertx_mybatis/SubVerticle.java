package io.frjufvjn.lab.vertx_mybatis;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.frjufvjn.lab.vertx_mybatis.factory.VertxSqlConnectionFactory;
import io.frjufvjn.lab.vertx_mybatis.query.QueryModule;
import io.frjufvjn.lab.vertx_mybatis.query.QueryServices;
import io.frjufvjn.lab.vertx_mybatis.secure.CryptoModule;
import io.frjufvjn.lab.vertx_mybatis.secure.CryptoService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.SQLRowStream;

public class SubVerticle extends AbstractVerticle {

	private final Logger logger = LogManager.getLogger(SubVerticle.class);
	private final String messageConsumerAddr = "req.multi.service";
	private final String messageReplyTag = "res.multi.service";
	private final String messageDelimeter = "!";
	private final String endMessageDelimeter = "#";
	private final String serviceName = "massiveOracleQuery_iscLog";

	@SuppressWarnings("unused")
	private boolean isCsvQuote = true;

	private Properties properties;
	private Injector services = null;

	@Override
	public void start(Future<Void> startFuture) throws Exception {

		properties = new Properties();
		try ( InputStream inputStream = getClass().getResourceAsStream("/config/app.properties") ) {
			properties.load(inputStream);
		}

		isCsvQuote = "Y".equals(properties.getProperty("CSV_QUOTE_APPLY_YN")) ? true : false;

		// Merge Guice Services
		services = Guice.createInjector(new QueryModule(), new CryptoModule());

		EventBus eb = vertx.eventBus();

		eb.consumer(messageConsumerAddr, msg -> {
			int thisHash = vertx.getOrCreateContext().hashCode();
			logger.info("hashCode : " + thisHash + ":" + msg.body().toString() );

			Map<String, Object> reqData = new LinkedHashMap<String, Object>(); // 순서주의 !!!

			JsonObject msgReq = new JsonObject(msg.body().toString());
			String userId = msgReq.getString("userid");
			String sdate = msgReq.getString("startdt");
			String edate = msgReq.getString("enddt");
			int startrow = msgReq.getInteger("startrow");
			int endrow = msgReq.getInteger("endrow");
			String phone = msgReq.getString("phone");
			String custno = msgReq.getString("custno");
			int idx = msgReq.getInteger("idx");

			final JDBCClient jdbcClient = VertxSqlConnectionFactory.getClient();

			jdbcClient.getConnection(conn -> {
				if (conn.failed()) {
					logger.error(conn.cause().getMessage());
					replyFailMessage(msg, msgReq);
					return;
				}

				final SQLConnection connection = conn.result();

				try {
					reqData.put("sqlName", serviceName);
					reqData.put("SDATE", sdate);
					reqData.put("EDATE", edate);

					if (!"no".equals(phone)) {
						reqData.put("PHONE_NUM", services.getInstance(CryptoService.class).getEncryptData(phone));
					}
					if (!"no".equals(custno)) {
						reqData.put("CUST_NO", custno);
					}

					reqData.put("endRow", endrow);
					reqData.put("startRow", startrow);

					// Get Query Using MyBatis ORM Framework
					Map<String, Object> queryInfo = services.getInstance(QueryServices.class).getQuery(reqData);

					// Query Stream With Param
					connection.queryStreamWithParams(
							(String) queryInfo.get("sql"),
							(JsonArray)queryInfo.get("sqlParam"), res -> {
								if ( res.failed() ) {
									logger.error(res.cause().getMessage());
									replyFailMessage(msg, msgReq);
								}

								String path = properties.getProperty("EXPORT_TARGET_PATH", "");
								String fileNamePrefix = properties.getProperty("EXPORT_FILE_NAME_PREFIX", "");
								String systime = getSysDateString(Calendar.getInstance().getTime(), "yyyyMMddkkmmss");
								String fullPath = path + fileNamePrefix + "-" + userId + "-" + systime + "-" + idx + ".csv";
								Buffer buff = Buffer.buffer();

								String headerColStr = properties.getProperty("HEADER_COL_STR", "");

								int secureColIdx = Integer.parseInt(properties.getProperty("SECURE_COL_IDX"));
								try {buff.appendBytes("NO,로그일자,로그시각,로그일시,고객번호,로그SID,연관로그SID,콜ID,발신구분,발신자번호,수신자번호,변조지역번호,변조구분,변조전화번호,LDAP키,LDAP구분,LDAP전화번호,LDAP조회시각,통화여부,통화연결시각,\r\n".getBytes("EUC-KR"));
								} catch (Exception e2) {}

								SQLRowStream sqlRowStream = res.result();

								sqlRowStream
								.resultSetClosedHandler(v -> {
									// will ask to restart the stream with the new result set if any
									sqlRowStream.moreResults();
								})
								.handler(row -> {
									// buffer append
									// row.stream().forEachOrdered((el) -> {
									// 	try {
									// 		if (el != null) {
									// 			String element = el.toString();
									// 			if (element.startsWith("{ENC}")) {
									// 				buff.appendBytes(("=\"" + secureService.getInstance(CryptoService.class).getDecryptData(element) + "\",").getBytes("EUC-KR"));
									// 			} else {
									// 				buff.appendBytes(("=\"" + element.replace("\r|\n", "") + "\",").getBytes("EUC-KR"));
									// 			}
									// 		} else {
									// 			buff.appendBytes("=\"\",".getBytes("EUC-KR"));
									// 		}
									// 	} catch (Exception e1) {
									// 		replyFailMessage(msg, msgReq);
									// 		throw new RuntimeException(e1);
									// 	}
									// });

									// try {
									// 	buff.appendBytes("\r\n".getBytes("EUC-KR"));
									// } catch (Exception e1) {
									// 	replyFailMessage(msg, msgReq);
									// 	throw new RuntimeException(e1);
									// }


									try {
										buff.appendBytes(toCsvStr(row, secureColIdx).getBytes("EUC-KR"));
									} catch (Exception e1) {
										replyFailMessage(msg, msgReq);
										throw new RuntimeException(e1);
									}
								})
								.endHandler(v -> {

									connection.close(done -> {
										sqlRowStream.close();
										// jdbcClient.close();
									});

									// file write from completed buffer
									vertx.fileSystem().writeFile(fullPath, buff, result -> {
										if (result.succeeded()) {
											if (logger.isDebugEnabled()) logger.debug("END (idx:"+idx+")");
											replySuccMessage(msg, msgReq, fullPath);

										} else {
											replyFailMessage(msg, msgReq);
										}
									});
								})
								.exceptionHandler(e -> {

									connection.close(done -> {
										sqlRowStream.close();
										// jdbcClient.close();
									});

									replyFailMessage(msg, msgReq);
									logger.error(e.getMessage());

								});
							});
				} catch (Exception e) {
					replyFailMessage(msg, msgReq);
					logger.error(e.getMessage());
				}
			});
		}).completionHandler(ar -> {
			startFuture.complete();
			logger.info("SubVerticle EventBus Ready!! (address:"+messageConsumerAddr+")");
		});
	}

	/**
	 * @description Parse CSV
	 * @param data
	 * @param secureColIdx
	 * @return
	 * @throws Exception
	 */
	private String toCsvStr(final JsonArray data, final int secureColIdx) throws Exception {

		StringBuilder sb = new StringBuilder();
		for (int i=0; i<data.size(); i++) {
			if ( data.getValue(i) != null ) {
				if ( i != secureColIdx ) {
					sb
					.append("=\"")
					.append(data.getValue(i).toString().replace(System.getProperty("line.separator"), ""))
					.append("\",");
				} else {
					sb
					.append("=\"")
					.append(services.getInstance(CryptoService.class).getDecryptData(data.getValue(i).toString()))
					.append("\",");
				}
			} else {
				sb.append("=\"\",");
			}
		}
		sb.append("\r\n");

		return sb.toString();

	}

	/**
	 * @description reply eventbus fail signal 
	 * @param msg
	 * @param msgReq
	 */
	private void replyFailMessage(Message<Object> msg, JsonObject msgReq) {
		msg.reply(String.join(messageDelimeter, messageReplyTag, "fail", msgReq.getInteger("idx").toString()) 
				+ endMessageDelimeter );
	}

	/**
	 * @description reply eventbus success signal
	 * @param msg
	 * @param msgReq
	 * @param filePath
	 */
	private void replySuccMessage(Message<Object> msg, JsonObject msgReq, String filePath) {
		msg.reply(String.join(messageDelimeter, messageReplyTag,"success", msgReq.getInteger("idx").toString(), filePath)
				+ endMessageDelimeter );
	}

	/**
	 * @description get sysdate string
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
}
