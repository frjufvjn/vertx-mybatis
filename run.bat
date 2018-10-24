@echo off

SETLOCAL
rem vertx run io.frjufvjn.lab.vertx_mybatis.DatabaseVerticle -cp target/vertx-mybatis-1.0.0-fat.jar
java -jar target/vertx-mybatis-1.0.0-fat.jar -instances 4

ENDLOCAL