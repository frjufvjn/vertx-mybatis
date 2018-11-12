package io.frjufvjn.lab.vertx_mybatis.mysqlBinlog;

import com.github.shyiko.mysql.binlog.event.EventType;

public enum BinlogEventType {
	TABLE_MAP("table-map"),
	XID("xid"),
	WRITE("write"),
	UPDATE("update"),
	DELETE("delete"),
	QUERY("query"),
	UNKNOWN("unknown");

	private String typeName;

	private BinlogEventType(String typeName) {
		this.typeName = typeName;
	}

	public String getEvtName() {
		return this.typeName;
	}

	public static BinlogEventType getEvtType(EventType evt) {

		if (EventType.TABLE_MAP == evt) {
			return TABLE_MAP;
		}
		else if (EventType.XID == evt) {
			return XID;
		}
		else if (EventType.isWrite(evt)) {
			return WRITE;
		}
		else if (EventType.isUpdate(evt)) {
			return UPDATE;
		}
		else if (EventType.isDelete(evt)) {
			return DELETE;
		}
		else if (EventType.QUERY == evt) {
			return QUERY;
		}
		else {
			return UNKNOWN;
		}
	}
}
