// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// ucptrie.cpp (modified from utrie2.cpp)
// created: 2017dec29 Markus W. Scherer

#define UCPTRIE_DEBUG  // TODO
#ifdef UCPTRIE_DEBUG
#   include <stdio.h>
#endif

#include "unicode/utypes.h"
#include "unicode/ucptrie.h"
#include "unicode/utf.h"
#include "unicode/utf8.h"
#include "unicode/utf16.h"
#include "cmemory.h"
#include "uassert.h"
#include "ucptrie_impl.h"

U_CAPI UCPTrie * U_EXPORT2
ucptrie_openFromBinary(UCPTrieType type, UCPTrieValueWidth valueWidth,
                       const void *data, int32_t length, int32_t *pActualLength,
                       UErrorCode *pErrorCode) {
    if (U_FAILURE(*pErrorCode)) {
        return nullptr;
    }

    if (length <= 0 || (U_POINTER_MASK_LSB(data, 3) != 0) ||
            type < UCPTRIE_TYPE_ANY || UCPTRIE_TYPE_SMALL < type ||
            valueWidth < UCPTRIE_VALUE_BITS_ANY || UCPTRIE_VALUE_BITS_8 < valueWidth) {
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return nullptr;
    }

    // Enough data for a trie header?
    if (length < (int32_t)sizeof(UCPTrieHeader)) {
        *pErrorCode = U_INVALID_FORMAT_ERROR;
        return nullptr;
    }

    // Check the signature.
    const UCPTrieHeader *header = (const UCPTrieHeader *)data;
    if (header->signature != UCPTRIE_SIG) {
        *pErrorCode = U_INVALID_FORMAT_ERROR;
        return nullptr;
    }

    int32_t options = header->options;
    int32_t typeInt = (options >> 6) & 3;
    int32_t valueWidthInt = options & UCPTRIE_OPTIONS_VALUE_BITS_MASK;
    if (typeInt > UCPTRIE_TYPE_SMALL || valueWidthInt > UCPTRIE_VALUE_BITS_8 ||
            (options & UCPTRIE_OPTIONS_RESERVED_MASK) != 0) {
        *pErrorCode = U_INVALID_FORMAT_ERROR;
        return nullptr;
    }
    UCPTrieType actualType = (UCPTrieType)typeInt;
    UCPTrieValueWidth actualValueWidth = (UCPTrieValueWidth)valueWidthInt;
    if (type < 0) {
        type = actualType;
    }
    if (valueWidth < 0) {
        valueWidth = actualValueWidth;
    }
    if (type != actualType || valueWidth != actualValueWidth) {
        *pErrorCode = U_INVALID_FORMAT_ERROR;
        return nullptr;
    }

    // Get the length values and offsets.
    UCPTrie tempTrie;
    uprv_memset(&tempTrie, 0, sizeof(tempTrie));
    tempTrie.indexLength = header->indexLength;
    tempTrie.dataLength =
        ((options & UCPTRIE_OPTIONS_DATA_LENGTH_MASK) << 4) | header->dataLength;
    tempTrie.index3NullOffset = header->index3NullOffset;
    tempTrie.dataNullOffset =
        ((options & UCPTRIE_OPTIONS_DATA_NULL_OFFSET_MASK) << 8) | header->dataNullOffset;

    tempTrie.highStart = header->shiftedHighStart << UCPTRIE_SHIFT_2;
    tempTrie.shifted12HighStart = (tempTrie.highStart + 0xfff) >> 12;
    tempTrie.type = type;
    tempTrie.valueWidth = valueWidth;

    // Calculate the actual length.
    int32_t actualLength = (int32_t)sizeof(UCPTrieHeader) + tempTrie.indexLength * 2;
    if (valueWidth == UCPTRIE_VALUE_BITS_16) {
        actualLength += tempTrie.dataLength * 2;
    } else if (valueWidth == UCPTRIE_VALUE_BITS_32) {
        actualLength += tempTrie.dataLength * 4;
    } else {
        actualLength += tempTrie.dataLength;
    }
    if (length < actualLength) {
        *pErrorCode = U_INVALID_FORMAT_ERROR;  // Not enough bytes.
        return nullptr;
    }

    // Allocate the trie.
    UCPTrie *trie = (UCPTrie *)uprv_malloc(sizeof(UCPTrie));
    if (trie == nullptr) {
        *pErrorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    uprv_memcpy(trie, &tempTrie, sizeof(tempTrie));
    trie->name = "fromSerialized";

    // Set the pointers to its index and data arrays.
    const uint16_t *p16 = (const uint16_t *)(header + 1);
    trie->index = p16;
    p16 += trie->indexLength;

    // Get the data.
    int32_t nullValueOffset = trie->dataNullOffset;
    if (nullValueOffset >= trie->dataLength) {
        nullValueOffset = trie->dataLength - UCPTRIE_HIGH_VALUE_NEG_DATA_OFFSET;
    }
    switch (valueWidth) {
    case UCPTRIE_VALUE_BITS_16:
        trie->data.ptr16 = p16;
        trie->nullValue = trie->data.ptr16[nullValueOffset];
        break;
    case UCPTRIE_VALUE_BITS_32:
        trie->data.ptr32 = (const uint32_t *)p16;
        trie->nullValue = trie->data.ptr32[nullValueOffset];
        break;
    case UCPTRIE_VALUE_BITS_8:
        trie->data.ptr8 = (const uint8_t *)p16;
        trie->nullValue = trie->data.ptr8[nullValueOffset];
        break;
    default:
        *pErrorCode = U_INVALID_FORMAT_ERROR;
        return nullptr;
    }

    if (pActualLength != nullptr) {
        *pActualLength = actualLength;
    }
    return trie;
}

U_CAPI void U_EXPORT2
ucptrie_close(UCPTrie *trie) {
    uprv_free(trie);
}

U_CAPI UCPTrieType U_EXPORT2
ucptrie_getType(const UCPTrie *trie) {
    return (UCPTrieType)trie->type;
}

U_CAPI UCPTrieValueWidth U_EXPORT2
ucptrie_getValueWidth(const UCPTrie *trie) {
    return (UCPTrieValueWidth)trie->valueWidth;
}

U_CAPI int32_t U_EXPORT2
ucptrie_internalSmallIndex(const UCPTrie *trie, UChar32 c) {
    int32_t i1 = c >> UCPTRIE_SHIFT_1;
    if (trie->type == UCPTRIE_TYPE_FAST) {
        U_ASSERT(0xffff < c && c < trie->highStart);
        i1 += UCPTRIE_BMP_INDEX_LENGTH - UCPTRIE_OMITTED_BMP_INDEX_1_LENGTH;
    } else {
        U_ASSERT((uint32_t)c < (uint32_t)trie->highStart && trie->highStart > UCPTRIE_SMALL_LIMIT);
        i1 += UCPTRIE_SMALL_INDEX_LENGTH;
    }
    int32_t i3Block = trie->index[trie->index[i1] + ((c >> UCPTRIE_SHIFT_2) & UCPTRIE_INDEX_2_MASK)];
    int32_t i3 = (c >> UCPTRIE_SHIFT_3) & UCPTRIE_INDEX_3_MASK;
    int32_t dataBlock;
    if ((i3Block & 0x8000) == 0) {
        // 16-bit indexes
        dataBlock = trie->index[i3Block + i3];
    } else {
        // 18-bit indexes stored in groups of 9 entries per 8 indexes.
        i3Block = (i3Block & 0x7fff) + (i3 & ~7) + (i3 >> 3);
        i3 &= 7;
        dataBlock = (trie->index[i3Block++] << (2 + (2 * i3))) & 0x30000;
        dataBlock |= trie->index[i3Block + i3];
    }
    return dataBlock + (c & UCPTRIE_SMALL_DATA_MASK);
}

U_CAPI int32_t U_EXPORT2
ucptrie_internalSmallU8Index(const UCPTrie *trie, int32_t lt1, uint8_t t2, uint8_t t3) {
    UChar32 c = (lt1 << 12) | (t2 << 6) | t3;
    if (c >= trie->highStart) {
        // Possible because the UTF-8 macro compares with shifted12HighStart which may be higher.
        return trie->dataLength - UCPTRIE_HIGH_VALUE_NEG_DATA_OFFSET;
    }
    return ucptrie_internalSmallIndex(trie, c);
}

U_CAPI int32_t U_EXPORT2
ucptrie_internalU8PrevIndex(const UCPTrie *trie, UChar32 c,
                            const uint8_t *start, const uint8_t *src) {
    int32_t i, length;
    // Support 64-bit pointers by avoiding cast of arbitrary difference.
    if ((src - start) <= 7) {
        i = length = (int32_t)(src - start);
    } else {
        i = length = 7;
        start = src - 7;
    }
    c = utf8_prevCharSafeBody(start, 0, &i, c, -1);
    i = length - i;  // Number of bytes read backward from src.
    int32_t idx = _UCPTRIE_CP_INDEX(trie, 0xffff, c);
    return (idx << 3) | i;
}

namespace {

inline uint32_t getValue(UCPTrieData data, UCPTrieValueWidth valueWidth, int32_t dataIndex) {
    switch (valueWidth) {
    case UCPTRIE_VALUE_BITS_16:
        return data.ptr16[dataIndex];
    case UCPTRIE_VALUE_BITS_32:
        return data.ptr32[dataIndex];
    case UCPTRIE_VALUE_BITS_8:
        return data.ptr8[dataIndex];
    default:
        // Unreachable if the trie is properly initialized.
        return 0xffffffff;
    }
}

}  // namespace

U_CAPI uint32_t U_EXPORT2
ucptrie_get(const UCPTrie *trie, UChar32 c) {
    int32_t dataIndex;
    if ((uint32_t)c <= 0x7f) {
        // linear ASCII
        dataIndex = c;
    } else {
        UChar32 fastMax = trie->type == UCPTRIE_TYPE_FAST ? 0xffff : UCPTRIE_SMALL_MAX;
        dataIndex = _UCPTRIE_CP_INDEX(trie, fastMax, c);
    }
    return getValue(trie->data, (UCPTrieValueWidth)trie->valueWidth, dataIndex);
}

namespace {

constexpr int32_t MAX_UNICODE = 0x10ffff;

constexpr int32_t ASCII_LIMIT = 0x80;

inline uint32_t maybeHandleValue(uint32_t value, uint32_t trieNullValue, uint32_t nullValue,
                                 UCPTrieHandleValue *handleValue, const void *context) {
    if (value == trieNullValue) {
        value = nullValue;
    } else if (handleValue != nullptr) {
        value = handleValue(context, value);
    }
    return value;
}

}  // namespace

U_CAPI UChar32 U_EXPORT2
ucptrie_getRange(const UCPTrie *trie, UChar32 start,
                 UCPTrieHandleValue *handleValue, const void *context, uint32_t *pValue) {
    if ((uint32_t)start > MAX_UNICODE) {
        return U_SENTINEL;
    }
    UCPTrieValueWidth valueWidth = (UCPTrieValueWidth)trie->valueWidth;
    if (start >= trie->highStart) {
        if (pValue != nullptr) {
            int32_t di = trie->dataLength - UCPTRIE_HIGH_VALUE_NEG_DATA_OFFSET;
            uint32_t value = getValue(trie->data, valueWidth, di);
            if (handleValue != nullptr) { value = handleValue(context, value); }
            *pValue = value;
        }
        return MAX_UNICODE;
    }

    uint32_t nullValue = trie->nullValue;
    if (handleValue != nullptr) { nullValue = handleValue(context, nullValue); }
    const uint16_t *index = trie->index;

    int32_t prevI3Block = -1;
    int32_t prevBlock = -1;
    UChar32 c = start;
    uint32_t value;
    bool haveValue = false;
    do {
        int32_t i3Block;
        int32_t i3;
        int32_t i3BlockLength;
        int32_t dataBlockLength;
        if (c <= 0xffff && (trie->type == UCPTRIE_TYPE_FAST || c <= UCPTRIE_SMALL_MAX)) {
            i3Block = 0;
            i3 = c >> UCPTRIE_FAST_SHIFT;
            i3BlockLength = trie->type == UCPTRIE_TYPE_FAST ?
                UCPTRIE_BMP_INDEX_LENGTH : UCPTRIE_SMALL_INDEX_LENGTH;
            dataBlockLength = UCPTRIE_FAST_DATA_BLOCK_LENGTH;
        } else {
            // Use the multi-stage index.
            int32_t i1 = c >> UCPTRIE_SHIFT_1;
            if (trie->type == UCPTRIE_TYPE_FAST) {
                U_ASSERT(0xffff < c && c < trie->highStart);
                i1 += UCPTRIE_BMP_INDEX_LENGTH - UCPTRIE_OMITTED_BMP_INDEX_1_LENGTH;
            } else {
                U_ASSERT(c < trie->highStart && trie->highStart > UCPTRIE_SMALL_LIMIT);
                i1 += UCPTRIE_SMALL_INDEX_LENGTH;
            }
            i3Block = trie->index[trie->index[i1] + ((c >> UCPTRIE_SHIFT_2) & UCPTRIE_INDEX_2_MASK)];
            if (i3Block == prevI3Block && (c - start) >= UCPTRIE_CP_PER_INDEX_2_ENTRY) {
                // The index-3 block is the same as the previous one, and filled with value.
                U_ASSERT((c & (UCPTRIE_CP_PER_INDEX_2_ENTRY - 1)) == 0);
                c += UCPTRIE_CP_PER_INDEX_2_ENTRY;
                continue;
            }
            prevI3Block = i3Block;
            if (i3Block == trie->index3NullOffset) {
                // This is the index-3 null block.
                if (haveValue) {
                    if (nullValue != value) {
                        return c - 1;
                    }
                } else {
                    value = nullValue;
                    if (pValue != nullptr) { *pValue = nullValue; }
                    haveValue = true;
                }
                prevBlock = trie->dataNullOffset;
                c = (c + UCPTRIE_CP_PER_INDEX_2_ENTRY) & ~(UCPTRIE_CP_PER_INDEX_2_ENTRY - 1);
                continue;
            }
            i3 = (c >> UCPTRIE_SHIFT_3) & UCPTRIE_INDEX_3_MASK;
            i3BlockLength = UCPTRIE_INDEX_3_BLOCK_LENGTH;
            dataBlockLength = UCPTRIE_SMALL_DATA_BLOCK_LENGTH;
        }
        // Enumerate data blocks for one index-3 block.
        do {
            int32_t block;
            if ((i3Block & 0x8000) == 0) {
                block = index[i3Block + i3];
            } else {
                // 18-bit indexes stored in groups of 9 entries per 8 indexes.
                int32_t group = (i3Block & 0x7fff) + (i3 & ~7) + (i3 >> 3);
                int32_t gi = i3 & 7;
                block = (index[group++] << (2 + (2 * gi))) & 0x30000;
                block |= index[group + gi];
            }
            if (block == prevBlock && (c - start) >= dataBlockLength) {
                // The block is the same as the previous one, and filled with value.
                U_ASSERT((c & (dataBlockLength - 1)) == 0);
                c += dataBlockLength;
            } else {
                int32_t dataMask = dataBlockLength - 1;
                prevBlock = block;
                if (block == trie->dataNullOffset) {
                    // This is the data null block.
                    if (haveValue) {
                        if (nullValue != value) {
                            return c - 1;
                        }
                    } else {
                        value = nullValue;
                        if (pValue != nullptr) { *pValue = nullValue; }
                        haveValue = true;
                    }
                    c = (c + dataBlockLength) & ~dataMask;
                } else {
                    int32_t di = block + (c & dataMask);
                    uint32_t value2 = getValue(trie->data, valueWidth, di);
                    value2 = maybeHandleValue(value2, trie->nullValue, nullValue,
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
                    while ((++c & dataMask) != 0) {
                        if (maybeHandleValue(getValue(trie->data, valueWidth, ++di),
                                             trie->nullValue, nullValue,
                                             handleValue, context) != value) {
                            return c - 1;
                        }
                    }
                }
            }
        } while (++i3 < i3BlockLength);
    } while (c < trie->highStart);
    U_ASSERT(haveValue);
    int32_t di = trie->dataLength - UCPTRIE_HIGH_VALUE_NEG_DATA_OFFSET;
    uint32_t highValue = getValue(trie->data, valueWidth, di);
    if (maybeHandleValue(highValue, trie->nullValue, nullValue,
                         handleValue, context) != value) {
        return c - 1;
    } else {
        return MAX_UNICODE;
    }
}

U_CAPI UChar32 U_EXPORT2
ucptrie_getRangeFixedSurr(const UCPTrie *trie, UChar32 start, UBool allSurr, uint32_t surrValue,
                          UCPTrieHandleValue *handleValue, const void *context, uint32_t *pValue) {
    uint32_t value;
    if (pValue == nullptr) {
        // We need to examine the range value even if the caller does not want it.
        pValue = &value;
    }
    UChar32 surrEnd = allSurr ? 0xdfff : 0xdbff;
    UChar32 end = ucptrie_getRange(trie, start, handleValue, context, pValue);
    if (end < 0xd7ff || start > surrEnd) {
        return end;
    }
    // The range overlaps with surrogates, or ends just before the first one.
    if (*pValue == surrValue) {
        if (end >= surrEnd) {
            // Surrogates followed by a non-surrValue range,
            // or surrogates are part of a larger surrValue range.
            return end;
        }
    } else {
        if (start <= 0xd7ff) {
            return 0xd7ff;  // Non-surrValue range ends before surrValue surrogates.
        }
        // Start is a surrogate with a non-surrValue code *unit* value.
        // Return a surrValue code *point* range.
        *pValue = surrValue;
        if (end > surrEnd) {
            return surrEnd;  // Inert surrogate range ends before non-surrValue rest of range.
        }
    }
    // See if the surrValue surrogate range can be merged with
    // an immediately following range.
    uint32_t value2;
    UChar32 end2 = ucptrie_getRange(trie, surrEnd + 1, handleValue, context, &value2);
    if (value2 == surrValue) {
        return end2;
    }
    return surrEnd;
}

U_CAPI int32_t U_EXPORT2
ucptrie_toBinary(const UCPTrie *trie,
                 void *data, int32_t capacity,
                 UErrorCode *pErrorCode) {
    if (U_FAILURE(*pErrorCode)) {
        return 0;
    }

    UCPTrieType type = (UCPTrieType)trie->type;
    UCPTrieValueWidth valueWidth = (UCPTrieValueWidth)trie->valueWidth;
    if (type < UCPTRIE_TYPE_FAST || UCPTRIE_TYPE_SMALL < type ||
            valueWidth < UCPTRIE_VALUE_BITS_16 || UCPTRIE_VALUE_BITS_8 < valueWidth ||
            capacity < 0 ||
            (capacity > 0 && (data == nullptr || (U_POINTER_MASK_LSB(data, 3) != 0)))) {
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    int32_t length = (int32_t)sizeof(UCPTrieHeader) + trie->indexLength * 2;
    switch (valueWidth) {
    case UCPTRIE_VALUE_BITS_16:
        length += trie->dataLength * 2;
        break;
    case UCPTRIE_VALUE_BITS_32:
        length += trie->dataLength * 4;
        break;
    case UCPTRIE_VALUE_BITS_8:
        length += trie->dataLength;
        break;
    default:
        // unreachable
        break;
    }
    if (capacity < length) {
        *pErrorCode = U_BUFFER_OVERFLOW_ERROR;
        return length;
    }

    char *bytes = (char *)data;
    UCPTrieHeader *header = (UCPTrieHeader *)bytes;
    header->signature = UCPTRIE_SIG;  // "Tri3"
    header->options = (uint16_t)(
        ((trie->dataLength & 0xf0000) >> 4) |
        ((trie->dataNullOffset & 0xf0000) >> 8) |
        (trie->type << 6) |
        valueWidth);
    header->indexLength = (uint16_t)trie->indexLength;
    header->dataLength = (uint16_t)trie->dataLength;
    header->index3NullOffset = trie->index3NullOffset;
    header->dataNullOffset = (uint16_t)trie->dataNullOffset;
    header->shiftedHighStart = trie->highStart >> UCPTRIE_SHIFT_2;
    bytes += sizeof(UCPTrieHeader);

    uprv_memcpy(bytes, trie->index, trie->indexLength * 2);
    bytes += trie->indexLength * 2;

    switch (valueWidth) {
    case UCPTRIE_VALUE_BITS_16:
        uprv_memcpy(bytes, trie->data.ptr16, trie->dataLength * 2);
        break;
    case UCPTRIE_VALUE_BITS_32:
        uprv_memcpy(bytes, trie->data.ptr32, trie->dataLength * 4);
        break;
    case UCPTRIE_VALUE_BITS_8:
        uprv_memcpy(bytes, trie->data.ptr8, trie->dataLength);
        break;
    default:
        // unreachable
        break;
    }
    return length;
}

// UTrie and UTrie2 signature values,
// in platform endianness and opposite endianness.
#define UTRIE_SIG       0x54726965
#define UTRIE_OE_SIG    0x65697254

#define UTRIE2_SIG      0x54726932
#define UTRIE2_OE_SIG   0x32697254

U_CAPI int32_t U_EXPORT2
ucptrie_getVersion(const void *data, int32_t length, UBool anyEndianOk) {
    uint32_t signature;
    if(length<16 || data==nullptr || (U_POINTER_MASK_LSB(data, 3)!=0)) {
        return 0;
    }
    signature=*(const uint32_t *)data;
    if(signature==UCPTRIE_SIG) {
        return 3;
    }
    if(anyEndianOk && signature==UCPTRIE_OE_SIG) {
        return 3;
    }
    if(signature==UTRIE2_SIG) {
        return 2;
    }
    if(anyEndianOk && signature==UTRIE2_OE_SIG) {
        return 2;
    }
    if(signature==UTRIE_SIG) {
        return 1;
    }
    if(anyEndianOk && signature==UTRIE_OE_SIG) {
        return 1;
    }
    return 0;
}

namespace {

#ifdef UCPTRIE_DEBUG
long countNull(const UCPTrie *trie) {
    uint32_t nullValue=trie->nullValue;
    int32_t length=trie->dataLength;
    long count=0;
    switch (trie->valueWidth) {
    case UCPTRIE_VALUE_BITS_16:
        for(int32_t i=0; i<length; ++i) {
            if(trie->data.ptr16[i]==nullValue) { ++count; }
        }
        break;
    case UCPTRIE_VALUE_BITS_32:
        for(int32_t i=0; i<length; ++i) {
            if(trie->data.ptr32[i]==nullValue) { ++count; }
        }
        break;
    case UCPTRIE_VALUE_BITS_8:
        for(int32_t i=0; i<length; ++i) {
            if(trie->data.ptr8[i]==nullValue) { ++count; }
        }
        break;
    default:
        // unreachable
        break;
    }
    return count;
}

U_CFUNC void
ucptrie_printLengths(const UCPTrie *trie, const char *which) {
    long indexLength=trie->indexLength;
    long dataLength=(long)trie->dataLength;
    long totalLength=(long)sizeof(UCPTrieHeader)+indexLength*2+
            dataLength*(trie->valueWidth==UCPTRIE_VALUE_BITS_16 ? 2 :
                        trie->valueWidth==UCPTRIE_VALUE_BITS_32 ? 4 : 1);
    printf("**UCPTrieLengths(%s %s)** index:%6ld  data:%6ld  countNull:%6ld  serialized:%6ld\n",
           which, trie->name, indexLength, dataLength, countNull(trie), totalLength);
}
#endif

}  // namespace

U_CAPI int32_t U_EXPORT2
ucptrie_swap(const UDataSwapper *ds,
             const void *inData, int32_t length, void *outData,
             UErrorCode *pErrorCode) {
    const UCPTrieHeader *inTrie;
    UCPTrieHeader trie;
    int32_t dataLength, size;
    UCPTrieValueWidth valueWidth;

    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(ds==nullptr || inData==nullptr || (length>=0 && outData==nullptr)) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    /* setup and swapping */
    if(length>=0 && length<(int32_t)sizeof(UCPTrieHeader)) {
        *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        return 0;
    }

    inTrie=(const UCPTrieHeader *)inData;
    trie.signature=ds->readUInt32(inTrie->signature);
    trie.options=ds->readUInt16(inTrie->options);
    trie.indexLength=ds->readUInt16(inTrie->indexLength);
    trie.dataLength = ds->readUInt16(inTrie->dataLength);

    UCPTrieType type = (UCPTrieType)((trie.options >> 6) & 3);
    valueWidth = (UCPTrieValueWidth)(trie.options & UCPTRIE_OPTIONS_VALUE_BITS_MASK);
    dataLength = ((int32_t)(trie.options & UCPTRIE_OPTIONS_DATA_LENGTH_MASK) << 4) | trie.dataLength;

    int32_t minIndexLength = type == UCPTRIE_TYPE_FAST ?
        UCPTRIE_BMP_INDEX_LENGTH : UCPTRIE_SMALL_INDEX_LENGTH;
    if( trie.signature!=UCPTRIE_SIG ||
        type > UCPTRIE_TYPE_SMALL ||
        (trie.options & UCPTRIE_OPTIONS_RESERVED_MASK) != 0 ||
        valueWidth > UCPTRIE_VALUE_BITS_8 ||
        trie.indexLength < minIndexLength ||
        dataLength < ASCII_LIMIT
    ) {
        *pErrorCode=U_INVALID_FORMAT_ERROR; /* not a UCPTrie */
        return 0;
    }

    size=sizeof(UCPTrieHeader)+trie.indexLength*2;
    switch(valueWidth) {
    case UCPTRIE_VALUE_BITS_16:
        size+=dataLength*2;
        break;
    case UCPTRIE_VALUE_BITS_32:
        size+=dataLength*4;
        break;
    case UCPTRIE_VALUE_BITS_8:
        size+=dataLength;
        break;
    default:
        *pErrorCode=U_INVALID_FORMAT_ERROR;
        return 0;
    }

    if(length>=0) {
        UCPTrieHeader *outTrie;

        if(length<size) {
            *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
            return 0;
        }

        outTrie=(UCPTrieHeader *)outData;

        /* swap the header */
        ds->swapArray32(ds, &inTrie->signature, 4, &outTrie->signature, pErrorCode);
        ds->swapArray16(ds, &inTrie->options, 12, &outTrie->options, pErrorCode);

        /* swap the index and the data */
        switch(valueWidth) {
        case UCPTRIE_VALUE_BITS_16:
            ds->swapArray16(ds, inTrie+1, (trie.indexLength+dataLength)*2, outTrie+1, pErrorCode);
            break;
        case UCPTRIE_VALUE_BITS_32:
            ds->swapArray16(ds, inTrie+1, trie.indexLength*2, outTrie+1, pErrorCode);
            ds->swapArray32(ds, (const uint16_t *)(inTrie+1)+trie.indexLength, dataLength*4,
                                     (uint16_t *)(outTrie+1)+trie.indexLength, pErrorCode);
            break;
        case UCPTRIE_VALUE_BITS_8:
            ds->swapArray16(ds, inTrie+1, trie.indexLength*2, outTrie+1, pErrorCode);
            if(inTrie!=outTrie) {
                uprv_memmove((outTrie+1)+trie.indexLength, (inTrie+1)+trie.indexLength, dataLength);
            }
            break;
        default:
            *pErrorCode=U_INVALID_FORMAT_ERROR;
            return 0;
        }
    }

    return size;
}

// ucptrie_swapAnyVersion() should be defined here but lives in ucptrie_builder.cpp
// to avoid a dependency from ucptrie.cpp on utrie.cpp.
