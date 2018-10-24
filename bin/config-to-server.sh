#!/bin/sh

APP_HOME=$HOME/dev/vertx-mybatis

if [ -f $APP_HOME/pom.xml.SERVER ]; then
	echo "pom.xml conv"
	mv $APP_HOME/pom.xml $APP_HOME/pom.xml.ORG
	mv $APP_HOME/pom.xml.SERVER $APP_HOME/pom.xml
fi

if [ -f $APP_HOME/src/main/resources/vertx-default-jul-logging.properties.SERVER ]; then
	echo "log properties conv"
	mv $APP_HOME/src/main/resources/vertx-default-jul-logging.properties $APP_HOME/src/main/resources/vertx-default-jul-logging.properties.ORG
	mv $APP_HOME/src/main/resources/vertx-default-jul-logging.properties.SERVER $APP_HOME/src/main/resources/vertx-default-jul-logging.properties
fi

if [ -f $APP_HOME/src/main/resources/config/app.properties.SERVER ]; then
	echo "app.properties conv"
	mv $APP_HOME/src/main/resources/config/app.properties $APP_HOME/src/main/resources/config/app.properties.ORG
	mv $APP_HOME/src/main/resources/config/app.properties.SERVER $APP_HOME/src/main/resources/config/app.properties
fi


echo "Converting Complete !!!"
