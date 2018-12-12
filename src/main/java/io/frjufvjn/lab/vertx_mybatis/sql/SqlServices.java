package io.frjufvjn.lab.vertx_mybatis.sql;

import io.vertx.ext.web.RoutingContext;

public interface SqlServices {

	/**
	 * @description sql insert service
	 * @param ctx
	 * @throws Exception
	 */
	public void sqlApiCreate(RoutingContext ctx) throws Exception;

	/**
	 * @description sql select service
	 * @param ctx
	 * @throws Exception
	 */
	public void sqlApiRead(RoutingContext ctx) throws Exception;

	/**
	 * @description sql update service
	 * @param ctx
	 * @throws Exception
	 */
	public void sqlApiUpdate(RoutingContext ctx) throws Exception;

	/**
	 * @description sql delete service
	 * @param ctx
	 * @throws Exception
	 */
	public void sqlApiDelete(RoutingContext ctx) throws Exception;
}
