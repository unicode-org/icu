#!/bin/sh
#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (C) 2008-2012 IBM Corporation and Others. All Rights Reserved. 
sed -f zappit.sed | tr -s '	 \012 ' ' '
