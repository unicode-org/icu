#!/usr/bin/python
# -*- coding: utf-8 -*-

# Copyright © 2016 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
# Copyright (C) 2011 IBM Corporation and Others. All Rights Reserved.
#
# Run this like so:
#  cd /path/to/ICU
#  python /path/to/thisscript/bomfix.py
#
# it will fixup any files that have a mime-type of "utf-8" but no BOM.

import os
import codecs
import subprocess

print "Fixing bom in .\n"

ctx = None

tree = os.walk(".")

nots=0
notutf8=0
noprops=0
utf8=0
fixed=0
tfiles=0
bom=codecs.BOM_UTF8


# my own rewrite
def my_propget(prop, path, ignored_rev, ignored_recurs, ignored_ctx):
    "function_docstring"
    try:
        return subprocess.check_output(["svn", "pg", prop, path], stderr=subprocess.STDOUT)
    except subprocess.CalledProcessError as cpe:
        # now decode the error
        if "E200005" in cpe.output:
            # not under version control
            return None
        if "W200017" in cpe.output:
            # property not found
            return None
        else:
            print "On " + fp + ":\n" + cpe.output + "\n"
            print "This error wasn't recognized by bomfix, sorry."
            raise cpe

for ent in tree:
    (path,dirs,files) = ent
    if(path.find("/.svn") != -1):
        continue
    for file in files:
        tfiles=tfiles+1
        revision = None
        # use relative path
        fp = path + "/" + file
        #print "testing " + fp
        props = my_propget("svn:mime-type", fp, revision, 0, ctx)
        if not props:
            noprops = noprops + 1
            continue

        if (fp == "./LICENSE"):
            print "Skipping: %s" % fp
            continue

        type = props

        # ends with \n because of process
        if (not type == "text/plain;charset=utf-8\n"):
            #print fp + ": delta " + type
            notutf8 = notutf8 + 1
            continue

        # fp is utf-8
        utf8=utf8+1

        f = open(fp, 'rb')
        bytes=f.read(3)
        if not bytes:
            print fp + ": could not read 3 bytes"
            continue
        elif (bytes == bom):
            #print fp + ": OK"
            continue

        f.seek(0)

        os.rename(fp,fp+".tmp")
        o=open(fp,'wb')
        o.write(bom)
        while(1):
            bytes = f.read(2048)
            if bytes:
                o.write(bytes)
            else:
                break
        o.close()
        f.close()
        os.remove(fp+".tmp")
        fixed=fixed+1


        print fp
            


print "%d files, %d not under svn, %d with no props, %d not utf8: %d utf8, %d fixed\n" % (tfiles,nots,noprops,notutf8,utf8,fixed)
