/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  gbsingle.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000oct26
*   created by: Markus W. Scherer
*
*   This tool reads a mapping table in a very simple format with combined syntax
*   for mappings from Unicode to GB 18030 and back and turns it into
*   a single-direction file with only either mapping direction.
*   The input format is as follows:
*       unicode [':' | '>'] codepage ['*']
*   With
*       unicode = hexadecimal number 0..10ffff
*       codepage = hexadecimal number 0..ffffffff for big-endian bytes
*       ':' for roundtrip mappings
*       '>' for fallbacks from Unicode to codepage
*       '*' ignored
*
*   The output format is as follows:
*   With no command line argument:
*       unicode ':' codepage
*   With a "gb" command line argument:
*       codepage ':' unicode
*
*   To compile, just call a C compiler/linker with this source file.
*   On Windows: cl gbsingle.c
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

extern int
main(int argc, const char *argv[]) {
    char line[200];
    char *end;
    unsigned long c, b;
    unsigned char fallback;
    char uniToGB;

    if(argc<=1) {
        puts("# Unicode:GB 18030");
        uniToGB=1;
    } else if(0==strcmp(argv[1], "gb")) {
        puts("# GB 18030:Unicode");
        uniToGB=0;
    } else {
        fprintf(stderr, "unknown argument %s\n", argv[1]);
        return 2;
    }

    /* parse the input file from stdin */
    while(gets(line)!=NULL) {
        /* pass through empty and comment lines */
        if(line[0]==0 || line[0]=='#' || line[0]==0x1a) {
            puts(line);
            continue;
        }

        /* end of code points, beginning of ranges? */
        if(0==strcmp(line, "ranges")) {
            break; /* ignore the rest of the file */
        }

        /* read Unicode code point */
        c=strtoul(line, &end, 16);
        if(end==line || *end!=':' && *end!='>') {
            fprintf(stderr, "error parsing code point from \"%s\"\n", line);
            return 1;
        }
        if(*end==':') {
            fallback=0;
        } else {
            fallback=1;
        }

        /* read byte sequence as one long value */
        b=strtoul(end+1, &end, 16);
        if(*end!=0 && *end!='*') {
            fprintf(stderr, "error parsing byte sequence from \"%s\"\n", line);
            return 1;
        }

        if(uniToGB) {
            /* output Unicode:GB 18030 including fallbacks */
            printf("%04lx:%02lx\n", c, b);
        } else if(!fallback) {
            /* output GB 18030:Unicode excluding fallbacks */
            printf("%02lx:%04lx\n", b, c);
        }
    }

    return 0;
}
