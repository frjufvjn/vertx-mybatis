package io.frjufvjn.lab.vertx_mybatis.mysqlBinlog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.IntStream;
import java.util.concurrent.ConcurrentHashMap;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.BinaryLogClient.EventListener;
import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.EventType;
import com.github.shyiko.mysql.binlog.event.UpdateRowsEventData;
import com.github.shyiko.mysql.binlog.event.WriteRowsEventData;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.github.shyiko.mysql.binlog.event.TableMapEventData;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;

public class BinLogClientVerticle extends AbstractVerticle {

	Logger logger = LoggerFactory.getLogger(BinLogClientVerticle.class);
	private List<JsonObject> finalList = new ArrayList<JsonObject>();
	private ConcurrentMap<String,String> tableMap = new ConcurrentHashMap<String,String>();
	private Injector services = null;

	@Override
	public void start(Future<Void> startFuture) throws Exception {

		/**
		 * @description bin-log service config Read
		 * */
		LocalMap<String,JsonArray> pubsubServices = vertx.sharedData().getLocalMap("ws.pubsubServices");
		vertx.fileSystem().readFile("config/pubsub-mysql-service.json", f -> {
			if ( f.succeeded() ) {
				pubsubServices.put("table", new JsonArray(f.result().getString(0, f.result().length(), "UTF-8")));
				logger.info("bin-log service config Read Complete!!");
			}
		});

		services = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(SchemaInfo.class).in(Scopes.SINGLETON);
			}
		});

		services.getInstance(SchemaInfo.class).loadSchemaData();



		/**
		 * @description bin-log client connect, listen
		 * */
		BinaryLogClient client = new BinaryLogClient("localhost", 3306, "root", config().getString("local-mysql-password"));

		client.registerEventListener(new EventListener() {
			public void onEvent(Event evt) {
				if ( logger.isDebugEnabled() ) {
					logger.debug(" - evt type : " + evt.getHeader().getEventType()
							+ "\n - HeaderInfo:[" + evt.getHeader().toString()
							+ "]\n - DataInfo:[" + evt.getData().toString() + "]\n"
							);
				}

				if ( EventType.TABLE_MAP == evt.getHeader().getEventType() ) {
					dispatch("tableMap", evt.getData());
				}

				if ( EventType.XID == evt.getHeader().getEventType() ) {
					dispatch("xid", null);
				}

				if ( EventType.isWrite(evt.getHeader().getEventType()) ) {
					dispatch("write", evt.getData());
				}

				if ( EventType.isUpdate(evt.getHeader().getEventType()) ) {
					dispatch("update", evt.getData());
				}

			}

		});

		client.connect(1000);
	}

	/**
	 * @description Event Dispatcher
	 * @param type
	 * @param data
	 */
	private void dispatch(String type, Object data) {

		switch (type) {
		case "query" :
			break;

			// Throttling by Transaction Row Range
		case "xid" :
			logger.info("- DML Transaction final result -");
			for (JsonObject obj : finalList) {
				logger.info(obj.encode());
			}
			finalList.clear();
			break;

		case "tableMap" :
			tableMap.clear();
			tableMap.put("schema", ((TableMapEventData) data).getDatabase());
			tableMap.put("table", ((TableMapEventData) data).getTable());
			break;

		case "write" :
			((WriteRowsEventData) data).getRows().forEach(row -> {
				handleRowEvent(tableMap.get("schema"), tableMap.get("table"), type, Arrays.asList(row));
			});
			break;

		case "update" :
			((UpdateRowsEventData) data).getRows().forEach(row -> {
				handleRowEvent(tableMap.get("schema"), tableMap.get("table"), type, Arrays.asList(row.getValue()));
			});
			break;
		}
	}

	/**
	 * @description handle row data
	 * @param schema
	 * @param table
	 * @param type
	 * @param fields
	 */
	private void handleRowEvent(String schema, String table, String type, List<Serializable> fields) {
		List<JsonObject> columns = services.getInstance(SchemaInfo.class).filterByTable(schema, table);

		Map<String, Object> row = IntStream
				.range(0, columns.size())
				.boxed()
				.collect(HashMap::new,
						(map, i) -> map.put(columns.get(i).getString("column_name"), fields.get(i)),
						HashMap::putAll);

		finalList.add(new JsonObject()
				.put("schema", schema)
				.put("table", table)
				.put("type", type)
				.put("row", new JsonObject(row)));
	}
}
