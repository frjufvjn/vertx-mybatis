package io.frjufvjn.lab.vertx_mybatis.common;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;

import io.frjufvjn.lab.vertx_mybatis.sql.SqlServiceImp;
import io.frjufvjn.lab.vertx_mybatis.sql.SqlServices;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.RoutingContext;

public abstract class ApiRequestCommon extends AbstractVerticle {

	/**
	 * @description SQL Service DI injection
	 * */
	private Injector sqlService = null;

	/**
	 * @description ApiRequestCommon's Constructor
	 */
	public ApiRequestCommon() {
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
	 * @description SQL API Multi-Row Base Bach Mode INSERT Service (C of CRUD)  
	 * @param ctx
	 */
	protected void apiCreateBatch(RoutingContext ctx) {
		try {
			sqlService.getInstance(SqlServices.class).sqlApiCreateBatch(ctx);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @description SQL API Multi-Row Base Bach Mode UPDATE Service (U of CRUD)  
	 * @param ctx
	 */
	protected void apiUpdateBatch(RoutingContext ctx) {
		try {
			sqlService.getInstance(SqlServices.class).sqlApiUpdateBatch(ctx);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @description SQL API Multi-Row Base Bach Mode DELETE Service (D of CRUD)  
	 * @param ctx
	 */
	protected void apiDeleteBatch(RoutingContext ctx) {
		try {
			sqlService.getInstance(SqlServices.class).sqlApiDeleteBatch(ctx);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}