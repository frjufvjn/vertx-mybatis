package io.frjufvjn.lab.vertx_mybatis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Launcher;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class AppMain 
{
	/**
	 * 	Number of Machine's Core CPU
	 */
	private static final int NUMBER_OF_CPU = Runtime.getRuntime().availableProcessors();

	/**
	 * @description Direct Execute Application in IDE Without CLI
	 * @param args
	 * @throws Exception
	 */
	public static void main( String[] args ) throws Exception
	{
		/*
		 * [
		 * 		{"schema":"test","table":"test1","type":"write","row":{"c3":"aa","c1":11,"c2":1}},
		 * 		{"schema":"test","table":"test1","type":"write","row":{"c3":"bb","c1":22,"c2":2}},
		 * 		{"schema":"test","table":"test2","type":"write","row":{"col2":null,"col3":null,"c1":null,"col1":"xxq"}},
		 * 		{"schema":"test","table":"test1","type":"write","row":{"c3":"cc","c1":33,"c2":3}}
		 * ]
		 * */
//		JsonArray arr = new JsonArray();
//		arr.add(new JsonObject().put("table", "test1").put("row", new JsonObject().put("c3", "aa").put("c1", "11") ));
//		arr.add(new JsonObject().put("table", "test1").put("row", new JsonObject().put("c3", "bb").put("c1", "22") ));
//		arr.add(new JsonObject().put("table", "test2").put("row", new JsonObject().put("col2", "").put("col1", "xxq") ));
//		arr.add(new JsonObject().put("table", "test1").put("row", new JsonObject().put("c3", "cc").put("c1", "33") ));
//		System.out.println(arr.encodePrettily() );
//
//		Map<String,JsonObject> sessions = new HashMap<String,JsonObject>();
//		sessions.put("111", 
//				new JsonObject().put("service-name", "svc01")
//				.put("filter", 
//						new JsonObject()
//						.put("c1", new JsonArray().add("100").add("101"))
//						.put("c2", new JsonArray().add("aa"))
//						)
//				);
//		System.out.println(sessions.toString());
//
//		sessions.forEach((k,v) -> {
//			if ( v.containsKey("filter") ) {
//				v.getJsonObject("filter").fieldNames().forEach(userFilterCol -> {
//					System.out.println(userFilterCol);
//				});
//			}
//		});

		// guavaCacheTest();

		// zipFileCreate();

		// ansiEncoding();

		// privateLibMigration();

		// (1) --------------------------------------------------------------------------------------
				Launcher.executeCommand("version", args);
				Launcher.executeCommand(
						"run", 
						new String[]{
								"io.frjufvjn.lab.vertx_mybatis.MainVerticle" // "io.frjufvjn.lab.vertx_mybatis.DatabaseVerticle"
								, "-cp", "target/vertx-mybatis-1.0.0.jar" 
								// , "-instances", Integer.toString(NUMBER_OF_CPU)
								// , "-ha"
								// , "--worker"
								// For hsqldb , "-Dtextdb.allow_full_path=true"
								// , "--worker", "io.frjufvjn.lab.vertx_mybatis.MainVerticle"
						});
	}

	private static void guavaCacheTest() throws InterruptedException {

		CacheLoader<String, String> cacheLoader =
				new CacheLoader<String, String>() {
			@Override
			public String load(String key) throws Exception {
				System.out.println("make cache {"+key+"}");
				return key.toUpperCase();
			}
		};

		// 최대 3개까지 캐쉬를 유지하고, 500 밀리초 이후 갱신됨.
		LoadingCache<String, String> cache =
				CacheBuilder.newBuilder()
				.maximumSize(3)
				.expireAfterAccess(500, TimeUnit.MILLISECONDS)
				.build(cacheLoader);

		cache.put("aa", "111");
		System.out.println(cache.asMap().toString());
		Thread.sleep(400);
		System.out.println(cache.asMap().toString());
	}

	/**
	 * @description zip test
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private static void zipFileCreate() throws FileNotFoundException, IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("Test String");

		File file = new File("d:\\test.zip");
		ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(file));
		ZipEntry zEntry = new ZipEntry("mytext.txt");
		zipOut.putNextEntry(zEntry);

		byte[] data = sb.toString().getBytes();
		zipOut.write(data, 0, data.length);
		zipOut.closeEntry();

		zipOut.close();
	}

	/**
	 *	@description Get the library into my project > lib directory from maven repository
	 */
	@SuppressWarnings("unused")
	private static void privateLibMigration() {

		/**
		 * It is a clipboard string obtained by selecting and copying all the jar files listed 
		 * in sts or Eclipse's "Package Explorer> Project> Maven Dependencies".
		 * */
		String[] libs = {
				"C:/Users/PJW/.m2/repository/junit/junit/3.8.1/junit-3.8.1.jar",
				"C:/Users/PJW/.m2/repository/io/vertx/vertx-core/3.5.4/vertx-core-3.5.4.jar",
				"C:/Users/PJW/.m2/repository/io/netty/netty-common/4.1.19.Final/netty-common-4.1.19.Final.jar",
				"C:/Users/PJW/.m2/repository/io/netty/netty-buffer/4.1.19.Final/netty-buffer-4.1.19.Final.jar",
				"C:/Users/PJW/.m2/repository/io/netty/netty-transport/4.1.19.Final/netty-transport-4.1.19.Final.jar",
				"C:/Users/PJW/.m2/repository/io/netty/netty-handler/4.1.19.Final/netty-handler-4.1.19.Final.jar",
				"C:/Users/PJW/.m2/repository/io/netty/netty-codec/4.1.19.Final/netty-codec-4.1.19.Final.jar",
				"C:/Users/PJW/.m2/repository/io/netty/netty-handler-proxy/4.1.19.Final/netty-handler-proxy-4.1.19.Final.jar",
				"C:/Users/PJW/.m2/repository/io/netty/netty-codec-socks/4.1.19.Final/netty-codec-socks-4.1.19.Final.jar",
				"C:/Users/PJW/.m2/repository/io/netty/netty-codec-http/4.1.19.Final/netty-codec-http-4.1.19.Final.jar",
				"C:/Users/PJW/.m2/repository/io/netty/netty-codec-http2/4.1.19.Final/netty-codec-http2-4.1.19.Final.jar",
				"C:/Users/PJW/.m2/repository/io/netty/netty-resolver/4.1.19.Final/netty-resolver-4.1.19.Final.jar",
				"C:/Users/PJW/.m2/repository/io/netty/netty-resolver-dns/4.1.19.Final/netty-resolver-dns-4.1.19.Final.jar",
				"C:/Users/PJW/.m2/repository/io/netty/netty-codec-dns/4.1.19.Final/netty-codec-dns-4.1.19.Final.jar",
				"C:/Users/PJW/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.9.6/jackson-core-2.9.6.jar",
				"C:/Users/PJW/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.9.6/jackson-databind-2.9.6.jar",
				"C:/Users/PJW/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.9.0/jackson-annotations-2.9.0.jar",
				"C:/Users/PJW/.m2/repository/io/vertx/vertx-web/3.5.4/vertx-web-3.5.4.jar",
				"C:/Users/PJW/.m2/repository/io/vertx/vertx-auth-common/3.5.4/vertx-auth-common-3.5.4.jar",
				"C:/Users/PJW/.m2/repository/io/vertx/vertx-bridge-common/3.5.4/vertx-bridge-common-3.5.4.jar",
				"C:/Users/PJW/.m2/repository/io/vertx/vertx-jdbc-client/3.5.4/vertx-jdbc-client-3.5.4.jar",
				"C:/Users/PJW/.m2/repository/io/vertx/vertx-sql-common/3.5.4/vertx-sql-common-3.5.4.jar",
				"C:/Users/PJW/.m2/repository/com/mchange/c3p0/0.9.5.2/c3p0-0.9.5.2.jar",
				"C:/Users/PJW/.m2/repository/com/mchange/mchange-commons-java/0.2.11/mchange-commons-java-0.2.11.jar",
				"C:/Users/PJW/.m2/repository/org/mybatis/mybatis/3.3.0/mybatis-3.3.0.jar",
				"C:/Users/PJW/.m2/repository/mysql/mysql-connector-java/5.1.25/mysql-connector-java-5.1.25.jar",
				"C:/Users/PJW/.m2/repository/org/hsqldb/hsqldb/2.3.4/hsqldb-2.3.4.jar",
				"C:/Users/PJW/.m2/repository/com/oracle/ojdbc14/10.2.0.3.0/ojdbc14-10.2.0.3.0.jar",
				"C:/Users/PJW/.m2/repository/org/apache/poi/poi/3.17/poi-3.17.jar",
				"C:/Users/PJW/.m2/repository/commons-codec/commons-codec/1.10/commons-codec-1.10.jar",
				"C:/Users/PJW/.m2/repository/org/apache/commons/commons-collections4/4.1/commons-collections4-4.1.jar",
				"C:/Users/PJW/.m2/repository/com/google/inject/guice/4.0/guice-4.0.jar",
				"C:/Users/PJW/.m2/repository/javax/inject/javax.inject/1/javax.inject-1.jar",
				"C:/Users/PJW/.m2/repository/aopalliance/aopalliance/1.0/aopalliance-1.0.jar",
				"C:/Users/PJW/.m2/repository/com/google/guava/guava/16.0.1/guava-16.0.1.jar",
				"C:/Users/PJW/.m2/repository/org/bouncycastle/bcprov-jdk15on/1.58/bcprov-jdk15on-1.58.jar",
				"C:/Users/PJW/.m2/repository/org/jasypt/jasypt-spring3/1.9.2/jasypt-spring3-1.9.2.jar",
				"C:/Users/PJW/.m2/repository/org/jasypt/jasypt/1.9.2/jasypt-1.9.2.jar",
				"C:/Users/PJW/.m2/repository/org/apache/commons/commons-lang3/3.3.2/commons-lang3-3.3.2.jar"
		};

		String mavenRepositoryDir = "C:/Users/PJW/.m2/repository";
		String userDir = System.getProperty("user.dir").replace("\\", "/")+"/lib";

		for (int i = 0; i < libs.length; i++) {

			String targetPath = libs[i].replace(mavenRepositoryDir, userDir);
			String tmp[] = targetPath.split("/");
			String tmpPath = targetPath.replace(tmp[tmp.length-1], "");

			File file = new File(tmpPath);
			if(!file.exists()){
				file.mkdirs();
			}

			fileCopy(libs[i], targetPath);
		}
	}

	/**
	 * @description file copy method using FileChannel
	 * @param in
	 * @param out
	 * @return
	 */
	private static boolean fileCopy(String in, String out) {
		try (	FileInputStream inputStream = new FileInputStream(in);
				FileOutputStream outputStream = new FileOutputStream(out);
				FileChannel fcin =  inputStream.getChannel();
				FileChannel fcout = outputStream.getChannel(); ) {

			long size = fcin.size();
			fcin.transferTo(0, size, fcout);

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;		
	}

	/**
	 * @description 
	 * 	<li>csv file ansi encoding test
	 *	<li>Why? Korean language crash on Windows Excel Program
	 * @return
	 */
	public static boolean ansiEncoding() {

		String[] args = {
				"C:/Users/PJW/Desktop/정우/testzip/LOG-testuserid-20181016104119-0.csv",
				"C:/Users/PJW/Desktop/정우/testzip/LOG-testuserid-20181016104119-0-cv.csv"
		};

		try (	FileInputStream fis =  new FileInputStream(args[0]);
				BufferedReader r = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
				FileOutputStream fos = new FileOutputStream(args[1]);
				Writer w = new BufferedWriter(new OutputStreamWriter(fos, "EUC-KR"));) {

			String oemString = "";
			while ( (oemString= r.readLine()) != null) {
				w.write(oemString + "\r\n");
				w.flush();
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}
}
