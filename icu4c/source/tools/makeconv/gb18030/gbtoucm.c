/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  gbtoucm.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000oct19
*   created by: Markus W. Scherer
*
*   This tool reads a mapping table in a very simple format and turns it into
*   .ucm file format.
*   The input format is as follows:
*       unicode [':' | '>'] codepage ['*']
*   With
*       unicode = hexadecimal number 0..10ffff
*       codepage = hexadecimal number 0..ffffffff for big-endian bytes
*       ':' for roundtrip mappings
*       '>' for fallbacks from Unicode to codepage
*       '*' ignored
*
*   To compile, just call a C compiler/linker with this source file.
*   On Windows: cl gbtoucm.c
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

        /* output in .ucm format */
        if(b<=0xff) {
            printf("<U%04lx> \\x%02x |%u\n", c, b, fallback);
        } else if(b<=0xffff) {
            printf("<U%04lx> \\x%02x\\x%02x |%u\n", c, b>>8, b&0xff, fallback);
        } else if(b<=0xffffff) {
            printf("<U%04lx> \\x%02x\\x%02x\\x%02x |%u\n", c, b>>16, (b>>8)&0xff, b&0xff, fallback);
        } else {
            printf("<U%04lx> \\x%02x\\x%02x\\x%02x\\x%02x |%u\n", c, b>>24, (b>>16)&0xff, (b>>8)&0xff, b&0xff, fallback);
        }
    }

    return 0;
}
