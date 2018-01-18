// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// utrie3_builder.cpp (modified from utrie2_builder.cpp)
// created: 2017dec29 Markus W. Scherer

#define UTRIE3_DEBUG  // TODO
#ifdef UTRIE3_DEBUG
#   include <stdio.h>
#endif

#include "unicode/utypes.h"
#include "cmemory.h"
#include "uassert.h"
#include "utrie3.h"
#include "utrie3_impl.h"

#include "utrie.h" /* for utrie3_fromUTrie() and utrie_swap() */

using icu::LocalMemory;

/* Implementation notes ----------------------------------------------------- */

/*
 * The UTRIE3_SHIFT_1, UTRIE3_SHIFT_2, UTRIE3_INDEX_SHIFT and other values
 * have been chosen to minimize trie sizes overall.
 * TODO: Now chosen as a compromise between size (if it increases overall) and simpler UTF-8 code.
 * Most of the code is flexible enough to work with a range of values,
 * within certain limits.
 *
 * Requires UTRIE3_SHIFT_2<=7. Otherwise 0x80 which is the top of the ASCII-linear data
 * is not a multiple of UTRIE3_DATA_BLOCK_LENGTH
 * and map[block>>UTRIE3_SHIFT_2] (used in reference counting and compaction remapping)
 * stops working.
 *
 * Requires UTRIE3_SHIFT_1>=10 because utrie3_enumForLeadSurrogate()
 * assumes that a single index-2 block is used for 0x400 code points
 * corresponding to one lead surrogate.
 *
 * Requires UTRIE3_SHIFT_1<=16. Otherwise one single index-2 block contains
 * more than one Unicode plane, and the split of the index-2 table into a BMP
 * part and a supplementary part, with a gap in between, would not work.
 *
 * Requires UTRIE3_INDEX_SHIFT>=1 not because of the code but because
 * there is data with more than 64k distinct values,
 * for example for Unihan collation with a separate collation weight per
 * Han character.
 */

/* Building a trie ----------------------------------------------------------*/

enum {
    /** The null index-2 block, following the gap in the index-2 table. */
    UNEWTRIE3_INDEX_2_NULL_OFFSET=UNEWTRIE3_INDEX_GAP_OFFSET+UNEWTRIE3_INDEX_GAP_LENGTH,

    /** The start of allocated index-2 blocks. */
    UNEWTRIE3_INDEX_2_START_OFFSET=UNEWTRIE3_INDEX_2_NULL_OFFSET+UTRIE3_INDEX_2_BLOCK_LENGTH,

    /** The null data block. */
    UNEWTRIE3_DATA_NULL_OFFSET=UTRIE3_DATA_START_OFFSET,

    /** The start of allocated data blocks. */
    UNEWTRIE3_DATA_START_OFFSET=UNEWTRIE3_DATA_NULL_OFFSET+UTRIE3_DATA_BLOCK_LENGTH
};

/* Start with allocation of 16k data entries. */
constexpr int32_t UNEWTRIE3_INITIAL_DATA_LENGTH=((int32_t)1<<14);

/* Grow about 8x each time. */
constexpr int32_t UNEWTRIE3_MEDIUM_DATA_LENGTH=((int32_t)1<<17);

constexpr int32_t MAX_UNICODE=0x10ffff;

constexpr int32_t UNICODE_LIMIT=0x110000;
constexpr int32_t BMP_LIMIT=0x10000;
constexpr int32_t ASCII_LIMIT=0x80;

//nstexpr int32_t I_LIMIT=UNICODE_LIMIT>>UTRIE3_SHIFT_2;
constexpr int32_t BMP_I_LIMIT=BMP_LIMIT>>UTRIE3_SHIFT_2;  // TODO: = UTRIE3_INDEX_2_BMP_LENGTH?
constexpr int32_t ASCII_I_LIMIT=ASCII_LIMIT>>UTRIE3_SHIFT_2;

constexpr uint8_t ALL_SAME=0;
constexpr uint8_t MIXED=1;
constexpr uint8_t SAME_AS=2;
constexpr uint8_t MOVED=3;
constexpr uint8_t TYPE_MASK=3;

/**
 * Added to an ALL_SAME or MIXED block during compaction if a supplementary block
 * has the same data.
 */
constexpr uint8_t SUPP_DATA=0x10;

U_CAPI UTrie3 * U_EXPORT2
utrie3_open(uint32_t initialValue, uint32_t errorValue, UErrorCode *pErrorCode) {
    UTrie3 *trie;
    UNewTrie3 *newTrie;
    uint32_t *data;

    if(U_FAILURE(*pErrorCode)) {
        return NULL;
    }

    trie=(UTrie3 *)uprv_malloc(sizeof(UTrie3));
    newTrie=(UNewTrie3 *)uprv_malloc(sizeof(UNewTrie3));
    data=(uint32_t *)uprv_malloc(UNEWTRIE3_INITIAL_DATA_LENGTH*4);
    if(trie==NULL || newTrie==NULL || data==NULL) {
        uprv_free(trie);
        uprv_free(newTrie);
        uprv_free(data);
        *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }

    uprv_memset(trie, 0, sizeof(UTrie3));
    trie->index2NullOffset = UTRIE3_NO_INDEX2_NULL_OFFSET;
    trie->dataNullOffset = UTRIE3_NO_DATA_NULL_OFFSET;
    trie->initialValue=initialValue;
    trie->errorValue=errorValue;
    trie->highValue=initialValue;
    trie->newTrie=newTrie;
    trie->name="open";

    newTrie->data=data;
    newTrie->dataCapacity=UNEWTRIE3_INITIAL_DATA_LENGTH;
    newTrie->dataLength=0;
    newTrie->dataNullIndex = -1;

    return trie;
}

static UNewTrie3 *
cloneBuilder(const UNewTrie3 *other, int32_t highStart) {
    UNewTrie3 *newTrie=(UNewTrie3 *)uprv_malloc(sizeof(UNewTrie3));
    if(newTrie==NULL) {
        return NULL;
    }
    uprv_memcpy(newTrie, other, sizeof(*newTrie));

    newTrie->data=(uint32_t *)uprv_malloc(other->dataCapacity*4);
    if(newTrie->data==NULL) {
        uprv_free(newTrie);
        return NULL;
    }

    uprv_memcpy(newTrie->data, other->data, (size_t)other->dataLength*4);
    newTrie->dataCapacity = other->dataCapacity;
    newTrie->dataLength = other->dataLength;
    newTrie->dataNullIndex = other->dataNullIndex;

    int32_t iLimit=highStart>>UTRIE3_SHIFT_2;
    uprv_memcpy(newTrie->flags, other->flags, iLimit);
    uprv_memcpy(newTrie->index, other->index, iLimit*4);

    return newTrie;
}

U_CAPI UTrie3 * U_EXPORT2
utrie3_clone(const UTrie3 *other, UErrorCode *pErrorCode) {
    UTrie3 *trie;

    if(U_FAILURE(*pErrorCode)) {
        return NULL;
    }
    if(other==NULL || (other->memory==NULL && other->newTrie==NULL)) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    trie=(UTrie3 *)uprv_malloc(sizeof(UTrie3));
    if(trie==NULL) {
        return NULL;
    }
    uprv_memcpy(trie, other, sizeof(UTrie3));

    if(other->memory!=NULL) {
        trie->memory=uprv_malloc(other->length);
        if(trie->memory!=NULL) {
            trie->isMemoryOwned=TRUE;
            uprv_memcpy(trie->memory, other->memory, other->length);

            /* make the clone's pointers point to its own memory */
            trie->index=(uint16_t *)trie->memory+(other->index-(uint16_t *)other->memory);
            if(other->data16!=NULL) {
                trie->data16=(uint16_t *)trie->memory+(other->data16-(uint16_t *)other->memory);
            }
            if(other->data32!=NULL) {
                trie->data32=(uint32_t *)trie->memory+(other->data32-(uint32_t *)other->memory);
            }
        }
    } else /* other->newTrie!=NULL */ {
        trie->newTrie=cloneBuilder(other->newTrie, trie->highStart);
    }

    if(trie->memory==NULL && trie->newTrie==NULL) {
        uprv_free(trie);
        trie=NULL;
    }
    return trie;
}

namespace {
#if 0  // TODO
typedef struct NewTrieAndStatus {
    UTrie3 *trie;
    UErrorCode errorCode;
    UBool exclusiveLimit;  /* rather than inclusive range end */  // TODO: remove
} NewTrieAndStatus;

UBool U_CALLCONV
copyEnumRange(const void *context, UChar32 start, UChar32 end, uint32_t value) {
    NewTrieAndStatus *nt=(NewTrieAndStatus *)context;
    if(value!=nt->trie->initialValue) {
        if(nt->exclusiveLimit) {
            --end;
        }
        if(end >= nt->trie->highStart) {
            end = nt->trie->highStart - 1;
            if(start > end) {
                return TRUE;
            }
        }
        if(start==end) {
            utrie3_set32(nt->trie, start, value, &nt->errorCode);
        } else {
            utrie3_setRange32(nt->trie, start, end, value, TRUE, &nt->errorCode);
        }
        return U_SUCCESS(nt->errorCode);
    } else {
        return TRUE;
    }
}
#endif

#ifdef UTRIE3_DEBUG
long countInitial(const UTrie3 *trie) {
    uint32_t initialValue=trie->initialValue;
    int32_t length=trie->dataLength;
    long count=0;
    if(trie->data16!=nullptr) {
        for(int32_t i=0; i<length; ++i) {
            if(trie->data16[i]==initialValue) { ++count; }
        }
    } else {
        for(int32_t i=0; i<length; ++i) {
            if(trie->data32[i]==initialValue) { ++count; }
        }
    }
    return count;
}

void
utrie3_printLengths(const UTrie3 *trie, const char *which) {
    long indexLength=trie->indexLength;
    long dataLength=(long)trie->dataLength;
    long totalLength=(long)sizeof(UTrie3Header)+indexLength*2+dataLength*(trie->data32!=NULL ? 4 : 2);
    printf("**UTrie3Lengths(%s %s)** index:%6ld  data:%6ld  countInitial:%6ld  serialized:%6ld\n",
           which, trie->name, indexLength, dataLength, countInitial(trie), totalLength);
}
#endif

}  // namespace

#if 0
U_CAPI UTrie3 * U_EXPORT2
utrie3_cloneAsThawed(const UTrie3 *other, UErrorCode *pErrorCode) {
    NewTrieAndStatus context;

    if(U_FAILURE(*pErrorCode)) {
        return NULL;
    }
    if(other==NULL || (other->memory==NULL && other->newTrie==NULL)) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }
    if(other->newTrie!=NULL) {
        return utrie3_clone(other, pErrorCode);  /* clone an unfrozen trie */
    }

    /* Clone the frozen trie by enumerating it and building a new one. */
    context.trie=utrie3_open(other->highValue, other->errorValue, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        return NULL;
    }
    context.exclusiveLimit=FALSE;
    context.errorCode=*pErrorCode;
    utrie3_enum(other, NULL, copyEnumRange, &context);
    *pErrorCode=context.errorCode;
    if(U_FAILURE(*pErrorCode)) {
        utrie3_close(context.trie);
        context.trie=NULL;
    }
    return context.trie;
}
#endif

U_CFUNC uint32_t
utrie3_get32FromBuilder(const UTrie3 *trie, UChar32 c) {
    if((uint32_t)c>MAX_UNICODE) {
        return trie->errorValue;
    }
    if(c>=trie->highStart) {
        return trie->highValue;
    }
    const UNewTrie3 *newTrie=trie->newTrie;
    int32_t i=c>>UTRIE3_SHIFT_2;
    if(newTrie->flags[i]==ALL_SAME) {
        return newTrie->index[i];
    } else {
        return newTrie->data[newTrie->index[i] + (c&UTRIE3_DATA_MASK)];
    }
}

// TODO: move to utrie3.cpp
U_CFUNC int32_t
utrie3_getRange32FromBuilder(const UTrie3 *trie, UChar32 start,
                             UTrie3EnumValue *enumValue, const void *context, uint32_t *pValue) {
    if((uint32_t)start>MAX_UNICODE) {
        return U_SENTINEL;
    }
    if(start>=trie->highStart) {
        if(pValue!=nullptr) { *pValue=trie->highValue; }
        return MAX_UNICODE;
    }
    uint32_t initialValue=trie->initialValue;
    if(enumValue!=nullptr) { initialValue=enumValue(context, initialValue); }
    const UNewTrie3 *newTrie=trie->newTrie;
    UChar32 c=start;
    uint32_t value;
    int32_t i=c>>UTRIE3_SHIFT_2;
    uint8_t flags=newTrie->flags[i];
    if(flags==ALL_SAME) {
        value=newTrie->index[i];
        if(value==trie->initialValue) {
            value=initialValue;
        } else if(enumValue!=nullptr) {
            value=enumValue(context, value);
        }
        if(pValue!=nullptr) { *pValue=value; }
        c=(c+UTRIE3_DATA_MASK)&~UTRIE3_DATA_MASK;
    } else /* MIXED */ {
        uint32_t di=newTrie->index[i] + (c&UTRIE3_DATA_MASK);
        value=newTrie->data[di];
        if(value==trie->initialValue) {
            value=initialValue;
        } else if(enumValue!=nullptr) {
            value=enumValue(context, value);
        }
        if(pValue!=nullptr) { *pValue=value; }
        while((++c&UTRIE3_DATA_MASK)!=0) {
            uint32_t value2=newTrie->data[++di];
            if(value2==trie->initialValue) {
                value2=initialValue;
            } else if(enumValue!=nullptr) {
                value2=enumValue(context, value2);
            }
            if(value2!=value) {
                return c-1;
            }
        }
    }
    ++i;
    while(c<trie->highStart) {
        flags=newTrie->flags[i];
        if(flags==ALL_SAME) {
            uint32_t value2=newTrie->index[i];
            if(value2==trie->initialValue) {
                value2=initialValue;
            } else if(enumValue!=nullptr) {
                value2=enumValue(context, value2);
            }
            if(value2!=value) {
                break;
            }
            c+=UTRIE3_DATA_BLOCK_LENGTH;
        } else /* MIXED */ {
            uint32_t di=newTrie->index[i];
            do {
                uint32_t value2=newTrie->data[di++];
                if(value2==trie->initialValue) {
                    value2=initialValue;
                } else if(enumValue!=nullptr) {
                    value2=enumValue(context, value2);
                }
                if(value2!=value) {
                    return c-1;
                }
            } while((++c&UTRIE3_DATA_MASK)!=0);
        }
        ++i;
    }
    if(value==initialValue) {
        return MAX_UNICODE;
    } else {
        return c-1;
    }
}

namespace {

void
writeBlock(uint32_t *block, uint32_t value) {
    uint32_t *limit=block+UTRIE3_DATA_BLOCK_LENGTH;
    while(block<limit) {
        *block++=value;
    }
}

void ensureHighStart(UTrie3 *trie, UChar32 c) {
    if(c>=trie->highStart) {
        UNewTrie3 *newTrie=trie->newTrie;
        // Round up to a full block.
        c=(c+UTRIE3_DATA_BLOCK_LENGTH)&~UTRIE3_DATA_BLOCK_LENGTH;
        int32_t i=trie->highStart>>UTRIE3_SHIFT_2;
        int32_t iLimit=c>>UTRIE3_SHIFT_2;
        do {
            newTrie->flags[i]=ALL_SAME;
            newTrie->index[i]=trie->initialValue;
        } while(++i<iLimit);
        trie->highStart=c;
    }
}

int32_t
allocDataBlock(UNewTrie3 *newTrie, uint32_t value) {
    int32_t newBlock=newTrie->dataLength;
    int32_t newTop=newBlock+UTRIE3_DATA_BLOCK_LENGTH;
    if(newTop>newTrie->dataCapacity) {
        int32_t capacity;
        if(newTrie->dataCapacity<UNEWTRIE3_MEDIUM_DATA_LENGTH) {
            capacity=UNEWTRIE3_MEDIUM_DATA_LENGTH;
        } else if(newTrie->dataCapacity<UNEWTRIE3_MAX_DATA_LENGTH) {
            capacity=UNEWTRIE3_MAX_DATA_LENGTH;
        } else {
            // Should never occur.
            // Either UNEWTRIE3_MAX_DATA_LENGTH is incorrect,
            // or the code writes more values than should be possible.
            return -1;
        }
        uint32_t *data=(uint32_t *)uprv_malloc(capacity*4);
        if(data==NULL) {
            return -1;
        }
        uprv_memcpy(data, newTrie->data, (size_t)newTrie->dataLength*4);
        uprv_free(newTrie->data);
        newTrie->data=data;
        newTrie->dataCapacity=capacity;
    }
    newTrie->dataLength=newTop;
    writeBlock(newTrie->data+newBlock, value);
    return newBlock;
}

/**
 * No error checking for illegal arguments.
 *
 * @return -1 if no new data block available (out of memory in data array)
 * @internal
 */
int32_t
getDataBlock(UNewTrie3 *newTrie, UChar32 c) {
    int32_t i=c>>UTRIE3_SHIFT_2;
    uint8_t flags=newTrie->flags[i];
    if(flags==MIXED) {
        return newTrie->index[i];
    }
    int32_t newBlock=allocDataBlock(newTrie, newTrie->index[i]);
    if(newBlock>=0) {
        newTrie->flags[i]=MIXED;
        newTrie->index[i]=newBlock;
    }
    return newBlock;
}

}  // namespace

U_CAPI void U_EXPORT2
utrie3_set32(UTrie3 *trie, UChar32 c, uint32_t value, UErrorCode *pErrorCode) {
    if(U_FAILURE(*pErrorCode)) {
        return;
    }
    if((uint32_t)c>MAX_UNICODE) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    UNewTrie3 *newTrie=trie->newTrie;
    if(newTrie==nullptr) {
        *pErrorCode=U_NO_WRITE_PERMISSION;
        return;
    }

    ensureHighStart(trie, c);
    int32_t block=getDataBlock(newTrie, c);
    if(block<0) {
        *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
        return;
    }

    newTrie->data[block+(c&UTRIE3_DATA_MASK)]=value;
}

/**
 * initialValue is ignored if overwrite=TRUE
 * @internal
 */
static void
fillBlock(uint32_t *block, UChar32 start, UChar32 limit,
          uint32_t value, uint32_t initialValue, UBool overwrite) {
    uint32_t *pLimit=block+limit;
    block+=start;
    if(overwrite) {
        while(block<pLimit) {
            *block++=value;
        }
    } else {
        while(block<pLimit) {
            if(*block==initialValue) {
                *block=value;
            }
            ++block;
        }
    }
}

U_CAPI void U_EXPORT2
utrie3_setRange32(UTrie3 *trie,
                  UChar32 start, UChar32 end,
                  uint32_t value, UBool overwrite,
                  UErrorCode *pErrorCode) {
    if(U_FAILURE(*pErrorCode)) {
        return;
    }
    if((uint32_t)start>MAX_UNICODE || (uint32_t)end>MAX_UNICODE || start>end) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    UNewTrie3 *newTrie=trie->newTrie;
    if(newTrie==nullptr) {
        *pErrorCode=U_NO_WRITE_PERMISSION;
        return;
    }
    if(!overwrite && value==trie->initialValue) {
        return; /* nothing to do */
    }
    ensureHighStart(trie, end);

    UChar32 limit=end+1;
    if(start&UTRIE3_DATA_MASK) {
        /* set partial block at [start..following block boundary[ */
        int32_t block=getDataBlock(newTrie, start);
        if(block<0) {
            *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
            return;
        }

        UChar32 nextStart=(start+UTRIE3_DATA_BLOCK_LENGTH)&~UTRIE3_DATA_MASK;
        if(nextStart<=limit) {
            fillBlock(newTrie->data+block, start&UTRIE3_DATA_MASK, UTRIE3_DATA_BLOCK_LENGTH,
                      value, trie->initialValue, overwrite);
            start=nextStart;
        } else {
            fillBlock(newTrie->data+block, start&UTRIE3_DATA_MASK, limit&UTRIE3_DATA_MASK,
                      value, trie->initialValue, overwrite);
            return;
        }
    }

    /* number of positions in the last, partial block */
    int32_t rest=limit&UTRIE3_DATA_MASK;

    /* round down limit to a block boundary */
    limit&=~UTRIE3_DATA_MASK;

    /* iterate over all-value blocks */
    while(start<limit) {
        int32_t i=start>>UTRIE3_SHIFT_2;
        uint8_t flags=newTrie->flags[i];
        if(flags==ALL_SAME) {
            if(overwrite || newTrie->index[i]==trie->initialValue) {
                newTrie->index[i]=value;
            }
        } else /* MIXED */ {
            fillBlock(newTrie->data+newTrie->index[i], 0, UTRIE3_DATA_BLOCK_LENGTH,
                      value, trie->initialValue, overwrite);
        }
        start+=UTRIE3_DATA_BLOCK_LENGTH;
    }

    if(rest>0) {
        /* set partial block at [last block boundary..limit[ */
        int32_t block=getDataBlock(newTrie, start);
        if(block<0) {
            *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
            return;
        }

        fillBlock(newTrie->data+block, 0, rest, value, trie->initialValue, overwrite);
    }
}

/* compaction --------------------------------------------------------------- */

namespace {

inline UBool
equal_uint32(const uint32_t *s, const uint32_t *t, int32_t length) {
    while(length>0 && *s==*t) {
        ++s;
        ++t;
        --length;
    }
    return (UBool)(length==0);
}

UBool allValuesSameAs(const uint32_t *p, int32_t length, uint32_t value) {
    const uint32_t *pLimit = p + length;
    while (p < pLimit && *p == value) { ++p; }
    return p == pLimit;
}

/** Search for an identical block. */
int32_t findSameBlock(const uint32_t *p, int32_t length, const uint32_t *other,
                      int32_t blockLength, int32_t granularity) {
    // Ensure that we do not even partially get past dataLength.
    length -= blockLength;

    for (int32_t block = 0; block <= length; block += granularity) {
        if (equal_uint32(p + block, other, blockLength)) {
            return block;
        }
    }
    return -1;
}

int32_t findAllSameBlock(const uint32_t *p, int32_t length, uint32_t value,
                         int32_t blockLength, int32_t granularity) {
    // Ensure that we do not even partially get past length.
    length -= blockLength;

    for (int32_t block = 0; block <= length; block += granularity) {
        if (p[block] == value) {
            for (int32_t i = 1;; ++i) {
                if (i == blockLength) {
                    return block;
                }
                if (p[block + i] != value) {
                    block += i & ~(granularity - 1);
                    break;
                }
            }
        }
    }
    return -1;
}

/**
 * Look for maximum overlap (modulo granularity) of the beginning of the other block
 * with the previous, adjacent block.
 */
int32_t getOverlap(const uint32_t *p, int32_t length, const uint32_t *other,
                   int32_t blockLength, int32_t granularity) {
    int32_t overlap = blockLength - granularity;
    while (overlap > 0 && !equal_uint32(p + (length-overlap), other, overlap)) {
        overlap -= granularity;
    }
    return overlap;
}

int32_t getAllSameOverlap(const uint32_t *p, int32_t length, uint32_t value,
                          int32_t blockLength, int32_t granularity) {
    int32_t min = length - (blockLength - granularity);
    int32_t i = length;
    while (min < i && p[i - 1] == value) { --i; }
    return (length - i) & ~(granularity - 1);
}

/*
 * Find the start of the last range in the trie by enumerating backward.
 * Indexes for supplementary code points higher than this will be omitted.
 */
UChar32
findHighStart(UNewTrie3 *newTrie, int32_t highStart, uint32_t highValue) {
    int32_t i=highStart>>UTRIE3_SHIFT_2;
    while(i>0) {
        uint8_t flags=newTrie->flags[--i];
        bool match;
        if(flags==ALL_SAME) {
            match= newTrie->index[i]==highValue;
        } else /* MIXED */ {
            const uint32_t *p=newTrie->data+newTrie->index[i];
            for(int32_t j=0;; ++j) {
                if(j==UTRIE3_DATA_BLOCK_LENGTH) {
                    match=true;
                    break;
                }
                if(p[j]!=highValue) {
                    match=false;
                    break;
                }
            }
        }
        if(!match) {
            return (i+1)<<UTRIE3_SHIFT_2;
        }
    }
    return 0;
}

int32_t compactWholeDataBlocks(UNewTrie3 *newTrie, uint32_t initialValue, UChar32 highStart) {
    int32_t newDataLength=0;
    int32_t iLimit=highStart>>UTRIE3_SHIFT_2;
    for(int32_t i=0; i<iLimit; ++i) {
        uint8_t flags=newTrie->flags[i];
        uint32_t value=newTrie->index[i];
        if(flags==MIXED) {
            // Really mixed?
            const uint32_t *p = newTrie->data + value;
            value = *p++;
            if (allValuesSameAs(p + 1, UTRIE3_DATA_BLOCK_LENGTH - 1, value)) {
                newTrie->flags[i]=ALL_SAME;
                newTrie->index[i]=value;
                // Fall through to ALL_SAME handling.
            } else {
                // Is there another whole mixed block with the same data?
                p=newTrie->data+newTrie->index[i];
                for(int32_t j=0;; ++j) {
                    if(j==i) {
                        // Unique mixed-value block.
                        newDataLength+=UTRIE3_DATA_BLOCK_LENGTH;
                        break;
                    }
                    if((newTrie->flags[j]&TYPE_MASK)==MIXED &&
                            equal_uint32(p, newTrie->data+newTrie->index[j], UTRIE3_DATA_BLOCK_LENGTH)) {
                        if(i>=BMP_I_LIMIT) {
                            newTrie->flags[j]|=SUPP_DATA;
                        }
                        newTrie->flags[i]=SAME_AS;
                        newTrie->index[i]=j;
                        break;
                    }
                }
                continue;
            }
        } else {
            U_ASSERT(flags==ALL_SAME);
        }
        // Is there another ALL_SAME block with the same value?
        for(int32_t j=0;; ++j) {
            if(j==i) {
                if (value == initialValue) {
                    newTrie->dataNullIndex = i;
                }
                // Unique same-value block.
                newDataLength+=UTRIE3_DATA_BLOCK_LENGTH;
                break;
            }
            if((newTrie->flags[j]&TYPE_MASK)==ALL_SAME && newTrie->index[j]==value) {
                if(i>=BMP_I_LIMIT) {
                    newTrie->flags[j]|=SUPP_DATA;
                }
                newTrie->flags[i]=SAME_AS;
                newTrie->index[i]=j;
                break;
            }
        }
    }
    return newDataLength;
}

#ifdef UTRIE3_DEBUG
#   define DEBUG_DO(expr) expr
#else
#   define DEBUG_DO(expr)
#endif

/**
 * Compacts a build-time trie.
 *
 * The compaction
 * - removes blocks that are identical with earlier ones
 * - overlaps each new non-duplicate block as much as possible with the previously-written one
 * - moves supplementary data blocks in steps of the data granularity
 *
 * It does not try to find an optimal order of writing, deduplicating, and overlapping blocks.
 */
void
compactData(UTrie3 *trie, UChar32 highStart, uint32_t *newData) {
    UNewTrie3 *newTrie = trie->newTrie;
#ifdef UTRIE3_DEBUG
    int32_t countSame=0, sumOverlaps=0;
#endif

    // linear ASCII data
    int32_t start=0;
    int32_t newStart=ASCII_LIMIT;
    for(int32_t i=0; start<newStart; start+=UTRIE3_DATA_BLOCK_LENGTH, ++i) {
        newTrie->flags[i]=MOVED;
        newTrie->index[i]=start;
    }
    U_ASSERT(start==newStart);

    // Write all BMP data before supplementary-only data, to maximize the chance
    // that unshifted BMP indexes work.
    // First write BMP data blocks that are not shared with supplementary code points.
    int32_t granularity=1;
    int32_t iLimit=BMP_I_LIMIT;
    for(int32_t i=ASCII_I_LIMIT;; ++i) {
        if(i==iLimit) {
            if(granularity==1) {
                // Supplementary data needs a bigger data granularity for shifted indexes,
                // so that more than 64k values are supported.
                // Padding here also ensures that the final dataLength is
                // a multiple of the shifted granularity.
                while((newStart&(UTRIE3_DATA_GRANULARITY-1))!=0) {
                    newTrie->data[newStart++]=0xaaaa5555;
                }
                granularity=UTRIE3_DATA_GRANULARITY;

                // Now write data blocks that are used for supplementary code points.
                i=ASCII_I_LIMIT;
                iLimit=highStart>>UTRIE3_SHIFT_2;
            } else {
                break;
            }
        }
        uint8_t flags=newTrie->flags[i];
        if(granularity!=1) {
            // BMP: Look for flags without SUPP_DATA. Then look for the remaining ones.
            flags&=TYPE_MASK;
        }
        if(flags==ALL_SAME) {
            uint32_t value=newTrie->index[i];
            int32_t n = findAllSameBlock(newData, newStart, value, UTRIE3_DATA_BLOCK_LENGTH, granularity);
            if (n >= 0) {
                DEBUG_DO(++countSame);
                newTrie->index[i]=n;
            } else {
                n=getAllSameOverlap(newData, newStart, value, UTRIE3_DATA_BLOCK_LENGTH, granularity);
                DEBUG_DO(sumOverlaps+=n);
                newTrie->index[i]=newStart-n;
                while(n<UTRIE3_DATA_BLOCK_LENGTH) {
                    newData[newStart++]=value;
                    ++n;
                }
            }
            newTrie->flags[i]=MOVED;
        } else if(flags==MIXED) {
            const uint32_t *block=newTrie->data+newTrie->index[i];
            int32_t n = findSameBlock(newData, newStart, block, UTRIE3_DATA_BLOCK_LENGTH, granularity);
            if (n >= 0) {
                DEBUG_DO(++countSame);
                newTrie->index[i]=n;
            } else {
                n=getOverlap(newData, newStart, block, UTRIE3_DATA_BLOCK_LENGTH, granularity);
                DEBUG_DO(sumOverlaps+=n);
                newTrie->index[i]=newStart-n;
                while(n<UTRIE3_DATA_BLOCK_LENGTH) {
                    newData[newStart++]=block[n++];
                }
            }
            newTrie->flags[i]=MOVED;
        }
    }

    for(int32_t i=ASCII_I_LIMIT; i<iLimit; ++i) {
        if(newTrie->flags[i]==SAME_AS) {
            DEBUG_DO(++countSame);
            uint32_t j=newTrie->index[i];
            U_ASSERT(newTrie->flags[j]==MOVED);
            newTrie->flags[i]=MOVED;
            newTrie->index[i]=newTrie->index[j];
        } else {
            U_ASSERT(newTrie->flags[i]==MOVED);
        }
    }

    if (newTrie->dataNullIndex >= 0) {
        trie->dataNullOffset = newTrie->index[newTrie->dataNullIndex];
    } else {
        trie->dataNullOffset = UTRIE3_NO_DATA_NULL_OFFSET;
    }

#ifdef UTRIE3_DEBUG
    /* we saved some space */
    printf("compacting UTrie3: count of 32-bit data words %lu->%lu  countSame=%ld  sumOverlaps=%ld\n",
            (long)newTrie->dataLength, (long)newStart, (long)countSame, (long)sumOverlaps);
#endif

    trie->dataLength=newStart;
}

void
compactIndex2(UTrie3 *trie, UChar32 highStart, uint16_t index1[]) {
    // The BMP index is linear, and the index-1 table is used only for supplementary code points.
    if (highStart <= BMP_LIMIT) {
        trie->indexLength = BMP_I_LIMIT;
        return;
    }

    UNewTrie3 *newTrie = trie->newTrie;
    // Compact the supplementary part of newTrie->index.
    uint32_t *index = newTrie->index;
    int32_t start = BMP_I_LIMIT;
    int32_t newStart = start;
    int32_t iLimit = highStart >> UTRIE3_SHIFT_2;
    // Set index-1 entries to the new starts of index-2 blocks, offset by the index-1 length.
    // The index-1 table is inserted before the supplementary index-2 blocks later,
    // when writing the final structure.
    U_ASSERT(highStart > BMP_LIMIT);
    int32_t offset = (highStart - BMP_LIMIT) >> UTRIE3_SHIFT_1;
    int32_t nullOffset = -1;

    for (; start < iLimit; start += UTRIE3_INDEX_2_BLOCK_LENGTH) {
        int32_t i2;
        // Is this the same as the index-2 null block?
        if (nullOffset >= 0 &&
                allValuesSameAs(index + start, UTRIE3_INDEX_2_BLOCK_LENGTH, trie->dataNullOffset)) {
            i2 = nullOffset;
        } else {
            // Find an earlier index block with the same values.
            // Either a BMP index block or a supplementary index-2 block,
            // but not crossing the boundary.
            int32_t n = findSameBlock(index, BMP_I_LIMIT, index + start, UTRIE3_INDEX_2_BLOCK_LENGTH, 1);
            if (n >= 0) {
                i2 = n;
            } else {
                n = findSameBlock(index + BMP_I_LIMIT, newStart - BMP_I_LIMIT,
                                index + start, UTRIE3_INDEX_2_BLOCK_LENGTH, 1);
                if (n >= 0) {
                    i2 = BMP_I_LIMIT + offset + n;
                } else {
                    n = getOverlap(index + BMP_I_LIMIT, newStart - BMP_I_LIMIT,
                                index + start, UTRIE3_INDEX_2_BLOCK_LENGTH, 1);
                    i2 = offset + (newStart - n);
                    if (n > 0 || newStart != start) {
                        while (n < UTRIE3_INDEX_2_BLOCK_LENGTH) {
                            index[newStart++] = index[start + n++];
                        }
                    }
                }
            }
            // Is this the first index-2 block with all initial values?
            if (nullOffset < 0 && newTrie->dataNullIndex >= 0 &&
                    allValuesSameAs(index + start, UTRIE3_INDEX_2_BLOCK_LENGTH, trie->dataNullOffset)) {
                nullOffset = i2;
            }
        }
        index1[(start >> UTRIE3_SHIFT_1_2) - UTRIE3_OMITTED_BMP_INDEX_1_LENGTH] = i2;
    }

    if (nullOffset >= 0) {
        trie->index2NullOffset = nullOffset;
    } else {
        trie->index2NullOffset = UTRIE3_NO_INDEX2_NULL_OFFSET;
    }

    // Ensure data table alignment:
    // Needs to be granularity-aligned for a 16-bit trie
    // (so that dataMove will be down-shiftable),
    // and 2-aligned for uint32_t data.
    int32_t length = newStart + offset;
    while ((length & ((UTRIE3_DATA_GRANULARITY - 1) | 1)) != 0) {
        // Arbitrary value: 0x3fffc not possible for real data.
        index[newStart++] = (int32_t)0xffff << UTRIE3_INDEX_SHIFT;
        ++length;
    }

#ifdef UTRIE3_DEBUG
    /* we saved some space */
    printf("compacting UTrie3: count of 16-bit index words %lu->%lu\n",
            (long)(iLimit + offset), (long)length);
#endif

    trie->indexLength = length;
}

void
compactTrie(UTrie3 *trie, uint16_t index1[], LocalMemory<uint32_t> &newData, UErrorCode *pErrorCode) {
    UNewTrie3 *newTrie=trie->newTrie;

    // Find highStart and round it up.
    uint32_t highValue=utrie3_get32FromBuilder(trie, MAX_UNICODE);
    UChar32 highStart=findHighStart(newTrie, trie->highStart, highValue);
    if((highStart&(UTRIE3_CP_PER_INDEX_1_ENTRY-1))!=0) {
        int32_t i=highStart>>UTRIE3_SHIFT_2;
        do {
            newTrie->flags[i] = ALL_SAME;
            newTrie->index[i] = highValue;
            ++i;
            highStart+=UTRIE3_DATA_BLOCK_LENGTH;
        } while((highStart&(UTRIE3_CP_PER_INDEX_1_ENTRY-1))!=0);
    }
    if(highStart==UNICODE_LIMIT) {
        highValue=trie->initialValue;
    }
    trie->highValue=highValue;

    trie->highStart=highStart;
    trie->highStartLead16=U16_LEAD(highStart);
    trie->shiftedHighStart=highStart>>UTRIE3_SHIFT_1;

#ifdef UTRIE3_DEBUG
    printf("UTrie3: highStart U+%06lx  highValue 0x%lx  initialValue 0x%lx\n",
            (long)highStart, (long)highValue, (long)trie->initialValue);
#endif

    // We always store indexes and data values for the BMP.
    // Use a version of highStart pinned to the supplementary range.
    UChar32 suppHighStart;
    if (highStart <= BMP_LIMIT) {
        for (int32_t i = (highStart >> UTRIE3_SHIFT_2); i < BMP_I_LIMIT; ++i) {
            newTrie->flags[i] = ALL_SAME;
            newTrie->index[i] = highValue;
        }
        suppHighStart = BMP_LIMIT;
    } else {
        suppHighStart = highStart;
    }

    uint32_t asciiData[ASCII_LIMIT];
    for(int32_t i=0; i<ASCII_LIMIT; ++i) {
        asciiData[i]=utrie3_get32FromBuilder(trie, i);
    }

    int32_t newDataLength=compactWholeDataBlocks(newTrie, trie->initialValue, suppHighStart);
    newDataLength+=ASCII_LIMIT;
    uint32_t *p=newData.allocateInsteadAndCopy(newDataLength);
    if(p==nullptr) {
        *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    uprv_memcpy(p, asciiData, sizeof(asciiData));

    compactData(trie, suppHighStart, p);
    U_ASSERT(trie->dataLength<=newDataLength);
    compactIndex2(trie, suppHighStart, index1);
}

}  // namespace

/* serialization ------------------------------------------------------------ */

/* Compact and internally serialize the trie. */
U_CAPI void U_EXPORT2
utrie3_freeze(UTrie3 *trie, UTrie3ValueBits valueBits, UErrorCode *pErrorCode) {
    /* argument check */
    if(U_FAILURE(*pErrorCode)) {
        return;
    }
    if (trie == nullptr || valueBits > UTRIE3_32_VALUE_BITS) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    UNewTrie3 *newTrie=trie->newTrie;
    if(newTrie==NULL) {
        /* already frozen */
        UTrie3ValueBits frozenValueBits=
            trie->data16!=NULL ? UTRIE3_16_VALUE_BITS : UTRIE3_32_VALUE_BITS;
        if(valueBits!=frozenValueBits) {
            *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        }
        return;
    }

    uint16_t index1[UTRIE3_MAX_INDEX_1_LENGTH];
    LocalMemory<uint32_t> newData;
    compactTrie(trie, index1, newData, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        // TODO: at every exit delete newTrie?!
        return;
    }
    UChar32 highStart=trie->highStart;

    int32_t dataMove;  /* >0 if the data is moved to the end of the index array */
    if(valueBits==UTRIE3_16_VALUE_BITS) {
        dataMove=trie->indexLength;
    } else {
        dataMove=0;
    }

    // Are all shifted supplementary indexes within limits?
    if (((dataMove + newTrie->dataLength) >> UTRIE3_INDEX_SHIFT) > 0xffff) {
        *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        return;
    }

    // Are all unshifted BMP indexes within limits?
    const uint32_t *p=newTrie->index;
    int32_t i;
    for(i=UTRIE3_INDEX_2_BMP_LENGTH; i>0; --i) {
        int32_t bmpIndex=dataMove + *p++;
        if(bmpIndex>0xffff) {
            *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
            return;
        }
    }

    /* calculate the total serialized length */
    int32_t length=sizeof(UTrie3Header)+trie->indexLength*2;
    if(valueBits==UTRIE3_16_VALUE_BITS) {
        length+=newTrie->dataLength*2;
    } else {
        length+=newTrie->dataLength*4;
    }

    trie->memory=uprv_malloc(length);
    if(trie->memory==NULL) {
        *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    trie->length=length;
    trie->isMemoryOwned=TRUE;

    trie->dataLength=newTrie->dataLength;

    /* set the header fields */
    UTrie3Header *header=(UTrie3Header *)trie->memory;

    header->signature=UTRIE3_SIG;  // "Tri3"
    header->options = ((uint32_t)trie->dataNullOffset << 12) | valueBits;

    header->indexLength=(uint16_t)trie->indexLength;
    header->shiftedDataLength=(uint16_t)(trie->dataLength>>UTRIE3_INDEX_SHIFT);
    header->index2NullOffset=trie->index2NullOffset;
    header->shiftedHighStart = trie->shiftedHighStart;
    header->highValue=trie->highValue;
    header->errorValue=trie->errorValue;

    /* fill the index and data arrays */
    uint16_t *dest16=(uint16_t *)(header+1);
    trie->index=dest16;

    /* write BMP index-2 array values, not right-shifted, after adding dataMove */
    p = newTrie->index;
    for(i = 0; i < UTRIE3_INDEX_2_BMP_LENGTH; ++i) {
        *dest16++=(uint16_t)(dataMove + trie->index[i]);
    }

    if(highStart>BMP_LIMIT) {
        int32_t index1Length=(highStart-BMP_LIMIT)>>UTRIE3_SHIFT_1;

        /* write 16-bit index-1 values for supplementary code points */
        uprv_memcpy(dest16, index1, index1Length * 2);

        /*
         * write the index-2 array values for supplementary code points,
         * shifted right by UTRIE3_INDEX_SHIFT, after adding dataMove
         */
        int32_t iLimit = trie->indexLength - index1Length;
        for (i = BMP_I_LIMIT; i < iLimit; ++i) {
            *dest16++ = (uint16_t)((dataMove + trie->index[i]) >> UTRIE3_INDEX_SHIFT);
        }
    }

    /* write the 16/32-bit data array */
    p = newData.getAlias();
    switch(valueBits) {
    case UTRIE3_16_VALUE_BITS:
        /* write 16-bit data values */
        trie->data16=dest16;
        trie->data32=NULL;
        for(i=newTrie->dataLength; i>0; --i) {
            *dest16++=(uint16_t)*p++;
        }
        break;
    case UTRIE3_32_VALUE_BITS:
        /* write 32-bit data values */
        trie->data16=NULL;
        trie->data32=(uint32_t *)dest16;
        uprv_memcpy(dest16, p, (size_t)newTrie->dataLength*4);
        break;
    default:  // TODO: remove
        // Will not occur, valueBits checked at the beginning.
        break;
    }

    /* Delete the UNewTrie3. */
    uprv_free(newTrie->data);
    uprv_free(newTrie);
    trie->newTrie=NULL;

#ifdef UTRIE3_DEBUG
    utrie3_printLengths(trie, "");
#endif
}

/*
 * This is here to avoid a dependency from utrie3.cpp on utrie.c.
 * This file already depends on utrie.c.
 * Otherwise, this should be in utrie3.cpp right after utrie3_swap().
 * TODO: find a better place
 */
U_CAPI int32_t U_EXPORT2
utrie3_swapAnyVersion(const UDataSwapper *ds,
                      const void *inData, int32_t length, void *outData,
                      UErrorCode *pErrorCode) {
    if(U_SUCCESS(*pErrorCode)) {
        switch(utrie3_getVersion(inData, length, TRUE)) {
        case 1:
            return utrie_swap(ds, inData, length, outData, pErrorCode);
//         case 2:  TODO
//             return utrie2_swap(ds, inData, length, outData, pErrorCode);
        case 3:
            return utrie3_swap(ds, inData, length, outData, pErrorCode);
        default:
            *pErrorCode=U_INVALID_FORMAT_ERROR;
            return 0;
        }
    }
    return 0;
}
