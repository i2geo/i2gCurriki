#!/bin/bash
DIR=`which $0`
cd `dirname $DIR`
langs=`./fetchlanguages.sh long`
TAB='"eng"'
for l in $langs ; do
  if [[ $l != "eng" ]]; then
    TAB=$TAB,\"$l\"
  fi
done
cp ./curriki-data-metadata.js saved-curriki-data-metadata-js
sed "s/^[ ]*Curriki\.data\.language\.list[= ].*$/Curriki\.data\.language\.list=[ $TAB ];/" < ./curriki-data-metadata.js > ./TMP
mv ./TMP ./curriki-data-metadata.js
#./concat_js.sh
