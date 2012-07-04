#!/bin/sh

## wget -O bigList.html 'http://i2geo.net/xwiki/bin/view/XWiki/BigListTraces?xpage=plain'
curl -o bigList.html 'http://localhost:53080/xwiki/bin/view/XWiki/BigListTraces?xpage=print'
cp bigList.html BigListTraces.html
