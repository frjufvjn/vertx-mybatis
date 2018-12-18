package io.frjufvjn.lab.vertx_mybatis.query;

import java.util.Map;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * 	@author PJW
 *	@description Get Query String & Bind Values Using MyBatis Framework
 */
public interface QueryServices {

	public Map<String,Object> getQuery(Map<String,Object> reqData) throws Exception;
	
	public Map<String,Object> getQuery(RoutingContext ctx) throws Exception;
	
	public Map<String,Object> getQuery(JsonObject params) throws Exception;
	
	public Map<String,Object> getQueryWithoutParam(RoutingContext ctx) throws Exception;
}
