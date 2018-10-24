#!/bin/sh
export MAVEN_HOME=/home/stat/dev/apache-maven-3.5.3/
export JAVA_HOME=/home/stat/dev/jdk1.8.0_181/


#$MAVEN_HOME/bin/mvn install -f ../pom.xml
$MAVEN_HOME/bin/mvn clean package -o -f ../pom.xml 
