package io.frjufvjn.lab.vertx_mybatis.secure;

public interface CryptoService {
	
	public String getDecryptData(String data) throws Exception;
	
	public String getEncryptData(String data) throws Exception;
}
