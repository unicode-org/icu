/*
**********************************************************************
*   Copyright (C) 2002-2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   10/11/02    aliu        Creation.
*   2010nov19   Markus Scherer  Rewrite for formatVersion 2.
**********************************************************************
*/

#include "unicode/utypes.h"
#include "unicode/bytestriebuilder.h"
#include "unicode/putil.h"
#include "unicode/uclean.h"
#include "cmemory.h"
#include "charstr.h"
#include "cstring.h"
#include "denseranges.h"
#include "unewdata.h"
#include "uoptions.h"
#include "propname.h"
#include "toolutil.h"
#include "uvectr32.h"
#include "writesrc.h"

#include <stdio.h>

// We test for ASCII delimiters and White_Space, and build ASCII string BytesTries.
#if U_CHARSET_FAMILY!=U_ASCII_FAMILY
#   error This builder requires U_CHARSET_FAMILY==U_ASCII_FAMILY.
#endif

#define LENGTHOF(array) (int32_t)(sizeof(array)/sizeof((array)[0]))

U_NAMESPACE_USE

//----------------------------------------------------------------------
// BEGIN DATA
// 
// This is the raw data to be output.  We define the data structure,
// then include a machine-generated header that contains the actual
// data.

#include "unicode/uchar.h"
#include "unicode/uscript.h"
#include "unicode/unorm.h"
#include "unicode/unorm2.h"

class AliasName {
public:
    const char* str;
    int32_t     index;
    char        normalized[64];

    AliasName(const char* str, int32_t index);

    int compare(const AliasName& other) const {
        return uprv_strcmp(normalized, other.normalized);
    }

    UBool operator==(const AliasName& other) const {
        return compare(other) == 0;
    }

    UBool operator!=(const AliasName& other) const {
        return compare(other) != 0;
    }
};

AliasName::AliasName(const char* _str,
                     int32_t _index) :
    str(_str),
    index(_index)
{
    // Build the normalized form of the alias.
    const char *s=str;
    char c;
    int32_t i=0;
    while((c=*s++)!=0) {
        // Ignore delimiters '-', '_', and ASCII White_Space.
        if(c==0x2d || c==0x5f || c==0x20 || (0x09<=c && c<=0x0d)) {
            continue;
        }
        normalized[i++]=uprv_tolower(c);
    }
    normalized[i]=0;
    if(i>=LENGTHOF(normalized)) {
        fprintf(stderr,
                "Error: Property (value) alias '%s' results in "
                "too-long normalized string (length %d)\n",
                str, (int)i);
        exit(U_BUFFER_OVERFLOW_ERROR);
    }
}

class Alias {
public:
    int32_t     enumValue;
    int32_t     nameGroupIndex;

    Alias(int32_t enumValue, int32_t nameGroupIndex);

    int32_t getUniqueNames(int32_t* nameGroupIndices) const;
};

Alias::Alias(int32_t anEnumValue, int32_t aNameGroupIndex) :
    enumValue(anEnumValue),
    nameGroupIndex(aNameGroupIndex)
{
}

class Property : public Alias {
public:
    int32_t         valueCount;
    const Alias* valueList;

    Property(int32_t enumValue,
             int32_t nameGroupIndex,
             int32_t valueCount,
             const Alias* valueList);
};

Property::Property(int32_t _enumValue,
                   int32_t _nameGroupIndex,
                   int32_t _valueCount,
                   const Alias* _valueList) :
    Alias(_enumValue, _nameGroupIndex),
    valueCount(_valueCount),
    valueList(_valueList)
{
}

// *** Include the data header ***
#include "data.h"

/* return a list of unique names, not including "", for this property
 * @param stringIndices array of at least MAX_NAMES_PER_GROUP
 * elements, will be filled with indices into STRING_TABLE
 * @return number of indices, >= 1
 */
int32_t Alias::getUniqueNames(int32_t* stringIndices) const {
    int32_t count = 0;
    int32_t i = nameGroupIndex;
    UBool done = FALSE;
    while (!done) {
        int32_t j = NAME_GROUP[i++];
        if (j < 0) {
            done = TRUE;
            j = -j;
        }
        if (j == 0) continue; // omit "" entries
        UBool dupe = FALSE;
        for (int32_t k=0; k<count; ++k) {
            if (stringIndices[k] == j) {
                dupe = TRUE;
                break;
            }
            // also do a string check for things like "age|Age"
            if (STRING_TABLE[stringIndices[k]] == STRING_TABLE[j]) {
                //printf("Found dupe %s|%s\n",
                //       STRING_TABLE[stringIndices[k]].str,
                //       STRING_TABLE[j].str);
                dupe = TRUE;
                break;
            }
        }
        if (dupe) continue; // omit duplicates
        stringIndices[count++] = j;
    }
    return count;
}

// END DATA
//----------------------------------------------------------------------

class Builder {
public:
    Builder(UErrorCode &errorCode) : valueMaps(errorCode), btb(errorCode), maxNameLength(0) {}

    void build() {
        IcuToolErrorCode errorCode("genpname Builder::build()");

        // Build main property aliases value map at value map offset 0,
        // so that we need not store another offset for it.
        UVector32 propEnums(errorCode);
        int32_t propIndex;
        for(propIndex=0; propIndex<PROPERTY_COUNT; ++propIndex) {
            propEnums.sortedInsert(PROPERTY[propIndex].enumValue, errorCode);
        }
        int32_t ranges[10][2];
        int32_t numPropRanges=uprv_makeDenseRanges(propEnums.getBuffer(), PROPERTY_COUNT, 0x100,
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
        buildAliasesBytesTrie(PROPERTY, PROPERTY_COUNT, errorCode);

        // Build the name group for the first property, at nameGroups offset 0.
        // Name groups for *value* aliases must not start at offset 0
        // because that is a missing-value marker for sparse value ranges.
        setPropertyInt(PROPERTY[0].enumValue, 0,
                       writeNameGroup(PROPERTY[0], errorCode));

        // Build the known-repeated binary properties once.
        int32_t binPropsValueMapOffset=valueMaps.size();
        int32_t bytesTrieOffset=buildAliasesBytesTrie(VALUES_binprop, VALUES_binprop_COUNT, errorCode);
        valueMaps.addElement(bytesTrieOffset, errorCode);
        buildValueMap(VALUES_binprop, VALUES_binprop_COUNT, errorCode);

        // Build the known-repeated canonical combining class properties once.
        int32_t cccValueMapOffset=valueMaps.size();
        bytesTrieOffset=buildAliasesBytesTrie(VALUES_ccc, VALUES_ccc_COUNT, errorCode);
        valueMaps.addElement(bytesTrieOffset, errorCode);
        buildValueMap(VALUES_ccc, VALUES_ccc_COUNT, errorCode);

        // Build the rest of the data.
        for(propIndex=0; propIndex<PROPERTY_COUNT; ++propIndex) {
            if(propIndex>0) {
                // writeNameGroup(PROPERTY[0], ...) already done
                setPropertyInt(PROPERTY[propIndex].enumValue, 0,
                               writeNameGroup(PROPERTY[propIndex], errorCode));
            }
            int32_t valueCount=PROPERTY[propIndex].valueCount;
            if(valueCount>0) {
                int32_t valueMapOffset;
                const Alias *valueList=PROPERTY[propIndex].valueList;
                if(valueList==VALUES_binprop) {
                    valueMapOffset=binPropsValueMapOffset;
                } else if(valueList==VALUES_ccc || valueList==VALUES_lccc || valueList==VALUES_tccc) {
                    valueMapOffset=cccValueMapOffset;
                } else {
                    valueMapOffset=valueMaps.size();
                    bytesTrieOffset=buildAliasesBytesTrie(valueList, valueCount, errorCode);
                    valueMaps.addElement(bytesTrieOffset, errorCode);
                    buildValueMap(valueList, valueCount, errorCode);
                }
                setPropertyInt(PROPERTY[propIndex].enumValue, 1, valueMapOffset);
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
    }

    int32_t writeNameGroup(const Alias &alias, UErrorCode &errorCode) {
        int32_t nameOffset=nameGroups.length();
        // Count how many aliases this group has.
        int32_t i=alias.nameGroupIndex;
        int32_t nameIndex;
        do { nameIndex=NAME_GROUP[i++]; } while(nameIndex>=0);
        int32_t count=i-alias.nameGroupIndex;
        // The first byte tells us how many aliases there are.
        // We use only values 0..0x1f in the first byte because when we write
        // the name groups as an invariant-character string into a source file,
        // those values (C0 control codes) are written as numbers rather than as characters.
        if(count>=0x20) {
            fprintf(stderr, "Error: Too many aliases in the group with index %d\n",
                    (int)alias.nameGroupIndex);
            exit(U_INDEX_OUTOFBOUNDS_ERROR);
        }
        nameGroups.append((char)count, errorCode);
        // There is at least a short name (sometimes empty) and a long name. (count>=2)
        // Note: Sometimes the short and long names are the same.
        // In such a case, we could set a flag and omit the duplicate,
        // but that would save only about 1.35% of total data size (Unicode 6.0/ICU 4.6)
        // which is not worth the trouble.
        i=alias.nameGroupIndex;
        int32_t n;
        do {
            nameIndex=n=NAME_GROUP[i++];
            if(nameIndex<0) {
                nameIndex=-nameIndex;
            }
            const char *s=STRING_TABLE[nameIndex].str;
            int32_t sLength=uprv_strlen(s)+1;
            if(sLength>maxNameLength) {
                maxNameLength=sLength;
            }
            nameGroups.append(s, sLength, errorCode);  // including NUL
        } while(n>=0);
        return nameOffset;
    }

    void buildValueMap(const Alias aliases[], int32_t length, UErrorCode &errorCode) {
        UVector32 sortedValues(errorCode);
        UVector32 nameOffsets(errorCode);  // Parallel to aliases[].
        int32_t i;
        for(i=0; i<length; ++i) {
            sortedValues.sortedInsert(aliases[i].enumValue, errorCode);
            nameOffsets.addElement(writeNameGroup(aliases[i], errorCode), errorCode);
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
                    int32_t aliasIndex=aliasesIndexOf(aliases, length, j);
                    int32_t nameOffset= aliasIndex>=0 ? nameOffsets.elementAti(aliasIndex) : 0;
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
                        aliasesIndexOf(aliases, length,
                                       sortedValues.elementAti(i))), errorCode);
            }
        }
    }

    static int32_t aliasesIndexOf(const Alias aliases[], int32_t length, int32_t value) {
        for(int32_t i=0;; ++i) {
            if(aliases[i].enumValue==value) {
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

    void addAliasToBytesTrie(const Alias &alias, UErrorCode &errorCode) {
        int32_t names[MAX_NAMES_PER_GROUP];
        int32_t numNames=alias.getUniqueNames(names);
        for(int32_t i=0; i<numNames; ++i) {
            // printf("* adding %s: 0x%lx\n", STRING_TABLE[names[i]].normalized, (long)alias.enumValue);
            btb.add(STRING_TABLE[names[i]].normalized, alias.enumValue, errorCode);
        }
    }

    int32_t buildAliasesBytesTrie(const Alias aliases[], int32_t length, UErrorCode &errorCode) {
        btb.clear();
        for(int32_t i=0; i<length; ++i) {
            addAliasToBytesTrie(aliases[i], errorCode);
        }
        int32_t bytesTrieOffset=bytesTries.length();
        bytesTries.append(btb.buildStringPiece(USTRINGTRIE_BUILD_SMALL, errorCode), errorCode);
        return bytesTrieOffset;
    }

    // Overload for Property. Property is-an Alias, but when we iterate through
    // the array we need to increment by the right object size.
    int32_t buildAliasesBytesTrie(const Property aliases[], int32_t length,
                                  UErrorCode &errorCode) {
        btb.clear();
        for(int32_t i=0; i<length; ++i) {
            addAliasToBytesTrie(aliases[i], errorCode);
        }
        int32_t bytesTrieOffset=bytesTries.length();
        bytesTries.append(btb.buildStringPiece(USTRINGTRIE_BUILD_SMALL, errorCode), errorCode);
        return bytesTrieOffset;
    }

    int32_t indexes[PropNameData::IX_COUNT];
    UVector32 valueMaps;
    BytesTrieBuilder btb;
    CharString bytesTries;
    CharString nameGroups;
    int32_t maxNameLength;
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
    { VERSION_0, VERSION_1, VERSION_2, VERSION_3 } /* Unicode version */
};

static void writeDataFile(const char *destdir, const Builder& builder, UBool useCopyright) {
    IcuToolErrorCode errorCode("genpname writeDataFile()");
    UNewDataMemory *pdata=udata_create(destdir, PNAME_DATA_TYPE, PNAME_DATA_NAME, &dataInfo,
                                       useCopyright ? U_COPYRIGHT_STRING : 0, errorCode);
    errorCode.assertSuccess();

    udata_writeBlock(pdata, builder.indexes, PropNameData::IX_COUNT*4);
    udata_writeBlock(pdata, builder.valueMaps.getBuffer(), builder.valueMaps.size()*4);
    udata_writeBlock(pdata, builder.bytesTries.data(), builder.bytesTries.length());
    udata_writeBlock(pdata, builder.nameGroups.data(), builder.nameGroups.length());

    int32_t dataLength=(int32_t)udata_finish(pdata, errorCode);
    if(dataLength!=builder.indexes[PropNameData::IX_TOTAL_SIZE]) {
        fprintf(stderr,
                "udata_finish(pnames.icu) reports %ld bytes written but should be %ld\n",
                (long)dataLength, (long)builder.indexes[PropNameData::IX_TOTAL_SIZE]);
        exit(U_INTERNAL_PROGRAM_ERROR);
    }
}

static void writeCSourceFile(const char *destdir, const Builder& builder) {
    FILE *f=usrc_create(destdir, "propname_data.h");
    if(f==NULL) {
        return;  // usrc_create() reported an error.
    }

    fputs("#ifndef INCLUDED_FROM_PROPNAME_CPP\n"
          "#   error This file must be #included from propname.cpp only.\n"
          "#endif\n\n", f);

    fputs("U_NAMESPACE_BEGIN\n\n", f);

    usrc_writeArray(f, "const int32_t PropNameData::indexes[%ld]={",
                    builder.indexes, 32, PropNameData::IX_COUNT,
                    "};\n\n");
    usrc_writeArray(f, "const int32_t PropNameData::valueMaps[%ld]={\n",
                    builder.valueMaps.getBuffer(), 32, builder.valueMaps.size(),
                    "\n};\n\n");
    usrc_writeArray(f, "const uint8_t PropNameData::bytesTries[%ld]={\n",
                    builder.bytesTries.data(), 8, builder.bytesTries.length(),
                    "\n};\n\n");
    usrc_writeArrayOfMostlyInvChars(
        f, "const char PropNameData::nameGroups[%ld]={\n",
        builder.nameGroups.data(), builder.nameGroups.length(),
        "\n};\n\n");

    fputs("U_NAMESPACE_END\n", f);

    fclose(f);
}

enum {
    HELP_H,
    HELP_QUESTION_MARK,
    VERBOSE,
    COPYRIGHT,
    DESTDIR,
    CSOURCE
};

/* Keep these values in sync with the above enums */
static UOption options[]={
    UOPTION_HELP_H,
    UOPTION_HELP_QUESTION_MARK,
    UOPTION_VERBOSE,
    UOPTION_COPYRIGHT,
    UOPTION_DESTDIR,
    UOPTION_DEF("csource", 'C', UOPT_NO_ARG)
};

extern int main(int argc, char *argv[]) {
    U_MAIN_INIT_ARGS(argc, argv);

    /* preset then read command line options */
    options[DESTDIR].value=u_getDataDirectory();
    argc=u_parseArgs(argc, argv, LENGTHOF(options), options);

    /* error handling, printing usage message */
    if(argc<0) {
        fprintf(stderr, "error in command line argument \"%s\"\n", argv[-argc]);
    }
    if(argc!=1 || options[HELP_H].doesOccur || options[HELP_QUESTION_MARK].doesOccur) {
        fprintf(stderr,
            "Usage: %s [-options]\n"
            "\tCreates " PNAME_DATA_NAME "." PNAME_DATA_TYPE "\n"
            "\n",
            argv[0]);
        fprintf(stderr,
            "Options:\n"
            "\t-h or -? or --help  this usage text\n"
            "\t-v or --verbose     turn on verbose output\n"
            "\t-c or --copyright   include a copyright notice\n"
            "\t-d or --destdir     destination directory, followed by the path\n"
            "\t-C or --csource     generate a .h source file rather than the .icu binary\n");
        return argc!=1 ? U_ILLEGAL_ARGUMENT_ERROR : U_ZERO_ERROR;
    }

    IcuToolErrorCode errorCode("genpname main() Builder()");
    Builder builder(errorCode);
    errorCode.assertSuccess();
    builder.build();
    if(options[VERBOSE].doesOccur) {
        printf("length of all value maps:  %6ld\n", (long)builder.valueMaps.size());
        printf("length of all BytesTries:  %6ld\n", (long)builder.bytesTries.length());
        printf("length of all name groups: %6ld\n", (long)builder.nameGroups.length());
        printf("length of pnames.icu data: %6ld\n", (long)builder.indexes[PropNameData::IX_TOTAL_SIZE]);
    }

    if(options[CSOURCE].doesOccur) {
        writeCSourceFile(options[DESTDIR].value, builder);
    } else {
        writeDataFile(options[DESTDIR].value, builder, options[COPYRIGHT].doesOccur);
    }

    return 0; // success
}
