// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
**********************************************************************
*   Copyright (C) 2002-2016, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   10/11/02    aliu        Creation.
*   2010nov19   Markus Scherer  Rewrite for formatVersion 2.
*   2011dec18   Markus Scherer  Moved genpname/genpname.cpp to genprops/pnamesbuilder.cpp.
**********************************************************************
*/

#include "unicode/utypes.h"
#include "unicode/bytestrie.h"
#include "unicode/bytestriebuilder.h"
#include "unicode/putil.h"
#include "unicode/uclean.h"
#include "charstr.h"
#include "cstring.h"
#include "denseranges.h"
#include "genprops.h"
#include "propname.h"
#include "toolutil.h"
#include "uhash.h"
#include "uinvchar.h"
#include "unewdata.h"
#include "uvectr32.h"
#include "writesrc.h"

#include <stdio.h>

// We test for ASCII delimiters and White_Space, and build ASCII string BytesTries.
#if U_CHARSET_FAMILY!=U_ASCII_FAMILY
#   error This builder requires U_CHARSET_FAMILY==U_ASCII_FAMILY.
#endif

U_NAMESPACE_USE

//----------------------------------------------------------------------
// BEGIN DATA
// 
// This is the raw data to be output.  We define the data structure,
// then include a machine-generated header that contains the actual
// data.

#include "unicode/uchar.h"
#include "unicode/unorm2.h"
#include "unicode/uscript.h"

// Dilemma: We want to use MAX_ALIASES to define fields in the Value class.
// However, we need to define the class before including the data header
// and we can use MAX_ALIASES only after including it.
// So we define a second constant and at runtime check that it's >=MAX_ALIASES.
static const int32_t VALUE_MAX_ALIASES=4;

static const int32_t JOINED_ALIASES_CAPACITY=100;

class Value {
public:
    Value(int32_t enumValue, const char *joinedAliases)
            : enumValue(enumValue), joinedAliases(joinedAliases), count(0) {
        if(uprv_strlen(joinedAliases)>=JOINED_ALIASES_CAPACITY) {
            fprintf(stderr,
                    "genprops error: pnamesbuilder.cpp Value::Value(%ld, \"%s\"): "
                    "joined aliases too long: "
                    "increase JOINED_ALIASES_CAPACITY, to at least %ld\n",
                    (long)enumValue, joinedAliases, uprv_strlen(joinedAliases)+1);
            exit(U_BUFFER_OVERFLOW_ERROR);
        }
        // Copy the space-separated aliases into NUL-separated ones and count them.
        // Write a normalized version of each one.
        const char *j=joinedAliases;
        char *a=aliasesBuffer;
        char *n=normalizedBuffer;
        char c;
        do {
            aliases[count]=a;
            normalized[count++]=n;
            while((c=*j++)!=' ' && c!=0) {
                *a++=c;
                // Ignore delimiters '-' and '_'.
                if(!(c=='-' || c=='_')) {
                    *n++=uprv_tolower(c);
                }
            }
            *a++=0;
            *n++=0;
        } while(c!=0);
    }

    /**
     * Writes at most MAX_ALIASES pointers for unique normalized aliases
     * (no empty strings) to dest and returns how many there are.
     */
    int32_t getUniqueNormalizedAliases(const char *dest[]) const {
        int32_t numUnique=0;
        for(int32_t i=0; i<count; ++i) {
            const char *s=normalized[i];
            if(*s!=0) {  // Omit empty strings.
                for(int32_t j=0;; ++j) {
                    if(j==numUnique) {
                        // s is a new unique alias.
                        dest[numUnique++]=s;
                        break;
                    }
                    if(0==uprv_strcmp(s, dest[j])) {
                        // s is equal or equivalent to an earlier alias.
                        break;
                    }
                }
            }
        }
        return numUnique;
    }

    int32_t enumValue;
    const char *joinedAliases;
    char aliasesBuffer[JOINED_ALIASES_CAPACITY];
    char normalizedBuffer[JOINED_ALIASES_CAPACITY];
    const char *aliases[VALUE_MAX_ALIASES];
    const char *normalized[VALUE_MAX_ALIASES];
    int32_t count;
};

class Property : public Value {
public:
    // A property with a values array.
    Property(int32_t enumValue, const char *joinedAliases,
             const Value *values, int32_t valueCount)
            : Value(enumValue, joinedAliases),
              values(values), valueCount(valueCount) {}
    // A binary property (enumValue<UCHAR_BINARY_LIMIT), or one without values.
    Property(int32_t enumValue, const char *joinedAliases);

    const Value *values;
    int32_t valueCount;
};

// *** Include the data header ***
#include "pnames_data.h"

Property::Property(int32_t enumValue, const char *joinedAliases)
        : Value(enumValue, joinedAliases),
          values(enumValue<UCHAR_BINARY_LIMIT ? VALUES_binprop : NULL),
          valueCount(enumValue<UCHAR_BINARY_LIMIT ? 2 : 0) {}

// END DATA
//----------------------------------------------------------------------

class PNamesPropertyNames : public PropertyNames {
public:
    PNamesPropertyNames()
            : valueMaps(NULL), bytesTries(NULL) {}
    void init(const int32_t *vm, const uint8_t *bt) {
        valueMaps=vm;
        bytesTries=bt;
    }
    virtual int32_t getPropertyEnum(const char *name) const;
    virtual int32_t getPropertyValueEnum(int32_t property, const char *name) const;
private:
    int32_t findProperty(int32_t property) const;
    UBool containsName(BytesTrie &trie, const char *name) const;
    int32_t getPropertyOrValueEnum(int32_t bytesTrieOffset, const char *alias) const;

    const int32_t *valueMaps;
    const uint8_t *bytesTries;
};

class PNamesBuilderImpl : public PNamesBuilder {
public:
    PNamesBuilderImpl(UErrorCode &errorCode)
            : valueMaps(errorCode), btb(errorCode), maxNameLength(0),
              nameGroupToOffset(NULL) {}

    ~PNamesBuilderImpl() {
        uhash_close(nameGroupToOffset);
    }

    virtual void build(UErrorCode &errorCode) {
        if(U_FAILURE(errorCode)) { return; }
        if(VALUE_MAX_ALIASES<MAX_ALIASES) {
            fprintf(stderr,
                    "genprops error: pnamesbuilder.cpp VALUE_MAX_ALIASES=%d<%d=MAX_ALIASES -- "
                    "need to change VALUE_MAX_ALIASES to at least %d\n",
                    (int)VALUE_MAX_ALIASES, (int)MAX_ALIASES, (int)MAX_ALIASES);
            errorCode=U_INTERNAL_PROGRAM_ERROR;
            return;
        }
        nameGroupToOffset=uhash_open(uhash_hashChars, uhash_compareChars, NULL, &errorCode);
        // Build main property aliases value map at value map offset 0,
        // so that we need not store another offset for it.
        UVector32 propEnums(errorCode);
        int32_t propIndex;
        for(propIndex=0; propIndex<LENGTHOF(PROPERTIES); ++propIndex) {
            propEnums.sortedInsert(PROPERTIES[propIndex].enumValue, errorCode);
        }
        int32_t ranges[10][2];
        int32_t numPropRanges=uprv_makeDenseRanges(propEnums.getBuffer(),
                                                   LENGTHOF(PROPERTIES), 0x100,
                                                   ranges, LENGTHOF(ranges));
        valueMaps.addElement(numPropRanges, errorCode);
        int32_t i, j;
        for(i=0; i<numPropRanges; ++i) {
            valueMaps.addElement(ranges[i][0], errorCode);
            valueMaps.addElement(ranges[i][1]+1, errorCode);
            for(j=ranges[i][0]; j<=ranges[i][1]; ++j) {
                // Reserve two slots per property for the name group offset and the value-map offset.
                valueMaps.addElement(0, errorCode);
                valueMaps.addElement(0, errorCode);
            }
        }

        // Build the properties trie first, at BytesTrie offset 0,
        // so that we need not store another offset for it.
        buildPropertiesBytesTrie(PROPERTIES, LENGTHOF(PROPERTIES), errorCode);

        // Build the name group for the first property, at nameGroups offset 0.
        // Name groups for *value* aliases must not start at offset 0
        // because that is a missing-value marker for sparse value ranges.
        setPropertyInt(PROPERTIES[0].enumValue, 0,
                       writeValueAliases(PROPERTIES[0], errorCode));

        // Build the known-repeated binary properties once.
        int32_t binPropsValueMapOffset=valueMaps.size();
        int32_t bytesTrieOffset=buildValuesBytesTrie(VALUES_binprop, 2, errorCode);
        valueMaps.addElement(bytesTrieOffset, errorCode);
        buildValueMap(VALUES_binprop, 2, errorCode);

        // Note: It is slightly wasteful to store binary properties like all others.
        // Since we know that they are in the lowest range of property enum values
        // and share the same name group and BytesTrie,
        // we could just store those two indexes once.
        // (This would save 8 bytes per binary property, or about half a kilobyte.)

        // Build the known-repeated canonical combining class properties once.
        int32_t cccValueMapOffset=valueMaps.size();
        bytesTrieOffset=buildValuesBytesTrie(VALUES_ccc, LENGTHOF(VALUES_ccc), errorCode);
        valueMaps.addElement(bytesTrieOffset, errorCode);
        buildValueMap(VALUES_ccc, LENGTHOF(VALUES_ccc), errorCode);

        // Build the rest of the data.
        for(propIndex=0; propIndex<LENGTHOF(PROPERTIES); ++propIndex) {
            if(propIndex>0) {
                // writeValueAliases(PROPERTIES[0], ...) already done
                setPropertyInt(PROPERTIES[propIndex].enumValue, 0,
                               writeValueAliases(PROPERTIES[propIndex], errorCode));
            }
            int32_t valueCount=PROPERTIES[propIndex].valueCount;
            if(valueCount>0) {
                int32_t valueMapOffset;
                const Value *values=PROPERTIES[propIndex].values;
                if(values==VALUES_binprop) {
                    valueMapOffset=binPropsValueMapOffset;
                } else if(values==VALUES_ccc || values==VALUES_lccc || values==VALUES_tccc) {
                    valueMapOffset=cccValueMapOffset;
                } else {
                    valueMapOffset=valueMaps.size();
                    bytesTrieOffset=buildValuesBytesTrie(values, valueCount, errorCode);
                    valueMaps.addElement(bytesTrieOffset, errorCode);
                    buildValueMap(values, valueCount, errorCode);
                }
                setPropertyInt(PROPERTIES[propIndex].enumValue, 1, valueMapOffset);
            }
        }

        // Write the indexes.
        int32_t offset=(int32_t)sizeof(indexes);
        indexes[PropNameData::IX_VALUE_MAPS_OFFSET]=offset;
        offset+=valueMaps.size()*4;
        indexes[PropNameData::IX_BYTE_TRIES_OFFSET]=offset;
        offset+=bytesTries.length();
        indexes[PropNameData::IX_NAME_GROUPS_OFFSET]=offset;
        offset+=nameGroups.length();
        for(i=PropNameData::IX_RESERVED3_OFFSET; i<=PropNameData::IX_TOTAL_SIZE; ++i) {
            indexes[i]=offset;
        }
        indexes[PropNameData::IX_MAX_NAME_LENGTH]=maxNameLength;
        for(i=PropNameData::IX_RESERVED7; i<PropNameData::IX_COUNT; ++i) {
            indexes[i]=0;
        }

        if(!beQuiet) {
            puts("* pnames.icu stats *");
            printf("length of all value maps:  %6ld\n", (long)valueMaps.size());
            printf("length of all BytesTries:  %6ld\n", (long)bytesTries.length());
            printf("length of all name groups: %6ld\n", (long)nameGroups.length());
            printf("length of pnames.icu data: %6ld\n", (long)indexes[PropNameData::IX_TOTAL_SIZE]);
        }
    }

    virtual void writeCSourceFile(const char *path, UErrorCode &errorCode);
    virtual void writeBinaryData(const char *path, UBool withCopyright, UErrorCode &errorCode);

    int32_t writeValueAliases(const Value &value, UErrorCode &errorCode) {
        int32_t nameOffset=uhash_geti(nameGroupToOffset, (void *)value.joinedAliases);
        if(nameOffset!=0) {
            // The same list of aliases has been written already.
            return nameOffset-1;  // Was incremented to reserve 0 for "not found".
        }
        // Write this not-yet-seen list of aliases.
        nameOffset=nameGroups.length();
        uhash_puti(nameGroupToOffset, (void *)value.joinedAliases,
                   nameOffset+1, &errorCode);
        // The first byte tells us how many aliases there are.
        // We use only values 0..0x1f in the first byte because when we write
        // the name groups as an invariant-character string into a source file,
        // those values (C0 control codes) are written as numbers rather than as characters.
        int32_t count=value.count;
        if(count>=0x20) {
            fprintf(stderr, "Error: Too many aliases in \"%s\"\n", value.joinedAliases);
            exit(U_INDEX_OUTOFBOUNDS_ERROR);
        }
        nameGroups.append((char)count, errorCode);
        // There is at least a short name (sometimes empty) and a long name. (count>=2)
        // Note: Sometimes the short and long names are the same.
        // In such a case, we could set a flag and omit the duplicate,
        // but that would save only about 1.35% of total data size (Unicode 6.0/ICU 4.6)
        // which is not worth the trouble.
        // Note: In Unicode 6.1, there are more duplicates due to newly added
        // short names for blocks and other properties.
        // It might now be worth changing the data structure.
        for(int32_t i=0; i<count; ++i) {
            const char *s=value.aliases[i];
            int32_t sLength=uprv_strlen(s)+1;
            if(sLength>maxNameLength) {
                maxNameLength=sLength;
            }
            nameGroups.append(s, sLength, errorCode);  // including NUL
        }
        return nameOffset;
    }

    void buildValueMap(const Value values[], int32_t length, UErrorCode &errorCode) {
        UVector32 sortedValues(errorCode);
        UVector32 nameOffsets(errorCode);  // Parallel to values[].
        int32_t i;
        for(i=0; i<length; ++i) {
            sortedValues.sortedInsert(values[i].enumValue, errorCode);
            nameOffsets.addElement(writeValueAliases(values[i], errorCode), errorCode);
        }
        int32_t ranges[10][2];
        int32_t numRanges=uprv_makeDenseRanges(sortedValues.getBuffer(), length, 0xe0,
                                               ranges, LENGTHOF(ranges));
        if(numRanges>0) {
            valueMaps.addElement(numRanges, errorCode);
            for(i=0; i<numRanges; ++i) {
                valueMaps.addElement(ranges[i][0], errorCode);
                valueMaps.addElement(ranges[i][1]+1, errorCode);
                for(int32_t j=ranges[i][0]; j<=ranges[i][1]; ++j) {
                    // The range might not be completely dense, so j might not have an entry,
                    // in which case we write a nameOffset of 0.
                    // Real nameOffsets for property values are never 0.
                    // (The first name group is for the first property name.)
                    int32_t valueIndex=valuesIndexOf(values, length, j);
                    int32_t nameOffset= valueIndex>=0 ? nameOffsets.elementAti(valueIndex) : 0;
                    valueMaps.addElement(nameOffset, errorCode);
                }
            }
        } else {
            // No dense ranges.
            valueMaps.addElement(0x10+length, errorCode);
            for(i=0; i<length; ++i) {
                valueMaps.addElement(sortedValues.elementAti(i), errorCode);
            }
            for(i=0; i<length; ++i) {
                valueMaps.addElement(
                    nameOffsets.elementAti(
                        valuesIndexOf(values, length,
                                       sortedValues.elementAti(i))), errorCode);
            }
        }
    }

    static int32_t valuesIndexOf(const Value values[], int32_t length, int32_t value) {
        for(int32_t i=0;; ++i) {
            if(values[i].enumValue==value) {
                return i;
            }
        }
        return -1;
    }

    void setPropertyInt(int32_t prop, int32_t subIndex, int32_t value) {
        // Assume that prop is in the valueMaps.elementAti(0) ranges.
        int32_t index=1;
        for(;;) {
            int32_t rangeStart=valueMaps.elementAti(index);
            int32_t rangeLimit=valueMaps.elementAti(index+1);
            index+=2;
            if(rangeStart<=prop && prop<rangeLimit) {
                valueMaps.setElementAt(value, index+2*(prop-rangeStart)+subIndex);
                break;
            }
            index+=2*(rangeLimit-rangeStart);
        }
    }

    void addValueToBytesTrie(const Value &value, UErrorCode &errorCode) {
        const char *aliases[MAX_ALIASES];
        int32_t numAliases=value.getUniqueNormalizedAliases(aliases);
        for(int32_t i=0; i<numAliases; ++i) {
            btb.add(aliases[i], value.enumValue, errorCode);
        }
    }

    int32_t buildValuesBytesTrie(const Value values[], int32_t length, UErrorCode &errorCode) {
        btb.clear();
        for(int32_t i=0; i<length; ++i) {
            addValueToBytesTrie(values[i], errorCode);
        }
        int32_t bytesTrieOffset=bytesTries.length();
        bytesTries.append(btb.buildStringPiece(USTRINGTRIE_BUILD_SMALL, errorCode), errorCode);
        return bytesTrieOffset;
    }

    // Variant of buildValuesBytesTrie() for Property.
    // Property is-a Value, and the source code is the same,
    // but when we iterate through the array we need to increment by the right object size.
    int32_t buildPropertiesBytesTrie(const Property properties[], int32_t length,
                                     UErrorCode &errorCode) {
        btb.clear();
        for(int32_t i=0; i<length; ++i) {
            addValueToBytesTrie(properties[i], errorCode);
        }
        int32_t bytesTrieOffset=bytesTries.length();
        bytesTries.append(btb.buildStringPiece(USTRINGTRIE_BUILD_SMALL, errorCode), errorCode);
        return bytesTrieOffset;
    }

    virtual const PropertyNames *getPropertyNames() {
        pnames.init(valueMaps.getBuffer(),
                    reinterpret_cast<const uint8_t *>(bytesTries.data()));
        return &pnames;
    }

private:
    int32_t indexes[PropNameData::IX_COUNT];
    UVector32 valueMaps;
    BytesTrieBuilder btb;
    CharString bytesTries;
    CharString nameGroups;
    int32_t maxNameLength;
    PNamesPropertyNames pnames;
    UHashtable *nameGroupToOffset;
};

/* UDataInfo cf. udata.h */
static const UDataInfo dataInfo = {
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    sizeof(UChar),
    0,

    { PNAME_SIG_0, PNAME_SIG_1, PNAME_SIG_2, PNAME_SIG_3 },
    { 2, 0, 0, 0 },                 /* formatVersion */
    UNICODE_VERSION
};

void
PNamesBuilderImpl::writeBinaryData(const char *path, UBool withCopyright, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }
    UNewDataMemory *pdata=udata_create(path, PNAME_DATA_TYPE, PNAME_DATA_NAME, &dataInfo,
                                       withCopyright ? U_COPYRIGHT_STRING : 0, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops: udata_create(%s, pnames.icu) failed - %s\n",
                path, u_errorName(errorCode));
        return;
    }

    udata_writeBlock(pdata, indexes, PropNameData::IX_COUNT*4);
    udata_writeBlock(pdata, valueMaps.getBuffer(), valueMaps.size()*4);
    udata_writeBlock(pdata, bytesTries.data(), bytesTries.length());
    udata_writeBlock(pdata, nameGroups.data(), nameGroups.length());

    int32_t dataLength=(int32_t)udata_finish(pdata, &errorCode);
    if(dataLength!=indexes[PropNameData::IX_TOTAL_SIZE]) {
        fprintf(stderr,
                "udata_finish(pnames.icu) reports %ld bytes written but should be %ld\n",
                (long)dataLength, (long)indexes[PropNameData::IX_TOTAL_SIZE]);
        errorCode=U_INTERNAL_PROGRAM_ERROR;
    }
}

void
PNamesBuilderImpl::writeCSourceFile(const char *path, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }
    FILE *f=usrc_create(path, "propname_data.h", 2016,
                        "icu/tools/unicode/c/genprops/pnamesbuilder.cpp");
    if(f==NULL) {
        errorCode=U_FILE_ACCESS_ERROR;
        return;
    }

    fputs("#ifdef INCLUDED_FROM_PROPNAME_CPP\n\n"
          "U_NAMESPACE_BEGIN\n\n", f);

    usrc_writeArray(f, "const int32_t PropNameData::indexes[%ld]={",
                    indexes, 32, PropNameData::IX_COUNT,
                    "};\n\n");
    usrc_writeArray(f, "const int32_t PropNameData::valueMaps[%ld]={\n",
                    valueMaps.getBuffer(), 32, valueMaps.size(),
                    "\n};\n\n");
    usrc_writeArray(f, "const uint8_t PropNameData::bytesTries[%ld]={\n",
                    bytesTries.data(), 8, bytesTries.length(),
                    "\n};\n\n");
    usrc_writeArrayOfMostlyInvChars(
        f, "const char PropNameData::nameGroups[%ld]={\n",
        nameGroups.data(), nameGroups.length(),
        "\n};\n\n");

    fputs("U_NAMESPACE_END\n\n"
          "#endif  // INCLUDED_FROM_PROPNAME_CPP\n", f);

    fclose(f);
}

PNamesBuilder *
createPNamesBuilder(UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return NULL; }
    PNamesBuilder *pb=new PNamesBuilderImpl(errorCode);
    if(pb==NULL) {
        errorCode=U_MEMORY_ALLOCATION_ERROR;
    }
    return pb;
}

// Note: The following is a partial copy of runtime propname.cpp code.
// Consider changing that into a semi-public API to avoid duplication.

int32_t PNamesPropertyNames::findProperty(int32_t property) const {
    int32_t i=1;  // valueMaps index, initially after numRanges
    for(int32_t numRanges=valueMaps[0]; numRanges>0; --numRanges) {
        // Read and skip the start and limit of this range.
        int32_t start=valueMaps[i];
        int32_t limit=valueMaps[i+1];
        i+=2;
        if(property<start) {
            break;
        }
        if(property<limit) {
            return i+(property-start)*2;
        }
        i+=(limit-start)*2;  // Skip all entries for this range.
    }
    return 0;
}

UBool PNamesPropertyNames::containsName(BytesTrie &trie, const char *name) const {
    if(name==NULL) {
        return FALSE;
    }
    UStringTrieResult result=USTRINGTRIE_NO_VALUE;
    char c;
    while((c=*name++)!=0) {
        c=uprv_invCharToLowercaseAscii(c);
        // Ignore delimiters '-', '_', and ASCII White_Space.
        if(c==0x2d || c==0x5f || c==0x20 || (0x09<=c && c<=0x0d)) {
            continue;
        }
        if(!USTRINGTRIE_HAS_NEXT(result)) {
            return FALSE;
        }
        result=trie.next((uint8_t)c);
    }
    return USTRINGTRIE_HAS_VALUE(result);
}

int32_t PNamesPropertyNames::getPropertyOrValueEnum(int32_t bytesTrieOffset, const char *alias) const {
    BytesTrie trie(bytesTries+bytesTrieOffset);
    if(containsName(trie, alias)) {
        return trie.getValue();
    } else {
        return UCHAR_INVALID_CODE;
    }
}

int32_t
PNamesPropertyNames::getPropertyEnum(const char *alias) const {
    return getPropertyOrValueEnum(0, alias);
}

int32_t
PNamesPropertyNames::getPropertyValueEnum(int32_t property, const char *alias) const {
    int32_t valueMapIndex=findProperty(property);
    if(valueMapIndex==0) {
        return UCHAR_INVALID_CODE;  // Not a known property.
    }
    valueMapIndex=valueMaps[valueMapIndex+1];
    if(valueMapIndex==0) {
        return UCHAR_INVALID_CODE;  // The property does not have named values.
    }
    // valueMapIndex is the start of the property's valueMap,
    // where the first word is the BytesTrie offset.
    return getPropertyOrValueEnum(valueMaps[valueMapIndex], alias);
}
