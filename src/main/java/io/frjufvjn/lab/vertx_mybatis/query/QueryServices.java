package io.frjufvjn.lab.vertx_mybatis.query;

import java.util.Map;

public interface QueryServices {

	/**
	 * @description Get Query String & Bind Values Using ORM Framework (MyBatis)
	 * */
	public Map<String,Object> getQuery(Map<String,Object> reqData) throws Exception;
}
