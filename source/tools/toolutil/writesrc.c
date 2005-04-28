/*
*******************************************************************************
*
*   Copyright (C) 2005, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  writesrc.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2005apr23
*   created by: Markus W. Scherer
*
*   Helper functions for writing source code for data.
*/

#ifndef __WRITESRC_H__
#define __WRITESRC_H__

#include <stdio.h>
#include <time.h>
#include "unicode/utypes.h"
#include "unicode/putil.h"
#include "utrie.h"
#include "cstring.h"
#include "writesrc.h"

U_CAPI FILE * U_EXPORT2
usrc_create(const char *path, const char *filename) {
    char buffer[1024];
    const char *p;
    char *q;
    FILE *f;
    char c;

    if(path==NULL) {
        p=filename;
    } else {
        /* concatenate path and filename, with U_FILE_SEP_CHAR in between if necessary */
        uprv_strcpy(buffer, path);
        q=buffer+uprv_strlen(buffer);
        if(q>buffer && (c=*(q-1))!=U_FILE_SEP_CHAR && c!=U_FILE_ALT_SEP_CHAR) {
            *q++=U_FILE_SEP_CHAR;
        }
        uprv_strcpy(q, filename);
        p=buffer;
    }

    f=fopen(p, "w");
    if(f!=NULL) {
        char year[8];
        const struct tm *lt;
        time_t t;

        time(&t);
        lt=localtime(&t);
        strftime(year, sizeof(year), "%Y", lt);
        strftime(buffer, sizeof(buffer), "%Y-%m-%d", lt);
        fprintf(
            f,
            "/*\n"
            " * Copyright (C) 1999-%s, International Business Machines\n"
            " * Corporation and others.  All Rights Reserved.\n"
            " *\n"
            " * file name: %s\n"
            " *\n"
            " * machine-generated on: %s\n"
            " */\n\n",
            year,
            filename,
            buffer);
    } else {
        fprintf(
            stderr,
            "usrc_create(%s, %s): unable to create file\n",
            path!=NULL ? path : NULL, filename);
    }
    return f;
}

U_CAPI void U_EXPORT2
usrc_writeArray(FILE *f, const void *p, int32_t width, int32_t length) {
    const uint8_t *p8;
    const uint16_t *p16;
    const uint32_t *p32;
    uint32_t value;
    int32_t i, col;

    p8=NULL;
    p16=NULL;
    p32=NULL;
    switch(width) {
    case 8:
        p8=(const uint8_t *)p;
        break;
    case 16:
        p16=(const uint16_t *)p;
        break;
    case 32:
        p32=(const uint32_t *)p;
        break;
    default:
        fprintf(stderr, "usrc_writeArray(width=%ld) unrecognized width\n", (long)width);
        return;
    }
    fputs("{\n", f);
    for(i=col=0; i<length; ++i, ++col) {
        if(i>0) {
            if(col<16) {
                fputc(',', f);
            } else {
                fputs(",\n", f);
                col=0;
            }
        }
        switch(width) {
        case 8:
            value=p8[i];
            break;
        case 16:
            value=p16[i];
            break;
        case 32:
            value=p32[i];
            break;
        default:
            value=0; /* unreachable */
            break;
        }
        if(value<=9) {
            fprintf(f, "%lu", (unsigned long)value);
        } else {
            fprintf(f, "0x%lx", (unsigned long)value);
        }
    }
    fputs("\n};\n\n", f);
}

U_CAPI void U_EXPORT2
usrc_writeUTrie(FILE *f, const uint8_t *p, int32_t length,
                const char *declaration,
                const char *arrayStorage, const char *arrayPrefix,
                const char *getFoldingOffsetName) {
    UTrie trie={ NULL };
    UErrorCode errorCode;

    errorCode=U_ZERO_ERROR;
    utrie_unserialize(&trie, p, length, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(
            stderr,
            "usrc_writeUTrie() failed to utrie_unserialize(data[%ld]) - %s\n",
            (long)length,
            u_errorName(errorCode));
        return;
    }

    if(trie.data32==NULL) {
        /* 16-bit trie */
        int32_t arrayLength=trie.indexLength+trie.dataLength;
        fprintf(f,
            "%s uint16_t %s_index[%ld]=",
            arrayStorage, arrayPrefix,
            (long)arrayLength);
        usrc_writeArray(f, trie.index, 16, arrayLength);
        fprintf(
            f,
            "%s={\n"
            "    %s_index,\n"
            "    NULL,\n",
            declaration,
            arrayPrefix);
    } else {
        /* 32-bit trie */
        fprintf(f,
            "%s uint16_t %s_index[%ld]=",
            arrayStorage, arrayPrefix,
            (long)trie.indexLength);
        usrc_writeArray(f, trie.index, 16, trie.indexLength);
        fprintf(f,
            "%s uint32_t %s_index[%ld]=",
            arrayStorage, arrayPrefix,
            (long)trie.dataLength);
        usrc_writeArray(f, trie.data32, 32, trie.dataLength);
        fprintf(
            f,
            "%s={\n"
            "    %s_index,\n"
            "    %s_data32,\n",
            declaration,
            arrayPrefix, arrayPrefix);
    }

    if(getFoldingOffsetName!=NULL) {
        fprintf(f, "    %s,\n", getFoldingOffsetName);
    } else {
        fputs("    utrie_defaultGetFoldingOffset,\n", f);
    }
    fprintf(
        f,
        "    %ld,\n"
        "    %ld,\n"
        "    %lu,\n"
        "    %s\n"
        "};\n\n",
        (long)trie.indexLength, (long)trie.dataLength,
        (unsigned long)trie.initialValue,
        trie.isLatin1Linear ? "TRUE" : "FALSE");
}

#endif
