// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*
*   Copyright (C) 2004-2016, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  casepropsbuilder.cpp (was gencase/store.c)
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2004aug28
*   created by: Markus W. Scherer
*
*   Store Unicode case mapping properties efficiently for
*   random access.
*/

#include <stdio.h>
#include <stdlib.h>
#include "unicode/utypes.h"
#include "unicode/localpointer.h"
#include "unicode/uchar.h"
#include "unicode/udata.h"
#include "unicode/uniset.h"
#include "unicode/usetiter.h"
#include "unicode/ustring.h"
#include "cmemory.h"
#include "cstring.h"
#include "genprops.h"
#include "ppucd.h"
#include "uassert.h"
#include "uarrsort.h"
#include "ucase.h"
#include "unewdata.h"
#include "utrie2.h"
#include "writesrc.h"

/* Unicode case mapping properties file format ---------------------------------

The file format prepared and written here contains several data
structures that store indexes or data.

Before the data contents described below, there are the headers required by
the udata API for loading ICU data. Especially, a UDataInfo structure
precedes the actual data. It contains platform properties values and the
file format version.

The following is a description of format version 4.0 .

Format version 1.1 adds data for case closure.

Format version 1.2 adds an exception bit for case-ignorable. Needed because
the Cased and Case_Ignorable properties are not disjoint.

Format version 2.0 changes from UTrie to UTrie2.

Format version 3.0 (ICU 49) shuffles the trie bits to simplify some builder and runtime code.
It moves the Case_Ignorable flag from sometimes-trie-bit 6, sometimes-exception-bit 11
to always-trie-bit 2 and adjusts the higher trie bits accordingly.
Exception index reduced from 12 bits to 11, simple case mapping delta reduced from 10 bits to 9.

Format version 4.0 (ICU 62) swaps trie data bits 3 and 4, exception vs. case-sensitive,
and when exception=1 then data bits 15..4 (not 15..5) are used for the exception index,
and the case-sensitive bit is moved into the excWord. This will allow for more exceptions words.
Also, an additional optional exception slot is used for a 16-bit delta,
with one more excWord bit if the delta is actually negative,
for a reasonably compact, and compressible, encoding of simple case mappings
between distant blocks for Cherokee, Georgian, and similar.
Another excWord bit is used to indicate that the character has no simple case folding,
even if it has a simple lowercase mapping.

The file contains the following structures:

    const int32_t indexes[i0] with values i0, i1, ...:
    (see UCASE_IX_... constants for names of indexes)

    i0 indexLength; -- length of indexes[] (UCASE_IX_TOP)
    i1 dataLength; -- length in bytes of the post-header data (incl. indexes[])
    i2 trieSize; -- size in bytes of the case mapping properties trie
    i3 exceptionsLength; -- length in uint16_t of the exceptions array
    i4 unfoldLength; -- length in uint16_t of the reverse-folding array (new in format version 1.1)

    i5..i14 reservedIndexes; -- reserved values; 0 for now

    i15 maxFullLength; -- maximum length of a full case mapping/folding string


    Serialized trie, see utrie2.h;

    const uint16_t exceptions[exceptionsLength];

    const UChar unfold[unfoldLength];


Trie data word:
Bits
if(exception) {
    15..4   unsigned exception index
} else {
    if(not uncased) {
        15..7   signed delta to simple case mapping code point
                (add delta to input code point)
    } else {
        15..7   reserved, 0
    }
     6..5   0 normal character with cc=0
            1 soft-dotted character
            2 cc=230
            3 other cc
            The runtime code relies on these two bits to be adjacent with this encoding.
}
    4   case-sensitive
    3   exception
    2   case-ignorable
 1..0   0 uncased
        1 lowercase
        2 uppercase
        3 titlecase
        The runtime code relies on the case-ignorable and case type bits 2..0
        to be the lowest bits with this encoding.


Exceptions:
A sub-array of the exceptions array is indexed by the exception index in a
trie word.
The sub-array consists of the following fields:
    uint16_t excWord;
    uint16_t optional values [];
    UTF-16 strings for full (string) mappings for lowercase, case folding, uppercase, titlecase

excWord: (see UCASE_EXC_...)
Bits
    15  conditional case folding
    14  conditional special casing
13..12  same as non-exception trie data bits 6..5
        moved here because the exception index needs more bits than the delta
        0 normal character with cc=0
        1 soft-dotted character
        2 cc=230
        3 other cc
    11  same as non-exception case-sensitive bit
    10  the delta in the optional value slot is negative
     9  no simple case folding, even if there is a simple lowercase mapping
     8  if set, then for each optional-value slot there are 2 uint16_t values
        (high and low parts of 32-bit values)
        instead of single ones
 7.. 0  bits for which optional value is present

Optional-value slots:
0   lowercase mapping (code point)
1   case folding (code point)
2   uppercase mapping (code point)
3   titlecase mapping (code point)
4   delta to simple case mapping code point
    (add delta to input code point, or subtract if excWord bit 10 is set)
5   reserved
6   closure mappings (new in format version 1.1)
7   there is at least one full (string) case mapping
    the length of each is encoded in a nibble of this optional value,
    and the strings follow this optional value in the same order:
    lower/fold/upper/title

The optional closure mappings value is used as follows:
Bits 0..3 contain the length of a string of code points for case closure.
The string immediately follows the full case mappings, or the closure value
slot if there are no full case mappings.
Bits 4..15 are reserved and could be used in the future to indicate the
number of strings for case closure.
Complete case closure for a code point is given by the union of all simple
and full case mappings and foldings, plus the case closure code points
(and potentially, in the future, case closure strings).

For space saving, some values are not stored. Lookups are as follows:
- If special casing is conditional, then no full lower/upper/title mapping
  strings are stored.
- If case folding is conditional, then no simple or full case foldings are
  stored.
- Fall back in this order:
    full (string) mapping -- if full mappings are used
    simple (code point) mapping of the same type
    simple fold->simple lower
    simple title->simple upper
    finally, the original code point (no mapping)

This fallback order is strict:
In particular, the fallback from full case folding is to simple case folding,
not to full lowercase mapping.

Reverse case folding data ("unfold") array: (new in format version 1.1)

This array stores some miscellaneous values followed by a table. The data maps
back from multi-character strings to their original code points, for use
in case closure.

The table contains two columns of strings.
The string in the first column is the case folding of each of the code points
in the second column. The strings are terminated with NUL or by the end of the
column, whichever comes first.

The miscellaneous data takes up one pseudo-row and includes:
- number of rows
- number of UChars per row
- number of UChars in the left (folding string) column

The table is sorted by its first column. Values in the first column are unique.

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

    /* dataFormat="cAsE" */
    { UCASE_FMT_0, UCASE_FMT_1, UCASE_FMT_2, UCASE_FMT_3 },
    { 4, 0, 0, 0 },  /* formatVersion */
    { 11, 0, 0, 0 }  /* dataVersion */
};

#define UGENCASE_EXC_SHIFT     20
#define UGENCASE_EXC_MASK      0xfff00000

enum {
    MAX_EXC_COUNT=(UGENCASE_EXC_MASK>>UGENCASE_EXC_SHIFT)+1
};

struct ExcProps {
    ExcProps() :
            delta(0), hasConditionalCaseMappings(FALSE), hasTurkicCaseFolding(FALSE),
            hasNoSimpleCaseFolding(FALSE) {}
    ExcProps(const UniProps &otherProps) :
            props(otherProps),
            delta(0), hasConditionalCaseMappings(FALSE), hasTurkicCaseFolding(FALSE),
            hasNoSimpleCaseFolding(FALSE) {}

    UniProps props;
    UnicodeSet closure;
    int32_t delta;
    UBool hasConditionalCaseMappings;
    UBool hasTurkicCaseFolding;
    UBool hasNoSimpleCaseFolding;
};

/*
 * Values for the ucase.icu unfold[] data array.
 * The values are stored in ucase.icu so that the runtime code will work with
 * changing values, but they are hardcoded here for simplicity.
 * They are optimized, that is, provide for minimal table column widths,
 * for the actual Unicode data, so that the table size is minimized.
 * Future versions of Unicode may require increases of some of these values.
 */
enum {
    UGENCASE_UNFOLD_STRING_WIDTH=3,
    UGENCASE_UNFOLD_CP_WIDTH=2,
    UGENCASE_UNFOLD_WIDTH=UGENCASE_UNFOLD_STRING_WIDTH+UGENCASE_UNFOLD_CP_WIDTH
};

class CasePropsBuilder : public PropsBuilder {
public:
    CasePropsBuilder(UErrorCode &errorCode);
    virtual ~CasePropsBuilder();

    virtual void setUnicodeVersion(const UVersionInfo version);
    virtual void setProps(const UniProps &, const UnicodeSet &newValues, UErrorCode &errorCode);
    virtual void build(UErrorCode &errorCode);
    virtual void writeCSourceFile(const char *path, UErrorCode &errorCode);
    virtual void writeBinaryData(const char *path, UBool withCopyright, UErrorCode &errorCode);

private:
    uint32_t makeExcProps(UChar32 c, uint32_t value, UErrorCode &errorCode);
    void addUnfolding(UChar32 c, const UnicodeString &s, UErrorCode &errorCode);
    void makeUnfoldData(UErrorCode &errorCode);
    void addClosureMapping(UChar32 src, UChar32 dest, UErrorCode &errorCode);
    UBool addClosure(UChar32 orig, UChar32 prev2, UChar32 prev, UChar32 c, uint32_t value,
                     UErrorCode &errorCode);
    void makeCaseClosure(UErrorCode &errorCode);
    int32_t makeException(UChar32 c, uint32_t value, ExcProps &ep, UErrorCode &errorCode);
    void makeExceptions(UErrorCode &errorCode);

    UnicodeSet relevantProps;
    /*
     * Unicode set collecting the case-sensitive characters;
     * see uchar.h UCHAR_CASE_SENSITIVE.
     * Add code points from case mappings/foldings in
     * the root locale and with default options.
     */
    UnicodeSet caseSensitive;
    /* reverse case folding ("unfold") data */
    UnicodeString unfold;
    UnicodeString exceptions;
    ExcProps **excProps;
    int32_t excPropsCount;
    /* becomes indexes[UCASE_IX_MAX_FULL_LENGTH] */
    int32_t maxFullLength;
    UTrie2 *pTrie;
};

CasePropsBuilder::CasePropsBuilder(UErrorCode &errorCode)
        : excProps(NULL), excPropsCount(0), maxFullLength(U16_MAX_LENGTH), pTrie(NULL) {
    // This builder encodes the following properties.
    relevantProps.
        add(UCHAR_CANONICAL_COMBINING_CLASS).  // 0 vs. 230 vs. other
        add(UCHAR_SOFT_DOTTED).
        add(UCHAR_LOWERCASE).
        add(UCHAR_UPPERCASE).
        add(UCHAR_CASE_IGNORABLE).
        add(UCHAR_SIMPLE_CASE_FOLDING).
        add(UCHAR_SIMPLE_LOWERCASE_MAPPING).
        add(UCHAR_SIMPLE_TITLECASE_MAPPING).
        add(UCHAR_SIMPLE_UPPERCASE_MAPPING).
        add(UCHAR_CASE_FOLDING).
        add(UCHAR_LOWERCASE_MAPPING).
        add(UCHAR_TITLECASE_MAPPING).
        add(UCHAR_UPPERCASE_MAPPING).
        add(PPUCD_CONDITIONAL_CASE_MAPPINGS).
        add(PPUCD_TURKIC_CASE_FOLDING);
    // Write "unfold" meta data into the first row. Must be UGENCASE_UNFOLD_WIDTH UChars.
    unfold.
        append(0).
        append((UChar)UGENCASE_UNFOLD_WIDTH).
        append((UChar)UGENCASE_UNFOLD_STRING_WIDTH).
        append(0).
        append(0);
    U_ASSERT(unfold.length()==UGENCASE_UNFOLD_WIDTH);
    pTrie=utrie2_open(0, 0, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: casepropsbuilder utrie2_open() failed - %s\n",
                u_errorName(errorCode));
        return;
    }
    excProps=new ExcProps *[MAX_EXC_COUNT];
    if(excProps==NULL) {
        fprintf(stderr,
                "genprops error: casepropsbuilder out of memory allocating "
                "the array of exceptions properties\n");
        errorCode=U_MEMORY_ALLOCATION_ERROR;
    }
}

CasePropsBuilder::~CasePropsBuilder() {
    utrie2_close(pTrie);
    for(int32_t i=0; i<excPropsCount; ++i) {
        delete excProps[i];
    }
    delete[] excProps;
}

void
CasePropsBuilder::setUnicodeVersion(const UVersionInfo version) {
    uprv_memcpy(dataInfo.dataVersion, version, 4);
}

/* -------------------------------------------------------------------------- */

void
CasePropsBuilder::addUnfolding(UChar32 c, const UnicodeString &s, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }

    int32_t length=s.length();
    if(length>UGENCASE_UNFOLD_STRING_WIDTH) {
        fprintf(stderr, "genprops error: case folding too long (length=%ld>%d=UGENCASE_UNFOLD_STRING_WIDTH)\n",
                (long)length, UGENCASE_UNFOLD_STRING_WIDTH);
        errorCode=U_INTERNAL_PROGRAM_ERROR;
    }
    unfold.append(s);
    while(length<UGENCASE_UNFOLD_STRING_WIDTH) {
        unfold.append(0);
        ++length;
    }

    unfold.append(c);
    if(U16_LENGTH(c)<UGENCASE_UNFOLD_CP_WIDTH) {
        unfold.append(0);
    }

    U_ASSERT((unfold.length()%UGENCASE_UNFOLD_WIDTH)==0);
}

/* store a character's properties ------------------------------------------- */

void
CasePropsBuilder::setProps(const UniProps &props, const UnicodeSet &newValues,
                           UErrorCode &errorCode) {
    if(U_FAILURE(errorCode) || newValues.containsNone(relevantProps)) { return; }

    UChar32 start=props.start;
    UChar32 end=props.end;

    /* default: map to self */
    int32_t delta=0;
    UBool noDelta=FALSE;

    uint32_t type;
    if(props.binProps[UCHAR_LOWERCASE]) {
        type=UCASE_LOWER;
    } else if(props.binProps[UCHAR_UPPERCASE]) {
        type=UCASE_UPPER;
    } else if(props.getIntProp(UCHAR_GENERAL_CATEGORY)==U_TITLECASE_LETTER) {
        type=UCASE_TITLE;
    } else {
        type=UCASE_NONE;
    }
    uint32_t value=type;

    // Examine simple case mappings.
    UBool hasMapping=FALSE;
    if(props.suc>=0) {
        /* uppercase mapping as delta if the character is lowercase */
        hasMapping=TRUE;
        if(type==UCASE_LOWER) {
            delta=props.suc-start;
        } else {
            noDelta=TRUE;
            value|=UCASE_EXCEPTION;
        }
    }
    if(props.slc>=0) {
        /* lowercase mapping as delta if the character is uppercase or titlecase */
        hasMapping=TRUE;
        if(type>=UCASE_UPPER) {
            delta=props.slc-start;
        } else {
            noDelta=TRUE;
            value|=UCASE_EXCEPTION;
        }
    }
    if(props.stc>=0) {
        hasMapping=TRUE;
    }
    if(props.suc!=props.stc) {
        noDelta=TRUE;
        value|=UCASE_EXCEPTION;
    }

    // Simple case folding falls back to simple lowercasing.
    // If they differ, then store them separately.
    UChar32 scf=props.scf;
    if(scf>=0 && scf!=props.slc) {
        hasMapping=noDelta=TRUE;
        value|=UCASE_EXCEPTION;
    }

    // If there is no case folding but there is a lowercase mapping,
    // then set a bit for that.
    // For example: Cherokee uppercase syllables since Unicode 8.
    // (Full case folding falls back to simple case folding,
    // not to full lowercasing, so we need not also handle it specially
    // for such cases.)
    UBool hasNoSimpleCaseFolding=FALSE;
    if(scf<0 && props.slc>=0) {
        hasNoSimpleCaseFolding=TRUE;
        value|=UCASE_EXCEPTION;
    }

    if(noDelta) {
        delta=0;
    } else if(delta<UCASE_MIN_DELTA || UCASE_MAX_DELTA<delta) {
        // The case mapping delta is too big for the main data word.
        // Store it in an exceptions slot.
        value|=UCASE_EXCEPTION;
    }

    // Examine full case mappings.
    if(!props.lc.isEmpty() || !props.uc.isEmpty() || !props.tc.isEmpty() ||
        newValues.contains(PPUCD_CONDITIONAL_CASE_MAPPINGS)
    ) {
        hasMapping=TRUE;
        value|=UCASE_EXCEPTION;
    }
    if( (!props.cf.isEmpty() && props.cf!=UnicodeString(props.scf)) ||
        newValues.contains(PPUCD_TURKIC_CASE_FOLDING)
    ) {
        hasMapping=TRUE;
        value|=UCASE_EXCEPTION;
    }

    if(props.binProps[UCHAR_SOFT_DOTTED]) {
        value|=UCASE_SOFT_DOTTED;
    }
    int32_t cc=props.getIntProp(UCHAR_CANONICAL_COMBINING_CLASS);
    if(cc!=0) {
        if(props.binProps[UCHAR_SOFT_DOTTED]) {
            fprintf(stderr, "genprops error: a soft-dotted character has ccc!=0\n");
            errorCode=U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        if(cc==230) {
            value|=UCASE_ABOVE;
        } else {
            value|=UCASE_OTHER_ACCENT;
        }
    }

    if(props.binProps[UCHAR_CASE_IGNORABLE]) {
        value|=UCASE_IGNORABLE;
    }

    if((hasMapping || (value&UCASE_EXCEPTION)) && start!=end) {
        fprintf(stderr,
                "genprops error: range %04lX..%04lX has case mappings "
                "or reasons for data structure exceptions\n",
                (long)start, (long)end);
        errorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    /* handle exceptions */
    if(value&UCASE_EXCEPTION) {
        /* simply store exceptions for later processing and encoding */
        if(excPropsCount==MAX_EXC_COUNT) {
            fprintf(stderr, "genprops error: casepropsbuilder: too many exceptions\n");
            errorCode=U_INDEX_OUTOFBOUNDS_ERROR;
            return;
        }
        ExcProps *newExcProps=new ExcProps(props);
        if(newExcProps==NULL) {
            fprintf(stderr,
                    "genprops error: casepropsbuilder out of memory allocating "
                    "exceptions properties\n");
            errorCode=U_MEMORY_ALLOCATION_ERROR;
            return;
        }
        newExcProps->props.scf=scf;
        newExcProps->delta=delta;
        newExcProps->hasConditionalCaseMappings=
            newValues.contains(PPUCD_CONDITIONAL_CASE_MAPPINGS) ||
            // See ICU-13416: և ligature ech-yiwn has language-specific
            // uppercase and titlecase mappings.
            start==0x0587;
        newExcProps->hasTurkicCaseFolding=newValues.contains(PPUCD_TURKIC_CASE_FOLDING);
        newExcProps->hasNoSimpleCaseFolding=hasNoSimpleCaseFolding;
        value|=(uint32_t)excPropsCount<<UGENCASE_EXC_SHIFT;
        excProps[excPropsCount++]=newExcProps;
    } else {
        /* store the simple case mapping delta */
        value|=((uint32_t)delta<<UCASE_DELTA_SHIFT)&UCASE_DELTA_MASK;
    }

    utrie2_setRange32(pTrie, start, end, value, TRUE, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: unable to set case mapping values: %s\n",
                u_errorName(errorCode));
        return;
    }

    if(hasMapping) {
        /* update the case-sensitive set */
        caseSensitive.add(start);
        if(scf>=0) { caseSensitive.add(scf); }
        if(props.slc>=0) { caseSensitive.add(props.slc); }
        if(props.suc>=0) { caseSensitive.add(props.suc); }
        if(props.stc>=0) { caseSensitive.add(props.stc); }
        caseSensitive.addAll(props.cf);
        caseSensitive.addAll(props.lc);
        caseSensitive.addAll(props.uc);
        caseSensitive.addAll(props.tc);

        /* update maxFullLength */
        if(props.cf.length()>maxFullLength) { maxFullLength=props.cf.length(); }
        if(props.lc.length()>maxFullLength) { maxFullLength=props.lc.length(); }
        if(props.uc.length()>maxFullLength) { maxFullLength=props.uc.length(); }
        if(props.tc.length()>maxFullLength) { maxFullLength=props.tc.length(); }
    }

    /* add the multi-character case folding to the "unfold" data */
    if(props.cf.hasMoreChar32Than(0, 0x7fffffff, 1)) {
        addUnfolding(start, props.cf, errorCode);
    }
}

uint32_t
CasePropsBuilder::makeExcProps(UChar32 c, uint32_t value, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return 0; }
    if(excPropsCount==MAX_EXC_COUNT) {
        fprintf(stderr, "genprops error: casepropsbuilder: too many exceptions\n");
        errorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        return 0;
    }
    LocalPointer<ExcProps> newExcProps(new ExcProps);
    if(newExcProps==NULL) {
        fprintf(stderr,
                "genprops error: casepropsbuilder out of memory allocating "
                "exceptions properties\n");
        errorCode=U_MEMORY_ALLOCATION_ERROR;
        return 0;
    }

    if((value&UCASE_TYPE_MASK)>UCASE_NONE) {
        // Decode the simple case mapping.
        UChar32 next=c+UCASE_GET_DELTA(value);
        if(next!=c) {
            UniProps &p=newExcProps->props;
            if((value&UCASE_TYPE_MASK)==UCASE_LOWER) {
                p.suc=p.stc=next;
            } else {
                p.slc=next;
            }
        }
    }

    value&=~(UGENCASE_EXC_MASK|UCASE_DELTA_MASK); // remove previous simple mapping
    value|=(uint32_t)excPropsCount<<UGENCASE_EXC_SHIFT;
    value|=UCASE_EXCEPTION;
    excProps[excPropsCount++]=newExcProps.orphan();
    return value;
}

/* finalize reverse case folding ("unfold") data ---------------------------- */

static int32_t U_CALLCONV
compareUnfold(const void *context, const void *left, const void *right) {
    return u_memcmp((const UChar *)left, (const UChar *)right, UGENCASE_UNFOLD_WIDTH);
}

void
CasePropsBuilder::makeUnfoldData(UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }

    UChar *p, *q;
    int32_t i, j, k;

    /* sort the data */
    int32_t unfoldLength=unfold.length();
    int32_t unfoldRows=unfoldLength/UGENCASE_UNFOLD_WIDTH-1;
    UChar *unfoldBuffer=unfold.getBuffer(-1);
    uprv_sortArray(unfoldBuffer+UGENCASE_UNFOLD_WIDTH, unfoldRows, UGENCASE_UNFOLD_WIDTH*2,
                   compareUnfold, NULL, FALSE, &errorCode);

    /* make unique-string rows by merging adjacent ones' code point columns */

    /* make p point to row i-1 */
    p=unfoldBuffer+UGENCASE_UNFOLD_WIDTH;

    for(i=1; i<unfoldRows;) {
        if(0==u_memcmp(p, p+UGENCASE_UNFOLD_WIDTH, UGENCASE_UNFOLD_STRING_WIDTH)) {
            /* concatenate code point columns */
            q=p+UGENCASE_UNFOLD_STRING_WIDTH;
            for(j=1; j<UGENCASE_UNFOLD_CP_WIDTH && q[j]!=0; ++j) {}
            for(k=0; k<UGENCASE_UNFOLD_CP_WIDTH && q[UGENCASE_UNFOLD_WIDTH+k]!=0; ++j, ++k) {
                q[j]=q[UGENCASE_UNFOLD_WIDTH+k];
            }
            if(j>UGENCASE_UNFOLD_CP_WIDTH) {
                fprintf(stderr, "genprops error: too many code points in unfold[]: %ld>%d=UGENCASE_UNFOLD_CP_WIDTH\n",
                        (long)j, UGENCASE_UNFOLD_CP_WIDTH);
                errorCode=U_BUFFER_OVERFLOW_ERROR;
                return;
            }

            /* move following rows up one */
            --unfoldRows;
            u_memmove(p+UGENCASE_UNFOLD_WIDTH, p+UGENCASE_UNFOLD_WIDTH*2, (unfoldRows-i)*UGENCASE_UNFOLD_WIDTH);
        } else {
            p+=UGENCASE_UNFOLD_WIDTH;
            ++i;
        }
    }

    unfoldBuffer[UCASE_UNFOLD_ROWS]=(UChar)unfoldRows;

    if(beVerbose) {
        puts("unfold data:");

        p=unfoldBuffer;
        for(i=0; i<unfoldRows; ++i) {
            p+=UGENCASE_UNFOLD_WIDTH;
            printf("[%2d] %04x %04x %04x <- %04x %04x\n",
                   (int)i, p[0], p[1], p[2], p[3], p[4]);
        }
    }

    unfold.releaseBuffer((unfoldRows+1)*UGENCASE_UNFOLD_WIDTH);
}

/* case closure ------------------------------------------------------------- */

void
CasePropsBuilder::addClosureMapping(UChar32 src, UChar32 dest, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }

    if(beVerbose) {
        printf("add closure mapping U+%04lx->U+%04lx\n",
                (unsigned long)src, (unsigned long)dest);
    }

    uint32_t value=utrie2_get32(pTrie, src);
    if((value&UCASE_EXCEPTION)==0) {
        /*
         * decode value into p2 (enough for makeException() to work properly),
         * add the closure mapping,
         * and set the new exception for src
         */
        value=makeExcProps(src, value, errorCode);
        utrie2_set32(pTrie, src, value, &errorCode);
        if(U_FAILURE(errorCode)) {
            fprintf(stderr, "genprops error: unable to set case mapping values, code: %s\n",
                            u_errorName(errorCode));
            return;
        }
    }
    excProps[value>>UGENCASE_EXC_SHIFT]->closure.add(dest);
}

/*
 * Find missing case mapping relationships and add mappings for case closure.
 * This function starts from an "original" code point and recursively
 * finds its case mappings and the case mappings of where it maps to.
 *
 * The recursion depth is capped at 3 nested calls of this function.
 * In each call, the current code point is c, and the function enumerates
 * all of c's simple (single-code point) case mappings.
 * prev is the code point that case-mapped to c.
 * prev2 is the code point that case-mapped to prev.
 *
 * The initial function call has prev2<0, prev<0, and c==orig
 * (marking no code points).
 * It enumerates c's case mappings and recurses without further action.
 *
 * The second-level function call has prev2<0, prev==orig, and c is
 * the destination code point of one of prev's case mappings.
 * The function checks if any of c's case mappings go back to orig
 * and adds a closure mapping if not.
 * In other words, it turns a case mapping relationship of
 *   orig->c
 * into
 *   orig<->c
 *
 * The third-level function call has prev2==orig, prev>=0, and c is
 * the destination code point of one of prev's case mappings.
 * (And prev is the destination of one of prev2's case mappings.)
 * The function checks if any of c's case mappings go back to orig
 * and adds a closure mapping if not.
 * In other words, it turns case mapping relationships of
 *   orig->prev->c or orig->prev<->c
 * into
 *   orig->prev->c->orig or orig->prev<->c->orig
 * etc.
 * (Graphically, this closes a triangle.)
 *
 * With repeated application on all code points until no more closure mappings
 * are added, all case equivalence groups get complete mappings.
 * That is, in each group of code points with case relationships
 * each code point will in the end have some mapping to each other
 * code point in the group.
 *
 * @return TRUE if a closure mapping was added
 */
UBool
CasePropsBuilder::addClosure(UChar32 orig, UChar32 prev2, UChar32 prev, UChar32 c, uint32_t value,
                             UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return FALSE; }

    UChar32 next;
    UBool someMappingsAdded=FALSE;

    if(c!=orig) {
        /* get the properties for c */
        value=utrie2_get32(pTrie, c);
    }
    /* else if c==orig then c's value was passed in */

    if(value&UCASE_EXCEPTION) {
        UnicodeSet set;

        ExcProps &ep=*excProps[value>>UGENCASE_EXC_SHIFT];
        UniProps &p=ep.props;

        /*
         * marker for whether any of c's mappings goes to orig
         * c==orig: prevent adding a closure mapping when getting orig's own, direct mappings
         */
        UBool mapsToOrig=(UBool)(c==orig);

        /* collect c's case mapping destinations in set[] */
        if((next=p.suc)>=0 && next!=c) {
            set.add(next);
        }
        if((next=p.slc)>=0 && next!=c) {
            set.add(next);
        }
        if(p.suc!=(next=p.stc) && next!=c) {
            set.add(next);
        }
        if((next=p.scf)>=0 && next!=c) {
            set.add(next);
        }

        /* add c's current closure mappings to set */
        set.addAll(ep.closure);

        /* process all code points to which c case-maps */
        UnicodeSetIterator iter(set);
        while(iter.next()) {
            next=iter.getCodepoint(); /* next!=c */

            if(next==orig) {
                mapsToOrig=TRUE; /* remember that we map to orig */
            } else if(prev2<0 && next!=prev) {
                /*
                 * recurse unless
                 * we have reached maximum depth (prev2>=0) or
                 * this is a mapping to one of the previous code points (orig, prev, c)
                 */
                someMappingsAdded|=addClosure(orig, prev, c, next, 0, errorCode);
            }
        }

        if(!mapsToOrig) {
            addClosureMapping(c, orig, errorCode);
            return TRUE;
        }
    } else {
        if((value&UCASE_TYPE_MASK)>UCASE_NONE) {
            /* one simple case mapping, don't care which one */
            next=c+UCASE_GET_DELTA(value);
            if(next!=c) {
                /*
                 * recurse unless
                 * we have reached maximum depth (prev2>=0) or
                 * this is a mapping to one of the previous code points (orig, prev, c)
                 */
                if(prev2<0 && next!=orig && next!=prev) {
                    someMappingsAdded|=addClosure(orig, prev, c, next, 0, errorCode);
                }

                if(c!=orig && next!=orig) {
                    /* c does not map to orig, add a closure mapping c->orig */
                    addClosureMapping(c, orig, errorCode);
                    return TRUE;
                }
            }
        }
    }

    return someMappingsAdded;
}

void
CasePropsBuilder::makeCaseClosure(UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }

    /*
     * finalize the "unfold" data because we need to use it to add closure mappings
     * for situations like FB05->"st"<-FB06
     * where we would otherwise miss the FB05<->FB06 relationship
     */
    makeUnfoldData(errorCode);

    /* use the "unfold" data to add mappings */

    /* p always points to the code points; this loop ignores the strings completely */
    const UChar *p=unfold.getBuffer()+UGENCASE_UNFOLD_WIDTH+UGENCASE_UNFOLD_STRING_WIDTH;
    int32_t unfoldRows=unfold.length()/UGENCASE_UNFOLD_WIDTH-1;

    for(int32_t i=0; i<unfoldRows; p+=UGENCASE_UNFOLD_WIDTH, ++i) {
        int32_t j=0;
        UChar32 c;
        U16_NEXT_UNSAFE(p, j, c);
        while(j<UGENCASE_UNFOLD_CP_WIDTH && p[j]!=0) {
            UChar32 c2;
            U16_NEXT_UNSAFE(p, j, c2);
            addClosure(c, U_SENTINEL, c, c2, 0, errorCode);
        }
    }

    if(beVerbose) {
        puts("---- ---- ---- ---- (done with closures from unfolding)");
    }

    /* add further closure mappings from analyzing simple mappings */
    UBool someMappingsAdded;
    do {
        someMappingsAdded=FALSE;

        for(UChar32 c=0; c<=0x10ffff; ++c) {
            uint32_t value=utrie2_get32(pTrie, c);
            if(value!=0) {
                someMappingsAdded|=addClosure(c, U_SENTINEL, U_SENTINEL, c, value, errorCode);
            }
        }

        if(beVerbose && someMappingsAdded) {
            puts("---- ---- ---- ----");
        }
    } while(someMappingsAdded);
}

/* exceptions --------------------------------------------------------------- */

static UBool
fullMappingEqualsSimple(const UnicodeString &s, UChar32 simple, UChar32 c) {
    if(simple<=0) {
        simple=c; /* UCD has no simple mapping if it's the same as the code point itself */
    }
    return s.length()==U16_LENGTH(simple) && s.char32At(0)==simple;
}

int32_t
CasePropsBuilder::makeException(UChar32 c, uint32_t value, ExcProps &ep, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return 0; }

    /* exceptions.length() might be returned for storing in the trie word */
    if(exceptions.length()>=UCASE_MAX_EXCEPTIONS) {
        fprintf(stderr, "genprops error: casepropsbuilder: too many exceptions words\n");
        errorCode=U_BUFFER_OVERFLOW_ERROR;
        return 0;
    }

    /* copy and shift the soft-dotted and case-sensitive bits */
    UChar excWord=(UChar)((value&(UCASE_DOT_MASK|UCASE_SENSITIVE))<<UCASE_EXC_DOT_SHIFT);

    UniProps &p=ep.props;

    /* set the bits for conditional mappings */
    if(ep.hasConditionalCaseMappings) {
        excWord|=UCASE_EXC_CONDITIONAL_SPECIAL;
        p.lc.remove();
        p.uc.remove();
        p.tc.remove();
    }
    if(ep.hasTurkicCaseFolding) {
        excWord|=UCASE_EXC_CONDITIONAL_FOLD;
        p.cf.remove();
    }
    if(ep.hasNoSimpleCaseFolding) {
        excWord|=UCASE_EXC_NO_SIMPLE_CASE_FOLDING;
    }

    /* remove redundant data */
    /* do not store full mappings if they are the same as the simple ones */
    if(fullMappingEqualsSimple(p.lc, p.slc, c)) {
        p.lc.remove();
    }
    if(fullMappingEqualsSimple(p.uc, p.suc, c)) {
        p.uc.remove();
    }
    if(fullMappingEqualsSimple(p.tc, p.stc, c)) {
        p.tc.remove();
    }
    if(fullMappingEqualsSimple(p.cf, p.scf, c)) {
        p.cf.remove();
    }

    /* write the optional slots */
    uint32_t slots[8];
    uint32_t slotBits=0;
    int32_t count=0;

    if(ep.delta!=0) {
        int32_t delta=ep.delta;
        if(delta<0) {
            excWord|=UCASE_EXC_DELTA_IS_NEGATIVE;
            delta=-delta;
        }
        slots[count]=(uint32_t)delta;
        slotBits|=slots[count];
        ++count;
        excWord|=U_MASK(UCASE_EXC_DELTA);
    } else {
        if(p.slc>=0) {
            slots[count]=(uint32_t)p.slc;
            slotBits|=slots[count];
            ++count;
            excWord|=U_MASK(UCASE_EXC_LOWER);
        }
        if( p.scf>=0 &&
                (p.slc>=0 ?
                    p.scf!=p.slc :
                    p.scf!=c)) {
            slots[count]=(uint32_t)p.scf;
            slotBits|=slots[count];
            ++count;
            excWord|=U_MASK(UCASE_EXC_FOLD);
        }
        if(p.suc>=0) {
            slots[count]=(uint32_t)p.suc;
            slotBits|=slots[count];
            ++count;
            excWord|=U_MASK(UCASE_EXC_UPPER);
        }
        if(p.suc!=p.stc) {
            if(p.stc>=0) {
                slots[count]=(uint32_t)p.stc;
            } else {
                slots[count]=(uint32_t)c;
            }
            slotBits|=slots[count];
            ++count;
            excWord|=U_MASK(UCASE_EXC_TITLE);
        }
    }

    /* length of case closure */
    UnicodeString closureString;
    if(!ep.closure.isEmpty()) {
        UnicodeSetIterator iter(ep.closure);
        while(iter.next()) { closureString.append(iter.getCodepoint()); }
        int32_t length=closureString.length();
        if(length>UCASE_CLOSURE_MAX_LENGTH) {
            fprintf(stderr,
                    "genprops error: case closure for U+%04lX has length %d "
                    "which exceeds UCASE_CLOSURE_MAX_LENGTH=%d\n",
                    (long)c, (int)length, (int)UCASE_CLOSURE_MAX_LENGTH);
            errorCode=U_BUFFER_OVERFLOW_ERROR;
            return 0;
        }
        slots[count]=(uint32_t)length; /* must be 1..UCASE_CLOSURE_MAX_LENGTH */
        slotBits|=slots[count];
        ++count;
        excWord|=U_MASK(UCASE_EXC_CLOSURE);
    }

    /* lengths of full case mapping strings, stored in the last slot */
    int32_t fullLengths=
        p.lc.length()|
        (p.cf.length()<<4)|
        (p.uc.length()<<8)|
        (p.tc.length()<<12);
    if(fullLengths!=0) {
        slots[count]=(uint32_t)fullLengths;
        slotBits|=slots[count];
        ++count;
        excWord|=U_MASK(UCASE_EXC_FULL_MAPPINGS);
    }

    if(count==0) {
        /* No optional slots: Try to share excWord entries. */
        int32_t excIndex=exceptions.indexOf((UChar)excWord);
        if(excIndex>=0) {
            return excIndex;
        }
        /* not found */
        excIndex=exceptions.length();
        exceptions.append((UChar)excWord);
        return excIndex;
    } else {
        /* write slots */
        UnicodeString excString;
        excString.append((UChar)0);  /* placeholder for excWord which will be stored at excIndex */

        if(slotBits<=0xffff) {
            for(int32_t i=0; i<count; ++i) {
                excString.append((UChar)slots[i]);
            }
        } else {
            excWord|=UCASE_EXC_DOUBLE_SLOTS;
            for(int32_t i=0; i<count; ++i) {
                excString.append((UChar)(slots[i]>>16));
                excString.append((UChar)slots[i]);
            }
        }

        /* write the full case mapping strings */
        excString.append(p.lc);
        excString.append(p.cf);
        excString.append(p.uc);
        excString.append(p.tc);

        /* write the closure data */
        excString.append(closureString);

        /* write the main exceptions word */
        excString.setCharAt(0, (UChar)excWord);

        // Try to share data.
        if(count==1 && ep.delta!=0) {
            int32_t excIndex=exceptions.indexOf(excString);
            if(excIndex>=0) {
                return excIndex;
            }
        }
        int32_t excIndex=exceptions.length();
        exceptions.append(excString);
        return excIndex;
    }
}

void
CasePropsBuilder::makeExceptions(UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }

    /*
     * Encode case-ignorable as delta==1 on uncased characters,
     * and with an exception bit on cased characters and characters with another exception.
     *
     * Change from temporary UGENCASE_EXC_SHIFT'ed index into excProps[]
     * to UCASE_EXC_SHIFT'ed index into encoded exceptions[].
     */
    for(UChar32 c=0; c<=0x10ffff; ++c) {
        uint32_t value=utrie2_get32(pTrie, c);
        if(value&UCASE_EXCEPTION) {
            int32_t excIndex=makeException(c, value, *excProps[value>>UGENCASE_EXC_SHIFT], errorCode);
            value=(value&~(UGENCASE_EXC_MASK|UCASE_EXC_MASK))|((uint32_t)excIndex<<UCASE_EXC_SHIFT);
            utrie2_set32(pTrie, c, value, &errorCode);
        }
    }
}

/* generate output data ----------------------------------------------------- */

static int32_t indexes[UCASE_IX_TOP]={
    UCASE_IX_TOP, 0, 0, 0,
    0, 0, 0, 0,
    0, 0, 0, 0,
    0, 0, 0, 0
};

static uint8_t trieBlock[100000];
static int32_t trieSize;

void
CasePropsBuilder::build(UErrorCode &errorCode) {
    if(!beQuiet) {
        puts("* ucase.icu stats *");
    }

    makeCaseClosure(errorCode);
    if(U_FAILURE(errorCode)) { return; }

    /*
     * Add one complex mapping to caseSensitive that was filtered out above:
     * Greek final Sigma has a conditional mapping but not locale-sensitive,
     * and it is taken when lowercasing just U+03A3 alone.
     * 03A3; 03C2; 03A3; 03A3; Final_Sigma; # GREEK CAPITAL LETTER SIGMA
     */
    caseSensitive.add(0x3c2);

    UnicodeSetIterator iter(caseSensitive);
    while(iter.next()) {
        UChar32 c=iter.getCodepoint();
        uint32_t value=utrie2_get32(pTrie, c);
        if((value&UCASE_SENSITIVE)==0) {
            utrie2_set32(pTrie, c, value|UCASE_SENSITIVE, &errorCode);
        }
    }
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops/case error: unable to set UCASE_SENSITIVE: %s\n",
                u_errorName(errorCode));
        return;
    }

    makeExceptions(errorCode);
    if(U_FAILURE(errorCode)) { return; }

    utrie2_freeze(pTrie, UTRIE2_16_VALUE_BITS, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops/case error: utrie2_freeze() failed: %s\n",
                u_errorName(errorCode));
        return;
    }
    trieSize=utrie2_serialize(pTrie, trieBlock, sizeof(trieBlock), &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops/case error: utrie2_serialize() failed: %s (length %ld)\n",
                u_errorName(errorCode), (long)trieSize);
        return;
    }

    indexes[UCASE_IX_EXC_LENGTH]=exceptions.length();
    indexes[UCASE_IX_TRIE_SIZE]=trieSize;
    indexes[UCASE_IX_UNFOLD_LENGTH]=unfold.length();
    indexes[UCASE_IX_LENGTH]=(int32_t)sizeof(indexes)+trieSize+2*exceptions.length()+2*unfold.length();

    indexes[UCASE_IX_MAX_FULL_LENGTH]=maxFullLength;

    if(!beQuiet) {
        printf("trie size in bytes:                    %5d\n", (int)trieSize);
        printf("number of code points with exceptions: %5d\n", excPropsCount);
        printf("size in bytes of exceptions:           %5d\n", 2*exceptions.length());
        printf("size in bytes of reverse foldings:     %5d\n", 2*unfold.length());
        printf("data size:                             %5d\n", (int)indexes[UCASE_IX_LENGTH]);
    }
}

void
CasePropsBuilder::writeCSourceFile(const char *path, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }

    FILE *f=usrc_create(path, "ucase_props_data.h", 2016,
                        "icu/tools/unicode/c/genprops/casepropsbuilder.cpp");
    if(f==NULL) {
        errorCode=U_FILE_ACCESS_ERROR;
        return;
    }
    fputs("#ifdef INCLUDED_FROM_UCASE_CPP\n\n", f);
    usrc_writeArray(f,
        "static const UVersionInfo ucase_props_dataVersion={",
        dataInfo.dataVersion, 8, 4,
        "};\n\n");
    usrc_writeArray(f,
        "static const int32_t ucase_props_indexes[UCASE_IX_TOP]={",
        indexes, 32, UCASE_IX_TOP,
        "};\n\n");
    usrc_writeUTrie2Arrays(f,
        "static const uint16_t ucase_props_trieIndex[%ld]={\n", NULL,
        pTrie,
        "\n};\n\n");
    usrc_writeArray(f,
        "static const uint16_t ucase_props_exceptions[%ld]={\n",
        exceptions.getBuffer(), 16, exceptions.length(),
        "\n};\n\n");
    usrc_writeArray(f,
        "static const uint16_t ucase_props_unfold[%ld]={\n",
        unfold.getBuffer(), 16, unfold.length(),
        "\n};\n\n");
    fputs(
        "static const UCaseProps ucase_props_singleton={\n"
        "  NULL,\n"
        "  ucase_props_indexes,\n"
        "  ucase_props_exceptions,\n"
        "  ucase_props_unfold,\n",
        f);
    usrc_writeUTrie2Struct(f,
        "  {\n",
        pTrie, "ucase_props_trieIndex", NULL,
        "  },\n");
    usrc_writeArray(f, "  { ", dataInfo.formatVersion, 8, 4, " }\n");
    fputs("};\n\n"
          "#endif  // INCLUDED_FROM_UCASE_CPP\n", f);
    fclose(f);
}

void
CasePropsBuilder::writeBinaryData(const char *path, UBool withCopyright, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }

    UNewDataMemory *pData=udata_create(path, "icu", "ucase", &dataInfo,
                                       withCopyright ? U_COPYRIGHT_STRING : NULL, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops: udata_create(%s, ucase.icu) failed - %s\n",
                path, u_errorName(errorCode));
        return;
    }

    udata_writeBlock(pData, indexes, sizeof(indexes));
    udata_writeBlock(pData, trieBlock, trieSize);
    udata_writeBlock(pData, exceptions.getBuffer(), 2*exceptions.length());
    udata_writeBlock(pData, unfold.getBuffer(), 2*unfold.length());

    /* finish up */
    long dataLength=udata_finish(pData, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: casepropsbuilder error %d writing the output file\n", errorCode);
        exit(errorCode);
    }

    if(dataLength!=indexes[UCASE_IX_LENGTH]) {
        fprintf(stderr,
                "udata_finish(ucase.icu) reports %ld bytes written but should be %ld\n",
                dataLength, (long)indexes[UCASE_IX_LENGTH]);
        errorCode=U_INTERNAL_PROGRAM_ERROR;
    }
}

PropsBuilder *
createCasePropsBuilder(UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return NULL; }
    PropsBuilder *pb=new CasePropsBuilder(errorCode);
    if(pb==NULL) {
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
