/*
**********************************************************************
*   Copyright (c) 2001-2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*/
#include "upropset.h"
#include "ustrfmt.h"
#include "unicode/unistr.h"
#include "unicode/uscript.h"
#include "unicode/uniset.h"
#include "unicode/parsepos.h"
#include "unicode/uchar.h"
#include "hash.h"
#include "mutex.h"
#include "ucln.h"
#include "charstr.h"


static Hashtable* NAME_MAP = NULL;

static Hashtable* CATEGORY_MAP = NULL;

static Hashtable* COMBINING_CLASS_MAP = NULL;

static Hashtable* BIDI_CLASS_MAP = NULL;

static Hashtable* BINARY_PROPERTY_MAP = NULL;

static Hashtable* BOOLEAN_VALUE_MAP = NULL;

/**
 * A cache mapping character category integers, as returned by
 * UCharacter.getType(), to sets.  Entries are initially
 * null and are created on demand.
 */
static UnicodeSet* CATEGORY_CACHE = NULL;

/**
 * A cache mapping script integers, as defined by
 * UScript, to sets.  Entries are initially
 * null and are created on demand.
 */
static UnicodeSet* SCRIPT_CACHE = NULL;

// Special value codes
static const int32_t ANY = -1; // general category: all code points

static const int32_t ASCII = -2; // [\u0000-\u007F]

// Offset used to ensure non-zero values
#define MAPVAL 0x10000

// >From UnicodeData:
// 3400;<CJK Ideograph Extension A, First>;Lo;0;L;;;;;N;;;;;
// 4DB5;<CJK Ideograph Extension A, Last>;Lo;0;L;;;;;N;;;;;
// 4E00;<CJK Ideograph, First>;Lo;0;L;;;;;N;;;;;
// 9FA5;<CJK Ideograph, Last>;Lo;0;L;;;;;N;;;;;
// AC00;<Hangul Syllable, First>;Lo;0;L;;;;;N;;;;;
// D7A3;<Hangul Syllable, Last>;Lo;0;L;;;;;N;;;;;
// D800;<Non Private Use High Surrogate, First>;Cs;0;L;;;;;N;;;;;
// DB7F;<Non Private Use High Surrogate, Last>;Cs;0;L;;;;;N;;;;;
// DB80;<Private Use High Surrogate, First>;Cs;0;L;;;;;N;;;;;
// DBFF;<Private Use High Surrogate, Last>;Cs;0;L;;;;;N;;;;;
// DC00;<Low Surrogate, First>;Cs;0;L;;;;;N;;;;;
// DFFF;<Low Surrogate, Last>;Cs;0;L;;;;;N;;;;;
// E000;<Private Use, First>;Co;0;L;;;;;N;;;;;
// F8FF;<Private Use, Last>;Co;0;L;;;;;N;;;;;
// 20000;<CJK Ideograph Extension B, First>;Lo;0;L;;;;;N;;;;;
// 2A6D6;<CJK Ideograph Extension B, Last>;Lo;0;L;;;;;N;;;;;
// F0000;<Plane 15 Private Use, First>;Co;0;L;;;;;N;;;;;
// FFFFD;<Plane 15 Private Use, Last>;Co;0;L;;;;;N;;;;;
// 100000;<Plane 16 Private Use, First>;Co;0;L;;;;;N;;;;;
// 10FFFD;<Plane 16 Private Use, Last>;Co;0;L;;;;;N;;;;;
// 
// >Large Blocks of Unassigned: (from DerivedGeneralCategory)
// 1044E..1CFFF  ; Cn # [52146]
// 1D800..1FFFF  ; Cn # [10240]
// 2A6D7..2F7FF  ; Cn # [20777]
// 2FA1E..E0000  ; Cn # [722403]
// E0080..EFFFF  ; Cn # [65408]

/**
 * A set of all characters _except_ the second through last characters of
 * certain ranges.  These ranges are ranges of characters whose
 * properties are all exactly alike, e.g. CJK Ideographs from
 * U+4E00 to U+9FA5.
 */
static UnicodeSet* INCLUSIONS = NULL;

//----------------------------------------------------------------
// Unicode string and character constants
//----------------------------------------------------------------

static const UChar POSIX_OPEN[]  = { 91,58,0 }; // "[:"
static const UChar POSIX_CLOSE[] = { 58,93,0 }; // ":]"

static const UChar PERL_OPEN[]  = { 92,112,0 }; // "\\p"
static const UChar PERL_CLOSE[] = { 125,0 };    // "}"

static const UChar HAT        = 0x005E; /*^*/
static const UChar UPPER_P    = 0x0050; /*P*/
static const UChar LEFT_BRACE = 0x007B; /*{*/
static const UChar EQUALS     = 0x003D; /*=*/

// TODO: The Inclusion List should be generated from the UCD for each
// version, and thus should be accessed from the properties data file
// (Even better: move the logic into UCharacter for building these
// properties, since that is where it belongs!)

// See INCLUSIONS above
static const UChar INCLUSIONS_PATTERN[] =
{91,94,92,117,51,52,48,49,45,92,117,52,68,66,53,32,
92,117,52,69,48,49,45,92,117,57,70,65,53,32,
92,117,65,67,48,49,45,92,117,68,55,65,51,32,
92,117,68,56,48,49,45,92,117,68,66,55,70,32,
92,117,68,66,56,49,45,92,117,68,66,70,70,32,
92,117,68,67,48,49,45,92,117,68,70,70,70,32,
92,117,69,48,48,49,45,92,117,70,56,70,70,32,
92,85,48,48,48,49,48,52,52,70,45,92,85,48,48,48,49,67,70,70,70,32,
92,85,48,48,48,49,68,56,48,49,45,92,85,48,48,48,49,70,70,70,70,32,
92,85,48,48,48,50,48,48,48,49,45,92,85,48,48,48,50,65,54,68,54,32,
92,85,48,48,48,50,65,54,68,56,45,92,85,48,48,48,50,70,55,70,70,32,
92,85,48,48,48,50,70,65,49,70,45,92,85,48,48,48,69,48,48,48,48,32,
92,85,48,48,48,69,48,48,56,49,45,92,85,48,48,48,69,70,70,70,70,32,
92,85,48,48,48,70,48,48,48,49,45,92,85,48,48,48,70,70,70,70,68,32,
92,85,48,48,49,48,48,48,48,49,45,92,85,48,48,49,48,70,70,70,68,93,0};
// "[^\\u3401-\\u4DB5 \\u4E01-\\u9FA5 \\uAC01-\\uD7A3 \\uD801-\\uDB7F \\uDB81-\\uDBFF \\uDC01-\\uDFFF \\uE001-\\uF8FF \\U0001044F-\\U0001CFFF \\U0001D801-\\U0001FFFF \\U00020001-\\U0002A6D6 \\U0002A6D8-\\U0002F7FF \\U0002FA1F-\\U000E0000 \\U000E0081-\\U000EFFFF \\U000F0001-\\U000FFFFD \\U00100001-\\U0010FFFD]"

/**
 * Cleanup function for UnicodePropertySet
 */
U_CFUNC UBool upropset_cleanup(void) {
    if (NAME_MAP != NULL) {
        delete NAME_MAP; NAME_MAP = NULL;
        delete CATEGORY_MAP; CATEGORY_MAP = NULL;
        delete COMBINING_CLASS_MAP; COMBINING_CLASS_MAP = NULL;
        delete BIDI_CLASS_MAP; BIDI_CLASS_MAP = NULL;
        delete BINARY_PROPERTY_MAP; BINARY_PROPERTY_MAP = NULL;
        delete BOOLEAN_VALUE_MAP; BOOLEAN_VALUE_MAP = NULL;
        delete[] CATEGORY_CACHE; CATEGORY_CACHE = NULL;
        delete[] SCRIPT_CACHE; SCRIPT_CACHE = NULL;
        delete INCLUSIONS; INCLUSIONS = NULL;
    }
    return TRUE;
}

U_NAMESPACE_BEGIN

const char CharString::fgClassID=0;

//----------------------------------------------------------------
// Public API
//----------------------------------------------------------------

/**
 * Return true if the given position, in the given pattern, appears
 * to be the start of a property set pattern [:foo:], \p{foo}, or
 * \P{foo}.
 */
UBool UnicodePropertySet::resemblesPattern(const UnicodeString& pattern,
                                           int32_t pos) {
    // Patterns are at least 5 characters long
    if ((pos+5) > pattern.length()) {
        return FALSE;
    }

    // Look for an opening [:, [:^, \p, or \P
    return (0 == pattern.compare(pos, 2, POSIX_OPEN)) ||
        (0 == pattern.caseCompare(pos, 2, PERL_OPEN, U_FOLD_CASE_DEFAULT));
}

/**
 * Create a UnicodeSet by parsing the given pattern at the given
 * parse position.
 *
 * @param pattern the pattern string
 * @param ppos on entry, the position at which to begin parsing.
 * This shold be one of the locations marked '^':
 *
 *   [:blah:]     \p{blah}     \P{blah}
 *   ^       %    ^       %    ^       %
 *
 * On return, the position after the last character parsed, that is,
 * the locations marked '%'.  If the parse fails, ppos is returned
 * unchanged.
 * @return a newly-constructed UnicodeSet object, or null upon
 * failure.
 */
UnicodeSet* UnicodePropertySet::createFromPattern(const UnicodeString& pattern,
                                                  ParsePosition& ppos) {
    init();

    UnicodeSet* set = NULL;

    int32_t pos = ppos.getIndex();

    // On entry, ppos should point to one of the following locations:

    // Minimum length is 5 characters, e.g. \p{L}
    if ((pos+5) > pattern.length()) {
        return NULL;
    }

    UBool posix = FALSE; // true for [:pat:], false for \p{pat} \P{pat}
    UBool invert = FALSE;

    // Look for an opening [:, [:^, \p, or \P
    if (0 == pattern.compare(pos, 2, POSIX_OPEN)) {
        posix = TRUE;
        pos = skipWhitespace(pattern, pos+2);
        if (pos < pattern.length() && pattern.charAt(pos) == HAT) {
            ++pos;
            invert = TRUE;
        }
    } else if (0 == pattern.caseCompare(pos, 2, PERL_OPEN, U_FOLD_CASE_DEFAULT)) {
        invert = (pattern.charAt(pos+1) == UPPER_P);
        pos = skipWhitespace(pattern, pos+2);
        if (pos == pattern.length() || pattern.charAt(pos++) != LEFT_BRACE) {
            // Syntax error; "\p" or "\P" not followed by "{"
            return NULL;
        }
    } else {
        // Open delimiter not seen
        return NULL;
    }

    // Look for the matching close delimiter, either :] or }
    int32_t close = pattern.indexOf(posix ? POSIX_CLOSE : PERL_CLOSE, pos);
    if (close < 0) {
        // Syntax error; close delimiter missing
        return NULL;
    }

    // Look for an '=' sign.  If this is present, we will parse a
    // medium \p{gc=Cf} or long \p{GeneralCategory=Format}
    // pattern.
    int32_t equals = pattern.indexOf(EQUALS, pos);
    if (equals >= 0 && equals < close) {
        // Equals seen; parse medium/long pattern
        UnicodeString typeName = munge(pattern, pos, equals);
        UnicodeString valueName = munge(pattern, equals+1, close);
        SetFactory factory;
        factory = voidPtrToSetFactory(NAME_MAP->get(typeName));
        if (factory == NULL) {
            // Not a factory; try a binary property of the form
            // \p{foo=true}, where the value can be 'true', 't',
            // 'false', or 'f'.
            int32_t v = BOOLEAN_VALUE_MAP->geti(valueName) - MAPVAL;
            if (v >= 0) {
                set = createBinaryPropertySet(typeName);
                invert ^= !v;
            }

            if (set == NULL) {
                // Syntax error; type name not recognized
                return NULL;
            }
        } else {
            set = (*factory)(valueName);
        }
    } else {
        // No equals seen; parse short format \p{Cf}
        UnicodeString shortName = munge(pattern, pos, close);

        // First try general category
        set = createCategorySet(shortName);

        // If this fails, try script
        if (set == NULL) {
            set = createScriptSet(shortName);
        }

        // If this fails, try binary property
        if (set == NULL) {
            set = createBinaryPropertySet(shortName);
        }
    }

    // Upon failure, return NULL with ppos unchanged
    if (set == NULL) {
        return NULL;
    }

    if (invert) {
        set->complement();
    }

    // Move to the limit position after the close delimiter
    ppos.setIndex(close + (posix ? 2 : 1));

    return set;
}

//----------------------------------------------------------------
// Property set factory static methods
// NOTE: This will change/go away when we implement UCharacter
// based property retrieval.
//----------------------------------------------------------------

static UBool _numericValueFilter(UChar32 c, void* context) {
    int32_t value = * (int32_t*) context;
    // TODO: Change this to a more generic function, like
    // u_charNumericValue (when one exists).
    return u_charDigitValue(c) == value;
}

UnicodeSet* UnicodePropertySet::createNumericValueSet(const UnicodeString& valueName) {
    CharString cvalueName(valueName);
    UnicodeSet* set = new UnicodeSet();
    char* end;
    double value = uprv_strtod(cvalueName, &end);
    int32_t ivalue = (int32_t) value;
    if (ivalue != value || ivalue < 0 || *end != 0) {
        // UCharacter doesn't support negative or non-integral
        // values, so just return an empty set
        return set;
    }
    initSetFromFilter(*set, _numericValueFilter, &ivalue);
    return set;
}

static UBool _combiningClassFilter(UChar32 c, void* context) {
    int32_t value = * (int32_t*) context;
    return u_getCombiningClass(c) == value;
}

UnicodeSet* UnicodePropertySet::createCombiningClassSet(const UnicodeString& valueName) {
    CharString cvalueName(valueName);
    UnicodeSet* set = new UnicodeSet();
    char* end;
    double value = uprv_strtod(cvalueName, &end);
    int32_t ivalue = (int32_t) value;
    if (ivalue != value || ivalue < 0 || *end != 0) {
    // We have a non-integral or negative value, or non-numeric text.
    // Try to lookup a symbolic combining class name
    ivalue = COMBINING_CLASS_MAP->geti(valueName) - MAPVAL;
    }
    if (ivalue >= 0) {
    // We have a potentially valid combining class
        initSetFromFilter(*set, _combiningClassFilter, &ivalue);
    }
    return set;
}

static UBool _bidiClassFilter(UChar32 c, void* context) {
    int32_t value = * (int32_t*) context;
    return u_charDirection(c) == value;
}

UnicodeSet* UnicodePropertySet::createBidiClassSet(const UnicodeString& valueName) {
    int32_t valueCode = BIDI_CLASS_MAP->geti(valueName) - MAPVAL;
    if (valueCode < 0) {
        return NULL;
    }
    UnicodeSet* set = new UnicodeSet();
    initSetFromFilter(*set, _bidiClassFilter, &valueCode);
    return set;
}

/**
 * Given a general category value name, create a corresponding
 * set and return it, or return null if the name is invalid.
 * @param valueName a pre-munged general category value name
 */
UnicodeSet* UnicodePropertySet::createCategorySet(const UnicodeString& valueName) {
    int32_t valueCode = CATEGORY_MAP->geti(valueName);
    if (valueCode == 0) {
        return NULL;
    }

    UnicodeSet* set = new UnicodeSet();
    if (valueCode == ANY) {
        set->set(0x00, 0x10FFFF);
    } else if (valueCode == ASCII) {
        set->set(0x00, 0x7F);
    } else {
        for (int32_t cat=0; cat<U_CHAR_CATEGORY_COUNT; ++cat) {
            if ((valueCode & (1 << cat)) != 0) {
                set->addAll(getCategorySet(cat));
            }
        }
    }
    return set;
}

/**
 * Given a script value name, create a corresponding set and
 * return it, or return null if the name is invalid.
 * @param valueName a pre-munged script value name
 */
UnicodeSet* UnicodePropertySet::createScriptSet(const UnicodeString& valueName) {
    CharString cvalueName(valueName);
    UErrorCode ec = U_ZERO_ERROR;
    const int32_t capacity = 10;
    UScriptCode script[capacity]={USCRIPT_INVALID_CODE};

    // Ignore the return value of uscript_getCode
    // since this is locale independent.
    uscript_getCode(cvalueName,script,capacity, &ec);

    if (script[0] == USCRIPT_INVALID_CODE || U_FAILURE(ec)) {
        // Syntax error; unknown short name
        return NULL;
    }
    return new UnicodeSet(getScriptSet(script[0]));
}

static UBool _binaryPropertyFilter(UChar32 c, void* context) {
    int32_t code = * (int32_t*) context;
    return u_hasBinaryProperty(c, (UProperty) code);
}

/**
 * Given a binary property name, create a corresponding
 * set and return it, or return null if the name is invalid.
 * @param valueName a pre-munged binary property name
 */
UnicodeSet* UnicodePropertySet::createBinaryPropertySet(const UnicodeString& name) {
    int32_t code = BINARY_PROPERTY_MAP->geti(name) - MAPVAL;
    if (code < 0) {
        return NULL;
    }

    UnicodeSet* set = new UnicodeSet(); 
    initSetFromFilter(*set, _binaryPropertyFilter, &code);
    return set;
}

//----------------------------------------------------------------
// Utility methods
//----------------------------------------------------------------

U_CDECL_BEGIN
static UBool U_CALLCONV
_enumCategoryRange(const void * /*context*/,
                   UChar32 start, UChar32 limit, UCharCategory type)
{
    CATEGORY_CACHE[type].add(start, limit-1);
    return TRUE;
}
U_CDECL_END

/**
 * Returns a UnicodeSet for the given category.  This set is
 * cached and returned again if this method is called again with
 * the same parameter.
 *
 * Callers MUST NOT MODIFY the returned set.
 */
const UnicodeSet& UnicodePropertySet::getCategorySet(int32_t cat) {
    if (CATEGORY_CACHE == 0) {
        CATEGORY_CACHE = new UnicodeSet[32]; // 32 is guaranteed by the Unicode standard
        if (CATEGORY_CACHE == 0) {
            return *((const UnicodeSet *)0);
        }
        u_enumCharTypes(_enumCategoryRange, 0);
    }
    return CATEGORY_CACHE[cat];
}

static UBool _scriptFilter(UChar32 c, void* context) {
    UScriptCode value = * (UScriptCode*) context;
    UErrorCode ec = U_ZERO_ERROR;
    return uscript_getScript(c, &ec) == value;
}

/**
 * Returns a UnicodeSet for the given script.  This set is
 * cached and returned again if this method is called again with
 * the same parameter.
 *
 * Callers MUST NOT MODIFY the returned set.
 */
const UnicodeSet& UnicodePropertySet::getScriptSet(UScriptCode script) {
    if (SCRIPT_CACHE[script].isEmpty()) {
        initSetFromFilter(SCRIPT_CACHE[script], _scriptFilter, &script);
    }
    return SCRIPT_CACHE[script];
}

/**
 * Given a string, munge it to lose the whitespace, underscores, and hyphens.
 * So "General  Category " or "General_Category" or " General-Category"
 * become "GeneralCategory". We munge all type and value
 * strings, and store all type and value keys pre-munged.  NOTE:
 * Unlike the Java version, we do not modify the case, since we use a
 * case-insensitive compare function.
 */
UnicodeString UnicodePropertySet::munge(const UnicodeString& str,
                                        int32_t start, int32_t limit) {
    UnicodeString buf;
    for (int32_t i=start; i<limit; ) {
        UChar32 c = str.char32At(i);
        i += UTF_CHAR_LENGTH(c);
        if (c != 95/*_*/ && c != 45/*-*/ && !u_isspace(c)) {
            buf.append(c);
        }
    }
    return buf;
}

/**
 * Skip over a sequence of zero or more white space characters
 * at pos.  Return the index of the first non-white-space character
 * at or after pos, or str.length(), if there is none.
 */
int32_t UnicodePropertySet::skipWhitespace(const UnicodeString& str,
                                           int32_t pos) {
    while (pos < str.length()) {
        UChar32 c = str.char32At(pos);
        if (!u_isspace(c)) {
            break;
        }
        pos += UTF_CHAR_LENGTH(c);
    }
    return pos;
}

//----------------------------------------------------------------
// Generic filter-based scanning code
//
// NOTE: In general, we don't want to do this!  This is a temporary
// implementation until we have time for something that examines
// the underlying UCharacter data structures in an intelligent
// way.  Iterating over all code points is dumb.  What we want to
// do, for instance, is iterate over internally-stored ranges
// of characters that have a given property.
//----------------------------------------------------------------

void UnicodePropertySet::initSetFromFilter(UnicodeSet& set, Filter filter,
                                           void* context) {
    // Walk through all Unicode characters, noting the start
    // and end of each range for which filter.contain(c) is
    // true.  Add each range to a set.
    //
    // To improve performance, use the INCLUSIONS set, which
    // encodes information about character ranges that are known
    // to have identical properties, such as the CJK Ideographs
    // from U+4E00 to U+9FA5.  INCLUSIONS contains all characters
    // except the first characters of such ranges.
    //
    // TODO Where possible, instead of scanning over code points,
    // use internal property data to initialize UnicodeSets for
    // those properties.  Scanning code points is slow.

    if (INCLUSIONS == NULL) {
        Mutex lock;
        if (INCLUSIONS == NULL) {
            UErrorCode ec = U_ZERO_ERROR;
            INCLUSIONS = new UnicodeSet(INCLUSIONS_PATTERN, ec);
        }
    }

    set.clear();

    int32_t startHasProperty = -1;
    int limitRange = INCLUSIONS->getRangeCount();
    
    for (int j=0; j<limitRange; ++j) {
        // get current range
        UChar32 start = INCLUSIONS->getRangeStart(j);
        UChar32 end = INCLUSIONS->getRangeEnd(j);
        
        // for all the code points in the range, process
        for (UChar32 ch = start; ch <= end; ++ch) {
            // only add to the unicodeset on inflection points --
            // where the hasProperty value changes to false
            if ((*filter)((UChar32) ch, context)) {
                if (startHasProperty < 0) {
                    startHasProperty = ch;
                }
            } else if (startHasProperty >= 0) {
                set.add((UChar32)startHasProperty, (UChar32)ch-1);
                startHasProperty = -1;
            }
        }
    }
    if (startHasProperty >= 0) {
        set.add((UChar32)startHasProperty, (UChar32)0x10FFFF);
    }
}

//----------------------------------------------------------------
// Type and value name maps
//----------------------------------------------------------------

/**
 * Add a type mapping to the name map.
 */
void UnicodePropertySet::addType(const UnicodeString& shortName,
                                 const UnicodeString& longName,
                                 SetFactory factory) {
    UErrorCode ec = U_ZERO_ERROR;
    void* p = setFactoryToVoidPtr(factory);
    NAME_MAP->put(shortName, p, ec);
    NAME_MAP->put(longName, p, ec);
}

/**
 * Add a value mapping to the name map.
 */
void UnicodePropertySet::addValue(Hashtable* map,
                                  const UnicodeString& shortName,
                                  const UnicodeString& longName,
                                  int32_t value) {
    // assert(value != 0);
    UErrorCode ec = U_ZERO_ERROR;
    map->puti(shortName, value, ec);
    if (longName.length() != 0) {
        map->puti(longName, value, ec);
    }
}

void UnicodePropertySet::init() {
    if (NAME_MAP != NULL) {
        return;
    }

    NAME_MAP = new Hashtable(TRUE);
    CATEGORY_MAP = new Hashtable(TRUE);
    COMBINING_CLASS_MAP = new Hashtable(TRUE);
    BIDI_CLASS_MAP = new Hashtable(TRUE);
    BINARY_PROPERTY_MAP = new Hashtable(TRUE);
    BOOLEAN_VALUE_MAP = new Hashtable(TRUE);
    SCRIPT_CACHE = new UnicodeSet[(size_t)USCRIPT_CODE_LIMIT];

    // NOTE:  All short and long names taken from
    // PropertyAliases.txt and PropertyValueAliases.txt.

    // NOTE:  We munge all search keys to have no whitespace
    // and upper case.  As such, all stored keys should have
    // this format.

    //------------------------------------------------------------
    // MAIN KEY MAP

    addType("GC", "GENERALCATEGORY", createCategorySet);

    addType("CCC", "CANONICALCOMBININGCLASS", createCombiningClassSet);

    addType("BC", "BIDICLASS", createBidiClassSet);

    //addType("DT", "DECOMPOSITIONTYPE", DECOMPOSITION_TYPE);

    addType("NV", "NUMERICVALUE", createNumericValueSet);

    //addType("NT", "NUMERICTYPE", NUMERIC_TYPE);
    //addType("EA", "EASTASIANWIDTH", EAST_ASIAN_WIDTH);
    //addType("LB", "LINEBREAK", LINE_BREAK);
    //addType("JT", "JOININGTYPE", JOINING_TYPE);

    addType("SC", "SCRIPT", createScriptSet);

    //------------------------------------------------------------
    // Boolean Value MAP

    addValue(BOOLEAN_VALUE_MAP, "T", "TRUE", MAPVAL + 1);
    addValue(BOOLEAN_VALUE_MAP, "F", "FALSE", MAPVAL + 0);

    //------------------------------------------------------------
    // General Category MAP

    addValue(CATEGORY_MAP, "ANY", "", ANY); // special case

    addValue(CATEGORY_MAP, "ASCII", "", ASCII); // special case

    addValue(CATEGORY_MAP, "C", "OTHER",
             (1 << U_CONTROL_CHAR) |
             (1 << U_FORMAT_CHAR) |
             (1 << U_GENERAL_OTHER_TYPES) |
             (1 << U_PRIVATE_USE_CHAR) |
             (1 << U_SURROGATE));

    addValue(CATEGORY_MAP, "CC", "CONTROL",
             1 << U_CONTROL_CHAR);
    addValue(CATEGORY_MAP, "CF", "FORMAT",
             1 << U_FORMAT_CHAR);
    addValue(CATEGORY_MAP, "CN", "UNASSIGNED",
             1 << U_GENERAL_OTHER_TYPES);
    addValue(CATEGORY_MAP, "CO", "PRIVATEUSE",
             1 << U_PRIVATE_USE_CHAR);
    addValue(CATEGORY_MAP, "CS", "SURROGATE",
             1 << U_SURROGATE);

    addValue(CATEGORY_MAP, "L", "LETTER",
             (1 << U_LOWERCASE_LETTER) |
             (1 << U_MODIFIER_LETTER) |
             (1 << U_OTHER_LETTER) |
             (1 << U_TITLECASE_LETTER) |
             (1 << U_UPPERCASE_LETTER));

    addValue(CATEGORY_MAP, "LL", "LOWERCASELETTER",
             1 << U_LOWERCASE_LETTER);
    addValue(CATEGORY_MAP, "LM", "MODIFIERLETTER",
             1 << U_MODIFIER_LETTER);
    addValue(CATEGORY_MAP, "LO", "OTHERLETTER",
             1 << U_OTHER_LETTER);
    addValue(CATEGORY_MAP, "LT", "TITLECASELETTER",
             1 << U_TITLECASE_LETTER);
    addValue(CATEGORY_MAP, "LU", "UPPERCASELETTER",
             1 << U_UPPERCASE_LETTER);

    addValue(CATEGORY_MAP, "M", "MARK",
             (1 << U_NON_SPACING_MARK) |
             (1 << U_COMBINING_SPACING_MARK) |
             (1 << U_ENCLOSING_MARK));

    addValue(CATEGORY_MAP, "MN", "NONSPACINGMARK",
             1 << U_NON_SPACING_MARK);
    addValue(CATEGORY_MAP, "MC", "SPACINGMARK",
             1 << U_COMBINING_SPACING_MARK);
    addValue(CATEGORY_MAP, "ME", "ENCLOSINGMARK",
             1 << U_ENCLOSING_MARK);

    addValue(CATEGORY_MAP, "N", "NUMBER",
             (1 << U_DECIMAL_DIGIT_NUMBER) |
             (1 << U_LETTER_NUMBER) |
             (1 << U_OTHER_NUMBER));

    addValue(CATEGORY_MAP, "ND", "DECIMALNUMBER",
             1 << U_DECIMAL_DIGIT_NUMBER);
    addValue(CATEGORY_MAP, "NL", "LETTERNUMBER",
             1 << U_LETTER_NUMBER);
    addValue(CATEGORY_MAP, "NO", "OTHERNUMBER",
             1 << U_OTHER_NUMBER);

    addValue(CATEGORY_MAP, "P", "PUNCTUATION",
             (1 << U_CONNECTOR_PUNCTUATION) |
             (1 << U_DASH_PUNCTUATION) |
             (1 << U_END_PUNCTUATION) |
             (1 << U_FINAL_PUNCTUATION) |
             (1 << U_INITIAL_PUNCTUATION) |
             (1 << U_OTHER_PUNCTUATION) |
             (1 << U_START_PUNCTUATION));

    addValue(CATEGORY_MAP, "PC", "CONNECTORPUNCTUATION",
             1 << U_CONNECTOR_PUNCTUATION);
    addValue(CATEGORY_MAP, "PD", "DASHPUNCTUATION",
             1 << U_DASH_PUNCTUATION);
    addValue(CATEGORY_MAP, "PE", "ENDPUNCTUATION",
             1 << U_END_PUNCTUATION);
    addValue(CATEGORY_MAP, "PF", "FINALPUNCTUATION",
             1 << U_FINAL_PUNCTUATION);
    addValue(CATEGORY_MAP, "PI", "INITIALPUNCTUATION",
             1 << U_INITIAL_PUNCTUATION);
    addValue(CATEGORY_MAP, "PO", "OTHERPUNCTUATION",
             1 << U_OTHER_PUNCTUATION);
    addValue(CATEGORY_MAP, "PS", "STARTPUNCTUATION",
             1 << U_START_PUNCTUATION);

    addValue(CATEGORY_MAP, "S", "SYMBOL",
             (1 << U_CURRENCY_SYMBOL) |
             (1 << U_MODIFIER_SYMBOL) |
             (1 << U_MATH_SYMBOL) |
             (1 << U_OTHER_SYMBOL));

    addValue(CATEGORY_MAP, "SC", "CURRENCYSYMBOL",
             1 << U_CURRENCY_SYMBOL);
    addValue(CATEGORY_MAP, "SK", "MODIFIERSYMBOL",
             1 << U_MODIFIER_SYMBOL);
    addValue(CATEGORY_MAP, "SM", "MATHSYMBOL",
             1 << U_MATH_SYMBOL);
    addValue(CATEGORY_MAP, "SO", "OTHERSYMBOL",
             1 << U_OTHER_SYMBOL);

    addValue(CATEGORY_MAP, "Z", "SEPARATOR",
             (1 << U_LINE_SEPARATOR) |
             (1 << U_PARAGRAPH_SEPARATOR) |
             (1 << U_SPACE_SEPARATOR));

    addValue(CATEGORY_MAP, "ZL", "LINESEPARATOR",
             1 << U_LINE_SEPARATOR);
    addValue(CATEGORY_MAP, "ZP", "PARAGRAPHSEPARATOR",
             1 << U_PARAGRAPH_SEPARATOR);
    addValue(CATEGORY_MAP, "ZS", "SPACESEPARATOR",
             1 << U_SPACE_SEPARATOR);

    //------------------------------------------------------------
    // Combining Class MAP

    addValue(COMBINING_CLASS_MAP, "NR", "NOTREORDERED", MAPVAL + 0);
    addValue(COMBINING_CLASS_MAP, "OV", "OVERLAY", MAPVAL + 1);
    addValue(COMBINING_CLASS_MAP, "NU", "NUKTA", MAPVAL + 7);
    addValue(COMBINING_CLASS_MAP, "KV", "KANAVOICING", MAPVAL + 8);
    addValue(COMBINING_CLASS_MAP, "V", "VIRAMA", MAPVAL + 9);
    addValue(COMBINING_CLASS_MAP, "ATBL", "ATTACHEDBELOWLEFT", MAPVAL + 202);
    addValue(COMBINING_CLASS_MAP, "ATAR", "ATTACHEDABOVERIGHT", MAPVAL + 216);
    addValue(COMBINING_CLASS_MAP, "BL", "BELOWLEFT", MAPVAL + 218);
    addValue(COMBINING_CLASS_MAP, "B", "BELOW", MAPVAL + 220);
    addValue(COMBINING_CLASS_MAP, "BR", "BELOWRIGHT", MAPVAL + 222);
    addValue(COMBINING_CLASS_MAP, "L", "LEFT", MAPVAL + 224);
    addValue(COMBINING_CLASS_MAP, "R", "RIGHT", MAPVAL + 226);
    addValue(COMBINING_CLASS_MAP, "AL", "ABOVELEFT", MAPVAL + 228);
    addValue(COMBINING_CLASS_MAP, "A", "ABOVE", MAPVAL + 230);
    addValue(COMBINING_CLASS_MAP, "AR", "ABOVERIGHT", MAPVAL + 232);
    addValue(COMBINING_CLASS_MAP, "DB", "DOUBLEBELOW", MAPVAL + 232);
    addValue(COMBINING_CLASS_MAP, "DA", "DOUBLEABOVE", MAPVAL + 234);
    addValue(COMBINING_CLASS_MAP, "IS", "IOTASUBSCRIPT", MAPVAL + 240);

    //------------------------------------------------------------
    // Bidi Class MAP

    addValue(BIDI_CLASS_MAP, "AL", "ARABICLETTER", MAPVAL + U_RIGHT_TO_LEFT_ARABIC);
    addValue(BIDI_CLASS_MAP, "AN", "ARABICNUMBER", MAPVAL + U_ARABIC_NUMBER);
    addValue(BIDI_CLASS_MAP, "B", "PARAGRAPHSEPARATOR", MAPVAL + U_BLOCK_SEPARATOR);
    addValue(BIDI_CLASS_MAP, "BN", "BOUNDARYNEUTRAL", MAPVAL + U_BOUNDARY_NEUTRAL);
    addValue(BIDI_CLASS_MAP, "CS", "COMMONSEPARATOR", MAPVAL + U_COMMON_NUMBER_SEPARATOR);
    addValue(BIDI_CLASS_MAP, "EN", "EUROPEANNUMBER", MAPVAL + U_EUROPEAN_NUMBER);
    addValue(BIDI_CLASS_MAP, "ES", "EUROPEANSEPARATOR", MAPVAL + U_EUROPEAN_NUMBER_SEPARATOR);
    addValue(BIDI_CLASS_MAP, "ET", "EUROPEANTERMINATOR", MAPVAL + U_EUROPEAN_NUMBER_TERMINATOR);
    addValue(BIDI_CLASS_MAP, "L", "LEFTTORIGHT", MAPVAL + U_LEFT_TO_RIGHT); 
    addValue(BIDI_CLASS_MAP, "LRE", "LEFTTORIGHTEMBEDDING", MAPVAL + U_LEFT_TO_RIGHT_EMBEDDING);
    addValue(BIDI_CLASS_MAP, "LRO", "LEFTTORIGHTOVERRIDE", MAPVAL + U_LEFT_TO_RIGHT_OVERRIDE);
    addValue(BIDI_CLASS_MAP, "NSM", "NONSPACINGMARK", MAPVAL + U_DIR_NON_SPACING_MARK);
    addValue(BIDI_CLASS_MAP, "ON", "OTHERNEUTRAL", MAPVAL + U_OTHER_NEUTRAL); 
    addValue(BIDI_CLASS_MAP, "PDF", "POPDIRECTIONALFORMAT", MAPVAL + U_POP_DIRECTIONAL_FORMAT);
    addValue(BIDI_CLASS_MAP, "R", "RIGHTTOLEFT", MAPVAL + U_RIGHT_TO_LEFT); 
    addValue(BIDI_CLASS_MAP, "RLE", "RIGHTTOLEFTEMBEDDING", MAPVAL + U_RIGHT_TO_LEFT_EMBEDDING);
    addValue(BIDI_CLASS_MAP, "RLO", "RIGHTTOLEFTOVERRIDE", MAPVAL + U_RIGHT_TO_LEFT_OVERRIDE);
    addValue(BIDI_CLASS_MAP, "S", "SEGMENTSEPARATOR", MAPVAL + U_SEGMENT_SEPARATOR);
    addValue(BIDI_CLASS_MAP, "WS", "WHITESPACENEUTRAL", MAPVAL + U_WHITE_SPACE_NEUTRAL);
    
    //------------------------------------------------------------
    // Binary Properties MAP.  Names taken from PropertyAliases.txt.
    // The following are not supported:

    // CE        ; Composition_Exclusion
    // NBrk      ; Non_Break
    // NFD_QC    ; NFD_Quick_Check
    // NFKD_QC   ; NFKD_Quick_Check
    // OAlpha    ; Other_Alphabetic
    // ODI       ; Other_Default_Ignorable_Code_Point
    // OGr_Ext   ; Other_Grapheme_Extend
    // OLower    ; Other_Lowercase
    // OMath     ; Other_Math
    // OUpper    ; Other_Uppercase
    // XO_NFC    ; Expands_On_NFC
    // XO_NFD    ; Expands_On_NFD
    // XO_NFKC   ; Expands_On_NFKC
    // XO_NFKD   ; Expands_On_NFKD

    addValue(BINARY_PROPERTY_MAP, "ALPHA", "ALPHABETIC", MAPVAL + UCHAR_ALPHABETIC);
    addValue(BINARY_PROPERTY_MAP, "AHEX", "ASCII_HEXDIGIT", MAPVAL + UCHAR_ASCII_HEX_DIGIT);
    addValue(BINARY_PROPERTY_MAP, "BIDIC", "BIDICONTROL", MAPVAL + UCHAR_BIDI_CONTROL);
    addValue(BINARY_PROPERTY_MAP, "BIDIM", "BIDIMIRRORED", MAPVAL + UCHAR_BIDI_MIRRORED);
    addValue(BINARY_PROPERTY_MAP, "DASH", "", MAPVAL + UCHAR_DASH);
    addValue(BINARY_PROPERTY_MAP, "DI", "DEFAULTIGNORABLECODEPOINT", MAPVAL + UCHAR_DEFAULT_IGNORABLE_CODE_POINT);
    addValue(BINARY_PROPERTY_MAP, "DEP", "DEPRECATED", MAPVAL + UCHAR_DEPRECATED);
    addValue(BINARY_PROPERTY_MAP, "DIA", "DIACRITIC", MAPVAL + UCHAR_DIACRITIC);
    addValue(BINARY_PROPERTY_MAP, "EXT", "EXTENDER", MAPVAL + UCHAR_EXTENDER);
    addValue(BINARY_PROPERTY_MAP, "COMPEX", "FULLCOMPOSITIONEXCLUSION", MAPVAL + UCHAR_FULL_COMPOSITION_EXCLUSION);
    addValue(BINARY_PROPERTY_MAP, "GRBASE", "GRAPHEMEBASE", MAPVAL + UCHAR_GRAPHEME_BASE);
    addValue(BINARY_PROPERTY_MAP, "GREXT", "GRAPHEMEEXTEND", MAPVAL + UCHAR_GRAPHEME_EXTEND);
    addValue(BINARY_PROPERTY_MAP, "GRLINK", "GRAPHEMELINK", MAPVAL + UCHAR_GRAPHEME_LINK);
    addValue(BINARY_PROPERTY_MAP, "HEX", "HEXDIGIT", MAPVAL + UCHAR_HEX_DIGIT);
    addValue(BINARY_PROPERTY_MAP, "HYPHEN", "", MAPVAL + UCHAR_HYPHEN);
    addValue(BINARY_PROPERTY_MAP, "IDC", "IDCONTINUE", MAPVAL + UCHAR_ID_CONTINUE);
    addValue(BINARY_PROPERTY_MAP, "IDS", "IDSTART", MAPVAL + UCHAR_ID_START);
    addValue(BINARY_PROPERTY_MAP, "IDEO", "IDEOGRAPHIC", MAPVAL + UCHAR_IDEOGRAPHIC);
    addValue(BINARY_PROPERTY_MAP, "IDSB", "IDSBINARYOPERATOR", MAPVAL + UCHAR_IDS_BINARY_OPERATOR);
    addValue(BINARY_PROPERTY_MAP, "IDST", "IDSTRINARYOPERATOR", MAPVAL + UCHAR_IDS_TRINARY_OPERATOR);
    addValue(BINARY_PROPERTY_MAP, "JOINC", "JOINCONTROL", MAPVAL + UCHAR_JOIN_CONTROL);
    addValue(BINARY_PROPERTY_MAP, "LOE", "LOGICALORDEREXCEPTION", MAPVAL + UCHAR_LOGICAL_ORDER_EXCEPTION);
    addValue(BINARY_PROPERTY_MAP, "LOWER", "LOWERCASE", MAPVAL + UCHAR_LOWERCASE);
    addValue(BINARY_PROPERTY_MAP, "MATH", "", MAPVAL + UCHAR_MATH);
    addValue(BINARY_PROPERTY_MAP, "NCHAR", "NONCHARACTERCODEPOINT", MAPVAL + UCHAR_NONCHARACTER_CODE_POINT);
    addValue(BINARY_PROPERTY_MAP, "QMARK", "QUOTATIONMARK", MAPVAL + UCHAR_QUOTATION_MARK);
    addValue(BINARY_PROPERTY_MAP, "RADICAL", "", MAPVAL + UCHAR_RADICAL);
    addValue(BINARY_PROPERTY_MAP, "SD", "SOFTDOTTED", MAPVAL + UCHAR_SOFT_DOTTED);
    addValue(BINARY_PROPERTY_MAP, "TERM", "TERMINALPUNCTUATION", MAPVAL + UCHAR_TERMINAL_PUNCTUATION);
    addValue(BINARY_PROPERTY_MAP, "UIDEO", "UNIFIEDIDEOGRAPH", MAPVAL + UCHAR_UNIFIED_IDEOGRAPH);
    addValue(BINARY_PROPERTY_MAP, "UPPER", "UPPERCASE", MAPVAL + UCHAR_UPPERCASE);
    addValue(BINARY_PROPERTY_MAP, "WSPACE", "WHITESPACE", MAPVAL + UCHAR_WHITE_SPACE);
    addValue(BINARY_PROPERTY_MAP, "XIDC", "XIDCONTINUE", MAPVAL + UCHAR_XID_CONTINUE);
    addValue(BINARY_PROPERTY_MAP, "XIDS", "XIDSTART", MAPVAL + UCHAR_XID_START);
}

U_NAMESPACE_END

//eof
