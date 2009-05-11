[fleXive] 3.1
-------------

* To start a [fleXive] server:
    On Unix:    sh start.sh
    On Windows: start.bat
    Other OS:   cd jetty-6.1.17
                java -Dopenejb.configuration=openejb.conf.xml -Xmx128m -jar start.jar

* Backend URL: http://localhost:8080/flexive/adm/

* Backend default login:
    User:       supervisor
    Password:   supervisor

* Uninstall:
    java -jar Uninstaller/uninstaller.jar


Data sources
------------

* Configuration in openejb.conf.xml

* Create a new schema in flexive-dist (configuration: database.properties):
    ant db.create
    ant db.config.create

Creating new projects:
----------------------

* Create an Ant-based project:
    cd flexive-dist
    ant project.create

* Deploy an EAR to Jetty:
    ant deploy.jetty
    (You can set the Jetty directory and EAR filename from the command line, e.g.
     -Djetty.dir=../jetty -Dear.file=dist/my-project.ear)

* Using Maven: see
    http://www.flexive.org/docs/website/writing_applications_maven.html