/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  genmbcs.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000jul06
*   created by: Markus W. Scherer
*/

#include <stdio.h>
#include "unicode/utypes.h"
#include "cstring.h"
#include "cmemory.h"
#include "unewdata.h"
#include "ucnvmbcs.h"
#include "genmbcs.h"

enum {
    MBCS_STATE_FLAG_DIRECT=1,
    MBCS_STATE_FLAG_SURROGATES,

    MBCS_STATE_FLAG_READY=16
};

enum {
    MBCS_MAX_STATE_COUNT=128,
    MBCS_MAX_FALLBACK_COUNT=1000
};

struct MBCSData {
    /* toUnicode */
    int32_t stateTable[MBCS_MAX_STATE_COUNT][256];
    uint32_t stateFlags[MBCS_MAX_STATE_COUNT],
             stateOffsetSum[MBCS_MAX_STATE_COUNT];
    _MBCSToUFallback toUFallbacks[MBCS_MAX_FALLBACK_COUNT];
    uint16_t *unicodeCodeUnits;
    _MBCSHeader header;
    uint32_t countToUCodeUnits;

    /* fromUnicode */
    uint16_t table[0x20440];
    uint8_t *fromUBytes;
    uint32_t stage2Top, stage3Top, maxCharLength;
};

static void
MBCSInit(MBCSData *mbcsData, uint8_t maxCharLength) {
    uprv_memset(mbcsData, 0, sizeof(MBCSData));
    mbcsData->header.version[0]=1;
    mbcsData->stateFlags[0]=MBCS_STATE_FLAG_DIRECT;
    mbcsData->maxCharLength=maxCharLength;
    mbcsData->header.flags=maxCharLength-1; /* outputType */
}

MBCSData *
MBCSOpen(uint8_t maxCharLength) {
    MBCSData *mbcsData=(MBCSData *)uprv_malloc(sizeof(MBCSData));
    if(mbcsData!=NULL) {
        MBCSInit(mbcsData, maxCharLength);
    }
    return mbcsData;
}

void
MBCSClose(MBCSData *mbcsData) {
    if(mbcsData!=NULL) {
        if(mbcsData->unicodeCodeUnits!=NULL) {
            uprv_free(mbcsData->unicodeCodeUnits);
        }
        if(mbcsData->fromUBytes!=NULL) {
            uprv_free(mbcsData->fromUBytes);
        }
        uprv_free(mbcsData);
    }
}

const char *
skipWhitespace(const char *s) {
    while(*s==' ' || *s=='\t') {
        ++s;
    }
    return s;
}

/*
 * state table row grammar (ebnf-style):
 * (whitespace is allowed between all tokens)
 *
 * row=[firstentry ','] entry (',' entry)*
 * firstentry="initial" | "surrogates"
 *            (initial state (default for state 0), output is all surrogate pairs)
 * entry=range [':' nextstate] ['.' action]
 * range=number ['-' number]
 * nextstate=number
 *           (0..7f)
 * action='u' | 's' | 'p' | 'i'
 *        (unassigned, state change only, surrogate pair, illegal)
 * number=(1- or 2-digit hexadecimal number)
 */
static const char *
parseState(const char *s, int32_t state[256], uint32_t *pFlags) {
    const char *t;
    uint32_t start, end, i;
    int32_t value;

    /* initialize the state */
    for(i=0; i<256; ++i) {
        state[i]=0x80000000|(MBCS_STATE_ILLEGAL<<27);
    }

    /* skip leading white space */
    s=skipWhitespace(s);

    /* is there a "direct" or "surrogates" directive? */
    if(uprv_strncmp("direct", s, 6)==0) {
        *pFlags=MBCS_STATE_FLAG_DIRECT;
        s=skipWhitespace(s+6);
        if(*s++!=',') {
            return s-1;
        }
    } else if(*pFlags==0 && uprv_strncmp("surrogates", s, 10)==0) {
        *pFlags=MBCS_STATE_FLAG_SURROGATES;
        s=skipWhitespace(s+10);
        if(*s++!=',') {
            return s-1;
        }
    }

    for(;;) {
        /* read an entry, the start of the range first */
        s=skipWhitespace(s);
        start=uprv_strtoul(s, (char **)&t, 16);
        if(s==t || 0xff<start) {
            return s;
        }
        s=skipWhitespace(t);

        /* read the end of the range if there is one */
        if(*s=='-') {
            s=skipWhitespace(s+1);
            end=uprv_strtoul(s, (char **)&t, 16);
            if(s==t || end<start || 0xff<end) {
                return s;
            }
            s=skipWhitespace(t);
        } else {
            end=start;
        }

        /* determine the state values for this range */
        if(*s!=':' && *s!='.') {
            /* the default is: final state with valid entries */
            value=0x80000000|(MBCS_STATE_VALID_16<<27UL);
        } else {
            value=0;
            if(*s==':') {
                /* get the next state, default to 0 */
                s=skipWhitespace(s+1);
                i=uprv_strtoul(s, (char **)&t, 16);
                if(s!=t) {
                    if(0x7f<i) {
                        return s;
                    }
                    s=skipWhitespace(t);
                    value|=i;
                }
            }

            /* get the state action, default to valid */
            if(*s=='.') {
                /* this is a final state */
                value|=0x80000000;

                s=skipWhitespace(s+1);
                if(*s=='u') {
                    value|=MBCS_STATE_UNASSIGNED<<27UL;
                    s=skipWhitespace(s+1);
                } else if(*s=='p') {
                    if(*pFlags!=MBCS_STATE_FLAG_DIRECT) {
                        value|=MBCS_STATE_VALID_16_PAIR<<27UL;
                    } else {
                        value|=MBCS_STATE_VALID_16<<27UL;
                    }
                    s=skipWhitespace(s+1);
                } else if(*s=='s') {
                    value|=MBCS_STATE_CHANGE_ONLY<<27UL;
                    s=skipWhitespace(s+1);
                } else if(*s=='i') {
                    value|=MBCS_STATE_ILLEGAL<<27UL;
                    s=skipWhitespace(s+1);
                } else {
                    value|=MBCS_STATE_VALID_16<<27UL;
                }
            } else {
                /* this is an intermediate state, nothing to do */
            }
        }

        /* adjust "final valid" states according to the state flags */
        if(((uint32_t)value>>27U)==(16|MBCS_STATE_VALID_16)) {
            switch(*pFlags) {
            case 0:
                /* no adjustment */
                break;
            case MBCS_STATE_FLAG_DIRECT:
                /* set the valid-direct code point to "unassigned"==0xfffe */
                value=value&0x87ffffff|(MBCS_STATE_VALID_DIRECT_16<<27UL)|(0xfffe<<7L);
                break;
            case MBCS_STATE_FLAG_SURROGATES:
                value=value&0x87ffffff|(MBCS_STATE_VALID_16_PAIR<<27UL);
                break;
            default:
                break;
            }
        }

        /* set this value for the range */
        for(i=start; i<=end; ++i) {
            state[i]=value;
        }

        if(*s==',') {
            ++s;
        } else {
            return *s==0 ? NULL : s;
        }
    }
}

UBool
MBCSAddState(MBCSData *mbcsData, const char *s) {
    const char *error;

    if(mbcsData->header.countStates==MBCS_MAX_STATE_COUNT) {
        fprintf(stderr, "error: too many states (maximum %u)\n", MBCS_MAX_STATE_COUNT);
        return FALSE;
    }

    error=parseState(s, mbcsData->stateTable[mbcsData->header.countStates],
                       &mbcsData->stateFlags[mbcsData->header.countStates]);
    if(error!=NULL) {
        fprintf(stderr, "parse error in state definition at '%s'\n", error);
        return FALSE;
    }

    ++mbcsData->header.countStates;
    return TRUE;
}

UBool
MBCSProcessStates(MBCSData *mbcsData) {
    uint32_t sum, i;
    int32_t entry;
    int state, cell, count;
    UBool allStatesReady;

    /*
     * first make sure that all "next state" values are within limits
     * and that all next states after final ones have the "direct"
     * flag of initial states
     */
    for(state=mbcsData->header.countStates-1; state>=0; --state) {
        for(cell=0; cell<256; ++cell) {
            entry=mbcsData->stateTable[state][cell];
            if((uint32_t)(entry&0x7f)>=mbcsData->header.countStates) {
                fprintf(stderr, "error: state table entry [%x][%x] has a next state of %x that is too high\n",
                    state, cell, entry&0x7f);
                return FALSE;
            }
            if(entry<0 && mbcsData->stateFlags[entry&0x7f]!=MBCS_STATE_FLAG_DIRECT) {
                fprintf(stderr, "error: state table entry [%x][%x] is final but has a non-initial next state of %x\n",
                    state, cell, entry&0x7f);
                return FALSE;
            }
        }
    }

    /*
     * Sum up the offsets for all states.
     * In each final state (where there are only final entries),
     * the offsets add up directly.
     * In all other state table rows, for each transition entry to another state,
     * the offsets sum of that state needs to be added.
     * This is achieved in at most countStates iterations.
     */
    allStatesReady=FALSE;
    for(count=mbcsData->header.countStates; !allStatesReady && count>=0; --count) {
        allStatesReady=TRUE;
        for(state=mbcsData->header.countStates-1; state>=0; --state) {
            if(!(mbcsData->stateFlags[state]&MBCS_STATE_FLAG_READY)) {
                allStatesReady=FALSE;
                sum=0;

                /* at first, add up only the final delta offsets to keep them <512 */
                for(cell=0; cell<256; ++cell) {
                    entry=mbcsData->stateTable[state][cell];
                    if(entry<0) {
                        switch((uint32_t)entry>>27U) {
                        case 16|MBCS_STATE_VALID_16:
                            mbcsData->stateTable[state][cell]=entry&0xf800007f|(sum<<7L);
                            sum+=1;
                            break;
                        case 16|MBCS_STATE_VALID_16_PAIR:
                            mbcsData->stateTable[state][cell]=entry&0xf800007f|(sum<<7L);
                            sum+=2;
                            break;
                        default:
                            /* no addition */
                            break;
                        }
                    }
                }

                /* now, add up the delta offsets for the transitional entries */
                for(cell=0; cell<256; ++cell) {
                    entry=mbcsData->stateTable[state][cell];
                    if(entry>=0) {
                        if(mbcsData->stateFlags[entry&0x7f]&MBCS_STATE_FLAG_READY) {
                            mbcsData->stateTable[state][cell]=entry&0xf800007f|(sum<<7L);
                            sum+=mbcsData->stateOffsetSum[entry&0x7f];
                        } else {
                            /* that next state does not have a sum yet, we cannot finish the one for this state */
                            sum=0xffffffff;
                            break;
                        }
                    }
                }

                if(sum!=0xffffffff) {
                    mbcsData->stateOffsetSum[state]=sum;
                    mbcsData->stateFlags[state]|=MBCS_STATE_FLAG_READY;
                }
            }
        }
    }

    if(!allStatesReady) {
        fprintf(stderr, "error: the state table contains loops\n");
        return FALSE;
    }

    /*
     * For all "direct" (i.e., initial) states>0,
     * the offsets need to be increased by the sum of
     * the previous initial states.
     */
    sum=mbcsData->stateOffsetSum[0];
    for(state=1; state<(int)mbcsData->header.countStates; ++state) {
        if((mbcsData->stateFlags[state]&0xf)==MBCS_STATE_FLAG_DIRECT) {
            uint32_t sum2=sum<<7;
            sum+=mbcsData->stateOffsetSum[state];
            for(cell=0; cell<256; ++cell) {
                entry=mbcsData->stateTable[state][cell];
                if(entry>=0) {
                    mbcsData->stateTable[state][cell]=entry+sum2;
                }
            }
        }
    }
    if(VERBOSE) {
        printf("the total number of offsets is 0x%lx=%lu\n", sum, sum);
    }

    /* round up to the next even number to have the following data 32-bit-aligned */
    sum=(sum+1)&~1;
    mbcsData->countToUCodeUnits=sum;

    /* allocate the code unit array and prefill it with "unassigned" values */
    if(sum>0) {
        mbcsData->unicodeCodeUnits=(uint16_t *)uprv_malloc(sum*sizeof(uint16_t));
        if(mbcsData->unicodeCodeUnits==NULL) {
            fprintf(stderr, "error: out of memory allocating %ld 16-bit code units\n", sum);
            return FALSE;
        }
        for(i=0; i<sum; ++i) {
            mbcsData->unicodeCodeUnits[i]=0xfffe;
        }
    }

    /* allocate the codepage mappings and preset the first 16 characters to 0 */
    mbcsData->fromUBytes=(uint8_t *)uprv_malloc(0x100000*mbcsData->maxCharLength); /* 1M mappings is the maximum possible */
    if(mbcsData->fromUBytes==NULL) {
        fprintf(stderr, "error: out of memory allocating %ldMB for target mappings\n", mbcsData->maxCharLength);
        return FALSE;
    }
    uprv_memset(mbcsData->fromUBytes, 0, 16*mbcsData->maxCharLength);
    mbcsData->stage2Top=0x80;
    mbcsData->stage3Top=16*mbcsData->maxCharLength;

    return TRUE;
}

static UBool
setFallback(MBCSData *mbcsData, uint32_t offset, UChar32 c) {
    _MBCSToUFallback *toUFallbacks=mbcsData->toUFallbacks;
    uint32_t i, limit;

    /* first, see if there is already a fallback for this offset */
    limit=mbcsData->header.countToUFallbacks;

    /* do a linear search for the fallback mapping (the table is not yet sorted) */
    for(i=0; i<limit; ++i) {
        if(offset==toUFallbacks[i].offset) {
            toUFallbacks[i].codePoint=c;
            return TRUE;
        }
    }

    /* if there is no fallback for this offset, then add one */
    if(limit>=MBCS_MAX_FALLBACK_COUNT) {
        fprintf(stderr, "error: too many toUnicode fallbacks, currently at: U+%lx\n", c);
        return FALSE;
    }
    toUFallbacks[limit].offset=offset;
    toUFallbacks[limit].codePoint=c;
    mbcsData->header.countToUFallbacks=limit+1;
    return TRUE;
}

static void
removeFallback(MBCSData *mbcsData, uint32_t offset) {
    _MBCSToUFallback *toUFallbacks=mbcsData->toUFallbacks;
    uint32_t i, limit;

    /* see if there is a fallback for this offset */
    limit=mbcsData->header.countToUFallbacks;

    /* do a linear search for the fallback mapping (the table is not yet sorted) */
    for(i=0; i<limit; ++i) {
        if(offset==toUFallbacks[limit].offset) {
            /* copy the last fallback entry here to keep the list contiguous */
            toUFallbacks[i].offset=toUFallbacks[limit-1].offset;
            toUFallbacks[i].codePoint=toUFallbacks[limit-1].codePoint;
            mbcsData->header.countToUFallbacks=limit-1;
            return;
        }
    }
}

/*
 * isFallback is almost a boolean:
 * 1 (TRUE)  this is a fallback mapping
 * 0 (FALSE) this is a precise mapping
 * -1        the precision of this mapping is not specified
 */
UBool
MBCSAddToUnicode(MBCSData *mbcsData,
                 const uint8_t *bytes, int32_t length,
                 UChar32 c,
                 int8_t isFallback) {
    uint32_t offset=0, b=0;
    int32_t i=0, entry;
    uint8_t state=0;

    if(mbcsData->header.countStates==0) {
        fprintf(stderr, "error: there is no state information!\n");
        return FALSE;
    }

    /* put together a 32-bit value for the byte sequence for errors */
    for(i=0; i<length; ++i) {
        b=(b<<8)|bytes[i];
    }

    /*
     * Walk down the state table like in conversion,
     * much like getNextUChar().
     * We assume that c<=0x10ffff.
     */
    for(i=0;;) {
        entry=mbcsData->stateTable[state][bytes[i++]];
        if(entry>=0) {
            if(i==length) {
                fprintf(stderr, "error: byte sequence too short, ends in non-final state %hu: %lx (U+%lx)\n", state, b, c);
                return FALSE;
            }
            state=(uint8_t)(entry&0x7f);
            offset+=entry>>7;
        } else {
            if(i<length) {
                fprintf(stderr, "error: byte sequence too long by %d bytes, final state %hu: %lx (U+%lx)\n", (length-i), state, b, c);
                return FALSE;
            }
            switch((uint32_t)entry>>27U) {
            case 16|MBCS_STATE_ILLEGAL:
                fprintf(stderr, "error: byte sequence ends in illegal state: %lx (U+%lx)\n", b, c);
                return FALSE;
            case 16|MBCS_STATE_CHANGE_ONLY:
                fprintf(stderr, "error: byte sequence ends in state-change-only: %lx (U+%lx)\n", b, c);
                return FALSE;
            case 16|MBCS_STATE_UNASSIGNED:
                fprintf(stderr, "error: byte sequence ends in unassigned state: %lx (U+%lx)\n", b, c);
                return FALSE;
            case 16|MBCS_STATE_FALLBACK_DIRECT_16:
            case 16|MBCS_STATE_VALID_DIRECT_16:
            case 16|MBCS_STATE_FALLBACK_DIRECT_20:
            case 16|MBCS_STATE_VALID_DIRECT_20:
                if((entry&0x7ffff80)!=0x7fff00) {
                    /* the "direct" action's value is not "unassigned" any more */
                    if(isFallback>=0 && (uint32_t)entry>>27U>=(16|MBCS_STATE_VALID_DIRECT_16)) {
                        /* do not overwrite precise mappings with specified-precision mappings */
                        if(isFallback==0) {
                            /* precise over precise: error */
                            fprintf(stderr, "error: duplicate byte sequence: %lx (U+%lx)\n", b, c);
                            return FALSE;
                        } else {
                            /* fallback over precise: ignore */
                            if(VERBOSE) {
                                fprintf(stderr, "duplicate byte sequence: %lx (U+%lx)\n", b, c);
                            }
                            return TRUE;
                        }
                    }
                    if(VERBOSE) {
                        fprintf(stderr, "duplicate byte sequence: %lx (U+%lx)\n", b, c);
                    }
                    /*
                     * Continue after the above warning
                     * if the precision of the mapping is unspecified
                     * or a fallback is overriding a previous fallback.
                     */
                }
                /* reassign the correct action code */
                entry=
                    entry&0x8000007f|
                    (MBCS_STATE_FALLBACK_DIRECT_16+(isFallback>0 ? 0 : 2)+(c>=0x10000 ? 1 : 0))
                        <<27;
                /* put the code point into bits 22..7 for BMP, c-0x10000 into 26..7 for others */
                if(c<=0xffff) {
                    entry|=c<<7;
                } else {
                    entry|=(c-0x10000)<<7;
                }
                mbcsData->stateTable[state][bytes[i-1]]=entry;
                break;
            case 16|MBCS_STATE_VALID_16:
                /* bits 26..16 are not used, 0 */
                /* bits 15..7 contain the final offset delta to one 16-bit code unit */
                offset+=(uint16_t)entry>>7;
                if(isFallback>0) {
                    /* assign only if there is no precise mapping */
                    if(mbcsData->unicodeCodeUnits[offset]==0xfffe) {
                        return setFallback(mbcsData, offset, c);
                    }
                } else {
                    if(c>=0x10000) {
                        fprintf(stderr, "error: code point does not fit into valid-16-bit state: %lx (U+%lx)\n", b, c);
                        return FALSE;
                    }
                    if(mbcsData->unicodeCodeUnits[offset]!=0xfffe) {
                        if(isFallback==0) {
                            fprintf(stderr, "error: duplicate byte sequence: %lx (U+%lx)\n", b, c);
                            return FALSE;
                        }
                        if(VERBOSE) {
                            fprintf(stderr, "duplicate byte sequence: %lx (U+%lx)\n", b, c);
                        }
                        /* continue after the above warning if the precision of the mapping is unspecified */
                    }
                    mbcsData->unicodeCodeUnits[offset]=(uint16_t)c;
                    removeFallback(mbcsData, offset);
                }
                break;
            case 16|MBCS_STATE_VALID_16_PAIR:
                /* bits 26..16 are not used, 0 */
                /* bits 15..7 contain the final offset delta to two 16-bit code units */
                offset+=(uint16_t)entry>>7;
                if(isFallback>0) {
                    /* assign only if there is no precise mapping */
                    if(mbcsData->unicodeCodeUnits[offset]==0xfffe) {
                        return setFallback(mbcsData, offset, c);
                    }
                } else {
                    if(mbcsData->unicodeCodeUnits[offset]!=0xfffe) {
                        if(isFallback==0) {
                            fprintf(stderr, "error: duplicate byte sequence: %lx (U+%lx)\n", b, c);
                            return FALSE;
                        }
                        if(VERBOSE) {
                            fprintf(stderr, "duplicate byte sequence: %lx (U+%lx)\n", b, c);
                        }
                        /* continue after the above warning if the precision of the mapping is unspecified */
                    }
                    if(c<=0xffff) {
                        /* set BMP code point */
                        mbcsData->unicodeCodeUnits[offset]=(uint16_t)c;
                    } else {
                        /* set a surrogate pair */
                        mbcsData->unicodeCodeUnits[offset++]=(uint16_t)(0xd7c0+(c>>10));
                        mbcsData->unicodeCodeUnits[offset]=(uint16_t)(0xdc00+(c&0x3ff));
                    }
                    removeFallback(mbcsData, offset);
                }
                break;
            default:
                /* reserved, must never occur */
                fprintf(stderr, "internal error: byte sequence reached reserved action code, entry %lx: %lx (U+%lx)\n", entry, b, c);
                return FALSE;
            }

            return TRUE;
        }
    }
}

UBool
MBCSAddFromUnicode(MBCSData *mbcsData,
                   const uint8_t *bytes, int32_t length,
                   UChar32 c,
                   int8_t isFallback) {
    uint8_t *p;
    uint32_t i, b=0, index;

    /* put together a 32-bit value for the byte sequence for errors */
    for(i=0; i<(uint32_t)length; ++i) {
        b=(b<<8)|bytes[i];
    }

    /*
     * Walk down the triple-stage compact array and
     * allocate parts as necessary.
     * Note that stage 2 and 3 blocks 0 are reserved for all-unassigned mappings.
     * We assume that length<=maxCharLength and that c<=0x10ffff.
     */

    /* inspect stage 1 */
    index=c>>10;
    if(mbcsData->table[index]==0) {
        /* allocate another block in stage 2 */
        if(mbcsData->stage2Top==2*0xffc0) {
            fprintf(stderr, "error: too many code points: %lx (U+%lx)\n", b, c);
            return FALSE;
        }
        /*
         * each block has 64*2 entries:
         * 6 code point bits 9..4 with 1 flags value and 1 stage 3 index
         * stage 1 values are half of the indexes to the stage 2 blocks
         * so that they fit into 16 bits;
         * therefore, stage 1 values increase only by 64 per stage 2 block
         */
        mbcsData->table[index]=(uint16_t)(mbcsData->stage2Top/2);
        mbcsData->stage2Top+=0x80;
    }

    /* inspect stage 2 */
    index=0x440+2*((uint32_t)mbcsData->table[index]+((c>>4)&0x3f));
    if(mbcsData->table[index+1]==0) {
        /* allocate another block in stage 3 */
        if(mbcsData->stage3Top+16*mbcsData->maxCharLength>=0x100000) {
            fprintf(stderr, "error: too many code points: %lx (U+%lx)\n", b, c);
            return FALSE;
        }
        /* each block has 16*maxCharLength bytes */
        mbcsData->table[index+1]=(uint16_t)((mbcsData->stage3Top/16)/mbcsData->maxCharLength);
        uprv_memset(mbcsData->fromUBytes+mbcsData->stage3Top, 0, 16*mbcsData->maxCharLength);
        mbcsData->stage3Top+=16*mbcsData->maxCharLength;
    }

    if(isFallback<=0) {
        /* for a precise mapping, make sure that there is no other precise one */
        if((mbcsData->table[index]&(1<<(c&0xf)))!=0) {
            if(isFallback==0) {
                fprintf(stderr, "error: duplicate code point: %lx (U+%lx)\n", b, c);
                return FALSE;
            }
            if(VERBOSE) {
                fprintf(stderr, "duplicate code point: %lx (U+%lx)\n", b, c);
            }
            /* continue after the above warning if the precision of the mapping is unspecified */
        }

        /* set the "assigned" flag */
        mbcsData->table[index]|=(1<<(c&0xf));
    } else {
        /* do not write a fallback if there is a precise mapping already */
        if((mbcsData->table[index]&(1<<(c&0xf)))!=0) {
            return TRUE;
        }
    }

    /* write the codepage bytes into stage 3 */
    ++index;
    p=mbcsData->fromUBytes+(16*(uint32_t)mbcsData->table[index]+(c&0xf))*mbcsData->maxCharLength;
    switch(mbcsData->maxCharLength) {
    case 4:
        *p++=(uint8_t)(b>>24);
    case 3:
        *p++=(uint8_t)(b>>16);
    case 2:
        *p++=(uint8_t)(b>>8);
    case 1:
        *p++=(uint8_t)b;
    default:
        break;
    }

    return TRUE;
}

static int
compareFallbacks(const void *fb1, const void *fb2) {
    return ((const _MBCSToUFallback *)fb1)->offset-((const _MBCSToUFallback *)fb2)->offset;
}

static UBool
MBCSTransformEUC(MBCSData *mbcsData) {
    uint8_t *p, *q;
    uint32_t i, oldLength=mbcsData->maxCharLength, old3Top=mbcsData->stage3Top, new3Top;
    uint8_t b;

    if(oldLength<3) {
        return FALSE;
    }

    /* test if all first bytes are in {0, 0x8e, 0x8f} */
    p=mbcsData->fromUBytes;
    for(i=0; i<old3Top; i+=oldLength) {
        b=p[i];
        if(b!=0 && b!=0x8e && b!=0x8f) {
            /* some first byte does not fit the EUC pattern, nothing to be done */
            return FALSE;
        }
    }

    /* modify outputType and adjust stage3Top */
    mbcsData->header.flags=MBCS_OUTPUT_3_EUC+oldLength-3;
    mbcsData->stage3Top=new3Top=(old3Top*(oldLength-1))/oldLength;

    /*
     * EUC-encode all byte sequences;
     * see "CJKV Information Processing" (1st ed. 1999) from Ken Lunde, O'Reilly,
     * p. 161 in chapter 4 "Encoding Methods"
     */
    q=p;
    for(i=0; i<old3Top; i+=oldLength) {
        b=*p++;
        if(b==0) {
            /* short sequences are stored directly */
            /* code set 0 or 1 */
            *q++=*p++;
            *q++=*p++;
        } else if(b==0x8e) {
            /* code set 2 */
            *q++=(uint8_t)(*p++&0x7f);
            *q++=*p++;
        } else /* b==0x8f */ {
            /* code set 3 */
            *q++=*p++;
            *q++=(uint8_t)(*p++&0x7f);
        }
        if(oldLength==4) {
            *q++=*p++;
        }
    }

    return TRUE;
}

void
MBCSPostprocess(MBCSData *mbcsData) {
    int32_t entry;
    int state, cell;

    /* this needs to be printed before the EUC transformation because later maxCharLength might not be correct */
    if(VERBOSE) {
        printf("number of codepage characters in 16-blocks: 0x%lx=%lu\n",
               mbcsData->stage3Top/mbcsData->maxCharLength,
               mbcsData->stage3Top/mbcsData->maxCharLength);
    }

    /* test each state table entry */
    for(state=0; state<(int)mbcsData->header.countStates; ++state) {
        for(cell=0; cell<256; ++cell) {
            entry=mbcsData->stateTable[state][cell];
            /*
             * if the entry is a final one with a "...-direct" action code
             * and the code point is "unassigned" (0xfffe), then change it to
             * the "unassigned" action code with bits 26..7 set to zero.
             */
            if( ((uint32_t)(((entry&0xf8000000)>>27U)-(16|MBCS_STATE_FALLBACK_DIRECT_16))<=3) &&
                (entry&0x7ffff80)==0x7fff00
            ) {
                mbcsData->stateTable[state][cell]=(entry&0x8000007f)|(MBCS_STATE_UNASSIGNED<<27UL);
            }
        }
    }

    /* sort toUFallbacks */
    if(mbcsData->header.countToUFallbacks>0) {
        qsort(mbcsData->toUFallbacks, mbcsData->header.countToUFallbacks, sizeof(_MBCSToUFallback), compareFallbacks);
    }

    MBCSTransformEUC(mbcsData);
}

uint32_t
MBCSWrite(MBCSData *mbcsData, UNewDataMemory *pData) {
    /* fill the header */
    mbcsData->header.offsetToUCodeUnits=
        sizeof(_MBCSHeader)+
        mbcsData->header.countStates*1024+
        mbcsData->header.countToUFallbacks*sizeof(_MBCSToUFallback);
    mbcsData->header.offsetFromUTable=
        mbcsData->header.offsetToUCodeUnits+
        mbcsData->countToUCodeUnits*2;
    mbcsData->header.offsetFromUBytes=
        mbcsData->header.offsetFromUTable+
        (0x440+mbcsData->stage2Top)*2;

    /* write the MBCS data */
    udata_writeBlock(pData, &mbcsData->header, sizeof(_MBCSHeader));
    udata_writeBlock(pData, mbcsData->stateTable, mbcsData->header.countStates*1024);
    udata_writeBlock(pData, mbcsData->toUFallbacks, mbcsData->header.countToUFallbacks*sizeof(_MBCSToUFallback));
    udata_writeBlock(pData, mbcsData->unicodeCodeUnits, mbcsData->countToUCodeUnits*2);
    udata_writeBlock(pData, mbcsData->table, (0x440+mbcsData->stage2Top)*2);
    udata_writeBlock(pData, mbcsData->fromUBytes, mbcsData->stage3Top);

    /* return the number of bytes that should have been written */
    return mbcsData->header.offsetFromUBytes+mbcsData->stage3Top;
}

#if 0
    /* test code, uses only this file and genmbcs.h */

extern int
main(int argc, const char *argv[]) {
    MBCSData *mbcsData;
    static uint8_t bytes[4];
    int32_t entry;
    int i, j;

    /*
     * sample arguments for shift-jis (max 2):
     * 0-7f,81-9f:1,a1-df,e0-ef:1  40-7e,80-fc
     *
     * sample arguments for euc-jp (max 3):
     * 0-7f,8e:2,8f:3,a1-fe:1  a1-fe  a1-df  a1-fe:1
     */
    if(argc>=2) {
        mbcsData=MBCSOpen(3);

        for(i=1; i<argc; ++i) {
            if(!MBCSAddState(mbcsData, argv[i])) {
                return 2;
            }
        }
        MBCSProcessStates(mbcsData);

        bytes[0]=0x5c;
        MBCSAddToUnicode(mbcsData, bytes, 1, 0xa5, TRUE);
        MBCSAddFromUnicode(mbcsData, bytes, 1, 0xa5, TRUE);
        bytes[0]=0xe2;
        bytes[1]=0xa3;
        MBCSAddToUnicode(mbcsData, bytes, 2, 0x4e00, FALSE);
        MBCSAddFromUnicode(mbcsData, bytes, 2, 0x4e00, FALSE);
        bytes[0]=0x8e;
        bytes[1]=0xdf;
        MBCSAddToUnicode(mbcsData, bytes, 2, 0x3415, FALSE);
        MBCSAddFromUnicode(mbcsData, bytes, 2, 0x3415, FALSE);
        bytes[0]=0x8f;
        bytes[1]=0xbb;
        bytes[2]=0xcc;
        MBCSAddToUnicode(mbcsData, bytes, 3, 0x9876, FALSE);
        MBCSAddFromUnicode(mbcsData, bytes, 3, 0x9876, FALSE);

        MBCSPostprocess(mbcsData);

        for(i=0; i<(int)mbcsData->header.countStates; ++i) {
            printf("state=%x: flags=%x\n", i, mbcsData->stateFlags[i]);
            for(j=0; j<256; ++j) {
                entry=mbcsData->stateTable[i][j];
                printf("%2lx  %8lx = %u.%x.%5x.%2x\n", j, entry,
                       (uint32_t)entry>>31, entry>>27&0xf, entry>>7&0xfffff, entry&0x7f);
            }
        }

        MBCSClose(mbcsData);
    } else {
        fprintf(stderr, "error: missing state table arguments\n");
        return 1;
    }
    return 0;
}
#endif
