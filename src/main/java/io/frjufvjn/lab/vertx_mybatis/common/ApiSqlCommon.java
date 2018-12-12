package io.frjufvjn.lab.vertx_mybatis.common;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;

import io.frjufvjn.lab.vertx_mybatis.sql.SqlServiceImp;
import io.frjufvjn.lab.vertx_mybatis.sql.SqlServices;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.RoutingContext;

public abstract class ApiSqlCommon extends AbstractVerticle {

	/**
	 * @description SQL Service DI injection
	 * */
	private Injector sqlService = null;

	/**
	 * @description ApiSqlCommon's Constructor
	 */
	public ApiSqlCommon() {
		super();
		sqlService = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(SqlServices.class).to(SqlServiceImp.class).in(Scopes.SINGLETON);
			}
		});
	}

	/**
	 * @description SQL API INSERT Service (C of CRUD)  
	 * @param ctx
	 */
	protected void apiCreate(RoutingContext ctx) {
		try {
			sqlService.getInstance(SqlServices.class).sqlApiCreate(ctx);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @description SQL API SELECT Service (R of CRUD)  
	 * @param ctx
	 */
	protected void apiRead(RoutingContext ctx) {
		try {
			sqlService.getInstance(SqlServices.class).sqlApiRead(ctx);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @description SQL API UPDATE Service (U of CRUD)  
	 * @param ctx
	 */
	protected void apiUpdate(RoutingContext ctx) {
		try {
			sqlService.getInstance(SqlServices.class).sqlApiUpdate(ctx);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @description SQL API DELETE Service (D of CRUD)  
	 * @param ctx
	 */
	protected void apiDelete(RoutingContext ctx) {
		try {
			sqlService.getInstance(SqlServices.class).sqlApiDelete(ctx);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @description SQL API Multi-Row Base INSERT Service (C of CRUD)  
	 * @param ctx
	 */
	protected void apiCreateMulti(RoutingContext ctx) {
		// TODO
	}

	/**
	 * @description SQL API Multi-Row Base UPDATE Service (U of CRUD)  
	 * @param ctx
	 */
	protected void apiUpdateMulti(RoutingContext ctx) {
		// TODO
	}

	/**
	 * @description SQL API Multi-Row Base DELETE Service (D of CRUD)  
	 * @param ctx
	 */
	protected void apiDeleteMulti(RoutingContext ctx) {
		// TODO
	}

}