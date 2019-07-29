package io.frjufvjn.lab.vertx_mybatis.sql;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class SqlInnerModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(SqlInnerServices.class).in(Scopes.SINGLETON);
	}
}
