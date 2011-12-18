/*
*******************************************************************************
*
*   Copyright (C) 1999-2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  corepropswriter.cpp (was store.c)
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999dec11
*   created by: Markus W. Scherer
*
*   Store Unicode character properties efficiently for
*   random access.
*/

#include <stdio.h>
#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/udata.h"
#include "cmemory.h"
#include "cstring.h"
#include "genprops.h"
#include "unewdata.h"
#include "uprops.h"
#include "utrie2.h"
#include "writesrc.h"

/* Unicode character properties file format ------------------------------------

The file format prepared and written here contains several data
structures that store indexes or data.

Before the data contents described below, there are the headers required by
the udata API for loading ICU data. Especially, a UDataInfo structure
precedes the actual data. It contains platform properties values and the
file format version.

The following is a description of format version 7 .

Data contents:

The contents is a parsed, binary form of several Unicode character
database files, most prominently UnicodeData.txt.

Any Unicode code point from 0 to 0x10ffff can be looked up to get
the properties, if any, for that code point. This means that the input
to the lookup are 21-bit unsigned integers, with not all of the
21-bit range used.

It is assumed that client code keeps a uint32_t pointer
to the beginning of the data:

    const uint32_t *p32;

Formally, the file contains the following structures:

    const int32_t indexes[16] with values i0..i15:

  i0 indicates the length of the main trie.
  i0..i3 all have the same value in format versions 4.0 and higher;
         the related props32[] and exceptions[] and uchars[] were used in format version 3

    i0 propsIndex; -- 32-bit unit index to the table of 32-bit properties words
    i1 exceptionsIndex;  -- 32-bit unit index to the table of 32-bit exception words
    i2 exceptionsTopIndex; -- 32-bit unit index to the array of UChars for special mappings

    i3 additionalTrieIndex; -- 32-bit unit index to the additional trie for more properties
    i4 additionalVectorsIndex; -- 32-bit unit index to the table of properties vectors
    i5 additionalVectorsColumns; -- number of 32-bit words per properties vector

    i6 scriptExtensionsIndex; -- 32-bit unit index to the Script_Extensions data
    i7 reservedIndex7; -- 32-bit unit index to the top of the Script_Extensions data
    i8 reservedIndex8; -- for now: i7, i8 and i9 have the same values
    i9 dataTopIndex; -- size of the data file (number of 32-bit units after the header)

    i10 maxValues; -- maximum code values for vector word 0, see uprops.h (new in format version 3.1+)
    i11 maxValues2; -- maximum code values for vector word 2, see uprops.h (new in format version 3.2)
    i12..i15 reservedIndexes; -- reserved values; 0 for now

    PT serialized properties trie, see utrie2.h (byte size: 4*(i0-16))

  P, E, and U are not used (empty) in format versions 4 and above

    P  const uint32_t props32[i1-i0];
    E  const uint32_t exceptions[i2-i1];
    U  const UChar uchars[2*(i3-i2)];

    AT serialized trie for additional properties (byte size: 4*(i4-i3))
    PV const uint32_t propsVectors[(i6-i4)/i5][i5]==uint32_t propsVectors[i6-i4];

    SCX const uint16_t scriptExtensions[2*(i7-i6)];

      SCX contains Script_Extensions lists and (Script code, Script_Extensions index) pairs.
      A Script_Extensions list is a sequence of UScriptCode values in ascending order,
      with the last code having bit 15 set for termination.
      A (Script code, Script_Extensions index) pair is the main UScriptCode (Script value)
      followed by the index of the Script_Extensions list.
      If the propsVectors[] column 0 value indicates that there are Script_Extensions,
      then the UPROPS_SCRIPT_MASK bit field is an index to either a list or a pair in SCX,
      rather than the Script itself. The high bits in the UPROPS_SCRIPT_X_MASK fields
      indicate whether the main Script value is Common or Inherited (and the index is to a list)
      vs. another value (and the index is to a pair).
      (See UPROPS_SCRIPT_X_WITH_COMMON etc. in uprops.h.)

Trie lookup and properties:

In order to condense the data for the 21-bit code space, several properties of
the Unicode code assignment are exploited:
- The code space is sparse.
- There are several 10k of consecutive codes with the same properties.
- Characters and scripts are allocated in groups of 16 code points.
- Inside blocks for scripts the properties are often repetitive.
- The 21-bit space is not fully used for Unicode.

The lookup of properties for a given code point is done with a trie lookup,
using the UTrie implementation.
The trie lookup result is a 16-bit properties word.

With a given Unicode code point

    UChar32 c;

and 0<=c<0x110000, the lookup is done like this:

    uint16_t props;
    UTRIE_GET16(trie, c, props);

Each 16-bit properties word contains:

 0.. 4  general category
     5  reserved
 6..15  numeric type and value (ntv)

Encoding of numeric type and value in the 10-bit ntv field:
    ntv             type            value
    0               U_NT_NONE       0
    1..10           U_NT_DECIMAL    0..9
    11..20          U_NT_DIGIT      0..9
    21..0x2ff       U_NT_NUMERIC    see below
    0x300..0x3ff    reserved

    For U_NT_NUMERIC:
    ntv             value
    21..0xaf        integer     0..154
    0xb0..0x1df     fraction    ((ntv>>4)-12) / ((ntv&0xf)+1) = -1..17 / 1..16
    0x1e0..0x2ff    large int   ((ntv>>5)-14) * 10^((ntv&0x1f)+2) = (1..9)*(10^2..10^33)
                    (only one significant decimal digit)

--- Additional properties (new in format version 2.1) ---

The second trie for additional properties (AT) is also a UTrie with 16-bit data.
The data words consist of 32-bit unit indexes (not row indexes!) into the
table of unique properties vectors (PV).
Each vector contains a set of properties.
The width of a vector (number of uint32_t per row) may change
with the formatVersion, it is stored in i5.

Current properties: see icu/source/common/uprops.h

--- Changes in format version 3.1 ---

See i10 maxValues above, contains only UBLOCK_COUNT and USCRIPT_CODE_LIMIT.

--- Changes in format version 3.2 ---

- The tries use linear Latin-1 ranges.
- The additional properties bits store full properties XYZ instead
  of partial Other_XYZ, so that changes in the derivation formulas
  need not be tracked in runtime library code.
- Joining Type and Line Break are also stored completely, so that uprops.c
  needs no runtime formulas for enumerated properties either.
- Store the case-sensitive flag in the main properties word.
- i10 also contains U_LB_COUNT and U_EA_COUNT.
- i11 contains maxValues2 for vector word 2.

--- Changes in format version 4 ---

The format changes between version 3 and 4 because the properties related to
case mappings and bidi/shaping are pulled out into separate files
for modularization.
In order to reduce the need for code changes, some of the previous data
structures are omitted, rather than rearranging everything.

(The change to format version 4 is for ICU 3.4. The last CVS revision of
genprops/store.c for format version 3.2 is 1.48.)

The main trie's data is significantly simplified:
- The trie's 16-bit data word is used directly instead of as an index
  into props32[].
- The trie uses the default trie folding functions instead of custom ones.
- Numeric values are stored directly in the trie data word, with special
  encodings.
- No more exception data (the data that needed it was pulled out, or, in the
  case of numeric values, encoded differently).
- No more string data (pulled out - was for case mappings).

Also, some of the previously used properties vector bits are reserved again.

The indexes[] values for the omitted structures are still filled in
(indicating zero-length arrays) so that the swapper code remains unchanged.

--- Changes in format version 5 ---

Format version 5 became necessary because the bit field for script codes
overflowed. The changes are incompatible because
old code would have seen nonsensically low values for new, higher script codes.

Rearranged bit fields in the second trie (AT) and widened three (Script, Block,
Word_Break) by one bit each.

Modified bit fields in icu/source/common/uprops.h

--- Changes in format version 6 ---

Format version 6 became necessary because Unicode 5.2 adds fractions with
denominators 9, 10 and 16, and it was easier to redesign the encoding of numeric
types and values rather than add another variant to the previous format.

--- Changes in format version 7 ---

Unicode 6.0 adds Script_Extensions. For characters with script extensions data,
the script code bits are an index into the new Script_Extensions array rather
than a script code.

Change from UTrie to UTrie2.

----------------------------------------------------------------------------- */

U_NAMESPACE_USE

/* UDataInfo cf. udata.h */
static UDataInfo dataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    U_SIZEOF_UCHAR,
    0,

    { 0x55, 0x50, 0x72, 0x6f },                 /* dataFormat="UPro" */
    { 7, 0, 0, 0 },                             /* formatVersion */
    { 6, 0, 0, 0 }                              /* dataVersion */
};

static UTrie2 *pTrie=NULL;

class CorePropsWriter : public PropsWriter {
public:
    CorePropsWriter(UErrorCode &errorCode);
    virtual ~CorePropsWriter();

    virtual void setUnicodeVersion(const UVersionInfo version);
    virtual void setProps(const UniProps &, const UnicodeSet &newValues, UErrorCode &errorCode);
    virtual void finalizeData(UErrorCode &errorCode);
    virtual void writeCSourceFile(const char *path, UErrorCode &errorCode);
    virtual void writeBinaryData(const char *path, UBool withCopyright, UErrorCode &errorCode);
};

CorePropsWriter::CorePropsWriter(UErrorCode &errorCode) {
    pTrie=utrie2_open(0, 0, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: corepropswriter utrie2_open() failed - %s\n",
                u_errorName(errorCode));
    }
}

CorePropsWriter::~CorePropsWriter() {
    utrie2_close(pTrie);
}

void
CorePropsWriter::setUnicodeVersion(const UVersionInfo version) {
    uprv_memcpy(dataInfo.dataVersion, version, 4);
}

// For nt=U_NT_NUMERIC.
static int32_t
encodeNumericValue(UChar32 start, const char *s, UErrorCode &errorCode) {
    /* get a possible minus sign */
    UBool isNegative;
    if(*s=='-') {
        isNegative=TRUE;
        ++s;
    } else {
        isNegative=FALSE;
    }

    int32_t value=0, den=0, exp=0, ntv=0;
    char *numberLimit;
    /* try large, single-significant-digit numbers, may otherwise overflow strtoul() */
    if('1'<=s[0] && s[0]<='9' && s[1]=='0' && s[2]=='0') {
        value=s[0]-'0';
        numberLimit=const_cast<char *>(s);
        while(*(++numberLimit)=='0') {
            ++exp;
        }
    } else {
        /* normal number parsing */
        unsigned long ul=uprv_strtoul(s, &numberLimit, 10);
        if(ul>0x7fffffff) {
            ntv=-1;
        } else {
            value=(int32_t)ul;
        }
        if(s<numberLimit && *numberLimit=='/') {
            /* fractional value, get the denominator */
            ul=uprv_strtoul(numberLimit+1, &numberLimit, 10);
            if(ul==0 || ul>0x7fffffff) {
                ntv=-1;
            } else {
                den=(int32_t)ul;
            }
        }
    }
    if(isNegative) {
        value=-(int32_t)value;
    }

    if(ntv<0) {
        // pass
    } else if(den==0 && value>=0) {
        if(exp==2 && (value*100)<=UPROPS_NTV_MAX_SMALL_INT) {
            /* small integer parsed like a large one */
            ntv=UPROPS_NTV_NUMERIC_START+value*100;
        } else if(exp==0) {
            if(value<=UPROPS_NTV_MAX_SMALL_INT) {
                /* small integer */
                ntv=UPROPS_NTV_NUMERIC_START+value;
            } else {
                /* large integer parsed like a small one */
                /* split the value into mantissa and exponent, base 10 */
                int32_t mant=value;
                while((mant%10)==0) {
                    mant/=10;
                    ++exp;
                }
                if(mant<=9) {
                    ntv=((mant+14)<<5)+(exp-2);
                }
            }
        } else if(2<=exp && exp<=33 && 1<=value && value<=9) {
            /* large, single-significant-digit integer */
            ntv=((value+14)<<5)+(exp-2);
        }
    } else if(exp==0) {
        if(-1<=value && value<=17 && 1<=den && den<=16) {
            /* fraction */
            ntv=((value+12)<<4)+(den-1);
        }
    }
    if(ntv<0 || *numberLimit!=0) {
        fprintf(stderr, "genprops error: unable to encode numeric value nv=%s\n", s);
        errorCode=U_ILLEGAL_ARGUMENT_ERROR;
    }
    return ntv;
}

void
CorePropsWriter::setProps(const UniProps &props, const UnicodeSet &newValues, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }
    UChar32 start=props.start;
    UChar32 end=props.end;

    int32_t type=props.getIntProp(UCHAR_NUMERIC_TYPE);
    const char *nvString=props.numericValue;
    if(type!=U_NT_NONE && nvString==NULL && start==end) {
        fprintf(stderr, "genprops error: cp line has Numeric_Type but no Numeric_Value\n");
        errorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    if(!newValues.contains(UCHAR_GENERAL_CATEGORY) && !newValues.contains(UCHAR_NUMERIC_VALUE)) {
        return;
    }

    int32_t ntv=UPROPS_NTV_NONE;  // numeric type & value
    if(nvString!=NULL) {
        int32_t digitValue=props.digitValue;
        if( type<=U_NT_NONE || U_NT_NUMERIC<type ||
            ((type==U_NT_DECIMAL || type==U_NT_DIGIT) && digitValue<0)
        ) {
            fprintf(stderr, "genprops error: nt=%d but nv=%s\n",
                    (int)type, nvString==NULL ? "NULL" : nvString);
            errorCode=U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }

        switch(type) {
        case U_NT_NONE:
            ntv=UPROPS_NTV_NONE;
            break;
        case U_NT_DECIMAL:
            ntv=UPROPS_NTV_DECIMAL_START+digitValue;
            break;
        case U_NT_DIGIT:
            ntv=UPROPS_NTV_DIGIT_START+digitValue;
            break;
        case U_NT_NUMERIC:
            if(digitValue>=0) {
                ntv=UPROPS_NTV_NUMERIC_START+digitValue;
            } else {
                ntv=encodeNumericValue(start, nvString, errorCode);
                if(U_FAILURE(errorCode)) {
                    return;
                }
            }
        default:
            break;  // unreachable
        }
    }

    uint32_t value=
        (uint32_t)props.getIntProp(UCHAR_GENERAL_CATEGORY) |
        (ntv<<UPROPS_NUMERIC_TYPE_VALUE_SHIFT);
    if(start==end) {
        utrie2_set32(pTrie, start, value, &errorCode);
    } else {
        utrie2_setRange32(pTrie, start, end, value, TRUE, &errorCode);
    }
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "error: utrie2_setRange32(properties trie %04lX..%04lX) failed - %s\n",
                (long)start, (long)end, u_errorName(errorCode));
    }
}

static int32_t indexes[UPROPS_INDEX_COUNT]={
    0, 0, 0, 0,
    0, 0, 0, 0,
    0, 0, 0, 0,
    0, 0, 0, 0
};

static uint8_t trieBlock[40000];
static int32_t trieSize;
static int32_t totalSize;

void
CorePropsWriter::finalizeData(UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }

    utrie2_freeze(pTrie, UTRIE2_16_VALUE_BITS, &errorCode);
    trieSize=utrie2_serialize(pTrie, trieBlock, sizeof(trieBlock), &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: utrie2_freeze(main trie)+utrie2_serialize() failed: %s (length %ld)\n",
                u_errorName(errorCode), (long)trieSize);
        return;
    }

    int32_t offset=sizeof(indexes)/4;       /* uint32_t offset to the properties trie */
    offset+=trieSize>>2;
    indexes[UPROPS_PROPS32_INDEX]=          /* set indexes to the same offsets for empty */
    indexes[UPROPS_EXCEPTIONS_INDEX]=       /* structures from the old format version 3 */
    indexes[UPROPS_EXCEPTIONS_TOP_INDEX]=   /* so that less runtime code has to be changed */
    indexes[UPROPS_ADDITIONAL_TRIE_INDEX]=offset;

    if(beVerbose) {
        printf("trie size in bytes:                    %5u\n", (int)trieSize);
    }

    totalSize=4*offset+props2FinalizeData(indexes, errorCode);

    if(beVerbose) {
        printf("data size:                            %6ld\n", (long)totalSize);
    }
}

void
CorePropsWriter::writeCSourceFile(const char *path, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }

    FILE *f=usrc_createFromGenerator(path, "uchar_props_data.h",
                                     "icu/tools/src/unicode/c/genprops/corepropswriter.cpp");
    if(f==NULL) {
        errorCode=U_FILE_ACCESS_ERROR;
        return;
    }
    fputs("#ifndef INCLUDED_FROM_UCHAR_C\n"
          "#   error This file must be #included from uchar.c only.\n"
          "#endif\n\n", f);
    usrc_writeArray(f,
        "static const UVersionInfo dataVersion={",
        dataInfo.dataVersion, 8, 4,
        "};\n\n");
    usrc_writeUTrie2Arrays(f,
        "static const uint16_t propsTrie_index[%ld]={\n", NULL,
        pTrie,
        "\n};\n\n");
    usrc_writeUTrie2Struct(f,
        "static const UTrie2 propsTrie={\n",
        pTrie, "propsTrie_index", NULL,
        "};\n\n");

    props2AppendToCSourceFile(f, errorCode);

    usrc_writeArray(f,
        "static const int32_t indexes[UPROPS_INDEX_COUNT]={",
        indexes, 32, UPROPS_INDEX_COUNT,
        "};\n\n");
    fclose(f);
}

void
CorePropsWriter::writeBinaryData(const char *path, UBool withCopyright, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }

    UNewDataMemory *pData=udata_create(path, "icu", "uprops", &dataInfo,
                                       withCopyright ? U_COPYRIGHT_STRING : NULL, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops: udata_create(%s, uprops.icu) failed - %s\n",
                path, u_errorName(errorCode));
        return;
    }

    udata_writeBlock(pData, indexes, sizeof(indexes));
    udata_writeBlock(pData, trieBlock, trieSize);
    props2AppendToBinaryFile(pData, errorCode);

    long dataLength=udata_finish(pData, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops: error %s writing the output file\n", u_errorName(errorCode));
        return;
    }

    if(dataLength!=(long)totalSize) {
        fprintf(stderr, "genprops: data length %ld != calculated size %ld\n",
                dataLength, (long)totalSize);
        errorCode=U_INTERNAL_PROGRAM_ERROR;
    }
}

PropsWriter *
createCorePropsWriter(UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return NULL; }
    PropsWriter *pw=new CorePropsWriter(errorCode);
    if(pw==NULL) {
        errorCode=U_MEMORY_ALLOCATION_ERROR;
    }
    return pw;
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
