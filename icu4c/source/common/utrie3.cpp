// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// utrie3.cpp (modified from utrie2.cpp)
// created: 2017dec29 Markus W. Scherer

#define UTRIE3_DEBUG  // TODO
#ifdef UTRIE3_DEBUG
#   include <stdio.h>
#endif

#include "unicode/utypes.h"
#include "unicode/utf.h"
#include "unicode/utf8.h"
#include "unicode/utf16.h"
#include "cmemory.h"
#include "utrie3.h"
#include "utrie3_impl.h"
#include "uassert.h"

/* Public UTrie3 API implementation ----------------------------------------- */

U_CAPI UTrie3 * U_EXPORT2
utrie3_openFromSerialized(UTrie3ValueBits valueBits,
                          const void *data, int32_t length, int32_t *pActualLength,
                          UErrorCode *pErrorCode) {
    if (U_FAILURE(*pErrorCode)) {
        return 0;
    }

    if (length<=0 || (U_POINTER_MASK_LSB(data, 3)!=0) ||
            valueBits < 0 || UTRIE3_32_VALUE_BITS < valueBits) {
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    // enough data for a trie header?
    if (length < (int32_t)sizeof(UTrie3Header)) {
        *pErrorCode = U_INVALID_FORMAT_ERROR;
        return 0;
    }

    // Check the signature.
    const UTrie3Header *header = (const UTrie3Header *)data;
    if (header->signature != UTRIE3_SIG) {
        *pErrorCode = U_INVALID_FORMAT_ERROR;
        return 0;
    }

    int32_t options = header->options;
    if (valueBits != (UTrie3ValueBits)(options & UTRIE3_OPTIONS_VALUE_BITS_MASK) ||
            (options & UTRIE3_OPTIONS_RESERVED_MASK) != 0) {
        *pErrorCode = U_INVALID_FORMAT_ERROR;
        return 0;
    }

    // Get the length values and offsets.
    UTrie3 tempTrie;
    uprv_memset(&tempTrie, 0, sizeof(tempTrie));
    tempTrie.indexLength = header->indexLength;
    tempTrie.dataLength =
        ((options & UTRIE3_OPTIONS_DATA_LENGTH_MASK) << 4) | header->dataLength;
    tempTrie.index2NullOffset = header->index2NullOffset;
    tempTrie.dataNullOffset =
        ((options & UTRIE3_OPTIONS_DATA_NULL_OFFSET_MASK) << 8) | header->dataNullOffset;

    tempTrie.highStart = header->shiftedHighStart << UTRIE3_SUPP_SHIFT_1;
    tempTrie.shifted12HighStart = (tempTrie.highStart + 0xfff) >> 12;

    // Calculate the actual length.
    int32_t actualLength = (int32_t)sizeof(UTrie3Header) + tempTrie.indexLength * 2;
    if (valueBits == UTRIE3_16_VALUE_BITS) {
        actualLength += tempTrie.dataLength * 2;
    } else {
        actualLength += tempTrie.dataLength * 4;
    }
    if (length < actualLength) {
        *pErrorCode = U_INVALID_FORMAT_ERROR;  // Not enough bytes.
        return 0;
    }

    // Allocate the trie.
    UTrie3 *trie = (UTrie3 *)uprv_malloc(sizeof(UTrie3));
    if (trie == nullptr) {
        *pErrorCode = U_MEMORY_ALLOCATION_ERROR;
        return 0;
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
        nullValueOffset = trie->dataLength - UTRIE3_HIGH_VALUE_NEG_DATA_OFFSET;
    }
    switch (valueBits) {
    case UTRIE3_16_VALUE_BITS:
        trie->data16 = p16;
        trie->data32 = nullptr;
        trie->nullValue = trie->data16[nullValueOffset];
        break;
    case UTRIE3_32_VALUE_BITS:
        trie->data16 = nullptr;
        trie->data32 = (const uint32_t *)p16;
        trie->nullValue = trie->data32[nullValueOffset];
        break;
    default:
        *pErrorCode = U_INVALID_FORMAT_ERROR;
        return 0;
    }

    if (pActualLength != nullptr) {
        *pActualLength = actualLength;
    }
    return trie;
}

U_CAPI void U_EXPORT2
utrie3_close(UTrie3 *trie) {
    uprv_free(trie);
}

U_CAPI int32_t U_EXPORT2
utrie3_internalIndexFromSupp(const UTrie3 *trie, UChar32 c) {
    U_ASSERT(0xffff < c && c < trie->highStart);
    int32_t i1Block = trie->index[UTRIE3_BMP_INDEX_LENGTH - UTRIE3_OMITTED_BMP_INDEX_0_LENGTH +
                                  (c >> UTRIE3_SUPP_SHIFT_0)];
    int32_t i2Block = trie->index[i1Block + ((c >> UTRIE3_SUPP_SHIFT_1) & UTRIE3_INDEX_1_MASK)];
    int32_t i2 = (c >> UTRIE3_SUPP_SHIFT_2) & UTRIE3_INDEX_2_MASK;
    int32_t dataBlock;
    if ((i2Block & 0x8000) == 0) {
        // 16-bit indexes
        dataBlock = trie->index[i2Block + i2];
    } else {
        // 18-bit indexes stored in groups of 9 entries per 8 indexes.
        i2Block = (i2Block & 0x7fff) + (i2 & ~7) + (i2 >> 3);
        i2 &= 7;
        dataBlock = (trie->index[i2Block++] << (2 + (2 * i2))) & 0x30000;
        dataBlock |= trie->index[i2Block + i2];
    }
    return dataBlock + (c & UTRIE3_SUPP_DATA_MASK);
}

U_CAPI int32_t U_EXPORT2
utrie3_internalIndexFromSuppU8(const UTrie3 *trie, int32_t lt1, uint8_t t2, uint8_t t3) {
    UChar32 c = (lt1 << 12) | (t2 << 6) | t3;
    if (c >= trie->highStart) {
        // Possible because the UTF-8 macro compares with shifted12HighStart which may be higher.
        return trie->dataLength - UTRIE3_HIGH_VALUE_NEG_DATA_OFFSET;
    }
    return utrie3_internalIndexFromSupp(trie, c);
}

U_CAPI int32_t U_EXPORT2
utrie3_internalU8PrevIndex(const UTrie3 *trie, UChar32 c,
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
    int32_t idx = _UTRIE3_INDEX_FROM_CP(trie, c);
    return (idx << 3) | i;
}

U_CAPI uint32_t U_EXPORT2
utrie3_get(const UTrie3 *trie, UChar32 c) {
    int32_t dataIndex;
    if ((uint32_t)c <= 0x7f) {
        // linear ASCII
        dataIndex = c;
    } else {
        dataIndex = _UTRIE3_INDEX_FROM_CP(trie, c);
    }
    if (trie->data32 == nullptr) {
        return trie->data16[dataIndex];
    } else {
        return trie->data32[dataIndex];
    }
}

namespace {

constexpr int32_t MAX_UNICODE = 0x10ffff;

constexpr int32_t ASCII_LIMIT = 0x80;

inline uint32_t maybeHandleValue(uint32_t value, uint32_t trieNullValue, uint32_t nullValue,
                                 UTrie3HandleValue *handleValue, const void *context) {
    if (value == trieNullValue) {
        value = nullValue;
    } else if (handleValue != nullptr) {
        value = handleValue(context, value);
    }
    return value;
}

}  // namespace

U_CAPI int32_t U_EXPORT2
utrie3_getRange(const UTrie3 *trie, UChar32 start,
                UTrie3HandleValue *handleValue, const void *context, uint32_t *pValue) {
    if ((uint32_t)start > MAX_UNICODE) {
        return U_SENTINEL;
    }
    const uint16_t *data16 = trie->data16;
    const uint32_t *data32 = trie->data32;
    if (start >= trie->highStart) {
        if (pValue != nullptr) {
            int32_t di = trie->dataLength - UTRIE3_HIGH_VALUE_NEG_DATA_OFFSET;
            uint32_t value = data32 != nullptr ? data32[di] : data16[di];
            if (handleValue != nullptr) { value = handleValue(context, value); }
            *pValue = value;
        }
        return MAX_UNICODE;
    }

    uint32_t nullValue = trie->nullValue;
    if (handleValue != nullptr) { nullValue = handleValue(context, nullValue); }
    const uint16_t *index = trie->index;

    int32_t prevI2Block = -1;
    int32_t prevBlock = -1;
    UChar32 c = start;
    uint32_t value;
    bool haveValue = false;
    do {
        int32_t i2Block;
        int32_t i2;
        int32_t i2BlockLength;
        int32_t dataBlockLength;
        if (c <= 0xffff) {
            i2Block = 0;
            i2 = c >> UTRIE3_BMP_SHIFT;
            i2BlockLength = UTRIE3_BMP_INDEX_LENGTH;
            dataBlockLength = UTRIE3_BMP_DATA_BLOCK_LENGTH;
        } else {
            // Supplementary code points
            int32_t i1Block = trie->index[UTRIE3_BMP_INDEX_LENGTH - UTRIE3_OMITTED_BMP_INDEX_0_LENGTH +
                                          (c >> UTRIE3_SUPP_SHIFT_0)];
            i2Block = trie->index[i1Block + ((c >> UTRIE3_SUPP_SHIFT_1) & UTRIE3_INDEX_1_MASK)];
            if (i2Block == prevI2Block && (c - start) >= UTRIE3_CP_PER_INDEX_1_ENTRY) {
                // The index-2 block is the same as the previous one, and filled with value.
                U_ASSERT((c & (UTRIE3_CP_PER_INDEX_1_ENTRY - 1)) == 0);
                c += UTRIE3_CP_PER_INDEX_1_ENTRY;
                continue;
            }
            prevI2Block = i2Block;
            if (i2Block == trie->index2NullOffset) {
                // This is the index-2 null block.
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
                c = (c + UTRIE3_CP_PER_INDEX_1_ENTRY) & ~(UTRIE3_CP_PER_INDEX_1_ENTRY - 1);
                continue;
            }
            i2 = (c >> UTRIE3_SUPP_SHIFT_2) & UTRIE3_INDEX_2_MASK;
            i2BlockLength = UTRIE3_INDEX_2_BLOCK_LENGTH;
            dataBlockLength = UTRIE3_SUPP_DATA_BLOCK_LENGTH;
        }
        // Enumerate data blocks for one index-2 block.
        do {
            int32_t block;
            if ((i2Block & 0x8000) == 0) {
                block = index[i2Block + i2];
            } else {
                // 18-bit indexes stored in groups of 9 entries per 8 indexes.
                int32_t group = (i2Block & 0x7fff) + (i2 & ~7) + (i2 >> 3);
                int32_t gi = i2 & 7;
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
                    uint32_t value2 = data32 != nullptr ? data32[di] : data16[di];
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
                        if (maybeHandleValue(data32 != nullptr ? data32[++di] : data16[++di],
                                             trie->nullValue, nullValue,
                                             handleValue, context) != value) {
                            return c - 1;
                        }
                    }
                }
            }
        } while (++i2 < i2BlockLength);
    } while (c < trie->highStart);
    U_ASSERT(haveValue);
    int32_t di = trie->dataLength - UTRIE3_HIGH_VALUE_NEG_DATA_OFFSET;
    uint32_t highValue = data32 != nullptr ? data32[di] : data16[di];
    if (maybeHandleValue(highValue, trie->nullValue, nullValue,
                         handleValue, context) != value) {
        return c - 1;
    } else {
        return MAX_UNICODE;
    }
}

U_CAPI int32_t U_EXPORT2
utrie3_serialize(const UTrie3 *trie,
                 void *data, int32_t capacity,
                 UErrorCode *pErrorCode) {
    if (U_FAILURE(*pErrorCode)) {
        return 0;
    }

    if (trie == nullptr || capacity < 0 ||
            (capacity > 0 && (data == nullptr || (U_POINTER_MASK_LSB(data, 3) != 0)))) {
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    UTrie3ValueBits valueBits;
    int32_t length = (int32_t)sizeof(UTrie3Header) + trie->indexLength * 2;
    if (trie->data16 != nullptr) {
        valueBits = UTRIE3_16_VALUE_BITS;
        length += trie->dataLength * 2;
    } else {
        valueBits = UTRIE3_32_VALUE_BITS;
        length += trie->dataLength * 4;
    }
    if (capacity < length) {
        *pErrorCode = U_BUFFER_OVERFLOW_ERROR;
        return length;
    }

    char *bytes = (char *)data;
    UTrie3Header *header = (UTrie3Header *)bytes;
    header->signature = UTRIE3_SIG;  // "Tri3"
    header->options = (uint16_t)(
        ((trie->dataLength & 0xf0000) >> 4) |
        ((trie->dataNullOffset & 0xf0000) >> 8) |
        valueBits);
    header->indexLength = (uint16_t)trie->indexLength;
    header->dataLength = (uint16_t)trie->dataLength;
    header->index2NullOffset = trie->index2NullOffset;
    header->dataNullOffset = (uint16_t)trie->dataNullOffset;
    header->shiftedHighStart = trie->highStart >> UTRIE3_SUPP_SHIFT_1;
    bytes += sizeof(UTrie3Header);

    uprv_memcpy(bytes, trie->index, trie->indexLength * 2);
    bytes += trie->indexLength * 2;

    if (trie->data16 != nullptr) {
        uprv_memcpy(bytes, trie->data16, trie->dataLength * 2);
    } else {
        uprv_memcpy(bytes, trie->data32, trie->dataLength * 4);
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
utrie3_getVersion(const void *data, int32_t length, UBool anyEndianOk) {
    uint32_t signature;
    if(length<16 || data==NULL || (U_POINTER_MASK_LSB(data, 3)!=0)) {
        return 0;
    }
    signature=*(const uint32_t *)data;
    if(signature==UTRIE3_SIG) {
        return 3;
    }
    if(anyEndianOk && signature==UTRIE3_OE_SIG) {
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

#ifdef UTRIE3_DEBUG
long countNull(const UTrie3 *trie) {
    uint32_t nullValue=trie->nullValue;
    int32_t length=trie->dataLength;
    long count=0;
    if(trie->data16!=nullptr) {
        for(int32_t i=0; i<length; ++i) {
            if(trie->data16[i]==nullValue) { ++count; }
        }
    } else {
        for(int32_t i=0; i<length; ++i) {
            if(trie->data32[i]==nullValue) { ++count; }
        }
    }
    return count;
}

U_CFUNC void
utrie3_printLengths(const UTrie3 *trie, const char *which) {
    long indexLength=trie->indexLength;
    long dataLength=(long)trie->dataLength;
    long totalLength=(long)sizeof(UTrie3Header)+indexLength*2+dataLength*(trie->data32!=NULL ? 4 : 2);
    printf("**UTrie3Lengths(%s %s)** index:%6ld  data:%6ld  countNull:%6ld  serialized:%6ld\n",
           which, trie->name, indexLength, dataLength, countNull(trie), totalLength);
}
#endif

}  // namespace

U_CAPI int32_t U_EXPORT2
utrie3_swap(const UDataSwapper *ds,
            const void *inData, int32_t length, void *outData,
            UErrorCode *pErrorCode) {
    const UTrie3Header *inTrie;
    UTrie3Header trie;
    int32_t dataLength, size;
    UTrie3ValueBits valueBits;

    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(ds==NULL || inData==NULL || (length>=0 && outData==NULL)) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    /* setup and swapping */
    if(length>=0 && length<(int32_t)sizeof(UTrie3Header)) {
        *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        return 0;
    }

    inTrie=(const UTrie3Header *)inData;
    trie.signature=ds->readUInt32(inTrie->signature);
    trie.options=ds->readUInt16(inTrie->options);
    trie.indexLength=ds->readUInt16(inTrie->indexLength);
    trie.dataLength = ds->readUInt16(inTrie->dataLength);

    valueBits=(UTrie3ValueBits)(trie.options&UTRIE3_OPTIONS_VALUE_BITS_MASK);
    dataLength = ((int32_t)(trie.options & UTRIE3_OPTIONS_DATA_LENGTH_MASK) << 4) | trie.dataLength;

    if( trie.signature!=UTRIE3_SIG ||
        UTRIE3_32_VALUE_BITS < valueBits ||
        (trie.options & UTRIE3_OPTIONS_RESERVED_MASK) != 0 ||
        trie.indexLength < UTRIE3_BMP_INDEX_LENGTH ||
        dataLength < ASCII_LIMIT
    ) {
        *pErrorCode=U_INVALID_FORMAT_ERROR; /* not a UTrie */
        return 0;
    }

    size=sizeof(UTrie3Header)+trie.indexLength*2;
    switch(valueBits) {
    case UTRIE3_16_VALUE_BITS:
        size+=dataLength*2;
        break;
    case UTRIE3_32_VALUE_BITS:
        size+=dataLength*4;
        break;
    default:
        *pErrorCode=U_INVALID_FORMAT_ERROR;
        return 0;
    }

    if(length>=0) {
        UTrie3Header *outTrie;

        if(length<size) {
            *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
            return 0;
        }

        outTrie=(UTrie3Header *)outData;

        /* swap the header */
        ds->swapArray32(ds, &inTrie->signature, 4, &outTrie->signature, pErrorCode);
        ds->swapArray16(ds, &inTrie->options, 12, &outTrie->options, pErrorCode);

        /* swap the index and the data */
        switch(valueBits) {
        case UTRIE3_16_VALUE_BITS:
            ds->swapArray16(ds, inTrie+1, (trie.indexLength+dataLength)*2, outTrie+1, pErrorCode);
            break;
        case UTRIE3_32_VALUE_BITS:
            ds->swapArray16(ds, inTrie+1, trie.indexLength*2, outTrie+1, pErrorCode);
            ds->swapArray32(ds, (const uint16_t *)(inTrie+1)+trie.indexLength, dataLength*4,
                                     (uint16_t *)(outTrie+1)+trie.indexLength, pErrorCode);
            break;
        default:
            *pErrorCode=U_INVALID_FORMAT_ERROR;
            return 0;
        }
    }

    return size;
}

// utrie3_swapAnyVersion() should be defined here but lives in utrie3_builder.cpp
// to avoid a dependency from utrie3.cpp on utrie.cpp.
