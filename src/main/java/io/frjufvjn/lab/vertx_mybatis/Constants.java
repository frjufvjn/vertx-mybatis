package io.frjufvjn.lab.vertx_mybatis;

public final class Constants {
	public Constants() {}
	
	public static final String VERTICLE_SUB = "io.frjufvjn.lab.vertx_mybatis.SubVerticle";
	public static final String VERTICLE_EVENTBUS_SQL = "io.frjufvjn.lab.vertx_mybatis.sql.EBSqlServiceVerticle";
	
	public static final String WEBSOCKET_CHANNEL = "ws.channel";
	
	public static final String EVENTBUS_SQL_VERTICLE_ADDR = "msg.eventbussqlverticle";
	public static final String EVENTBUS_SQL_VERTICLE_FAIL_SIGNAL = "fail";
	
	public static final String API_AHTHENTICATE_QUERY = "SELECT PASSWORD, PASSWORD_SALT FROM CS_USERMASTER WHERE user_id = ?";
}
