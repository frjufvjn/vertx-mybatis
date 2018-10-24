package io.frjufvjn.lab.vertx_mybatis.secure;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

/**
 * Crypto Common Util
 */
public class CryptoManager implements CryptoService {

	private final String DBDATA_SEC_KEY = "1234567890123456";
    private final String DBDATA_SEC_IV = "1234567890123456";
	private final String DBDATA_SEC_ALGORITHM = "AES/CBC/PKCS5Padding";
	private final String DBDATA_SEC_PREFIX = "{ENC}";
	private final String DBDATA_SEC_CHARSET = "UTF-8";

	@Override
	public String getDecryptData(String data) throws Exception {
		return decryptDbData(data.replace(getPrefixEncryptDbData(), ""));
	}

	@Override
	public String getEncryptData(String data) throws Exception {
		return String.join("", getPrefixEncryptDbData(), encryptDbData(data));
	}

	private String decryptDbData(String data) throws Exception {
		Key key = getDbDataAesKey();
		Cipher cipher = Cipher.getInstance(DBDATA_SEC_ALGORITHM);
		IvParameterSpec ivParameterSpec = getDbDataAesIv();
		cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);
		//		byte[] decryptBytes = Base64.getDecoder().decode(data);
		byte[] decryptBytes = Base64.decodeBase64(data);
		String decryptData = new String(cipher.doFinal(decryptBytes), DBDATA_SEC_CHARSET);
		return decryptData;
	}

	private String encryptDbData(String data) throws Exception {
		Key key = getDbDataAesKey();
		Cipher cipher = Cipher.getInstance(DBDATA_SEC_ALGORITHM);
		IvParameterSpec ivParameterSpec = getDbDataAesIv();
		cipher.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec);
		byte[] encryptBytes = cipher.doFinal(data.getBytes(DBDATA_SEC_CHARSET));
		//		String encryptData = Base64.getEncoder().encodeToString(encryptBytes).replaceAll("\r|\n", "");
		String encryptData = Base64.encodeBase64String(encryptBytes).replaceAll("\r|\n", "");
		return encryptData;
	}

	private String getPrefixEncryptDbData() {
		return DBDATA_SEC_PREFIX;
	}

	private Key getDbDataAesKey() throws Exception {
		Key keySpec;

		byte[] keyBytes = new byte[16];
		byte[] b = DBDATA_SEC_KEY.getBytes(DBDATA_SEC_CHARSET);

		int len = b.length;
		if (len > keyBytes.length) {
			len = keyBytes.length;
		}

		System.arraycopy(b, 0, keyBytes, 0, len);
		keySpec = new SecretKeySpec(keyBytes, "AES");
		return keySpec;
	}

	private IvParameterSpec getDbDataAesIv() throws Exception {
		IvParameterSpec ivSpec;

		byte[] ivBytes = new byte[16];
		byte[] b = DBDATA_SEC_IV.getBytes(DBDATA_SEC_CHARSET);

		int len = b.length;
		if (len > ivBytes.length) {
			len = ivBytes.length;
		}

		System.arraycopy(b, 0, ivBytes, 0, len);
		ivSpec = new IvParameterSpec(ivBytes);

		return ivSpec;
	}
}
