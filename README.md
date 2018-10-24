# vertx-mybatis
Use asynchronous non-blocking database access using the sql client provided by vertx. Then you organize your project so that it is easy to use without giving up the ORM framework. 
## Description
### Synopsis
#### pure vertx sql client
The code block below is an example of a pure sql client code provided by vertx. I wanted to build an sql service using (without giving up) an ORM framework like mybatis.
```java
final SQLConnection connection = conn.result();
connection.queryWithParams("select * from test where id = ?", new JsonArray().add(2), rs -> {
    ...
});
```
#### get query and param
Once you have mybatis sqlsession, you can use BoundSql and ParameterMapping methods to retrieve queries and parameters and use them in your vertex sql client. See [QueryGetter.java](src/main/java/io/frjufvjn/lab/vertx_mybatis/query/QueryGetter.java)
* org.apache.ibatis.mapping.BoundSql
* org.apache.ibatis.mapping.ParameterMapping
```java
// Get BoudSql
BoundSql boundSql = sqlsession.getConfiguration()
        .getMappedStatement(sqlName) // MyBatis SQL ID
        .getSqlSource()
        .getBoundSql(reqData)
        ;
// A query containing '?' Is returned.
queryString = boundSql.getSql();

// The parameters defined in the mapper are returned.
List<ParameterMapping> paramMapping = boundSql.getParameterMappings();
for ( ParameterMapping mapping : paramMapping ) {
    String key = mapping.getProperty();
    Object value = ((Map<String,Object>)reqData).get(key);
}
```
### Usecase
#### Goal
Provides boilerplate for developers to create sql only and make it easy to access api server easily.
#### Large DB data csv export
_Recently, we had to add a large Excel download to an existing web application that had a lot of side effects when adding services._
1. Invoke the REST API with parameters including the number of patches to the legacy application (Spring framework), assuming 50,000 data requests. [Go Code](./client-test/RestFulController.java#L135)
2. Establish appropriate paging rules (eg, 10,000 responses) to invoke the DB service by dividing 50,000 requests into 10,000 requests. [Go Code](./src/main/java/io/frjufvjn/lab/vertx_mybatis/MainVerticle.java#L209)
3. Call the DB service n times as defined in step 2. [Go Code](./src/main/java/io/frjufvjn/lab/vertx_mybatis/MainVerticle.java#L248)
4. Use vertex sql client to process asynchronous non-block. [Go Code](./src/main/java/io/frjufvjn/lab/vertx_mybatis/SubVerticle.java#L130)
5. Process vertex cvs parsing using queryStream () of vertx and generate csv file asynchronously using vertex fileSystem () feature. You can see that the processed result is different to the order requested, as shown by the arrow in the figure. (AsyncResult) [Go Code](./src/main/java/io/frjufvjn/lab/vertx_mybatis/SubVerticle.java#L178)
6. CompositeFuture.all () is used to wait for the future of all the processing results, and then batches the response processing. [Go Code](./src/main/java/io/frjufvjn/lab/vertx_mybatis/MainVerticle.java#L253)
7. Get the csv file path list in the Legacy Application. [Go Code](./client-test/RestFulController.java#L173)
8. Stream the HTTP chunked response with the file list in step 7. [Go Code](./client-test/RestFulController.java#L183)

* Related Codes
    * [RestFulController.java](./client-test/RestFulController.java)
    * [MainVerticle.java](./src/main/java/io/frjufvjn/lab/vertx_mybatis/MainVerticle.java)
    * [SubVerticle.java](./src/main/java/io/frjufvjn/lab/vertx_mybatis/SubVerticle.java)

![title](/img/vertx-mybatis.png)
#### [WIP] CRUD template
#### [WIP] Transaction template
#### [WIP] Realtime changed data Pub/Sub feature
### Codes View
```java
public void main () {
.
├── LICENSE
├── README.md
├── bin
│   ├── build.sh // Maven build script
│   ├── config-to-server.sh // Apply server properties
│   ├── memchk.sh // Linux Server Target Process Memory View
│   ├── run.sh // This application start and stop script
│   └── tree-view.sh
├── client-test
│   ├── RestFulController.java // Legacy application Code (Spring Controler)
│   └── vertx-mybatis-test.html // Client Call Test
├── data
├── pom.xml
├── run.bat
└── src
    └── main
        ├── java
        │   └── io
        │       └── frjufvjn
        │           └── lab
        │               └── vertx_mybatis
        │                   ├── AppMain.java // Direct Execute Application in IDE Without CLI
        │                   ├── MainVerticle.java // Main Verticle
        │                   ├── SubVerticle.java // Sub Verticle (DB Access)
        │                   ├── TemplateDatabaseVerticle.java
        │                   ├── factory
        │                   │   ├── MyBatisConnectionFactory.java // MyBatis Connection
        │                   │   └── VertxSqlConnectionFactory.java // Vertx JDBC Client Connection
        │                   ├── query
        │                   │   ├── QueryGetter.java // Query Getter Using Mybatis
        │                   │   ├── QueryModule.java
        │                   │   ├── QueryServices.java
        │                   │   └── TestClazz.java
        │                   └── secure
        │                       ├── CryptoManager.java // Decrypt and Encrypt
        │                       └── CryptoService.java
        └── resources
            ├── config
            │   ├── app.properties // application property
            │   ├── db-config.xml // mybatis config
            │   └── db.properties // db connection information property
            ├── mapper
            │   └── users.xml // mybatis mapper xml
            └── vertx-default-jul-logging.properties // JUL logging
}
```
## Installation
### For Linux
#### Build
```console
$ cd bin
$ ./config-to-server.sh # That's because the author's development machine is Windows.
$ ./build.sh
```
#### Run
```console
$ cd bin
$ ./run.sh [start/stop/status] # execute vertx-fat.jar
```
### For Windows
#### Build
```console
> mvn install
> mvn clean package -f ./pom.xml
```
#### Run
```console
> run.bat # execute vertx-fat.jar
```
## License
MIT