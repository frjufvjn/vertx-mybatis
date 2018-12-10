#!/bin/sh

############################################################
# vertx java application server start&stop script
# Created by jwpark7@hansol.com
############################################################

JAVA_HOME="$HOME/dev/jdk1.8.0_191" # MODIFIABLE
VERSION=1.0.0 # MODIFIABLE
APP_NAME="[CSV DB Data Export Server]" # MODIFIABLE
AUTHOR="jwpark7@hansol.com"
VERTX_INSTANCES=4 # MODIFIABLE
JMX_PORT=8991 # MODIFIABLE

SERVER_IP=$(ip addr | grep 'state UP' -A2 | tail -n1 | awk '{print $2}' | cut -f1  -d'/')

JVM_OPTION="-server -Xmx512m -Xms256m -XX:MaxMetaspaceSize=128m -XX:MetaspaceSize=64m"  #"-server -Xms512m -Xmx1024m" # MODIFIABLE jdk1.8 not use : {-XX:PermSize=256m -XX:MaxPermSize=256m}
JMX_OPTION="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=$JMX_PORT -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Djava.rmi.server.hostname=$SERVER_IP"

YELLOW='\033[0;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
LIGHTGREEN='\033[1;32m'
LIGHTBLUE='\033[1;34m'
NC='\033[0m'

if [ "$2" = "" ]; then
	echo
        echo "Specific Version?? No, Set it to the default version.($VERSION)"
else
        VERSION="$2"
fi

echo
echo "####################################################################"
echo -e "\t${LIGHTGREEN}$APP_NAME${NC} Management Script"
echo -e "\t- Usage : ./run.sh [start/stop/status]"
echo -e "\t- Version: $VERSION"
echo -e "\t- Number of instances: $VERTX_INSTANCES"
echo -e "\tCreated by $AUTHOR"
echo "####################################################################"
echo

#COMMAND="$JAVA_HOME/bin/java -jar ../target/vertx-mybatis-$VERSION-fat.jar -instances $VERTX_INSTANCES" # MODIFIABLE
#COMMAND="$JAVA_HOME/bin/java $JVM_OPTION $JMX_OPTION -jar ../target/vertx-mybatis-$VERSION-fat.jar -instances $VERTX_INSTANCES" # MODIFIABLE
COMMAND="$JAVA_HOME/bin/java $JVM_OPTION -jar ../target/vertx-mybatis-$VERSION-fat.jar -instances $VERTX_INSTANCES" # MODIFIABLE

if [ "$1" = "start" ]; then
        if pgrep -f "$COMMAND"; then
                echo -e "${RED}$APP_NAME is already Running, start command skipped!!!${NC}"
                echo
        else
                echo -e "${YELLOW}Start... $APP_NAME${NC}"
                echo
                nohup $COMMAND 1>/dev/null 2>&1 &
		
		sleep 2s

		if pgrep -f "$COMMAND"; then
			echo -e "${YELLOW}$APP_NAME Started successfully${NC}!!!"
			echo
		else
			echo -e "${RED}$APP_NAME Start fail${NC}"
			echo
		fi
        fi

elif [ "$1" = "stop" ]; then
        echo -e "${YELLOW}Stop... $APP_NAME${NC}"
        echo

        pgrep -f "$COMMAND" > /dev/null 2>&1

        if [ $? -ne 0 ] ; then
                echo -e "${RED}$APP_NAME is not Running, stop command skipped!!!${NC}"
                echo
        else
                pkill -f "$COMMAND"
		
		sleep 2s

		if pgrep -f "$COMMAND"; then
			echo -e "${RED}Stop failed!!!${NC}"
			echo
		else
			echo -e "${YELLOW}Stoped successfully!!!${NC}"
			echo
		fi
        fi

elif [ "$1" = "status" ]; then
        if pgrep -f "$COMMAND"; then
                echo -e "${YELLOW}$APP_NAME is Running....${NC}"
                echo
        else
                echo -e "${YELLOW}$APP_NAME is NOT Running....${NC}"
                echo
        fi
else
        echo -e "${RED}WARNING!! Usage --> ./run.sh [start/stop/status]${NC}"
        echo
fi



RUNPID=$(pgrep -f "$COMMAND")
#echo ">>pid:$RUNPID"
PCNT=`echo $RUNPID | wc -c`

#echo ">>>cnt:$PCNT"

if [ ${PCNT} -le 1 ]; then
	echo
else
	TMPPORT=$(netstat -natp 2>/dev/null | grep ${RUNPID} | grep LISTEN | awk '{print $4}')
	RUNPORT=${TMPPORT:3}
	echo -e "${YELLOW}$APP_NAME Listening at: http://localhost:$RUNPORT${NC}"
	echo
	echo
	echo -e "${LIGHTGREEN}Port Check${NC}"
	echo "____________________________________________________________________________________________________"	
	netstat -natp 2>/dev/null | grep ${RUNPID} 
	echo "____________________________________________________________________________________________________"
	echo
fi


