#!/bin/bash

# Until we're publishing gxformat2 to Maven Central just copy from
# gxformat2 source (assumed to be under parent directory).
# gxformat2 is cloned from https://github.com/galaxyproject/gxformat2
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

# Denis: restore modified files that are not present/modified in gxformat2
git checkout src/main/java/org/galaxyproject/gxformat2/Cytoscape.java
git restore src/main/java/org/galaxyproject/gxformat2/CytoscapeDAG.java
git restore src/test/java/org/galaxyproject/gxformat2/CytoscapeTest.java
