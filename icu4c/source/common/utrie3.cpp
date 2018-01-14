// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// utrie3.cpp (modified from utrie2.cpp)
// created: 2017dec29 Markus W. Scherer

#include "unicode/utypes.h"
#include "unicode/utf.h"
#include "unicode/utf8.h"
#include "unicode/utf16.h"
#include "cmemory.h"
#include "utrie3.h"
#include "utrie3_impl.h"
#include "uassert.h"

/* Public UTrie3 API implementation ----------------------------------------- */

U_CAPI uint32_t U_EXPORT2
utrie3_get32(const UTrie3 *trie, UChar32 c) {
    if(trie->data16!=NULL) {
        uint32_t result;
        UTRIE3_GET16(trie, c, result);
        return result;
    } else if(trie->data32!=NULL) {
        uint32_t result;
        UTRIE3_GET32(trie, c, result);
        return result;
    } else if((uint32_t)c>0x10ffff) {
        return trie->errorValue;
    } else if(c>=trie->highStart) {
        return trie->highValue;
    } else {
        const UNewTrie3 *newTrie=trie->newTrie;
        int32_t i2=newTrie->index1[c>>UTRIE3_SHIFT_1]+
            ((c>>UTRIE3_SHIFT_2)&UTRIE3_INDEX_2_MASK);
        int32_t block=newTrie->index2[i2];
        return newTrie->data[block+(c&UTRIE3_DATA_MASK)];
    }
}

U_CAPI int32_t U_EXPORT2
utrie3_internalIndexFromSuppPieces(const uint16_t *trieIndex, int32_t c1, int32_t c2, int32_t c3) {
    int32_t dataIndex;
    _UTRIE3_INDEX_FROM_SUPP_PIECES(trieIndex, c1, c2, c3, dataIndex);
    return dataIndex;
}

U_CAPI int32_t U_EXPORT2
utrie3_internalU8PrevIndex(const UTrie3 *trie, UChar32 c,
                           const uint8_t *start, const uint8_t *src) {
    int32_t i, length;
    /* support 64-bit pointers by avoiding cast of arbitrary difference */
    if((src-start)<=7) {
        i=length=(int32_t)(src-start);
    } else {
        i=length=7;
        start=src-7;
    }
    c=utf8_prevCharSafeBody(start, 0, &i, c, -1);
    i=length-i;  /* number of bytes read backward from src */
    if(c>=0) {
        int32_t idx;
        if(c<=0xffff) {
            idx=_UTRIE3_INDEX_FROM_BMP(trie->index, c);
        } else if(c>=trie->highStart) {
            return -16|i;  // for highValue
        } else {
            _UTRIE3_INDEX_FROM_SUPP(trie->index, c, idx);
        }
        return (idx<<3)|i;
    } else {
        return -8|i;  // for errorValue
    }
}

U_CAPI UTrie3 * U_EXPORT2
utrie3_openFromSerialized(UTrie3ValueBits valueBits,
                          const void *data, int32_t length, int32_t *pActualLength,
                          UErrorCode *pErrorCode) {
    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }

    if( length<=0 || (U_POINTER_MASK_LSB(data, 3)!=0) ||
        valueBits<0 || UTRIE3_COUNT_VALUE_BITS<=valueBits
    ) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    /* enough data for a trie header? */
    if(length<(int32_t)sizeof(UTrie3Header)) {
        *pErrorCode=U_INVALID_FORMAT_ERROR;
        return 0;
    }

    /* check the signature */
    const UTrie3Header *header=(const UTrie3Header *)data;
    if(header->signature!=UTRIE3_SIG) {
        *pErrorCode=U_INVALID_FORMAT_ERROR;
        return 0;
    }

    /* get the options */
    if(valueBits!=(UTrie3ValueBits)(header->options&UTRIE3_OPTIONS_VALUE_BITS_MASK)) {
        *pErrorCode=U_INVALID_FORMAT_ERROR;
        return 0;
    }

    /* get the length values and offsets */
    UTrie3 tempTrie;
    uprv_memset(&tempTrie, 0, sizeof(tempTrie));
    tempTrie.indexLength=header->indexLength;
    tempTrie.dataLength=header->shiftedDataLength<<UTRIE3_INDEX_SHIFT;
    tempTrie.index2NullOffset=header->index2NullOffset;
    tempTrie.dataNullOffset=header->dataNullOffset;

    tempTrie.highStart=header->shiftedHighStart<<UTRIE3_SHIFT_1;
    tempTrie.highStartLead16=U16_LEAD(tempTrie.highStart);
    tempTrie.shiftedHighStart=header->shiftedHighStart;
    tempTrie.highValue=header->highValue;
    tempTrie.errorValue=header->errorValue;

    /* calculate the actual length */
    int32_t actualLength=(int32_t)sizeof(UTrie3Header)+tempTrie.indexLength*2;
    if(valueBits==UTRIE3_16_VALUE_BITS) {
        actualLength+=tempTrie.dataLength*2;
    } else {
        actualLength+=tempTrie.dataLength*4;
    }
    if(length<actualLength) {
        *pErrorCode=U_INVALID_FORMAT_ERROR;  /* not enough bytes */
        return 0;
    }

    /* allocate the trie */
    UTrie3 *trie=(UTrie3 *)uprv_malloc(sizeof(UTrie3));
    if(trie==NULL) {
        *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
        return 0;
    }
    uprv_memcpy(trie, &tempTrie, sizeof(tempTrie));
    trie->memory=(uint32_t *)data;
    trie->length=actualLength;
    trie->isMemoryOwned=FALSE;
    trie->name="fromSerialized";

    /* set the pointers to its index and data arrays */
    const uint16_t *p16=(const uint16_t *)(header+1);
    trie->index=p16;
    p16+=trie->indexLength;

    /* get the data */
    switch(valueBits) {
    case UTRIE3_16_VALUE_BITS:
        trie->data16=p16;
        trie->data32=NULL;
        trie->initialValue=trie->index[trie->dataNullOffset];
        break;
    case UTRIE3_32_VALUE_BITS:
        trie->data16=NULL;
        trie->data32=(const uint32_t *)p16;
        trie->initialValue=trie->data32[trie->dataNullOffset];
        break;
    default:
        *pErrorCode=U_INVALID_FORMAT_ERROR;
        return 0;
    }

    if(pActualLength!=NULL) {
        *pActualLength=actualLength;
    }
    return trie;
}

U_CAPI UTrie3 * U_EXPORT2
utrie3_openDummy(UTrie3ValueBits valueBits,
                 uint32_t initialValue, uint32_t errorValue,
                 UErrorCode *pErrorCode) {
    if(U_FAILURE(*pErrorCode)) {
        return nullptr;
    }

    if(valueBits<0 || UTRIE3_COUNT_VALUE_BITS<=valueBits) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return nullptr;
    }

    /* calculate the total length of the dummy trie data */
    int32_t indexLength=UTRIE3_INDEX_1_OFFSET;
    int32_t dataLength=UTRIE3_DATA_START_OFFSET;
    int32_t length=(int32_t)sizeof(UTrie3Header)+indexLength*2;
    if(valueBits==UTRIE3_16_VALUE_BITS) {
        length+=dataLength*2;
    } else {
        length+=dataLength*4;
    }

    /* allocate the trie */
    char *bytes=(char *)uprv_malloc(sizeof(UTrie3)+length);
    if(bytes==nullptr) {
        *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    UTrie3 *trie=(UTrie3 *)bytes;
    uprv_memset(trie, 0, sizeof(UTrie3));
    trie->memory=bytes+sizeof(UTrie3);
    trie->length=length;

    /* set the UTrie3 fields */
    int32_t dataMove;  // >0 if the data is moved to the end of the index array
    if(valueBits==UTRIE3_16_VALUE_BITS) {
        dataMove=indexLength;
    } else {
        dataMove=0;
    }

    trie->indexLength=indexLength;
    trie->dataLength=dataLength;
    trie->index2NullOffset=UTRIE3_INDEX_2_OFFSET;
    trie->dataNullOffset=(uint16_t)dataMove;
    trie->initialValue=initialValue;
    trie->errorValue=errorValue;
    trie->highStart=0;
    trie->highStartLead16=0xd7c0;  // U16_LEAD(0) (below lead surrogates)
    trie->shiftedHighStart=0;
    trie->highValue=initialValue;
    trie->name="dummy";

    /* set the header fields */
    UTrie3Header *header=(UTrie3Header *)trie->memory;

    header->signature=UTRIE3_SIG; /* "Tri2" */
    header->options=(uint16_t)valueBits;

    header->indexLength=(uint16_t)indexLength;
    header->shiftedDataLength=(uint16_t)(dataLength>>UTRIE3_INDEX_SHIFT);
    header->index2NullOffset=(uint16_t)UTRIE3_INDEX_2_OFFSET;
    header->dataNullOffset=(uint16_t)dataMove;
    header->shiftedHighStart=0;

    /* fill the index and data arrays */
    uint16_t *dest16=(uint16_t *)(header+1);
    trie->index=dest16;

    /* write BMP index-2 array values, not right-shifted */
    int32_t i;
    for(i=0; i<UTRIE3_INDEX_2_BMP_LENGTH; ++i) {
        *dest16++=(uint16_t)dataMove;  /* null data block */
    }

    /* write the 16/32-bit data array */
    switch(valueBits) {
    case UTRIE3_16_VALUE_BITS:
        /* write 16-bit data values */
        trie->data16=dest16;
        trie->data32=NULL;
        for(i=0; i<0x80; ++i) {
            *dest16++=(uint16_t)initialValue;
        }
        break;
    case UTRIE3_32_VALUE_BITS: {
        /* write 32-bit data values */
        uint32_t *p=(uint32_t *)dest16;
        trie->data16=NULL;
        trie->data32=p;
        for(i=0; i<0x80; ++i) {
            *p++=initialValue;
        }
        break;
    }
    default:
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return nullptr;
    }

    return trie;
}

U_CAPI void U_EXPORT2
utrie3_close(UTrie3 *trie) {
    if(trie!=NULL) {
        if(trie->isMemoryOwned) {
            uprv_free(trie->memory);
        }
        if(trie->newTrie!=NULL) {
            uprv_free(trie->newTrie->data);
            uprv_free(trie->newTrie);
        }
        uprv_free(trie);
    }
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

U_CAPI UBool U_EXPORT2
utrie3_isFrozen(const UTrie3 *trie) {
    return (UBool)(trie->newTrie==NULL);
}

U_CAPI int32_t U_EXPORT2
utrie3_serialize(const UTrie3 *trie,
                 void *data, int32_t capacity,
                 UErrorCode *pErrorCode) {
    /* argument check */
    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }

    if( trie==NULL || trie->memory==NULL || trie->newTrie!=NULL ||
        capacity<0 || (capacity>0 && (data==NULL || (U_POINTER_MASK_LSB(data, 3)!=0)))
    ) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    if(capacity>=trie->length) {
        uprv_memcpy(data, trie->memory, trie->length);
    } else {
        *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
    }
    return trie->length;
}

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
    trie.shiftedDataLength=ds->readUInt16(inTrie->shiftedDataLength);

    valueBits=(UTrie3ValueBits)(trie.options&UTRIE3_OPTIONS_VALUE_BITS_MASK);
    dataLength=(int32_t)trie.shiftedDataLength<<UTRIE3_INDEX_SHIFT;

    if( trie.signature!=UTRIE3_SIG ||
        valueBits<0 || UTRIE3_COUNT_VALUE_BITS<=valueBits ||
        trie.indexLength<UTRIE3_INDEX_1_OFFSET ||
        dataLength<UTRIE3_DATA_START_OFFSET
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
        ds->swapArray32(ds, &inTrie->highValue, 8, &outTrie->highValue, pErrorCode);

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

// utrie3_swapAnyVersion() should be defined here but lives in utrie3_builder.c
// to avoid a dependency from utrie3.cpp on utrie.c.

/* enumeration -------------------------------------------------------------- */

#define MIN_VALUE(a, b) ((a)<(b) ? (a) : (b))

/* default UTrie3EnumValue() returns the input value itself */
static uint32_t U_CALLCONV
enumSameValue(const void * /*context*/, uint32_t value) {
    return value;
}

/**
 * Enumerate all ranges of code points with the same relevant values.
 * The values are transformed from the raw trie entries by the enumValue function.
 *
 * Currently requires start<limit and both start and limit must be multiples
 * of UTRIE3_DATA_BLOCK_LENGTH.
 *
 * Optimizations:
 * - Skip a whole block if we know that it is filled with a single value,
 *   and it is the same as we visited just before.
 * - Handle the null block specially because we know a priori that it is filled
 *   with a single value.
 */
static void
enumEitherTrie(const UTrie3 *trie,
               UChar32 start, UChar32 limit,
               UTrie3EnumValue *enumValue, UTrie3EnumRange *enumRange, const void *context) {
    const uint32_t *data32;
    const uint16_t *idx;

    uint32_t value, prevValue, initialValue;
    UChar32 c, prev, highStart;
    int32_t j, i2Block, prevI2Block, index2NullOffset, block, prevBlock, nullBlock;

    if(enumRange==NULL) {
        return;
    }
    if(enumValue==NULL) {
        enumValue=enumSameValue;
    }

    if(trie->newTrie==NULL) {
        /* frozen trie */
        idx=trie->index;
        U_ASSERT(idx!=NULL); /* the following code assumes trie->newTrie is not NULL when idx is NULL */
        data32=trie->data32;

        index2NullOffset=trie->index2NullOffset;
        nullBlock=trie->dataNullOffset;
    } else {
        /* unfrozen, mutable trie */
        idx=NULL;
        data32=trie->newTrie->data;
        U_ASSERT(data32!=NULL); /* the following code assumes idx is not NULL when data32 is NULL */

        index2NullOffset=trie->newTrie->index2NullOffset;
        nullBlock=trie->newTrie->dataNullOffset;
    }

    highStart=trie->highStart;

    /* get the enumeration value that corresponds to an initial-value trie data entry */
    initialValue=enumValue(context, trie->initialValue);

    /* set variables for previous range */
    prevI2Block=-1;
    prevBlock=-1;
    prev=start;
    prevValue=0;

    /* enumerate index-2 blocks */
    for(c=start; c<limit && c<highStart;) {
        /* Code point limit for iterating inside this i2Block. */
        UChar32 tempLimit=c+UTRIE3_CP_PER_INDEX_1_ENTRY;
        if(limit<tempLimit) {
            tempLimit=limit;
        }
        if(c<=0xffff) {
            i2Block=c>>UTRIE3_SHIFT_2;
        } else {
            /* supplementary code points */
            if(idx!=NULL) {
                i2Block=idx[(UTRIE3_INDEX_1_OFFSET-UTRIE3_OMITTED_BMP_INDEX_1_LENGTH)+
                              (c>>UTRIE3_SHIFT_1)];
            } else {
                i2Block=trie->newTrie->index1[c>>UTRIE3_SHIFT_1];
            }
            if(i2Block==prevI2Block && (c-prev)>=UTRIE3_CP_PER_INDEX_1_ENTRY) {
                /*
                 * The index-2 block is the same as the previous one, and filled with prevValue.
                 * Only possible for supplementary code points because the linear-BMP index-2
                 * table creates unique i2Block values.
                 */
                c+=UTRIE3_CP_PER_INDEX_1_ENTRY;
                continue;
            }
        }
        prevI2Block=i2Block;
        if(i2Block==index2NullOffset) {
            /* this is the null index-2 block */
            if(prevValue!=initialValue) {
                if(prev<c && !enumRange(context, prev, c-1, prevValue)) {
                    return;
                }
                prevBlock=nullBlock;
                prev=c;
                prevValue=initialValue;
            }
            c+=UTRIE3_CP_PER_INDEX_1_ENTRY;
        } else {
            /* enumerate data blocks for one index-2 block */
            int32_t i2, i2Limit;
            i2=(c>>UTRIE3_SHIFT_2)&UTRIE3_INDEX_2_MASK;
            if((c>>UTRIE3_SHIFT_1)==(tempLimit>>UTRIE3_SHIFT_1)) {
                i2Limit=(tempLimit>>UTRIE3_SHIFT_2)&UTRIE3_INDEX_2_MASK;
            } else {
                i2Limit=UTRIE3_INDEX_2_BLOCK_LENGTH;
            }
            for(; i2<i2Limit; ++i2) {
                if(idx!=NULL) {
                    block=(int32_t)idx[i2Block+i2];
                    if(i2Block>=UTRIE3_INDEX_2_BMP_LENGTH) {
                        block<<=UTRIE3_INDEX_SHIFT;
                    }
                } else {
                    block=trie->newTrie->index2[i2Block+i2];
                }
                if(block==prevBlock && (c-prev)>=UTRIE3_DATA_BLOCK_LENGTH) {
                    /* the block is the same as the previous one, and filled with prevValue */
                    c+=UTRIE3_DATA_BLOCK_LENGTH;
                    continue;
                }
                prevBlock=block;
                if(block==nullBlock) {
                    /* this is the null data block */
                    if(prevValue!=initialValue) {
                        if(prev<c && !enumRange(context, prev, c-1, prevValue)) {
                            return;
                        }
                        prev=c;
                        prevValue=initialValue;
                    }
                    c+=UTRIE3_DATA_BLOCK_LENGTH;
                } else {
                    for(j=0; j<UTRIE3_DATA_BLOCK_LENGTH; ++j) {
                        value=enumValue(context, data32!=NULL ? data32[block+j] : idx[block+j]);
                        if(value!=prevValue) {
                            if(prev<c && !enumRange(context, prev, c-1, prevValue)) {
                                return;
                            }
                            prev=c;
                            prevValue=value;
                        }
                        ++c;
                    }
                }
            }
        }
    }

    if(c>limit) {
        c=limit;  /* could be higher if in the index2NullOffset */
    } else if(c<limit) {
        /* c==highStart<limit */
        value=enumValue(context, trie->highValue);
        if(value!=prevValue) {
            if(prev<c && !enumRange(context, prev, c-1, prevValue)) {
                return;
            }
            prev=c;
            prevValue=value;
        }
        c=limit;
    }

    /* deliver last range */
    enumRange(context, prev, c-1, prevValue);
}

U_CAPI void U_EXPORT2
utrie3_enum(const UTrie3 *trie,
            UTrie3EnumValue *enumValue, UTrie3EnumRange *enumRange, const void *context) {
    enumEitherTrie(trie, 0, 0x110000, enumValue, enumRange, context);
}

U_CAPI void U_EXPORT2
utrie3_enumForLeadSurrogate(const UTrie3 *trie, UChar32 lead,
                            UTrie3EnumValue *enumValue, UTrie3EnumRange *enumRange,
                            const void *context) {
    if(!U16_IS_LEAD(lead)) {
        return;
    }
    lead=(lead-0xd7c0)<<10;   /* start code point */
    enumEitherTrie(trie, lead, lead+0x400, enumValue, enumRange, context);
}

/* C++ convenience wrappers ------------------------------------------------- */

U_NAMESPACE_BEGIN

uint16_t BackwardUTrie3StringIterator::previous16() {
    codePointLimit=codePointStart;
    if(start>=codePointStart) {
        codePoint=U_SENTINEL;
        return trie->errorValue;
    }
    uint16_t result;
    UTRIE3_U16_PREV16(trie, start, codePointStart, codePoint, result);
    return result;
}

uint16_t ForwardUTrie3StringIterator::next16() {
    codePointStart=codePointLimit;
    if(codePointLimit==limit) {
        codePoint=U_SENTINEL;
        return trie->errorValue;
    }
    uint16_t result;
    UTRIE3_U16_NEXT16(trie, codePointLimit, limit, codePoint, result);
    return result;
}

U_NAMESPACE_END
