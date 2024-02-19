// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*
*   Copyright (C) 2003-2013, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  ucdstrip.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2003feb20
*   created by: Markus W. Scherer
*
*   Simple tool for Unicode Character Database files with semicolon-delimited fields.
*   Removes comments behind data lines but not in others.
*
*   To compile, just call a C compiler/linker with this source file.
*   On Windows: cl ucdstrip.c
*/

#include <stdio.h>
#include <string.h>

extern int
main(int argc, const char *argv[]) {
    static char line[2000];

    /*
     * Careful: Do not strip a comment right after the
     * UTF-8 signature byte sequence EF BB BF (U+FEFF "BOM")
     * which can occur on the first line of a UTF-8 text file.
     */
    while(gets(line)!=NULL) {
        char *end=strrchr(line, '#');
        char c;
        /*
         * Assume that a data line comment is preceded by some white space.
         * This also protects data like '#' in UCA_Rules.txt.
         */
        if(end!=NULL && end!=line && ((c=*(end-1))==' ' || c=='\t')) {
            /* ignore whitespace before the comment */
            while(end!=line && ((c=*(end-1))==' ' || c=='\t')) {
                --end;
            }
            *end=0;
        }
        puts(line);
    }

    return 0;
}
