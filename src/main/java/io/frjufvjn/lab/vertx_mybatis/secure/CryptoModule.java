package io.frjufvjn.lab.vertx_mybatis.secure;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class CryptoModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(CryptoService.class).to(CryptoManager.class)
		.in(Scopes.SINGLETON);
	}
}
