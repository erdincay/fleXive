REM Start a Jetty http instance on port 8080 and a https instance on port 8443.


cd jetty
java -Xmx200m -Dopenejb.configuration=openejb.conf.xml -Dopenejb.logger.external=true -jar start.jar etc/jetty.xml etc/jetty-ssl.xml