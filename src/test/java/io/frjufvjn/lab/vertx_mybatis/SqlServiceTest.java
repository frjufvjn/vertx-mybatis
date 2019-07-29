package io.frjufvjn.lab.vertx_mybatis;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Guice;

import io.frjufvjn.lab.vertx_mybatis.sql.SqlInnerModule;
import io.frjufvjn.lab.vertx_mybatis.sql.SqlInnerServices;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import junit.framework.TestCase;

@RunWith(VertxUnitRunner.class)
public class SqlServiceTest extends TestCase {
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
	public void test_sqlService(TestContext ctx) {
		Async async = ctx.async();

		Guice.createInjector(new SqlInnerModule()).getInstance(SqlInnerServices.class)
		.sqlRead(
				new JsonObject()
				.put("sqlName", "sysmon_get_server_list")
				, ar -> {
					if (ar.succeeded()) {
						System.out.println(ar.result());
					} else {
						System.err.println(ar.cause().getMessage());
					}
					async.complete();
				});


	}
}
