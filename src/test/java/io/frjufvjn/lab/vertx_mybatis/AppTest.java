package io.frjufvjn.lab.vertx_mybatis;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.SqlSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.frjufvjn.lab.vertx_mybatis.factory.MyBatisConnectionFactory;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import junit.framework.TestCase;

@RunWith(VertxUnitRunner.class)
public class AppTest extends TestCase {

	private final Logger logger = LogManager.getLogger(AppTest.class);

	Vertx vertx;

	@Before
	public void setup() throws IOException {
		vertx = Vertx.vertx();
	}

	@After
	public void teardown(TestContext context) {
		vertx.close(context.asyncAssertSuccess());
	}

	@Test
	public void test_helloWorld(TestContext context) {
		Async async = context.async();
		context.assertEquals(1, 1);
		async.complete();
	}

	/**
	 * @description mybatis connection factory Test
	 * <li>goal: prove that mybatis connection factory is independent of the actual jdbc connection.
	 * <li>Since it is a singleton, there is about 300ms delay for the first time and then 1ms later.
	 * <li>Conclusion: In the end, what you do is independent of jdbc, and you only need to run sql mapper. The actual jdbc process is handled by the vertx sql client.
	 * */
	@Test
	public void test_mybatisConnection(TestContext ctx) {

		Async async = ctx.async();

		Map<String, Object> reqData = new LinkedHashMap<String,Object>();

		long start1 = routine(reqData);
		logger.info("1. elapse: {}ms", (System.currentTimeMillis() - start1));

		long start2 = routine(reqData);
		logger.info("2. elapse: {}ms", (System.currentTimeMillis() - start2));

		long start3 = routine(reqData);
		logger.info("3. elapse: {}ms", (System.currentTimeMillis() - start3));

		long start4 = routine(reqData);
		logger.info("4. elapse: {}ms", (System.currentTimeMillis() - start4));

		long start5 = routine(reqData);
		logger.info("5. elapse: {}ms", (System.currentTimeMillis() - start5));

		async.complete();
	}








	private long routine(Map<String, Object> reqData) {
		String sqlName = "sql_mysql_live_01";
		long start = System.currentTimeMillis();
		try ( final SqlSession sqlsession = MyBatisConnectionFactory.getSqlSessionFactory().openSession(); ) {

			reqData.put("kk", "11");

			// Get BoudSql
			BoundSql boundSql = sqlsession.getConfiguration()
					.getMappedStatement(sqlName) // MyBatis SQL ID
					.getSqlSource()
					.getBoundSql(reqData)
					;

			String queryString = boundSql.getSql();
			logger.info("query : {}", queryString);

			// get Parameter
			List<ParameterMapping> paramMapping = boundSql.getParameterMappings();

			logger.info("query param :");
			for ( ParameterMapping mapping : paramMapping ) {
				String key = mapping.getProperty();
				logger.info("key -->{}", key);
			}
		}
		return start;
	}
}
