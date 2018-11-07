package io.frjufvjn.lab.vertx_mybatis.mysqlBinlog;

import java.io.Serializable;
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
import com.github.shyiko.mysql.binlog.event.QueryEventData;
import com.github.shyiko.mysql.binlog.event.DeleteRowsEventData;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.github.shyiko.mysql.binlog.event.TableMapEventData;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;

public class BinLogClientVerticle extends AbstractVerticle {

	Logger logger = LoggerFactory.getLogger(BinLogClientVerticle.class);
	private JsonArray finalList = new JsonArray();
	private ConcurrentMap<String,String> tableMap = new ConcurrentHashMap<String,String>();
	private Injector services = null;
	LocalMap<String,JsonArray> pubsubServices = null;
	LocalMap<String,JsonObject> sessions = null;

	@Override
	public void start(Future<Void> startFuture) throws Exception {

		sessions = vertx.sharedData().getLocalMap("ws.channel");

		/**
		 * @description bin-log service config Read
		 * */
		vertx.fileSystem().readFile("config/pubsub-mysql-service.json", f -> {
			if ( f.succeeded() ) {
				pubsubServices = vertx.sharedData().getLocalMap("ws.pubsubServices");
				pubsubServices.put("table", new JsonArray(f.result().getString(0, f.result().length(), "UTF-8")));
				logger.info("bin-log service config Read Complete!!");
			}
		});

		/**
		 * @description MySQL information_schema data service
		 * */
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

				if (EventType.TABLE_MAP == evt.getHeader().getEventType()) {
					dispatch("tableMap", evt.getData());
				}

				if (EventType.XID == evt.getHeader().getEventType()) {
					dispatch("xid", null);
				}

				if (EventType.isWrite(evt.getHeader().getEventType())) {
					dispatch("write", evt.getData());
				}

				if (EventType.isUpdate(evt.getHeader().getEventType())) {
					dispatch("update", evt.getData());
				}

				if (EventType.isDelete(evt.getHeader().getEventType())) {
					dispatch("delete", evt.getData());
				}

				if (EventType.QUERY == evt.getHeader().getEventType()) {
					dispatch("query", evt.getData());
				}
			}

		});

		client.connect(1000);
	}

	/**
	 * @description Event Dispatcher
	 * @param type
	 * @param data
	 * @throws Exception 
	 */
	private void dispatch(String type, Object data) {

		switch (type) {
		case "query" :
			String sql = ((QueryEventData) data).getSql().toUpperCase().trim();
			if (sql.startsWith("CREATE TABLE") ||
					sql.startsWith("DROP TABLE") ||
					sql.startsWith("ALTER TABLE")) {
				if (logger.isDebugEnabled())
					logger.debug("Handle DDL statement, clear column mapping");
				services.getInstance(SchemaInfo.class).loadSchemaData();
			}
			break;

			// Throttling by Transaction Row Range
		case "xid" :
			logger.info("- DML Transaction final result -");

			pubsubServices.get("table").forEach(svc -> {
				long count = finalList.stream()
						.filter(item -> ((JsonObject) svc).getString("trigger-table").equals(((JsonObject) item).getString("table")) )
						.count();

				if ( count > 0 ) {
					logger.info("execute service : " + ((JsonObject) svc).getString("service-name") + ", 이때 sql실행 : [" + " 실행할 sql : " + ((JsonObject) svc).getString("service-name") + "]");
					sessions.forEach((key,obj) -> {
						if (obj.getValue("service-name").equals(((JsonObject) svc).getString("service-name"))) {
							logger.info("subscription 대상 유저 : " + key);
							EventBus eb = vertx.eventBus();
							eb.send("msg.mysql.live.select", new JsonObject().put("user-key", key).put("data", finalList));
						}
					});
				}
			});

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

		case "delete" :
			((DeleteRowsEventData) data).getRows().forEach(row -> {
				handleRowEvent(tableMap.get("schema"), tableMap.get("table"), type, Arrays.asList(row));
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
