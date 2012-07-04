#!/bin/sh

# This script takes a .csv file (output from OpenOffice or MS-Excel) representing a curriculum text and adds missing double quotes (") where necessary. 

# Note, however, that commas or double quotes should not be embedded in the entries, not even escaped. 
# Don't use "a,a","b","c","d" or "a"a","b","c","d"

# usage: csvrepair [SOURCE FILE] [TARGET FILE]

# Created by Maxim Hendriks, Technische Universiteit Eindhoven, The Netherlands, 2010.

sed '
s/^,/"",/
s/,$/,""/
: loop
s/,[^",]*,/,"",/g
t loop' < $1 > $2


