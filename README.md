# vertx-mybatis
Use asynchronous non-blocking database access using the sql client provided by vertx. Then you organize your project so that it is easy to use without giving up the SQL Mapper framework (MyBatis) ~~ORM framework~~. 
## Description
### Synopsis
#### pure vertx sql client
The code block below is an example of a pure sql client code provided by vertx. I wanted to build an sql service using (without giving up) an SQL Mapper framework like mybatis.
```java
final SQLConnection connection = conn.result();
connection.queryWithParams("select * from test where id = ?", new JsonArray().add(2), rs -> {
    ...
});
```
#### get query and param
Once you have mybatis sqlsession, you can use BoundSql and ParameterMapping methods to retrieve queries and parameters and use them in your vertex sql client. See [QueryGetter.java](src/main/java/io/frjufvjn/lab/vertx_mybatis/query/QueryServiceImp.java)
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
#### The way to make REST API
1. Create or Edit MyBatis mapper.xml
2. URL
- C: /api/create, R: /api/read, U: /api/update, D: /api/delete
- Batch C: /api/create/multi, U: /api/update/multi, D: /api/delete/multi
3. HTTP POST method Call

```
header --> "Authorization": "Bearer `JWT Token`" // It can be obtained through "/api/newToken" service.
payload = {
    sqlName: 'sqlid', // mybatis sql id
    param1: 'foo',
    param2: 'bar',
    ...
}
```
#### CRUD API feature
[related codes](./src/main/java/io/frjufvjn/lab/vertx_mybatis/common/ApiRequestCommon.java)
#### Batch CUD API feature
[related codes](./src/main/java/io/frjufvjn/lab/vertx_mybatis/common/ApiRequestCommon.java)
#### Realtime changed data Pub/Sub feature
##### - MySQL
Use [mysql-binlog-connector-java](https://github.com/shyiko/mysql-binlog-connector-java)
& Inspired by [vertx-mysql-binlog-client](https://github.com/guoyu511/vertx-mysql-binlog-client)

Thanks!!

Unlike the [vertx-mysql-binlog-client](https://github.com/guoyu511/vertx-mysql-binlog-client), however, it does not release row-level events, but instead emit events in TR range.

[related codes](./src/main/java/io/frjufvjn/lab/vertx_mybatis/mysqlBinlog/BinLogClientVerticle.java)

##### - PostgreSQL (WIP)
#### GraphQL Integration (WIP)
#### Large DB data csv export
_Recently, we had to add a large Excel download to an existing web application that had a lot of side effects when adding services._
1. Invoke the REST API with parameters including the number of patches to the legacy application (Spring framework), assuming 50,000 data requests.
2. Establish appropriate paging rules (eg, 10,000 responses) to invoke the DB service by dividing 50,000 requests into 10,000 requests. 
3. Call the DB service n times as defined in step 2.
4. Use vertex sql client to process asynchronous non-block.
5. Process vertex cvs parsing using queryStream () of vertx and generate csv file asynchronously using vertex fileSystem () feature. You can see that the processed result is different to the order requested, as shown by the arrow in the figure. (AsyncResult)
6. CompositeFuture.all () is used to wait for the future of all the processing results, and then batches the response processing.
7. Get the csv file path list in the Legacy Application.
8. Stream the HTTP chunked response with the file list in step 7.

* Related Codes
    * [MainVerticle.java](./src/main/java/io/frjufvjn/lab/vertx_mybatis/MainVerticle.java)
    * [RestFulController.java](./client-test/RestFulController.java)
    * [SubVerticle.java](./src/main/java/io/frjufvjn/lab/vertx_mybatis/SubVerticle.java)

![title](/img/vertx-mybatis.png)

### Codes View
![title](/img/diagram.png)
### Codes Tree
```java
public void main () {
.
├── bin
│   ├── build.sh // Maven build script
│   ├── config-to-server.sh // Apply server properties
│   ├── memchk.sh // Linux Server Target Process Memory View
│   └── run.sh // This application start and stop script
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
        │                   ├── BareInstance.java
        │                   ├── Constants.java
        │                   ├── MainVerticle.java // Main Verticle
        │                   ├── SubVerticle.java
        │                   ├── common
        │                   │   ├── ApiErrorType.java
        │                   │   ├── ApiRequestCommon.java // API Request
        │                   │   └── ApiResponseCommon.java // API Response
        │                   ├── factory
        │                   │   ├── MyBatisConnectionFactory.java // MyBatis Connection
        │                   │   └── VertxSqlConnectionFactory.java // Vertx JDBC Client Connection
        │                   ├── mysqlBinlog
        │                   │   ├── BinLogClientVerticle.java
        │                   │   ├── BinlogEventType.java
        │                   │   └── SchemaService.java
        │                   ├── query
        │                   │   ├── QueryModule.java
        │                   │   ├── QueryServiceImp.java // Query Getter Using Mybatis
        │                   │   └── QueryServices.java
        │                   ├── secure
        │                   │   ├── CryptoManager.java
        │                   │   ├── CryptoModule.java
        │                   │   └── CryptoService.java
        │                   └── sql
        │                       ├── EBSqlServiceVerticle.java // eventbus sql verticle
        │                       ├── SqlServiceImp.java // API DB Service implement
        │                       └── SqlServices.java
        ├── js
        │   └── jsVerticle.js
        └── resources
            ├── config
            │   ├── app.properties // application property
            │   ├── db-config.xml // mybatis config
            │   ├── db.properties // db connection information property
            │   ├── db.properties.SAMPLE
            │   ├── keystore.jceks
            │   ├── pubsub-mysql-server.json // mysql pubsub server connection config
            │   └── pubsub-mysql-service.json // mysql pubsub service config
            ├── log4j2.xml // log4j2 log
            ├── mapper
            │   ├── test.xml // mybatis mapper xml
            │   └── users.xml
            ├── vertx-default-jul-logging.properties // JUL log (not use in this project)
            └── webroot
                ├── dbRTC.js
                ├── index.html // API Service Test Page (http://localhost:18080)
                ├── sha256.js
                └── ws-test.html
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
See http://localhost:18080
## License
MIT
