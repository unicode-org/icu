/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  ucmmerge.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000nov09
*   created by: Markus W. Scherer
*
*   This tool reads two .ucm files and merges them.
*   Merging the files allows to update the ICU data while keeping ICU-specific
*   changes like "MBCS"->"EBCDIC_STATEFUL" or adding <icu:state>.
*
*   To compile, just call a C compiler/linker with this source file.
*   On Windows: cl ucmmerge.c
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

extern int
main(int argc, const char *argv[]) {
    FILE *old, *update;
    char line[200];
    char *s, *end;
    unsigned long b, i, mappingsTop=0;

    /* open the two input files */
    if(argc<3) {
        fprintf(stderr, "usage: %s old-ucm-filename new-ucm-filename\n", argv[0]);
        return 2;
    }
    old=fopen(argv[1], "r");
    if(old==NULL) {
        fprintf(stderr, "error: unable to open %s\n", argv[1]);
        return 2;
    }
    update=fopen(argv[2], "r");
    if(update==NULL) {
        fprintf(stderr, "error: unable to open %s\n", argv[2]);
        return 2;
    }

    /* copy old until before the "created on" line */
    for(;;) {
        if(fgets(line, sizeof(line), old)==NULL) {
            return 1;
        }
        if(0==strncmp(line, "# File created on ", 18)) {
            break;
        }
        fputs(line, stdout);
    }

    /* skip update until before the "created on" line */
    for(;;) {
        if(fgets(line, sizeof(line), update)==NULL) {
            return 1;
        }
        if(0==strncmp(line, "# File created on ", 18)) {
            break;
        }
    }

    /* copy the "created on" line from update */
    fputs(line, stdout);

    /* copy the rest of the old header including the "CHARMAP" line */
    for(;;) {
        if(fgets(line, sizeof(line), old)==NULL) {
            return 1;
        }
        fputs(line, stdout);
        if(0==strncmp(line, "CHARMAP", 7)) {
            break;
        }
    }

    /* skip the rest of the update header */
    for(;;) {
        if(fgets(line, sizeof(line), update)==NULL) {
            return 1;
        }
        if(0==strncmp(line, "CHARMAP", 7)) {
            break;
        }
    }

    /* copy the rest of the update file */
    for(;;) {
        if(fgets(line, sizeof(line), update)==NULL) {
            break;
        }
        fputs(line, stdout);
    }

    fclose(old);
    fclose(update);
    return 0;
}
