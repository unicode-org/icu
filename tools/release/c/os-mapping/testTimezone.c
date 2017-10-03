// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
******************************************************************************
*
*   Copyright (C) 2006-2008, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
*  FILE NAME : testTimezone.c
*
*   Date        Name        Description
*   03/02/2006  grhoten/mow Creation.
******************************************************************************
*/

#include "unicode/putil.h"
#include "unicode/udat.h"
#include <stdio.h>
#include <string.h>
#include <time.h>

int main(int argc, const char* const argv[]) {
    UErrorCode status = U_ZERO_ERROR;
    char sysTimeStr[256];
    struct tm summerTimeTM;
    struct tm winterTimeTM;

    memset(sysTimeStr, 0, sizeof(sysTimeStr));
    memset(&summerTimeTM, 0, sizeof(summerTimeTM));
    memset(&winterTimeTM, 0, sizeof(winterTimeTM));

    strptime("2006-06-01 12:00", "%Y-%m-%d %H:%M", &summerTimeTM);
    strftime(sysTimeStr, sizeof(sysTimeStr)-1, "%Y-%m-%d %H:%M", &summerTimeTM);
    puts(sysTimeStr);
}
