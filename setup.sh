#!/bin/bash
if [ ! -f codenameone_settings.properties ]
then
	cp codenameone_settings.properties.sample codenameone_settings.properties
fi
DIR="$( cd "$(dirname "$0")" ; pwd -P )"
UPDATE_JAR=$HOME/.codenameone/UpdateCodenameOne.jar
if [ ! -f $UPDATE_JAR ]
then
	curl https://www.codenameone.com/files/updates/UpdateCodenameOne.jar > $UPDATE_JAR
fi
rm Versions.properties
java -jar $UPDATE_JAR $DIR force

