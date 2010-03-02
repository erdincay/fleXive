#!/usr/bin/env sh
#
# Start a Jetty http instance on port 8080 and a https instance on port 8443.
#

DIR=$(cd $(dirname $0); pwd -P)
cd $DIR/jetty
java -Dopenejb.configuration=openejb.conf.xml -Xmx200m -jar start.jar etc/jetty.xml etc/jetty-ssl.xml
 