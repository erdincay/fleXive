#!/bin/sh

cd database && rm -rf h2 && mvn test && cd ..
