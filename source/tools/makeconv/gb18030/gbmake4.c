/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  gbmake4.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000oct19
*   created by: Markus W. Scherer
*
*   This tool reads and processes codepage mapping files for GB 18030.
*   Its main function is to read a mapping table with the one- and two-byte
*   mappings of GB 18030 and to then output a mapping table with all of the
*   four-byte mappings for the BMP.
*   Four-byte mappings that are included in the input are skipped in the output.
*   When an "r" argument is specified, it will instead write a list of
*   ranges of contiguous mappings where both Unicode code points and GB 18030
*   four-byte sequences form contiguous blocks.
*   This kind of output can be appended to a mapping table with a "ranges" line
*   in between, and the resulting output will exclude the input ranges.
*   This is useful for generating a partial mapping table and to handle the input
*   ranges algorithmically in conversion.
*
*   Single surrogates are excluded from the output.
*
*   To compile, just call a C compiler/linker with this source file.
*   On Windows: cl gbmake4.c
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* array of flags for each Unicode BMP code point */
static char
flags[0x10000]={ 0 };

/* flag values: 0: not assigned  1:from Unicode  2:to Unicode  4:four-byte sequence */
#define UNASSIGNED  0
#define FROMU       1
#define TOU         2
#define ROUNDTRIP   3
#define FOURBYTE    4

static void
incFourGB18030(unsigned char bytes[4]) {
    if(bytes[3]<0x39) {
        ++bytes[3];
    } else {
        bytes[3]=0x30;
        if(bytes[2]<0xfe) {
            ++bytes[2];
        } else {
            bytes[2]=0x81;
            if(bytes[1]<0x39) {
                ++bytes[1];
            } else {
                bytes[1]=0x30;
                ++bytes[0];
            }
        }
    }
}

static int
readRanges() {
    char line[200];
    char *s, *end;
    unsigned long c1, c2;

    /* parse the input file from stdin, in the format of gbkuni30.txt */
    while(gets(line)!=NULL) {
        /* skip empty and comment lines */
        if(line[0]==0 || line[0]=='#') {
            continue;
        }

        /* find the Unicode code point range */
        s=strstr(line, "U+");
        if(s==NULL) {
            fprintf(stderr, "error parsing range from \"%s\"\n", line);
            return 1;
        }

        /* read range */
        s+=2;
        c1=strtoul(s, &end, 16);
        if(end==s || *end!='-') {
            fprintf(stderr, "error parsing range start from \"%s\"\n", line);
            return 1;
        }

        s=end+1;
        c2=strtoul(s, &end, 16);
        if(end==s || *end!=' ' && *end!=0) {
            fprintf(stderr, "error parsing range end from \"%s\"\n", line);
            return 1;
        }

        /* ignore ranges above the BMP */
        if(c2>0xffff) {
            c2=0xffff;
        }

        /* set the flags for all code points in this range */
        while(c1<=c2) {
            if(flags[c1]!=UNASSIGNED) {
                fprintf(stderr, "error: range covers already-assigned U+%04lX\n", c1);
                return 1;
            }
            flags[c1++]=ROUNDTRIP|FOURBYTE;
        }
    }

    return 0;
}

extern int
main(int argc, const char *argv[]) {
    char line[200];
    char *end;
    unsigned long c, b;
    unsigned char bytes[4]={ 0x81, 0x30, 0x81, 0x30 };
    char flag;

    /* parse the input file from stdin, in the format of gbkuni30.txt */
    while(gets(line)!=NULL) {
        /* skip empty and comment lines */
        if(line[0]==0 || line[0]=='#' || line[0]==0x1a) {
            continue;
        }

        /* end of code points, beginning of ranges? */
        if(0==strcmp(line, "ranges")) {
            int result=readRanges();
            if(result!=0) {
                return result;
            }
            break;
        }

        /* read Unicode code point */
        c=strtoul(line, &end, 16);
        if(end==line) {
            fprintf(stderr, "error: missing code point in \"%s\"\n", line);
            return 1;
        }
        if(*end==':') {
            flag=ROUNDTRIP;
        } else if(*end=='>') {
            flag=FROMU;
        } else if(*end=='<') {
            flag=TOU;
        } else {
            fprintf(stderr, "error: delimiter not one of :>< in \"%s\"\n", line);
            return 1;
        }

        /* ignore non-BMP code points */
        if(c>0xffff) {
            continue;
        }

        /* read byte sequence as one long value */
        b=strtoul(end+1, &end, 16);
        if(*end!=0 && *end!='*') {
            fprintf(stderr, "error parsing byte sequence from \"%s\"\n", line);
            return 1;
        }
        if(b>0xffff) {
            flag|=FOURBYTE;
        }

        /* set the flag for the code point, make sure the mapping from Unicode is not duplicate */
        if((flags[c]&flag&FROMU)!=0) {
            fprintf(stderr, "error: duplicate assignment for U+%04lX, old flags %u, new %s\n", c, flags[c], line);
            return 1;
        }
        flags[c]|=flag;
    }

    if(argc<=1) {
        /* generate all four-byte sequences that are not already in the input */
        for(c=0x80; c<=0xffff; ++c) {
            /* skip single surrogates */
            if(c==0xd800) {
                c=0xe000;
            }
            if(flags[c]==UNASSIGNED) {
                printf("%04lX:%02X%02X%02X%02X\n", c, bytes[0], bytes[1], bytes[2], bytes[3]);
                /* increment the sequence for the next code point */
                incFourGB18030(bytes);
            } else if(flags[c]&FOURBYTE) {
                /* increment the four-byte sequence for each already-used four-byte sequence */
                incFourGB18030(bytes);
            }
        }
    } else if(0==strcmp(argv[1], "r")) {
        /* generate ranges of contiguous code points with four-byte sequences for what is not covered by the input */
        unsigned char b1[4], b2[4];
        unsigned long c1, c2;

        printf("ranges\n");
        for(c1=0x80; c1<=0xffff;) {
            /* skip single surrogates */
            if(c1==0xd800) {
                c1=0xe000;
            }

            /* get start bytes of range */
            memcpy(b1, bytes, 4);

            /* look for the first non-range code point */
            for(c2=c1; c2<=0xffff && flags[c2]==UNASSIGNED && c2!=0xd800; ++c2) {
                /* save this sequence to avoid decrementing it after this loop */
                memcpy(b2, bytes, 4);
                /* increment the sequence for the next code point */
                incFourGB18030(bytes);
            }
            /* c2 is the first code point after the range; b2 are the bytes for the last code point in the range */

            /* print this range, number of codes first for easy sorting */
            printf("%06lX  U+%04lX-%04lX  GB+%02X%02X%02X%02X-%02X%02X%02X%02X\n",
                c2-c1, c1, c2-1,
                b1[0], b1[1], b1[2], b1[3],
                b2[0], b2[1], b2[2], b2[3]);

            /* skip single surrogates */
            if(c2==0xd800) {
                c2=0xe000;
            }

            /* skip all assigned Unicode BMP code points */
            for(c1=c2; c1<=0xffff && flags[c1]!=UNASSIGNED; ++c1) {
                if(flags[c1]&FOURBYTE) {
                    /* increment the four-byte sequence for each already-used four-byte sequence */
                    incFourGB18030(bytes);
                }
            }
        }
    } else {
        fprintf(stderr, "unknown mode argument \"%s\"\n", argv[1]);
        return 2;
    }

    return 0;
}
