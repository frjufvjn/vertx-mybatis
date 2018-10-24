package io.frjufvjn.lab.vertx_mybatis.query;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class QueryModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(QueryServices.class).to(QueryGetter.class)
		.in(Scopes.SINGLETON);
	}
}
