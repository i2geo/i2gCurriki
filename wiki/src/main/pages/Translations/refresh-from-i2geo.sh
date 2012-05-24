#!/bin/sh

commandHere="$0"
binPath=`dirname $commandHere`
cd ${binPath}
echo "Updating phrases in directory \"`pwd`\""

## languages to be updated: german and english are in CVS only
languages=`curl -s "http://i2geo.net/xwiki/bin/view/XWiki/SupportedLanguages?xpage=plain&len=2"`

baseURL="http://i2geo.net/xwiki/bin/view/Translations/"
files="XWiki CRS Registration FileCheck MyCurriki Groups ToTranslate CreateResources Search GlobalClassTranslations QF-Translations DocumentBundle SKBi18n CompEd EditView"
SOURCE=`mktemp ./TMPXXXXXX`
XSL=`mktemp ./TMPxslXXXXXX`
echo With the following languages: $languages

dumpHeader() {
cat <<- the-end
#
# \$Id: refresh-from-i2geo.sh,v 1.4 2010/08/17 12:57:34 paul Exp ${language}.properties \$
#
# Phrase file for I2Geo for language $language
# ---- please do not edit here
# ---- please edit at http://i2geo.net/xwiki/bin/view/Translations/${file}?language=${language}
# 
the-end
}


cat >"$XSL" <<- the-end
<xsl:stylesheet version = '1.0'
 xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
 <xsl:output method='text'/>
 <xsl:template match='/'>
 <xsl:value-of select='//content'/>
 </xsl:template>
</xsl:stylesheet>
the-end


echo "Starting through languages..."
for file in $files ; do
echo
echo "==== $file ======= "
for language in $languages ; do
  export language
  url="${baseURL}${file}?xpage=xml&language=${language}"
  echo "${language}: Fetching source of \n  ${url} "
  XML_PAGE=`mktemp ./TMPXXXXXX`
  curl -s "$url" > $XML_PAGE
  xsltproc $XSL $XML_PAGE > $SOURCE

  ( dumpHeader && cat $SOURCE) | \
    sed 's|\(^1 .*$\)|\# \1|'  |  \
    sed 's|\(^<pre>$\)|\# \1|' | \
    sed 's|\(^</pre>$\)|\# \1|' > "${file}.${language}.properties"
  rm $XML_PAGE
done
done
rm $XSL
rm $SOURCE
echo " ============================ "
echo "... finished updating phrases."
