/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  lineargb.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000oct03
*   created by: Markus W. Scherer
*
*   This tool operates on 4-byte GB 18030 codepage sequences. It can
*   - calculate the linear value of such a sequence, with the lowest one,
*     81 30 81 30, getting value 0
*   - calculate the linear difference between two sequences
*   - calculate a sequence that is linearly offset from another
*
*   To compile, just call a C compiler/linker with this source file.
*   On Windows: cl lineargb.c
*/

#include <stdio.h>
#include <stdlib.h>

#define LINEAR_18030(a, b, c, d) ((((a)*10+(b))*126L+(c))*10L+(d))
#define LINEAR_18030_BASE LINEAR_18030(0x81, 0x30, 0x81, 0x30)

static long
getLinear(const char *argv[]) {
    unsigned int a, b, c, d;

    a=(unsigned int)strtoul(argv[0], NULL, 16);
    b=(unsigned int)strtoul(argv[1], NULL, 16);
    c=(unsigned int)strtoul(argv[2], NULL, 16);
    d=(unsigned int)strtoul(argv[3], NULL, 16);

    return LINEAR_18030(a, b, c, d);
}

extern int
main(int argc, const char *argv[]) {
    if(argc==5) {
        printf("Linear value: %ld\n", getLinear(argv+1)-LINEAR_18030_BASE);
        return 0;
    } else if(argc==6) {
        int a, b, c, d;
        long linear=getLinear(argv+1)-LINEAR_18030_BASE+strtoul(argv[5], NULL, 0);
        d=(int)(0x30+linear%10); linear/=10;
        c=(int)(0x81+linear%126); linear/=126;
        b=(int)(0x30+linear%10); linear/=10;
        a=(int)(0x81+linear);
        printf("Offset byte sequence: 0x%02x 0x%02x 0x%02x 0x%02x\n",
               a, b, c, d);
        return 0;
    } else if(argc==9) {
        printf("Linear difference: %ld\n", getLinear(argv+5)-getLinear(argv+1));
        return 0;
    } else {
        printf("Usage: %s a b c d [offset | e f g h] calculates with hexadecimal GB 18030 byte values.\n"
               "Just one sequence: prints linear value.\n"
               "Two sequences: prints the linear difference.\n"
               "One sequence and an offset (decimal or with 0x): prints offset byte sequence\n",
               argv[0]);
        return 1;
    }
}
