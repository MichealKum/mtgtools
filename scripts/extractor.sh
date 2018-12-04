#!/bin/bash

cat $@ | grep -e "mtgimageid" \
 | sed -e "s#.*src=\"\([^\"]*\)\".*mtgimageid=\"\([0-9]*\)\([0-9][0-9]\)\".*#mkdir -p ../images_new/\3; cp \1 ../images_new/\3/\2\3#" \
 | while read -r cmd; do echo "-> $cmd"; eval $cmd; done