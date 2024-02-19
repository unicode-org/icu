#!/bin/sh
#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
#*****************************************************************************
#
#   Copyright (C) 2006-2008, International Business Machines
#   Corporation and others.  All Rights Reserved.
#
#*****************************************************************************
timezones=`locate zoneinfo | fgrep -v evolution-data-server`
for tzone in $timezones
do 
    if [ -f $tzone ]; then
        tzname=${tzone#/usr/share/zoneinfo/}
        ICUTime=`LD_LIBRARY_PATH=../dev-icu/icu/source/lib/ TZ=$tzname ./checkTimezone`
        systemHour=`TZ=$tzname date +%I`
        # OS X does not allow %-I so this ugly mess needs to occur
        if [ "${systemHour:0:1}" == "0" ]; then
            systemHour=${systemHour:1:1}
        fi
        systemTimeShort=`TZ=$tzname date +%b\ %d,\ %Y`
        systemTimeShort="${systemTimeShort} ${systemHour}"
        systemTimeAMPM=`TZ=$tzname date +%p`
        index1=`expr "$systemTimeShort" : '.*'`
        index2=`expr "$ICUTime" : '.*'`-2
        ICUTimeShort=${ICUTime:0:${index1}}
        ICUAMPM=${ICUTime:${index2}:2}
        systemTime=`TZ=$tzname date +%b\ %d,\ %Y\ %r`
        if [ "$systemTimeShort" == "$ICUTimeShort" ] && [ "$systemTimeAMPM" == "$ICUAMPM" ]; then
            if [ "$opt1" != "-bad" ]; then
                echo TZ=$tzname
                echo System: $systemTime
                echo ICU: $ICUTime
            fi
        else 
            if [ "$opt1" == "-all" ]; then
                echo --TZ=$tzname
                echo --System: $systemTime
                echo --ICU: $ICUTime
            else
                echo TZ=$tzname
                echo System: $systemTime
                echo ICU: $ICUTime
            fi
        fi
    fi
done
