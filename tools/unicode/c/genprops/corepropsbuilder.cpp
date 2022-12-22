// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*
*   Copyright (C) 1999-2016, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  corepropsbuilder.cpp (was store.c & props2.cpp)
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
#include "unicode/uniset.h"
#include "unicode/unistr.h"
#include "unicode/usetiter.h"
#include "unicode/uscript.h"
#include "cmemory.h"
#include "cstring.h"
#include "genprops.h"
#include "propsvec.h"
#include "uassert.h"
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

The following is a description of format version 7.8 .

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
      then the script-code-or-index bit fields are an index to either a list or a pair in SCX,
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
    21..0x3ff       U_NT_NUMERIC    see below

    For U_NT_NUMERIC:
    ntv             value
    21..0xaf        integer     0..154
    0xb0..0x1df     fraction    ((ntv>>4)-12) / ((ntv&0xf)+1) = -1..17 / 1..16
    0x1e0..0x2ff    large int   ((ntv>>5)-14) * 10^((ntv&0x1f)+2) = (1..9)*(10^2..10^33)
                    (only one significant decimal digit)
    0x300..0x323    base-60 (sexagesimal) integer (new in format version 7.1)
                                ((ntv>>2)-0xbf) * 60^((ntv&3)+1) = (1..9)*(60^1..60^4)
    0x324..0x34b    fraction-20 (new in format version 7.3)
                                frac20 = ntv-0x324 = 0..0x17 -> 1|3|5|7 / 20|40|80|160|320|640
                                numerator: num = 2*(frac20&3)+1
                                denominator: den = 20<<(frac20>>2)
    0x34c..0x35b    fraction-32 (new in format version 7.6)
                                frac32 = ntv-0x34c = 0..15 -> 1|3|5|7 / 32|64|128|256
                                numerator: num = 2*(frac32&3)+1
                                denominator: den = 32<<(frac32>>2)
    0x35c..0x3ff    reserved

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

--- Changes in format version 7.1 ---

Unicode 6.2 adds sexagesimal (base-60) numeric values:
    cp;12432;na=CUNEIFORM NUMERIC SIGN SHAR2 TIMES GAL PLUS DISH;nv=216000
    cp;12433;na=CUNEIFORM NUMERIC SIGN SHAR2 TIMES GAL PLUS MIN;nv=432000

The encoding of numeric values was extended to handle such values.

--- Changes in format version 7.2 ---

ICU 57 adds 4 Emoji properties to vector word 2.
https://unicode-org.atlassian.net/browse/ICU-11802
http://www.unicode.org/reports/tr51/#Emoji_Properties

--- Changes in format version 7.3 ---

ICU 58 adds fraction-20 numeric values for new Unicode 9 Malayalam fraction characters.

--- Changes in format version 7.4 ---

ICU 60 adds the Prepended_Concatenation_Mark property to vector word 1.

ICU 60 adds the Emoji_Component property to vector word 2, for emoji 5.
https://unicode-org.atlassian.net/browse/ICU-13062
http://www.unicode.org/reports/tr51/#Emoji_Properties

--- Changes in format version 7.5 ---

ICU 62 adds the Extended_Pictographic property to vector word 2, for emoji 11.
http://www.unicode.org/reports/tr51/#Emoji_Properties

--- Changes in format version 7.6 ---

ICU 64 adds fraction-32 numeric values for new Unicode 12 Tamil fraction characters.

--- Changes in format version 7.7 ---

ICU 66 adds two bits for the UScriptCode or Script_Extensions index in vector word 0.
The value is split across bits 21..20 & 7..0.

--- Changes in format version 7.8 ---

ICU 70 moves the emoji properties from uprops.icu to (new) uemoji.icu.
The 6 bits in vector word 2 that stored emoji properties are unused again.

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
    { 7, 8, 0, 0 },                             /* formatVersion */
    { 14, 0, 0, 0 }                             /* dataVersion */
};

inline uint32_t splitScriptCodeOrIndex(uint32_t v) {
    return
        ((v << UPROPS_SCRIPT_HIGH_SHIFT) & UPROPS_SCRIPT_HIGH_MASK) |
        (v & UPROPS_SCRIPT_LOW_MASK);
}

class CorePropsBuilder : public PropsBuilder {
public:
    CorePropsBuilder(UErrorCode &errorCode);
    virtual ~CorePropsBuilder();

    virtual void setUnicodeVersion(const UVersionInfo version);
    virtual void setProps(const UniProps &, const UnicodeSet &newValues, UErrorCode &errorCode);
    virtual void build(UErrorCode &errorCode);
    virtual void writeCSourceFile(const char *path, UErrorCode &errorCode);
    virtual void writeBinaryData(const char *path, UBool withCopyright, UErrorCode &errorCode);

private:
    void setGcAndNumeric(const UniProps &, const UnicodeSet &newValues, UErrorCode &errorCode);

    UTrie2 *pTrie;
    UTrie2 *props2Trie;
    UPropsVectors *pv;
    UnicodeString scriptExtensions;
};

CorePropsBuilder::CorePropsBuilder(UErrorCode &errorCode)
        : pTrie(nullptr), props2Trie(nullptr), pv(nullptr) {
    pTrie=utrie2_open(0, 0, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: corepropsbuilder utrie2_open() failed - %s\n",
                u_errorName(errorCode));
    }
    pv=upvec_open(UPROPS_VECTOR_WORDS, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: corepropsbuilder upvec_open() failed - %s\n",
                u_errorName(errorCode));
    }
}

CorePropsBuilder::~CorePropsBuilder() {
    utrie2_close(pTrie);
    utrie2_close(props2Trie);
    upvec_close(pv);
}

void
CorePropsBuilder::setUnicodeVersion(const UVersionInfo version) {
    uprv_memcpy(dataInfo.dataVersion, version, 4);
}

static int32_t encodeFractional20(int32_t value, int32_t den) {
    if(den<20 || 640<den) { return -1; }
    int32_t frac20;
    if(value==1 || value==3 || value==5 || value==7) {
        frac20=value/2;
    } else {
        return -1;
    }
    // Denominator: 20 times which power of 2: 0..5 into bits 4..2
    do {
        if(den==20) {
            return UPROPS_NTV_FRACTION20_START+frac20;
        }
        if(den&1) {
            return -1;  // odd denominator, and we would lose the low bit in den/=2
        }
        den/=2;
        frac20+=4;
    } while(den>=20);
    return -1;
}

static int32_t encodeFractional32(int32_t value, int32_t den) {
    if(den<32 || 256<den) { return -1; }
    int32_t frac32;
    if(value==1 || value==3 || value==5 || value==7) {
        frac32=value/2;
    } else {
        return -1;
    }
    // Denominator: 32 times which power of 2: 0..3 into bits 3..2
    do {
        if(den==32) {
            return UPROPS_NTV_FRACTION32_START+frac32;
        }
        if(den&1) {
            return -1;  // odd denominator, and we would lose the low bit in den/=2
        }
        den/=2;
        frac32+=4;
    } while(den>=32);
    return -1;
}

// For nt=U_NT_NUMERIC.
static int32_t
encodeNumericValue(UChar32 start, const char *s, UErrorCode &errorCode) {
    const char *original=s;
    /* get a possible minus sign */
    UBool isNegative;
    if(*s=='-') {
        isNegative=true;
        ++s;
    } else {
        isNegative=false;
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
        if(s==numberLimit || (*numberLimit!=0 && *numberLimit!='/') || ul>0x7fffffff) {
            ntv=-1;
        } else {
            value=(int32_t)ul;
        }
        if(ntv>=0 && *numberLimit=='/') {
            /* fractional value, get the denominator */
            s=numberLimit+1;
            ul=uprv_strtoul(s, &numberLimit, 10);
            if(s==numberLimit || *numberLimit!=0 || ul==0 || ul>0x7fffffff) {
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
                // Note: value<=0x7fffffff guarantees exp<=33
                if(mant<=9) {
                    ntv=((mant+14)<<5)+(exp-2);
                } else {
                    // Try sexagesimal (base 60) numbers.
                    mant=value;
                    exp=0;
                    while((mant%60)==0) {
                        mant/=60;
                        ++exp;
                    }
                    if(mant<=9 && exp<=4) {
                        ntv=((mant+0xbf)<<2)+(exp-1);
                    } else {
                        ntv=-1;
                    }
                }
            }
        } else if(2<=exp && exp<=33 && 1<=value && value<=9) {
            /* large, single-significant-digit integer */
            ntv=((value+14)<<5)+(exp-2);
        } else {
            ntv=-1;
        }
    } else if(exp==0 && -1<=value && value<=17 && 1<=den && den<=16) {
        /* fraction */
        ntv=((value+12)<<4)+(den-1);
    } else if(exp==0 && value==-1 && den==0) {
        /* -1 parsed with den=0, encoded as pseudo-fraction -1/1 */
        ntv=((value+12)<<4);
    } else if(exp==0 && (ntv=encodeFractional20(value, den))>=0) {
        // fits into fractional-20 format
    } else if(exp==0 && (ntv=encodeFractional32(value, den))>=0) {
        // fits into fractional-32 format
    } else {
        ntv=-1;
    }
    if(ntv<0 || *numberLimit!=0) {
        fprintf(stderr, "genprops error: unable to encode numeric value nv=%s\n", original);
        errorCode=U_ILLEGAL_ARGUMENT_ERROR;
    }
    return ntv;
}

void
CorePropsBuilder::setGcAndNumeric(const UniProps &props, const UnicodeSet &newValues,
                                  UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }
    UChar32 start=props.start;
    UChar32 end=props.end;

    int32_t type=props.getIntProp(UCHAR_NUMERIC_TYPE);
    const char *nvString=props.numericValue;
    if(type!=U_NT_NONE && nvString==nullptr && start==end) {
        fprintf(stderr, "genprops error: cp line has Numeric_Type but no Numeric_Value\n");
        errorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    if(!newValues.contains(UCHAR_GENERAL_CATEGORY) && !newValues.contains(UCHAR_NUMERIC_VALUE)) {
        return;
    }

    int32_t ntv=UPROPS_NTV_NONE;  // numeric type & value
    if(nvString!=nullptr && uprv_strcmp(nvString, "NaN")!=0) {
        int32_t digitValue=props.digitValue;
        if( type<=U_NT_NONE || U_NT_NUMERIC<type ||
            ((type==U_NT_DECIMAL || type==U_NT_DIGIT) && digitValue<0)
        ) {
            fprintf(stderr, "genprops error: nt=%d but nv=%s\n",
                    (int)type, nvString==nullptr ? "nullptr" : nvString);
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
        utrie2_setRange32(pTrie, start, end, value, true, &errorCode);
    }
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "error: utrie2_setRange32(properties trie %04lX..%04lX) failed - %s\n",
                (long)start, (long)end, u_errorName(errorCode));
    }
}

struct PropToBinary {
    int32_t prop;  // UProperty
    int32_t vecWord, vecShift;
};

static const PropToBinary
propToBinaries[]={
    { UCHAR_WHITE_SPACE,                    1, UPROPS_WHITE_SPACE },
    { UCHAR_DASH,                           1, UPROPS_DASH },
    // Note: The Hyphen property is stabilized since Unicode 4.0
    // and deprecated since Unicode 6.0.
    { UCHAR_HYPHEN,                         1, UPROPS_HYPHEN },
    { UCHAR_QUOTATION_MARK,                 1, UPROPS_QUOTATION_MARK },
    { UCHAR_TERMINAL_PUNCTUATION,           1, UPROPS_TERMINAL_PUNCTUATION },
    // Note: The Hex_Digit and ASCII_Hex_Digit properties are probably stable enough
    // so that they could be hardcoded.
    { UCHAR_HEX_DIGIT,                      1, UPROPS_HEX_DIGIT },
    { UCHAR_ASCII_HEX_DIGIT,                1, UPROPS_ASCII_HEX_DIGIT },
    { UCHAR_IDEOGRAPHIC,                    1, UPROPS_IDEOGRAPHIC },
    { UCHAR_DIACRITIC,                      1, UPROPS_DIACRITIC },
    { UCHAR_EXTENDER,                       1, UPROPS_EXTENDER },
    // Note: The Noncharacter_Code_Point property is probably stable enough
    // so that it could be hardcoded.
    { UCHAR_NONCHARACTER_CODE_POINT,        1, UPROPS_NONCHARACTER_CODE_POINT },
    // Note: The Grapheme_Link property is deprecated since Unicode 5.0
    // because it is a "Duplication of ccc=9" (UAX #44).
    { UCHAR_GRAPHEME_LINK,                  1, UPROPS_GRAPHEME_LINK },
    { UCHAR_IDS_BINARY_OPERATOR,            1, UPROPS_IDS_BINARY_OPERATOR },
    { UCHAR_IDS_TRINARY_OPERATOR,           1, UPROPS_IDS_TRINARY_OPERATOR },
    { UCHAR_RADICAL,                        1, UPROPS_RADICAL },
    { UCHAR_UNIFIED_IDEOGRAPH,              1, UPROPS_UNIFIED_IDEOGRAPH },
    { UCHAR_DEPRECATED,                     1, UPROPS_DEPRECATED },
    { UCHAR_LOGICAL_ORDER_EXCEPTION,        1, UPROPS_LOGICAL_ORDER_EXCEPTION },
    { UCHAR_S_TERM,                         1, UPROPS_S_TERM },
    { UCHAR_VARIATION_SELECTOR,             1, UPROPS_VARIATION_SELECTOR },
    // Note: Pattern_Syntax & Pattern_White_Space are available via
    // the internal PatternProps class and need not be stored here any more.
    { UCHAR_PATTERN_SYNTAX,                 1, UPROPS_PATTERN_SYNTAX },
    { UCHAR_PATTERN_WHITE_SPACE,            1, UPROPS_PATTERN_WHITE_SPACE },
    { UCHAR_XID_START,                      1, UPROPS_XID_START },
    { UCHAR_XID_CONTINUE,                   1, UPROPS_XID_CONTINUE },
    { UCHAR_MATH,                           1, UPROPS_MATH },
    { UCHAR_ALPHABETIC,                     1, UPROPS_ALPHABETIC },
    { UCHAR_GRAPHEME_EXTEND,                1, UPROPS_GRAPHEME_EXTEND },
    { UCHAR_DEFAULT_IGNORABLE_CODE_POINT,   1, UPROPS_DEFAULT_IGNORABLE_CODE_POINT },
    { UCHAR_ID_START,                       1, UPROPS_ID_START },
    { UCHAR_ID_CONTINUE,                    1, UPROPS_ID_CONTINUE },
    { UCHAR_GRAPHEME_BASE,                  1, UPROPS_GRAPHEME_BASE },

    { UCHAR_PREPENDED_CONCATENATION_MARK,   1, UPROPS_PREPENDED_CONCATENATION_MARK },
};

struct PropToEnum {
    int32_t prop;  // UProperty
    int32_t vecWord, vecShift;
    uint32_t vecMask;
};

static const PropToEnum
propToEnums[]={
    { UCHAR_BLOCK,                      0, UPROPS_BLOCK_SHIFT, UPROPS_BLOCK_MASK },
    { UCHAR_EAST_ASIAN_WIDTH,           0, UPROPS_EA_SHIFT, UPROPS_EA_MASK },
    { UCHAR_DECOMPOSITION_TYPE,         2, 0, UPROPS_DT_MASK },
    { UCHAR_GRAPHEME_CLUSTER_BREAK,     2, UPROPS_GCB_SHIFT, UPROPS_GCB_MASK },
    { UCHAR_WORD_BREAK,                 2, UPROPS_WB_SHIFT, UPROPS_WB_MASK },
    { UCHAR_SENTENCE_BREAK,             2, UPROPS_SB_SHIFT, UPROPS_SB_MASK },
    { UCHAR_LINE_BREAK,                 2, UPROPS_LB_SHIFT, UPROPS_LB_MASK },
};

void
CorePropsBuilder::setProps(const UniProps &props, const UnicodeSet &newValues,
                           UErrorCode &errorCode) {
    setGcAndNumeric(props, newValues, errorCode);
    if(U_FAILURE(errorCode)) { return; }

    UChar32 start=props.start;
    UChar32 end=props.end;
    if(start==0 && end==0x10ffff) {
        // Also set bits for initialValue and errorValue.
        end=UPVEC_MAX_CP;
    }

    if(newValues.containsSome(0, UCHAR_BINARY_LIMIT-1)) {
        for(int32_t i=0; i<LENGTHOF(propToBinaries); ++i) {
            const PropToBinary &p2b=propToBinaries[i];
            U_ASSERT(p2b.vecShift<32);
            if(newValues.contains(p2b.prop)) {
                uint32_t mask=U_MASK(p2b.vecShift);
                uint32_t value= props.binProps[p2b.prop] ? mask : 0;
                upvec_setValue(pv, start, end, p2b.vecWord, value, mask, &errorCode);
            }
        }
    }

    // Set int property values.
    if(newValues.containsSome(UCHAR_INT_START, UCHAR_INT_LIMIT-1)) {
        for(int32_t i=0; i<LENGTHOF(propToEnums); ++i) {
            const PropToEnum &p2e=propToEnums[i];
            U_ASSERT(p2e.vecShift<32);
            if(newValues.contains(p2e.prop)) {
                uint32_t mask=p2e.vecMask;
                uint32_t value=(uint32_t)(props.getIntProp(p2e.prop)<<p2e.vecShift);
                U_ASSERT((value&mask)==value);
                upvec_setValue(pv, start, end, p2e.vecWord, value, mask, &errorCode);
            }
        }
    }
    if(newValues.contains(UCHAR_AGE)) {
        if(props.age[0]>15 || props.age[1]>15 || props.age[2]!=0 || props.age[3]!=0) {
            char buffer[U_MAX_VERSION_STRING_LENGTH];
            u_versionToString(props.age, buffer);
            fprintf(stderr, "genprops error: age %s cannot be encoded\n", buffer);
            errorCode=U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        uint32_t version=(props.age[0]<<4)|props.age[1];
        upvec_setValue(pv, start, end,
                       0, version<<UPROPS_AGE_SHIFT, UPROPS_AGE_MASK,
                       &errorCode);
    }

    // Set the script value if the Script_Extensions revert to {Script}.
    // Otherwise we would have to duplicate the code for doing so.
    // Script and Script_Extensions share a bit field, so that by setting it to just the script
    // we remove the Script_Extensions.
    // (We do not just set the script bit in newValues because that is const.)
    // For example, for U+3000:
    // block;3000..303F;age=1.1;...;sc=Zyyy;scx=Bopo Hang Hani Hira Kana Yiii;vo=U
    // cp;3000;...;gc=Zs;lb=BA;na=IDEOGRAPHIC SPACE;...;SB=SP;scx=<script>;WSpace
    UBool revertToScript=
        newValues.contains(UCHAR_SCRIPT_EXTENSIONS) && props.scx.isEmpty() &&
        !newValues.contains(UCHAR_SCRIPT);
    if(newValues.contains(UCHAR_SCRIPT) || revertToScript) {
        int32_t script=props.getIntProp(UCHAR_SCRIPT);
        uint32_t value=splitScriptCodeOrIndex(script);
        // Use UPROPS_SCRIPT_X_MASK:
        // When writing a Script code, remove Script_Extensions bits as well.
        // If needed, they will get written again.
        upvec_setValue(pv, start, end, 0, value, UPROPS_SCRIPT_X_MASK, &errorCode);
    }
    // Write a new (Script, Script_Extensions) value if there are Script_Extensions
    // and either Script or Script_Extensions are new on the current line.
    // (If only Script is new, then it just clobbered the relevant bits.)
    if( !props.scx.isEmpty() &&
        (newValues.contains(UCHAR_SCRIPT) || newValues.contains(UCHAR_SCRIPT_EXTENSIONS))
    ) {
        UnicodeString codes;  // vector of 16-bit UScriptCode values
        UnicodeSetIterator iter(props.scx);
        while(iter.next()) { codes.append((UChar)iter.getCodepoint()); }

        // Set bit 15 on the last script code, for termination.
        int32_t length=codes.length();
        codes.setCharAt(length-1, (UChar)(codes[length-1]|0x8000));
        // Find this list of codes in the Script_Extensions data so far, or add this list.
        int32_t index=scriptExtensions.indexOf(codes);
        if(index<0) {
            index=scriptExtensions.length();
            scriptExtensions.append(codes);
        }

        // Encode the (Script, Script_Extensions index) pair.
        int32_t script=props.getIntProp(UCHAR_SCRIPT);
        uint32_t scriptX;
        if(script==USCRIPT_COMMON) {
            scriptX=UPROPS_SCRIPT_X_WITH_COMMON;
        } else if(script==USCRIPT_INHERITED) {
            scriptX=UPROPS_SCRIPT_X_WITH_INHERITED;
        } else {
            // Store an additional pair of 16-bit units for an unusual main Script code
            // together with the Script_Extensions index.
            UnicodeString codeIndexPair;
            codeIndexPair.append((UChar)script).append((UChar)index);
            index=scriptExtensions.indexOf(codeIndexPair);
            if(index<0) {
                index=scriptExtensions.length();
                scriptExtensions.append(codeIndexPair);
            }
            scriptX=UPROPS_SCRIPT_X_WITH_OTHER;
        }
        if(index>UPROPS_MAX_SCRIPT) {
            fprintf(stderr, "genprops: Script_Extensions indexes overflow bit fields\n");
            errorCode=U_BUFFER_OVERFLOW_ERROR;
            return;
        }
        scriptX|=splitScriptCodeOrIndex(index);
        upvec_setValue(pv, start, end, 0, scriptX, UPROPS_SCRIPT_X_MASK, &errorCode);
    }
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: unable to set props2 values for %04lX..%04lX: %s\n",
                (long)start, (long)end, u_errorName(errorCode));
    }
}

static int32_t indexes[UPROPS_INDEX_COUNT]={
    0, 0, 0, 0,
    0, 0, 0, 0,
    0, 0, 0, 0,
    0, 0, 0, 0
};

static uint8_t trieBlock[100000];
static int32_t trieSize;
static uint8_t props2TrieBlock[100000];
static int32_t props2TrieSize;

static int32_t totalSize;

void
CorePropsBuilder::build(UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }

    utrie2_freeze(pTrie, UTRIE2_16_VALUE_BITS, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr,
                "genprops/core error: utrie2_freeze(main trie) failed: %s\n",
                u_errorName(errorCode));
        return;
    }
    trieSize=utrie2_serialize(pTrie, trieBlock, sizeof(trieBlock), &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr,
                "genprops/core error: utrie2_serialize(main trie) failed: %s (length %ld)\n",
                u_errorName(errorCode), (long)trieSize);
        return;
    }

    props2Trie=upvec_compactToUTrie2WithRowIndexes(pv, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops/core error: unable to build trie for additional properties: %s\n",
                u_errorName(errorCode));
        return;
    }

    props2TrieSize=utrie2_serialize(props2Trie,
                                    props2TrieBlock, (int32_t)sizeof(props2TrieBlock),
                                    &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr,
                "genprops/core error: utrie2_freeze(additional properties)+utrie2_serialize() "
                "failed: %s\n",
                u_errorName(errorCode));
        return;
    }

    int32_t pvRows;
    upvec_getArray(pv, &pvRows, nullptr);
    int32_t pvCount=pvRows*UPROPS_VECTOR_WORDS;

    /* round up scriptExtensions to multiple of 4 bytes */
    if(scriptExtensions.length()&1) {
        scriptExtensions.append((UChar)0);
    }

    /* set indexes */
    int32_t offset=sizeof(indexes)/4;       /* uint32_t offset to the properties trie */
    offset+=trieSize>>2;
    indexes[UPROPS_PROPS32_INDEX]=          /* set indexes to the same offsets for empty */
    indexes[UPROPS_EXCEPTIONS_INDEX]=       /* structures from the old format version 3 */
    indexes[UPROPS_EXCEPTIONS_TOP_INDEX]=   /* so that less runtime code has to be changed */
    indexes[UPROPS_ADDITIONAL_TRIE_INDEX]=offset;

    offset+=props2TrieSize/4;
    indexes[UPROPS_ADDITIONAL_VECTORS_INDEX]=offset;
    indexes[UPROPS_ADDITIONAL_VECTORS_COLUMNS_INDEX]=UPROPS_VECTOR_WORDS;
    offset+=pvCount;
    indexes[UPROPS_SCRIPT_EXTENSIONS_INDEX]=offset;
    offset+=scriptExtensions.length()/2;
    indexes[UPROPS_RESERVED_INDEX_7]=offset;
    indexes[UPROPS_RESERVED_INDEX_8]=offset;
    indexes[UPROPS_DATA_TOP_INDEX]=offset;
    totalSize=4*offset;

    indexes[UPROPS_MAX_VALUES_INDEX]=
        (((int32_t)U_EA_COUNT-1)<<UPROPS_EA_SHIFT)|
        (((int32_t)UBLOCK_COUNT-1)<<UPROPS_BLOCK_SHIFT)|
        (int32_t)splitScriptCodeOrIndex(USCRIPT_CODE_LIMIT-1);
    indexes[UPROPS_MAX_VALUES_2_INDEX]=
        (((int32_t)U_LB_COUNT-1)<<UPROPS_LB_SHIFT)|
        (((int32_t)U_SB_COUNT-1)<<UPROPS_SB_SHIFT)|
        (((int32_t)U_WB_COUNT-1)<<UPROPS_WB_SHIFT)|
        (((int32_t)U_GCB_COUNT-1)<<UPROPS_GCB_SHIFT)|
        ((int32_t)U_DT_COUNT-1);

    if(!beQuiet) {
        puts("* uprops.icu stats *");
        printf("trie size in bytes:                    %5u\n", (int)trieSize);
        printf("size in bytes of additional props trie:%5u\n", (int)props2TrieSize);
        printf("number of additional props vectors:    %5u\n", (int)pvRows);
        printf("number of 32-bit words per vector:     %5u\n", UPROPS_VECTOR_WORDS);
        printf("number of 16-bit scriptExtensions:     %5u\n", (int)scriptExtensions.length());
        printf("data size:                            %6ld\n", (long)totalSize);
    }
}

void
CorePropsBuilder::writeCSourceFile(const char *path, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }

    int32_t pvRows;
    const uint32_t *pvArray=upvec_getArray(pv, &pvRows, nullptr);
    int32_t pvCount=pvRows*UPROPS_VECTOR_WORDS;

    FILE *f=usrc_create(path, "uchar_props_data.h", 2016,
                        "icu/tools/unicode/c/genprops/corepropsbuilder.cpp");
    if(f==nullptr) {
        errorCode=U_FILE_ACCESS_ERROR;
        return;
    }
    fputs("#ifdef INCLUDED_FROM_UCHAR_C\n\n", f);
    usrc_writeArray(f,
        "static const UVersionInfo dataVersion={",
        dataInfo.dataVersion, 8, 4,
        "",
        "};\n\n");
    usrc_writeUTrie2Arrays(f,
        "static const uint16_t propsTrie_index[%ld]={\n", nullptr,
        pTrie,
        "\n};\n\n");
    usrc_writeUTrie2Struct(f,
        "static const UTrie2 propsTrie={\n",
        pTrie, "propsTrie_index", nullptr,
        "};\n\n");

    usrc_writeUTrie2Arrays(f,
        "static const uint16_t propsVectorsTrie_index[%ld]={\n", nullptr,
        props2Trie,
        "\n};\n\n");
    usrc_writeUTrie2Struct(f,
        "static const UTrie2 propsVectorsTrie={\n",
        props2Trie, "propsVectorsTrie_index", nullptr,
        "};\n\n");

    usrc_writeArray(f,
        "static const uint32_t propsVectors[%ld]={\n",
        pvArray, 32, pvCount,
        "",
        "};\n\n");
    fprintf(f, "static const int32_t countPropsVectors=%ld;\n", (long)pvCount);
    fprintf(f, "static const int32_t propsVectorsColumns=%ld;\n", (long)UPROPS_VECTOR_WORDS);

    usrc_writeArray(f,
        "static const uint16_t scriptExtensions[%ld]={\n",
        scriptExtensions.getBuffer(), 16, scriptExtensions.length(),
        "",
        "};\n\n");

    usrc_writeArray(f,
        "static const int32_t indexes[UPROPS_INDEX_COUNT]={",
        indexes, 32, UPROPS_INDEX_COUNT,
        "",
        "};\n\n");
    fputs("#endif  // INCLUDED_FROM_UCHAR_C\n", f);
    fclose(f);
}

void
CorePropsBuilder::writeBinaryData(const char *path, UBool withCopyright, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }

    int32_t pvRows;
    const uint32_t *pvArray=upvec_getArray(pv, &pvRows, nullptr);
    int32_t pvCount=pvRows*UPROPS_VECTOR_WORDS;

    UNewDataMemory *pData=udata_create(path, "icu", "uprops", &dataInfo,
                                       withCopyright ? U_COPYRIGHT_STRING : nullptr, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops: udata_create(%s, uprops.icu) failed - %s\n",
                path, u_errorName(errorCode));
        return;
    }

    udata_writeBlock(pData, indexes, sizeof(indexes));
    udata_writeBlock(pData, trieBlock, trieSize);
    udata_writeBlock(pData, props2TrieBlock, props2TrieSize);
    udata_writeBlock(pData, pvArray, pvCount*4);
    udata_writeBlock(pData, scriptExtensions.getBuffer(), scriptExtensions.length()*2);

    long dataLength=udata_finish(pData, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops: error %s writing the output file\n", u_errorName(errorCode));
        return;
    }

    if(dataLength!=(long)totalSize) {
        fprintf(stderr,
                "udata_finish(uprops.icu) reports %ld bytes written but should be %ld\n",
                dataLength, (long)totalSize);
        errorCode=U_INTERNAL_PROGRAM_ERROR;
    }
}

PropsBuilder *
createCorePropsBuilder(UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return nullptr; }
    PropsBuilder *pb=new CorePropsBuilder(errorCode);
    if(pb==nullptr) {
        errorCode=U_MEMORY_ALLOCATION_ERROR;
    }
    return pb;
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
