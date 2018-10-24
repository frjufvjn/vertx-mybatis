package io.frjufvjn.lab.vertx_mybatis.query;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.SqlSession;

import io.frjufvjn.lab.vertx_mybatis.factory.MyBatisConnectionFactory;
import io.vertx.core.json.JsonArray;

public class QueryGetter implements QueryServices {

	@Override
	public Map<String, Object> getQuery(Map<String, Object> reqData) throws Exception {

		Map<String,Object> result = new LinkedHashMap<String,Object>();
		String queryString = "SELECT 1";
		JsonArray jsonParam = new JsonArray();


		try ( final SqlSession sqlsession = MyBatisConnectionFactory.getSqlSessionFactory().openSession(); ) {
			String sqlName = reqData.get("sqlName").toString();
			reqData.remove("sqlName");

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
}
