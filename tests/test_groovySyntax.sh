#!/bin/bash

thisScriptPath=`dirname $(readlink -f $0)`
groovyScriptsPath=`dirname $thisScriptPath`/files/groovy
groovyChecker=$thisScriptPath/syntaxChecking.groovy
exitStatus=0

echo Checking scripts in files/groovy

for script in `ls $groovyScriptsPath`; do
    echo Testing groovy syntax for $script
    groovy $groovyChecker $script
    if [ ! $? -eq 0 ]; then
        exitStatus=1
    else
        echo PASSED
    fi
    echo
done

if [ ! $exitStatus -eq 0 ]; then
    echo Some syntax check have failed. Please fix. Exiting
fi
exit $exitStatus