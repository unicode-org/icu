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
*   When an "r" argument is specified, it will instead write a list of
*   ranges of contiguous mappings where both Unicode code points and GB 18030
*   four-byte sequences form contiguous blocks.
*   This kind of output can be appended to a mapping table with a "ranges" line
*   in between, and the resulting output will exclude the input ranges.
*   This is useful for generating a partial mapping table and to handle the input
*   ranges algorithmically in conversion.
*
*   To compile, just call a C compiler/linker with this source file.
*   On Windows: cl gbmake4.c
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* in the printed standard, U+303e is mismapped; this sequence must be skipped */
static const unsigned char skip303eBytes[4]={ 0x81, 0x39, 0xa6, 0x34 };

/* array of flags for each Unicode BMP code point */
static char
flags[0x10000]={ 0 };
/* flag values: 0: not assigned  1:one/two-byte sequence  2:four-byte sequence */

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

static void
incSkipFourGB18030(unsigned char bytes[4]) {
    incFourGB18030(bytes);
    if(0==memcmp(bytes, skip303eBytes, 4) && flags[0x303e]==1) {
        /* make sure to skip the mismapped sequence if the data correctly maps U+303e==GB+a989 */
        incFourGB18030(bytes);
    }
}

static int
readRanges() {
    char line[200];
    char *s, *end;
    unsigned long c1, c2;

    /* parse the input file from stdin, in the format of gb18030markus2.txt */
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

        /* set the flags for all code points in this range */
        while(c1<=c2) {
            flags[c1++]=2;
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

    /* parse the input file from stdin, in the format of gb18030markus2.txt */
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
        if(end==line || *end!=':' && *end!='>') {
            fprintf(stderr, "error parsing code point from \"%s\"\n", line);
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

        /* set the flag for the code point */
        flags[c]= b<=0xffff ? 1 : 2;
    }

    if(argc<=1) {
        /* generate all four-byte sequences that are no already in the input */
        for(c=0x81; c<=0xffff; ++c) {
            if(flags[c]==0) {
                printf("%04lx:%02x%02x%02x%02x\n", c, bytes[0], bytes[1], bytes[2], bytes[3]);
            }
            if(flags[c]!=1) {
                incSkipFourGB18030(bytes);
            }
        }
    } else if(0==strcmp(argv[1], "r")) {
        /* generate ranges of contiguous code points with four-byte sequences for what is not covered by the input */
        unsigned char b1[4], b2[4];
        unsigned long c1, c2;

        printf("ranges\n");
        for(c1=0x81; c1<=0xffff;) {
            /* get start bytes of range */
            memcpy(b1, bytes, 4);

            /* look for the first non-range code point */
            for(c2=c1; c2<=0xffff && flags[c2]==0; ++c2) {
                /* save this sequence to avoid decrementing it after this loop */
                memcpy(b2, bytes, 4);
                /* increment the sequence for the next code point */
                incSkipFourGB18030(bytes);
            }
            /* c2 is the first code point after the range; b2 are the bytes for the last code point in the range */

            /* print this range, number of codes first for easy sorting */
            printf("%06lx  U+%04lx-%04lx  GB+%02x%02x%02x%02x-%02x%02x%02x%02x\n",
                c2-c1, c1, c2-1,
                b1[0], b1[1], b1[2], b1[3],
                b2[0], b2[1], b2[2], b2[3]);

            /* skip all assigned Unicode BMP code points */
            for(c1=c2; c1<=0xffff && flags[c1]!=0; ++c1) {
                if(flags[c1]==2) {
                    incSkipFourGB18030(bytes);
                }
            }
        }
    } else {
        fprintf(stderr, "unknown mode argument \"%s\"\n", argv[1]);
        return 2;
    }

    return 0;
}
