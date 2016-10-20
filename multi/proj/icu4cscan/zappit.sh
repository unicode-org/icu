#!/bin/sh
# Copyright (C) 2008-2012 IBM Corporation and Others. All Rights Reserved. 
sed -f zappit.sed | tr -s '	 \012 ' ' '
