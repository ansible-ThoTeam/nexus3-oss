#!/usr/bin/env bash

thisScriptPath=`dirname $(readlink -f $0)`
rolePath=`dirname $thisScriptPath`
groovyScriptsPath=${rolePath}/files/groovy
groovyChecker=$thisScriptPath/syntaxChecking.groovy
exitStatus=0

echo Checking scripts in files/groovy and template templates/backup.groovy.j2
scriptFiles=`ls -d -A -1 $groovyScriptsPath/*`
scriptFiles="${scriptFiles} ${rolePath}/templates/backup.groovy.j2"

for script in ${scriptFiles} ; do
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
