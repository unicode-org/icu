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
                     char *fields[][2], int32_t fieldCount,
                     UParseLineFn *lineFn, void *context,
                     UErrorCode *pErrorCode) {
    FileStream *file;
    char line[300];
    char *start, *limit;
    int32_t i, length;

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    if(fields==NULL || lineFn==NULL || fieldCount<=0) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    if(filename==NULL || *filename==0 || *filename=='-' && filename[1]==0) {
        filename=NULL;
        file=T_FileStream_stdin();
    } else {
        file=T_FileStream_open(filename, "r");
    }
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

        /* for each field, call the corresponding field function */
        start=line;
        for(i=0; i<fieldCount; ++i) {
            /* set the limit pointer of this field */
            limit=start;
            while(*limit!=delimiter && *limit!=0) {
                ++limit;
            }

            /* set the field start and limit in the fields array */
            fields[i][0]=start;
            fields[i][1]=limit;

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

        /* call the field function */
        lineFn(context, fields, fieldCount, pErrorCode);
        if(U_FAILURE(*pErrorCode)) {
            break;
        }
    }

    if(filename!=NULL) {
        T_FileStream_close(file);
    }
}
