#!/bin/bash
(
echo "cards.addImages(["
find $1 -type f \
 | while read fn; do basename $fn; done \
 | paste -s -d, -
echo "]);"
 )> images.json