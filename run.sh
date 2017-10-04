#!/bin/bash

pmd-bin-5.8.1/bin/run.sh pmd -d "$1" -f csv -R java-design | grep 'God' > "$2"
pmd-bin-5.8.1/bin/run.sh pmd -d "$1" -f csv -R java-coupling | grep 'CouplingBetweenObjects' >> "$2"
pmd-bin-5.8.1/bin/run.sh pmd -d "$1" -f csv -R java-codesize | grep 'NPathComplexity' >> "$2"
