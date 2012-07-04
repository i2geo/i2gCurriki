#!/bin/sh

# This script takes a .csv file (output from OpenOffice or MS-Excel) representing a curriculum text 
# and creates a suitable set of files to browse this curriculum on the i2geo platform.
# It is dependent on the jsTree library for the drop down menu structure. The data for the menu
# is generated in an xml file.

# The csv file has to contain four entries per line, each delimited as by double quotes, as in "xxx".
# If this is not the case, first run csvrepair.
# The first line is supposed to be a header line, containing something like "ID","PID","TITLE","KEYWORD".
# This line will be automatically deleted, so don't put actual data into the first line.

# usage: csvcurr [FILE] [TARGET DIR]

# Created by Maxim Hendriks, Technische Universiteit Eindhoven, The Netherlands, 2010.



# Preparatory checks

if [ ! $2 ]
then
    echo "This script needs two arguments: a .csv file and a target directory"
    exit
fi

if [ ! -f $1 ]
then
  echo "The file $1 does not exist"
  exit
fi

if [ ! ${1##*\.} == "csv" ]
then
  echo "The file does not have the extension .csv"
  exit
fi

if [ ! -d $2 ]
then
    echo "The second argument is not a directory"
    exit
fi

filename=${1##*/}

targetdir=`echo "$2" | grep -e '.*/$'`
if [ "$targetdir" != $2 ]
then
  targetdir=$2'/'
else
  targetdir=$2
fi


AskForOverwrite()
{
  if [ -f "$1" ]
  then
    echo "The file $1 already exists. Overwrite? (Enter to confirm, non-empty input to quit)"
    read perm
    if [ "$perm" != "" ]
    then
      writestatus="no"
      return
    fi
    rm "$1"
  fi
  writestatus="yes"
}


# Check if the .CSV file is well-formed

malformedlines=`sed '1,1 d' $1 | grep -v '^"[^"]*","[^"]*","[^"]*","[^"]*"$'`

if [ "$malformedlines" != "" ]
then
  echo "The .csv file is malformed. The following lines do not contain four values separated by commas:"
  echo "$malformedlines"
  exit
fi



# Create the XML file containing the menu tree with the aid of jsTree.

xmlfile=$targetdir${filename%\.*}"_tree.xml"
xmlfileshort=${filename%\.*}"_tree.xml"
AskForOverwrite $xmlfile

if [ $writestatus = "yes" ]
then
  echo '<?xml version="1.0" encoding="UTF-8"?>' >> $xmlfile
  echo '<root>' >> $xmlfile

sed '
1,1 d
/^[",]*$/d
s/"\([^"]*\)"/<item id="\1" /1 
s/,"\([^"]*\)"/parent_id="\1">/1
s_,"\([^"]*\)".*_<content><name><\![CDATA[\1]]></name></content></item>_1' < $1 >> $xmlfile

  echo '</root>' >> $xmlfile
fi


# Create the HTML file containing queries (links to the intergeo ontology) from the field with keys in the .CSV.

queryfile=$targetdir${filename%\.*}"_queries.html"
queryfileshort=${filename%\.*}"_queries.html"
AskForOverwrite $queryfile

if [ $writestatus = "yes" ] 
then
  echo '  <body>' >> $queryfile

sed '
1,1 d
/^[",]*$/d
s#\("[^"]*"\),"[^"]*","[^"]*","\([^"]*\)"#<div id=\1 style="display: none;">\
\2\
</div>#' < $1 |
sed '
/^[A-Za-z0-9_ ]/ s/[^A-Za-z0-9_]/ /g
/^[A-Za-z0-9_ ]/ s/\([A-Za-z0-9_]*\)/\1\
/g
/^[A-Za-z0-9_ ]/ s/ //g' |
sed '
/^$/d
/^[A-Za-z0-9_ ]/ s_\(.*\)_      <a href="#" onClick="triggersearch('\''\1'\'');">\1</a>_' >> $queryfile

# old version
#sed '
#/^$/d
#/^[A-Za-z0-9_ ]/ s_\(.*\)_      <a href="http://i2geo.net/comped/show.html?uri=\1" onClick="if (parent.opener \&\& !parent.opener.closed) {parent.opener.chooseNode('\''\1'\'');} else {i2geowindow = window.open('\''http://i2geo.net/'\'','\''i2geo'\''); i2geowindow.chooseNode('\''\1'\'');} return false;">\1</a>_' >> $queryfile

  echo '  </body>' >> $queryfile

  python TranslateLinks.py $queryfile

fi



# The descriptions of the tree items (sections, subsections etc.) has to be given in a separate file, 
# with identifiers attached to <div> elements that correspond to the identifiers in the .CSV. 
# An HTML content file can be created automatically, if the user wishes. This will then not contain 
# any text, but it will already contain all the <div> elements.

contentfile=$targetdir${filename%\.*}"_content.html"
contentfileshort=${filename%\.*}"_content.html"

echo "Would you like to generate an empty HTML file for the content description of the nodes of the tree? (Enter to confirm, non-empty input to quit)"
read createcontent
if [ "$createcontent" = "" ]
then
  AskForOverwrite $contentfile

  if [ $writestatus = "yes" ]
  then
    echo '<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">' >> $contentfile
    echo '<html xmlns="http://www.w3.org/1999/xhtml">' >> $contentfile
    echo '  <head>' >> $contentfile
    echo '    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />' >> $contentfile
    echo '    <link rel="stylesheet" type="text/css" title="basic" media="screen,projection" href="../../mapstyle.css" />' >> $contentfile
    echo '    <style> body {background: white; color: black; font-size: 0.8em; text-align: left;} </style>' >> $contentfile
    echo '  </head>' >> $contentfile
    echo '  <body>' >> $contentfile

sed '
1,1 d
/^[",]*$/ d
s#\("[^"]*"\).*#    <div id=\1 style="display: none;">\
    </div>#' < $1 >> $contentfile

    echo '  </body>' >> $contentfile
    echo '</html>' >> $contentfile
  fi
fi



# Finally, create the main HTML page combining the correct tree and the corresponding HTML content file.

mainfile=$targetdir${filename%\.*}"_main.html"
AskForOverwrite $mainfile

if [ $writestatus = "yes" ] 
then

  echo "What title should be used at the top of the main page? (Press Enter to use the file name as default)"
  read title

  if [ "$title" = "" ]
  then
    title=${filename%\.*}
  fi

sed '
s/\$TITLE/'"$title"'/
s#\$XMLTREEFILE#'$xmlfileshort'#
s/\$CONTENTFILE/'$contentfileshort'/ 
s/\$QUERYFILE/'$queryfileshort'/ ' < curriculumtext_mainfile_template.html > $mainfile

fi






