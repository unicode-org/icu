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
    UNEWTRIE3_DATA_START_OFFSET=UNEWTRIE3_DATA_NULL_OFFSET+UTRIE3_DATA_BLOCK_LENGTH,

    /**
     * The start of data blocks for U+0800 and above.
     * Below, compaction uses a block length of 64 for 2-byte UTF-8.
     * From here on, compaction uses UTRIE3_DATA_BLOCK_LENGTH.
     * Data values for 0x780 code points beyond ASCII.
     */
    UNEWTRIE3_DATA_0800_OFFSET=UNEWTRIE3_DATA_START_OFFSET+0x780
};

/* Start with allocation of 16k data entries. */
#define UNEWTRIE3_INITIAL_DATA_LENGTH ((int32_t)1<<14)

/* Grow about 8x each time. */
#define UNEWTRIE3_MEDIUM_DATA_LENGTH ((int32_t)1<<17)

static int32_t
allocIndex2Block(UNewTrie3 *trie);

U_CAPI UTrie3 * U_EXPORT2
utrie3_open(uint32_t initialValue, uint32_t errorValue, UErrorCode *pErrorCode) {
    UTrie3 *trie;
    UNewTrie3 *newTrie;
    uint32_t *data;
    int32_t i, j;

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
    trie->initialValue=initialValue;
    trie->errorValue=errorValue;
    trie->highValue=initialValue;
    trie->highStart=0x110000;
    trie->highStartLead16=0xdc00;  // U16_LEAD(0x110000) (after lead surrogates)
    trie->shiftedHighStart=0x110000>>UTRIE3_SHIFT_1;
    trie->newTrie=newTrie;

    newTrie->data=data;
    newTrie->dataCapacity=UNEWTRIE3_INITIAL_DATA_LENGTH;
    newTrie->initialValue=initialValue;
    newTrie->errorValue=errorValue;
    newTrie->highStart=0x110000;
    newTrie->firstFreeBlock=0;  /* no free block in the list */
    newTrie->isCompacted=FALSE;

    // Preallocate and reset ASCII.
    for(i=0; i<0x80; ++i) {
        newTrie->data[i]=initialValue;
    }
    // Preallocate and reset the null data block.
    for(i=UNEWTRIE3_DATA_NULL_OFFSET; i<UNEWTRIE3_DATA_START_OFFSET; ++i) {
        newTrie->data[i]=initialValue;
    }
    newTrie->dataNullOffset=UNEWTRIE3_DATA_NULL_OFFSET;
    newTrie->dataLength=UNEWTRIE3_DATA_START_OFFSET;

    /* set the index-2 indexes for the ASCII data blocks */
    for(i=0, j=0; j<0x80; ++i, j+=UTRIE3_DATA_BLOCK_LENGTH) {
        newTrie->index2[i]=j;
        newTrie->map[i]=1;
    }
    /*
     * Reference counts for the null data block: all blocks except for the ASCII blocks.
     * Plus 1 so that we don't drop this block during compaction.
     */
    /* i==newTrie->dataNullOffset */
    newTrie->map[i++]=
        (0x110000>>UTRIE3_SHIFT_2)-
        (0x80>>UTRIE3_SHIFT_2)+
        1;
    j+=UTRIE3_DATA_BLOCK_LENGTH;
    for(; j<UNEWTRIE3_DATA_START_OFFSET; ++i, j+=UTRIE3_DATA_BLOCK_LENGTH) {
        newTrie->map[i]=0;
    }

    /*
     * set the remaining indexes in the BMP index-2 block
     * to the null data block
     */
    for(i=0x80>>UTRIE3_SHIFT_2; i<UTRIE3_INDEX_2_BMP_LENGTH; ++i) {
        newTrie->index2[i]=UNEWTRIE3_DATA_NULL_OFFSET;
    }

    /*
     * Fill the index gap with impossible values so that compaction
     * does not overlap other index-2 blocks with the gap.
     */
    for(i=0; i<UNEWTRIE3_INDEX_GAP_LENGTH; ++i) {
        newTrie->index2[UNEWTRIE3_INDEX_GAP_OFFSET+i]=-1;
    }

    /* set the indexes in the null index-2 block */
    for(i=0; i<UTRIE3_INDEX_2_BLOCK_LENGTH; ++i) {
        newTrie->index2[UNEWTRIE3_INDEX_2_NULL_OFFSET+i]=UNEWTRIE3_DATA_NULL_OFFSET;
    }
    newTrie->index2NullOffset=UNEWTRIE3_INDEX_2_NULL_OFFSET;
    newTrie->index2Length=UNEWTRIE3_INDEX_2_START_OFFSET;

    /* set the index-1 indexes for the linear index-2 block */
    for(i=0, j=0;
        i<UTRIE3_OMITTED_BMP_INDEX_1_LENGTH;
        ++i, j+=UTRIE3_INDEX_2_BLOCK_LENGTH
    ) {
        newTrie->index1[i]=j;
    }

    /* set the remaining index-1 indexes to the null index-2 block */
    for(; i<UNEWTRIE3_INDEX_1_LENGTH; ++i) {
        newTrie->index1[i]=UNEWTRIE3_INDEX_2_NULL_OFFSET;
    }

    /*
     * Preallocate and reset data for U+0080..U+07ff,
     * for 2-byte UTF-8 which will be compacted in 64-blocks
     * even if UTRIE3_DATA_BLOCK_LENGTH is smaller.
     */
    for(i=0x80; i<0x800; i+=UTRIE3_DATA_BLOCK_LENGTH) {
        utrie3_set32(trie, i, initialValue, pErrorCode);
    }

    return trie;
}

static UNewTrie3 *
cloneBuilder(const UNewTrie3 *other) {
    UNewTrie3 *trie;

    trie=(UNewTrie3 *)uprv_malloc(sizeof(UNewTrie3));
    if(trie==NULL) {
        return NULL;
    }

    trie->data=(uint32_t *)uprv_malloc(other->dataCapacity*4);
    if(trie->data==NULL) {
        uprv_free(trie);
        return NULL;
    }
    trie->dataCapacity=other->dataCapacity;

    /* clone data */
    uprv_memcpy(trie->index1, other->index1, sizeof(trie->index1));
    uprv_memcpy(trie->index2, other->index2, (size_t)other->index2Length*4);
    trie->index2NullOffset=other->index2NullOffset;
    trie->index2Length=other->index2Length;

    uprv_memcpy(trie->data, other->data, (size_t)other->dataLength*4);
    trie->dataNullOffset=other->dataNullOffset;
    trie->dataLength=other->dataLength;

    /* reference counters */
    if(other->isCompacted) {
        trie->firstFreeBlock=0;
    } else {
        uprv_memcpy(trie->map, other->map, ((size_t)other->dataLength>>UTRIE3_SHIFT_2)*4);
        trie->firstFreeBlock=other->firstFreeBlock;
    }

    trie->initialValue=other->initialValue;
    trie->errorValue=other->errorValue;
    trie->highStart=other->highStart;
    trie->isCompacted=other->isCompacted;

    return trie;
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
        trie->newTrie=cloneBuilder(other->newTrie);
    }

    if(trie->memory==NULL && trie->newTrie==NULL) {
        uprv_free(trie);
        trie=NULL;
    }
    return trie;
}

typedef struct NewTrieAndStatus {
    UTrie3 *trie;
    UErrorCode errorCode;
    UBool exclusiveLimit;  /* rather than inclusive range end */
} NewTrieAndStatus;

static UBool U_CALLCONV
copyEnumRange(const void *context, UChar32 start, UChar32 end, uint32_t value) {
    NewTrieAndStatus *nt=(NewTrieAndStatus *)context;
    if(value!=nt->trie->initialValue) {
        if(nt->exclusiveLimit) {
            --end;
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

#ifdef UTRIE3_DEBUG
#if 0  // TODO
static void
utrie_printLengths(const UTrie *trie) {
    long indexLength=trie->indexLength;
    long dataLength=(long)trie->dataLength;
    long totalLength=(long)sizeof(UTrieHeader)+indexLength*2+dataLength*(trie->data32!=NULL ? 4 : 2);
    printf("**UTrieLengths** index:%6ld  data:%6ld  serialized:%6ld\n",
           indexLength, dataLength, totalLength);
}
#endif
static long countInitial(const UTrie3 *trie) {
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

static void
utrie3_printLengths(const UTrie3 *trie, const char *which) {
    long indexLength=trie->indexLength;
    long dataLength=(long)trie->dataLength;
    long totalLength=(long)sizeof(UTrie3Header)+indexLength*2+dataLength*(trie->data32!=NULL ? 4 : 2);
    printf("**UTrie3Lengths(%s)** index:%6ld  data:%6ld  serialized:%6ld  countInitial:%6ld\n",
           which, indexLength, dataLength, totalLength, countInitial(trie));
}
#endif

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
    if(other->newTrie!=NULL && !other->newTrie->isCompacted) {
        return utrie3_clone(other, pErrorCode);  /* clone an unfrozen trie */
    }

    /* Clone the frozen trie by enumerating it and building a new one. */
    context.trie=utrie3_open(other->initialValue, other->errorValue, pErrorCode);
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

#if 0  // TODO
/* Almost the same as utrie3_cloneAsThawed() but copies a UTrie and freezes the clone. */
U_CAPI UTrie3 * U_EXPORT2
utrie3_fromUTrie(const UTrie *trie1, uint32_t errorValue, UErrorCode *pErrorCode) {
    NewTrieAndStatus context;
    UChar lead;

    if(U_FAILURE(*pErrorCode)) {
        return NULL;
    }
    if(trie1==NULL) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }
    context.trie=utrie3_open(trie1->initialValue, errorValue, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        return NULL;
    }
    context.exclusiveLimit=TRUE;
    context.errorCode=*pErrorCode;
    utrie_enum(trie1, NULL, copyEnumRange, &context);
    *pErrorCode=context.errorCode;
    for(lead=0xd800; lead<0xdc00; ++lead) {
        uint32_t value;
        if(trie1->data32==NULL) {
            value=UTRIE_GET16_FROM_LEAD(trie1, lead);
        } else {
            value=UTRIE_GET32_FROM_LEAD(trie1, lead);
        }
        if(value!=trie1->initialValue) {
            utrie3_set32(context.trie, lead, value, pErrorCode);
        }
    }
    if(U_SUCCESS(*pErrorCode)) {
        utrie3_freeze(context.trie,
                      trie1->data32!=NULL ? UTRIE3_32_VALUE_BITS : UTRIE3_16_VALUE_BITS,
                      pErrorCode);
    }
#ifdef UTRIE3_DEBUG
    if(U_SUCCESS(*pErrorCode)) {
        utrie_printLengths(trie1);
        utrie3_printLengths(context.trie, "fromUTrie");
    }
#endif
    if(U_FAILURE(*pErrorCode)) {
        utrie3_close(context.trie);
        context.trie=NULL;
    }
    return context.trie;
}
#endif

static inline UBool
isInNullBlock(UNewTrie3 *trie, UChar32 c) {
    int32_t i2=trie->index1[c>>UTRIE3_SHIFT_1]+
        ((c>>UTRIE3_SHIFT_2)&UTRIE3_INDEX_2_MASK);
    int32_t block=trie->index2[i2];
    return (UBool)(block==trie->dataNullOffset);
}

static int32_t
allocIndex2Block(UNewTrie3 *trie) {
    int32_t newBlock, newTop;

    newBlock=trie->index2Length;
    newTop=newBlock+UTRIE3_INDEX_2_BLOCK_LENGTH;
    if(newTop>UPRV_LENGTHOF(trie->index2)) {
        /*
         * Should never occur.
         * Either UTRIE3_MAX_BUILD_TIME_INDEX_LENGTH is incorrect,
         * or the code writes more values than should be possible.
         */
        return -1;
    }
    trie->index2Length=newTop;
    uprv_memcpy(trie->index2+newBlock, trie->index2+trie->index2NullOffset, UTRIE3_INDEX_2_BLOCK_LENGTH*4);
    return newBlock;
}

static int32_t
getIndex2Block(UNewTrie3 *trie, UChar32 c) {
    int32_t i1=c>>UTRIE3_SHIFT_1;
    int32_t i2=trie->index1[i1];
    if(i2==trie->index2NullOffset) {
        i2=allocIndex2Block(trie);
        if(i2<0) {
            return -1;  /* program error */
        }
        trie->index1[i1]=i2;
    }
    return i2;
}

static int32_t
allocDataBlock(UNewTrie3 *trie, int32_t copyBlock) {
    int32_t newBlock, newTop;

    if(trie->firstFreeBlock!=0) {
        /* get the first free block */
        newBlock=trie->firstFreeBlock;
        trie->firstFreeBlock=-trie->map[newBlock>>UTRIE3_SHIFT_2];
    } else {
        /* get a new block from the high end */
        newBlock=trie->dataLength;
        newTop=newBlock+UTRIE3_DATA_BLOCK_LENGTH;
        if(newTop>trie->dataCapacity) {
            /* out of memory in the data array */
            int32_t capacity;
            uint32_t *data;

            if(trie->dataCapacity<UNEWTRIE3_MEDIUM_DATA_LENGTH) {
                capacity=UNEWTRIE3_MEDIUM_DATA_LENGTH;
            } else if(trie->dataCapacity<UNEWTRIE3_MAX_DATA_LENGTH) {
                capacity=UNEWTRIE3_MAX_DATA_LENGTH;
            } else {
                /*
                 * Should never occur.
                 * Either UNEWTRIE3_MAX_DATA_LENGTH is incorrect,
                 * or the code writes more values than should be possible.
                 */
                return -1;
            }
            data=(uint32_t *)uprv_malloc(capacity*4);
            if(data==NULL) {
                return -1;
            }
            uprv_memcpy(data, trie->data, (size_t)trie->dataLength*4);
            uprv_free(trie->data);
            trie->data=data;
            trie->dataCapacity=capacity;
        }
        trie->dataLength=newTop;
    }
    uprv_memcpy(trie->data+newBlock, trie->data+copyBlock, UTRIE3_DATA_BLOCK_LENGTH*4);
    trie->map[newBlock>>UTRIE3_SHIFT_2]=0;
    return newBlock;
}

/* call when the block's reference counter reaches 0 */
static void
releaseDataBlock(UNewTrie3 *trie, int32_t block) {
    /* put this block at the front of the free-block chain */
    trie->map[block>>UTRIE3_SHIFT_2]=-trie->firstFreeBlock;
    trie->firstFreeBlock=block;
}

static inline UBool
isWritableBlock(UNewTrie3 *trie, int32_t block) {
    return (UBool)(block!=trie->dataNullOffset && 1==trie->map[block>>UTRIE3_SHIFT_2]);
}

static inline void
setIndex2Entry(UNewTrie3 *trie, int32_t i2, int32_t block) {
    int32_t oldBlock;
    ++trie->map[block>>UTRIE3_SHIFT_2];  /* increment first, in case block==oldBlock! */
    oldBlock=trie->index2[i2];
    if(0 == --trie->map[oldBlock>>UTRIE3_SHIFT_2]) {
        releaseDataBlock(trie, oldBlock);
    }
    trie->index2[i2]=block;
}

/**
 * No error checking for illegal arguments.
 *
 * @return -1 if no new data block available (out of memory in data array)
 * @internal
 */
static int32_t
getDataBlock(UNewTrie3 *trie, UChar32 c) {
    int32_t i2, oldBlock, newBlock;

    i2=getIndex2Block(trie, c);
    if(i2<0) {
        return -1;  /* program error */
    }

    i2+=(c>>UTRIE3_SHIFT_2)&UTRIE3_INDEX_2_MASK;
    oldBlock=trie->index2[i2];
    if(isWritableBlock(trie, oldBlock)) {
        return oldBlock;
    }

    /* allocate a new data block */
    newBlock=allocDataBlock(trie, oldBlock);
    if(newBlock<0) {
        /* out of memory in the data array */
        return -1;
    }
    setIndex2Entry(trie, i2, newBlock);
    return newBlock;
}

U_CAPI void U_EXPORT2
utrie3_set32(UTrie3 *trie, UChar32 c, uint32_t value, UErrorCode *pErrorCode) {
    if(U_FAILURE(*pErrorCode)) {
        return;
    }
    if((uint32_t)c>0x10ffff) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    UNewTrie3 *newTrie=trie->newTrie;
    if(newTrie==nullptr || newTrie->isCompacted) {
        *pErrorCode=U_NO_WRITE_PERMISSION;
        return;
    }

    int32_t block=getDataBlock(newTrie, c);
    if(block<0) {
        *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
        return;
    }

    newTrie->data[block+(c&UTRIE3_DATA_MASK)]=value;
}

static void
writeBlock(uint32_t *block, uint32_t value) {
    uint32_t *limit=block+UTRIE3_DATA_BLOCK_LENGTH;
    while(block<limit) {
        *block++=value;
    }
}

/**
 * initialValue is ignored if overwrite=TRUE
 * @internal
 */
static void
fillBlock(uint32_t *block, UChar32 start, UChar32 limit,
          uint32_t value, uint32_t initialValue, UBool overwrite) {
    uint32_t *pLimit;

    pLimit=block+limit;
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
    /*
     * repeat value in [start..end]
     * mark index values for repeat-data blocks by setting bit 31 of the index values
     * fill around existing values if any, if(overwrite)
     */
    UNewTrie3 *newTrie;
    int32_t block, rest, repeatBlock;
    UChar32 limit;

    if(U_FAILURE(*pErrorCode)) {
        return;
    }
    if((uint32_t)start>0x10ffff || (uint32_t)end>0x10ffff || start>end) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    newTrie=trie->newTrie;
    if(newTrie==NULL || newTrie->isCompacted) {
        *pErrorCode=U_NO_WRITE_PERMISSION;
        return;
    }
    if(!overwrite && value==newTrie->initialValue) {
        return; /* nothing to do */
    }

    limit=end+1;
    if(start&UTRIE3_DATA_MASK) {
        UChar32 nextStart;

        /* set partial block at [start..following block boundary[ */
        block=getDataBlock(newTrie, start);
        if(block<0) {
            *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
            return;
        }

        nextStart=(start+UTRIE3_DATA_BLOCK_LENGTH)&~UTRIE3_DATA_MASK;
        if(nextStart<=limit) {
            fillBlock(newTrie->data+block, start&UTRIE3_DATA_MASK, UTRIE3_DATA_BLOCK_LENGTH,
                      value, newTrie->initialValue, overwrite);
            start=nextStart;
        } else {
            fillBlock(newTrie->data+block, start&UTRIE3_DATA_MASK, limit&UTRIE3_DATA_MASK,
                      value, newTrie->initialValue, overwrite);
            return;
        }
    }

    /* number of positions in the last, partial block */
    rest=limit&UTRIE3_DATA_MASK;

    /* round down limit to a block boundary */
    limit&=~UTRIE3_DATA_MASK;

    /* iterate over all-value blocks */
    if(value==newTrie->initialValue) {
        repeatBlock=newTrie->dataNullOffset;
    } else {
        repeatBlock=-1;
    }

    while(start<limit) {
        int32_t i2;
        UBool setRepeatBlock=FALSE;

        if(value==newTrie->initialValue && isInNullBlock(newTrie, start)) {
            start+=UTRIE3_DATA_BLOCK_LENGTH; /* nothing to do */
            continue;
        }

        /* get index value */
        i2=getIndex2Block(newTrie, start);
        if(i2<0) {
            *pErrorCode=U_INTERNAL_PROGRAM_ERROR;
            return;
        }
        i2+=(start>>UTRIE3_SHIFT_2)&UTRIE3_INDEX_2_MASK;
        block=newTrie->index2[i2];
        if(isWritableBlock(newTrie, block)) {
            /* already allocated */
            if(overwrite && block>=UNEWTRIE3_DATA_0800_OFFSET) {
                /*
                 * We overwrite all values, and it's not a
                 * protected (ASCII-linear or 2-byte UTF-8) block:
                 * replace with the repeatBlock.
                 */
                setRepeatBlock=TRUE;
            } else {
                /* !overwrite, or protected block: just write the values into this block */
                fillBlock(newTrie->data+block,
                          0, UTRIE3_DATA_BLOCK_LENGTH,
                          value, newTrie->initialValue, overwrite);
            }
        } else if(newTrie->data[block]!=value && (overwrite || block==newTrie->dataNullOffset)) {
            /*
             * Set the repeatBlock instead of the null block or previous repeat block:
             *
             * If !isWritableBlock() then all entries in the block have the same value
             * because it's the null block or a range block (the repeatBlock from a previous
             * call to utrie3_setRange32()).
             * No other blocks are used multiple times before compacting.
             *
             * The null block is the only non-writable block with the initialValue because
             * of the repeatBlock initialization above. (If value==initialValue, then
             * the repeatBlock will be the null data block.)
             *
             * We set our repeatBlock if the desired value differs from the block's value,
             * and if we overwrite any data or if the data is all initial values
             * (which is the same as the block being the null block, see above).
             */
            setRepeatBlock=TRUE;
        }
        if(setRepeatBlock) {
            if(repeatBlock>=0) {
                setIndex2Entry(newTrie, i2, repeatBlock);
            } else {
                /* create and set and fill the repeatBlock */
                repeatBlock=getDataBlock(newTrie, start);
                if(repeatBlock<0) {
                    *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
                    return;
                }
                writeBlock(newTrie->data+repeatBlock, value);
            }
        }

        start+=UTRIE3_DATA_BLOCK_LENGTH;
    }

    if(rest>0) {
        /* set partial block at [last block boundary..limit[ */
        block=getDataBlock(newTrie, start);
        if(block<0) {
            *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
            return;
        }

        fillBlock(newTrie->data+block, 0, rest, value, newTrie->initialValue, overwrite);
    }

    return;
}

/* compaction --------------------------------------------------------------- */

static inline UBool
equal_int32(const int32_t *s, const int32_t *t, int32_t length) {
    while(length>0 && *s==*t) {
        ++s;
        ++t;
        --length;
    }
    return (UBool)(length==0);
}

static inline UBool
equal_uint32(const uint32_t *s, const uint32_t *t, int32_t length) {
    while(length>0 && *s==*t) {
        ++s;
        ++t;
        --length;
    }
    return (UBool)(length==0);
}

static int32_t
findSameIndex2Block(const int32_t *idx, int32_t index2Length, int32_t otherBlock) {
    int32_t block;

    /* ensure that we do not even partially get past index2Length */
    index2Length-=UTRIE3_INDEX_2_BLOCK_LENGTH;

    for(block=0; block<=index2Length; ++block) {
        if(equal_int32(idx+block, idx+otherBlock, UTRIE3_INDEX_2_BLOCK_LENGTH)) {
            return block;
        }
    }
    return -1;
}

static int32_t
findSameDataBlock(const uint32_t *data, int32_t dataLength, int32_t otherBlock) {
    int32_t block;

    /* ensure that we do not even partially get past dataLength */
    dataLength-=UTRIE3_DATA_BLOCK_LENGTH;

    for(block=0; block<=dataLength; block+=UTRIE3_DATA_GRANULARITY) {
        if(equal_uint32(data+block, data+otherBlock, UTRIE3_DATA_BLOCK_LENGTH)) {
            return block;
        }
    }
    return -1;
}

/*
 * Find the start of the last range in the trie by enumerating backward.
 * Indexes for supplementary code points higher than this will be omitted.
 */
static UChar32
findHighStart(UNewTrie3 *trie, uint32_t highValue) {
    const uint32_t *data32;

    uint32_t value, initialValue;
    UChar32 c, prev;
    int32_t i1, i2, j, i2Block, prevI2Block, index2NullOffset, block, prevBlock, nullBlock;

    data32=trie->data;
    initialValue=trie->initialValue;

    index2NullOffset=trie->index2NullOffset;
    nullBlock=trie->dataNullOffset;

    /* set variables for previous range */
    if(highValue==initialValue) {
        prevI2Block=index2NullOffset;
        prevBlock=nullBlock;
    } else {
        prevI2Block=-1;
        prevBlock=-1;
    }
    prev=0x110000;

    /* enumerate index-2 blocks */
    i1=UNEWTRIE3_INDEX_1_LENGTH;
    c=prev;
    while(c>0) {
        i2Block=trie->index1[--i1];
        if(i2Block==prevI2Block) {
            /* the index-2 block is the same as the previous one, and filled with highValue */
            c-=UTRIE3_CP_PER_INDEX_1_ENTRY;
            continue;
        }
        prevI2Block=i2Block;
        if(i2Block==index2NullOffset) {
            /* this is the null index-2 block */
            if(highValue!=initialValue) {
                return c;
            }
            c-=UTRIE3_CP_PER_INDEX_1_ENTRY;
        } else {
            /* enumerate data blocks for one index-2 block */
            for(i2=UTRIE3_INDEX_2_BLOCK_LENGTH; i2>0;) {
                block=trie->index2[i2Block+ --i2];
                if(block==prevBlock) {
                    /* the block is the same as the previous one, and filled with highValue */
                    c-=UTRIE3_DATA_BLOCK_LENGTH;
                    continue;
                }
                prevBlock=block;
                if(block==nullBlock) {
                    /* this is the null data block */
                    if(highValue!=initialValue) {
                        return c;
                    }
                    c-=UTRIE3_DATA_BLOCK_LENGTH;
                } else {
                    for(j=UTRIE3_DATA_BLOCK_LENGTH; j>0;) {
                        value=data32[block+ --j];
                        if(value!=highValue) {
                            return c;
                        }
                        --c;
                    }
                }
            }
        }
    }

    /* deliver last range */
    return 0;
}

/*
 * Compact a build-time trie.
 *
 * The compaction
 * - removes blocks that are identical with earlier ones
 * - overlaps adjacent blocks as much as possible (if overlap==TRUE)
 * - moves blocks in steps of the data granularity
 * - moves and overlaps blocks that overlap with multiple values in the overlap region
 *
 * It does not
 * - try to move and overlap blocks that are not already adjacent
 */
static void
compactData(UNewTrie3 *trie) {
#ifdef UTRIE3_DEBUG
    int32_t countSame=0, sumOverlaps=0;
#endif

    /* do not compact linear-ASCII data */
    int32_t start=0;
    int32_t newStart=UTRIE3_DATA_START_OFFSET;
    int32_t i;
    for(i=0; start<newStart; start+=UTRIE3_DATA_BLOCK_LENGTH, ++i) {
        trie->map[i]=start;
    }

    U_ASSERT(start==newStart);
    for(; start<trie->dataLength; start+=UTRIE3_DATA_BLOCK_LENGTH) {
        /*
         * start: index of first entry of current block
         * newStart: index where the current block is to be moved
         *           (right after current end of already-compacted data)
         */

        /* skip blocks that are not used */
        if(trie->map[start>>UTRIE3_SHIFT_2]<=0) {
            /* leave newStart with the previous block! */
            continue;
        }

        /* search for an identical block */
        int32_t movedStart=findSameDataBlock(trie->data, newStart, start);
        if(movedStart>=0) {
#ifdef UTRIE3_DEBUG
            ++countSame;
#endif
            /* found an identical block, set the other block's index value for the current block */
            trie->map[start>>UTRIE3_SHIFT_2]=movedStart;

            /* leave newStart with the previous block! */
            continue;
        }

        /* see if the beginning of this block can be overlapped with the end of the previous block */
        /* look for maximum overlap (modulo granularity) with the previous, adjacent block */
        int32_t overlap;
        for(overlap=UTRIE3_DATA_BLOCK_LENGTH-UTRIE3_DATA_GRANULARITY;
            overlap>0 && !equal_uint32(trie->data+(newStart-overlap), trie->data+start, overlap);
            overlap-=UTRIE3_DATA_GRANULARITY) {}

#ifdef UTRIE3_DEBUG
            sumOverlaps+=overlap;
#endif
        if(overlap>0 || newStart<start) {
            /* some overlap, or just move the whole block */
            trie->map[start>>UTRIE3_SHIFT_2]=newStart-overlap;

            /* move the non-overlapping values to their new positions */
            int32_t j=start+overlap;
            int32_t limit=start+UTRIE3_DATA_BLOCK_LENGTH;
            do {
                trie->data[newStart++]=trie->data[j++];
            } while(j<limit);
        } else /* no overlap && newStart==start */ {
            trie->map[start>>UTRIE3_SHIFT_2]=start;
            newStart=start+UTRIE3_DATA_BLOCK_LENGTH;
        }
    }

    /* now adjust the index-2 table */
    for(i=0; i<trie->index2Length; ++i) {
        if(i==UNEWTRIE3_INDEX_GAP_OFFSET) {
            /* Gap indexes are invalid (-1). Skip over the gap. */
            i+=UNEWTRIE3_INDEX_GAP_LENGTH;
        }
        trie->index2[i]=trie->map[trie->index2[i]>>UTRIE3_SHIFT_2];
    }
    trie->dataNullOffset=trie->map[trie->dataNullOffset>>UTRIE3_SHIFT_2];

    /* ensure dataLength alignment */
    while((newStart&(UTRIE3_DATA_GRANULARITY-1))!=0) {
        trie->data[newStart++]=trie->initialValue;
    }

#ifdef UTRIE3_DEBUG
    /* we saved some space */
    printf("compacting UTrie3: count of 32-bit data words %lu->%lu  countSame=%ld  sumOverlaps=%ld\n",
            (long)trie->dataLength, (long)newStart, (long)countSame, (long)sumOverlaps);
#endif

    trie->dataLength=newStart;
}

static void
compactIndex2(UNewTrie3 *trie) {
    /* do not compact linear-BMP index-2 blocks */
    int32_t start=0;
    int32_t newStart=UTRIE3_INDEX_2_BMP_LENGTH;
    int32_t i;
    for(i=0; start<newStart; start+=UTRIE3_INDEX_2_BLOCK_LENGTH, ++i) {
        trie->map[i]=start;
    }

    /* Reduce the index table gap to what will be needed at runtime. */
    U_ASSERT(trie->highStart>0x10000);
    newStart+=UTRIE3_UTF8_2B_INDEX_2_LENGTH+((trie->highStart-0x10000)>>UTRIE3_SHIFT_1);

    for(start=UNEWTRIE3_INDEX_2_NULL_OFFSET; start<trie->index2Length; start+=UTRIE3_INDEX_2_BLOCK_LENGTH) {
        /*
         * start: index of first entry of current block
         * newStart: index where the current block is to be moved
         *           (right after current end of already-compacted data)
         */

        /* search for an identical block */
        int32_t movedStart=findSameIndex2Block(trie->index2, newStart, start);
        if(movedStart>=0) {
            /* found an identical block, set the other block's index value for the current block */
            trie->map[start>>UTRIE3_SHIFT_1_2]=movedStart;

            /* leave newStart with the previous block! */
            continue;
        }

        /* see if the beginning of this block can be overlapped with the end of the previous block */
        /* look for maximum overlap with the previous, adjacent block */
        int32_t  overlap;
        for(overlap=UTRIE3_INDEX_2_BLOCK_LENGTH-1;
            overlap>0 && !equal_int32(trie->index2+(newStart-overlap), trie->index2+start, overlap);
            --overlap) {}

        if(overlap>0 || newStart<start) {
            /* some overlap, or just move the whole block */
            trie->map[start>>UTRIE3_SHIFT_1_2]=newStart-overlap;

            /* move the non-overlapping indexes to their new positions */
            int32_t j=start+overlap;
            int32_t limit=start+UTRIE3_INDEX_2_BLOCK_LENGTH;
            do {
                trie->index2[newStart++]=trie->index2[j++];
            } while(j<limit);
        } else /* no overlap && newStart==start */ {
            trie->map[start>>UTRIE3_SHIFT_1_2]=start;
            newStart=start+UTRIE3_INDEX_2_BLOCK_LENGTH;
        }
    }

    /* now adjust the index-1 table */
    for(i=0; i<UNEWTRIE3_INDEX_1_LENGTH; ++i) {
        trie->index1[i]=trie->map[trie->index1[i]>>UTRIE3_SHIFT_1_2];
    }
    trie->index2NullOffset=trie->map[trie->index2NullOffset>>UTRIE3_SHIFT_1_2];

    /*
     * Ensure data table alignment:
     * Needs to be granularity-aligned for 16-bit trie
     * (so that dataMove will be down-shiftable),
     * and 2-aligned for uint32_t data.
     */
    while((newStart&((UTRIE3_DATA_GRANULARITY-1)|1))!=0) {
        /* Arbitrary value: 0x3fffc not possible for real data. */
        trie->index2[newStart++]=(int32_t)0xffff<<UTRIE3_INDEX_SHIFT;
    }

#ifdef UTRIE3_DEBUG
    /* we saved some space */
    printf("compacting UTrie3: count of 16-bit index-2 words %lu->%lu\n",
            (long)trie->index2Length, (long)newStart);
#endif

    trie->index2Length=newStart;
}

static void
compactTrie(UTrie3 *trie, UErrorCode *pErrorCode) {
    UNewTrie3 *newTrie;
    UChar32 highStart, suppHighStart;
    uint32_t highValue;

    newTrie=trie->newTrie;

    /* find highStart and round it up */
    highValue=utrie3_get32(trie, 0x10ffff);
    highStart=findHighStart(newTrie, highValue);
    highStart=(highStart+(UTRIE3_CP_PER_INDEX_1_ENTRY-1))&~(UTRIE3_CP_PER_INDEX_1_ENTRY-1);
    if(highStart==0x110000) {
        highValue=trie->errorValue;
    }
    trie->highValue=highValue;

    /*
     * Set trie->highStart only after utrie3_get32(trie, highStart).
     * Otherwise utrie3_get32(trie, highStart) would try to read the highValue.
     */
    trie->highStart=newTrie->highStart=highStart;
    trie->highStartLead16=U16_LEAD(newTrie->highStart);
    trie->shiftedHighStart=newTrie->highStart>>UTRIE3_SHIFT_1;

#ifdef UTRIE3_DEBUG
    printf("UTrie3: highStart U+%06lx  highValue 0x%lx  initialValue 0x%lx\n",
            (long)highStart, (long)highValue, (long)trie->initialValue);
#endif

    if(highStart<0x110000) {
        /* Blank out [highStart..10ffff] to release associated data blocks. */
        suppHighStart= highStart<=0x10000 ? 0x10000 : highStart;
        utrie3_setRange32(trie, suppHighStart, 0x10ffff, trie->initialValue, TRUE, pErrorCode);
        if(U_FAILURE(*pErrorCode)) {
            return;
        }
    }

    compactData(newTrie);
    if(highStart>0x10000) {
        compactIndex2(newTrie);
#ifdef UTRIE3_DEBUG
    } else {
        printf("UTrie3: highStart U+%04lx  count of 16-bit index-2 words %lu->%lu\n",
                (long)highStart, (long)trie->newTrie->index2Length, (long)UTRIE3_INDEX_1_OFFSET);
#endif
    }

    newTrie->isCompacted=TRUE;
}

/* serialization ------------------------------------------------------------ */

/**
 * Maximum length of the runtime index array.
 * Limited by its own 16-bit index values, and by uint16_t UTrie3Header.indexLength.
 * (The actual maximum length is lower,
 * (0x110000>>UTRIE3_SHIFT_2)+UTRIE3_UTF8_2B_INDEX_2_LENGTH+UTRIE3_MAX_INDEX_1_LENGTH.)
 */
#define UTRIE3_MAX_INDEX_LENGTH 0xffff

/**
 * Maximum length of the runtime data array.
 * Limited by 16-bit index values that are left-shifted by UTRIE3_INDEX_SHIFT,
 * and by uint16_t UTrie3Header.shiftedDataLength.
 */
#define UTRIE3_MAX_DATA_LENGTH (0xffff<<UTRIE3_INDEX_SHIFT)

/* Compact and internally serialize the trie. */
U_CAPI void U_EXPORT2
utrie3_freeze(UTrie3 *trie, UTrie3ValueBits valueBits, UErrorCode *pErrorCode) {
    UNewTrie3 *newTrie;
    UTrie3Header *header;
    uint32_t *p;
    uint16_t *dest16;
    int32_t i, length;
    int32_t allIndexesLength;
    int32_t dataMove;  /* >0 if the data is moved to the end of the index array */
    UChar32 highStart;

    /* argument check */
    if(U_FAILURE(*pErrorCode)) {
        return;
    }
    if( trie==NULL ||
        valueBits<0 || UTRIE3_COUNT_VALUE_BITS<=valueBits
    ) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    newTrie=trie->newTrie;
    if(newTrie==NULL) {
        /* already frozen */
        UTrie3ValueBits frozenValueBits=
            trie->data16!=NULL ? UTRIE3_16_VALUE_BITS : UTRIE3_32_VALUE_BITS;
        if(valueBits!=frozenValueBits) {
            *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        }
        return;
    }

    /* compact if necessary */
    if(!newTrie->isCompacted) {
        compactTrie(trie, pErrorCode);
        if(U_FAILURE(*pErrorCode)) {
            return;
        }
    }
    highStart=trie->highStart;

    if(highStart<=0x10000) {
        allIndexesLength=UTRIE3_INDEX_1_OFFSET;
    } else {
        allIndexesLength=newTrie->index2Length;
    }
    if(valueBits==UTRIE3_16_VALUE_BITS) {
        dataMove=allIndexesLength;
    } else {
        dataMove=0;
    }

    /* are indexLength and dataLength within limits? */
    if( /* for unshifted indexLength */
        allIndexesLength>UTRIE3_MAX_INDEX_LENGTH ||
        /* for unshifted dataNullOffset */
        (dataMove+newTrie->dataNullOffset)>0xffff ||
        /* for unshifted 2-byte UTF-8 index-2 values */
        (dataMove+UNEWTRIE3_DATA_0800_OFFSET)>0xffff ||
        /* for shiftedDataLength */
        (dataMove+newTrie->dataLength)>UTRIE3_MAX_DATA_LENGTH
    ) {
        *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        return;
    }

    /* calculate the total serialized length */
    length=sizeof(UTrie3Header)+allIndexesLength*2;
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

    trie->indexLength=allIndexesLength;
    trie->dataLength=newTrie->dataLength;
    if(highStart<=0x10000) {
        trie->index2NullOffset=0xffff;
    } else {
        trie->index2NullOffset=UTRIE3_INDEX_2_OFFSET+newTrie->index2NullOffset;
    }
    trie->dataNullOffset=(uint16_t)(dataMove+newTrie->dataNullOffset);

    /* set the header fields */
    header=(UTrie3Header *)trie->memory;

    header->signature=UTRIE3_SIG;  // "Tri3"
    header->options=(uint16_t)valueBits;

    header->indexLength=(uint16_t)trie->indexLength;
    header->shiftedDataLength=(uint16_t)(trie->dataLength>>UTRIE3_INDEX_SHIFT);
    header->index2NullOffset=trie->index2NullOffset;
    header->dataNullOffset=trie->dataNullOffset;
    header->shiftedHighStart=(uint16_t)(highStart>>UTRIE3_SHIFT_1);
    header->highValue=trie->highValue;
    header->errorValue=trie->errorValue;

    /* fill the index and data arrays */
    dest16=(uint16_t *)(header+1);
    trie->index=dest16;

    /* write the index-2 array values shifted right by UTRIE3_INDEX_SHIFT, after adding dataMove */
    p=(uint32_t *)newTrie->index2;
    for(i=UTRIE3_INDEX_2_BMP_LENGTH; i>0; --i) {
        *dest16++=(uint16_t)((dataMove + *p++)>>UTRIE3_INDEX_SHIFT);
    }

    /* write UTF-8 2-byte index-2 values, not right-shifted */
    for(i=0; i<(0xc2-0xc0); ++i) {                                  /* C0..C1 */
        *dest16++=trie->dataNullOffset;
    }
    for(; i<(0xe0-0xc0); ++i) {                                     /* C2..DF */
        *dest16++=(uint16_t)(dataMove+newTrie->index2[i]);
    }

    if(highStart>0x10000) {
        int32_t index1Length=(highStart-0x10000)>>UTRIE3_SHIFT_1;
        int32_t index2Offset=UTRIE3_INDEX_2_BMP_LENGTH+UTRIE3_UTF8_2B_INDEX_2_LENGTH+index1Length;

        /* write 16-bit index-1 values for supplementary code points */
        p=(uint32_t *)newTrie->index1+UTRIE3_OMITTED_BMP_INDEX_1_LENGTH;
        for(i=index1Length; i>0; --i) {
            *dest16++=(uint16_t)(UTRIE3_INDEX_2_OFFSET + *p++);
        }

        /*
         * write the index-2 array values for supplementary code points,
         * shifted right by UTRIE3_INDEX_SHIFT, after adding dataMove
         */
        p=(uint32_t *)newTrie->index2+index2Offset;
        for(i=newTrie->index2Length-index2Offset; i>0; --i) {
            *dest16++=(uint16_t)((dataMove + *p++)>>UTRIE3_INDEX_SHIFT);
        }
    }

    /* write the 16/32-bit data array */
    switch(valueBits) {
    case UTRIE3_16_VALUE_BITS:
        /* write 16-bit data values */
        trie->data16=dest16;
        trie->data32=NULL;
        p=newTrie->data;
        for(i=newTrie->dataLength; i>0; --i) {
            *dest16++=(uint16_t)*p++;
        }
        break;
    case UTRIE3_32_VALUE_BITS:
        /* write 32-bit data values */
        trie->data16=NULL;
        trie->data32=(uint32_t *)dest16;
        uprv_memcpy(dest16, newTrie->data, (size_t)newTrie->dataLength*4);
        break;
    default:
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return;
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
 */
U_CAPI int32_t U_EXPORT2
utrie3_swapAnyVersion(const UDataSwapper *ds,
                      const void *inData, int32_t length, void *outData,
                      UErrorCode *pErrorCode) {
    if(U_SUCCESS(*pErrorCode)) {
        switch(utrie3_getVersion(inData, length, TRUE)) {
        case 1:
            return utrie_swap(ds, inData, length, outData, pErrorCode);
        case 2:
            return utrie3_swap(ds, inData, length, outData, pErrorCode);
        default:
            *pErrorCode=U_INVALID_FORMAT_ERROR;
            return 0;
        }
    }
    return 0;
}
