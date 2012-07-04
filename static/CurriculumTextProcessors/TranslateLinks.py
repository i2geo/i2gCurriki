#!/usr/bin/python

from lxml import etree
import os
import sys

# backup the original file and open for writing
filename = sys.argv[1]
backupname = filename + "~"
os.rename(filename, backupname)
newfile = open(filename,'w')

doc = etree.parse(backupname)
body = doc.getroot()

lang = ""
while not len(lang) == 2:
    lang = raw_input("Enter the language the queries should be rendered in, using two letters (cz,de,en,es,fr,nl)\n")

for div in body:
    for anch in div:
        cmd = 'curl -s --header "Accept-Language: ' + lang + '" ' + "'http://i2geo.net/SearchI2G/render?uri=" + anch.text + "_r'"
        stdout_handle = os.popen(cmd, "r")
        curloutput = stdout_handle.read()
        if len(curloutput) == 0:
            cmd = 'curl -s --header "Accept-Language: ' + lang + '" ' + "'http://i2geo.net/SearchI2G/render?uri=" + anch.text + "'"
            stdout_handle = os.popen(cmd, "r")
            curloutput = stdout_handle.read()
        if len(curloutput) > 0:
            curloutput = '<html>' + curloutput + '</html>'
            root = etree.fromstring(curloutput)
            anch.text = root[0][1].text

newfile.writelines([
'<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" >\n',
'<html xmlns="http://www.w3.org/1999/xhtml">\n',
'  <head>\n',
'    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />\n',
'    <link rel="stylesheet" type="text/css" title="basic" media="screen,projection" href="../../mapstyle.css" />\n',
'    <style> body {background: white; color: black; font-size: 0.8em; text-align: left;} </style>\n',
'    <script type="text/javascript">\n',
'      function triggersearch(node) {\n',
'        if (parent.opener && !parent.opener.closed) {\n',
'          parent.opener.chooseNode(node);\n',
'        } else {\n',
'          i2geowindow = window.open("http://i2geo.net/","i2geo"); \n',
'          i2geowindow.chooseNode(node);\n',
'        } \n',
'        return false;\n',
'      }\n',
'    </script>\n',
'  </head>\n'])


newfile.write(etree.tostring(body))

newfile.write('</html>')

exit()
