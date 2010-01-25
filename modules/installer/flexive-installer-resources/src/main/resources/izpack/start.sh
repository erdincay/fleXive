#!/usr/bin/env sh

DIR=$(cd $(dirname $0); pwd -P)
cd $DIR/jetty
java -Dopenejb.configuration=openejb.conf.xml -Xmx200m -jar start.jar
 