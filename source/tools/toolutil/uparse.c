/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  uparse.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000apr18
*   created by: Markus W. Scherer
*
*   This file provides a parser for files that are delimited by one single
*   character like ';' or TAB. Example: the Unicode Character Properties files
*   like UnicodeData.txt are semicolon-delimited.
*/

#include <stdio.h>
#include "unicode/utypes.h"
#include "cstring.h"
#include "filestrm.h"
#include "uparse.h"

U_CAPI void U_EXPORT2
u_parseDelimitedFile(const char *filename, char delimiter,
                     UParseFieldFn *fields[], int32_t fieldCount,
                     void *context,
                     UErrorCode *pErrorCode) {
    FileStream *file;
    char line[300];
    char *start, *limit;
    int32_t i, length;

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    file=T_FileStream_open(filename, "r");
    if(file==NULL) {
        fprintf(stderr, "*** unable to open input file %s ***\n", filename);
        *pErrorCode=U_FILE_ACCESS_ERROR;
        return;
    }

    while(T_FileStream_readLine(file, line, sizeof(line))!=NULL) {
        length=uprv_strlen(line);

        /* remove trailing newline characters */
        while(length>0 && (line[length-1]=='\r' || line[length-1]=='\n')) {
            line[--length]=0;
        }

        /* skip this line if it is empty or a comment */
        if(line[0]==0 || line[0]=='#') {
            continue;
        }

        /* call the preparation function */
        if(fields[0]!=NULL) {
            fields[0](context, line, line+length, -1, pErrorCode);
            if(U_FAILURE(*pErrorCode)) {
                break;
            }
        }

        /* for each field, call the corresponding field function */
        start=line;
        for(i=0; i<fieldCount; ++i) {
            /* set the limit pointer of this field */
            limit=start;
            while(*limit!=';' && *limit!=0) {
                ++limit;
            }

            /* call the field function */
            if(fields[i+1]!=NULL) {
                fields[i+1](context, start, limit, i, pErrorCode);
                if(U_FAILURE(*pErrorCode)) {
                    limit=line+length;
                    i=fieldCount;
                    break;
                }
            }

            /* set start to the beginning of the next field, if any */
            start=limit;
            if(*start!=0) {
                ++start;
            } else if(i+1<fieldCount) {
                fprintf(stderr, "*** too few fields in line %s ***\n", line);
                *pErrorCode=U_PARSE_ERROR;
                limit=line+length;
                i=fieldCount;
                break;
            }
        }

        /* error in a field function? */
        if(U_FAILURE(*pErrorCode)) {
            break;
        }

        /* call the finalizing function */
        if(fields[fieldCount+1]!=NULL) {
            fields[fieldCount+1](context, line, line+length, fieldCount, pErrorCode);
            if(U_FAILURE(*pErrorCode)) {
                break;
            }
        }
    }

    T_FileStream_close(file);
}
