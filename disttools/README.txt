[fleXive] 3.0 README
====================

Thank you for using [fleXive]. This directory contains all [fleXive] libraries,
the administration backend application and an Ant-based build system for
managing your projects.

A complete description of the [fleXive] distribution can be found in the
reference documentation, available online at
http://www.flexive.org/docs/website/writing_applications.html


Prerequisites
=============

For working with the [fleXive] distribution you need:

* JDK 5 or higher
* Apache Ant 1.7 or higher: http://ant.apache.org/


Getting started
===============

To get started, open a command shell in this directory and type "ant".
If Ant is installed correctly you will see a brief explanation of the
build targets. You can enter a build target or execute it from
the command line using "ant <build target>".

To create a deployable EAR file, execute "ant ear". Then copy flexive.ear to
your application server's deployment directory.

To setup your databases, enter your database configuration in "database.properties"
in this directory and execute "ant db.create" and "ant db.config.create".

If you are a Glassfish (Sun Application Server) user, execute "ant glassfish.libs"
to copy all libraries needed for Glassfish compatibility to a directory. For further
information please consult the reference documentation.


Resources
=========

http://www.flexive.org
http://www.flexive.org/documentation/reference-documentation.html
http://forum.flexive.org
http://wiki.flexive.org


