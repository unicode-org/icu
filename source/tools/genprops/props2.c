/*
*******************************************************************************
*
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  props2.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2002feb24
*   created by: Markus W. Scherer
*
*   Parse more Unicode Character Database files and store
*   additional Unicode character properties in bit set vectors.
*/

#include <stdio.h>
#include "unicode/utypes.h"
#include "unicode/uscript.h"
#include "cstring.h"
#include "cmemory.h"
#include "utrie.h"
#include "uprops.h"
#include "propsvec.h"
#include "uparse.h"
#include "genprops.h"

/* data --------------------------------------------------------------------- */

static UNewTrie *trie;
static uint32_t *pv;
static int32_t pvCount;

/* prototypes --------------------------------------------------------------- */

static void
parseAge(const char *filename, uint32_t *pv, UErrorCode *pErrorCode);

static void
parseScripts(const char *filename, uint32_t *pv, UErrorCode *pErrorCode);

/* -------------------------------------------------------------------------- */

U_CFUNC void
generateAdditionalProperties(char *filename, const char *suffix, UErrorCode *pErrorCode) {
    char *basename;

    basename=filename+uprv_strlen(filename);

    pv=upvec_open(UPROPS_VECTOR_WORDS, 20000);

    /* process various UCD .txt files */
    writeUCDFilename(basename, "DerivedAge", suffix);
    parseAge(filename, pv, pErrorCode);

    writeUCDFilename(basename, "Scripts", suffix);
    parseScripts(filename, pv, pErrorCode);

    trie=utrie_open(NULL, NULL, 50000, 0, FALSE);
    if(trie==NULL) {
        *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
        upvec_close(pv);
        return;
    }

    pvCount=upvec_toTrie(pv, trie, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "genprops error: unable to build trie for additional properties: %s\n", u_errorName(*pErrorCode));
        exit(*pErrorCode);
    }
}

/* DerivedAge.txt ----------------------------------------------------------- */

static void
ageLineFn(void *context,
          char *fields[][2], int32_t fieldCount,
          UErrorCode *pErrorCode) {
    uint32_t *pv=(uint32_t *)context;
    char *s, *end;
    uint32_t value, start, limit, version;

    u_parseCodePointRange(fields[0][0], &start, &limit, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "genprops: syntax error in DerivedAge.txt field 0 at %s\n", fields[0][0]);
        exit(*pErrorCode);
    }
    ++limit;

    /* parse version number */
    s=(char *)u_skipWhitespace(fields[1][0]);
    value=(uint32_t)uprv_strtoul(s, &end, 10);
    if(s==end || value==0 || value>15 || (*end!='.' && *end!=' ' && *end!='\t' && *end!=0)) {
        fprintf(stderr, "genprops: syntax error in DerivedAge.txt field 1 at %s\n", fields[1][0]);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }
    version=value<<4;

    /* parse minor version number */
    if(*end=='.') {
        s=(char *)u_skipWhitespace(end+1);
        value=(uint32_t)uprv_strtoul(s, &end, 10);
        if(s==end || value>15 || (*end!=' ' && *end!='\t' && *end!=0)) {
            fprintf(stderr, "genprops: syntax error in DerivedAge.txt field 1 at %s\n", fields[1][0]);
            *pErrorCode=U_PARSE_ERROR;
            exit(U_PARSE_ERROR);
        }
        version|=value;
    }

    if(!upvec_setValue(pv, start, limit, 0, version<<UPROPS_AGE_SHIFT, UPROPS_AGE_MASK, pErrorCode)) {
        fprintf(stderr, "genprops: unable to set character age: %s\n", u_errorName(*pErrorCode));
        exit(*pErrorCode);
    }
}

static void
parseAge(const char *filename, uint32_t *pv, UErrorCode *pErrorCode) {
    char *fields[2][2];

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    u_parseDelimitedFile(filename, ';', fields, 2, ageLineFn, pv, pErrorCode);
}

/* Scripts.txt -------------------------------------------------------------- */

static void
scriptsLineFn(void *context,
              char *fields[][2], int32_t fieldCount,
              UErrorCode *pErrorCode) {
    uint32_t *pv=(uint32_t *)context;
    char *s, *end;
    uint32_t start, limit;
    UScriptCode script;

    u_parseCodePointRange(fields[0][0], &start, &limit, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "genprops: syntax error in Scripts.txt field 0 at %s\n", fields[0][0]);
        exit(*pErrorCode);
    }
    ++limit;

    /* parse script name */
    s=(char *)u_skipWhitespace(fields[1][0]);

    /* trim trailing whitespace */
    end=fields[1][1];
    while(s<end && (*(end-1)==' ' || *(end-1)=='\t')) {
        --end;
    }
    *end=0;
    if( 1!=uscript_getCode(s, &script, 1, pErrorCode) ||
        U_FAILURE(*pErrorCode) ||
        script<=USCRIPT_INVALID_CODE
    ) {
        fprintf(stderr, "genprops: syntax error in Scripts.txt field 1 at %s\n", fields[1][0]);
        if(U_SUCCESS(*pErrorCode)) {
            *pErrorCode=U_PARSE_ERROR;
        }
        exit(*pErrorCode);
    }

    if(!upvec_setValue(pv, start, limit, 0, (uint32_t)script, UPROPS_SCRIPT_MASK, pErrorCode)) {
        fprintf(stderr, "genprops: unable to set script code: %s\n", u_errorName(*pErrorCode));
        exit(*pErrorCode);
    }
}

static void
parseScripts(const char *filename, uint32_t *pv, UErrorCode *pErrorCode) {
    char *fields[2][2];

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    /*
     * UTR 24 says:
     * Section 2:
     *   "Common - For characters that may be used
     *             within multiple scripts,
     *             or any unassigned code points."
     *
     * Section 4:
     *   "The value COMMON is the default value,
     *    given to all code points that are not
     *    explicitly mentioned in the data file."
     */
    if(!upvec_setValue(pv, 0, 0x110000, 0, (uint32_t)USCRIPT_COMMON, UPROPS_SCRIPT_MASK, pErrorCode)) {
        fprintf(stderr, "genprops: unable to set script code: %s\n", u_errorName(*pErrorCode));
        exit(*pErrorCode);
    }

    u_parseDelimitedFile(filename, ';', fields, 2, scriptsLineFn, pv, pErrorCode);
}

/* data serialization ------------------------------------------------------- */

U_CFUNC int32_t
writeAdditionalData(uint8_t *p, int32_t capacity, int32_t indexes[UPROPS_INDEX_COUNT]) {
    int32_t length;
    UErrorCode errorCode;

    errorCode=U_ZERO_ERROR;
    length=utrie_serialize(trie, p, capacity, getFoldedPropsValue, TRUE, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: unable to serialize trie for additional properties: %s\n", u_errorName(errorCode));
        exit(errorCode);
    }
    if(p!=NULL) {
        p+=length;
        capacity-=length;
        if(beVerbose) {
            printf("size in bytes of additional props trie:%5u\n", length);
        }

        /* set indexes */
        indexes[UPROPS_ADDITIONAL_VECTORS_INDEX]=
            indexes[UPROPS_ADDITIONAL_TRIE_INDEX]+length/4;
        indexes[UPROPS_ADDITIONAL_VECTORS_COLUMNS_INDEX]=UPROPS_VECTOR_WORDS;
        indexes[UPROPS_RESERVED_INDEX]=
            indexes[UPROPS_ADDITIONAL_VECTORS_INDEX]+pvCount;
    }

    if(p!=NULL && (pvCount*4)<=capacity) {
        uprv_memcpy(p, pv, pvCount*4);
        if(beVerbose) {
            printf("number of additional props vectors:    %5u\n", pvCount/UPROPS_VECTOR_WORDS);
            printf("number of 32-bit words per vector:     %5u\n", UPROPS_VECTOR_WORDS);
        }
    }
    length+=pvCount*4;

    if(p!=NULL) {
        utrie_close(trie);
        upvec_close(pv);
    }
    return length;
}
