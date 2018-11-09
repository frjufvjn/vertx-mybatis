package io.frjufvjn.lab.vertx_mybatis.mysqlBinlog;

import java.util.List;
import java.util.stream.Collectors;

import io.frjufvjn.lab.vertx_mybatis.factory.VertxSqlConnectionFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class SchemaService {
	Logger logger = LoggerFactory.getLogger(SchemaService.class);
	private List<JsonObject> informationSchema = null;

	/**
	 * @description Get Schema Data
	 */
	public void loadSchemaData() {
		String getSchemaSql = "SELECT table_schema, table_name, column_name "
				+ "FROM information_schema.`COLUMNS` "
				+ "WHERE table_schema = 'test'";
		VertxSqlConnectionFactory.getClient().query(getSchemaSql, ar -> {
			if (ar.succeeded()) {
				if ( informationSchema != null ) informationSchema.clear();
				informationSchema = ar.result().getRows();
				logger.info("Get Schema Data Complete!!");
			}
		});
	}

	/**
	 * @description return filtered schema list by schemaName and tableName
	 * @param schemaName
	 * @param tableName
	 * @return
	 */
	public List<JsonObject> filterByTable(String schemaName, String tableName) {
		return informationSchema.stream()
				.filter(item 
						-> schemaName.equals(item.getString("table_schema")) 
							&& tableName.equals(item.getString("table_name")) )
				.collect(Collectors.toList());
	}
}
