#!/bin/sh
# gee, officer grepc ... grep you!
# grepc.sh string  --- greps for string over all .c/.h/.cc/.hh files
find . \( -name "*.gen" -o -name '*.[ch]' -o -name '*.cc' -o -name '*.hh' \) -print | xargs grep "$@" 
