/*
*******************************************************************************
*
*   Copyright (C) 2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  ucm.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2003jun20
*   created by: Markus W. Scherer
*
*   This file reads a .ucm file, stores its mappings and sorts them.
*   It implements handling of Unicode conversion mappings from .ucm files
*   for makeconv, canonucm, rptp2ucm, etc.
*
*   Unicode code point sequences with a length of more than 1,
*   as well as byte sequences with more than 4 bytes or more than one complete
*   character sequence are handled to support m:n mappings.
*/

#include "unicode/utypes.h"
#include "unicode/ustring.h"
#include "cstring.h"
#include "cmemory.h"
#include "uarrsort.h"
#include "ucnvmbcs.h"
#include "ucnv_ext.h"
#include "uparse.h"
#include "ucm.h"
#include <stdio.h>

/* -------------------------------------------------------------------------- */

/*
### TODO
allow file without fallback indicators for backward compatibility
only for makeconv
must not sort such mappings
disallow when using extension tables because that requires sorting

rptp2ucm has its own mapping parser and sets all-|1 and |3 mappings; normalization function generates |0 and |2

*/

static void
printMapping(UCMapping *m, UChar32 *codePoints, uint8_t *bytes, FILE *f) {
    int32_t j;

    for(j=0; j<m->uLen; ++j) {
        fprintf(f, "<U%04lX>", codePoints[j]);
    }

    fputc(' ', f);

    for(j=0; j<m->bLen; ++j) {
        fprintf(f, "\\x%02X", bytes[j]);
    }

    if(m->f>=0) {
        fprintf(f, " |%lu\n", m->f);
    } else {
        fputs("\n", f);
    }
}

U_CAPI void U_EXPORT2
ucm_printMapping(UCMTable *table, UCMapping *m, FILE *f) {
    printMapping(m, UCM_GET_CODE_POINTS(table, m), UCM_GET_BYTES(table, m), f);
}

U_CAPI void U_EXPORT2
ucm_printTable(UCMTable *table, FILE *f, UBool byUnicode) {
    UCMapping *m;
    int32_t i, length;

    m=table->mappings;
    length=table->mappingsLength;
    if(byUnicode) {
        for(i=0; i<length; ++m, ++i) {
            ucm_printMapping(table, m, f);
        }
    } else {
        const int32_t *map=table->reverseMap;
        for(i=0; i<length; ++i) {
            ucm_printMapping(table, m+map[i], f);
        }
    }
}

/* mapping comparisons ------------------------------------------------------ */

static int32_t
compareUnicode(UCMTable *lTable, const UCMapping *l,
               UCMTable *rTable, const UCMapping *r) {
    const UChar32 *lu, *ru;
    int32_t result, i, length;

    if(l->uLen==1 && r->uLen==1) {
        /* compare two single code points */
        return l->u-r->u;
    }

    /* get pointers to the code point sequences */
    lu=UCM_GET_CODE_POINTS(lTable, l);
    ru=UCM_GET_CODE_POINTS(rTable, r);

    /* get the minimum length */
    if(l->uLen<=r->uLen) {
        length=l->uLen;
    } else {
        length=r->uLen;
    }

    /* compare the code points */
    for(i=0; i<length; ++i) {
        result=lu[i]-ru[i];
        if(result!=0) {
            return result;
        }
    }

    /* compare the lengths */
    return l->uLen-r->uLen;
}

static int32_t
compareBytes(UCMTable *lTable, const UCMapping *l,
             UCMTable *rTable, const UCMapping *r,
             UBool lexical) {
    const uint8_t *lb, *rb;
    int32_t result, i, length;

    /*
     * A lexical comparison is used for sorting in the builder, to allow
     * an efficient search for a byte sequence that could be a prefix
     * of a previously entered byte sequence.
     *
     * Comparing by lengths first is for compatibility with old .ucm tools
     * like canonucm and rptp2ucm.
     */
    if(lexical) {
        /* get the minimum length and continue */
        if(l->bLen<=r->bLen) {
            length=l->bLen;
        } else {
            length=r->bLen;
        }
    } else {
        /* compare lengths first */
        result=l->bLen-r->bLen;
        if(result!=0) {
            return result;
        } else {
            length=l->bLen;
        }
    }

    /* get pointers to the byte sequences */
    lb=UCM_GET_BYTES(lTable, l);
    rb=UCM_GET_BYTES(rTable, r);

    /* compare the bytes */
    for(i=0; i<length; ++i) {
        result=lb[i]-rb[i];
        if(result!=0) {
            return result;
        }
    }

    /* compare the lengths */
    return l->bLen-r->bLen;
}

/* compare UCMappings for sorting */
static int32_t
compareMappings(UCMTable *table, const void *left, const void *right, UBool uFirst) {
    const UCMapping *l=(const UCMapping *)left, *r=(const UCMapping *)right;
    int32_t result;

    /* choose which side to compare first */
    if(uFirst) {
        /* Unicode then bytes */
        result=compareUnicode(table, l, table, r);
        if(result==0) {
            result=compareBytes(table, l, table, r, FALSE); /* not lexically, like canonucm */
        }
    } else {
        /* bytes then Unicode */
        result=compareBytes(table, l, table, r, TRUE); /* lexically, for builder */
        if(result==0) {
            result=compareUnicode(table, l, table, r);
        }
    }

    if(result!=0) {
        return result;
    }

    /* compare the flags */
    return l->f-r->f;
}

/* sorting by Unicode first sorts mappings directly */
static int32_t
compareMappingsUnicodeFirst(const void *context, const void *left, const void *right) {
    return compareMappings((UCMTable *)context, left, right, TRUE);
}

/* sorting by bytes first sorts the reverseMap; use indirection to mappings */
static int32_t
compareMappingsBytesFirst(const void *context, const void *left, const void *right) {
    UCMTable *table=(UCMTable *)context;
    int32_t l=*(const int32_t *)left, r=*(const int32_t *)right;
    return compareMappings(table, table->mappings+l, table->mappings+r, FALSE);
}

U_CAPI void U_EXPORT2
ucm_sortTable(UCMTable *t) {
    UErrorCode errorCode;
    int32_t i;

    errorCode=U_ZERO_ERROR;

    /* 1. sort by Unicode first */
    uprv_sortArray(t->mappings, t->mappingsLength, sizeof(UCMapping),
                   compareMappingsUnicodeFirst, t,
                   FALSE, &errorCode);

    /* build the reverseMap */
    if(t->reverseMap==NULL) {
        /*
         * allocate mappingsCapacity instead of mappingsLength so that
         * if mappings are added, the reverseMap need not be
         * reallocated each time
         * (see moveMappings() and ucm_addMapping())
         */
        t->reverseMap=(int32_t *)uprv_malloc(t->mappingsCapacity*sizeof(int32_t));
        if(t->reverseMap==NULL) {
            fprintf(stderr, "ucm error: unable to allocate reverseMap\n");
            exit(U_MEMORY_ALLOCATION_ERROR);
        }
    }
    for(i=0; i<t->mappingsLength; ++i) {
        t->reverseMap[i]=i;
    }

    /* 2. sort reverseMap by mappings bytes first */
    uprv_sortArray(t->reverseMap, t->mappingsLength, sizeof(int32_t),
                   compareMappingsBytesFirst, t,
                   FALSE, &errorCode);

    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "ucm error: sortTable()/uprv_sortArray() fails - %s\n",
                u_errorName(errorCode));
        exit(errorCode);
    }
}

/*

### TODO normalization function for a table (in or for rptp2ucm)
sort table
if there are mappings with the same code points and bytes but |1 and |3, merge them into one |0 (or make |2 where necessary)
if mappings were merged, sort again
-> for rptp2ucm

*/

/* lookups ------------------------------------------------------------------ */

/*
### TODO lookups?

binary search for first mapping with some code point or byte sequence
check if a code point is the first of any mapping (RT or FB)
check if a byte sequence is a prefix of any mapping (RT or RFB)
check if there is a mapping with the same source units; return whether the target is same or different

*/

enum {
    MOVE_TO_EXT=0x10,
    REMOVE_MAPPING=0x20,
    MOVE_ANY=0x30
};

/*
 * move mappings with MOVE_ANY ored into their flags from the base table
 * to the extension table
 */
static void
moveMappings(UCMTable *base, UCMTable *ext) {
    UCMapping *mb, *mbLimit;
    int8_t flag;
    UBool didMove;

    mb=base->mappings;
    mbLimit=mb+base->mappingsLength;
    didMove=FALSE;

    while(mb<mbLimit) {
        flag=mb->f;
        if(flag&MOVE_ANY) {
            /* restore the original flag value */
            mb->f=flag&~MOVE_ANY;
            didMove=TRUE;

            if(ext!=NULL && (flag&MOVE_TO_EXT)) {
                /* add the mapping to the extension table */
                ucm_addMapping(ext, mb, UCM_GET_CODE_POINTS(base, mb), UCM_GET_BYTES(base, mb));
            }

            /* move the last base mapping down and overwrite the current one */
            if(mb<(mbLimit-1)) {
                uprv_memcpy(mb, mbLimit-1, sizeof(UCMapping));
            }
            --mbLimit;
            --base->mappingsLength;
        } else {
            ++mb;
        }
    }

    if(didMove) {
        ucm_sortTable(base);
        ucm_printTable(base, stdout, TRUE); puts(""); /* ### TODO */
        if(ext!=NULL) {
            ucm_sortTable(ext);
            ucm_printTable(ext, stdout, TRUE); puts(""); /* ### TODO */
        }
    }
}

enum {
    NEEDS_MOVE=1,
    HAS_ERRORS=2
};

static uint8_t
checkBaseExtUnicode(UCMTable *base, UCMTable *ext, UBool moveToExt) {
    UCMapping *mb, *me, *mbLimit, *meLimit;
    int32_t cmp;
    uint8_t result;

    mb=base->mappings;
    mbLimit=mb+base->mappingsLength;

    me=ext->mappings;
    meLimit=me+ext->mappingsLength;

    result=0;

    for(;;) {
        /* skip irrelevant mappings on both sides */
        for(;;) {
            if(mb==mbLimit) {
                return result;
            }

            if(0<=mb->f && mb->f<=2) {
                break;
            }

            ++mb;
        }

        for(;;) {
            if(me==meLimit) {
                return result;
            }

            if(0<=me->f && me->f<=2) {
                break;
            }

            ++me;
        }

        /* compare the base and extension mappings */
        cmp=compareUnicode(base, mb, ext, me);
        if(cmp<0) {
            /* does mb map from an input sequence that is a prefix of me's? */
            if( mb->uLen<me->uLen &&
                0==uprv_memcmp(UCM_GET_CODE_POINTS(base, mb), UCM_GET_CODE_POINTS(ext, me), 4*mb->uLen)
            ) {
                if(moveToExt) {
                    /* mark this mapping to be moved to the extension table */
                    mb->f|=MOVE_TO_EXT;
                } else {
                    fprintf(stderr,
                            "ucm error: the base table contains a mapping whose input sequence\n"
                            "           is a prefix of the input sequence of an extension mapping\n");
                    ucm_printMapping(base, mb, stderr);
                    ucm_printMapping(ext, me, stderr);
                }
                result|=NEEDS_MOVE;
            }

            ++mb;
        } else if(cmp==0) {
            /*
             * same output: remove the extension mapping,
             * otherwise treat as an error
             */
            if( mb->f==me->f && mb->bLen==me->bLen &&
                0==uprv_memcmp(UCM_GET_BYTES(base, mb), UCM_GET_BYTES(ext, me), mb->bLen)
            ) {
                me->f|=REMOVE_MAPPING;
                result|=NEEDS_MOVE;
            } else {
                fprintf(stderr,
                        "ucm error: the base table contains a mapping whose input sequence\n"
                        "           is the same as the input sequence of an extension mapping\n"
                        "           but it maps differently\n");
                ucm_printMapping(base, mb, stderr);
                ucm_printMapping(ext, me, stderr);
                result|=HAS_ERRORS;
            }

            ++mb;
        } else /* cmp>0 */ {
            ++me;
        }
    }
}

static uint8_t
checkBaseExtBytes(UCMStates *baseStates, UCMTable *base, UCMTable *ext, UBool moveToExt) {
    UCMapping *mb, *me;
    int32_t *baseMap, *extMap;
    int32_t b, e, bLimit, eLimit, cmp;
    uint8_t result;
    UBool isSISO;

    baseMap=base->reverseMap;
    extMap=ext->reverseMap;

    b=e=0;
    bLimit=base->mappingsLength;
    eLimit=ext->mappingsLength;

    result=0;

    isSISO=(UBool)(baseStates->outputType==MBCS_OUTPUT_2_SISO);

    for(;;) {
        /* skip irrelevant mappings on both sides */
        for(;;) {
            if(b==bLimit) {
                return result;
            }
            mb=base->mappings+baseMap[b];

            if(mb->f==0 || mb->f==3) {
                break;
            }

            ++b;
        }

        for(;;) {
            if(e==eLimit) {
                return result;
            }
            me=ext->mappings+extMap[e];

            if(me->f==0 || me->f==3) {
                break;
            }

            ++e;
        }

        /* compare the base and extension mappings */
        cmp=compareBytes(base, mb, ext, me, TRUE);
        if(cmp<0) {
            /*
             * does mb map from an input sequence that is a prefix of me's?
             * for SI/SO tables, a single byte is never a prefix because it
             * occurs in a separate single-byte state
             */
            if( mb->bLen<me->bLen &&
                (!isSISO || mb->bLen>1) &&
                0==uprv_memcmp(UCM_GET_BYTES(base, mb), UCM_GET_BYTES(ext, me), mb->bLen)
            ) {
                if(moveToExt) {
                    /* mark this mapping to be moved to the extension table */
                    mb->f|=MOVE_TO_EXT;
                    result|=NEEDS_MOVE;
                } else {
                    fprintf(stderr,
                            "ucm error: the base table contains a mapping whose input sequence\n"
                            "           is a prefix of the input sequence of an extension mapping\n");
                    ucm_printMapping(base, mb, stderr);
                    ucm_printMapping(ext, me, stderr);
                    result|=HAS_ERRORS;
                }
            }

            ++b;
        } else if(cmp==0) {
            /*
             * same output: remove the extension mapping,
             * otherwise treat as an error
             */
            if( mb->f==me->f && mb->uLen==me->uLen &&
                0==uprv_memcmp(UCM_GET_CODE_POINTS(base, mb), UCM_GET_CODE_POINTS(ext, me), 4*mb->uLen)
            ) {
                me->f|=REMOVE_MAPPING;
                result|=NEEDS_MOVE;
            } else {
                fprintf(stderr,
                        "ucm error: the base table contains a mapping whose input sequence\n"
                        "           is the same as the input sequence of an extension mapping\n"
                        "           but it maps differently\n");
                ucm_printMapping(base, mb, stderr);
                ucm_printMapping(ext, me, stderr);
                result|=HAS_ERRORS;
            }

            ++b;
        } else /* cmp>0 */ {
            ++e;
        }
    }
}

U_CAPI UBool U_EXPORT2
ucm_checkValidity(UCMTable *table, UCMStates *baseStates) {
    UCMapping *m, *mLimit;
    int32_t count;
    UBool isOK;

    m=table->mappings;
    mLimit=m+table->mappingsLength;
    isOK=TRUE;

    while(m<mLimit) {
        count=ucm_countChars(baseStates, UCM_GET_BYTES(table, m), m->bLen);
        if(count<1) {
            ucm_printMapping(table, m, stderr);
            isOK=FALSE;
        }
        ++m;
    }

    return isOK;
}

U_CAPI UBool U_EXPORT2
ucm_checkBaseExt(UCMStates *baseStates, UCMTable *base, UCMTable *ext, UBool moveToExt) {
    uint8_t result;

    /* if we have an extension table, we must always use precision flags */
    if(base->flagsType!=UCM_FLAGS_EXPLICIT || ext->flagsType!=UCM_FLAGS_EXPLICIT) {
        fprintf(stderr, "ucm error: the base or extension table contains mappings without precision flags\n");
        return FALSE;
    }

    /* checking requires both tables to be sorted */
    ucm_sortTable(base);
    ucm_sortTable(ext);

    /* check */
    result=
        checkBaseExtUnicode(base, ext, moveToExt)|
        checkBaseExtBytes(baseStates, base, ext, moveToExt);

    if(result&HAS_ERRORS) {
        return FALSE;
    }

    if(result&NEEDS_MOVE) {
        moveMappings(ext, NULL);
        moveMappings(base, ext);
    }

    return TRUE;
}

/* ucm parser --------------------------------------------------------------- */

U_CAPI int8_t U_EXPORT2
ucm_parseBytes(uint8_t bytes[UCNV_EXT_MAX_BYTES], const char *line, const char **ps) {
    const char *s=*ps;
    char *end;
    int8_t bLen;

    bLen=0;
    for(;;) {
        /* skip an optional plus sign */
        if(bLen>0 && *s=='+') {
            ++s;
        }
        if(*s!='\\') {
            break;
        }

        if(bLen==UCNV_EXT_MAX_BYTES) {
            fprintf(stderr, "ucm error: too many bytes on \"%s\"\n", line);
            return -1;
        }
        if( s[1]!='x' ||
            (bytes[bLen]=(uint8_t)uprv_strtoul(s+2, &end, 16), end)!=s+4
        ) {
            fprintf(stderr, "ucm error: byte must be formatted as \\xXX (2 hex digits) - \"%s\"\n", line);
            return -1;
        }
        ++bLen;
        s=end;
    }

    *ps=s;
    return bLen;
}

/* parse a mapping line; must not be empty */
U_CAPI UBool U_EXPORT2
ucm_parseMappingLine(UCMapping *m,
                     UChar32 codePoints[UCNV_EXT_MAX_UCHARS],
                     uint8_t bytes[UCNV_EXT_MAX_BYTES],
                     const char *line) {
    const char *s;
    char *end;
    int32_t u16Length;
    int8_t uLen, bLen, f;

    s=line;
    uLen=bLen=0;

    /* parse code points */
    for(;;) {
        /* skip an optional plus sign */
        if(uLen>0 && *s=='+') {
            ++s;
        }
        if(*s!='<') {
            break;
        }

        if(uLen==UCNV_EXT_MAX_UCHARS) {
            fprintf(stderr, "ucm error: too many code points on \"%s\"\n", line);
            return FALSE;
        }
        if( s[1]!='U' ||
            (codePoints[uLen]=(UChar32)uprv_strtoul(s+2, &end, 16), end)==s+2 ||
            *end!='>'
        ) {
            fprintf(stderr, "ucm error: Unicode code point must be formatted as <UXXXX> (1..6 hex digits) - \"%s\"\n", line);
            return FALSE;
        }
        if((uint32_t)codePoints[uLen]>0x10ffff || U_IS_SURROGATE(codePoints[uLen])) {
            fprintf(stderr, "ucm error: Unicode code point must be 0..d7ff or e000..10ffff - \"%s\"\n", line);
            return FALSE;
        }
        ++uLen;
        s=end+1;
    }

    if(uLen==0) {
        fprintf(stderr, "ucm error: no Unicode code points on \"%s\"\n", line);
        return FALSE;
    } else if(uLen==1) {
        m->u=codePoints[0];
    } else {
        UErrorCode errorCode=U_ZERO_ERROR;
        u_strFromUTF32(NULL, 0, &u16Length, codePoints, uLen, &errorCode);
        if( (U_FAILURE(errorCode) && errorCode!=U_BUFFER_OVERFLOW_ERROR) ||
            u16Length>UCNV_EXT_MAX_UCHARS
        ) {
            fprintf(stderr, "ucm error: too many UChars on \"%s\"\n", line);
            return FALSE;
        }
    }

    s=u_skipWhitespace(s);

    /* parse bytes */
    bLen=ucm_parseBytes(bytes, line, &s);

    if(bLen<0) {
        return FALSE;
    } else if(bLen==0) {
        fprintf(stderr, "ucm error: no bytes on \"%s\"\n", line);
        return FALSE;
    } else if(bLen<=4) {
        uprv_memcpy(m->b.bytes, bytes, bLen);
    }

    /* skip everything until the fallback indicator, even the start of a comment */
    for(;;) {
        if(*s==0) {
            f=-1; /* no fallback indicator */
            break;
        } else if(*s=='|') {
            f=(int8_t)(s[1]-'0');
            if((uint8_t)f>3) {
                fprintf(stderr, "ucm error: fallback indicator must be |0..|3 - \"%s\"\n", line);
                return FALSE;
            }
            break;
        }
        ++s;
    }

    m->uLen=uLen;
    m->bLen=bLen;
    m->f=f;
    return TRUE;
}

/* general APIs ------------------------------------------------------------- */

U_CAPI UCMTable * U_EXPORT2
ucm_openTable() {
    UCMTable *table=(UCMTable *)uprv_malloc(sizeof(UCMTable));
    if(table==NULL) {
        fprintf(stderr, "ucm error: unable to allocate a UCMTable\n");
        exit(U_MEMORY_ALLOCATION_ERROR);
    }

    memset(table, 0, sizeof(UCMTable));
    return table;
}

U_CAPI void U_EXPORT2
ucm_closeTable(UCMTable *table) {
    if(table!=NULL) {
        uprv_free(table->mappings);
        uprv_free(table->codePoints);
        uprv_free(table->bytes);
        uprv_free(table->reverseMap);
        uprv_free(table);
    }
}

U_CAPI void U_EXPORT2
ucm_addMapping(UCMTable *table,
               UCMapping *m,
               UChar32 codePoints[UCNV_EXT_MAX_UCHARS],
               uint8_t bytes[UCNV_EXT_MAX_BYTES]) {
    UCMapping *tm;
    UChar32 c;
    int32_t index;

    if(table->mappingsLength>=table->mappingsCapacity) {
        /* make the mappings array larger */
        if(table->mappingsCapacity==0) {
            table->mappingsCapacity=1000;
        } else {
            table->mappingsCapacity*=10;
        }
        table->mappings=(UCMapping *)uprv_realloc(table->mappings,
                                             table->mappingsCapacity*sizeof(UCMapping));
        if(table->mappings==NULL) {
            fprintf(stderr, "ucm error: unable to allocate %d UCMappings\n",
                            table->mappingsCapacity);
            exit(U_MEMORY_ALLOCATION_ERROR);
        }

        if(table->reverseMap!=NULL) {
            /* the reverseMap must be reallocated in a new sort */
            uprv_free(table->reverseMap);
            table->reverseMap=NULL;
        }
    }

    if(m->uLen>1 && table->codePointsCapacity==0) {
        table->codePointsCapacity=10000;
        table->codePoints=(UChar32 *)uprv_malloc(table->codePointsCapacity*4);
        if(table->codePoints==NULL) {
            fprintf(stderr, "ucm error: unable to allocate %d UChar32s\n",
                            table->codePointsCapacity);
            exit(U_MEMORY_ALLOCATION_ERROR);
        }
    }

    if(m->bLen>4 && table->bytesCapacity==0) {
        table->bytesCapacity=10000;
        table->bytes=(uint8_t *)uprv_malloc(table->bytesCapacity);
        if(table->bytes==NULL) {
            fprintf(stderr, "ucm error: unable to allocate %d bytes\n",
                            table->bytesCapacity);
            exit(U_MEMORY_ALLOCATION_ERROR);
        }
    }

    if(m->uLen>1) {
        index=table->codePointsLength;
        table->codePointsLength+=m->uLen;
        if(table->codePointsLength>table->codePointsCapacity) {
            fprintf(stderr, "ucm error: too many code points in multiple-code point mappings\n");
            exit(U_MEMORY_ALLOCATION_ERROR);
        }

        uprv_memcpy(table->codePoints+index, codePoints, m->uLen*4);
        m->u=index;
    }

    if(m->bLen>4) {
        index=table->bytesLength;
        table->bytesLength+=m->bLen;
        if(table->bytesLength>table->bytesCapacity) {
            fprintf(stderr, "ucm error: too many bytes in mappings with >4 charset bytes\n");
            exit(U_MEMORY_ALLOCATION_ERROR);
        }

        uprv_memcpy(table->bytes+index, bytes, m->bLen);
        m->b.index=index;
    }

    /* set unicodeMask */
    for(index=0; index<m->uLen; ++index) {
        c=codePoints[index];
        if(c>=0x10000) {
            table->unicodeMask|=UCNV_HAS_SUPPLEMENTARY; /* there are supplementary code points */
        } else if(U_IS_SURROGATE(c)) {
            table->unicodeMask|=UCNV_HAS_SURROGATES;    /* there are surrogate code points */
        }
    }

    /* set flagsType */
    if(m->f<0) {
        table->flagsType|=UCM_FLAGS_IMPLICIT;
    } else {
        table->flagsType|=UCM_FLAGS_EXPLICIT;
    }

    tm=table->mappings+table->mappingsLength++;
    uprv_memcpy(tm, m, sizeof(UCMapping));
}

U_CAPI UCMFile * U_EXPORT2
ucm_open() {
    UCMFile *ucm=(UCMFile *)uprv_malloc(sizeof(UCMFile));
    if(ucm==NULL) {
        fprintf(stderr, "ucm error: unable to allocate a UCMFile\n");
        exit(U_MEMORY_ALLOCATION_ERROR);
    }

    memset(ucm, 0, sizeof(UCMFile));

    ucm->base=ucm_openTable();
    ucm->ext=ucm_openTable();

    ucm->states.stateFlags[0]=MBCS_STATE_FLAG_DIRECT;
    ucm->states.conversionType=UCNV_UNSUPPORTED_CONVERTER;
    ucm->states.outputType=-1;
    ucm->states.minCharLength=ucm->states.maxCharLength=1;

    return ucm;
}

U_CAPI void U_EXPORT2
ucm_close(UCMFile *ucm) {
    if(ucm!=NULL) {
        uprv_free(ucm->base);
        uprv_free(ucm->ext);
        uprv_free(ucm);
    }
}

U_CAPI UBool U_EXPORT2
ucm_addMappingFromLine(UCMFile *ucm, const char *line, UBool forBase, UCMStates *baseStates) {
    UCMapping m={ 0 };
    UChar32 codePoints[UCNV_EXT_MAX_UCHARS];
    uint8_t bytes[UCNV_EXT_MAX_BYTES];
    int32_t count;

    if(!ucm_parseMappingLine(&m, codePoints, bytes, line)) {
        return FALSE;
    }

    if(baseStates!=NULL) {
        /* check validity of the bytes and count the characters in them */
        count=ucm_countChars(baseStates, bytes, m.bLen);
        if(count<1) {
            /* illegal byte sequence */
            printMapping(&m, codePoints, bytes, stderr);
            return FALSE;
        }
    } else {
        /* not used - adding a mapping for an extension-only table before its base table is read */
        count=0;
    }

    /*
     * Add the mapping to the base table if this is requested
     * and it is a 1:1 mapping.
     * Otherwise, add it to the extension table.
     *
     * Also add |2 SUB mappings for <subchar1>
     * and |1 fallbacks from something other than U+0000 to 0x00
     * to the extension table.
     */
    if( forBase && m.uLen==1 && count==1 &&
        !((m.f==2 && m.bLen==1 && ucm->states.maxCharLength>1) ||
          (m.f==1 && m.bLen==1 && bytes[0]==0 && !(m.uLen==1 && codePoints[0]==0)))
    ) {
        ucm_addMapping(ucm->base, &m, codePoints, bytes);
        return TRUE;
    }

    ucm_addMapping(ucm->ext, &m, codePoints, bytes);
    return TRUE;
}
