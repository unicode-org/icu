// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// utrie3builder.cpp (inspired by utrie2_builder.cpp)
// created: 2017dec29 Markus W. Scherer

#define UTRIE3_DEBUG  // TODO
#ifdef UTRIE3_DEBUG
#   include <stdio.h>
#endif

#include "unicode/utypes.h"
#include "unicode/uobject.h"
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

constexpr int32_t I_LIMIT = UNICODE_LIMIT >> UTRIE3_SUPP_SHIFT_2;
constexpr int32_t BMP_I_LIMIT = BMP_LIMIT >> UTRIE3_SUPP_SHIFT_2;
constexpr int32_t ASCII_I_LIMIT = ASCII_LIMIT >> UTRIE3_SUPP_SHIFT_2;

constexpr int32_t SUPP_DATA_BLOCKS_PER_BMP_BLOCK = (1 << (UTRIE3_BMP_SHIFT - UTRIE3_SUPP_SHIFT_2));

// Flag values for data blocks.
constexpr uint8_t ALL_SAME = 0;
constexpr uint8_t MIXED = 1;
constexpr uint8_t SAME_AS = 2;

/** Start with allocation of 16k data entries. */
constexpr int32_t INITIAL_DATA_LENGTH = ((int32_t)1 << 14);

/** Grow about 8x each time. */
constexpr int32_t MEDIUM_DATA_LENGTH = ((int32_t)1 << 17);

/**
 * Maximum length of the build-time data array.
 * One entry per 0x110000 code points.
 */
constexpr int32_t MAX_DATA_LENGTH = UNICODE_LIMIT;

// Flag values for index-2 blocks while compacting/building.
constexpr uint8_t I2_NULL = 0;
constexpr uint8_t I2_BMP = 1;
constexpr uint8_t I2_16 = 2;
constexpr uint8_t I2_18 = 3;

// This maximum index length guarantees that the index-2 null offset
// cannot be the start of an actual index-2 block.
constexpr int32_t MAX_INDEX_LENGTH = UTRIE3_NO_INDEX2_NULL_OFFSET + UTRIE3_INDEX_2_BLOCK_LENGTH - 1;

constexpr int32_t INDEX_2_18BIT_BLOCK_LENGTH = UTRIE3_INDEX_2_BLOCK_LENGTH + UTRIE3_INDEX_2_BLOCK_LENGTH / 8;

class AllSameBlocks;

class Trie3Builder : public UMemory {
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

    bool ensureHighStart(UChar32 c);
    int32_t allocDataBlock(int32_t blockLength);
    int32_t getDataBlock(int32_t i);

    void maskValues(uint32_t mask);
    UChar32 findHighStart() const;
    int32_t compactWholeDataBlocks(AllSameBlocks &allSameBlocks);
    int32_t compactData(uint32_t *newData);
    int32_t compactIndex2(UErrorCode &errorCode);
    int32_t compactTrie(UErrorCode &errorCode);

    uint32_t *index;
    int32_t indexCapacity;
    int32_t index2NullOffset;
    uint32_t *data;
    int32_t dataCapacity, dataLength;
    int32_t dataNullOffset;

    uint32_t origInitialValue;
    uint32_t initialValue;
    uint32_t errorValue;
    UChar32 highStart;
    uint32_t highValue;
#ifdef UTRIE3_DEBUG
public:
    const char *name;  // TODO
#endif
private:
    /** Temporary array while building the final data. */
    uint16_t *index16;
    uint8_t flags[UNICODE_LIMIT >> UTRIE3_SUPP_SHIFT_2];
};

Trie3Builder::Trie3Builder(uint32_t iniValue, uint32_t errValue, UErrorCode &errorCode) :
        index(nullptr), indexCapacity(0), index2NullOffset(-1),
        data(nullptr), dataCapacity(0), dataLength(0), dataNullOffset(-1),
        origInitialValue(iniValue), initialValue(iniValue), errorValue(errValue),
        highStart(0), highValue(initialValue), name("open"), index16(nullptr) {
    if (U_FAILURE(errorCode)) { return; }
    index = (uint32_t *)uprv_malloc(BMP_I_LIMIT * 4);
    data = (uint32_t *)uprv_malloc(INITIAL_DATA_LENGTH * 4);
    if (index == nullptr || data == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    indexCapacity = BMP_I_LIMIT;
    dataCapacity = INITIAL_DATA_LENGTH;
}

Trie3Builder::Trie3Builder(const Trie3Builder &other, UErrorCode &errorCode) :
        index(nullptr), indexCapacity(0), index2NullOffset(other.index2NullOffset),
        data(nullptr), dataCapacity(0), dataLength(0), dataNullOffset(other.dataNullOffset),
        origInitialValue(other.origInitialValue), initialValue(other.initialValue),
        errorValue(other.errorValue),
        highStart(other.highStart), highValue(other.highValue), name("builder clone"),
        index16(nullptr) {
    if (U_FAILURE(errorCode)) { return; }
    int32_t iCapacity = highStart <= BMP_LIMIT ? BMP_I_LIMIT : I_LIMIT;
    index = (uint32_t *)uprv_malloc(iCapacity * 4);
    data = (uint32_t *)uprv_malloc(other.dataCapacity * 4);
    if (index == nullptr || data == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    indexCapacity = iCapacity;
    dataCapacity = other.dataCapacity;

    int32_t iLimit = highStart >> UTRIE3_SUPP_SHIFT_2;
    uprv_memcpy(flags, other.flags, iLimit);
    uprv_memcpy(index, other.index, iLimit * 4);
    uprv_memcpy(data, other.data, (size_t)other.dataLength * 4);
    dataLength = other.dataLength;
}

Trie3Builder::~Trie3Builder() {
    uprv_free(index);
    uprv_free(data);
    uprv_free(index16);
}

Trie3Builder *Trie3Builder::fromUTrie3(const UTrie3 *trie, UErrorCode &errorCode) {
    // Use the highValue as the initialValue to reduce the highStart.
    uint32_t errorValue;
    uint32_t initialValue;
    if (trie->data32 != nullptr) {
        errorValue = trie->data32[trie->dataLength - UTRIE3_ERROR_VALUE_NEG_DATA_OFFSET];
        initialValue = trie->data32[trie->dataLength - UTRIE3_HIGH_VALUE_NEG_DATA_OFFSET];
    } else {
        errorValue = trie->data16[trie->dataLength - UTRIE3_ERROR_VALUE_NEG_DATA_OFFSET];
        initialValue = trie->data16[trie->dataLength - UTRIE3_HIGH_VALUE_NEG_DATA_OFFSET];
    }
    Trie3Builder *builder = new Trie3Builder(initialValue, errorValue, errorCode);
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
    index2NullOffset = dataNullOffset = -1;
    dataLength = 0;
    highValue = initialValue = origInitialValue;
    highStart = 0;
    uprv_free(index16);
    index16 = nullptr;
}

uint32_t Trie3Builder::get(UChar32 c) const {
    if ((uint32_t)c > MAX_UNICODE) {
        return errorValue;
    }
    if (c >= highStart) {
        return highValue;
    }
    int32_t i = c >> UTRIE3_SUPP_SHIFT_2;
    if (flags[i] == ALL_SAME) {
        return index[i];
    } else {
        return data[index[i] + (c & UTRIE3_SUPP_DATA_MASK)];
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
    int32_t i = c >> UTRIE3_SUPP_SHIFT_2;
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
            c = (c + UTRIE3_SUPP_DATA_BLOCK_LENGTH) & ~UTRIE3_SUPP_DATA_MASK;
        } else /* MIXED */ {
            int32_t di = index[i] + (c & UTRIE3_SUPP_DATA_MASK);
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
            while ((++c & UTRIE3_SUPP_DATA_MASK) != 0) {
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
    uint32_t *limit = block + UTRIE3_SUPP_DATA_BLOCK_LENGTH;
    while (block < limit) {
        *block++ = value;
    }
}

bool Trie3Builder::ensureHighStart(UChar32 c) {
    if (c >= highStart) {
        // Round up to a UTRIE3_CP_PER_INDEX_1_ENTRY boundary to simplify compaction.
        c = (c + UTRIE3_CP_PER_INDEX_1_ENTRY) & ~(UTRIE3_CP_PER_INDEX_1_ENTRY - 1);
        int32_t i = highStart >> UTRIE3_SUPP_SHIFT_2;
        int32_t iLimit = c >> UTRIE3_SUPP_SHIFT_2;
        if (iLimit > indexCapacity) {
            uint32_t *newIndex = (uint32_t *)uprv_malloc(I_LIMIT * 4);
            if (newIndex == nullptr) { return false; }
            uprv_memcpy(newIndex, index, i * 4);
            uprv_free(index);
            index = newIndex;
            indexCapacity = I_LIMIT;
        }
        do {
            flags[i] = ALL_SAME;
            index[i] = initialValue;
        } while(++i < iLimit);
        highStart = c;
    }
    return true;
}

int32_t Trie3Builder::allocDataBlock(int32_t blockLength) {
    int32_t newBlock = dataLength;
    int32_t newTop = newBlock + blockLength;
    if (newTop > dataCapacity) {
        int32_t capacity;
        if (dataCapacity < MEDIUM_DATA_LENGTH) {
            capacity = MEDIUM_DATA_LENGTH;
        } else if (dataCapacity < MAX_DATA_LENGTH) {
            capacity = MAX_DATA_LENGTH;
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
    dataLength = newTop;
    return newBlock;
}

/**
 * No error checking for illegal arguments.
 *
 * @return -1 if no new data block available (out of memory in data array)
 * @internal
 */
int32_t Trie3Builder::getDataBlock(int32_t i) {
    if (flags[i] == MIXED) {
        return index[i];
    }
    if (i < BMP_I_LIMIT) {
        int32_t newBlock = allocDataBlock(UTRIE3_BMP_DATA_BLOCK_LENGTH);
        if (newBlock < 0) { return newBlock; }
        int32_t i0 = i & ~(SUPP_DATA_BLOCKS_PER_BMP_BLOCK -1);
        int32_t iLimit = i0 + SUPP_DATA_BLOCKS_PER_BMP_BLOCK;
        do {
            U_ASSERT(flags[i0] == ALL_SAME);
            writeBlock(data + newBlock, index[i0]);
            flags[i0] = MIXED;
            index[i0++] = newBlock;
            newBlock += UTRIE3_SUPP_DATA_BLOCK_LENGTH;
        } while (i0 < iLimit);
        return index[i];
    } else {
        int32_t newBlock = allocDataBlock(UTRIE3_SUPP_DATA_BLOCK_LENGTH);
        if (newBlock < 0) { return newBlock; }
        writeBlock(data + newBlock, index[i]);
        flags[i] = MIXED;
        index[i] = newBlock;
        return newBlock;
    }
}

void Trie3Builder::set(UChar32 c, uint32_t value, UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) {
        return;
    }
    if ((uint32_t)c > MAX_UNICODE) {
        errorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    int32_t block;
    if (!ensureHighStart(c) || (block = getDataBlock(c >> UTRIE3_SUPP_SHIFT_2)) < 0) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return;
    }

    data[block + (c & UTRIE3_SUPP_DATA_MASK)] = value;
}

/**
 * initialValue is ignored if overwrite=TRUE
 * @internal
 */
void
fillBlock(uint32_t *block, UChar32 start, UChar32 limit,
          uint32_t value, uint32_t initialValue, UBool overwrite) {
    uint32_t *pLimit = block + limit;
    block += start;
    if (overwrite) {
        while (block < pLimit) {
            *block++ = value;
        }
    } else {
        while (block < pLimit) {
            if (*block == initialValue) {
                *block = value;
            }
            ++block;
        }
    }
}

void Trie3Builder::setRange(UChar32 start, UChar32 end, uint32_t value, UBool overwrite,
                            UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) {
        return;
    }
    if ((uint32_t)start > MAX_UNICODE || (uint32_t)end > MAX_UNICODE || start > end) {
        errorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    if (!overwrite && value == initialValue) {
        return;  // nothing to do
    }
    if (!ensureHighStart(end)) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return;
    }

    UChar32 limit = end + 1;
    if (start & UTRIE3_SUPP_DATA_MASK) {
        // Set partial block at [start..following block boundary[.
        int32_t block = getDataBlock(start >> UTRIE3_SUPP_SHIFT_2);
        if (block < 0) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
            return;
        }

        UChar32 nextStart = (start + UTRIE3_SUPP_DATA_MASK) & ~UTRIE3_SUPP_DATA_MASK;
        if (nextStart <= limit) {
            fillBlock(data + block, start & UTRIE3_SUPP_DATA_MASK, UTRIE3_SUPP_DATA_BLOCK_LENGTH,
                      value, initialValue, overwrite);
            start = nextStart;
        } else {
            fillBlock(data + block, start & UTRIE3_SUPP_DATA_MASK, limit & UTRIE3_SUPP_DATA_MASK,
                      value, initialValue, overwrite);
            return;
        }
    }

    // Number of positions in the last, partial block.
    int32_t rest = limit & UTRIE3_SUPP_DATA_MASK;

    // Round down limit to a block boundary.
    limit &= ~UTRIE3_SUPP_DATA_MASK;

    // Iterate over all-value blocks.
    while (start < limit) {
        int32_t i = start >> UTRIE3_SUPP_SHIFT_2;
        if (flags[i] == ALL_SAME) {
            if (overwrite || index[i] == initialValue) {
                index[i] = value;
            }
        } else /* MIXED */ {
            fillBlock(data + index[i], 0, UTRIE3_SUPP_DATA_BLOCK_LENGTH,
                      value, initialValue, overwrite);
        }
        start += UTRIE3_SUPP_DATA_BLOCK_LENGTH;
    }

    if (rest > 0) {
        // Set partial block at [last block boundary..limit[.
        int32_t block = getDataBlock(start >> UTRIE3_SUPP_SHIFT_2);
        if (block < 0) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
            return;
        }

        fillBlock(data + block, 0, rest, value, initialValue, overwrite);
    }
}

/* compaction --------------------------------------------------------------- */

void Trie3Builder::maskValues(uint32_t mask) {
    initialValue &= mask;
    errorValue &= mask;
    highValue &= mask;
    int32_t iLimit = highStart >> UTRIE3_SUPP_SHIFT_2;
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
equalBlocks(const uint32_t *s, const uint32_t *t, int32_t length) {
    while (length > 0 && *s == *t) {
        ++s;
        ++t;
        --length;
    }
    return length == 0;
}

inline bool
equalBlocks(const uint16_t *s, const uint32_t *t, int32_t length) {
    while (length > 0 && *s == *t) {
        ++s;
        ++t;
        --length;
    }
    return length == 0;
}

inline bool
equalBlocks(const uint16_t *s, const uint16_t *t, int32_t length) {
    while (length > 0 && *s == *t) {
        ++s;
        ++t;
        --length;
    }
    return length == 0;
}

bool allValuesSameAs(const uint32_t *p, int32_t length, uint32_t value) {
    const uint32_t *pLimit = p + length;
    while (p < pLimit && *p == value) { ++p; }
    return p == pLimit;
}

/** Search for an identical block. */
int32_t findSameBlock(const uint32_t *p, int32_t pStart, int32_t length,
                      const uint32_t *q, int32_t qStart, int32_t blockLength) {
    // Ensure that we do not even partially get past length.
    length -= blockLength;

    q += qStart;
    while (pStart <= length) {
        if (equalBlocks(p + pStart, q, blockLength)) {
            return pStart;
        }
        ++pStart;
    }
    return -1;
}

int32_t findSameBlock(const uint16_t *p, int32_t pStart, int32_t length,
                      const uint32_t *q, int32_t qStart, int32_t blockLength) {
    // Ensure that we do not even partially get past length.
    length -= blockLength;

    q += qStart;
    while (pStart <= length) {
        if (equalBlocks(p + pStart, q, blockLength)) {
            return pStart;
        }
        ++pStart;
    }
    return -1;
}

int32_t findSameBlock(const uint16_t *p, int32_t pStart, int32_t length,
                      const uint16_t *q, int32_t qStart, int32_t blockLength) {
    // Ensure that we do not even partially get past length.
    length -= blockLength;

    q += qStart;
    while (pStart <= length) {
        if (equalBlocks(p + pStart, q, blockLength)) {
            return pStart;
        }
        ++pStart;
    }
    return -1;
}

int32_t findAllSameBlock(const uint32_t *p, int32_t length, uint32_t value,
                         int32_t blockLength) {
    // Ensure that we do not even partially get past length.
    length -= blockLength;

    for (int32_t block = 0; block <= length; ++block) {
        if (p[block] == value) {
            for (int32_t i = 1;; ++i) {
                if (i == blockLength) {
                    return block;
                }
                if (p[block + i] != value) {
                    block += i;
                    break;
                }
            }
        }
    }
    return -1;
}

/**
 * Look for maximum overlap of the beginning of the other block
 * with the previous, adjacent block.
 */
int32_t getOverlap(const uint32_t *p, int32_t length,
                   const uint32_t *q, int32_t qStart, int32_t blockLength) {
    int32_t overlap = blockLength - 1;
    U_ASSERT(overlap <= length);
    q += qStart;
    while (overlap > 0 && !equalBlocks(p + (length - overlap), q, overlap)) {
        --overlap;
    }
    return overlap;
}

int32_t getOverlap(const uint16_t *p, int32_t length,
                   const uint32_t *q, int32_t qStart, int32_t blockLength) {
    int32_t overlap = blockLength - 1;
    U_ASSERT(overlap <= length);
    q += qStart;
    while (overlap > 0 && !equalBlocks(p + (length - overlap), q, overlap)) {
        --overlap;
    }
    return overlap;
}

int32_t getOverlap(const uint16_t *p, int32_t length,
                   const uint16_t *q, int32_t qStart, int32_t blockLength) {
    int32_t overlap = blockLength - 1;
    U_ASSERT(overlap <= length);
    q += qStart;
    while (overlap > 0 && !equalBlocks(p + (length - overlap), q, overlap)) {
        --overlap;
    }
    return overlap;
}

int32_t getAllSameOverlap(const uint32_t *p, int32_t length, uint32_t value,
                          int32_t blockLength) {
    int32_t min = length - (blockLength - 1);
    int32_t i = length;
    while (min < i && p[i - 1] == value) { --i; }
    return length - i;
}

/**
 * Finds the start of the last range in the trie by enumerating backward.
 * Indexes for supplementary code points higher than this will be omitted.
 */
UChar32 Trie3Builder::findHighStart() const {
    int32_t i = highStart >> UTRIE3_SUPP_SHIFT_2;
    while (i > 0) {
        bool match;
        if (flags[--i] == ALL_SAME) {
            match = index[i] == highValue;
        } else /* MIXED */ {
            const uint32_t *p = data + index[i];
            for (int32_t j = 0;; ++j) {
                if (j == UTRIE3_SUPP_DATA_BLOCK_LENGTH) {
                    match = true;
                    break;
                }
                if (p[j] != highValue) {
                    match = false;
                    break;
                }
            }
        }
        if (!match) {
            return (i + 1) << UTRIE3_SUPP_SHIFT_2;
        }
    }
    return 0;
}

class AllSameBlocks {
public:
    static constexpr int32_t NEW_UNIQUE = -1;
    static constexpr int32_t OVERFLOW = -2;

    AllSameBlocks() : length(0), mostRecent(-1) {}

    int32_t findOrAdd(int32_t index, int32_t count, uint32_t value) {
        if (mostRecent >= 0 && values[mostRecent] == value) {
            refCounts[mostRecent] += count;
            return indexes[mostRecent];
        }
        for (int32_t i = 0; i < length; ++i) {
            if (values[i] == value) {
                mostRecent = i;
                refCounts[i] += count;
                return indexes[i];
            }
        }
        if (length == CAPACITY) {
            return OVERFLOW;
        }
        mostRecent = length;
        indexes[length] = index;
        values[length] = value;
        refCounts[length++] = count;
        return NEW_UNIQUE;
    }

    /** Replaces the block which has the lowest reference count. */
    void add(int32_t index, int32_t count, uint32_t value) {
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
        refCounts[least] = count;
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

int32_t Trie3Builder::compactWholeDataBlocks(AllSameBlocks &allSameBlocks) {
#ifdef UTRIE3_DEBUG
    bool overflow = false;
#endif

    // ASCII data will be stored as a linear table, even if the following code
    // does not yet count it that way.
    int32_t newDataCapacity = ASCII_LIMIT;
    // Add room for special values (errorValue, highValue) and padding.
    newDataCapacity += 4;
    int32_t iLimit = highStart >> UTRIE3_SUPP_SHIFT_2;
    int32_t blockLength = UTRIE3_BMP_DATA_BLOCK_LENGTH;
    int32_t inc = SUPP_DATA_BLOCKS_PER_BMP_BLOCK;
    for (int32_t i = 0; i < iLimit; i += inc) {
        if (i == BMP_I_LIMIT) {
            blockLength = UTRIE3_SUPP_DATA_BLOCK_LENGTH;
            inc = 1;
        }
        uint32_t value = index[i];
        if (flags[i] == MIXED) {
            // Really mixed?
            const uint32_t *p = data + value;
            value = *p;
            if (allValuesSameAs(p + 1, blockLength - 1, value)) {
                flags[i] = ALL_SAME;
                index[i] = value;
                // Fall through to ALL_SAME handling.
            } else {
                newDataCapacity += blockLength;
                continue;
            }
        } else {
            U_ASSERT(flags[i] == ALL_SAME);
            if (inc > 1) {
                // Do all of the BMP data block's ALL_SAME parts have the same value?
                bool allSame = true;
                int32_t next_i = i + inc;
                for (int32_t j = i + 1; j < next_i; ++j) {
                    U_ASSERT(flags[j] == ALL_SAME);
                    if (index[j] != value) {
                        allSame = false;
                        break;
                    }
                }
                if (!allSame) {
                    // Turn it into a MIXED block.
                    if (getDataBlock(i) < 0) {
                        return -1;
                    }
                    continue;
                }
            }
        }
        // Is there another ALL_SAME block with the same value?
        int32_t other = allSameBlocks.findOrAdd(i, inc, value);
        if (other == AllSameBlocks::OVERFLOW) {
            // The fixed-size array overflowed. Slow check for a duplicate block.
#ifdef UTRIE3_DEBUG
            if (!overflow) {
                puts("UTrie3 AllSameBlocks overflow");
                overflow = true;
            }
#endif
            int32_t jInc = SUPP_DATA_BLOCKS_PER_BMP_BLOCK;
            for (int32_t j = 0;; j += jInc) {
                if (j == i) {
                    allSameBlocks.add(i, inc, value);
                    break;
                }
                if (j == BMP_I_LIMIT) {
                    jInc = 1;
                }
                if (flags[j] == ALL_SAME && index[j] == value) {
                    allSameBlocks.add(j, jInc + inc, value);
                    other = j;
                    break;
                    // We could keep counting blocks with the same value
                    // before we add the first one, which may improve compaction in rare cases,
                    // but it would make it slower.
                }
            }
        }
        if (other >= 0) {
            flags[i] = SAME_AS;
            index[i] = other;
        } else {
            // New unique same-value block.
            newDataCapacity += blockLength;
        }
    }
    return newDataCapacity;
}

#ifdef UTRIE3_DEBUG
#   define DEBUG_DO(expr) expr
#else
#   define DEBUG_DO(expr)
#endif

#ifdef UTRIE3_DEBUG
// Braille symbols: U+28xx = UTF-8 E2 A0 80..E2 A3 BF
int32_t appendValue(char s[], int32_t length, uint32_t value) {
    value ^= value >> 16;
    value ^= value >> 8;
    s[length] = 0xE2;
    s[length + 1] = (char)(0xA0 + ((value >> 6) & 3));
    s[length + 2] = (char)(0x80 + (value & 0x3F));
    return length + 3;
}

void printBlock(const uint32_t *block, int32_t blockLength, uint32_t value,
                UChar32 start, int32_t overlap, uint32_t initialValue) {
    char s[UTRIE3_BMP_DATA_BLOCK_LENGTH * 3 + 3];
    int32_t length = 0;
    int32_t i;
    for (i = 0; i < overlap; ++i) {
        length = appendValue(s, length, 0);  // Braille blank
    }
    s[length++] = '|';
    for (; i < blockLength; ++i) {
        if (block != nullptr) {
            value = block[i];
        }
        if (value == initialValue) {
            value = 0x40;  // Braille lower left dot
        }
        length = appendValue(s, length, value);
    }
    s[length] = 0;
    start += overlap;
    if (start <= 0xffff) {
        printf("    %04lX  %s|\n", (long)start, s);
    } else if (start <= 0xfffff) {
        printf("   %5lX  %s|\n", (long)start, s);
    } else {
        printf("  %6lX  %s|\n", (long)start, s);
    }
}
#endif

/**
 * Compacts a build-time trie.
 *
 * The compaction
 * - removes blocks that are identical with earlier ones
 * - overlaps each new non-duplicate block as much as possible with the previously-written one
 * - works with BMP data blocks whose length is a multiple of that of supplementary data blocks
 *
 * It does not try to find an optimal order of writing, deduplicating, and overlapping blocks.
 */
int32_t Trie3Builder::compactData(uint32_t *newData) {
#ifdef UTRIE3_DEBUG
    int32_t countSame=0, sumOverlaps=0;
    bool printData = dataLength == 29088 /* line.brk */ ||
        // dataLength == 30048 /* CanonIterData */ ||
        dataLength == 50400 /* zh.txt~stroke */;
#endif

    // The linear ASCII data has been copied into newData already.
    int32_t newDataLength = 0;
    for (int32_t i = 0; newDataLength < ASCII_LIMIT;
            newDataLength += UTRIE3_BMP_DATA_BLOCK_LENGTH, i += SUPP_DATA_BLOCKS_PER_BMP_BLOCK) {
        index[i] = newDataLength;
#ifdef UTRIE3_DEBUG
        if (printData) {
            printBlock(newData + newDataLength, UTRIE3_BMP_DATA_BLOCK_LENGTH, 0, newDataLength, 0, initialValue);
        }
#endif
    }

    int32_t iLimit = highStart >> UTRIE3_SUPP_SHIFT_2;
    int32_t blockLength = UTRIE3_BMP_DATA_BLOCK_LENGTH;
    int32_t inc = SUPP_DATA_BLOCKS_PER_BMP_BLOCK;
    for (int32_t i = ASCII_I_LIMIT; i < iLimit; i += inc) {
        if (i == BMP_I_LIMIT) {
            blockLength = UTRIE3_SUPP_DATA_BLOCK_LENGTH;
            inc = 1;
        }
        if (flags[i] == ALL_SAME) {
            uint32_t value = index[i];
            int32_t n = findAllSameBlock(newData, newDataLength, value, blockLength);
            if (n >= 0) {
                DEBUG_DO(++countSame);
                index[i] = n;
            } else {
                n = getAllSameOverlap(newData, newDataLength, value, blockLength);
                DEBUG_DO(sumOverlaps += n);
#ifdef UTRIE3_DEBUG
                if (printData) {
                    printBlock(nullptr, blockLength, value, i << UTRIE3_SUPP_SHIFT_2, n, initialValue);
                }
#endif
                index[i] = newDataLength - n;
                while (n < blockLength) {
                    newData[newDataLength++] = value;
                    ++n;
                }
            }
        } else if (flags[i] == MIXED) {
            const uint32_t *block = data + index[i];
            int32_t n = findSameBlock(newData, 0, newDataLength, block, 0, blockLength);
            if (n >= 0) {
                DEBUG_DO(++countSame);
                index[i] = n;
            } else {
                n = getOverlap(newData, newDataLength, block, 0, blockLength);
                DEBUG_DO(sumOverlaps += n);
#ifdef UTRIE3_DEBUG
                if (printData) {
                    printBlock(block, blockLength, 0, i << UTRIE3_SUPP_SHIFT_2, n, initialValue);
                }
#endif
                index[i] = newDataLength - n;
                while (n < blockLength) {
                    newData[newDataLength++] = block[n++];
                }
            }
        } else /* SAME_AS */ {
            uint32_t j = index[i];
            index[i] = index[j];
        }
    }

#ifdef UTRIE3_DEBUG
    /* we saved some space */
    printf("compacting UTrie3: count of 32-bit data words %lu->%lu  countSame=%ld  sumOverlaps=%ld\n",
            (long)dataLength, (long)newDataLength, (long)countSame, (long)sumOverlaps);
#endif
    return newDataLength;
}

int32_t Trie3Builder::compactIndex2(UErrorCode &errorCode) {
    // Condense the BMP index table.
    // Also, does it contain an index-2 block with all dataNullOffset?
    int32_t i2FirstNull = -1;
    for (int32_t i = 0, j = 0; i < BMP_I_LIMIT; i += SUPP_DATA_BLOCKS_PER_BMP_BLOCK, ++j) {
        uint32_t i2 = index[j] = index[i];
        if (i2 == (uint32_t)dataNullOffset) {
            if (i2FirstNull < 0) {
                i2FirstNull = j;
            } else if (index2NullOffset < 0 &&
                    (j - i2FirstNull + 1) == UTRIE3_INDEX_2_BLOCK_LENGTH) {
                index2NullOffset = i2FirstNull;
            }
        } else {
            i2FirstNull = -1;
        }
    }

    if (highStart <= BMP_LIMIT) {
        // Only the linear BMP index, no supplementary index tables.
        return UTRIE3_BMP_INDEX_LENGTH;
    }

    // Find the largest "gap" of one or more index-2 null blocks.
    // Put the empty gap at highStart.
    int32_t gapStart = highStart >> UTRIE3_SUPP_SHIFT_2;
    int32_t gapLength = 0;
    int32_t tempGapStart = -1;

    // Examine supplementary index-2 blocks. For each determine one of:
    // - same as the index-2 null block
    // - same as a BMP index block
    // - 16-bit indexes
    // - 18-bit indexes
    // We store this in the first flags entry for the index-2 block.
    //
    // Also determine an upper limit for the index16 table length.
    int32_t index16Capacity = 0;
    i2FirstNull = index2NullOffset;
    int32_t iLimit = highStart >> UTRIE3_SUPP_SHIFT_2;
    for (int32_t i = BMP_I_LIMIT; i < iLimit;) {
        int32_t j = i;
        int32_t jLimit = i + UTRIE3_INDEX_2_BLOCK_LENGTH;
        uint32_t oredI2 = 0;
        bool isNull = true;
        do {
            uint32_t i2 = index[j];
            oredI2 |= i2;
            if (i2 != (uint32_t)dataNullOffset) {
                isNull = false;
            }
        } while (++j < jLimit);
        if (isNull) {
            flags[i] = I2_NULL;
            if (i2FirstNull < 0) {
                if (oredI2 <= 0xffff) {
                    index16Capacity += UTRIE3_INDEX_2_BLOCK_LENGTH;
                } else {
                    index16Capacity += INDEX_2_18BIT_BLOCK_LENGTH;
                }
                i2FirstNull = 0;
            }
            if (tempGapStart < 0) {
                tempGapStart = i;
            }
            int32_t tempGapLength = j - tempGapStart;
            if (tempGapLength > gapLength) {
                gapStart = tempGapStart;
                gapLength = tempGapLength;
            }
        } else {
            if (oredI2 <= 0xffff) {
                int32_t n = findSameBlock(index, 0, UTRIE3_BMP_INDEX_LENGTH,
                                          index, i, UTRIE3_INDEX_2_BLOCK_LENGTH);
                if (n >= 0) {
                    flags[i] = I2_BMP;
                    index[i] = n;
                } else {
                    flags[i] = I2_16;
                    index16Capacity += UTRIE3_INDEX_2_BLOCK_LENGTH;
                }
            } else {
                flags[i] = I2_18;
                index16Capacity += INDEX_2_18BIT_BLOCK_LENGTH;
            }
            tempGapStart = -1;
        }
        i = j;
    }

    int32_t gapLimit = gapStart + gapLength;
    // Note: There is a small chance that the data null block is only reachable from gap indexes,
    // in which case we only need one data null value rather than a whole data block.
    // It is not worth trying to prevent that.
    // Note: When nullValue!=highValue, it is possible that
    // the gap consumes the entire index-1 table.

    // Length of the index-1 table including gapStart & gapLimit but minus the gap.
    int32_t index1Length = 2 + ((highStart - BMP_LIMIT) >> UTRIE3_SUPP_SHIFT_1) -
        (gapLength >> UTRIE3_SUPP_SHIFT_1_2);

    // Include the index-1 table in the index array capacity.
    // +1 for possible index table padding.
    index16Capacity += index1Length + 1;

    // Compact the supplementary index-2 table.
    // Write the supplementary index-1 table at the start of index16, then the index-2 values.
    index16 = (uint16_t *)uprv_malloc(index16Capacity * 2);
    if (index16 == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return 0;
    }

    int32_t i1 = 0;
    index16[i1++] = gapStart >> UTRIE3_SUPP_SHIFT_1_2;
    index16[i1++] = gapLimit >> UTRIE3_SUPP_SHIFT_1_2;

    i2FirstNull = index2NullOffset;
    int32_t indexLength = index1Length;
    for (int32_t i = BMP_I_LIMIT;; i += UTRIE3_INDEX_2_BLOCK_LENGTH) {
        if (i == gapStart) {
            i = gapLimit;  // Skip the gap.
        }
        if (i == iLimit) { break; }
        int32_t i2;
        uint8_t f = flags[i];
        if (f == I2_NULL && i2FirstNull < 0) {
            // First index-2 null block. Write & overlap it like a normal block, then remember it.
            f = dataNullOffset <= 0xffff ? I2_16 : I2_18;
            i2FirstNull = 0;
        }
        if (f == I2_NULL) {
            i2 = index2NullOffset;
        } else if (f == I2_BMP) {
            i2 = index[i];
        } else if (f == I2_16) {
            int32_t n = findSameBlock(index16, index1Length, indexLength,
                                      index, i, UTRIE3_INDEX_2_BLOCK_LENGTH);
            if (n >= 0) {
                i2 = UTRIE3_BMP_INDEX_LENGTH + n;
            } else {
                if (indexLength == index1Length) {
                    // No overlap at the boundary between the index-1 and index-2 tables.
                    n = 0;
                } else {
                    n = getOverlap(index16, indexLength,
                                   index, i, UTRIE3_INDEX_2_BLOCK_LENGTH);
                }
                i2 = UTRIE3_BMP_INDEX_LENGTH + (indexLength - n);
                while (n < UTRIE3_INDEX_2_BLOCK_LENGTH) {
                    index16[indexLength++] = index[i + n++];
                }
            }
        } else {
            U_ASSERT(f == I2_18);
            // Encode an index-2 block that contains one or more data indexes exceeding 16 bits.
            int32_t j = i;
            int32_t jLimit = i + UTRIE3_INDEX_2_BLOCK_LENGTH;
            int32_t k = indexLength;
            do {
                ++k;
                uint32_t v = index[j++];
                uint32_t upperBits = (v & 0x30000) >> 2;
                index16[k++] = v;
                v = index[j++];
                upperBits |= (v & 0x30000) >> 4;
                index16[k++] = v;
                v = index[j++];
                upperBits |= (v & 0x30000) >> 6;
                index16[k++] = v;
                v = index[j++];
                upperBits |= (v & 0x30000) >> 8;
                index16[k++] = v;
                v = index[j++];
                upperBits |= (v & 0x30000) >> 10;
                index16[k++] = v;
                v = index[j++];
                upperBits |= (v & 0x30000) >> 12;
                index16[k++] = v;
                v = index[j++];
                upperBits |= (v & 0x30000) >> 14;
                index16[k++] = v;
                v = index[j++];
                upperBits |= (v & 0x30000) >> 16;
                index16[k++] = v;
                index16[k - 9] = upperBits;
            } while (j < jLimit);
            int32_t n = findSameBlock(index16, index1Length, indexLength,
                                      index16, indexLength, INDEX_2_18BIT_BLOCK_LENGTH);
            if (n >= 0) {
                i2 = (UTRIE3_BMP_INDEX_LENGTH + n) | 0x8000;
            } else {
                if (indexLength == index1Length) {
                    // No overlap at the boundary between the index-1 and index-2 tables.
                    n = 0;
                } else {
                    n = getOverlap(index16, indexLength,
                                   index16, indexLength, INDEX_2_18BIT_BLOCK_LENGTH);
                }
                i2 = (UTRIE3_BMP_INDEX_LENGTH + (indexLength - n)) | 0x8000;
                if (n > 0) {
                    int32_t start = indexLength;
                    while (n < INDEX_2_18BIT_BLOCK_LENGTH) {
                        index16[indexLength++] = index[start + n++];
                    }
                } else {
                    indexLength += INDEX_2_18BIT_BLOCK_LENGTH;
                }
            }
        }
        if (index2NullOffset < 0 && i2FirstNull >= 0) {
            index2NullOffset = i2;
        }
        // Set the index-1 table entry.
        index16[i1++] = i2;
    }
    U_ASSERT(i1 == index1Length);

    if (index2NullOffset < 0) {
        index2NullOffset = UTRIE3_NO_INDEX2_NULL_OFFSET;
    }

    U_ASSERT(indexLength <= index16Capacity);
    indexLength += UTRIE3_BMP_INDEX_LENGTH;

#ifdef UTRIE3_DEBUG
    /* we saved some space */
    printf("compacting UTrie3: count of 16-bit index words %lu->%lu  "
           "index-1 gap length %d %04lx..%04lx\n",
            (long)iLimit, (long)indexLength, (int)(gapLength >> UTRIE3_SUPP_SHIFT_1_2),
            (long)(gapStart << UTRIE3_SUPP_SHIFT_2), (long)(gapLimit << UTRIE3_SUPP_SHIFT_2));
#endif

    return indexLength;
}

int32_t Trie3Builder::compactTrie(UErrorCode &errorCode) {
    // Find the real highStart and round it up.
    U_ASSERT((highStart & (UTRIE3_CP_PER_INDEX_1_ENTRY - 1)) == 0);
    highValue = get(MAX_UNICODE);
    int32_t realHighStart = findHighStart();
    realHighStart = (realHighStart + (UTRIE3_CP_PER_INDEX_1_ENTRY - 1)) &
        ~(UTRIE3_CP_PER_INDEX_1_ENTRY - 1);
    if (realHighStart == UNICODE_LIMIT) {
        highValue = initialValue;
    }

#ifdef UTRIE3_DEBUG
    printf("UTrie3: highStart U+%06lx  highValue 0x%lx  initialValue 0x%lx\n",
            (long)realHighStart, (long)highValue, (long)initialValue);
#endif

    // We always store indexes and data values for the BMP.
    // Pin highStart to the supplementary range while building.
    if (realHighStart < BMP_LIMIT) {
        for (int32_t i = (realHighStart >> UTRIE3_SUPP_SHIFT_2); i < BMP_I_LIMIT; ++i) {
            flags[i] = ALL_SAME;
            index[i] = highValue;
        }
        highStart = BMP_LIMIT;
    } else {
        highStart = realHighStart;
    }

    uint32_t asciiData[ASCII_LIMIT];
    for (int32_t i = 0; i < ASCII_LIMIT; ++i) {
        asciiData[i] = get(i);
    }

    // First we look for which data blocks have the same value repeated over the whole block,
    // deduplicate such blocks, find a good null data block (for faster enumeration),
    // and get an upper bound for the necessary data array length.
    AllSameBlocks allSameBlocks;
    int32_t newDataCapacity = compactWholeDataBlocks(allSameBlocks);
    if (newDataCapacity < 0) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return 0;
    }
    uint32_t *newData = (uint32_t *)uprv_malloc(newDataCapacity * 4);
    if (newData == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return 0;
    }
    uprv_memcpy(newData, asciiData, sizeof(asciiData));

    int32_t newDataLength = compactData(newData);
    U_ASSERT(newDataLength <= newDataCapacity);
    uprv_free(data);
    data = newData;
    dataCapacity = newDataCapacity;
    dataLength = newDataLength;
    if (dataLength > (0x3ffff + UTRIE3_SUPP_DATA_BLOCK_LENGTH)) {
        // The offset of the last data block is too high to be stored in the index table.
        errorCode = U_INDEX_OUTOFBOUNDS_ERROR;
        return 0;
    }

    int32_t dataNullIndex = allSameBlocks.findMostUsed();
    if (dataNullIndex >= 0) {
        dataNullOffset = index[dataNullIndex];
#ifdef UTRIE3_DEBUG
        if (data[dataNullOffset] != initialValue) {
            printf("UTrie3 initialValue %lx -> more common nullValue %lx\n",
                   (long)initialValue, (long)data[dataNullOffset]);
        }
#endif
        initialValue = data[dataNullOffset];
    } else {
        dataNullOffset = UTRIE3_NO_DATA_NULL_OFFSET;
    }

    int32_t indexLength = compactIndex2(errorCode);
    highStart = realHighStart;
    return indexLength;
}

UTrie3 *Trie3Builder::build(UTrie3ValueBits valueBits, UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }
    if (valueBits > UTRIE3_32_VALUE_BITS) {
        errorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return nullptr;
    }

    // The builder always stores 32-bit values.
    // When we build a UTrie3 for a smaller value width, we first mask off unused bits
    // before compacting the data.
    if (valueBits != UTRIE3_32_VALUE_BITS) {
        maskValues(0xffff);
    }

    int32_t indexLength = compactTrie(errorCode);
    if (U_FAILURE(errorCode)) {
        clear();
        return nullptr;
    }

    // Ensure data table alignment: The index length must be even for uint32_t data.
    int32_t suppIndexLength = indexLength - UTRIE3_BMP_INDEX_LENGTH;
    if (valueBits == UTRIE3_32_VALUE_BITS && (indexLength & 1) != 0) {
        index16[suppIndexLength++] = 0xffee;  // arbitary value
        ++indexLength;
    }
    if (indexLength > MAX_INDEX_LENGTH) {
        errorCode = U_INDEX_OUTOFBOUNDS_ERROR;
        clear();
        return nullptr;
    }

    // Make the total trie structure length a multiple of 4 bytes by padding the data table,
    // and store special values as the last two data values.
    int32_t length = indexLength * 2;
    if (valueBits == UTRIE3_16_VALUE_BITS) {
        if (((indexLength ^ dataLength) & 1) != 0) {
            // padding
            data[dataLength++] = errorValue;
        }
        if (data[dataLength - 1] != errorValue || data[dataLength - 2] != highValue) {
            data[dataLength++] = highValue;
            data[dataLength++] = errorValue;
        }
        length += dataLength * 2;
    } else {
        // 32-bit data words never need padding to a multiple of 4 bytes.
        if (data[dataLength - 1] != errorValue || data[dataLength - 2] != highValue) {
            if (data[dataLength - 1] != highValue) {
                data[dataLength++] = highValue;
            }
            data[dataLength++] = errorValue;
        }
        length += dataLength * 4;
    }

    // Calculate the total length of the UTrie3 as a single memory block.
    length += sizeof(UTrie3);
    U_ASSERT((length & 3) == 0);

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

    trie->highStart = highStart;
    // Round up shifted12HighStart to a multiple of 0x1000 for easy testing from UTF-8 lead bytes.
    // Runtime code needs to then test for the real highStart as well.
    trie->shifted12HighStart = (highStart + 0xfff) >> 12;

    trie->index2NullOffset = index2NullOffset;
    trie->dataNullOffset = dataNullOffset;
    trie->nullValue = initialValue;

    bytes += sizeof(UTrie3);

    // Fill the index and data arrays.
    uint16_t *dest16 = (uint16_t *)bytes;
    trie->index = dest16;

    // Write BMP index array values.
    for (int32_t i = 0; i < UTRIE3_BMP_INDEX_LENGTH; ++i) {
        *dest16++ = (uint16_t)index[i];
    }

    if (suppIndexLength > 0) {
        // Write 16-bit index-1 and index-2 values for supplementary code points.
        uprv_memcpy(dest16, index16, suppIndexLength * 2);
        dest16 += suppIndexLength;
    }
    bytes += indexLength * 2;

    // Write the 16/32-bit data array.
    const uint32_t *p = data;
    switch (valueBits) {
    case UTRIE3_16_VALUE_BITS:
        // Write 16-bit data values.
        trie->data16 = dest16;
        trie->data32 = nullptr;
        for (int32_t i = dataLength; i > 0; --i) {
            *dest16++ = (uint16_t)*p++;
        }
        break;
    case UTRIE3_32_VALUE_BITS:
        // Write 32-bit data values.
        trie->data16 = nullptr;
        trie->data32 = (uint32_t *)bytes;
        uprv_memcpy(bytes, p, (size_t)dataLength * 4);
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
