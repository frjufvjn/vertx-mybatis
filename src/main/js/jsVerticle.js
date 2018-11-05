var console = require('vertx-js/util/console');
var _ = require('lodash-node');
var fs = vertx.fileSystem();
var LiveMysql = require('mysql-live-select');

vertx.createHttpServer().requestHandler(function (req) {
	console.log("js verticle !!!");
	req.response().putHeader("content-type", "text/html").end("<html><body><h1>Hello from vert.x!</h1></body></html>");

}).listen(18081);

var eb = vertx.eventBus();

eb.consumer("msg.jsverticle.test", function (message) {
	console.log("I have received a message: " + message.body());
	message.reply('reply');

	eb.send("msg.jsverticle", message.body());
});

// fs.readFile("target/classes/README.md", function (result, result_err) {
// 	if (result_err == null) {
// 		console.log(result);
// 	} else {
// 		console.error("Oh oh ..." + result_err);
// 	}
// });

/**
 * 	npm install mysql-live-select --save
	npm install util --save
	npm install events --save
	npm install crypto --save // built in module
	npm install fs --save // built in module
 * 
 */