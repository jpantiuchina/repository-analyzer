#!/bin/bash

echo blablabla
pmd-bin-5.8.1/bin/run.sh pmd -d "$1" -f csv -R java-design | grep 'God' > "$2"

echo miumiumiu