#!/bin/bash
cd /export/home/intergeo/platform/tomcat/webapps/static/JStrans 

langs=`./fetchlanguages.sh`


mkdir -p temp;
cd temp
rm -f *.js

for l in $langs ; do
  curl -s -o tmp 'http://i2geo.net/xwiki/bin/view/Util/JSTrans?xpage=plain&lang='${l}
  cp tmp JStrans-${l}.js
done
cd ..
rsync -c temp/*.js ./