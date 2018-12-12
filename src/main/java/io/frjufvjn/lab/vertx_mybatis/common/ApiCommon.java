package io.frjufvjn.lab.vertx_mybatis.common;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class ApiCommon {

	public ApiCommon() {
		super();
	}

	/**
	 * Send back a response with status 200 Ok.
	 *
	 * @param context routing context
	 */
	protected void ok(RoutingContext context) {
		context.response().end();
	}

	/**
	 * Send back a response with status 200 OK.
	 *
	 * @param context routing context
	 * @param content body content in JSON format
	 */
	protected void ok(RoutingContext context, String content) {
		context.response().setStatusCode(200)
		.putHeader("content-type", "application/json")
		.end(content);
	}

	/**
	 * Send back a response with status 204 No Content.
	 *
	 * @param context routing context
	 */
	protected void noContent(RoutingContext context) {
		context.response().setStatusCode(204).end();
	}

	/**
	 * Send back a response with status 500 Internal Error.
	 *
	 * @param context routing context
	 * @param ex      exception
	 */
	protected void internalError(RoutingContext context, Throwable ex) {
		context.response().setStatusCode(500)
		.putHeader("content-type", "application/json")
		.end(new JsonObject().put("error", ex.getMessage()).encodePrettily());
	}

	/**
	 * Send back a response with status 500 Internal Error.
	 *
	 * @param context routing context
	 * @param cause   error message
	 */
	protected void internalError(RoutingContext context, String cause) {
		context.response().setStatusCode(500)
		.putHeader("content-type", "application/json")
		.end(new JsonObject().put("error", cause).encodePrettily());
	}

	/**
	 * Send back a response with status 503 Service Unavailable.
	 *
	 * @param context routing context
	 */
	protected void serviceUnavailable(RoutingContext context) {
		context.fail(503);
	}


	/**
	 * Send back a response with status 503 Service Unavailable.
	 *
	 * @param context routing context
	 * @param ex      exception
	 */
	protected void serviceUnavailable(RoutingContext context, Throwable ex) {
		context.response().setStatusCode(503)
		.putHeader("content-type", "application/json")
		.end(new JsonObject().put("error", ex.getMessage()).encodePrettily());
	}

	/**
	 * Send back a response with status 503 Service Unavailable.
	 *
	 * @param context routing context
	 * @param cause   error message
	 */
	protected void serviceUnavailable(RoutingContext context, String cause) {
		context.response().setStatusCode(503)
		.putHeader("content-type", "application/json")
		.end(new JsonObject().put("error", cause).encodePrettily());
	}
}