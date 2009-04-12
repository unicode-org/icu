#  Copyright (c) 2008-2009, International Business Machines Corporation and others. All Rights Reserved.
# 
#
# Makefile for regenerating configure in the face of a bad ^M
# This should become unnecessary for autoconf past 2.63
# 
# Usage:    MAKE -f configure.mk configure

configure:	configure.in ./aclocal.m4
	( autoconf && mv configure configure.tmp && sed -e 's%^ac_cr=.*%ac_cr=`echo X |tr X "\\015"`%'  < configure.tmp > configure && chmod a+rx $@ && rm configure.tmp ) || ( rm $@ ; "echo configure build failed" ; /usr/bin/false  )

