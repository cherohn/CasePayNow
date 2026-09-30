#!/bin/sh
set -eu

cd "$(dirname "$0")"
mkdir -p build

if command -v javac >/dev/null 2>&1; then
    javac -encoding UTF-8 -Xlint:all -Werror -d build src/LeadService.java tests/LeadServiceTest.java
else
    # Some installations include the compiler module without the javac executable.
    java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -Xlint:all -Werror -d build src/LeadService.java tests/LeadServiceTest.java
fi

java -cp build LeadServiceTest
