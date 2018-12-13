package io.frjufvjn.lab.vertx_mybatis.query;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.SqlSession;

import io.frjufvjn.lab.vertx_mybatis.factory.MyBatisConnectionFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * @author PJW
 *
 */
public class QueryServiceImp implements QueryServices {

	/**
	 * 
	 */
	private final String SQL_NAME = "sqlName";

	/**
	 * 
	 */
	private final String DEFAULT_SQL = "SELECT 1";

	private final String BATCH_PARAM_KEY = "arr";

	/* (non-Javadoc)
	 * @see io.frjufvjn.lab.vertx_mybatis.query.QueryServices#getQuery(java.util.Map)
	 */
	@Override
	public Map<String, Object> getQuery(Map<String, Object> reqData) throws Exception {

		Map<String,Object> result = new LinkedHashMap<String,Object>();
		String queryString = DEFAULT_SQL;
		JsonArray jsonParam = new JsonArray();


		try ( final SqlSession sqlsession = MyBatisConnectionFactory.getSqlSessionFactory().openSession(); ) {
			String sqlName = reqData.get(SQL_NAME).toString();
			reqData.remove(SQL_NAME);

			// Get BoudSql
			BoundSql boundSql = sqlsession.getConfiguration()
					.getMappedStatement(sqlName) // MyBatis SQL ID
					.getSqlSource()
					.getBoundSql(reqData)
					;

			queryString = boundSql.getSql();

			// get Parameter
			List<ParameterMapping> paramMapping = boundSql.getParameterMappings();
			for ( ParameterMapping mapping : paramMapping ) {
				String key = mapping.getProperty();
				Object value = ((Map<String,Object>)reqData).get(key);

				jsonParam.add(value);
			}

			result.put("sql", queryString);
			result.put("sqlParam", jsonParam);
		}

		return result;
	}

	/* (non-Javadoc)
	 * @see io.frjufvjn.lab.vertx_mybatis.query.QueryServices#getQuery(io.vertx.ext.web.RoutingContext)
	 */
	@Override
	public Map<String, Object> getQuery(RoutingContext ctx) throws Exception {
		Map<String,Object> result = new LinkedHashMap<String,Object>();
		String queryString = DEFAULT_SQL;
		JsonArray jsonParam = new JsonArray();

		Map<String,Object> reqData = getParamToMap(ctx);

		if ( !reqData.containsKey(SQL_NAME) ) {
			throw new IllegalArgumentException("sqlName parameter not found");
		}

		try ( final SqlSession sqlsession = MyBatisConnectionFactory.getSqlSessionFactory().openSession(); ) {
			String sqlName = reqData.get(SQL_NAME).toString();
			reqData.remove(SQL_NAME);

			// Get BoudSql
			BoundSql boundSql = sqlsession.getConfiguration()
					.getMappedStatement(sqlName) // MyBatis SQL ID
					.getSqlSource()
					.getBoundSql(reqData)
					;

			queryString = boundSql.getSql();

			// get Parameter
			List<ParameterMapping> paramMapping = boundSql.getParameterMappings();

			if ( paramMapping.size() != reqData.size() ) {
				throw new IllegalStateException("The number of sql parameter is insufficient or exceeded.");
			}

			for ( ParameterMapping mapping : paramMapping ) {
				String key = mapping.getProperty();
				Object value = ((Map<String,Object>)reqData).get(key);

				jsonParam.add(value);
			}

			result.put("sql", queryString);
			result.put("sqlParam", jsonParam);
		}

		return result;
	}

	/* (non-Javadoc)
	 * @see io.frjufvjn.lab.vertx_mybatis.query.QueryServices#getQueryWithoutParam(io.vertx.ext.web.RoutingContext)
	 */
	@Override
	public Map<String, Object> getQueryWithoutParam(RoutingContext ctx) throws Exception {

		if ( !ctx.getBodyAsJson().containsKey(SQL_NAME) ) {
			throw new IllegalArgumentException("sqlName parameter not found");
		}

		String sqlName = ctx.getBodyAsJson().getString(SQL_NAME);

		JsonArray arr = ctx.getBodyAsJson().getJsonArray("arr");
		if ( arr.size() <= 0 ) {
			throw new IllegalArgumentException("array size is zero");
		}

		Map<String,Object> result = new LinkedHashMap<String,Object>();
		String queryString = DEFAULT_SQL;

		Map<String,Object> reqData = getListParamToMap(arr);

		try ( final SqlSession sqlsession = MyBatisConnectionFactory.getSqlSessionFactory().openSession(); ) {

			// Get BoudSql
			BoundSql boundSql = sqlsession.getConfiguration()
					.getMappedStatement(sqlName) // MyBatis SQL ID
					.getSqlSource()
					.getBoundSql(reqData)
					;

			queryString = boundSql.getSql();

			result.put("sql", queryString);
			result.put("batchParam", getBatchParam(ctx));
		}

		return result;
	}

	/**
	 * @param ctx
	 * @return
	 */
	private Map<String,Object> getParamToMap(RoutingContext ctx) {
		Map<String,Object> map = new LinkedHashMap<String,Object>();
		ctx.getBodyAsJson().forEach(param -> {
			map.put(param.getKey(), param.getValue());
		});
		return map;
	}

	/**
	 * @param arr
	 * @return
	 */
	private Map<String,Object> getListParamToMap(JsonArray arr) {
		// get only first row
		JsonObject firstObjFromArr = arr.getJsonObject(0);
		Map<String,Object> map = new LinkedHashMap<String,Object>();
		firstObjFromArr.forEach(param -> {
			map.put(param.getKey(), param.getValue());
		});
		return map;
	}


	/**
	 * @param ctx
	 */
	private List<JsonArray> getBatchParam(RoutingContext ctx) {
		List<JsonArray> batch = new ArrayList<JsonArray>();

		ctx.getBodyAsJson().getJsonArray(BATCH_PARAM_KEY)
			.stream().forEach(row -> {
				JsonObject obj = (JsonObject) row;
				JsonArray innerArr = new JsonArray();
				obj.fieldNames().forEach(key -> {
					innerArr.add(obj.getValue(key));
				});
				batch.add(innerArr);
			});
		return batch;
	}
}
