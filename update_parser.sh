#!/bin/bash

# Until we're publishing gxformat2 to Maven Central just copy from
# gxformat2 source (assumed to be under parent directory).
package="org/galaxyproject/gxformat2"

for src_type in "main/java" "test/java" "test/resources"
do
    rm -rf "src/${src_type}/${package}"
    mkdir -p "src/${src_type}/${package}"
    cp -a "../gxformat2/java/src/${src_type}/${package}" "src/${src_type}/${package}/.."
done
# Depends on files not copied over yet...
rm "src/test/java/${package}/LintExamplesTest.java"
rm "src/test/java/${package}/CytoscapeExamplesTest.java"
