#!/usr/bin/python

# Copyright (C) 2011 IBM Corporation and Others. All Rights Reserved.
#
# Run this like so:
#  cd /path/to/ICU
#  python /path/to/thisscript/bomfix.py
#
# it will fixup any files that have a mime-type of "utf-8" but no BOM.

import os
import svn.core, svn.client, svn.wc
import codecs


print "Fixing bom in .\n"

ctx = svn.client.svn_client_ctx_t()

tree = os.walk(".")

nots=0
notutf8=0
noprops=0
utf8=0
fixed=0
tfiles=0
bom=codecs.BOM_UTF8


for ent in tree:
    (path,dirs,files) = ent
    if(path.find("/.svn") != -1):
        continue
    for file in files:
        tfiles=tfiles+1
        revision = svn.core.svn_opt_revision_t()
        fp = os.path.abspath(path + "/" + file)
        #print "testing " + fp
        try:
            props = svn.client.propget("svn:mime-type", fp, revision, 0, ctx)
            if (not props or not props[fp]): 
                noprops = noprops + 1
                continue
            
            type = props[fp]

            if (not type == "text/plain;charset=utf-8"):
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
            
        except svn.core.SubversionException:
            nots = nots + 1
            #print "Not under vc: " + fp


print "%d files, %d not under svn, %d with no props, %d not utf8: %d utf8, %d fixed\n" % (tfiles,nots,noprops,notutf8,utf8,fixed)
