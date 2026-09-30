#!/bin/sh
set -eu
cd "$(dirname "$0")"

cd frontend
if [ ! -d node_modules ]; then npm ci --no-audit --no-fund; fi
npm run build
cd ../java
mkdir -p lib build
if [ ! -f lib/gson-2.14.0.jar ]; then
    curl --fail --location --output lib/gson-2.14.0.jar https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar
fi
printf '%s\n' '2cbd119bf1961c28788310963dc80ba65f58cdeec1dd139c8bdb1240faa2c36f  lib/gson-2.14.0.jar' | sha256sum -c -
if command -v javac >/dev/null 2>&1; then
    javac -encoding UTF-8 -cp lib/gson-2.14.0.jar -d build src/LeadService.java src/DemoServer.java
else
    java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -cp lib/gson-2.14.0.jar -d build src/LeadService.java src/DemoServer.java
fi
exec java -cp 'build:lib/gson-2.14.0.jar' DemoServer ../frontend/dist
