/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  ucmstrip.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000nov09
*   created by: Markus W. Scherer
*
*   This tool reads a .ucm file, expects there to be a line in the header with
*   "File created on..." and removes the lines before and including that.
*   Then it removes lines with <icu:state> and <uconv_class> and <code_set_name>.
*   This helps comparing .ucm files with different copyright statements and
*   different state specifications.
*
*   To compile, just call a C compiler/linker with this source file.
*   On Windows: cl ucmstrip.c
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

extern int
main(int argc, const char *argv[]) {
    char line[200];
    char *s, *end;
    unsigned long b, i, mappingsTop=0;

    /* parse the input file from stdin */
    /* skip lines until and including the one with "created on" */
    for(;;) {
        if(gets(line)==NULL) {
            return 0;
        }
        if(0==strncmp(line, "# File created on ", 18)) {
            break;
        }
    }

    /* write all lines except with <uconv_class> and <icu:state> and <code_set_name> */
    for(;;) {
        if(gets(line)==NULL) {
            return 0;
        }
        if(0!=strncmp(line, "<uconv_class>", 13) && 0!=strncmp(line, "<icu:state>", 11) && 0!=strncmp(line, "<code_set_name>", 14)) {
            puts(line);
        }
    }
}
