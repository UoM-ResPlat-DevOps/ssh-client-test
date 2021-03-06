#!/bin/bash

[[ -z $(which java) ]] && echo "Java is not found." >&2 && exit 1

JAR=$(dirname $0)/ssh-client-test.jar
[[ ! -f $JAR ]] && echo "${JAR} is not found." >&2 && exit 2

java -cp $JAR ssh.client.ScpTest ${1+"$@"}
