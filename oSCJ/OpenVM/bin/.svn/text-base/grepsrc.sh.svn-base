#!/bin/sh
# grepsrc.sh string  --- greps for string over all java files
find . \( -name "*.java" -o -name '*.aj' \) -print |
  grep -v "#" | xargs grep "$@" 
