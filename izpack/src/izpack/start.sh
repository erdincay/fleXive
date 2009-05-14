#!/usr/bin/env sh

DIR=$(cd $(dirname $0); pwd -P)
cd $DIR/jetty-6.1.17
java -Dopenejb.configuration=openejb.conf.xml -Xmx128m -server -jar start.jar
 