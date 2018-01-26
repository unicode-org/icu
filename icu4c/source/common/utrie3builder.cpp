// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// utrie3builder.cpp (inspired by utrie2_builder.cpp)
// created: 2017dec29 Markus W. Scherer

#define UTRIE3_DEBUG  // TODO
#ifdef UTRIE3_DEBUG
#   include <stdio.h>
#endif

#include "unicode/utypes.h"
#include "unicode/utf16.h"
#include "cmemory.h"
#include "uassert.h"
#include "utrie3.h"
#include "utrie3builder.h"
#include "utrie3_impl.h"

U_NAMESPACE_BEGIN

namespace {

constexpr int32_t MAX_UNICODE = 0x10ffff;

constexpr int32_t UNICODE_LIMIT = 0x110000;
constexpr int32_t BMP_LIMIT = 0x10000;
constexpr int32_t ASCII_LIMIT = 0x80;

constexpr int32_t I_LIMIT = UNICODE_LIMIT >> UTRIE3_SHIFT_2;
constexpr int32_t BMP_I_LIMIT = BMP_LIMIT >> UTRIE3_SHIFT_2;  // == UTRIE3_INDEX_2_BMP_LENGTH
constexpr int32_t ASCII_I_LIMIT = ASCII_LIMIT >> UTRIE3_SHIFT_2;

constexpr uint8_t ALL_SAME = 0;
constexpr uint8_t MIXED = 1;
constexpr uint8_t SAME_AS = 2;
constexpr uint8_t MOVED = 3;
constexpr uint8_t TYPE_MASK = 3;

/** Start with allocation of 16k data entries. */
constexpr int32_t INITIAL_DATA_LENGTH = ((int32_t)1 << 14);

/** Grow about 8x each time. */
constexpr int32_t MEDIUM_DATA_LENGTH = ((int32_t)1 << 17);

/**
 * Maximum length of the build-time data array.
 * One entry per 0x110000 code points.
 */
constexpr int32_t MAX_DATA_LENGTH = UNICODE_LIMIT;

/**
 * Added to an ALL_SAME or MIXED block during compaction if a supplementary block
 * has the same data.
 */
constexpr uint8_t SUPP_DATA=0x10;

class Trie3Builder {
public:
    Trie3Builder(uint32_t initialValue, uint32_t errorValue, UErrorCode &errorCode);
    Trie3Builder(const Trie3Builder &other, UErrorCode &errorCode);
    Trie3Builder(const Trie3Builder &other) = delete;
    ~Trie3Builder();

    Trie3Builder &operator=(const Trie3Builder &other) = delete;

    static Trie3Builder *fromUTrie3(const UTrie3 *trie, UErrorCode &errorCode);

    uint32_t get(UChar32 c) const;
    int32_t getRange(UChar32 start, UTrie3HandleValue *handleValue, const void *context,
                     uint32_t *pValue) const;

    void set(UChar32 c, uint32_t value, UErrorCode &errorCode);
    void setRange(UChar32 start, UChar32 end, uint32_t value, UBool overwrite, UErrorCode &errorCode);

    UTrie3 *build(UTrie3ValueBits valueBits, UErrorCode &errorCode);

private:
    void clear();

    void ensureHighStart(UChar32 c);
    int32_t allocDataBlock(uint32_t value);
    int32_t getDataBlock(UChar32 c);

    void maskValues(uint32_t mask);
    UChar32 findHighStart() const;
    int32_t compactWholeDataBlocks(UChar32 suppHighStart);
    void compactData(UChar32 suppHighStart, UErrorCode &errorCode);
    int32_t compactIndex2(UChar32 suppHighStart, uint16_t index1[]);
    int32_t compactTrie(uint16_t index1[], UErrorCode &errorCode);

    uint32_t *data;
    int32_t dataCapacity, dataLength;
    int32_t dataNullIndex;
    int32_t index2NullOffset;

    uint32_t initialValue;
    uint32_t errorValue;
    UChar32 highStart;
    uint32_t highValue;
public:
    const char *name;  // TODO
private:
    uint8_t flags[0x110000>>UTRIE3_SHIFT_2];
    uint32_t index[0x110000>>UTRIE3_SHIFT_2];
};

Trie3Builder::Trie3Builder(uint32_t iniValue, uint32_t errValue, UErrorCode &errorCode) :
        data(nullptr), dataCapacity(0), dataLength(0), dataNullIndex(-1), index2NullOffset(-1),
        initialValue(iniValue), errorValue(errValue), highStart(0), highValue(initialValue), name("open") {
    if (U_FAILURE(errorCode)) { return; }
    data = (uint32_t *)uprv_malloc(INITIAL_DATA_LENGTH * 4);
    if (data == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    dataCapacity = INITIAL_DATA_LENGTH;
}

Trie3Builder::Trie3Builder(const Trie3Builder &other, UErrorCode &errorCode) :
        data(nullptr), dataCapacity(0), dataLength(0),
        dataNullIndex(other.dataNullIndex), index2NullOffset(other.index2NullOffset),
        initialValue(other.initialValue), errorValue(other.errorValue),
        highStart(other.highStart), highValue(other.highValue), name("builder clone") {
    if (U_FAILURE(errorCode)) { return; }
    data = (uint32_t *)uprv_malloc(other.dataCapacity * 4);
    if (data == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    dataCapacity = other.dataCapacity;
    uprv_memcpy(data, other.data, (size_t)other.dataLength * 4);
    dataLength = other.dataLength;

    int32_t iLimit = highStart >> UTRIE3_SHIFT_2;
    uprv_memcpy(flags, other.flags, iLimit);
    uprv_memcpy(index, other.index, iLimit * 4);
}

Trie3Builder::~Trie3Builder() {
    uprv_free(data);
}

Trie3Builder *Trie3Builder::fromUTrie3(const UTrie3 *trie, UErrorCode &errorCode) {
    // Use the highValue as the initialValue to reduce the highStart.
    uint32_t initialValue = trie->highValue;
    Trie3Builder *builder = new Trie3Builder(initialValue, trie->errorValue, errorCode);
    if (U_FAILURE(errorCode)) {
        delete builder;
        return nullptr;
    }
    UChar32 start = 0, end;
    uint32_t value;
    while ((end = utrie3_getRange(trie, start, nullptr, nullptr, &value)) >= 0) {
        if (value != initialValue) {
            if (start == end) {
                builder->set(start, value, errorCode);
            } else {
                builder->setRange(start, end, value, TRUE, errorCode);
            }
        }
        start = end + 1;
    }
    if (U_SUCCESS(errorCode)) {
        return builder;
    } else {
        delete builder;
        return nullptr;
    }
}

void Trie3Builder::clear() {
    dataLength = 0;
    dataNullIndex = index2NullOffset = -1;
    highStart = 0;
    highValue = initialValue;
}

uint32_t Trie3Builder::get(UChar32 c) const {
    if ((uint32_t)c > MAX_UNICODE) {
        return errorValue;
    }
    if (c >= highStart) {
        return highValue;
    }
    int32_t i = c >> UTRIE3_SHIFT_2;
    if (flags[i] == ALL_SAME) {
        return index[i];
    } else {
        return data[index[i] + (c & UTRIE3_DATA_MASK)];
    }
}

inline uint32_t maybeHandleValue(uint32_t value, uint32_t initialValue, uint32_t nullValue,
                                 UTrie3HandleValue *handleValue, const void *context) {
    if (value == initialValue) {
        value = nullValue;
    } else if (handleValue != nullptr) {
        value = handleValue(context, value);
    }
    return value;
}

int32_t Trie3Builder::getRange(UChar32 start,
                               UTrie3HandleValue *handleValue, const void *context,
                               uint32_t *pValue) const {
    if ((uint32_t)start > MAX_UNICODE) {
        return U_SENTINEL;
    }
    if (start >= highStart) {
        if (pValue != nullptr) {
            uint32_t value = highValue;
            if (handleValue != nullptr) { value = handleValue(context, value); }
            *pValue = value;
        }
        return MAX_UNICODE;
    }
    uint32_t nullValue = initialValue;
    if (handleValue != nullptr) { nullValue = handleValue(context, nullValue); }
    UChar32 c = start;
    uint32_t value;
    bool haveValue = false;
    int32_t i = c >> UTRIE3_SHIFT_2;
    do {
        if (flags[i] == ALL_SAME) {
            uint32_t value2 = maybeHandleValue(index[i], initialValue, nullValue,
                                               handleValue, context);
            if (haveValue) {
                if (value2 != value) {
                    return c - 1;
                }
            } else {
                value = value2;
                if (pValue != nullptr) { *pValue = value; }
                haveValue = true;
            }
            c = (c + UTRIE3_DATA_BLOCK_LENGTH) & ~UTRIE3_DATA_MASK;
        } else /* MIXED */ {
            int32_t di = index[i] + (c & UTRIE3_DATA_MASK);
            uint32_t value2 = maybeHandleValue(data[di], initialValue, nullValue,
                                               handleValue, context);
            if (haveValue) {
                if (value2 != value) {
                    return c - 1;
                }
            } else {
                value = value2;
                if (pValue != nullptr) { *pValue = value; }
                haveValue = true;
            }
            while ((++c & UTRIE3_DATA_MASK) != 0) {
                if (maybeHandleValue(data[++di], initialValue, nullValue,
                                     handleValue, context) != value) {
                    return c - 1;
                }
            }
        }
        ++i;
    } while (c < highStart);
    U_ASSERT(haveValue);
    if (maybeHandleValue(highValue, initialValue, nullValue,
                         handleValue, context) != value) {
        return c - 1;
    } else {
        return MAX_UNICODE;
    }
}

void
writeBlock(uint32_t *block, uint32_t value) {
    uint32_t *limit=block+UTRIE3_DATA_BLOCK_LENGTH;
    while(block<limit) {
        *block++=value;
    }
}

void Trie3Builder::ensureHighStart(UChar32 c) {
    if(c>=highStart) {
        // Round up to a full block.
        c=(c+UTRIE3_DATA_BLOCK_LENGTH)&~UTRIE3_DATA_MASK;
        int32_t i=highStart>>UTRIE3_SHIFT_2;
        int32_t iLimit=c>>UTRIE3_SHIFT_2;
        do {
            flags[i]=ALL_SAME;
            index[i]=initialValue;
        } while(++i<iLimit);
        highStart=c;
    }
}

int32_t Trie3Builder::allocDataBlock(uint32_t value) {
    int32_t newBlock=dataLength;
    int32_t newTop=newBlock+UTRIE3_DATA_BLOCK_LENGTH;
    if(newTop>dataCapacity) {
        int32_t capacity;
        if(dataCapacity<MEDIUM_DATA_LENGTH) {
            capacity=MEDIUM_DATA_LENGTH;
        } else if(dataCapacity<MAX_DATA_LENGTH) {
            capacity=MAX_DATA_LENGTH;
        } else {
            // Should never occur.
            // Either MAX_DATA_LENGTH is incorrect,
            // or the code writes more values than should be possible.
            return -1;
        }
        uint32_t *newData = (uint32_t *)uprv_malloc(capacity * 4);
        if (newData == nullptr) {
            return -1;
        }
        uprv_memcpy(newData, data, (size_t)dataLength * 4);
        uprv_free(data);
        data = newData;
        dataCapacity = capacity;
    }
    dataLength=newTop;
    writeBlock(data+newBlock, value);
    return newBlock;
}

/**
 * No error checking for illegal arguments.
 *
 * @return -1 if no new data block available (out of memory in data array)
 * @internal
 */
int32_t Trie3Builder::getDataBlock(UChar32 c) {
    int32_t i=c>>UTRIE3_SHIFT_2;
    if(flags[i]==MIXED) {
        return index[i];
    }
    int32_t newBlock=allocDataBlock(index[i]);
    if(newBlock>=0) {
        flags[i]=MIXED;
        index[i]=newBlock;
    }
    return newBlock;
}

void Trie3Builder::set(UChar32 c, uint32_t value, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) {
        return;
    }
    if((uint32_t)c>MAX_UNICODE) {
        errorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    ensureHighStart(c);
    int32_t block=getDataBlock(c);
    if(block<0) {
        errorCode=U_MEMORY_ALLOCATION_ERROR;
        return;
    }

    data[block+(c&UTRIE3_DATA_MASK)]=value;
}

/**
 * initialValue is ignored if overwrite=TRUE
 * @internal
 */
void
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

void Trie3Builder::setRange(UChar32 start, UChar32 end,
                            uint32_t value, UBool overwrite,
                            UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) {
        return;
    }
    if((uint32_t)start>MAX_UNICODE || (uint32_t)end>MAX_UNICODE || start>end) {
        errorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    if(!overwrite && value==initialValue) {
        return; /* nothing to do */
    }
    ensureHighStart(end);

    UChar32 limit=end+1;
    if(start&UTRIE3_DATA_MASK) {
        /* set partial block at [start..following block boundary[ */
        int32_t block=getDataBlock(start);
        if(block<0) {
            errorCode=U_MEMORY_ALLOCATION_ERROR;
            return;
        }

        UChar32 nextStart=(start+UTRIE3_DATA_MASK)&~UTRIE3_DATA_MASK;
        if(nextStart<=limit) {
            fillBlock(data+block, start&UTRIE3_DATA_MASK, UTRIE3_DATA_BLOCK_LENGTH,
                      value, initialValue, overwrite);
            start=nextStart;
        } else {
            fillBlock(data+block, start&UTRIE3_DATA_MASK, limit&UTRIE3_DATA_MASK,
                      value, initialValue, overwrite);
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
        if(flags[i]==ALL_SAME) {
            if(overwrite || index[i]==initialValue) {
                index[i]=value;
            }
        } else /* MIXED */ {
            fillBlock(data+index[i], 0, UTRIE3_DATA_BLOCK_LENGTH,
                      value, initialValue, overwrite);
        }
        start+=UTRIE3_DATA_BLOCK_LENGTH;
    }

    if(rest>0) {
        /* set partial block at [last block boundary..limit[ */
        int32_t block=getDataBlock(start);
        if(block<0) {
            errorCode=U_MEMORY_ALLOCATION_ERROR;
            return;
        }

        fillBlock(data+block, 0, rest, value, initialValue, overwrite);
    }
}

/* compaction --------------------------------------------------------------- */

void Trie3Builder::maskValues(uint32_t mask) {
    initialValue &= mask;
    highValue &= mask;
    // Leave the errorValue as is: It is not stored in the data array,
    // and an error value outside the normal value range might be useful.
    int32_t iLimit = highStart >> UTRIE3_SHIFT_2;
    for (int32_t i = 0; i < iLimit; ++i) {
        if (flags[i] == ALL_SAME) {
            index[i] &= mask;
        }
    }
    for (int32_t i = 0; i < dataLength; ++i) {
        data[i] &= mask;
    }
}

inline bool
equal_uint32(const uint32_t *s, const uint32_t *t, int32_t length) {
    while(length>0 && *s==*t) {
        ++s;
        ++t;
        --length;
    }
    return length == 0;
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
    U_ASSERT(overlap <= length);
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
UChar32 Trie3Builder::findHighStart() const {
    int32_t i=highStart>>UTRIE3_SHIFT_2;
    while(i>0) {
        bool match;
        if(flags[--i]==ALL_SAME) {
            match= index[i]==highValue;
        } else /* MIXED */ {
            const uint32_t *p=data+index[i];
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

class AllSameBlocks {
public:
    static constexpr int32_t NEW_UNIQUE = -1;
    static constexpr int32_t OVERFLOW = -2;

    AllSameBlocks() : length(0), mostRecent(-1) {}

    int32_t findOrAdd(int32_t index, uint32_t value) {
        if (mostRecent >= 0 && values[mostRecent] == value) {
            ++refCounts[mostRecent];
            return indexes[mostRecent];
        }
        for (int32_t i = 0; i < length; ++i) {
            if (values[i] == value) {
                mostRecent = i;
                ++refCounts[i];
                return indexes[i];
            }
        }
        if (length == CAPACITY) {
            return OVERFLOW;
        }
        mostRecent = length;
        indexes[length] = index;
        values[length] = value;
        refCounts[length++] = 1;
        return NEW_UNIQUE;
    }

    /** Replaces the block which has the lowest reference count. */
    void add(int32_t index, uint32_t value) {
        U_ASSERT(length == CAPACITY);
        int32_t least = -1;
        int32_t leastCount = I_LIMIT;
        for (int32_t i = 0; i < length; ++i) {
            U_ASSERT(values[i] != value);
            if (refCounts[i] < leastCount) {
                least = i;
                leastCount = refCounts[i];
            }
        }
        U_ASSERT(least >= 0);
        mostRecent = least;
        indexes[least] = index;
        values[least] = value;
        refCounts[least] = 1;
    }

    int32_t findMostUsed() const {
        if (length == 0) { return -1; }
        int32_t max = -1;
        int32_t maxCount = 0;
        for (int32_t i = 0; i < length; ++i) {
            if (refCounts[i] > maxCount) {
                max = i;
                maxCount = refCounts[i];
            }
        }
        return indexes[max];
    }

private:
    static constexpr int32_t CAPACITY = 32;

    int32_t length;
    int32_t mostRecent;

    int32_t indexes[CAPACITY];
    uint32_t values[CAPACITY];
    int32_t refCounts[CAPACITY];
};

int32_t Trie3Builder::compactWholeDataBlocks(UChar32 suppHighStart) {
    AllSameBlocks allSameBlocks;
#ifdef UTRIE3_DEBUG
    bool overflow = false;
#endif

    int32_t newDataLength=0;
    int32_t iLimit=suppHighStart>>UTRIE3_SHIFT_2;
    for(int32_t i=0; i<iLimit; ++i) {
        uint32_t value=index[i];
        if(flags[i]==MIXED) {
            // Really mixed?
            const uint32_t *p = data + value;
            value = *p;
            if (allValuesSameAs(p + 1, UTRIE3_DATA_BLOCK_LENGTH - 1, value)) {
                flags[i]=ALL_SAME;
                index[i]=value;
                // Fall through to ALL_SAME handling.
            } else {
                // Is there another whole mixed block with the same data?
                for(int32_t j=0;; ++j) {
                    if(j==i) {
                        // Unique mixed-value block.
                        newDataLength+=UTRIE3_DATA_BLOCK_LENGTH;
                        break;
                    }
                    if((flags[j]&TYPE_MASK)==MIXED &&
                            equal_uint32(p, data+index[j], UTRIE3_DATA_BLOCK_LENGTH)) {
                        if(i>=BMP_I_LIMIT) {
                            flags[j]|=SUPP_DATA;
                        }
                        flags[i]=SAME_AS;
                        index[i]=j;
                        break;
                    }
                }
                continue;
            }
        } else {
            U_ASSERT(flags[i]==ALL_SAME);
        }
        // Is there another ALL_SAME block with the same value?
        int32_t other = allSameBlocks.findOrAdd(i, value);
        if (other == AllSameBlocks::OVERFLOW) {
            // The fixed-size array overflowed. Slow check for a duplicate block.
#ifdef UTRIE3_DEBUG
            if (!overflow) {
                puts("UTrie3 AllSameBlocks overflow");
                overflow = true;
            }
#endif
            for (other = 0;; ++other) {
                if (other == i) {
                    other = AllSameBlocks::NEW_UNIQUE;
                    allSameBlocks.add(i, value);
                    break;
                }
                if ((flags[other] & TYPE_MASK) == ALL_SAME &&
                        index[other] == value) {
                    allSameBlocks.add(other, value);
                    break;
                }
            }
        }
        if (other >= 0) {
            if (i >= BMP_I_LIMIT) {
                flags[other] |= SUPP_DATA;
            }
            flags[i] = SAME_AS;
            index[i] = other;
        } else {
            // New unique same-value block.
            newDataLength += UTRIE3_DATA_BLOCK_LENGTH;
        }
    }
    dataNullIndex = allSameBlocks.findMostUsed();
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
void Trie3Builder::compactData(UChar32 suppHighStart, UErrorCode &errorCode) {
    uint32_t asciiData[ASCII_LIMIT];
    for(int32_t i=0; i<ASCII_LIMIT; ++i) {
        asciiData[i] = get(i);
    }

    // First we look for which data blocks have the same value repeated over the whole block,
    // deduplicate whole blocks, and get an upper bound for the necessary data array length.
    // We deduplicate whole blocks first so that ones shared between BMP and supplementary
    // code points are found before different granularity alignment may prevent
    // sharing in the following code.
    int32_t newDataLength = compactWholeDataBlocks(suppHighStart);
    newDataLength += ASCII_LIMIT;
    uint32_t *newData = (uint32_t *)uprv_malloc(newDataLength * 4);
    if (newData == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    uprv_memcpy(newData, asciiData, sizeof(asciiData));

    if (dataNullIndex >= 0) {
#ifdef UTRIE3_DEBUG
        if (index[dataNullIndex] != initialValue) {
            printf("UTrie3 initialValue %lx -> more common %lx\n",
                   (long)initialValue, (long)index[dataNullIndex]);
        }
        initialValue = index[dataNullIndex];
#endif
    }

#ifdef UTRIE3_DEBUG
    int32_t countSame=0, sumOverlaps=0;
#endif

    // linear ASCII data
    int32_t start=0;
    int32_t newStart=ASCII_LIMIT;
    for(int32_t i=0; start<newStart; start+=UTRIE3_DATA_BLOCK_LENGTH, ++i) {
        flags[i]=MOVED;
        index[i]=start;
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
                    // We could use any value for padding.
                    // Repeat the last data value to increase chances for
                    // overlap across this padding.
                    newData[newStart] = newData[newStart - 1];
                    ++newStart;
                }
                granularity=UTRIE3_DATA_GRANULARITY;

                // Now write data blocks that are used for supplementary code points.
                i=ASCII_I_LIMIT;
                iLimit=suppHighStart>>UTRIE3_SHIFT_2;
            } else {
                break;
            }
        }
        uint8_t f = flags[i];
        if(granularity!=1) {
            // BMP: Look for flags without SUPP_DATA. Then look for the remaining ones.
            f &= TYPE_MASK;
        }
        if(f == ALL_SAME) {
            uint32_t value=index[i];
            int32_t n = findAllSameBlock(newData, newStart, value, UTRIE3_DATA_BLOCK_LENGTH, granularity);
            if (n >= 0) {
                DEBUG_DO(++countSame);
                index[i]=n;
            } else {
                n=getAllSameOverlap(newData, newStart, value, UTRIE3_DATA_BLOCK_LENGTH, granularity);
                DEBUG_DO(sumOverlaps+=n);
                index[i]=newStart-n;
                while(n<UTRIE3_DATA_BLOCK_LENGTH) {
                    newData[newStart++]=value;
                    ++n;
                }
            }
            flags[i]=MOVED;
        } else if(f == MIXED) {
            const uint32_t *block=data+index[i];
            int32_t n = findSameBlock(newData, newStart, block, UTRIE3_DATA_BLOCK_LENGTH, granularity);
            if (n >= 0) {
                DEBUG_DO(++countSame);
                index[i]=n;
            } else {
                n=getOverlap(newData, newStart, block, UTRIE3_DATA_BLOCK_LENGTH, granularity);
                DEBUG_DO(sumOverlaps+=n);
                index[i]=newStart-n;
                while(n<UTRIE3_DATA_BLOCK_LENGTH) {
                    newData[newStart++]=block[n++];
                }
            }
            flags[i]=MOVED;
        }
    }
    U_ASSERT(newStart <= newDataLength);

    for(int32_t i=ASCII_I_LIMIT; i<iLimit; ++i) {
        if(flags[i]==SAME_AS) {
            uint32_t j=index[i];
            U_ASSERT(flags[j]==MOVED);
            flags[i]=MOVED;
            index[i]=index[j];
        } else {
            U_ASSERT(flags[i]==MOVED);
        }
    }

#ifdef UTRIE3_DEBUG
    /* we saved some space */
    printf("compacting UTrie3: count of 32-bit data words %lu->%lu  countSame=%ld  sumOverlaps=%ld\n",
            (long)dataLength, (long)newStart, (long)countSame, (long)sumOverlaps);
#endif

    uprv_free(data);
    data = newData;
    dataCapacity = newDataLength;
    dataLength = newStart;
}

int32_t Trie3Builder::compactIndex2(UChar32 suppHighStart, uint16_t index1[]) {
    // The BMP index is linear, and the index-1 table is used only for supplementary code points.
    if (suppHighStart <= BMP_LIMIT) {
        return BMP_I_LIMIT;
    }

    uint32_t dataNullOffset = dataNullIndex >= 0 ? index[dataNullIndex] : UTRIE3_NO_DATA_NULL_OFFSET;

    // Compact the supplementary part of index[].
    int32_t start = BMP_I_LIMIT;
    int32_t newStart = start;
    int32_t iLimit = suppHighStart >> UTRIE3_SHIFT_2;
    // Set index-1 entries to the new starts of index-2 blocks, offset by the index-1 length.
    // The index-1 table is inserted before the supplementary index-2 blocks later,
    // when writing the final structure.
    U_ASSERT(suppHighStart > BMP_LIMIT);
    int32_t offset = (suppHighStart - BMP_LIMIT) >> UTRIE3_SHIFT_1;

    for (; start < iLimit; start += UTRIE3_INDEX_2_BLOCK_LENGTH) {
        int32_t i2;
        // Is this the same as the index-2 null block?
        if (index2NullOffset >= 0 &&
                allValuesSameAs(index + start, UTRIE3_INDEX_2_BLOCK_LENGTH, dataNullOffset)) {
            i2 = index2NullOffset;
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
                    if (newStart == BMP_I_LIMIT) {
                        // No overlap across the BMP boundary.
                        // Index shifting differs, and the index-1 table will be inserted there.
                        n = 0;
                    } else {
                        n = getOverlap(index + BMP_I_LIMIT, newStart - BMP_I_LIMIT,
                                       index + start, UTRIE3_INDEX_2_BLOCK_LENGTH, 1);
                    }
                    i2 = offset + (newStart - n);
                    if (n > 0 || newStart != start) {
                        while (n < UTRIE3_INDEX_2_BLOCK_LENGTH) {
                            index[newStart++] = index[start + n++];
                        }
                    } else {
                        newStart += UTRIE3_INDEX_2_BLOCK_LENGTH;
                    }
                }
            }
            // Is this the first index-2 block with all dataNullOffset?
            if (index2NullOffset < 0 && dataNullIndex >= 0 &&
                    allValuesSameAs(index + (i2 - offset), UTRIE3_INDEX_2_BLOCK_LENGTH,
                                    dataNullOffset)) {
                index2NullOffset = i2;
            }
        }
        index1[(start >> UTRIE3_SHIFT_1_2) - UTRIE3_OMITTED_BMP_INDEX_1_LENGTH] = i2;
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

    return length;
}

int32_t Trie3Builder::compactTrie(uint16_t index1[], UErrorCode &errorCode) {
    // Find the real highStart and round it up.
    highValue = get(MAX_UNICODE);
    highStart = findHighStart();
    if ((highStart & (UTRIE3_CP_PER_INDEX_1_ENTRY - 1)) != 0) {
        int32_t i = highStart >> UTRIE3_SHIFT_2;
        do {
            flags[i] = ALL_SAME;
            index[i] = highValue;
            ++i;
            highStart += UTRIE3_DATA_BLOCK_LENGTH;
        } while((highStart & (UTRIE3_CP_PER_INDEX_1_ENTRY - 1)) != 0);
    }
    if (highStart == UNICODE_LIMIT) {
        highValue = initialValue;
    }

#ifdef UTRIE3_DEBUG
    printf("UTrie3: highStart U+%06lx  highValue 0x%lx  initialValue 0x%lx\n",
            (long)highStart, (long)highValue, (long)initialValue);
#endif

    // We always store indexes and data values for the BMP.
    // Use a version of highStart pinned to the supplementary range.
    UChar32 suppHighStart;
    if (highStart <= BMP_LIMIT) {
        for (int32_t i = (highStart >> UTRIE3_SHIFT_2); i < BMP_I_LIMIT; ++i) {
            flags[i] = ALL_SAME;
            index[i] = highValue;
        }
        suppHighStart = BMP_LIMIT;
    } else {
        suppHighStart = highStart;
    }

    compactData(suppHighStart, errorCode);
    if(U_FAILURE(errorCode)) { return 0; }
    return compactIndex2(suppHighStart, index1);
}

UTrie3 *Trie3Builder::build(UTrie3ValueBits valueBits, UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }
    if (valueBits > UTRIE3_32_VALUE_BITS) {
        errorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return nullptr;
    }

    // The builder always stores 32-bit values.
    // When we build a UTrie3 for a smaller value width, we first mask off unused bits
    // before compacting the data.
    if (valueBits != UTRIE3_32_VALUE_BITS) {
        maskValues(0xffff);
    }

    uint16_t index1[UTRIE3_MAX_INDEX_1_LENGTH];
    int32_t indexLength = compactTrie(index1, errorCode);
    if(U_FAILURE(errorCode)) {
        clear();
        return nullptr;
    }

    int32_t dataMove;  /* >0 if the data is moved to the end of the index array */
    if(valueBits==UTRIE3_16_VALUE_BITS) {
        dataMove = indexLength;
    } else {
        dataMove=0;
    }

    // Are all shifted supplementary indexes within limits?
    if (((dataMove + dataLength) >> UTRIE3_INDEX_SHIFT) > 0xffff) {
        errorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        clear();
        return nullptr;
    }

    // Are all unshifted BMP indexes within limits?
    const uint32_t *p=index;
    int32_t i;
    for(i=UTRIE3_INDEX_2_BMP_LENGTH; i>0; --i) {
        if ((dataMove + *p++) > 0xffff) {
            errorCode=U_INDEX_OUTOFBOUNDS_ERROR;
            clear();
            return nullptr;
        }
    }

    // Calculate the total length of the UTrie3 as a single memory block.
    int32_t length = sizeof(UTrie3) + indexLength * 2;
    if (valueBits == UTRIE3_16_VALUE_BITS) {
        length += dataLength * 2;
    } else {
        length += dataLength * 4;
    }

    char *bytes = (char *)uprv_malloc(length);
    if (bytes == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        clear();
        return nullptr;
    }
    UTrie3 *trie = reinterpret_cast<UTrie3 *>(bytes);
    uprv_memset(trie, 0, sizeof(UTrie3));
    trie->indexLength = indexLength;
    trie->dataLength = dataLength;
    if (index2NullOffset >= 0) {
        trie->index2NullOffset = index2NullOffset;
    } else {
        trie->index2NullOffset = UTRIE3_NO_INDEX2_NULL_OFFSET;
    }
    trie->dataNullOffset = dataNullIndex >= 0 ?
        dataMove + index[dataNullIndex] : UTRIE3_NO_DATA_NULL_OFFSET;
    trie->initialValue = initialValue;
    trie->errorValue = errorValue;

    trie->highStart = highStart;
    trie->highStartLead16 = U16_LEAD(highStart);
    trie->shiftedHighStart = highStart >> UTRIE3_SHIFT_1;
    trie->highValue = highValue;
    bytes += sizeof(UTrie3);

    /* fill the index and data arrays */
    uint16_t *dest16 = (uint16_t *)bytes;
    trie->index = dest16;

    // Write BMP index-2 array values, not right-shifted, after adding dataMove.
    for (i = 0; i < UTRIE3_INDEX_2_BMP_LENGTH; ++i) {
        *dest16++ = (uint16_t)(dataMove + index[i]);
    }

    if(highStart>BMP_LIMIT) {
        int32_t index1Length=(highStart-BMP_LIMIT)>>UTRIE3_SHIFT_1;

        /* write 16-bit index-1 values for supplementary code points */
        uprv_memcpy(dest16, index1, index1Length * 2);
        dest16 += index1Length;

        /*
         * write the index-2 array values for supplementary code points,
         * shifted right by UTRIE3_INDEX_SHIFT, after adding dataMove
         */
        int32_t iLimit = indexLength - index1Length;
        for (i = BMP_I_LIMIT; i < iLimit; ++i) {
            *dest16++ = (uint16_t)((dataMove + index[i]) >> UTRIE3_INDEX_SHIFT);
        }
    }
    bytes += indexLength * 2;

    /* write the 16/32-bit data array */
    p = data;
    switch (valueBits) {
    case UTRIE3_16_VALUE_BITS:
        /* write 16-bit data values */
        trie->data16 = dest16;
        trie->data32 = nullptr;
        for (i = dataLength; i > 0; --i) {
            *dest16++ = (uint16_t)*p++;
        }
        break;
    case UTRIE3_32_VALUE_BITS:
        /* write 32-bit data values */
        trie->data16 = nullptr;
        trie->data32 = (uint32_t *)bytes;
        uprv_memcpy(dest16, p, (size_t)dataLength * 4);
        break;
    default:
        // Will not occur, valueBits checked at the beginning.
        break;
    }

    trie->name = name;

#ifdef UTRIE3_DEBUG
    utrie3_printLengths(trie, "");
#endif

    clear();
    return trie;
}

}  // namespace

U_NAMESPACE_END

U_NAMESPACE_USE

U_CAPI UTrie3Builder * U_EXPORT2
utrie3bld_open(uint32_t initialValue, uint32_t errorValue, UErrorCode *pErrorCode) {
    if (U_FAILURE(*pErrorCode)) {
        return nullptr;
    }
    Trie3Builder *builder = new Trie3Builder(initialValue, errorValue, *pErrorCode);
    if (U_FAILURE(*pErrorCode)) {
        delete builder;
        return nullptr;
    }
    return reinterpret_cast<UTrie3Builder *>(builder);
}

U_CAPI UTrie3Builder * U_EXPORT2
utrie3bld_clone(const UTrie3Builder *other, UErrorCode *pErrorCode) {
    if (U_FAILURE(*pErrorCode)) {
        return nullptr;
    }
    if (other == nullptr) {
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return nullptr;
    }
    Trie3Builder *clone = new Trie3Builder(
        *reinterpret_cast<const Trie3Builder *>(other), *pErrorCode);
    if (U_FAILURE(*pErrorCode)) {
        delete clone;
        return nullptr;
    }
    return reinterpret_cast<UTrie3Builder *>(clone);
}

U_CAPI void U_EXPORT2
utrie3bld_close(UTrie3Builder *builder) {
    delete reinterpret_cast<Trie3Builder *>(builder);
}

U_CAPI UTrie3Builder * U_EXPORT2
utrie3bld_fromUTrie3(const UTrie3 *trie, UErrorCode *pErrorCode) {
    if (U_FAILURE(*pErrorCode)) {
        return nullptr;
    }
    if (trie == nullptr) {
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return nullptr;
    }
    return reinterpret_cast<UTrie3Builder *>(Trie3Builder::fromUTrie3(trie, *pErrorCode));
}

U_CAPI uint32_t U_EXPORT2
utrie3bld_get(const UTrie3Builder *builder, UChar32 c) {
    return reinterpret_cast<const Trie3Builder *>(builder)->get(c);
}

U_CAPI int32_t U_EXPORT2
utrie3bld_getRange(const UTrie3Builder *builder, UChar32 start,
                   UTrie3HandleValue *handleValue, const void *context, uint32_t *pValue) {
    return reinterpret_cast<const Trie3Builder *>(builder)->getRange(start, handleValue, context, pValue);
}

U_CAPI void U_EXPORT2
utrie3bld_set(UTrie3Builder *builder, UChar32 c, uint32_t value, UErrorCode *pErrorCode) {
    if (U_FAILURE(*pErrorCode)) {
        return;
    }
    reinterpret_cast<Trie3Builder *>(builder)->set(c, value, *pErrorCode);
}

U_CAPI void U_EXPORT2
utrie3bld_setRange(UTrie3Builder *builder, UChar32 start, UChar32 end,
                   uint32_t value, UBool overwrite, UErrorCode *pErrorCode) {
    if (U_FAILURE(*pErrorCode)) {
        return;
    }
    reinterpret_cast<Trie3Builder *>(builder)->setRange(start, end, value, overwrite, *pErrorCode);
}

/* Compact and internally serialize the trie. */
U_CAPI UTrie3 * U_EXPORT2
utrie3bld_build(UTrie3Builder *builder, UTrie3ValueBits valueBits, UErrorCode *pErrorCode) {
    if (U_FAILURE(*pErrorCode)) {
        return nullptr;
    }
    return reinterpret_cast<Trie3Builder *>(builder)->build(valueBits, *pErrorCode);
}

U_CFUNC void utrie3bld_setName(UTrie3Builder *builder, const char *name) {
    reinterpret_cast<Trie3Builder *>(builder)->name = name;  // TODO
}

/*
 * This is here to avoid a dependency from utrie3.cpp on utrie.cpp.
 * This file already depends on utrie.cpp.
 * Otherwise, this should be in utrie3.cpp right after utrie3_swap().
 * TODO: find a better place
 */
U_CAPI int32_t U_EXPORT2
utrie3_swapAnyVersion(const UDataSwapper *ds,
                      const void *inData, int32_t length, void *outData,
                      UErrorCode *pErrorCode) {
    if(U_SUCCESS(*pErrorCode)) {
        switch(utrie3_getVersion(inData, length, TRUE)) {
//         case 1:
//             return utrie_swap(ds, inData, length, outData, pErrorCode);
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
