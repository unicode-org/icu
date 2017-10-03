#!/bin/sh
#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (c) 2009 IBM Corp. and Others. All Rights Reserved
#
# Sample "super configure" 
# this script is responsible for configuring ICU.

# input variables;
# $ICU_VER  - the ICU version in underscore format (4_2_0_1)

ICU_SRC="$1"
ICU_VER="$2"

U_HOST=`hostname`
U_SYS=`uname || echo unknown`
#echo $HOST

export rcs=none

case $U_SYS in
	AIX)
		# the preferred 'AIX' type to use
		AIX=AIX
		case $ICU_VER in 
		4*)
			#AIX=AIX4.3VA
			AIX=AIX
			;;
		*)
			AIX=AIX4.3VA
			;;
	        1*|2*|3_0*|3_1*)
			AIX=AIX4.3xlC
			PATH=/usr/vacpp/bin/:$PATH
			;;
		esac
		rcs=${AIX}
		;;
	*)
		rcs=none
		;;
esac



case $HOST in
        sys98*)
                rcs=$AIX
                ;;
        hp*)
                rcs='HP-UX/ACC'
                ;;
        merill*|redhat*|sunlight*)
                rcs='LinuxRedHat'
                ;;
        *)
		echo sh ${ICU_SRC}/configure
                #echo Unknown host $HOST, edit $0
                exit 0
                ;;
esac

echo sh ${ICU_SRC}/runConfigureICU "${rcs}"




