/*
**********************************************************************
*   Copyright (c) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
* $Source: /xsrl/Nsvn/icu/icu/source/i18n/Attic/upropset.cpp,v $
* $Date: 2001/10/17 19:20:41 $
* $Revision: 1.1 $
**********************************************************************
*/
#include "upropset.h"
#include "ustrfmt.h"
#include "unicode/unistr.h"
#include "unicode/uscript.h"
#include "unicode/uniset.h"
#include "unicode/parsepos.h"
#include "hash.h"

U_NAMESPACE_BEGIN

static Hashtable* NAME_MAP = NULL;

static Hashtable* CATEGORY_MAP = NULL;

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

//----------------------------------------------------------------------
// class _CharString
// An identical class named CharString can be found in transreg.cpp.
// If we find ourselves needing another copy of this utility class we
// should probably pull it out into putil or some such place.
//----------------------------------------------------------------------

class _CharString {
 public:
    _CharString(const UnicodeString& str);
    ~_CharString();
    operator char*() { return ptr; }
 private:
    char buf[128];
    char* ptr;
};

_CharString::_CharString(const UnicodeString& str) {
    if (str.length() >= (int32_t)sizeof(buf)) {
        ptr = new char[str.length() + 8];
    } else {
        ptr = buf;
    }
    str.extract(0, 0x7FFFFFFF, ptr, "");
}

_CharString::~_CharString() {
    if (ptr != buf) {
        delete[] ptr;
    }
}

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
            // Syntax error; type name not recognized
            return NULL;
        }
        set = (*factory)(valueName);
    } else {
        // No equals seen; parse short format \p{Cf}
        UnicodeString shortName = munge(pattern, pos, close);

        // First try general category
        set = createCategorySet(shortName);

        // If this fails, try script
        if (set == NULL) {
            set = createScriptSet(shortName);
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
    _CharString cvalueName(valueName);
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
        set->complement();
        return set;
    }
    for (int32_t cat=0; cat<U_CHAR_CATEGORY_COUNT; ++cat) {
        if ((valueCode & (1 << cat)) != 0) {
            set->addAll(getCategorySet(cat));
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
    _CharString cvalueName(valueName);
    UErrorCode ec = U_ZERO_ERROR;
    UScriptCode script = uscript_getCode(cvalueName, &ec);
    if (script == USCRIPT_INVALID_CODE || U_FAILURE(ec)) {
        // Syntax error; unknown short name
        return NULL;
    }
    return new UnicodeSet(getScriptSet(script));
}

//----------------------------------------------------------------
// Utility methods
//----------------------------------------------------------------

static UBool _categoryFilter(UChar32 c, void* context) {
    int32_t value = * (int32_t*) context;
    return u_charType(c) == value;
}

/**
 * Returns a UnicodeSet for the given category.  This set is
 * cached and returned again if this method is called again with
 * the same parameter.
 *
 * Callers MUST NOT MODIFY the returned set.
 */
const UnicodeSet& UnicodePropertySet::getCategorySet(int32_t cat) {
    if (CATEGORY_CACHE[cat].isEmpty()) {
        initSetFromFilter(CATEGORY_CACHE[cat], _categoryFilter, &cat);
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
        initSetFromFilter(CATEGORY_CACHE[script], _scriptFilter, &script);
    }
    return SCRIPT_CACHE[script];
}

/**
 * Given a string, munge it to lost the whitespace.  So "General
 * Category " becomes "GeneralCategory".  We munge all type and value
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
        if (!u_isspace(c)) {
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
    set.clear();

    int32_t start = -1;
    int32_t end = -2;
    
    // TODO Extend this up to UnicodeSet.MAX_VALUE when we have
    // better performance; i.e., when this code can get moved into
    // the UCharacter class and not have to iterate over code
    // points.  Right now it's way too slow to iterate to 10FFFF.
    
    for (int32_t i=UnicodeSet::MIN_VALUE; i<=0xFFFF/*TEMPORARY*/; ++i) {
        if ((*filter)((UChar32) i, context)) {
            if ((end+1) == i) {
                end = i;
            } else {
                if (start >= 0) {
                    set.add((UChar32)start, (UChar32)end);
                }
                start = end = i;
            }
        }
    }
    if (start >= 0) {
        set.add((UChar32)start, (UChar32)end);
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
    CATEGORY_CACHE = new UnicodeSet[U_CHAR_CATEGORY_COUNT];
    SCRIPT_CACHE = new UnicodeSet[USCRIPT_CODE_LIMIT];

    // NOTE:  We munge all search keys to have no whitespace
    // and upper case.  As such, all stored keys should have
    // this format.

    // Load the map with type data

    addType("GC", "GENERALCATEGORY", createCategorySet);

    //addType("CC", "COMBININGCLASS", COMBINING_CLASS);
    //addType("BC", "BIDICLASS", BIDI_CLASS);
    //addType("DT", "DECOMPOSITIONTYPE", DECOMPOSITION_TYPE);

    addType("NV", "NUMERICVALUE", createNumericValueSet);

    //addType("NT", "NUMERICTYPE", NUMERIC_TYPE);
    //addType("EA", "EASTASIANWIDTH", EAST_ASIAN_WIDTH);
    //addType("LB", "LINEBREAK", LINE_BREAK);
    //addType("JT", "JOININGTYPE", JOINING_TYPE);

    addType("SC", "SCRIPT", createScriptSet);

    // Load the map with value data

    // General Category

    addValue(CATEGORY_MAP, "ANY", "", ANY); // special case

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
}

void UnicodePropertySet::cleanup() {
    if (NAME_MAP != NULL) {
        delete NAME_MAP; NAME_MAP = NULL;
        delete CATEGORY_MAP; CATEGORY_MAP = NULL;
        delete[] CATEGORY_CACHE; CATEGORY_CACHE = NULL;
        delete[] SCRIPT_CACHE; SCRIPT_CACHE = NULL;
    }
}

U_NAMESPACE_END

//eof
