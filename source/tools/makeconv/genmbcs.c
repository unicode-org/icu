/*
*******************************************************************************
*
*   Copyright (C) 2000-2003, International Business Machines
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
#include "ucnv_cnv.h"
#include "ucnvmbcs.h"
#include "makeconv.h"
#include "genmbcs.h"

enum {
    MBCS_STATE_FLAG_DIRECT=1,
    MBCS_STATE_FLAG_SURROGATES,

    MBCS_STATE_FLAG_READY=16
};

enum {
    MBCS_STAGE_2_BLOCK_SIZE=0x40, /* 64; 64=1<<6 for 6 bits in stage 2 */
    MBCS_STAGE_2_BLOCK_SIZE_SHIFT=6, /* log2(MBCS_STAGE_2_BLOCK_SIZE) */
    MBCS_STAGE_1_SIZE=0x440, /* 0x110000>>10, or 17*64 for one entry per 1k code points */
    MBCS_STAGE_2_SIZE=0xfbc0, /* 0x10000-MBCS_STAGE_1_SIZE */
    MBCS_MAX_STAGE_2_TOP=MBCS_STAGE_2_SIZE,
    MBCS_STAGE_2_MAX_BLOCKS=MBCS_STAGE_2_SIZE>>MBCS_STAGE_2_BLOCK_SIZE_SHIFT,

    MBCS_STAGE_2_ALL_UNASSIGNED_INDEX=0, /* stage 1 entry for the all-unassigned stage 2 block */
    MBCS_STAGE_2_FIRST_ASSIGNED=MBCS_STAGE_2_BLOCK_SIZE, /* start of the first stage 2 block after the all-unassigned one */

    MBCS_MAX_STATE_COUNT=128,
    MBCS_MAX_FALLBACK_COUNT=8192
};

typedef struct MBCSData {
    NewConverter newConverter;

    /* toUnicode */
    int32_t stateTable[MBCS_MAX_STATE_COUNT][256];
    uint32_t stateFlags[MBCS_MAX_STATE_COUNT],
             stateOffsetSum[MBCS_MAX_STATE_COUNT];
    _MBCSToUFallback toUFallbacks[MBCS_MAX_FALLBACK_COUNT];
    uint16_t *unicodeCodeUnits;
    _MBCSHeader header;
    int32_t countToUCodeUnits;

    /* fromUnicode */
    uint16_t stage1[MBCS_STAGE_1_SIZE];
    uint16_t stage2Single[MBCS_STAGE_2_SIZE]; /* stage 2 for single-byte codepages */
    uint32_t stage2[MBCS_STAGE_2_SIZE]; /* stage 2 for MBCS */
    uint8_t *fromUBytes;
    uint32_t stage2Top, stage3Top, maxCharLength;
} MBCSData;

/* prototypes */
static void
MBCSClose(NewConverter *cnvData);

static UBool
MBCSProcessStates(NewConverter *cnvData);

static UBool
MBCSAddToUnicode(NewConverter *cnvData,
                 const uint8_t *bytes, int32_t length,
                 UChar32 c, uint32_t b,
                 int8_t isFallback);

static UBool
MBCSIsValid(NewConverter *cnvData,
            const uint8_t *bytes, int32_t length,
            uint32_t b);

static UBool
MBCSSingleAddFromUnicode(NewConverter *cnvData,
                         const uint8_t *bytes, int32_t length,
                         UChar32 c, uint32_t b,
                         int8_t isFallback);

static UBool
MBCSAddFromUnicode(NewConverter *cnvData,
                   const uint8_t *bytes, int32_t length,
                   UChar32 c, uint32_t b,
                   int8_t isFallback);

static void
MBCSPostprocess(NewConverter *cnvData, const UConverterStaticData *staticData);

static uint32_t
MBCSWrite(NewConverter *cnvData, const UConverterStaticData *staticData, UNewDataMemory *pData);

/* implementation ----------------------------------------------------------- */

static void
MBCSInit(MBCSData *mbcsData, uint8_t maxCharLength) {
    int i;

    uprv_memset(mbcsData, 0, sizeof(MBCSData));

    mbcsData->newConverter.close=MBCSClose;
    mbcsData->newConverter.startMappings=MBCSProcessStates;
    mbcsData->newConverter.isValid=MBCSIsValid;
    mbcsData->newConverter.addToUnicode=MBCSAddToUnicode;
    if(maxCharLength==1) {
        mbcsData->newConverter.addFromUnicode=MBCSSingleAddFromUnicode;
    } else {
        mbcsData->newConverter.addFromUnicode=MBCSAddFromUnicode;
    }
    mbcsData->newConverter.finishMappings=MBCSPostprocess;
    mbcsData->newConverter.write=MBCSWrite;

    mbcsData->header.version[0]=4;
    mbcsData->header.version[1]=1;
    mbcsData->stateFlags[0]=MBCS_STATE_FLAG_DIRECT;
    mbcsData->stage2Top=MBCS_STAGE_2_FIRST_ASSIGNED; /* after stage 1 and one all-unassigned stage 2 block */
    mbcsData->stage3Top=16*maxCharLength; /* after one all-unassigned stage 3 block */
    mbcsData->maxCharLength=maxCharLength;
    mbcsData->header.flags=maxCharLength-1; /* outputType */

    /* point all entries in stage 1 to the "all-unassigned" first block in stage 2 */
    for(i=0; i<MBCS_STAGE_1_SIZE; ++i) {
        mbcsData->stage1[i]=MBCS_STAGE_2_ALL_UNASSIGNED_INDEX;
    }
}

NewConverter *
MBCSOpen(uint8_t maxCharLength) {
    MBCSData *mbcsData=(MBCSData *)uprv_malloc(sizeof(MBCSData));
    if(mbcsData!=NULL) {
        MBCSInit(mbcsData, maxCharLength);
    }
    return &mbcsData->newConverter;
}

static void
MBCSClose(NewConverter *cnvData) {
    MBCSData *mbcsData=(MBCSData *)cnvData;
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

static const char *
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
 * row=[[firstentry ','] entry (',' entry)*]
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
    int32_t entry;

    /* initialize the state: all illegal with U+ffff */
    for(i=0; i<256; ++i) {
        state[i]=MBCS_ENTRY_FINAL(0, MBCS_STATE_ILLEGAL, 0xffff);
    }

    /* skip leading white space */
    s=skipWhitespace(s);

    /* is there an "initial" or "surrogates" directive? */
    if(uprv_strncmp("initial", s, 7)==0) {
        *pFlags=MBCS_STATE_FLAG_DIRECT;
        s=skipWhitespace(s+7);
        if(*s++!=',') {
            return s-1;
        }
    } else if(*pFlags==0 && uprv_strncmp("surrogates", s, 10)==0) {
        *pFlags=MBCS_STATE_FLAG_SURROGATES;
        s=skipWhitespace(s+10);
        if(*s++!=',') {
            return s-1;
        }
    } else if(*s==0) {
        /* empty state row: all-illegal */
        return NULL;
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

        /* determine the state entrys for this range */
        if(*s!=':' && *s!='.') {
            /* the default is: final state with valid entries */
            entry=MBCS_ENTRY_FINAL(0, MBCS_STATE_VALID_16, 0);
        } else {
            entry=MBCS_ENTRY_TRANSITION(0, 0);
            if(*s==':') {
                /* get the next state, default to 0 */
                s=skipWhitespace(s+1);
                i=uprv_strtoul(s, (char **)&t, 16);
                if(s!=t) {
                    if(0x7f<i) {
                        return s;
                    }
                    s=skipWhitespace(t);
                    entry=MBCS_ENTRY_SET_STATE(entry, i);
                }
            }

            /* get the state action, default to valid */
            if(*s=='.') {
                /* this is a final state */
                entry=MBCS_ENTRY_SET_FINAL(entry);

                s=skipWhitespace(s+1);
                if(*s=='u') {
                    /* unassigned set U+fffe */
                    entry=MBCS_ENTRY_FINAL_SET_ACTION_VALUE(entry, MBCS_STATE_UNASSIGNED, 0xfffe);
                    s=skipWhitespace(s+1);
                } else if(*s=='p') {
                    if(*pFlags!=MBCS_STATE_FLAG_DIRECT) {
                        entry=MBCS_ENTRY_FINAL_SET_ACTION(entry, MBCS_STATE_VALID_16_PAIR);
                    } else {
                        entry=MBCS_ENTRY_FINAL_SET_ACTION(entry, MBCS_STATE_VALID_16);
                    }
                    s=skipWhitespace(s+1);
                } else if(*s=='s') {
                    entry=MBCS_ENTRY_FINAL_SET_ACTION(entry, MBCS_STATE_CHANGE_ONLY);
                    s=skipWhitespace(s+1);
                } else if(*s=='i') {
                    /* illegal set U+ffff */
                    entry=MBCS_ENTRY_FINAL_SET_ACTION_VALUE(entry, MBCS_STATE_ILLEGAL, 0xffff);
                    s=skipWhitespace(s+1);
                } else {
                    /* default to valid */
                    entry=MBCS_ENTRY_FINAL_SET_ACTION(entry, MBCS_STATE_VALID_16);
                }
            } else {
                /* this is an intermediate state, nothing to do */
            }
        }

        /* adjust "final valid" states according to the state flags */
        if(MBCS_ENTRY_FINAL_ACTION(entry)==MBCS_STATE_VALID_16) {
            switch(*pFlags) {
            case 0:
                /* no adjustment */
                break;
            case MBCS_STATE_FLAG_DIRECT:
                /* set the valid-direct code point to "unassigned"==0xfffe */
                entry=MBCS_ENTRY_FINAL_SET_ACTION_VALUE(entry, MBCS_STATE_VALID_DIRECT_16, 0xfffe);
                break;
            case MBCS_STATE_FLAG_SURROGATES:
                entry=MBCS_ENTRY_FINAL_SET_ACTION_VALUE(entry, MBCS_STATE_VALID_16_PAIR, 0);
                break;
            default:
                break;
            }
        }

        /* set this entry for the range */
        for(i=start; i<=end; ++i) {
            state[i]=entry;
        }

        if(*s==',') {
            ++s;
        } else {
            return *s==0 ? NULL : s;
        }
    }
}

UBool
MBCSAddState(NewConverter *cnvData, const char *s) {
    MBCSData *mbcsData=(MBCSData *)cnvData;
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

static int32_t
sumUpStates(MBCSData *mbcsData) {
    int32_t entry, sum;
    int state, cell, count;
    UBool allStatesReady;

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
                    if(MBCS_ENTRY_IS_FINAL(entry)) {
                        switch(MBCS_ENTRY_FINAL_ACTION(entry)) {
                        case MBCS_STATE_VALID_16:
                            mbcsData->stateTable[state][cell]=MBCS_ENTRY_FINAL_SET_VALUE(entry, sum);
                            sum+=1;
                            break;
                        case MBCS_STATE_VALID_16_PAIR:
                            mbcsData->stateTable[state][cell]=MBCS_ENTRY_FINAL_SET_VALUE(entry, sum);
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
                    if(MBCS_ENTRY_IS_TRANSITION(entry)) {
                        if(mbcsData->stateFlags[MBCS_ENTRY_TRANSITION_STATE(entry)]&MBCS_STATE_FLAG_READY) {
                            mbcsData->stateTable[state][cell]=MBCS_ENTRY_TRANSITION_SET_OFFSET(entry, sum);
                            sum+=mbcsData->stateOffsetSum[MBCS_ENTRY_TRANSITION_STATE(entry)];
                        } else {
                            /* that next state does not have a sum yet, we cannot finish the one for this state */
                            sum=-1;
                            break;
                        }
                    }
                }

                if(sum!=-1) {
                    mbcsData->stateOffsetSum[state]=sum;
                    mbcsData->stateFlags[state]|=MBCS_STATE_FLAG_READY;
                }
            }
        }
    }

    if(!allStatesReady) {
        fprintf(stderr, "error: the state table contains loops\n");
        return -1;
    }

    /*
     * For all "direct" (i.e., initial) states>0,
     * the offsets need to be increased by the sum of
     * the previous initial states.
     */
    sum=mbcsData->stateOffsetSum[0];
    for(state=1; state<(int)mbcsData->header.countStates; ++state) {
        if((mbcsData->stateFlags[state]&0xf)==MBCS_STATE_FLAG_DIRECT) {
            int32_t sum2=sum;
            sum+=mbcsData->stateOffsetSum[state];
            for(cell=0; cell<256; ++cell) {
                entry=mbcsData->stateTable[state][cell];
                if(MBCS_ENTRY_IS_TRANSITION(entry)) {
                    mbcsData->stateTable[state][cell]=MBCS_ENTRY_TRANSITION_ADD_OFFSET(entry, sum2);
                }
            }
        }
    }
    if(VERBOSE) {
        printf("the total number of offsets is 0x%lx=%ld\n",
            (unsigned long)sum, (long)sum);
    }

    /* round up to the next even number to have the following data 32-bit-aligned */
    sum=(sum+1)&~1;
    return mbcsData->countToUCodeUnits=sum;
}

static UBool
MBCSProcessStates(NewConverter *cnvData) {
    MBCSData *mbcsData=(MBCSData *)cnvData;
    int32_t i, entry, sum;
    int state, cell;

    /*
     * first make sure that all "next state" values are within limits
     * and that all next states after final ones have the "direct"
     * flag of initial states
     */
    for(state=mbcsData->header.countStates-1; state>=0; --state) {
        for(cell=0; cell<256; ++cell) {
            entry=mbcsData->stateTable[state][cell];
            if((uint8_t)MBCS_ENTRY_STATE(entry)>=mbcsData->header.countStates) {
                fprintf(stderr, "error: state table entry [%x][%x] has a next state of %x that is too high\n",
                    state, cell, MBCS_ENTRY_STATE(entry));
                return FALSE;
            }
            if(MBCS_ENTRY_IS_FINAL(entry) && (mbcsData->stateFlags[MBCS_ENTRY_STATE(entry)]&0xf)!=MBCS_STATE_FLAG_DIRECT) {
                fprintf(stderr, "error: state table entry [%x][%x] is final but has a non-initial next state of %x\n",
                    state, cell, MBCS_ENTRY_STATE(entry));
                return FALSE;
            } else if(MBCS_ENTRY_IS_TRANSITION(entry) && (mbcsData->stateFlags[MBCS_ENTRY_STATE(entry)]&0xf)==MBCS_STATE_FLAG_DIRECT) {
                fprintf(stderr, "error: state table entry [%x][%x] is not final but has an initial next state of %x\n",
                    state, cell, MBCS_ENTRY_STATE(entry));
                return FALSE;
            }
        }
    }

    /* is this an SI/SO (like EBCDIC-stateful) state table? */
    if(mbcsData->header.countStates>=2 && (mbcsData->stateFlags[1]&0xf)==MBCS_STATE_FLAG_DIRECT) {
        if(mbcsData->maxCharLength!=2) {
            fprintf(stderr, "error: SI/SO codepages must have max 2 bytes/char (not %x)\n", mbcsData->maxCharLength);
            return FALSE;
        }
        if(mbcsData->header.countStates<3) {
            fprintf(stderr, "error: SI/SO codepages must have at least 3 states (not %x)\n", mbcsData->header.countStates);
            return FALSE;
        }
        /* are the SI/SO all in the right places? */
        if( mbcsData->stateTable[0][0xe]==MBCS_ENTRY_FINAL(1, MBCS_STATE_CHANGE_ONLY, 0) &&
            mbcsData->stateTable[0][0xf]==MBCS_ENTRY_FINAL(0, MBCS_STATE_CHANGE_ONLY, 0) &&
            mbcsData->stateTable[1][0xe]==MBCS_ENTRY_FINAL(1, MBCS_STATE_CHANGE_ONLY, 0) &&
            mbcsData->stateTable[1][0xf]==MBCS_ENTRY_FINAL(0, MBCS_STATE_CHANGE_ONLY, 0)
        ) {
            mbcsData->header.flags=MBCS_OUTPUT_2_SISO;
        } else {
            fprintf(stderr, "error: SI/SO codepages must have in states 0 and 1 transitions e:1.s, f:0.s\n");
            return FALSE;
        }
        state=2;
    } else {
        state=1;
    }

    /* check that no unexpected state is a "direct" one */
    while(state<(int)mbcsData->header.countStates) {
        if((mbcsData->stateFlags[state]&0xf)==MBCS_STATE_FLAG_DIRECT) {
            fprintf(stderr, "error: state %d is 'initial' - not supported except for SI/SO codepages\n", state);
            return FALSE;
        }
        ++state;
    }

    sum=sumUpStates(mbcsData);
    if(sum<0) {
        return FALSE;
    }

    /* allocate the code unit array and prefill it with "unassigned" values */
    if(sum>0) {
        mbcsData->unicodeCodeUnits=(uint16_t *)uprv_malloc(sum*sizeof(uint16_t));
        if(mbcsData->unicodeCodeUnits==NULL) {
            fprintf(stderr, "error: out of memory allocating %ld 16-bit code units\n",
                (long)sum);
            return FALSE;
        }
        for(i=0; i<sum; ++i) {
            mbcsData->unicodeCodeUnits[i]=0xfffe;
        }
    }

    /* allocate the codepage mappings and preset the first 16 characters to 0 */
    if(mbcsData->maxCharLength==1) {
        /* allocate 64k 16-bit results for single-byte codepages */
        sum=0x20000;
    } else {
        /* allocate 1M * maxCharLength bytes for at most 1M mappings */
        sum=0x100000*mbcsData->maxCharLength;
    }
    mbcsData->fromUBytes=(uint8_t *)uprv_malloc(sum);
    if(mbcsData->fromUBytes==NULL) {
        fprintf(stderr, "error: out of memory allocating %ldMB for target mappings\n",
            (long)sum);
        return FALSE;
    }
    /* initialize the all-unassigned first stage 3 block */
    uprv_memset(mbcsData->fromUBytes, 0, 64);

    return TRUE;
}

/* find a fallback for this offset; return the index or -1 if not found */
static int32_t
findFallback(MBCSData *mbcsData, uint32_t offset) {
    _MBCSToUFallback *toUFallbacks;
    int32_t i, limit;

    limit=mbcsData->header.countToUFallbacks;
    if(limit==0) {
        /* shortcut: most codepages do not have fallbacks from codepage to Unicode */
        return -1;
    }

    /* do a linear search for the fallback mapping (the table is not yet sorted) */
    toUFallbacks=mbcsData->toUFallbacks;
    for(i=0; i<limit; ++i) {
        if(offset==toUFallbacks[i].offset) {
            return i;
        }
    }
    return -1;
}

/* return TRUE for success */
static UBool
setFallback(MBCSData *mbcsData, uint32_t offset, UChar32 c) {
    int32_t i=findFallback(mbcsData, offset);
    if(i>=0) {
        /* if there is already a fallback for this offset, then overwrite it */
        mbcsData->toUFallbacks[i].codePoint=c;
        return TRUE;
    } else {
        /* if there is no fallback for this offset, then add one */
        i=mbcsData->header.countToUFallbacks;
        if(i>=MBCS_MAX_FALLBACK_COUNT) {
            fprintf(stderr, "error: too many toUnicode fallbacks, currently at: U+%x\n", c);
            return FALSE;
        } else {
            mbcsData->toUFallbacks[i].offset=offset;
            mbcsData->toUFallbacks[i].codePoint=c;
            mbcsData->header.countToUFallbacks=i+1;
            return TRUE;
        }
    }
}

/* remove fallback if there is one with this offset; return the code point if there was such a fallback, otherwise -1 */
static int32_t
removeFallback(MBCSData *mbcsData, uint32_t offset) {
    int32_t i=findFallback(mbcsData, offset);
    if(i>=0) {
        _MBCSToUFallback *toUFallbacks;
        int32_t limit, old;

        toUFallbacks=mbcsData->toUFallbacks;
        limit=mbcsData->header.countToUFallbacks;
        old=(int32_t)toUFallbacks[i].codePoint;

        /* copy the last fallback entry here to keep the list contiguous */
        toUFallbacks[i].offset=toUFallbacks[limit-1].offset;
        toUFallbacks[i].codePoint=toUFallbacks[limit-1].codePoint;
        mbcsData->header.countToUFallbacks=limit-1;
        return old;
    } else {
        return -1;
    }
}

/*
 * isFallback is almost a boolean:
 * 1 (TRUE)  this is a fallback mapping
 * 0 (FALSE) this is a precise mapping
 * -1        the precision of this mapping is not specified
 */
static UBool
MBCSAddToUnicode(NewConverter *cnvData,
                 const uint8_t *bytes, int32_t length,
                 UChar32 c, uint32_t b,
                 int8_t isFallback) {
    MBCSData *mbcsData=(MBCSData *)cnvData;
    uint32_t offset=0;
    int32_t i=0, entry, old;
    uint8_t state=0;

    if(mbcsData->header.countStates==0) {
        fprintf(stderr, "error: there is no state information!\n");
        return FALSE;
    }

    /* for SI/SO (like EBCDIC-stateful), double-byte sequences start in state 1 */
    if(length==2 && (mbcsData->header.flags&0xff)==MBCS_OUTPUT_2_SISO) {
        state=1;
    }

    /*
     * Walk down the state table like in conversion,
     * much like getNextUChar().
     * We assume that c<=0x10ffff.
     */
    for(i=0;;) {
        entry=mbcsData->stateTable[state][bytes[i++]];
        if(MBCS_ENTRY_IS_TRANSITION(entry)) {
            if(i==length) {
                fprintf(stderr, "error: byte sequence too short, ends in non-final state %hu: 0x%02lx (U+%x)\n",
                    state, (unsigned long)b, c);
                return FALSE;
            }
            state=(uint8_t)MBCS_ENTRY_TRANSITION_STATE(entry);
            offset+=MBCS_ENTRY_TRANSITION_OFFSET(entry);
        } else {
            if(i<length) {
                fprintf(stderr, "error: byte sequence too long by %d bytes, final state %hu: 0x%02lx (U+%x)\n",
                    (length-i), state, (unsigned long)b, c);
                return FALSE;
            }
            switch(MBCS_ENTRY_FINAL_ACTION(entry)) {
            case MBCS_STATE_ILLEGAL:
                fprintf(stderr, "error: byte sequence ends in illegal state at U+%04x<->0x%02lx\n",
                    c, (unsigned long)b);
                return FALSE;
            case MBCS_STATE_CHANGE_ONLY:
                fprintf(stderr, "error: byte sequence ends in state-change-only at U+%04x<->0x%02lx\n",
                    c, (unsigned long)b);
                return FALSE;
            case MBCS_STATE_UNASSIGNED:
                fprintf(stderr, "error: byte sequence ends in unassigned state at U+%04x<->0x%02lx\n",
                    c, (unsigned long)b);
                return FALSE;
            case MBCS_STATE_FALLBACK_DIRECT_16:
            case MBCS_STATE_VALID_DIRECT_16:
            case MBCS_STATE_FALLBACK_DIRECT_20:
            case MBCS_STATE_VALID_DIRECT_20:
                if(MBCS_ENTRY_SET_STATE(entry, 0)!=MBCS_ENTRY_FINAL(0, MBCS_STATE_VALID_DIRECT_16, 0xfffe)) {
                    /* the "direct" action's value is not "valid-direct-16-unassigned" any more */
                    if(MBCS_ENTRY_FINAL_ACTION(entry)==MBCS_STATE_VALID_DIRECT_16 || MBCS_ENTRY_FINAL_ACTION(entry)==MBCS_STATE_FALLBACK_DIRECT_16) {
                        old=MBCS_ENTRY_FINAL_VALUE(entry);
                    } else {
                        old=0x10000+MBCS_ENTRY_FINAL_VALUE(entry);
                    }
                    if(isFallback>=0) {
                        fprintf(stderr, "error: duplicate codepage byte sequence at U+%04x<->0x%02lx see U+%04lx\n",
                            c, (unsigned long)b, (long)old);
                        return FALSE;
                    } else if(VERBOSE) {
                        fprintf(stderr, "duplicate codepage byte sequence at U+%04x<->0x%02lx see U+%04lx\n",
                            c, (unsigned long)b, (long)old);
                    }
                    /*
                     * Continue after the above warning
                     * if the precision of the mapping is unspecified.
                     */
                }
                /* reassign the correct action code */
                entry=MBCS_ENTRY_FINAL_SET_ACTION(entry, (MBCS_STATE_VALID_DIRECT_16+(isFallback>0 ? 2 : 0)+(c>=0x10000 ? 1 : 0)));

                /* put the code point into bits 22..7 for BMP, c-0x10000 into 26..7 for others */
                if(c<=0xffff) {
                    entry=MBCS_ENTRY_FINAL_SET_VALUE(entry, c);
                } else {
                    entry=MBCS_ENTRY_FINAL_SET_VALUE(entry, c-0x10000);
                }
                mbcsData->stateTable[state][bytes[i-1]]=entry;
                break;
            case MBCS_STATE_VALID_16:
                /* bits 26..16 are not used, 0 */
                /* bits 15..7 contain the final offset delta to one 16-bit code unit */
                offset+=MBCS_ENTRY_FINAL_VALUE_16(entry);
                /* check that this byte sequence is still unassigned */
                if((old=mbcsData->unicodeCodeUnits[offset])!=0xfffe || (old=removeFallback(mbcsData, offset))!=-1) {
                    if(isFallback>=0) {
                        fprintf(stderr, "error: duplicate codepage byte sequence at U+%04x<->0x%02lx see U+%04lx\n",
                            c, (unsigned long)b, (long)old);
                        return FALSE;
                    } else if(VERBOSE) {
                        fprintf(stderr, "duplicate codepage byte sequence at U+%04x<->0x%02lx see U+%04lx\n",
                            c, (unsigned long)b, (long)old);
                    }
                }
                if(c>=0x10000) {
                    fprintf(stderr, "error: code point does not fit into valid-16-bit state at U+%04x<->0x%02lx\n",
                        c, (unsigned long)b);
                    return FALSE;
                }
                if(isFallback>0) {
                    /* assign only if there is no precise mapping */
                    if(mbcsData->unicodeCodeUnits[offset]==0xfffe) {
                        return setFallback(mbcsData, offset, c);
                    }
                } else {
                    mbcsData->unicodeCodeUnits[offset]=(uint16_t)c;
                }
                break;
            case MBCS_STATE_VALID_16_PAIR:
                /* bits 26..16 are not used, 0 */
                /* bits 15..7 contain the final offset delta to two 16-bit code units */
                offset+=MBCS_ENTRY_FINAL_VALUE_16(entry);
                /* check that this byte sequence is still unassigned */
                old=mbcsData->unicodeCodeUnits[offset];
                if(old<0xfffe) {
                    int32_t real;
                    if(old<0xd800) {
                        real=old;
                    } else if(old<=0xdfff) {
                        real=0x10000+((old&0x3ff)<<10)+((mbcsData->unicodeCodeUnits[offset+1])&0x3ff);
                    } else /* old<=0xe001 */ {
                        real=mbcsData->unicodeCodeUnits[offset+1];
                    }
                    if(isFallback>=0) {
                        fprintf(stderr, "error: duplicate codepage byte sequence at U+%04x<->0x%02lx see U+%04lx\n",
                            c, (unsigned long)b, (long)real);
                        return FALSE;
                    } else if(VERBOSE) {
                        fprintf(stderr, "duplicate codepage byte sequence at U+%04x<->0x%02lx see U+%04lx\n",
                            c, (unsigned long)b, (long)real);
                    }
                }
                if(isFallback>0) {
                    /* assign only if there is no precise mapping */
                    if(old<=0xdbff || old==0xe000) {
                        /* do nothing */
                    } else if(c<=0xffff) {
                        /* set a BMP fallback code point as a pair with 0xe001 */
                        mbcsData->unicodeCodeUnits[offset++]=0xe001;
                        mbcsData->unicodeCodeUnits[offset]=(uint16_t)c;
                    } else {
                        /* set a fallback surrogate pair with two second surrogates */
                        mbcsData->unicodeCodeUnits[offset++]=(uint16_t)(0xdbc0+(c>>10));
                        mbcsData->unicodeCodeUnits[offset]=(uint16_t)(0xdc00+(c&0x3ff));
                    }
                } else {
                    if(c<0xd800) {
                        /* set a BMP code point */
                        mbcsData->unicodeCodeUnits[offset]=(uint16_t)c;
                    } else if(c<=0xffff) {
                        /* set a BMP code point above 0xd800 as a pair with 0xe000 */
                        mbcsData->unicodeCodeUnits[offset++]=0xe000;
                        mbcsData->unicodeCodeUnits[offset]=(uint16_t)c;
                    } else {
                        /* set a surrogate pair */
                        mbcsData->unicodeCodeUnits[offset++]=(uint16_t)(0xd7c0+(c>>10));
                        mbcsData->unicodeCodeUnits[offset]=(uint16_t)(0xdc00+(c&0x3ff));
                    }
                }
                break;
            default:
                /* reserved, must never occur */
                fprintf(stderr, "internal error: byte sequence reached reserved action code, entry0x%02lx: 0x%02lx (U+%x)\n",
                    (unsigned long)entry, (unsigned long)b, c);
                return FALSE;
            }

            return TRUE;
        }
    }
}

/* is this byte sequence valid? (this is almost the same as MBCSAddToUnicode()) */
static UBool
MBCSIsValid(NewConverter *cnvData,
            const uint8_t *bytes, int32_t length,
            uint32_t b) {
    MBCSData *mbcsData=(MBCSData *)cnvData;
    uint32_t offset=0;
    int32_t i=0, entry;
    uint8_t state=0;

    if(mbcsData->header.countStates==0) {
        fprintf(stderr, "error: there is no state information!\n");
        return FALSE;
    }

    /* for SI/SO (like EBCDIC-stateful), double-byte sequences start in state 1 */
    if(length==2 && (mbcsData->header.flags&0xff)==MBCS_OUTPUT_2_SISO) {
        state=1;
    }

    /*
     * Walk down the state table like in conversion,
     * much like getNextUChar().
     * We assume that c<=0x10ffff.
     */
    for(i=0;;) {
        entry=mbcsData->stateTable[state][bytes[i++]];
        if(MBCS_ENTRY_IS_TRANSITION(entry)) {
            if(i==length) {
                fprintf(stderr, "error: byte sequence too short, ends in non-final state %hu: 0x%02lx\n",
                    state, (unsigned long)b);
                return FALSE;
            }
            state=(uint8_t)MBCS_ENTRY_TRANSITION_STATE(entry);
            offset+=MBCS_ENTRY_TRANSITION_OFFSET(entry);
        } else {
            if(i<length) {
                fprintf(stderr, "error: byte sequence too long by %d bytes, final state %hu: 0x%02lx\n",
                    (length-i), state, (unsigned long)b);
                return FALSE;
            }
            switch(MBCS_ENTRY_FINAL_ACTION(entry)) {
            case MBCS_STATE_ILLEGAL:
                fprintf(stderr, "error: byte sequence ends in illegal state: 0x%02lx\n",
                    (unsigned long)b);
                return FALSE;
            case MBCS_STATE_CHANGE_ONLY:
                fprintf(stderr, "error: byte sequence ends in state-change-only: 0x%02lx\n",
                    (unsigned long)b);
                return FALSE;
            case MBCS_STATE_UNASSIGNED:
                fprintf(stderr, "error: byte sequence ends in unassigned state: 0x%02lx\n",
                    (unsigned long)b);
                return FALSE;
            case MBCS_STATE_FALLBACK_DIRECT_16:
            case MBCS_STATE_VALID_DIRECT_16:
            case MBCS_STATE_FALLBACK_DIRECT_20:
            case MBCS_STATE_VALID_DIRECT_20:
            case MBCS_STATE_VALID_16:
            case MBCS_STATE_VALID_16_PAIR:
                return TRUE;
            default:
                /* reserved, must never occur */
                fprintf(stderr, "internal error: byte sequence reached reserved action code, entry0x%02lx: 0x%02lx\n",
                    (long)entry, (unsigned long)b);
                return FALSE;
            }
        }
    }
}

static UBool
MBCSSingleAddFromUnicode(NewConverter *cnvData,
                         const uint8_t *bytes, int32_t length,
                         UChar32 c, uint32_t b,
                         int8_t isFallback) {
    MBCSData *mbcsData=(MBCSData *)cnvData;
    uint16_t *p;
    uint32_t index;
    uint16_t old;

    /*
     * Walk down the triple-stage compact array ("trie") and
     * allocate parts as necessary.
     * Note that the first stage 2 and 3 blocks are reserved for all-unassigned mappings.
     * We assume that length<=maxCharLength and that c<=0x10ffff.
     */

    /* inspect stage 1 */
    index=c>>10;
    if(mbcsData->stage1[index]==MBCS_STAGE_2_ALL_UNASSIGNED_INDEX) {
        /* allocate another block in stage 2 */
        if(mbcsData->stage2Top>=MBCS_MAX_STAGE_2_TOP) {
            fprintf(stderr, "error: too many stage 2 entries at U+%04x<->0x%02lx\n",
                c, (unsigned long)b);
            return FALSE;
        }

        /*
         * each stage 2 block contains 64 16-bit words:
         * 6 code point bits 9..4 with 1 stage 3 index
         */
        mbcsData->stage1[index]=(uint16_t)mbcsData->stage2Top;
        mbcsData->stage2Top+=MBCS_STAGE_2_BLOCK_SIZE;
    }

    /* inspect stage 2 */
    index=(uint32_t)mbcsData->stage1[index]+((c>>4)&0x3f);
    if(mbcsData->stage2Single[index]==0) {
        /* allocate another block in stage 3 */
        if(mbcsData->stage3Top>=0x10000) {
            fprintf(stderr, "error: too many code points at U+%04x<->0x%02lx\n",
                c, (unsigned long)b);
            return FALSE;
        }
        /* each block has 16 uint16_t entries */
        mbcsData->stage2Single[index]=(uint16_t)mbcsData->stage3Top;
        uprv_memset(mbcsData->fromUBytes+2*mbcsData->stage3Top, 0, 32);
        mbcsData->stage3Top+=16;
    }

    /* write the codepage entry into stage 3 and get the previous entry */
    p=(uint16_t *)mbcsData->fromUBytes+mbcsData->stage2Single[index]+(c&0xf);
    old=*p;
    if(isFallback<=0) {
        *p=(uint16_t)(0xf00|b);
    } else if(IS_PRIVATE_USE(c)) {
        *p=(uint16_t)(0xc00|b);
    } else {
        *p=(uint16_t)(0x800|b);
    }

    /* check that this Unicode code point was still unassigned */
    if(old>=0x100) {
        if(isFallback>=0) {
            fprintf(stderr, "error: duplicate Unicode code point at U+%04x<->0x%02lx see 0x%02x\n",
                c, (unsigned long)b, old&0xff);
            return FALSE;
        } else if(VERBOSE) {
            fprintf(stderr, "duplicate Unicode code point at U+%04x<->0x%02lx see 0x%02x\n",
                c, (unsigned long)b, old&0xff);
        }
        /* continue after the above warning if the precision of the mapping is unspecified */
    }

    return TRUE;
}

static UBool
MBCSAddFromUnicode(NewConverter *cnvData,
                   const uint8_t *bytes, int32_t length,
                   UChar32 c, uint32_t b,
                   int8_t isFallback) {
    MBCSData *mbcsData=(MBCSData *)cnvData;
    uint8_t *p;
    uint32_t index, old;

    if( (mbcsData->header.flags&0xff)==MBCS_OUTPUT_2_SISO &&
        (*bytes==0xe || *bytes==0xf)
    ) {
        fprintf(stderr, "error: illegal mapping to SI or SO for SI/SO codepage: U+%04x<->0x%02lx\n",
            c, (unsigned long)b);
        return FALSE;
    }
    /*
     * Walk down the triple-stage compact array ("trie") and
     * allocate parts as necessary.
     * Note that the first stage 2 and 3 blocks are reserved for
     * all-unassigned mappings.
     * We assume that length<=maxCharLength and that c<=0x10ffff.
     */

    /* inspect stage 1 */
    index=c>>10;
    if(mbcsData->stage1[index]==MBCS_STAGE_2_ALL_UNASSIGNED_INDEX) {
        /* allocate another block in stage 2 */
        if(mbcsData->stage2Top>=MBCS_MAX_STAGE_2_TOP) {
            fprintf(stderr, "error: too many stage 2 entries at U+%04x<->0x%02lx\n",
                c, (unsigned long)b);
            return FALSE;
        }

        /*
         * each stage 2 block contains 64 32-bit words:
         * 6 code point bits 9..4 with value with bits 31..16 "assigned" flags and bits 15..0 stage 3 index
         */
        mbcsData->stage1[index]=(uint16_t)mbcsData->stage2Top;
        mbcsData->stage2Top+=MBCS_STAGE_2_BLOCK_SIZE;
    }

    /* inspect stage 2 */
    index=mbcsData->stage1[index]+((c>>4)&0x3f);
    if(mbcsData->stage2[index]==0) {
        /* allocate another block in stage 3 */
        if(mbcsData->stage3Top>=0x100000*mbcsData->maxCharLength) {
            fprintf(stderr, "error: too many code points at U+%04x<->0x%02lx\n",
                c, (unsigned long)b);
            return FALSE;
        }
        /* each block has 16*maxCharLength bytes */
        mbcsData->stage2[index]=(mbcsData->stage3Top/16)/mbcsData->maxCharLength;
        uprv_memset(mbcsData->fromUBytes+mbcsData->stage3Top, 0, 16*mbcsData->maxCharLength);
        mbcsData->stage3Top+=16*mbcsData->maxCharLength;
    }

    /* write the codepage bytes into stage 3 and get the previous bytes */
    old=0;
    p=mbcsData->fromUBytes+(16*(uint32_t)(uint16_t)mbcsData->stage2[index]+(c&0xf))*mbcsData->maxCharLength;
    switch(mbcsData->maxCharLength) {
    case 2:
        old=*(uint16_t *)p;
        *(uint16_t *)p=(uint16_t)b;
        break;
    case 3:
        old=(uint32_t)*p<<16;
        *p++=(uint8_t)(b>>16);
        old|=(uint32_t)*p<<8;
        *p++=(uint8_t)(b>>8);
        old|=*p;
        *p=(uint8_t)b;
        break;
    case 4:
        old=*(uint32_t *)p;
        *(uint32_t *)p=b;
        break;
    default:
        /* will never occur */
        break;
    }

    /* check that this Unicode code point was still unassigned */
    if((mbcsData->stage2[index]&(1UL<<(16+(c&0xf))))!=0 || old!=0) {
        if(isFallback>=0) {
            fprintf(stderr, "error: duplicate Unicode code point at U+%04x<->0x%02lx see 0x%02lx\n",
                c, (unsigned long)b, (unsigned long)old);
            return FALSE;
        } else if(VERBOSE) {
            fprintf(stderr, "duplicate Unicode code point at U+%04x<->0x%02lx see 0x%02lx\n",
                c, (unsigned long)b, (unsigned long)old);
        }
        /* continue after the above warning if the precision of the mapping is
           unspecified */
    }
    if(isFallback<=0) {
        /* set the "assigned" flag */
        mbcsData->stage2[index]|=(1UL<<(16+(c&0xf)));
    }

    return TRUE;
}

static int
compareFallbacks(const void *fb1, const void *fb2) {
    return ((const _MBCSToUFallback *)fb1)->offset-((const _MBCSToUFallback *)fb2)->offset;
}

/*
 * This function tries to compact toUnicode tables for 2-byte codepages
 * by finding lead bytes with all-unassigned trail bytes and adding another state
 * for them.
 */
static void
compactToUnicode2(MBCSData *mbcsData) {
    int32_t (*oldStateTable)[256];
    uint16_t count[256];
    uint16_t *oldUnicodeCodeUnits;
    int32_t entry, offset, oldOffset, trailOffset, oldTrailOffset, savings, sum;
    int32_t i, j, leadState, trailState, newState, fallback;
    uint16_t unit;

    /* find the lead state */
    if((mbcsData->header.flags&0xff)==MBCS_OUTPUT_2_SISO) {
        /* use the DBCS lead state for SI/SO codepages */
        leadState=1;
    } else {
        leadState=0;
    }

    /* find the main trail state: the most used target state */
    uprv_memset(count, 0, sizeof(count));
    for(i=0; i<256; ++i) {
        entry=mbcsData->stateTable[leadState][i];
        if(MBCS_ENTRY_IS_TRANSITION(entry)) {
            ++count[MBCS_ENTRY_TRANSITION_STATE(entry)];
        }
    }
    trailState=0;
    for(i=1; i<(int)mbcsData->header.countStates; ++i) {
        if(count[i]>count[trailState]) {
            trailState=i;
        }
    }

    /* count possible savings from lead bytes with all-unassigned results in all trail bytes */
    uprv_memset(count, 0, sizeof(count));
    savings=0;
    /* for each lead byte */
    for(i=0; i<256; ++i) {
        entry=mbcsData->stateTable[leadState][i];
        if(MBCS_ENTRY_IS_TRANSITION(entry) && (MBCS_ENTRY_TRANSITION_STATE(entry))==trailState) {
            /* the offset is different for each lead byte */
            offset=MBCS_ENTRY_TRANSITION_OFFSET(entry);
            /* for each trail byte for this lead byte */
            for(j=0; j<256; ++j) {
                entry=mbcsData->stateTable[trailState][j];
                switch(MBCS_ENTRY_FINAL_ACTION(entry)) {
                case MBCS_STATE_VALID_16:
                    entry=offset+MBCS_ENTRY_FINAL_VALUE_16(entry);
                    if(mbcsData->unicodeCodeUnits[entry]==0xfffe && findFallback(mbcsData, entry)<0) {
                        ++count[i];
                    } else {
                        j=999; /* do not count for this lead byte because there are assignments */
                    }
                    break;
                case MBCS_STATE_VALID_16_PAIR:
                    entry=offset+MBCS_ENTRY_FINAL_VALUE_16(entry);
                    if(mbcsData->unicodeCodeUnits[entry]==0xfffe) {
                        count[i]+=2;
                    } else {
                        j=999; /* do not count for this lead byte because there are assignments */
                    }
                    break;
                default:
                    break;
                }
            }
            if(j==256) {
                /* all trail bytes for this lead byte are unassigned */
                savings+=count[i];
            } else {
                count[i]=0;
            }
        }
    }
    /* subtract from the possible savings the cost of an additional state */
    savings=savings*2-1024; /* count bytes, not 16-bit words */
    if(savings<=0) {
        return;
    }
    if(VERBOSE) {
        printf("compacting toUnicode data saves %ld bytes\n", (long)savings);
    }
    if(mbcsData->header.countStates>=MBCS_MAX_STATE_COUNT) {
        fprintf(stderr, "cannot compact toUnicode because the maximum number of states is reached\n");
        return;
    }

    /* make a copy of the state table */
    oldStateTable=(int32_t (*)[256])uprv_malloc(mbcsData->header.countStates*1024);
    if(oldStateTable==NULL) {
        fprintf(stderr, "cannot compact toUnicode: out of memory\n");
        return;
    }
    uprv_memcpy(oldStateTable, mbcsData->stateTable, mbcsData->header.countStates*1024);

    /* add the new state */
    /*
     * this function does not catch the degenerate case where all lead bytes
     * have all-unassigned trail bytes and the lead state could be removed
     */
    newState=mbcsData->header.countStates++;
    mbcsData->stateFlags[newState]=0;
    /* copy the old trail state, turning all assigned states into unassigned ones */
    for(i=0; i<256; ++i) {
        entry=mbcsData->stateTable[trailState][i];
        switch(MBCS_ENTRY_FINAL_ACTION(entry)) {
        case MBCS_STATE_VALID_16:
        case MBCS_STATE_VALID_16_PAIR:
            mbcsData->stateTable[newState][i]=MBCS_ENTRY_FINAL_SET_ACTION_VALUE(entry, MBCS_STATE_UNASSIGNED, 0xfffe);
            break;
        default:
            mbcsData->stateTable[newState][i]=entry;
            break;
        }
    }

    /* in the lead state, redirect all lead bytes with all-unassigned trail bytes to the new state */
    for(i=0; i<256; ++i) {
        if(count[i]>0) {
            mbcsData->stateTable[leadState][i]=MBCS_ENTRY_SET_STATE(mbcsData->stateTable[leadState][i], newState);
        }
    }

    /* sum up the new state table */
    for(i=0; i<(int)mbcsData->header.countStates; ++i) {
        mbcsData->stateFlags[i]&=~MBCS_STATE_FLAG_READY;
    }
    sum=sumUpStates(mbcsData);

    /* allocate a new, smaller code units array */
    oldUnicodeCodeUnits=mbcsData->unicodeCodeUnits;
    if(sum==0) {
        mbcsData->unicodeCodeUnits=NULL;
        if(oldUnicodeCodeUnits!=NULL) {
            uprv_free(oldUnicodeCodeUnits);
        }
        uprv_free(oldStateTable);
        return;
    }
    mbcsData->unicodeCodeUnits=(uint16_t *)uprv_malloc(sum*sizeof(uint16_t));
    if(mbcsData->unicodeCodeUnits==NULL) {
        fprintf(stderr, "cannot compact toUnicode: out of memory allocating %ld 16-bit code units\n",
            (long)sum);
        /* revert to the old state table */
        mbcsData->unicodeCodeUnits=oldUnicodeCodeUnits;
        --mbcsData->header.countStates;
        uprv_memcpy(mbcsData->stateTable, oldStateTable, mbcsData->header.countStates*1024);
        uprv_free(oldStateTable);
        return;
    }
    for(i=0; i<sum; ++i) {
        mbcsData->unicodeCodeUnits[i]=0xfffe;
    }

    /* copy the code units for all assigned characters */
    /*
     * The old state table has the same lead _and_ trail states for assigned characters!
     * The differences are in the offsets, and in the trail states for some unassigned characters.
     * For each character with an assigned state in the new table, it was assigned in the old one.
     * Only still-assigned characters are copied.
     * Note that fallback mappings need to get their offset values adjusted.
     */

    /* for each initial state */
    for(leadState=0; leadState<(int)mbcsData->header.countStates; ++leadState) {
        if((mbcsData->stateFlags[leadState]&0xf)==MBCS_STATE_FLAG_DIRECT) {
            /* for each lead byte from there */
            for(i=0; i<256; ++i) {
                entry=mbcsData->stateTable[leadState][i];
                if(MBCS_ENTRY_IS_TRANSITION(entry)) {
                    trailState=(uint8_t)MBCS_ENTRY_TRANSITION_STATE(entry);
                    /* the new state does not have assigned states */
                    if(trailState!=newState) {
                        trailOffset=MBCS_ENTRY_TRANSITION_OFFSET(entry);
                        oldTrailOffset=MBCS_ENTRY_TRANSITION_OFFSET(oldStateTable[leadState][i]);
                        /* for each trail byte */
                        for(j=0; j<256; ++j) {
                            entry=mbcsData->stateTable[trailState][j];
                            /* copy assigned-character code units and adjust fallback offsets */
                            switch(MBCS_ENTRY_FINAL_ACTION(entry)) {
                            case MBCS_STATE_VALID_16:
                                offset=trailOffset+MBCS_ENTRY_FINAL_VALUE_16(entry);
                                /* find the old offset according to the old state table */
                                oldOffset=oldTrailOffset+MBCS_ENTRY_FINAL_VALUE_16(oldStateTable[trailState][j]);
                                unit=mbcsData->unicodeCodeUnits[offset]=oldUnicodeCodeUnits[oldOffset];
                                if(unit==0xfffe && (fallback=findFallback(mbcsData, oldOffset))>=0) {
                                    mbcsData->toUFallbacks[fallback].offset=0x80000000|offset;
                                }
                                break;
                            case MBCS_STATE_VALID_16_PAIR:
                                offset=trailOffset+MBCS_ENTRY_FINAL_VALUE_16(entry);
                                /* find the old offset according to the old state table */
                                oldOffset=oldTrailOffset+MBCS_ENTRY_FINAL_VALUE_16(oldStateTable[trailState][j]);
                                mbcsData->unicodeCodeUnits[offset++]=oldUnicodeCodeUnits[oldOffset++];
                                mbcsData->unicodeCodeUnits[offset]=oldUnicodeCodeUnits[oldOffset];
                                break;
                            default:
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /* remove temporary flags from fallback offsets that protected them from being modified twice */
    sum=mbcsData->header.countToUFallbacks;
    for(i=0; i<sum; ++i) {
        mbcsData->toUFallbacks[i].offset&=0x7fffffff;
    }

    /* free temporary memory */
    uprv_free(oldUnicodeCodeUnits);
    uprv_free(oldStateTable);
}

/*
 * recursive sub-function of compactToUnicodeHelper()
 * returns:
 * >0 number of bytes that are used in unicodeCodeUnits[] that could be saved,
 *    if all sequences from this state are unassigned, returns the
 * <0 there are assignments in unicodeCodeUnits[]
 * 0  no use of unicodeCodeUnits[]
 */
static int32_t
findUnassigned(MBCSData *mbcsData, int32_t state, int32_t offset, uint32_t b) {
    int32_t i, entry, savings, localSavings, belowSavings;
    UBool haveAssigned;

    localSavings=belowSavings=0;
    haveAssigned=FALSE;
    for(i=0; i<256; ++i) {
        entry=mbcsData->stateTable[state][i];
        if(MBCS_ENTRY_IS_TRANSITION(entry)) {
            savings=findUnassigned(mbcsData, MBCS_ENTRY_TRANSITION_STATE(entry), offset+MBCS_ENTRY_TRANSITION_OFFSET(entry), (b<<8)|(uint32_t)i);
            if(savings<0) {
                haveAssigned=TRUE;
            } else if(savings>0) {
                printf("    all-unassigned sequences from prefix 0x%02lx state %ld use %ld bytes\n",
                    (unsigned long)((b<<8)|i), (long)state, (long)savings);
                belowSavings+=savings;
            }
        } else if(!haveAssigned) {
            switch(MBCS_ENTRY_FINAL_ACTION(entry)) {
            case MBCS_STATE_VALID_16:
                entry=offset+MBCS_ENTRY_FINAL_VALUE_16(entry);
                if(mbcsData->unicodeCodeUnits[entry]==0xfffe && findFallback(mbcsData, entry)<0) {
                    localSavings+=2;
                } else {
                    haveAssigned=TRUE;
                }
                break;
            case MBCS_STATE_VALID_16_PAIR:
                entry=offset+MBCS_ENTRY_FINAL_VALUE_16(entry);
                if(mbcsData->unicodeCodeUnits[entry]==0xfffe) {
                    localSavings+=4;
                } else {
                    haveAssigned=TRUE;
                }
                break;
            default:
                break;
            }
        }
    }
    if(haveAssigned) {
        return -1;
    } else {
        return localSavings+belowSavings;
    }
}

/* helper function for finding compaction opportunities */
static void
compactToUnicodeHelper(MBCSData *mbcsData) {
    int32_t state, savings;

    if(!VERBOSE) {
        return;
    }

    /* for each initial state */
    for(state=0; state<(int)mbcsData->header.countStates; ++state) {
        if((mbcsData->stateFlags[state]&0xf)==MBCS_STATE_FLAG_DIRECT) {
            savings=findUnassigned(mbcsData, state, 0, 0);
            if(savings>0) {
                printf("    all-unassigned sequences from initial state %ld use %ld bytes\n",
                    (long)state, (long)savings);
            }
        }
    }
}

static UBool
transformEUC(MBCSData *mbcsData) {
    uint8_t *p8;
    uint32_t i, value, oldLength=mbcsData->maxCharLength, old3Top=mbcsData->stage3Top, new3Top;
    uint8_t b;

    if(oldLength<3) {
        return FALSE;
    }

    /* careful: 2-byte and 4-byte codes are stored in platform endianness! */

    /* test if all first bytes are in {0, 0x8e, 0x8f} */
    p8=mbcsData->fromUBytes;

#if !U_IS_BIG_ENDIAN
    if(oldLength==4) {
        p8+=3;
    }
#endif

    for(i=0; i<old3Top; i+=oldLength) {
        b=p8[i];
        if(b!=0 && b!=0x8e && b!=0x8f) {
            /* some first byte does not fit the EUC pattern, nothing to be done */
            return FALSE;
        }
    }
    /* restore p if it was modified above */
    p8=mbcsData->fromUBytes;

    /* modify outputType and adjust stage3Top */
    mbcsData->header.flags=MBCS_OUTPUT_3_EUC+oldLength-3;
    mbcsData->stage3Top=new3Top=(old3Top*(oldLength-1))/oldLength;

    /*
     * EUC-encode all byte sequences;
     * see "CJKV Information Processing" (1st ed. 1999) from Ken Lunde, O'Reilly,
     * p. 161 in chapter 4 "Encoding Methods"
     *
     * This also must reverse the byte order if the platform is little-endian!
     */
    if(oldLength==3) {
        uint16_t *q=(uint16_t *)p8;
        for(i=0; i<old3Top; i+=oldLength) {
            b=*p8;
            if(b==0) {
                /* short sequences are stored directly */
                /* code set 0 or 1 */
                (*q++)=(uint16_t)((p8[1]<<8)|p8[2]);
            } else if(b==0x8e) {
                /* code set 2 */
                (*q++)=(uint16_t)(((p8[1]&0x7f)<<8)|p8[2]);
            } else /* b==0x8f */ {
                /* code set 3 */
                (*q++)=(uint16_t)((p8[1]<<8)|(p8[2]&0x7f));
            }
            p8+=3;
        }
    } else /* oldLength==4 */ {
        uint8_t *q=p8;
        uint32_t *p32=(uint32_t *)p8;
        for(i=0; i<old3Top; i+=4) {
            value=(*p32++);
            if(value<=0xffffff) {
                /* short sequences are stored directly */
                /* code set 0 or 1 */
                (*q++)=(uint8_t)(value>>16);
                (*q++)=(uint8_t)(value>>8);
                (*q++)=(uint8_t)value;
            } else if(value<=0x8effffff) {
                /* code set 2 */
                (*q++)=(uint8_t)((value>>16)&0x7f);
                (*q++)=(uint8_t)(value>>8);
                (*q++)=(uint8_t)value;
            } else /* first byte is 0x8f */ {
                /* code set 3 */
                (*q++)=(uint8_t)(value>>16);
                (*q++)=(uint8_t)((value>>8)&0x7f);
                (*q++)=(uint8_t)value;
            }
        }
    }

    return TRUE;
}

/*
 * Compact stage 2 for SBCS by overlapping adjacent stage 2 blocks as far
 * as possible. Overlapping is done on unassigned head and tail
 * parts of blocks in steps of MBCS_STAGE_2_MULTIPLIER.
 * Stage 1 indexes need to be adjusted accordingly.
 * This function is very similar to genprops/store.c/compactStage().
 */
static void
singleCompactStage2(MBCSData *mbcsData) {
    /* this array maps the ordinal number of a stage 2 block to its new stage 1 index */
    uint16_t map[MBCS_STAGE_2_MAX_BLOCKS];
    uint16_t i, start, prevEnd, newStart;

    /* enter the all-unassigned first stage 2 block into the map */
    map[0]=MBCS_STAGE_2_ALL_UNASSIGNED_INDEX;

    /* begin with the first block after the all-unassigned one */
    start=newStart=MBCS_STAGE_2_FIRST_ASSIGNED;
    while(start<mbcsData->stage2Top) {
        prevEnd=(uint16_t)(newStart-1);

        /* find the size of the overlap */
        for(i=0; i<MBCS_STAGE_2_BLOCK_SIZE && mbcsData->stage2Single[start+i]==0 && mbcsData->stage2Single[prevEnd-i]==0; ++i) {}

        if(i>0) {
            map[start>>MBCS_STAGE_2_BLOCK_SIZE_SHIFT]=(uint16_t)(newStart-i);

            /* move the non-overlapping indexes to their new positions */
            start+=i;
            for(i=(uint16_t)(MBCS_STAGE_2_BLOCK_SIZE-i); i>0; --i) {
                mbcsData->stage2Single[newStart++]=mbcsData->stage2Single[start++];
            }
        } else if(newStart<start) {
            /* move the indexes to their new positions */
            map[start>>MBCS_STAGE_2_BLOCK_SIZE_SHIFT]=newStart;
            for(i=MBCS_STAGE_2_BLOCK_SIZE; i>0; --i) {
                mbcsData->stage2Single[newStart++]=mbcsData->stage2Single[start++];
            }
        } else /* no overlap && newStart==start */ {
            map[start>>MBCS_STAGE_2_BLOCK_SIZE_SHIFT]=start;
            start=newStart+=MBCS_STAGE_2_BLOCK_SIZE;
        }
    }

    /* adjust stage2Top */
    if(VERBOSE && newStart<mbcsData->stage2Top) {
        printf("compacting stage 2 from stage2Top=0x%lx to 0x%lx, saving %ld bytes\n",
                (unsigned long)mbcsData->stage2Top, (unsigned long)newStart,
                (long)(mbcsData->stage2Top-newStart)*2);
    }
    mbcsData->stage2Top=newStart;

    /* now adjust stage 1 */
    for(i=0; i<MBCS_STAGE_1_SIZE; ++i) {
        mbcsData->stage1[i]=map[mbcsData->stage1[i]>>MBCS_STAGE_2_BLOCK_SIZE_SHIFT];
    }
}

/* Compact stage 3 for SBCS - same algorithm as above. */
static void
singleCompactStage3(MBCSData *mbcsData) {
    uint16_t *stage3=(uint16_t *)mbcsData->fromUBytes;

    /* this array maps the ordinal number of a stage 3 block to its new stage 2 index */
    uint16_t map[0x1000];
    uint16_t i, start, prevEnd, newStart;

    /* enter the all-unassigned first stage 3 block into the map */
    map[0]=0;

    /* begin with the first block after the all-unassigned one */
    start=newStart=16;
    while(start<mbcsData->stage3Top) {
        prevEnd=(uint16_t)(newStart-1);

        /* find the size of the overlap */
        for(i=0; i<16 && stage3[start+i]==0 && stage3[prevEnd-i]==0; ++i) {}

        if(i>0) {
            map[start>>4]=(uint16_t)(newStart-i);

            /* move the non-overlapping indexes to their new positions */
            start+=i;
            for(i=(uint16_t)(16-i); i>0; --i) {
                stage3[newStart++]=stage3[start++];
            }
        } else if(newStart<start) {
            /* move the indexes to their new positions */
            map[start>>4]=newStart;
            for(i=16; i>0; --i) {
                stage3[newStart++]=stage3[start++];
            }
        } else /* no overlap && newStart==start */ {
            map[start>>4]=start;
            start=newStart+=16;
        }
    }

    /* adjust stage3Top */
    if(VERBOSE && newStart<mbcsData->stage3Top) {
        printf("compacting stage 3 from stage3Top=0x%lx to 0x%lx, saving %ld bytes\n",
                (unsigned long)mbcsData->stage3Top, (unsigned long)newStart,
                (long)(mbcsData->stage3Top-newStart)*2);
    }
    mbcsData->stage3Top=newStart;

    /* now adjust stage 2 */
    for(i=0; i<mbcsData->stage2Top; ++i) {
        mbcsData->stage2Single[i]=map[mbcsData->stage2Single[i]>>4];
    }
}

/*
 * Compact stage 2 by overlapping adjacent stage 2 blocks as far
 * as possible. Overlapping is done on unassigned head and tail
 * parts of blocks in steps of MBCS_STAGE_2_MULTIPLIER.
 * Stage 1 indexes need to be adjusted accordingly.
 * This function is very similar to genprops/store.c/compactStage().
 */
static void
compactStage2(MBCSData *mbcsData) {
    /* this array maps the ordinal number of a stage 2 block to its new stage 1 index */
    uint16_t map[MBCS_STAGE_2_MAX_BLOCKS];
    uint16_t i, start, prevEnd, newStart;

    /* enter the all-unassigned first stage 2 block into the map */
    map[0]=MBCS_STAGE_2_ALL_UNASSIGNED_INDEX;

    /* begin with the first block after the all-unassigned one */
    start=newStart=MBCS_STAGE_2_FIRST_ASSIGNED;
    while(start<mbcsData->stage2Top) {
        prevEnd=(uint16_t)(newStart-1);

        /* find the size of the overlap */
        for(i=0; i<MBCS_STAGE_2_BLOCK_SIZE && mbcsData->stage2[start+i]==0 && mbcsData->stage2[prevEnd-i]==0; ++i) {}

        if(i>0) {
            map[start>>MBCS_STAGE_2_BLOCK_SIZE_SHIFT]=(uint16_t)(newStart-i);

            /* move the non-overlapping indexes to their new positions */
            start+=i;
            for(i=(uint16_t)(MBCS_STAGE_2_BLOCK_SIZE-i); i>0; --i) {
                mbcsData->stage2[newStart++]=mbcsData->stage2[start++];
            }
        } else if(newStart<start) {
            /* move the indexes to their new positions */
            map[start>>MBCS_STAGE_2_BLOCK_SIZE_SHIFT]=newStart;
            for(i=MBCS_STAGE_2_BLOCK_SIZE; i>0; --i) {
                mbcsData->stage2[newStart++]=mbcsData->stage2[start++];
            }
        } else /* no overlap && newStart==start */ {
            map[start>>MBCS_STAGE_2_BLOCK_SIZE_SHIFT]=start;
            start=newStart+=MBCS_STAGE_2_BLOCK_SIZE;
        }
    }

    /* adjust stage2Top */
    if(VERBOSE && newStart<mbcsData->stage2Top) {
        printf("compacting stage 2 from stage2Top=0x%lx to 0x%lx, saving %ld bytes\n",
                (unsigned long)mbcsData->stage2Top, (unsigned long)newStart,
                (long)(mbcsData->stage2Top-newStart)*4);
    }
    mbcsData->stage2Top=newStart;

    /* now adjust stage 1 */
    for(i=0; i<MBCS_STAGE_1_SIZE; ++i) {
        mbcsData->stage1[i]=map[mbcsData->stage1[i]>>MBCS_STAGE_2_BLOCK_SIZE_SHIFT];
    }
}

static void
MBCSPostprocess(NewConverter *cnvData, const UConverterStaticData *staticData) {
    MBCSData *mbcsData=(MBCSData *)cnvData;
    int32_t entry;
    int state, cell;

    /* this needs to be printed before the EUC transformation because later maxCharLength might not be correct */
    if(VERBOSE) {
        printf("number of codepage characters in 16-blocks: 0x%lx=%lu\n",
               (unsigned long)mbcsData->stage3Top/mbcsData->maxCharLength,
               (unsigned long)mbcsData->stage3Top/mbcsData->maxCharLength);
    }

    /* test each state table entry */
    for(state=0; state<(int)mbcsData->header.countStates; ++state) {
        for(cell=0; cell<256; ++cell) {
            entry=mbcsData->stateTable[state][cell];
            /*
             * if the entry is a final one with an MBCS_STATE_VALID_DIRECT_16 action code
             * and the code point is "unassigned" (0xfffe), then change it to
             * the "unassigned" action code with bits 26..23 set to zero and U+fffe.
             */
            if(MBCS_ENTRY_SET_STATE(entry, 0)==MBCS_ENTRY_FINAL(0, MBCS_STATE_VALID_DIRECT_16, 0xfffe)) {
                mbcsData->stateTable[state][cell]=MBCS_ENTRY_FINAL_SET_ACTION(entry, MBCS_STATE_UNASSIGNED);
            }
        }
    }

    /* try to compact the toUnicode tables */
    if(mbcsData->maxCharLength==2) {
        compactToUnicode2(mbcsData);
    } else if(mbcsData->maxCharLength>2) {
        compactToUnicodeHelper(mbcsData);
    }

    /* sort toUFallbacks */
    /*
     * It should be safe to sort them before compactToUnicode2() is called,
     * because it should not change the relative order of the offset values
     * that it adjusts, but they need to be sorted at some point, and
     * it is safest here.
     */
    if(mbcsData->header.countToUFallbacks>0) {
        qsort(mbcsData->toUFallbacks, mbcsData->header.countToUFallbacks, sizeof(_MBCSToUFallback), compareFallbacks);
    }

    /* try to compact the fromUnicode tables */
    transformEUC(mbcsData);
    if(mbcsData->maxCharLength==1) {
        singleCompactStage3(mbcsData);
        singleCompactStage2(mbcsData);
    } else {
        compactStage2(mbcsData);
    }
}

static uint32_t
MBCSWrite(NewConverter *cnvData, const UConverterStaticData *staticData, UNewDataMemory *pData) {
    MBCSData *mbcsData=(MBCSData *)cnvData;
    int32_t i, stage1Top;

    /* adjust stage 1 entries to include the size of stage 1 in the offsets to stage 2 */
    if(mbcsData->maxCharLength==1) {
        if(staticData->unicodeMask&UCNV_HAS_SUPPLEMENTARY) {
            stage1Top=MBCS_STAGE_1_SIZE; /* 0x440==1088 */
        } else {
            stage1Top=0x40; /* 0x40==64 */
        }
        for(i=0; i<stage1Top; ++i) {
            mbcsData->stage1[i]+=(uint16_t)stage1Top;
        }

        /* stage2Top has counted 16-bit results, now we need to count bytes */
        mbcsData->stage2Top*=2;

        /* stage3Top has counted 16-bit results, now we need to count bytes */
        mbcsData->stage3Top*=2;
    } else {
        if(staticData->unicodeMask&UCNV_HAS_SUPPLEMENTARY) {
            stage1Top=MBCS_STAGE_1_SIZE; /* 0x440==1088 */
        } else {
            stage1Top=0x40; /* 0x40==64 */
        }
        for(i=0; i<stage1Top; ++i) {
            mbcsData->stage1[i]+=(uint16_t)stage1Top/2; /* stage 2 contains 32-bit entries, stage 1 16-bit entries */
        }

        /* stage2Top has counted 32-bit results, now we need to count bytes */
        mbcsData->stage2Top*=4;

        /* stage3Top has already counted bytes */
    }

    /* round up stage2Top and stage3Top so that the sizes of all data blocks are multiples of 4 */
    mbcsData->stage2Top=(mbcsData->stage2Top+3)&~3;
    mbcsData->stage3Top=(mbcsData->stage3Top+3)&~3;

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
        stage1Top*2+
        mbcsData->stage2Top;
    mbcsData->header.fromUBytesLength=mbcsData->stage3Top;

    /* write the MBCS data */
    udata_writeBlock(pData, &mbcsData->header, sizeof(_MBCSHeader));
    udata_writeBlock(pData, mbcsData->stateTable, mbcsData->header.countStates*1024);
    udata_writeBlock(pData, mbcsData->toUFallbacks, mbcsData->header.countToUFallbacks*sizeof(_MBCSToUFallback));
    udata_writeBlock(pData, mbcsData->unicodeCodeUnits, mbcsData->countToUCodeUnits*2);
    udata_writeBlock(pData, mbcsData->stage1, stage1Top*2);
    if(mbcsData->maxCharLength==1) {
        udata_writeBlock(pData, mbcsData->stage2Single, mbcsData->stage2Top);
    } else {
        udata_writeBlock(pData, mbcsData->stage2, mbcsData->stage2Top);
    }
    udata_writeBlock(pData, mbcsData->fromUBytes, mbcsData->stage3Top);

    /* return the number of bytes that should have been written */
    return mbcsData->header.offsetFromUBytes+mbcsData->header.fromUBytesLength;
}
