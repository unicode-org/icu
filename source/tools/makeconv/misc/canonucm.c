/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  canonucm.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000nov08
*   created by: Markus W. Scherer
*
*   This tool reads a .ucm file and canonicalizes it: In the CHARMAP section,
*   - sort by Unicode code points
*   - print all code points in uppercase hexadecimal
*   - print all Unicode code points with 4, 5, or 6 digits as needed
*   - remove the comments
*   - remove unnecessary spaces
*
*   To compile, just call a C compiler/linker with this source file.
*   On Windows: cl canonucm.c
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct Mapping {
    unsigned long u, b, f;
} Mapping;

static Mapping
mappings[200000];

/* lexically compare Mappings for sorting */
static int
compareMappings(const void *left, const void *right) {
    const Mapping *l=(const Mapping *)left, *r=(const Mapping *)right;
    long result;

    /* shift right 16 with sign-extend to take care of int possibly being 16 bits wide */
    result=(long)(l->u-r->u);
    if(result!=0) {
        return (int)(result>>16)|1;
    }
    result=(long)(l->b-r->b);
    if(result!=0) {
        return (int)(result>>16)|1;
    }
    return (int)(l->f-r->f);
}

extern int
main(int argc, const char *argv[]) {
    char line[200];
    char *s, *end;
    unsigned long b, i, mappingsTop=0;

    /* parse the input file from stdin */
    /* read and copy header */
    do {
        if(gets(line)==NULL) {
            fprintf(stderr, "error: no mapping section");
            return 1;
        }
        puts(line);
    } while(0!=strcmp(line, "CHARMAP"));

    /* copy empty and comment lines before the first mapping */
    for(;;) {
        if(gets(line)==NULL) {
            fprintf(stderr, "error: no mappings");
            return 1;
        }
        if(line[0]!=0 && line[0]!='#') {
            break;
        }
        puts(line);
    }

    /* process the charmap section, start with the line read above */
    for(;;) {
        /* ignore empty and comment lines */
        if(line[0]!=0 && line[0]!='#') {
            if(0!=strcmp(line, "END CHARMAP")) {
                if(mappingsTop==sizeof(mappings)/sizeof(mappings[0])) {
                    fprintf(stderr, "too many mappings\n");
                    return 1;
                }
                /* parse mapping */
                if(line[0]!='<' || line[1]!='U') {
                    fprintf(stderr, "parse error (does not start with \"<U\") in mapping line \"%s\"\n", line);
                    return 1;
                }
                /* parse Unicode code point */
                mappings[mappingsTop].u=strtoul(line+2, &end, 16);
                if(end==line+2 || mappings[mappingsTop].u>0x10ffff || *end!='>') {
                    fprintf(stderr, "parse error (Unicode code point) in mapping line \"%s\"\n", line);
                    return 1;
                }
                /* skip white space */
                s=end+1;
                while(*s==' ' || *s=='\t') {
                    ++s;
                }
                /* parse codepage bytes */
                b=0;
                for(;;) {
                    if(*s!='\\') {
                        break;
                    }
                    if(s[1]!='x') {
                        fprintf(stderr, "parse error (no 'x' in \"\\xXX\") in mapping line \"%s\"\n", line);
                        return 1;
                    }
                    s+=2;
                    b=(b<<8)|strtoul(s, &end, 16);
                    if(end!=s+2) {
                        fprintf(stderr, "parse error (codepage byte) in mapping line \"%s\"\n", line);
                        return 1;
                    }
                    s+=2;
                }
                mappings[mappingsTop].b=b;
                /* skip everything until the fallback indicator */
                while(*s!='|') {
                    if(*s==0) {
                        fprintf(stderr, "parse error (missing '|' fallback indicator) in mapping line \"%s\"\n", line);
                        return 1;
                    }
                    ++s;
                }
                /* parse fallback indicator */
                i=s[1]-'0';
                if(i>3) {
                    fprintf(stderr, "parse error (fallback indicator not 0..3) in mapping line \"%s\"\n", line);
                    return 1;
                }
                mappings[mappingsTop++].f=i;
            } else {
                /* sort and write all mappings */
                if(mappingsTop>0) {
                    qsort(mappings, mappingsTop, sizeof(Mapping), compareMappings);
                    for(i=0; i<mappingsTop; ++i) {
                        b=mappings[i].b;
                        if(b<=0xff) {
                            printf("<U%04lX> \\x%02lX |%lu\n", mappings[i].u, b, mappings[i].f);
                        } else if(b<=0xffff) {
                            printf("<U%04lX> \\x%02lX\\x%02lX |%lu\n", mappings[i].u, b>>8, b&0xff, mappings[i].f);
                        } else if(b<=0xffffff) {
                            printf("<U%04lX> \\x%02lX\\x%02lX\\x%02lX |%lu\n", mappings[i].u, b>>16, (b>>8)&0xff, b&0xff, mappings[i].f);
                        } else {
                            printf("<U%04lX> \\x%02lX\\x%02lX\\x%02lX\\x%02lX |%lu\n", mappings[i].u, b>>24, (b>>16)&0xff, (b>>8)&0xff, b&0xff, mappings[i].f);
                        }
                    }
                }
                /* output "END CHARMAP" */
                puts(line);
                return 0;
            }
        }
        /* read the next line */
        if(gets(line)==NULL) {
            fprintf(stderr, "incomplete charmap section\n");
            return 1;
        }
    }
}
