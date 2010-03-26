#!/bin/sh

cd test-database && rm -rf h2 && mvn test && cd ..
