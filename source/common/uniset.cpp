/*
**********************************************************************
*   Copyright (C) 1999-2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   10/20/99    alan        Creation.
**********************************************************************
*/

#include "unicode/uniset.h"
#include "unicode/parsepos.h"
#include "unicode/uchar.h"
#include "unicode/uscript.h"
#include "unicode/symtable.h"
#include "ruleiter.h"
#include "cmemory.h"
#include "uhash.h"
#include "util.h"
#include "uvector.h"
#include "uprops.h"
#include "charstr.h"
#include "ustrfmt.h"
#include "mutex.h"
#include "uassert.h"
#include "hash.h"
#include "ucmp8.h"

// HIGH_VALUE > all valid values. 110000 for codepoints
#define UNICODESET_HIGH 0x0110000

// LOW <= all valid values. ZERO for codepoints
#define UNICODESET_LOW 0x000000

// initial storage. Must be >= 0
#define START_EXTRA 16

// extra amount for growth. Must be >= 0
#define GROW_EXTRA START_EXTRA

// Define UChar constants using hex for EBCDIC compatibility
// Used #define to reduce private static exports and memory access time.
#define SET_OPEN        ((UChar)0x005B) /*[*/
#define SET_CLOSE       ((UChar)0x005D) /*]*/
#define HYPHEN          ((UChar)0x002D) /*-*/
#define COMPLEMENT      ((UChar)0x005E) /*^*/
#define COLON           ((UChar)0x003A) /*:*/
#define BACKSLASH       ((UChar)0x005C) /*\*/
#define INTERSECTION    ((UChar)0x0026) /*&*/
#define UPPER_U         ((UChar)0x0055) /*U*/
#define LOWER_U         ((UChar)0x0075) /*u*/
#define OPEN_BRACE      ((UChar)123)    /*{*/
#define CLOSE_BRACE     ((UChar)125)    /*}*/
#define UPPER_P         ((UChar)0x0050) /*P*/
#define LOWER_P         ((UChar)0x0070) /*p*/
#define UPPER_N         ((UChar)78)     /*N*/
#define EQUALS          ((UChar)0x003D) /*=*/

static const UChar POSIX_OPEN[]  = { SET_OPEN,COLON,0 };  // "[:"
static const UChar POSIX_CLOSE[] = { COLON,SET_CLOSE,0 };  // ":]"
static const UChar PERL_OPEN[]   = { BACKSLASH,LOWER_P,0 }; // "\\p"
static const UChar PERL_CLOSE[]  = { CLOSE_BRACE,0 };    // "}"
static const UChar NAME_OPEN[]   = { BACKSLASH,UPPER_N,0 };  // "\\N"
static const UChar HYPHEN_RIGHT_BRACE[] = {HYPHEN,SET_CLOSE,0}; /*-]*/

// Special property set IDs
static const char ANY[]   = "ANY";   // [\u0000-\U0010FFFF]
static const char ASCII[] = "ASCII"; // [\u0000-\u007F]

static const char NAME_PROP[] = "na"; // Unicode name property alias

// TODO: Remove the following special-case code when
// these four C99-compatibility properties are implemented
// as enums/names.
U_CDECL_BEGIN
    typedef UBool (U_CALLCONV *C99_Property_Function)(UChar32);
U_CDECL_END
static const struct C99_Map {
    const char* name;
    C99_Property_Function func;
} C99_DISPATCH[] = {
    // These three entries omitted; they clash with PropertyAliases
    // names for Unicode properties, so UnicodeSet already maps them
    // to those properties.
    //{ "alpha", u_isalpha },
    //{ "lower", u_islower },
    //{ "upper", u_isupper },

    // MUST be in SORTED order
    { "blank", u_isblank },
    { "cntrl", u_iscntrl },
    { "digit", u_isdigit },
    { "graph", u_isgraph },
    { "print", u_isprint },
    { "punct", u_ispunct },
    { "space", u_isspace },
    { "title", u_istitle },
    { "xdigit", u_isxdigit }
};
#define C99_COUNT (9)

// TEMPORARY: Remove when deprecated category code constructor is removed.
static const UChar CATEGORY_NAMES[] = {
    // Must be kept in sync with uchar.h/UCharCategory
    0x43, 0x6E, /* "Cn" */
    0x4C, 0x75, /* "Lu" */
    0x4C, 0x6C, /* "Ll" */
    0x4C, 0x74, /* "Lt" */
    0x4C, 0x6D, /* "Lm" */
    0x4C, 0x6F, /* "Lo" */
    0x4D, 0x6E, /* "Mn" */
    0x4D, 0x65, /* "Me" */
    0x4D, 0x63, /* "Mc" */
    0x4E, 0x64, /* "Nd" */
    0x4E, 0x6C, /* "Nl" */
    0x4E, 0x6F, /* "No" */
    0x5A, 0x73, /* "Zs" */
    0x5A, 0x6C, /* "Zl" */
    0x5A, 0x70, /* "Zp" */
    0x43, 0x63, /* "Cc" */
    0x43, 0x66, /* "Cf" */
    0x43, 0x6F, /* "Co" */
    0x43, 0x73, /* "Cs" */
    0x50, 0x64, /* "Pd" */
    0x50, 0x73, /* "Ps" */
    0x50, 0x65, /* "Pe" */
    0x50, 0x63, /* "Pc" */
    0x50, 0x6F, /* "Po" */
    0x53, 0x6D, /* "Sm" */
    0x53, 0x63, /* "Sc" */
    0x53, 0x6B, /* "Sk" */
    0x53, 0x6F, /* "So" */
    0x50, 0x69, /* "Pi" */
    0x50, 0x66, /* "Pf" */
    0x00
};

/**
 * Delimiter string used in patterns to close a category reference:
 * ":]".  Example: "[:Lu:]".
 */
static const UChar CATEGORY_CLOSE[] = {COLON, SET_CLOSE, 0x0000}; /* ":]" */

U_NAMESPACE_BEGIN

SymbolTable::~SymbolTable() {}

/**
 * Minimum value that can be stored in a UnicodeSet.
 */
const UChar32 UnicodeSet::MIN_VALUE = UNICODESET_LOW;

/**
 * Maximum value that can be stored in a UnicodeSet.
 */
const UChar32 UnicodeSet::MAX_VALUE = UNICODESET_HIGH - 1;

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(UnicodeSet)

static UnicodeSet* INCLUSIONS = NULL; // cached uprv_getInclusions()

static Hashtable* CASE_EQUIV_HASH = NULL; // for closeOver(USET_CASE)

static CompactByteArray* CASE_EQUIV_CBA = NULL; // for closeOver(USET_CASE)

// helper functions for matching of pattern syntax pieces ------------------ ***
// these functions are parallel to the PERL_OPEN etc. strings above

// using these functions is not only faster than UnicodeString::compare() and
// caseCompare(), but they also make UnicodeSet work for simple patterns when
// no Unicode properties data is available - when caseCompare() fails

static inline UBool
isPerlOpen(const UnicodeString &pattern, int32_t pos) {
    UChar c;
    return pattern.charAt(pos)==BACKSLASH && ((c=pattern.charAt(pos+1))==LOWER_P || c==UPPER_P);
}

static inline UBool
isPerlClose(const UnicodeString &pattern, int32_t pos) {
    return pattern.charAt(pos)==CLOSE_BRACE;
}

static inline UBool
isNameOpen(const UnicodeString &pattern, int32_t pos) {
    return pattern.charAt(pos)==BACKSLASH && pattern.charAt(pos+1)==UPPER_N;
}

static inline UBool
isPOSIXOpen(const UnicodeString &pattern, int32_t pos) {
    return pattern.charAt(pos)==SET_OPEN && pattern.charAt(pos+1)==COLON;
}

static inline UBool
isPOSIXClose(const UnicodeString &pattern, int32_t pos) {
    return pattern.charAt(pos)==COLON && pattern.charAt(pos+1)==SET_CLOSE;
}

/**
 * Modify the given UChar32 variable so that it is in range, by
 * pinning values < UNICODESET_LOW to UNICODESET_LOW, and
 * pinning values > UNICODESET_HIGH-1 to UNICODESET_HIGH-1.
 * It modifies its argument in-place and also returns it.
 */
static inline UChar32 pinCodePoint(UChar32& c) {
    if (c < UNICODESET_LOW) {
        c = UNICODESET_LOW;
    } else if (c > (UNICODESET_HIGH-1)) {
        c = (UNICODESET_HIGH-1);
    }
    return c;
}

//----------------------------------------------------------------
// Debugging
//----------------------------------------------------------------

// DO NOT DELETE THIS CODE.  This code is used to debug memory leaks.
// To enable the debugging, define the symbol DEBUG_MEM in the line
// below.  This will result in text being sent to stdout that looks
// like this:
//   DEBUG UnicodeSet: ct 0x00A39B20; 397 [\u0A81-\u0A83\u0A85-
//   DEBUG UnicodeSet: dt 0x00A39B20; 396 [\u0A81-\u0A83\u0A85-
// Each line lists a construction (ct) or destruction (dt) event, the
// object address, the number of outstanding objects after the event,
// and the pattern of the object in question.

// #define DEBUG_MEM

#ifdef DEBUG_MEM
#include <stdio.h>
static int32_t _dbgCount = 0;

static inline void _dbgct(UnicodeSet* set) {
    UnicodeString str;
    set->toPattern(str, TRUE);
    char buf[40];
    str.extract(0, 39, buf, "");
    printf("DEBUG UnicodeSet: ct 0x%08X; %d %s\n", set, ++_dbgCount, buf);
}

static inline void _dbgdt(UnicodeSet* set) {
    UnicodeString str;
    set->toPattern(str, TRUE);
    char buf[40];
    str.extract(0, 39, buf, "");
    printf("DEBUG UnicodeSet: dt 0x%08X; %d %s\n", set, --_dbgCount, buf);
}

#else

#define _dbgct(set)
#define _dbgdt(set)

#endif

//----------------------------------------------------------------
// UnicodeString in UVector support
//----------------------------------------------------------------

static void U_CALLCONV cloneUnicodeString(UHashTok *dst, UHashTok *src) {
    dst->pointer = new UnicodeString(*(UnicodeString*)src->pointer);
}

static int8_t U_CALLCONV compareUnicodeString(UHashTok t1, UHashTok t2) {
    const UnicodeString &a = *(const UnicodeString*)t1.pointer;
    const UnicodeString &b = *(const UnicodeString*)t2.pointer;
    return a.compare(b);
}

//----------------------------------------------------------------
// Constructors &c
//----------------------------------------------------------------

/**
 * Constructs an empty set.
 */
UnicodeSet::UnicodeSet() :
    len(1), capacity(1 + START_EXTRA), bufferCapacity(0),
    list(0), buffer(0), strings(0)
{
    list = (UChar32*) uprv_malloc(sizeof(UChar32) * capacity);
    if(list!=NULL){
        list[0] = UNICODESET_HIGH;
    }
    allocateStrings();
    _dbgct(this);
}

/**
 * Constructs a set containing the given range. If <code>end >
 * start</code> then an empty set is created.
 *
 * @param start first character, inclusive, of range
 * @param end last character, inclusive, of range
 */
UnicodeSet::UnicodeSet(UChar32 start, UChar32 end) :
    len(1), capacity(1 + START_EXTRA), bufferCapacity(0),
    list(0), buffer(0), strings(0)
{
    list = (UChar32*) uprv_malloc(sizeof(UChar32) * capacity);
    if(list!=NULL){
        list[0] = UNICODESET_HIGH;
    }
    allocateStrings();
    complement(start, end);
    _dbgct(this);
}

/**
 * Constructs a set from the given pattern, optionally ignoring
 * white space.  See the class description for the syntax of the
 * pattern language.
 * @param pattern a string specifying what characters are in the set
 */
UnicodeSet::UnicodeSet(const UnicodeString& pattern,
                       UErrorCode& status) :
    len(0), capacity(START_EXTRA), bufferCapacity(0),
    list(0), buffer(0), strings(0)
{   
    if(U_SUCCESS(status)){
        list = (UChar32*) uprv_malloc(sizeof(UChar32) * capacity);
        /* test for NULL */
        if(list == NULL) {
            status = U_MEMORY_ALLOCATION_ERROR;  
        }else{
            allocateStrings();
            applyPattern(pattern, USET_IGNORE_SPACE, NULL, status);
        }
    }
    _dbgct(this);
}

/**
 * Constructs a set from the given pattern, optionally ignoring
 * white space.  See the class description for the syntax of the
 * pattern language.
 * @param pattern a string specifying what characters are in the set
 * @param options bitmask for options to apply to the pattern.
 * Valid options are USET_IGNORE_SPACE and USET_CASE_INSENSITIVE.
 */
UnicodeSet::UnicodeSet(const UnicodeString& pattern,
                       uint32_t options,
                       const SymbolTable* symbols,
                       UErrorCode& status) :
    len(0), capacity(START_EXTRA), bufferCapacity(0),
    list(0), buffer(0), strings(0)
{   
    if(U_SUCCESS(status)){
        list = (UChar32*) uprv_malloc(sizeof(UChar32) * capacity);
        /* test for NULL */
        if(list == NULL) {
            status = U_MEMORY_ALLOCATION_ERROR;  
        }else{
            allocateStrings();
            applyPattern(pattern, options, symbols, status);
        }
    }
    _dbgct(this);
}

UnicodeSet::UnicodeSet(const UnicodeString& pattern, ParsePosition& pos,
                       uint32_t options,
                       const SymbolTable* symbols,
                       UErrorCode& status) :
    len(0), capacity(START_EXTRA), bufferCapacity(0),
    list(0), buffer(0), strings(0)
{
    if(U_SUCCESS(status)){
        list = (UChar32*) uprv_malloc(sizeof(UChar32) * capacity);
        /* test for NULL */
        if(list == NULL) {
            status = U_MEMORY_ALLOCATION_ERROR;   
        }else{
            allocateStrings();
            applyPattern(pattern, pos, options, symbols, status);
        }
    }
    _dbgct(this);
}

#ifdef U_USE_UNICODESET_DEPRECATES
/**
 * DEPRECATED Constructs a set from the given Unicode character category.
 * @param category an integer indicating the character category as
 * defined in uchar.h.
 * @deprecated To be removed after 2002-DEC-31
 */
UnicodeSet::UnicodeSet(int8_t category, UErrorCode& status) :
    len(0), capacity(START_EXTRA), bufferCapacity(0),
    list(0), buffer(0), strings(0)
{
    static const UChar OPEN[] = { 91, 58, 0 }; // "[:"
    static const UChar CLOSE[]= { 58, 93, 0 }; // ":]"
    if (U_SUCCESS(status)) {
        if (category < 0 || category >= U_CHAR_CATEGORY_COUNT) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
        } else {
            UnicodeString pattern(FALSE, CATEGORY_NAMES + category*2, 2);
            pattern.insert(0, OPEN);
            pattern.append(CLOSE);
            list = (UChar32*) uprv_malloc(sizeof(UChar32) * capacity);
            /* test for NULL */
            if(list == NULL) {
                status = U_MEMORY_ALLOCATION_ERROR;
            }else{
                allocateStrings();
                applyPattern(pattern, status);
            }
        }
    }
    _dbgct(this);
}
#endif

/**
 * Constructs a set that is identical to the given UnicodeSet.
 */
UnicodeSet::UnicodeSet(const UnicodeSet& o) :
    UnicodeFilter(o),
    len(0), capacity(o.len + GROW_EXTRA), bufferCapacity(0),
    list(0), buffer(0), strings(0)
{
    list = (UChar32*) uprv_malloc(sizeof(UChar32) * capacity);
    if(list!=NULL){
        allocateStrings();
        *this = o;
    }
    _dbgct(this);
}

/**
 * Destructs the set.
 */
UnicodeSet::~UnicodeSet() {
    _dbgdt(this); // first!
    uprv_free(list);
    if (buffer) {
        uprv_free(buffer);
    }
    delete strings;
}

/**
 * Assigns this object to be a copy of another.
 */
UnicodeSet& UnicodeSet::operator=(const UnicodeSet& o) {
    ensureCapacity(o.len);
    len = o.len;
    uprv_memcpy(list, o.list, len*sizeof(UChar32));
    UErrorCode ec = U_ZERO_ERROR;
    strings->assign(*o.strings, cloneUnicodeString, ec);
    pat = o.pat;
    return *this;
}

/**
 * Compares the specified object with this set for equality.  Returns
 * <tt>true</tt> if the two sets
 * have the same size, and every member of the specified set is
 * contained in this set (or equivalently, every member of this set is
 * contained in the specified set).
 *
 * @param o set to be compared for equality with this set.
 * @return <tt>true</tt> if the specified set is equal to this set.
 */
UBool UnicodeSet::operator==(const UnicodeSet& o) const {
    if (len != o.len) return FALSE;
    for (int32_t i = 0; i < len; ++i) {
        if (list[i] != o.list[i]) return FALSE;
    }
    if (*strings != *o.strings) return FALSE;
    return TRUE;
}

/**
 * Returns a copy of this object.  All UnicodeMatcher objects have
 * to support cloning in order to allow classes using
 * UnicodeMatchers, such as Transliterator, to implement cloning.
 */
UnicodeFunctor* UnicodeSet::clone() const {
    return new UnicodeSet(*this);
}

/**
 * Returns the hash code value for this set.
 *
 * @return the hash code value for this set.
 * @see Object#hashCode()
 */
int32_t UnicodeSet::hashCode(void) const {
    int32_t result = len;
    for (int32_t i = 0; i < len; ++i) {
        result *= 1000003;
        result += list[i];
    }
    return result;
}

//----------------------------------------------------------------
// Public API
//----------------------------------------------------------------

/**
 * Make this object represent the range <code>start - end</code>.
 * If <code>end > start</code> then this object is set to an
 * an empty range.
 *
 * @param start first character in the set, inclusive
 * @rparam end last character in the set, inclusive
 */
UnicodeSet& UnicodeSet::set(UChar32 start, UChar32 end) {
    clear();
    complement(start, end);
    return *this;
}

/**
 * Modifies this set to represent the set specified by the given
 * pattern, optionally ignoring white space.  See the class
 * description for the syntax of the pattern language.
 * @param pattern a string specifying what characters are in the set
 * @param ignoreSpaces if <code>true</code>, all spaces in the
 * pattern are ignored.  Spaces are those characters for which
 * <code>uprv_isRuleWhiteSpace()</code> is <code>true</code>.
 * Characters preceded by '\\' are escaped, losing any special
 * meaning they otherwise have.  Spaces may be included by
 * escaping them.
 * @exception <code>IllegalArgumentException</code> if the pattern
 * contains a syntax error.
 */
UnicodeSet& UnicodeSet::applyPattern(const UnicodeString& pattern,
                                     UErrorCode& status) {
    return applyPattern(pattern, USET_IGNORE_SPACE, NULL, status);
}


/**
 * Modifies this set to represent the set specified by the given
 * pattern, optionally ignoring white space.  See the class
 * description for the syntax of the pattern language.
 * @param pattern a string specifying what characters are in the set
 * @param options bitmask for options to apply to the pattern.
 * Valid options are USET_IGNORE_SPACE and USET_CASE_INSENSITIVE.
 */
UnicodeSet& UnicodeSet::applyPattern(const UnicodeString& pattern,
                                     uint32_t options,
                                     const SymbolTable* symbols,
                                     UErrorCode& status) {
    if (U_FAILURE(status)) {
        return *this;
    }

    ParsePosition pos(0);
    applyPattern(pattern, pos, options, symbols, status);
    if (U_FAILURE(status)) return *this;

    int32_t i = pos.getIndex();

    if (options & USET_IGNORE_SPACE) {
        // Skip over trailing whitespace
        ICU_Utility::skipWhitespace(pattern, i, TRUE);
    }

    if (i != pattern.length()) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
    }
    return *this;
}

UnicodeSet& UnicodeSet::applyPattern(const UnicodeString& pattern,
                              ParsePosition& pos,
                              uint32_t options,
                              const SymbolTable* symbols,
                              UErrorCode& status) {
    if (U_FAILURE(status)) {
        return *this;
    }
    // Need to build the pattern in a temporary string because
    // _applyPattern calls add() etc., which set pat to empty.
    UnicodeString rebuiltPat;
    RuleCharacterIterator chars(pattern, symbols, pos);
    applyPattern(chars, symbols, rebuiltPat, options, status);
    if (U_FAILURE(status)) return *this;
    if (chars.inVariable()) {
        // syntaxError(chars, "Extra chars in variable value");
        status = U_MALFORMED_SET;
        return *this;
    }
    pat = rebuiltPat;
    return *this;
}

/**
 * Return true if the given position, in the given pattern, appears
 * to be the start of a UnicodeSet pattern.
 */
UBool UnicodeSet::resemblesPattern(const UnicodeString& pattern, int32_t pos) {
    return ((pos+1) < pattern.length() &&
            pattern.charAt(pos) == (UChar)91/*[*/) ||
        resemblesPropertyPattern(pattern, pos);
}

/**
 * Append the <code>toPattern()</code> representation of a
 * string to the given <code>StringBuffer</code>.
 */
void UnicodeSet::_appendToPat(UnicodeString& buf, const UnicodeString& s, UBool escapeUnprintable) {
    UChar32 cp;
    for (int32_t i = 0; i < s.length(); i += UTF_CHAR_LENGTH(cp)) {
        _appendToPat(buf, cp = s.char32At(i), escapeUnprintable);
    }
}

/**
 * Append the <code>toPattern()</code> representation of a
 * character to the given <code>StringBuffer</code>.
 */
void UnicodeSet::_appendToPat(UnicodeString& buf, UChar32 c, UBool escapeUnprintable) {
    if (escapeUnprintable && ICU_Utility::isUnprintable(c)) {
        // Use hex escape notation (\uxxxx or \Uxxxxxxxx) for anything
        // unprintable
        if (ICU_Utility::escapeUnprintable(buf, c)) {
            return;
        }
    }
    // Okay to let ':' pass through
    switch (c) {
    case SET_OPEN:
    case SET_CLOSE:
    case HYPHEN:
    case COMPLEMENT:
    case INTERSECTION:
    case BACKSLASH:
    case 123/*{*/:
    case 125/*}*/:
    case SymbolTable::SYMBOL_REF:
    case COLON:
        buf.append(BACKSLASH);
        break;
    default:
        // Escape whitespace
        if (uprv_isRuleWhiteSpace(c)) {
            buf.append(BACKSLASH);
        }
        break;
    }
    buf.append(c);
}

/**
 * Returns a string representation of this set.  If the result of
 * calling this function is passed to a UnicodeSet constructor, it
 * will produce another set that is equal to this one.
 */
UnicodeString& UnicodeSet::toPattern(UnicodeString& result,
                                     UBool escapeUnprintable) const {
    result.truncate(0);
    return _toPattern(result, escapeUnprintable);
}

/**
 * Append a string representation of this set to result.  This will be
 * a cleaned version of the string passed to applyPattern(), if there
 * is one.  Otherwise it will be generated.
 */
UnicodeString& UnicodeSet::_toPattern(UnicodeString& result,
                                      UBool escapeUnprintable) const {
    if (pat.length() > 0) {
        int32_t i;
        int32_t backslashCount = 0;
        for (i=0; i<pat.length(); ) {
            UChar32 c = pat.char32At(i);
            i += UTF_CHAR_LENGTH(c);
            if (escapeUnprintable && ICU_Utility::isUnprintable(c)) {
                // If the unprintable character is preceded by an odd
                // number of backslashes, then it has been escaped.
                // Before unescaping it, we delete the final
                // backslash.
                if ((backslashCount % 2) == 1) {
                    result.truncate(result.length() - 1);
                }
                ICU_Utility::escapeUnprintable(result, c);
                backslashCount = 0;
            } else {
                result.append(c);
                if (c == BACKSLASH) {
                    ++backslashCount;
                } else {
                    backslashCount = 0;
                }
            }
        }
        return result;
    }
    
    return _generatePattern(result, escapeUnprintable);
}

/**
 * Generate and append a string representation of this set to result.
 * This does not use this.pat, the cleaned up copy of the string
 * passed to applyPattern().
 */
UnicodeString& UnicodeSet::_generatePattern(UnicodeString& result,
                                            UBool escapeUnprintable) const {
    result.append(SET_OPEN);

//  // Check against the predefined categories.  We implicitly build
//  // up ALL category sets the first time toPattern() is called.
//  for (int8_t cat=0; cat<Unicode::GENERAL_TYPES_COUNT; ++cat) {
//      if (*this == getCategorySet(cat)) {
//          result.append(COLON);
//          result.append(CATEGORY_NAMES, cat*2, 2);
//          return result.append(CATEGORY_CLOSE);
//      }
//  }

    int32_t count = getRangeCount();

    // If the set contains at least 2 intervals and includes both
    // MIN_VALUE and MAX_VALUE, then the inverse representation will
    // be more economical.
    if (count > 1 &&
        getRangeStart(0) == MIN_VALUE &&
        getRangeEnd(count-1) == MAX_VALUE) {

        // Emit the inverse
        result.append(COMPLEMENT);

        for (int32_t i = 1; i < count; ++i) {
            UChar32 start = getRangeEnd(i-1)+1;
            UChar32 end = getRangeStart(i)-1;
            _appendToPat(result, start, escapeUnprintable);
            if (start != end) {
                if ((start+1) != end) {
                    result.append(HYPHEN);
                }
                _appendToPat(result, end, escapeUnprintable);
            }
        }
    }

    // Default; emit the ranges as pairs
    else {
        for (int32_t i = 0; i < count; ++i) {
            UChar32 start = getRangeStart(i);
            UChar32 end = getRangeEnd(i);
            _appendToPat(result, start, escapeUnprintable);
            if (start != end) {
                if ((start+1) != end) {
                    result.append(HYPHEN);
                }
                _appendToPat(result, end, escapeUnprintable);
            }
        }
    }
    
    for (int32_t i = 0; i<strings->size(); ++i) {
        result.append(OPEN_BRACE);
        _appendToPat(result,
                     *(const UnicodeString*) strings->elementAt(i),
                     escapeUnprintable);
        result.append(CLOSE_BRACE);
    }
    return result.append(SET_CLOSE);
}

/**
 * Returns the number of elements in this set (its cardinality),
 * <em>n</em>, where <code>0 <= </code><em>n</em><code> <= 65536</code>.
 *
 * @return the number of elements in this set (its cardinality).
 */
int32_t UnicodeSet::size(void) const {
    int32_t n = 0;
    int32_t count = getRangeCount();
    for (int32_t i = 0; i < count; ++i) {
        n += getRangeEnd(i) - getRangeStart(i) + 1;
    }
    return n + strings->size();
}

/**
 * Returns <tt>true</tt> if this set contains no elements.
 *
 * @return <tt>true</tt> if this set contains no elements.
 */
UBool UnicodeSet::isEmpty(void) const {
    return len == 1 && strings->size() == 0;
}

/**
 * Returns true if this set contains the given character.
 * @param c character to be checked for containment
 * @return true if the test condition is met
 */
UBool UnicodeSet::contains(UChar32 c) const {
    // Set i to the index of the start item greater than ch
    // We know we will terminate without length test!
    // LATER: for large sets, add binary search
    //int32_t i = -1;
    //for (;;) {
    //    if (c < list[++i]) break;
    //}
    if (c >= UNICODESET_HIGH) { // Don't need to check LOW bound
        return FALSE;
    }
    int32_t i = findCodePoint(c);
    return ((i & 1) != 0); // return true if odd
}

/**
 * Returns the smallest value i such that c < list[i].  Caller
 * must ensure that c is a legal value or this method will enter
 * an infinite loop.  This method performs a binary search.
 * @param c a character in the range MIN_VALUE..MAX_VALUE
 * inclusive
 * @return the smallest integer i in the range 0..len-1,
 * inclusive, such that c < list[i]
 */
int32_t UnicodeSet::findCodePoint(UChar32 c) const {
    /* Examples:
                                       findCodePoint(c)
       set              list[]         c=0 1 3 4 7 8
       ===              ==============   ===========
       []               [110000]         0 0 0 0 0 0
       [\u0000-\u0003]  [0, 4, 110000]   1 1 1 2 2 2
       [\u0004-\u0007]  [4, 8, 110000]   0 0 0 1 1 2
       [:Any:]          [0, 110000]      1 1 1 1 1 1
     */

    // Return the smallest i such that c < list[i].  Assume
    // list[len - 1] == HIGH and that c is legal (0..HIGH-1).
    if (c < list[0]) return 0;
    // High runner test.  c is often after the last range, so an
    // initial check for this condition pays off. 
    if (len >= 2 && c >= list[len-2]) return len-1;
    int32_t lo = 0;
    int32_t hi = len - 1;
    // invariant: c >= list[lo]
    // invariant: c < list[hi]
    for (;;) {
        int32_t i = (lo + hi) >> 1;
        if (i == lo) return hi;
        if (c < list[i]) {
            hi = i;
        } else {
            lo = i;
        }
    }
    return 0; // To make compiler happy; never reached
}

/**
 * Returns true if this set contains every character
 * of the given range.
 * @param start first character, inclusive, of the range
 * @param end last character, inclusive, of the range
 * @return true if the test condition is met
 */
UBool UnicodeSet::contains(UChar32 start, UChar32 end) const {
    //int32_t i = -1;
    //for (;;) {
    //    if (start < list[++i]) break;
    //}
    int32_t i = findCodePoint(start);
    return ((i & 1) != 0 && end < list[i]);
}

/**
 * Returns <tt>true</tt> if this set contains the given
 * multicharacter string.
 * @param s string to be checked for containment
 * @return <tt>true</tt> if this set contains the specified string
 */
UBool UnicodeSet::contains(const UnicodeString& s) const {
    if (s.length() == 0) return FALSE;
    int32_t cp = getSingleCP(s);
    if (cp < 0) {
        return strings->contains((void*) &s);
    } else {
        return contains((UChar32) cp);
    }
}

/**
 * Returns true if this set contains all the characters and strings
 * of the given set.
 * @param c set to be checked for containment
 * @return true if the test condition is met
 */
UBool UnicodeSet::containsAll(const UnicodeSet& c) const {
    // The specified set is a subset if all of its pairs are contained in
    // this set.  It's possible to code this more efficiently in terms of
    // direct manipulation of the inversion lists if the need arises.
    int32_t n = c.getRangeCount();
    for (int i=0; i<n; ++i) {
        if (!contains(c.getRangeStart(i), c.getRangeEnd(i))) {
            return FALSE;
        }
    }
    if (!strings->containsAll(*c.strings)) return FALSE;
    return TRUE;
}
    
/**
 * Returns true if this set contains all the characters
 * of the given string.
 * @param s string containing characters to be checked for containment
 * @return true if the test condition is met
 */
UBool UnicodeSet::containsAll(const UnicodeString& s) const {
    UChar32 cp;
    for (int32_t i = 0; i < s.length(); i += UTF_CHAR_LENGTH(cp)) {
        cp = s.char32At(i);
        if (!contains(cp)) return FALSE;
    }
    return TRUE;
}
    
/**
 * Returns true if this set contains none of the characters
 * of the given range.
 * @param start first character, inclusive, of the range
 * @param end last character, inclusive, of the range
 * @return true if the test condition is met
 */
UBool UnicodeSet::containsNone(UChar32 start, UChar32 end) const {
    //int32_t i = -1;
    //for (;;) {
    //    if (start < list[++i]) break;
    //}
    int32_t i = findCodePoint(start);
    return ((i & 1) == 0 && end < list[i]);
}

/**
 * Returns true if this set contains none of the characters and strings
 * of the given set.
 * @param c set to be checked for containment
 * @return true if the test condition is met
 */
UBool UnicodeSet::containsNone(const UnicodeSet& c) const {
    // The specified set is a subset if all of its pairs are contained in
    // this set.  It's possible to code this more efficiently in terms of
    // direct manipulation of the inversion lists if the need arises.
    int32_t n = c.getRangeCount();
    for (int32_t i=0; i<n; ++i) {
        if (!containsNone(c.getRangeStart(i), c.getRangeEnd(i))) {
            return FALSE;
        }
    }
    if (!strings->containsNone(*c.strings)) return FALSE;
    return TRUE;
}
    
/**
 * Returns true if this set contains none of the characters
 * of the given string.
 * @param s string containing characters to be checked for containment
 * @return true if the test condition is met
 */
UBool UnicodeSet::containsNone(const UnicodeString& s) const {
    UChar32 cp;
    for (int32_t i = 0; i < s.length(); i += UTF_CHAR_LENGTH(cp)) {
        cp = s.char32At(i);
        if (contains(cp)) return FALSE;
    }
    return TRUE;
}

/**
 * Returns <tt>true</tt> if this set contains any character whose low byte
 * is the given value.  This is used by <tt>RuleBasedTransliterator</tt> for
 * indexing.
 */
UBool UnicodeSet::matchesIndexValue(uint8_t v) const {
    /* The index value v, in the range [0,255], is contained in this set if
     * it is contained in any pair of this set.  Pairs either have the high
     * bytes equal, or unequal.  If the high bytes are equal, then we have
     * aaxx..aayy, where aa is the high byte.  Then v is contained if xx <=
     * v <= yy.  If the high bytes are unequal we have aaxx..bbyy, bb>aa.
     * Then v is contained if xx <= v || v <= yy.  (This is identical to the
     * time zone month containment logic.)
     */
    int32_t i;
    for (i=0; i<getRangeCount(); ++i) {
        UChar32 low = getRangeStart(i);
        UChar32 high = getRangeEnd(i);
        if ((low & ~0xFF) == (high & ~0xFF)) {
            if ((low & 0xFF) <= v && v <= (high & 0xFF)) {
                return TRUE;
            }
        } else if ((low & 0xFF) <= v || v <= (high & 0xFF)) {
            return TRUE;
        }
    }
    if (strings->size() != 0) {
        for (i=0; i<strings->size(); ++i) {
            const UnicodeString& s = *(const UnicodeString*)strings->elementAt(i);
            //if (s.length() == 0) {
            //    // Empty strings match everything
            //    return TRUE;
            //}
            // assert(s.length() != 0); // We enforce this elsewhere
            UChar32 c = s.char32At(0);
            if ((c & 0xFF) == v) {
                return TRUE;
            }
        }
    }
    return FALSE;
}

/**
 * Implementation of UnicodeMatcher::matches().  Always matches the 
 * longest possible multichar string. 
 */
UMatchDegree UnicodeSet::matches(const Replaceable& text,
                                 int32_t& offset,
                                 int32_t limit,
                                 UBool incremental) {
    if (offset == limit) {
        // Strings, if any, have length != 0, so we don't worry
        // about them here.  If we ever allow zero-length strings
        // we much check for them here.
        if (contains(U_ETHER)) {
            return incremental ? U_PARTIAL_MATCH : U_MATCH;
        } else {
            return U_MISMATCH;
        }
    } else {
        if (strings->size() != 0) { // try strings first
            
            // might separate forward and backward loops later
            // for now they are combined

            // TODO Improve efficiency of this, at least in the forward
            // direction, if not in both.  In the forward direction we
            // can assume the strings are sorted.

            int32_t i;
            UBool forward = offset < limit;

            // firstChar is the leftmost char to match in the
            // forward direction or the rightmost char to match in
            // the reverse direction.
            UChar firstChar = text.charAt(offset);

            // If there are multiple strings that can match we
            // return the longest match.
            int32_t highWaterLength = 0;

            for (i=0; i<strings->size(); ++i) {
                const UnicodeString& trial = *(const UnicodeString*)strings->elementAt(i);

                //if (trial.length() == 0) {
                //    return U_MATCH; // null-string always matches
                //}
                // assert(trial.length() != 0); // We ensure this elsewhere

                UChar c = trial.charAt(forward ? 0 : trial.length() - 1);

                // Strings are sorted, so we can optimize in the
                // forward direction.
                if (forward && c > firstChar) break;
                if (c != firstChar) continue;
                        
                int32_t matchLen = matchRest(text, offset, limit, trial);

                if (incremental) {
                    int32_t maxLen = forward ? limit-offset : offset-limit;
                    if (matchLen == maxLen) {
                        // We have successfully matched but only up to limit.
                        return U_PARTIAL_MATCH;
                    }
                }

                if (matchLen == trial.length()) {
                    // We have successfully matched the whole string.
                    if (matchLen > highWaterLength) {
                        highWaterLength = matchLen;
                    }
                    // In the forward direction we know strings
                    // are sorted so we can bail early.
                    if (forward && matchLen < highWaterLength) {
                        break;
                    }
                    continue;
                }
            }

            // We've checked all strings without a partial match.
            // If we have full matches, return the longest one.
            if (highWaterLength != 0) {
                offset += forward ? highWaterLength : -highWaterLength;
                return U_MATCH;
            }
        }
        return UnicodeFilter::matches(text, offset, limit, incremental);
    }
}

/**
 * Returns the longest match for s in text at the given position.
 * If limit > start then match forward from start+1 to limit
 * matching all characters except s.charAt(0).  If limit < start,
 * go backward starting from start-1 matching all characters
 * except s.charAt(s.length()-1).  This method assumes that the
 * first character, text.charAt(start), matches s, so it does not
 * check it.
 * @param text the text to match
 * @param start the first character to match.  In the forward
 * direction, text.charAt(start) is matched against s.charAt(0).
 * In the reverse direction, it is matched against
 * s.charAt(s.length()-1).
 * @param limit the limit offset for matching, either last+1 in
 * the forward direction, or last-1 in the reverse direction,
 * where last is the index of the last character to match.
 * @return If part of s matches up to the limit, return |limit -
 * start|.  If all of s matches before reaching the limit, return
 * s.length().  If there is a mismatch between s and text, return
 * 0
 */
int32_t UnicodeSet::matchRest(const Replaceable& text,
                              int32_t start, int32_t limit,
                              const UnicodeString& s) {
    int32_t i;
    int32_t maxLen;
    int32_t slen = s.length();
    if (start < limit) {
        maxLen = limit - start;
        if (maxLen > slen) maxLen = slen;
        for (i = 1; i < maxLen; ++i) {
            if (text.charAt(start + i) != s.charAt(i)) return 0;
        }
    } else {
        maxLen = start - limit;
        if (maxLen > slen) maxLen = slen;
        --slen; // <=> slen = s.length() - 1;
        for (i = 1; i < maxLen; ++i) {
            if (text.charAt(start - i) != s.charAt(slen - i)) return 0;
        }
    }
    return maxLen;
}

/**
 * Implement of UnicodeMatcher
 */
void UnicodeSet::addMatchSetTo(UnicodeSet& toUnionTo) const {
    toUnionTo.addAll(*this);
}

/**
 * Returns the index of the given character within this set, where
 * the set is ordered by ascending code point.  If the character
 * is not in this set, return -1.  The inverse of this method is
 * <code>charAt()</code>.
 * @return an index from 0..size()-1, or -1
 */
int32_t UnicodeSet::indexOf(UChar32 c) const {
    if (c < MIN_VALUE || c > MAX_VALUE) {
        return -1;
    }
    int32_t i = 0;
    int32_t n = 0;
    for (;;) {
        UChar32 start = list[i++];
        if (c < start) {
            return -1;
        }
        UChar32 limit = list[i++];
        if (c < limit) {
            return n + c - start;
        }
        n += limit - start;
    }
}

/**
 * Returns the character at the given index within this set, where
 * the set is ordered by ascending code point.  If the index is
 * out of range, return (UChar32)-1.  The inverse of this method is
 * <code>indexOf()</code>.
 * @param index an index from 0..size()-1
 * @return the character at the given index, or (UChar32)-1.
 */
UChar32 UnicodeSet::charAt(int32_t index) const {
    if (index >= 0) {
        // len2 is the largest even integer <= len, that is, it is len
        // for even values and len-1 for odd values.  With odd values
        // the last entry is UNICODESET_HIGH.
        int32_t len2 = len & ~1;
        for (int32_t i=0; i < len2;) {
            UChar32 start = list[i++];
            int32_t count = list[i++] - start;
            if (index < count) {
                return (UChar32)(start + index);
            }
            index -= count;
        }
    }
    return (UChar32)-1;
}

/**
 * Adds the specified range to this set if it is not already
 * present.  If this set already contains the specified range,
 * the call leaves this set unchanged.  If <code>end > start</code>
 * then an empty range is added, leaving the set unchanged.
 *
 * @param start first character, inclusive, of range to be added
 * to this set.
 * @param end last character, inclusive, of range to be added
 * to this set.
 */
UnicodeSet& UnicodeSet::add(UChar32 start, UChar32 end) {
    if (pinCodePoint(start) < pinCodePoint(end)) {
        UChar32 range[3] = { start, end+1, UNICODESET_HIGH };
        add(range, 2, 0);
    } else if (start == end) {
        add(start);
    }
    return *this;
}

// #define DEBUG_US_ADD

#ifdef DEBUG_US_ADD
#include <stdio.h>
void dump(UChar32 c) {
    if (c <= 0xFF) {
        printf("%c", (char)c);
    } else {
        printf("U+%04X", c);
    }
}
void dump(const UChar32* list, int32_t len) {
    printf("[");
    for (int32_t i=0; i<len; ++i) {
        if (i != 0) printf(", ");
        dump(list[i]);
    }
    printf("]");
}
#endif

/**
 * Adds the specified character to this set if it is not already
 * present.  If this set already contains the specified character,
 * the call leaves this set unchanged.
 */
UnicodeSet& UnicodeSet::add(UChar32 c) {
    // find smallest i such that c < list[i]
    // if odd, then it is IN the set
    // if even, then it is OUT of the set
    int32_t i = findCodePoint(pinCodePoint(c));

    // already in set?
    if ((i & 1) != 0) return *this;
    
    // HIGH is 0x110000
    // assert(list[len-1] == HIGH);
    
    // empty = [HIGH]
    // [start_0, limit_0, start_1, limit_1, HIGH]
    
    // [..., start_k-1, limit_k-1, start_k, limit_k, ..., HIGH]
    //                             ^
    //                             list[i]

    // i == 0 means c is before the first range

#ifdef DEBUG_US_ADD
    printf("Add of ");
    dump(c);
    printf(" found at %d", i);
    printf(": ");
    dump(list, len);
    printf(" => ");
#endif

    if (c == list[i]-1) {
        // c is before start of next range
        list[i] = c;
        // if we touched the HIGH mark, then add a new one
        if (c == (UNICODESET_HIGH - 1)) {
            ensureCapacity(len+1);
            list[len++] = UNICODESET_HIGH;
        }
        if (i > 0 && c == list[i-1]) {
            // collapse adjacent ranges

            // [..., start_k-1, c, c, limit_k, ..., HIGH]
            //                     ^
            //                     list[i]

            //for (int32_t k=i-1; k<len-2; ++k) {
            //    list[k] = list[k+2];
            //}
            UChar32* dst = list + i - 1;
            UChar32* src = dst + 2;
            UChar32* srclimit = list + len;
            while (src < srclimit) *(dst++) = *(src++);

            len -= 2;
        }
    }

    else if (i > 0 && c == list[i-1]) {
        // c is after end of prior range
        list[i-1]++;
        // no need to check for collapse here
    }

    else {
        // At this point we know the new char is not adjacent to
        // any existing ranges, and it is not 10FFFF.


        // [..., start_k-1, limit_k-1, start_k, limit_k, ..., HIGH]
        //                             ^
        //                             list[i]

        // [..., start_k-1, limit_k-1, c, c+1, start_k, limit_k, ..., HIGH]
        //                             ^
        //                             list[i]

        ensureCapacity(len+2);

        //for (int32_t k=len-1; k>=i; --k) {
        //    list[k+2] = list[k];
        //}
        UChar32* src = list + len;
        UChar32* dst = src + 2;
        UChar32* srclimit = list + i;
        while (src > srclimit) *(--dst) = *(--src);

        list[i] = c;
        list[i+1] = c+1;
        len += 2;
    }
     
#ifdef DEBUG_US_ADD
    dump(list, len);
    printf("\n");

    for (i=1; i<len; ++i) {
        if (list[i] <= list[i-1]) {
            // Corrupt array!
            printf("ERROR: list has been corrupted\n");
            exit(1);
        }
    } 
#endif
  
    pat.truncate(0);
    return *this;
}

/**
 * Adds the specified multicharacter to this set if it is not already
 * present.  If this set already contains the multicharacter,
 * the call leaves this set unchanged.
 * Thus "ch" => {"ch"}
 * <br><b>Warning: you cannot add an empty string ("") to a UnicodeSet.</b>
 * @param s the source string
 * @return the modified set, for chaining
 */
UnicodeSet& UnicodeSet::add(const UnicodeString& s) {
    if (s.length() == 0) return *this;
    int32_t cp = getSingleCP(s);
    if (cp < 0) {
        if (!strings->contains((void*) &s)) {
            _add(s);
            pat.truncate(0);
        }
    } else {
        add((UChar32)cp, (UChar32)cp);
    }
    return *this;
}

/**
 * Adds the given string, in order, to 'strings'.  The given string
 * must have been checked by the caller to not be empty and to not
 * already be in 'strings'.
 */
void UnicodeSet::_add(const UnicodeString& s) {
    UnicodeString* t = new UnicodeString(s);
    UErrorCode ec = U_ZERO_ERROR;
    strings->sortedInsert(t, compareUnicodeString, ec);
}

/**
 * @return a code point IF the string consists of a single one.
 * otherwise returns -1.
 * @param string to test
 */
int32_t UnicodeSet::getSingleCP(const UnicodeString& s) {
    //if (s.length() < 1) {
    //    throw new IllegalArgumentException("Can't use zero-length strings in UnicodeSet");
    //}
    if (s.length() > 2) return -1;
    if (s.length() == 1) return s.charAt(0);

    // at this point, len = 2
    UChar32 cp = s.char32At(0);
    if (cp > 0xFFFF) { // is surrogate pair
        return cp;
    }
    return -1;
}

/**
 * Adds each of the characters in this string to the set. Thus "ch" => {"c", "h"}
 * If this set already any particular character, it has no effect on that character.
 * @param the source string
 * @return the modified set, for chaining
 */
UnicodeSet& UnicodeSet::addAll(const UnicodeString& s) {
    UChar32 cp;
    for (int32_t i = 0; i < s.length(); i += UTF_CHAR_LENGTH(cp)) {
        cp = s.char32At(i);
        add(cp, cp);
    }
    return *this;
}

/**
 * Retains EACH of the characters in this string. Note: "ch" == {"c", "h"}
 * If this set already any particular character, it has no effect on that character.
 * @param the source string
 * @return the modified set, for chaining
 */
UnicodeSet& UnicodeSet::retainAll(const UnicodeString& s) {
    UnicodeSet set;
    set.addAll(s);
    retainAll(set);
    return *this;
}

/**
 * Complement EACH of the characters in this string. Note: "ch" == {"c", "h"}
 * If this set already any particular character, it has no effect on that character.
 * @param the source string
 * @return the modified set, for chaining
 */
UnicodeSet& UnicodeSet::complementAll(const UnicodeString& s) {
    UnicodeSet set;
    set.addAll(s);
    complementAll(set);
    return *this;
}

/**
 * Remove EACH of the characters in this string. Note: "ch" == {"c", "h"}
 * If this set already any particular character, it has no effect on that character.
 * @param the source string
 * @return the modified set, for chaining
 */
UnicodeSet& UnicodeSet::removeAll(const UnicodeString& s) {
    UnicodeSet set;
    set.addAll(s);
    removeAll(set);
    return *this;
}

/**
 * Makes a set from a multicharacter string. Thus "ch" => {"ch"}
 * <br><b>Warning: you cannot add an empty string ("") to a UnicodeSet.</b>
 * @param the source string
 * @return a newly created set containing the given string
 */
UnicodeSet* UnicodeSet::createFrom(const UnicodeString& s) {
    UnicodeSet *set = new UnicodeSet();
    set->add(s);
    return set;
}


/**
 * Makes a set from each of the characters in the string. Thus "ch" => {"c", "h"}
 * @param the source string
 * @return a newly created set containing the given characters
 */
UnicodeSet* UnicodeSet::createFromAll(const UnicodeString& s) {
    UnicodeSet *set = new UnicodeSet();
    set->addAll(s);
    return set;
}

/**
 * Retain only the elements in this set that are contained in the
 * specified range.  If <code>end > start</code> then an empty range is
 * retained, leaving the set empty.
 *
 * @param start first character, inclusive, of range to be retained
 * to this set.
 * @param end last character, inclusive, of range to be retained
 * to this set.
 */
UnicodeSet& UnicodeSet::retain(UChar32 start, UChar32 end) {
    if (pinCodePoint(start) <= pinCodePoint(end)) {
        UChar32 range[3] = { start, end+1, UNICODESET_HIGH };
        retain(range, 2, 0);
    } else {
        clear();
    }
    return *this;
}

UnicodeSet& UnicodeSet::retain(UChar32 c) {
    return retain(c, c);
}

/**
 * Removes the specified range from this set if it is present.
 * The set will not contain the specified range once the call
 * returns.  If <code>end > start</code> then an empty range is
 * removed, leaving the set unchanged.
 * 
 * @param start first character, inclusive, of range to be removed
 * from this set.
 * @param end last character, inclusive, of range to be removed
 * from this set.
 */
UnicodeSet& UnicodeSet::remove(UChar32 start, UChar32 end) {
    if (pinCodePoint(start) <= pinCodePoint(end)) {
        UChar32 range[3] = { start, end+1, UNICODESET_HIGH };
        retain(range, 2, 2);
    }
    return *this;
}

/**
 * Removes the specified character from this set if it is present.
 * The set will not contain the specified range once the call
 * returns.
 */
UnicodeSet& UnicodeSet::remove(UChar32 c) {
    return remove(c, c);
}

/**
 * Removes the specified string from this set if it is present.
 * The set will not contain the specified character once the call
 * returns.
 * @param the source string
 * @return the modified set, for chaining
 */
UnicodeSet& UnicodeSet::remove(const UnicodeString& s) {
    if (s.length() == 0) return *this;
    int32_t cp = getSingleCP(s);
    if (cp < 0) {
        strings->removeElement((void*) &s);
        pat.truncate(0);
    } else {
        remove((UChar32)cp, (UChar32)cp);
    }
    return *this;
}

/**
 * Complements the specified range in this set.  Any character in
 * the range will be removed if it is in this set, or will be
 * added if it is not in this set.  If <code>end > start</code>
 * then an empty range is xor'ed, leaving the set unchanged.
 *
 * @param start first character, inclusive, of range to be removed
 * from this set.
 * @param end last character, inclusive, of range to be removed
 * from this set.
 */
UnicodeSet& UnicodeSet::complement(UChar32 start, UChar32 end) {
    if (pinCodePoint(start) <= pinCodePoint(end)) {
        UChar32 range[3] = { start, end+1, UNICODESET_HIGH };
        exclusiveOr(range, 2, 0);
    }
    pat.truncate(0);
    return *this;
}

UnicodeSet& UnicodeSet::complement(UChar32 c) {
    return complement(c, c);
}

/**
 * This is equivalent to
 * <code>complement(MIN_VALUE, MAX_VALUE)</code>.
 */
UnicodeSet& UnicodeSet::complement(void) {
    if (list[0] == UNICODESET_LOW) { 
        ensureBufferCapacity(len-1);
        uprv_memcpy(buffer, list + 1, (len-1)*sizeof(UChar32));
        --len;
    } else {
        ensureBufferCapacity(len+1);
        uprv_memcpy(buffer + 1, list, len*sizeof(UChar32));
        buffer[0] = UNICODESET_LOW;
        ++len;
    }
    swapBuffers();
    pat.truncate(0);
    return *this;
}

/**
 * Complement the specified string in this set.
 * The set will not contain the specified string once the call
 * returns.
 * <br><b>Warning: you cannot add an empty string ("") to a UnicodeSet.</b>
 * @param s the string to complement
 * @return this object, for chaining
 */
UnicodeSet& UnicodeSet::complement(const UnicodeString& s) {
    if (s.length() == 0) return *this;
    int32_t cp = getSingleCP(s);
    if (cp < 0) {
        if (strings->contains((void*) &s)) {
            strings->removeElement((void*) &s);
        } else {
            _add(s);
        }
        pat.truncate(0);
    } else {
        complement((UChar32)cp, (UChar32)cp);
    }
    return *this;
}    

/**
 * Adds all of the elements in the specified set to this set if
 * they're not already present.  This operation effectively
 * modifies this set so that its value is the <i>union</i> of the two
 * sets.  The behavior of this operation is unspecified if the specified
 * collection is modified while the operation is in progress.
 *
 * @param c set whose elements are to be added to this set.
 * @see #add(char, char)
 */
UnicodeSet& UnicodeSet::addAll(const UnicodeSet& c) {
    add(c.list, c.len, 0);

    // Add strings in order 
    for (int32_t i=0; i<c.strings->size(); ++i) {
        const UnicodeString* s = (const UnicodeString*)c.strings->elementAt(i);
        if (!strings->contains((void*) s)) {
            _add(*s);
        }
    }
    return *this;
}

/**
 * Retains only the elements in this set that are contained in the
 * specified set.  In other words, removes from this set all of
 * its elements that are not contained in the specified set.  This
 * operation effectively modifies this set so that its value is
 * the <i>intersection</i> of the two sets.
 *
 * @param c set that defines which elements this set will retain.
 */
UnicodeSet& UnicodeSet::retainAll(const UnicodeSet& c) {
    retain(c.list, c.len, 0);
    strings->retainAll(*c.strings);
    return *this;
}

/**
 * Removes from this set all of its elements that are contained in the
 * specified set.  This operation effectively modifies this
 * set so that its value is the <i>asymmetric set difference</i> of
 * the two sets.
 *
 * @param c set that defines which elements will be removed from
 *          this set.
 */
UnicodeSet& UnicodeSet::removeAll(const UnicodeSet& c) {
    retain(c.list, c.len, 2);
    strings->removeAll(*c.strings);
    return *this;
}

/**
 * Complements in this set all elements contained in the specified
 * set.  Any character in the other set will be removed if it is
 * in this set, or will be added if it is not in this set.
 *
 * @param c set that defines which elements will be xor'ed from
 *          this set.
 */
UnicodeSet& UnicodeSet::complementAll(const UnicodeSet& c) {
    exclusiveOr(c.list, c.len, 0);

    for (int32_t i=0; i<c.strings->size(); ++i) {
        void* e = c.strings->elementAt(i);
        if (!strings->removeElement(e)) {
            _add(*(const UnicodeString*)e);
        }
    }
    return *this;
}

/**
 * Removes all of the elements from this set.  This set will be
 * empty after this call returns.
 */
UnicodeSet& UnicodeSet::clear(void) {
    list[0] = UNICODESET_HIGH;
    len = 1;
    pat.truncate(0);
    strings->removeAllElements();
    return *this;
}

/**
 * Iteration method that returns the number of ranges contained in
 * this set.
 * @see #getRangeStart
 * @see #getRangeEnd
 */
int32_t UnicodeSet::getRangeCount() const {
    return len/2;
}

/**
 * Iteration method that returns the first character in the
 * specified range of this set.
 * @see #getRangeCount
 * @see #getRangeEnd
 */
UChar32 UnicodeSet::getRangeStart(int32_t index) const {
    return list[index*2];
}

/**
 * Iteration method that returns the last character in the
 * specified range of this set.
 * @see #getRangeStart
 * @see #getRangeEnd
 */
UChar32 UnicodeSet::getRangeEnd(int32_t index) const {
    return list[index*2 + 1] - 1;
}

int32_t UnicodeSet::getStringCount() const {
    return strings->size();
}

const UnicodeString* UnicodeSet::getString(int32_t index) const {
    return (const UnicodeString*) strings->elementAt(index);
}

/**
 * Reallocate this objects internal structures to take up the least
 * possible space, without changing this object's value.
 */
UnicodeSet& UnicodeSet::compact() {
    if (len != capacity) {
        capacity = len;
        UChar32* temp = (UChar32*) uprv_malloc(sizeof(UChar32) * capacity);
        uprv_memcpy(temp, list, len*sizeof(UChar32));
        uprv_free(list);
        list = temp;
    }
    uprv_free(buffer);
    buffer = NULL;
    return *this;
}

int32_t UnicodeSet::serialize(uint16_t *dest, int32_t destCapacity, UErrorCode& ec) const {
    int32_t bmpLength, length, destLength;

    if (U_FAILURE(ec)) {
        return 0;
    }

    if (destCapacity<0 || (destCapacity>0 && dest==NULL)) {
        ec=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    /* count necessary 16-bit units */
    length=this->len-1; // Subtract 1 to ignore final UNICODESET_HIGH
    // assert(length>=0);
    if (length==0) {
        /* empty set */
        if (destCapacity>0) {
            *dest=0;
        } else {
            ec=U_BUFFER_OVERFLOW_ERROR;
        }
        return 1;
    }
    /* now length>0 */

    if (this->list[length-1]<=0xffff) {
        /* all BMP */
        bmpLength=length;
    } else if (this->list[0]>=0x10000) {
        /* all supplementary */
        bmpLength=0;
        length*=2;
    } else {
        /* some BMP, some supplementary */
        for (bmpLength=0; bmpLength<length && this->list[bmpLength]<=0xffff; ++bmpLength) {}
        length=bmpLength+2*(length-bmpLength);
    }

    /* length: number of 16-bit array units */
    if (length>0x7fff) {
        /* there are only 15 bits for the length in the first serialized word */
        ec=U_INDEX_OUTOFBOUNDS_ERROR;
        return 0;
    }

    /*
     * total serialized length:
     * number of 16-bit array units (length) +
     * 1 length unit (always) +
     * 1 bmpLength unit (if there are supplementary values)
     */
    destLength=length+((length>bmpLength)?2:1);
    if (destLength<=destCapacity) {
        const UChar32 *p;
        int32_t i;

        *dest=(uint16_t)length;
        if (length>bmpLength) {
            *dest|=0x8000;
            *++dest=(uint16_t)bmpLength;
        }
        ++dest;

        /* write the BMP part of the array */
        p=this->list;
        for (i=0; i<bmpLength; ++i) {
            *dest++=(uint16_t)*p++;
        }

        /* write the supplementary part of the array */
        for (; i<length; i+=2) {
            *dest++=(uint16_t)(*p>>16);
            *dest++=(uint16_t)*p++;
        }
    } else {
        ec=U_BUFFER_OVERFLOW_ERROR;
    }
    return destLength;
}

//----------------------------------------------------------------
// Implementation: Pattern parsing
//----------------------------------------------------------------

/**
 * A small all-inline class to manage a UnicodeSet pointer.  Add
 * operator->() etc. as needed.
 */
class UnicodeSetPointer {
    UnicodeSet* p;
public:
    inline UnicodeSetPointer() : p(0) {}
    inline ~UnicodeSetPointer() { delete p; }
    inline UnicodeSet* pointer() { return p; }
    inline UBool allocate() {
        if (p == 0) {
            p = new UnicodeSet();
        }
        return p != 0;
    }
};

/**
 * Parse the pattern from the given RuleCharacterIterator.  The
 * iterator is advanced over the parsed pattern.
 * @param chars iterator over the pattern characters.  Upon return
 * it will be advanced to the first character after the parsed
 * pattern, or the end of the iteration if all characters are
 * parsed.
 * @param symbols symbol table to use to parse and dereference
 * variables, or null if none.
 * @param rebuiltPat the pattern that was parsed, rebuilt or
 * copied from the input pattern, as appropriate.
 * @param options a bit mask of zero or more of the following:
 * IGNORE_SPACE, CASE.
 */
void UnicodeSet::applyPattern(RuleCharacterIterator& chars,
                              const SymbolTable* symbols,
                              UnicodeString& rebuiltPat,
                              uint32_t options,
                              UErrorCode& ec) {
    if (U_FAILURE(ec)) return;

    // Syntax characters: [ ] ^ - & { }

    // Recognized special forms for chars, sets: c-c s-s s&s

    int32_t opts = RuleCharacterIterator::PARSE_VARIABLES |
                   RuleCharacterIterator::PARSE_ESCAPES;
    if ((options & USET_IGNORE_SPACE) != 0) {
        opts |= RuleCharacterIterator::SKIP_WHITESPACE;
    }

    UnicodeString patLocal, buf;
    UBool usePat = FALSE;
    UnicodeSetPointer scratch;
    RuleCharacterIterator::Pos backup;

    // mode: 0=before [, 1=between [...], 2=after ]
    // lastItem: 0=none, 1=char, 2=set
    int8_t lastItem = 0, mode = 0;
    UChar32 lastChar = 0;
    UChar op = 0;

    UBool invert = FALSE;

    clear();

    while (mode != 2 && !chars.atEnd()) {
        U_ASSERT((lastItem == 0 && op == 0) ||
                 (lastItem == 1 && (op == 0 || op == HYPHEN /*'-'*/)) ||
                 (lastItem == 2 && (op == 0 || op == HYPHEN /*'-'*/ ||
                                    op == INTERSECTION /*'&'*/)));

        UChar32 c = 0;
        UBool literal = FALSE;
        UnicodeSet* nested = 0; // alias - do not delete

        // -------- Check for property pattern

        // setMode: 0=none, 1=unicodeset, 2=propertypat, 3=preparsed
        int8_t setMode = 0;
        if (resemblesPropertyPattern(chars, opts)) {
            setMode = 2;
        }

        // -------- Parse '[' of opening delimiter OR nested set.
        // If there is a nested set, use `setMode' to define how
        // the set should be parsed.  If the '[' is part of the
        // opening delimiter for this pattern, parse special
        // strings "[", "[^", "[-", and "[^-".  Check for stand-in
        // characters representing a nested set in the symbol
        // table.

        else {
            // Prepare to backup if necessary
            chars.getPos(backup);
            c = chars.next(opts, literal, ec);
            if (U_FAILURE(ec)) return;

            if (c == 0x5B /*'['*/ && !literal) {
                if (mode == 1) {
                    chars.setPos(backup); // backup
                    setMode = 1;
                } else {
                    // Handle opening '[' delimiter
                    mode = 1;
                    patLocal.append((UChar) 0x5B /*'['*/);
                    chars.getPos(backup); // prepare to backup
                    c = chars.next(opts, literal, ec); 
                    if (U_FAILURE(ec)) return;
                    if (c == 0x5E /*'^'*/ && !literal) {
                        invert = TRUE;
                        patLocal.append((UChar) 0x5E /*'^'*/);
                        chars.getPos(backup); // prepare to backup
                        c = chars.next(opts, literal, ec);
                        if (U_FAILURE(ec)) return;
                    }
                    // Fall through to handle special leading '-';
                    // otherwise restart loop for nested [], \p{}, etc.
                    if (c == HYPHEN /*'-'*/) {
                        literal = TRUE;
                        // Fall through to handle literal '-' below
                    } else {
                        chars.setPos(backup); // backup
                        continue;
                    }
                }
            } else if (symbols != 0) {
                const UnicodeFunctor *m = symbols->lookupMatcher(c);
                if (m != 0) {
                    if (m->getDynamicClassID() != UnicodeSet::getStaticClassID()) {
                        ec = U_MALFORMED_SET;
                        return;
                    }
                    // casting away const, but `nested' won't be modified
                    // (important not to modify stored set)
                    nested = (UnicodeSet*) m;
                    setMode = 3;
                }
            }
        }

        // -------- Handle a nested set.  This either is inline in
        // the pattern or represented by a stand-in that has
        // previously been parsed and was looked up in the symbol
        // table.

        if (setMode != 0) {
            if (lastItem == 1) {
                if (op != 0) {
                    // syntaxError(chars, "Char expected after operator");
                    ec = U_MALFORMED_SET;
                    return;
                }
                add(lastChar, lastChar);
                _appendToPat(patLocal, lastChar, FALSE);
                lastItem = 0;
                op = 0;
            }

            if (op == HYPHEN /*'-'*/ || op == INTERSECTION /*'&'*/) {
                patLocal.append(op);
            }

            if (nested == 0) {
                // lazy allocation
                if (!scratch.allocate()) {
                    ec = U_MEMORY_ALLOCATION_ERROR;
                    return;
                }
                nested = scratch.pointer();
            }
            switch (setMode) {
            case 1:
                nested->applyPattern(chars, symbols, patLocal, options, ec);
                break;
            case 2:
                chars.skipIgnored(opts);
                nested->applyPropertyPattern(chars, patLocal, ec);
                if (U_FAILURE(ec)) return;
                break;
            case 3: // `nested' already parsed
                nested->_toPattern(patLocal, FALSE);
                break;
            }

            usePat = TRUE;

            if (mode == 0) {
                // Entire pattern is a category; leave parse loop
                *this = *nested;
                mode = 2;
                break;
            }

            switch (op) {
            case HYPHEN: /*'-'*/
                removeAll(*nested);
                break;
            case INTERSECTION: /*'&'*/
                retainAll(*nested);
                break;
            case 0:
                addAll(*nested);
                break;
            }

            op = 0;
            lastItem = 2;

            continue;
        }

        if (mode == 0) {
            // syntaxError(chars, "Missing '['");
            ec = U_MALFORMED_SET;
            return;
        }

        // -------- Parse special (syntax) characters.  If the
        // current character is not special, or if it is escaped,
        // then fall through and handle it below.

        if (!literal) {
            switch (c) {
            case 0x5D /*']'*/:
                if (lastItem == 1) {
                    add(lastChar, lastChar);
                    _appendToPat(patLocal, lastChar, FALSE);
                }
                // Treat final trailing '-' as a literal
                if (op == HYPHEN /*'-'*/) {
                    add(op, op);
                    patLocal.append(op);
                } else if (op == INTERSECTION /*'&'*/) {
                    // syntaxError(chars, "Trailing '&'");
                    ec = U_MALFORMED_SET;
                    return;
                }
                patLocal.append((UChar) 0x5D /*']'*/);
                mode = 2;
                continue;
            case HYPHEN /*'-'*/:
                if (op == 0) {
                    if (lastItem != 0) {
                        op = (UChar) c;
                        continue;
                    } else {
                        // Treat final trailing '-' as a literal
                        add(c, c);
                        c = chars.next(opts, literal, ec);
                        if (U_FAILURE(ec)) return;
                        if (c == 0x5D /*']'*/ && !literal) {
                            patLocal.append(HYPHEN_RIGHT_BRACE);
                            mode = 2;
                            continue;
                        }
                    }
                }
                // syntaxError(chars, "'-' not after char or set");
                ec = U_MALFORMED_SET;
                return;
            case INTERSECTION /*'&'*/:
                if (lastItem == 2 && op == 0) {
                    op = (UChar) c;
                    continue;
                }
                // syntaxError(chars, "'&' not after set");
                ec = U_MALFORMED_SET;
                return;
            case 0x5E /*'^'*/:
                // syntaxError(chars, "'^' not after '['");
                ec = U_MALFORMED_SET;
                return;
            case 0x7B /*'{'*/:
                if (op != 0) {
                    // syntaxError(chars, "Missing operand after operator");
                    ec = U_MALFORMED_SET;
                    return;
                }
                if (lastItem == 1) {
                    add(lastChar, lastChar);
                    _appendToPat(patLocal, lastChar, FALSE);
                }
                lastItem = 0;
                buf.truncate(0);
                {
                    UBool ok = FALSE;
                    while (!chars.atEnd()) {
                        c = chars.next(opts, literal, ec);
                        if (U_FAILURE(ec)) return;
                        if (c == 0x7D /*'}'*/ && !literal) {
                            ok = TRUE;
                            break;
                        }
                        buf.append(c);
                    }
                    if (buf.length() < 1 || !ok) {
                        // syntaxError(chars, "Invalid multicharacter string");
                        ec = U_MALFORMED_SET;
                        return;
                    }
                }
                // We have new string. Add it to set and continue;
                // we don't need to drop through to the further
                // processing
                add(buf);
                patLocal.append((UChar) 0x7B /*'{'*/);
                _appendToPat(patLocal, buf, FALSE);
                patLocal.append((UChar) 0x7D /*'}'*/);
                continue;
            case SymbolTable::SYMBOL_REF:
                //         symbols  nosymbols
                // [a-$]   error    error (ambiguous)
                // [a$]    anchor   anchor
                // [a-$x]  var "x"* literal '$'
                // [a-$.]  error    literal '$'
                // *We won't get here in the case of var "x"
                {
                    chars.getPos(backup);
                    c = chars.next(opts, literal, ec);
                    if (U_FAILURE(ec)) return;
                    UBool anchor = (c == 0x5D /*']'*/ && !literal);
                    if (symbols == 0 && !anchor) {
                        c = SymbolTable::SYMBOL_REF;
                        chars.setPos(backup);
                        break; // literal '$'
                    }
                    if (anchor && op == 0) {
                        if (lastItem == 1) {
                            add(lastChar, lastChar);
                            _appendToPat(patLocal, lastChar, FALSE);
                        }
                        add(U_ETHER);
                        usePat = TRUE;
                        patLocal.append((UChar) SymbolTable::SYMBOL_REF);
                        patLocal.append((UChar) 0x5D /*']'*/);
                        mode = 2;
                        continue;
                    }
                    // syntaxError(chars, "Unquoted '$'");
                    ec = U_MALFORMED_SET;
                    return;
                }
            default:
                break;
            }
        }

        // -------- Parse literal characters.  This includes both
        // escaped chars ("\u4E01") and non-syntax characters
        // ("a").

        switch (lastItem) {
        case 0:
            lastItem = 1;
            lastChar = c;
            break;
        case 1:
            if (op == HYPHEN /*'-'*/) {
                if (lastChar >= c) {
                    // Don't allow redundant (a-a) or empty (b-a) ranges;
                    // these are most likely typos.
                    // syntaxError(chars, "Invalid range");
                    ec = U_MALFORMED_SET;
                    return;
                }
                add(lastChar, c);
                _appendToPat(patLocal, lastChar, FALSE);
                patLocal.append(op);
                _appendToPat(patLocal, c, FALSE);
                lastItem = 0;
                op = 0;
            } else {
                add(lastChar, lastChar);
                _appendToPat(patLocal, lastChar, FALSE);
                lastChar = c;
            }
            break;
        case 2:
            if (op != 0) {
                // syntaxError(chars, "Set expected after operator");
                ec = U_MALFORMED_SET;
                return;
            }
            lastChar = c;
            lastItem = 1;
            break;
        }
    }

    if (mode != 2) {
        // syntaxError(chars, "Missing ']'");
        ec = U_MALFORMED_SET;
        return;
    }

    chars.skipIgnored(opts);

    /**
     * Handle global flags (invert, case insensitivity).  If this
     * pattern should be compiled case-insensitive, then we need
     * to close over case BEFORE COMPLEMENTING.  This makes
     * patterns like /[^abc]/i work.
     */
    if ((options & USET_CASE_INSENSITIVE) != 0) {
        closeOver(USET_CASE);
    }
    if (invert) {
        complement();
    }

    // Use the rebuilt pattern (patLocal) only if necessary.  Prefer the
    // generated pattern.
    if (usePat) {
        rebuiltPat.append(patLocal);
    } else {
        _generatePattern(rebuiltPat, FALSE);
    }
}

//----------------------------------------------------------------
// Implementation: Utility methods
//----------------------------------------------------------------

/**
 * Allocate our strings vector and return TRUE if successful.
 */
UBool UnicodeSet::allocateStrings() {
    UErrorCode ec = U_ZERO_ERROR;
    strings = new UVector(uhash_deleteUnicodeString,
                          uhash_compareUnicodeString, ec);
    if (U_FAILURE(ec)) {
        delete strings;
        strings = NULL;
        return FALSE;
    }
    return TRUE;
}

void UnicodeSet::ensureCapacity(int32_t newLen) {
    if (newLen <= capacity)
        return;
    capacity = newLen + GROW_EXTRA;
    UChar32* temp = (UChar32*) uprv_malloc(sizeof(UChar32) * capacity);
    uprv_memcpy(temp, list, len*sizeof(UChar32));
    uprv_free(list);
    list = temp;
}

void UnicodeSet::ensureBufferCapacity(int32_t newLen) {
    if (buffer != NULL && newLen <= bufferCapacity)
        return;
    if (buffer) {
        uprv_free(buffer);
    }
    bufferCapacity = newLen + GROW_EXTRA;
    buffer = (UChar32*) uprv_malloc(sizeof(UChar32) * bufferCapacity);
}

/**
 * Swap list and buffer.
 */
void UnicodeSet::swapBuffers(void) {
    // swap list and buffer
    UChar32* temp = list;
    list = buffer;
    buffer = temp;

    int32_t c = capacity;
    capacity = bufferCapacity;
    bufferCapacity = c;
}

//----------------------------------------------------------------
// Implementation: Fundamental operators
//----------------------------------------------------------------

static inline UChar32 max(UChar32 a, UChar32 b) {
    return (a > b) ? a : b;
}

// polarity = 0, 3 is normal: x xor y
// polarity = 1, 2: x xor ~y == x === y

void UnicodeSet::exclusiveOr(const UChar32* other, int32_t otherLen, int8_t polarity) {
    ensureBufferCapacity(len + otherLen);
    int32_t i = 0, j = 0, k = 0;
    UChar32 a = list[i++];
    UChar32 b;
    if (polarity == 1 || polarity == 2) {
        b = UNICODESET_LOW;
        if (other[j] == UNICODESET_LOW) { // skip base if already LOW
            ++j;
            b = other[j];
        }
    } else {
        b = other[j++];
    }
    // simplest of all the routines
    // sort the values, discarding identicals!
    for (;;) {
        if (a < b) {
            buffer[k++] = a;
            a = list[i++];
        } else if (b < a) {
            buffer[k++] = b;
            b = other[j++];
        } else if (a != UNICODESET_HIGH) { // at this point, a == b
            // discard both values!
            a = list[i++];
            b = other[j++];
        } else { // DONE!
            buffer[k++] = UNICODESET_HIGH;
            len = k;
            break;
        }
    }
    swapBuffers();
    pat.truncate(0);
}

// polarity = 0 is normal: x union y
// polarity = 2: x union ~y
// polarity = 1: ~x union y
// polarity = 3: ~x union ~y

void UnicodeSet::add(const UChar32* other, int32_t otherLen, int8_t polarity) {
    ensureBufferCapacity(len + otherLen);
    int32_t i = 0, j = 0, k = 0;
    UChar32 a = list[i++];
    UChar32 b = other[j++];
    // change from xor is that we have to check overlapping pairs
    // polarity bit 1 means a is second, bit 2 means b is.
    for (;;) {
        switch (polarity) {
          case 0: // both first; take lower if unequal
            if (a < b) { // take a
                // Back up over overlapping ranges in buffer[]
                if (k > 0 && a <= buffer[k-1]) {
                    // Pick latter end value in buffer[] vs. list[]
                    a = max(list[i], buffer[--k]);
                } else {
                    // No overlap
                    buffer[k++] = a;
                    a = list[i];
                }
                i++; // Common if/else code factored out
                polarity ^= 1;
            } else if (b < a) { // take b
                if (k > 0 && b <= buffer[k-1]) {
                    b = max(other[j], buffer[--k]);
                } else {
                    buffer[k++] = b;
                    b = other[j];
                }
                j++;
                polarity ^= 2;
            } else { // a == b, take a, drop b
                if (a == UNICODESET_HIGH) goto loop_end;
                // This is symmetrical; it doesn't matter if
                // we backtrack with a or b. - liu
                if (k > 0 && a <= buffer[k-1]) {
                    a = max(list[i], buffer[--k]);
                } else {
                    // No overlap
                    buffer[k++] = a;
                    a = list[i];
                }
                i++;
                polarity ^= 1;
                b = other[j++];
                polarity ^= 2;
            }
            break;
          case 3: // both second; take higher if unequal, and drop other
            if (b <= a) { // take a
                if (a == UNICODESET_HIGH) goto loop_end;
                buffer[k++] = a;
            } else { // take b
                if (b == UNICODESET_HIGH) goto loop_end;
                buffer[k++] = b;
            }
            a = list[i++];
            polarity ^= 1;   // factored common code
            b = other[j++];
            polarity ^= 2;
            break;
          case 1: // a second, b first; if b < a, overlap
            if (a < b) { // no overlap, take a
                buffer[k++] = a; a = list[i++]; polarity ^= 1;
            } else if (b < a) { // OVERLAP, drop b
                b = other[j++];
                polarity ^= 2;
            } else { // a == b, drop both!
                if (a == UNICODESET_HIGH) goto loop_end;
                a = list[i++];
                polarity ^= 1;
                b = other[j++];
                polarity ^= 2;
            }
            break;
          case 2: // a first, b second; if a < b, overlap
            if (b < a) { // no overlap, take b
                buffer[k++] = b;
                b = other[j++];
                polarity ^= 2;
            } else  if (a < b) { // OVERLAP, drop a
                a = list[i++];
                polarity ^= 1;
            } else { // a == b, drop both!
                if (a == UNICODESET_HIGH) goto loop_end;
                a = list[i++];
                polarity ^= 1;
                b = other[j++];
                polarity ^= 2;
            }
            break;
        }
    }
 loop_end:
    buffer[k++] = UNICODESET_HIGH;    // terminate
    len = k;
    swapBuffers();
    pat.truncate(0);
}

// polarity = 0 is normal: x intersect y
// polarity = 2: x intersect ~y == set-minus
// polarity = 1: ~x intersect y
// polarity = 3: ~x intersect ~y

void UnicodeSet::retain(const UChar32* other, int32_t otherLen, int8_t polarity) {
    ensureBufferCapacity(len + otherLen);
    int32_t i = 0, j = 0, k = 0;
    UChar32 a = list[i++];
    UChar32 b = other[j++];
    // change from xor is that we have to check overlapping pairs
    // polarity bit 1 means a is second, bit 2 means b is.
    for (;;) {
        switch (polarity) {
          case 0: // both first; drop the smaller
            if (a < b) { // drop a
                a = list[i++];
                polarity ^= 1;
            } else if (b < a) { // drop b
                b = other[j++];
                polarity ^= 2;
            } else { // a == b, take one, drop other
                if (a == UNICODESET_HIGH) goto loop_end;
                buffer[k++] = a;
                a = list[i++];
                polarity ^= 1;
                b = other[j++];
                polarity ^= 2;
            }
            break;
          case 3: // both second; take lower if unequal
            if (a < b) { // take a
                buffer[k++] = a;
                a = list[i++];
                polarity ^= 1;
            } else if (b < a) { // take b
                buffer[k++] = b;
                b = other[j++];
                polarity ^= 2;
            } else { // a == b, take one, drop other
                if (a == UNICODESET_HIGH) goto loop_end;
                buffer[k++] = a;
                a = list[i++];
                polarity ^= 1;
                b = other[j++];
                polarity ^= 2;
            }
            break;
          case 1: // a second, b first;
            if (a < b) { // NO OVERLAP, drop a
                a = list[i++];
                polarity ^= 1;
            } else if (b < a) { // OVERLAP, take b
                buffer[k++] = b;
                b = other[j++];
                polarity ^= 2;
            } else { // a == b, drop both!
                if (a == UNICODESET_HIGH) goto loop_end;
                a = list[i++];
                polarity ^= 1;
                b = other[j++];
                polarity ^= 2;
            }
            break;
          case 2: // a first, b second; if a < b, overlap
            if (b < a) { // no overlap, drop b
                b = other[j++];
                polarity ^= 2;
            } else  if (a < b) { // OVERLAP, take a
                buffer[k++] = a;
                a = list[i++];
                polarity ^= 1;
            } else { // a == b, drop both!
                if (a == UNICODESET_HIGH) goto loop_end;
                a = list[i++];
                polarity ^= 1;
                b = other[j++];
                polarity ^= 2;
            }
            break;
        }
    }
 loop_end:
    buffer[k++] = UNICODESET_HIGH;    // terminate
    len = k;
    swapBuffers();
    pat.truncate(0);
}

//----------------------------------------------------------------
// Property set implementation
//----------------------------------------------------------------

static UBool numericValueFilter(UChar32 ch, void* context) {
    return u_getNumericValue(ch) == *(double*)context;
}

static UBool generalCategoryMaskFilter(UChar32 ch, void* context) {
    int32_t value = *(int32_t*)context;
    return (U_GET_GC_MASK((UChar32) ch) & value) != 0;
}

static UBool versionFilter(UChar32 ch, void* context) {
    UVersionInfo v, none = { 0, 0, 0, 0};
    UVersionInfo* version = (UVersionInfo*)context;
    u_charAge(ch, v);
    return uprv_memcmp(&v, &none, sizeof(v)) > 0 && uprv_memcmp(&v, version, sizeof(v)) <= 0;
}

typedef struct {
    UProperty prop;
    int32_t value;
} IntPropertyContext;

static UBool intPropertyFilter(UChar32 ch, void* context) {
    IntPropertyContext* c = (IntPropertyContext*)context;
    return u_getIntPropertyValue((UChar32) ch, c->prop) == c->value;
}


/**
 * Generic filter-based scanning code for UCD property UnicodeSets.
 */
void UnicodeSet::applyFilter(UnicodeSet::Filter filter,
                             void* context,
                             UErrorCode &status) {
    // Walk through all Unicode characters, noting the start
    // and end of each range for which filter.contain(c) is
    // true.  Add each range to a set.
    //
    // To improve performance, use the INCLUSIONS set, which
    // encodes information about character ranges that are known
    // to have identical properties. INCLUSIONS contains
    // only the first characters of such ranges.
    //
    // TODO Where possible, instead of scanning over code points,
    // use internal property data to initialize UnicodeSets for
    // those properties.  Scanning code points is slow.
    if (U_FAILURE(status)) return;

    const UnicodeSet* inclusions = getInclusions(status);
    if (U_FAILURE(status)) {
        return;
    }

    clear();

    UChar32 startHasProperty = -1;
    int limitRange = inclusions->getRangeCount();

    for (int j=0; j<limitRange; ++j) {
        // get current range
        UChar32 start = inclusions->getRangeStart(j);
        UChar32 end = inclusions->getRangeEnd(j);

        // for all the code points in the range, process
        for (UChar32 ch = start; ch <= end; ++ch) {
            // only add to this UnicodeSet on inflection points --
            // where the hasProperty value changes to false
            if ((*filter)(ch, context)) {
                if (startHasProperty < 0) {
                    startHasProperty = ch;
                }
            } else if (startHasProperty >= 0) {
                add(startHasProperty, ch-1);
                startHasProperty = -1;
            }
        }
    }
    if (startHasProperty >= 0) {
        add((UChar32)startHasProperty, (UChar32)0x10FFFF);
    }
}

static UBool mungeCharName(char* dst, const char* src, int32_t dstCapacity) {
    /* Note: we use ' ' in compiler code page */
    int32_t j = 0;
    char ch;
    --dstCapacity; /* make room for term. zero */
    while ((ch = *src++) != 0) {
        if (ch == ' ' && (j==0 || (j>0 && dst[j-1]==' '))) {
            continue;
        }
        if (j >= dstCapacity) return FALSE;
        dst[j++] = ch;
    }
    if (j > 0 && dst[j-1] == ' ') --j;
    dst[j] = 0;
    return TRUE;
}

//----------------------------------------------------------------
// Property set API
//----------------------------------------------------------------

#define FAIL(ec) {ec=U_ILLEGAL_ARGUMENT_ERROR; return *this;}

// TODO: Remove the following special-case code when
// these four C99-compatibility properties are implemented
// as enums/names.
static UBool c99Filter(UChar32 ch, void* context) {
    struct C99_Map* m = (struct C99_Map*) context;
    return m->func(ch);
}

UnicodeSet&
UnicodeSet::applyIntPropertyValue(UProperty prop, int32_t value, UErrorCode& ec) {
    if (U_FAILURE(ec)) return *this;

    if (prop == UCHAR_GENERAL_CATEGORY_MASK) {
        applyFilter(generalCategoryMaskFilter, &value, ec);
#if UCONFIG_NO_NORMALIZATION
    } else if(prop == UCHAR_HANGUL_SYLLABLE_TYPE) {
        /*
         * Special code for when normalization is off.
         * HST is still available because it is hardcoded in uprops.c, but
         * the inclusions set does not have the necessary code points
         * for normalization properties.
         * I am hardcoding HST in this case because it is the only property
         * that prevents genbrk from compiling char.txt when normalization is off.
         * This saves me from turning off break iteration or making more
         * complicated changes in genbrk.
         *
         * This code is not efficient. For efficiency turn on normalization.
         *
         * markus 20030505
         */
        UChar32 c;

        clear();
        for(c=0x1100; c<=0xd7a3; ++c) {
            if(c==0x1200) {
                c=0xac00;
            }
            if(value == u_getIntPropertyValue(c, UCHAR_HANGUL_SYLLABLE_TYPE)) {
                add(c);
            }
        }
#endif
    } else {
        IntPropertyContext c = {prop, value};
        applyFilter(intPropertyFilter, &c, ec);
    }
    return *this;
}

UnicodeSet&
UnicodeSet::applyPropertyAlias(const UnicodeString& prop,
                               const UnicodeString& value,
                               UErrorCode& ec) {
    if (U_FAILURE(ec)) return *this;

    UProperty p;
    int32_t v;
    CharString pname(prop, NULL); // default codepage (not invariant codepage)
    CharString vname(value, NULL);
    UBool mustNotBeEmpty = FALSE;

    if (value.length() > 0) {
        p = u_getPropertyEnum(pname);
        if (p == UCHAR_INVALID_CODE) FAIL(ec);

        // Treat gc as gcm
        if (p == UCHAR_GENERAL_CATEGORY) {
            p = UCHAR_GENERAL_CATEGORY_MASK;
        }

        if ((p >= UCHAR_BINARY_START && p < UCHAR_BINARY_LIMIT) ||
            (p >= UCHAR_INT_START && p < UCHAR_INT_LIMIT) ||
            (p >= UCHAR_MASK_START && p < UCHAR_MASK_LIMIT)) {
            v = u_getPropertyValueEnum(p, vname);
            if (v == UCHAR_INVALID_CODE) {
                // Handle numeric CCC
                if (p == UCHAR_CANONICAL_COMBINING_CLASS ||
                    p == UCHAR_TRAIL_CANONICAL_COMBINING_CLASS ||
                    p == UCHAR_LEAD_CANONICAL_COMBINING_CLASS) {
                    char* end;
                    double value = uprv_strtod(vname, &end);
                    v = (int32_t) value;
                    if (v != value || v < 0 || *end != 0) {
                        // non-integral or negative value, or trailing junk
                        FAIL(ec);
                    }
                    // If the resultant set is empty then the numeric value
                    // was invalid.
                    mustNotBeEmpty = TRUE;
                } else {
                    FAIL(ec);
                }
            }
        }

        else {

            switch (p) {
            case UCHAR_NUMERIC_VALUE:
                {
                    char* end;
                    double value = uprv_strtod(vname, &end);
                    if (*end != 0) {
                        FAIL(ec);
                    }
                    applyFilter(numericValueFilter, &value, ec);
                    return *this;
                }
                break;
            case UCHAR_NAME:
            case UCHAR_UNICODE_1_NAME:
                {
                    // Must munge name, since u_charFromName() does not do
                    // 'loose' matching.
                    char buf[128]; // it suffices that this be > uprv_getMaxCharNameLength
                    if (!mungeCharName(buf, vname, sizeof(buf))) FAIL(ec);
                    UCharNameChoice choice = (p == UCHAR_NAME) ?
                        U_EXTENDED_CHAR_NAME : U_UNICODE_10_CHAR_NAME;
                    UChar32 ch = u_charFromName(choice, buf, &ec);
                    if (U_SUCCESS(ec)) {
                        clear();
                        add(ch);
                        return *this;
                    } else {
                        FAIL(ec);
                    }
                }
                break;
            case UCHAR_AGE:
                {
                    // Must munge name, since u_versionFromString() does not do
                    // 'loose' matching.
                    char buf[128];
                    if (!mungeCharName(buf, vname, sizeof(buf))) FAIL(ec);
                    UVersionInfo version;
                    u_versionFromString(version, buf);
                    applyFilter(versionFilter, &version, ec);
                    return *this;
                }
                break;
            default:
                // p is a non-binary, non-enumerated property that we
                // don't support (yet).
                FAIL(ec);
            }
        }
    }

    else {
        // value is empty.  Interpret as General Category, Script, or
        // Binary property.
        p = UCHAR_GENERAL_CATEGORY_MASK;
        v = u_getPropertyValueEnum(p, pname);
        if (v == UCHAR_INVALID_CODE) {
            p = UCHAR_SCRIPT;
            v = u_getPropertyValueEnum(p, pname);
            if (v == UCHAR_INVALID_CODE) {
                p = u_getPropertyEnum(pname);
                if (p >= UCHAR_BINARY_START && p < UCHAR_BINARY_LIMIT) {
                    v = 1;
                } else if (0 == uprv_comparePropertyNames(ANY, pname)) {
                    set(MIN_VALUE, MAX_VALUE);
                    return *this;
                } else if (0 == uprv_comparePropertyNames(ASCII, pname)) {
                    set(0, 0x7F);
                    return *this;
                } else {

                    // TODO: Remove the following special-case code when
                    // these four C99-compatibility properties are implemented
                    // as enums/names.
                    for (int32_t i=0; i<C99_COUNT; ++i) {
                        int32_t c = uprv_comparePropertyNames(pname, C99_DISPATCH[i].name);
                        if (c == 0) {
                            applyFilter(c99Filter, (void*) &C99_DISPATCH[i], ec);
                            return *this;
                        } else if (c < 0) {
                            // Further entries will not match; bail out
                            break;
                        }
                    }

                    FAIL(ec);
                }
            }
        }
    }
    
    applyIntPropertyValue(p, v, ec);

    if (U_SUCCESS(ec) && (mustNotBeEmpty && isEmpty())) {
        // mustNotBeEmpty is set to true if an empty set indicates
        // invalid input.
        ec = U_ILLEGAL_ARGUMENT_ERROR;
    }

    return *this;
}

//----------------------------------------------------------------
// Property set patterns
//----------------------------------------------------------------

/**
 * Return true if the given position, in the given pattern, appears
 * to be the start of a property set pattern.
 */
UBool UnicodeSet::resemblesPropertyPattern(const UnicodeString& pattern,
                                           int32_t pos) {
    // Patterns are at least 5 characters long
    if ((pos+5) > pattern.length()) {
        return FALSE;
    }

    // Look for an opening [:, [:^, \p, or \P
    return isPOSIXOpen(pattern, pos) || isPerlOpen(pattern, pos) || isNameOpen(pattern, pos);
}

/**
 * Return true if the given iterator appears to point at a
 * property pattern.  Regardless of the result, return with the
 * iterator unchanged.
 * @param chars iterator over the pattern characters.  Upon return
 * it will be unchanged.
 * @param iterOpts RuleCharacterIterator options
 */
UBool UnicodeSet::resemblesPropertyPattern(RuleCharacterIterator& chars,
                                           int32_t iterOpts) {
    // NOTE: literal will always be FALSE, because we don't parse escapes.
    UBool result = FALSE, literal;
    UErrorCode ec = U_ZERO_ERROR;
    iterOpts &= ~RuleCharacterIterator::PARSE_ESCAPES;
    RuleCharacterIterator::Pos pos;
    chars.getPos(pos);
    UChar32 c = chars.next(iterOpts, literal, ec);
    if (c == 0x5B /*'['*/ || c == 0x5C /*'\\'*/) {
        UChar32 d = chars.next(iterOpts & ~RuleCharacterIterator::SKIP_WHITESPACE,
                               literal, ec);
        result = (c == 0x5B /*'['*/) ? (d == 0x3A /*':'*/) :
                 (d == 0x4E /*'N'*/ || d == 0x70 /*'p'*/ || d == 0x50 /*'P'*/);
    }
    chars.setPos(pos);
    return result && U_SUCCESS(ec);
}

/**
 * Parse the given property pattern at the given parse position.
 */
UnicodeSet& UnicodeSet::applyPropertyPattern(const UnicodeString& pattern,
                                             ParsePosition& ppos,
                                             UErrorCode &ec) {
    int32_t pos = ppos.getIndex();

    UBool posix = FALSE; // true for [:pat:], false for \p{pat} \P{pat} \N{pat}
    UBool isName = FALSE; // true for \N{pat}, o/w false
    UBool invert = FALSE;

    if (U_FAILURE(ec)) return *this;

    // Minimum length is 5 characters, e.g. \p{L}
    if ((pos+5) > pattern.length()) {
        FAIL(ec);
    }

    // On entry, ppos should point to one of the following locations:
    // Look for an opening [:, [:^, \p, or \P
    if (isPOSIXOpen(pattern, pos)) {
        posix = TRUE;
        pos += 2;
        pos = ICU_Utility::skipWhitespace(pattern, pos);
        if (pos < pattern.length() && pattern.charAt(pos) == COMPLEMENT) {
            ++pos;
            invert = TRUE;
        }
    } else if (isPerlOpen(pattern, pos) || isNameOpen(pattern, pos)) {
        UChar c = pattern.charAt(pos+1);
        invert = (c == UPPER_P);
        isName = (c == UPPER_N);
        pos += 2;
        pos = ICU_Utility::skipWhitespace(pattern, pos);
        if (pos == pattern.length() || pattern.charAt(pos++) != OPEN_BRACE) {
            // Syntax error; "\p" or "\P" not followed by "{"
            FAIL(ec);
        }
    } else {
        // Open delimiter not seen
        FAIL(ec);
    }

    // Look for the matching close delimiter, either :] or }
    int32_t close = pattern.indexOf(posix ? POSIX_CLOSE : PERL_CLOSE, pos);
    if (close < 0) {
        // Syntax error; close delimiter missing
        FAIL(ec);
    }

    // Look for an '=' sign.  If this is present, we will parse a
    // medium \p{gc=Cf} or long \p{GeneralCategory=Format}
    // pattern.
    int32_t equals = pattern.indexOf(EQUALS, pos);
    UnicodeString propName, valueName;
    if (equals >= 0 && equals < close && !isName) {
        // Equals seen; parse medium/long pattern
        pattern.extractBetween(pos, equals, propName);
        pattern.extractBetween(equals+1, close, valueName);
    }

    else {
        // Handle case where no '=' is seen, and \N{}
        pattern.extractBetween(pos, close, propName);
            
        // Handle \N{name}
        if (isName) {
            // This is a little inefficient since it means we have to
            // parse NAME_PROP back to UCHAR_NAME even though we already
            // know it's UCHAR_NAME.  If we refactor the API to
            // support args of (UProperty, char*) then we can remove
            // NAME_PROP and make this a little more efficient.
            valueName = propName;
            propName = NAME_PROP;
        }
    }

    applyPropertyAlias(propName, valueName, ec);

    if (U_SUCCESS(ec)) {
        if (invert) {
            complement();
        }
            
        // Move to the limit position after the close delimiter if the
        // parse succeeded.
        ppos.setIndex(close + (posix ? 2 : 1));
    }

    return *this;
}

/**
 * Parse a property pattern.
 * @param chars iterator over the pattern characters.  Upon return
 * it will be advanced to the first character after the parsed
 * pattern, or the end of the iteration if all characters are
 * parsed.
 * @param rebuiltPat the pattern that was parsed, rebuilt or
 * copied from the input pattern, as appropriate.
 */
void UnicodeSet::applyPropertyPattern(RuleCharacterIterator& chars,
                                      UnicodeString& rebuiltPat,
                                      UErrorCode& ec) {
    if (U_FAILURE(ec)) return;
    UnicodeString pattern;
    chars.lookahead(pattern);
    ParsePosition pos(0);
    applyPropertyPattern(pattern, pos, ec);
    if (U_FAILURE(ec)) return;
    if (pos.getIndex() == 0) {
        // syntaxError(chars, "Invalid property pattern");
        ec = U_MALFORMED_SET;
        return;
    }
    chars.jumpahead(pos.getIndex());
    rebuiltPat.append(pattern, 0, pos.getIndex());
}

//----------------------------------------------------------------
// Inclusions list
//----------------------------------------------------------------

const UnicodeSet* UnicodeSet::getInclusions(UErrorCode &status) {
    umtx_lock(NULL);
    UBool f = (INCLUSIONS == NULL);
    umtx_unlock(NULL);
    if (f) {
        UnicodeSet* incl = new UnicodeSet();
        if (incl != NULL) {
            uprv_getInclusions((USet*)incl, &status);
            if (U_SUCCESS(status)) {
                umtx_lock(NULL);
                if (INCLUSIONS == NULL) {
                    INCLUSIONS = incl;
                    incl = NULL;        
                } 
                umtx_unlock(NULL);
            }
            delete incl;
        } else {
            status = U_MEMORY_ALLOCATION_ERROR;
        }
    }
    return INCLUSIONS;
}

/**
 * Cleanup function for UnicodeSet
 */
U_CFUNC UBool uset_cleanup(void) {
    if (INCLUSIONS != NULL) {
        delete INCLUSIONS;
        INCLUSIONS = NULL;
    }

    if (CASE_EQUIV_HASH != NULL) {
        delete CASE_EQUIV_HASH;
        CASE_EQUIV_HASH = NULL;
    }

    if (CASE_EQUIV_CBA != NULL) {
        ucmp8_close(CASE_EQUIV_CBA);
        CASE_EQUIV_CBA = NULL;
    }

    return TRUE;
}

//----------------------------------------------------------------
// Case folding API
//----------------------------------------------------------------

UnicodeSet& UnicodeSet::closeOver(int32_t attribute) {
    if ((attribute & USET_CASE) != 0) {
        UnicodeSet foldSet;
        UnicodeString str;
        int32_t n = getRangeCount();
        for (int32_t i=0; i<n; ++i) {
            UChar32 start = getRangeStart(i);
            UChar32 end   = getRangeEnd(i);
            for (UChar32 cp=start; cp<=end; ++cp) {
                str.truncate(0);
                str.append(u_foldCase(cp, U_FOLD_CASE_DEFAULT));
                foldSet.caseCloseOne(str);
            }
        }
        if (strings != NULL && strings->size() > 0) {
            for (int32_t j=0; j<strings->size(); ++j) {
                str = * (const UnicodeString*) strings->elementAt(j);
                foldSet.caseCloseOne(str.foldCase());
            }
        }
        *this = foldSet;
    }
    return *this;
}

//----------------------------------------------------------------
// Case folding implementation
//----------------------------------------------------------------

/**
 * Data structure representing a case-fold equivalency class.  It is a
 * SET containing 0 or more code units, and 0 or more strings of
 * length 2 code units or longer.
 *
 * This class is implemented as a 8-UChar buffer with a few
 * convenience methods on it.  The format of the buffer:
 * - All single code units in this set, followed by a terminating
 *   zero.  If none, then just a terminating zero.
 * - Zero or more 0-terminated strings, each of length >= 2
 *   code units.
 * - A single terminating (UChar)0.
 *
 * Usage:
 *
 * const CaseEquivClass& c = ...;
 * const UChar* p;
 * for (c.getStrings(p); *p; c.nextString(p)) {
 *   foo(p);
 * }
 */
class CaseEquivClass {
public:
    UChar data[8];

    /**
     * Return the string of single code units.  May be "".  Will never
     * be NULL.
     */
    const UChar* getSingles() const {
        return data;
    }

    /**
     * Return the first multi-code-unit string.  May be "" if there
     * are none.  Will never be NULL.
     * @param p pointer to be set to point to the first string.
     */
    void getStrings(const UChar*& p) const {
        p = data;
        nextString(p);
    }

    /**
     * Advance a pointer from one multi-code-unit string to the next.
     * May advance 'p' to point to "" if there are no more.
     * Do NOT call if *p == 0.
     * @param p pointer to be advanced to point to the next string.
     */
    static void nextString(const UChar*& p) {
        while (*p++) {}
    }
};

/**
 * IMPORTANT: The following two static data arrays represent the
 * information used to do case closure.  The first array is an array
 * of pairs.  That is, for each even index e, entries [e] and [e+1]
 * form a pair of case equivalent code units.  The entry at [e] is the
 * folded one, that is, the one for which u_foldCase(x)==x.
 *
 * The second static array is an array of CaseEquivClass objects.
 * Since these objects are just adorned UChar[] arrays, they can be
 * initialized in place in the array, and all of them can live in a
 * single piece of static memory, with no heap allocation.
 */

// MACHINE-GENERATED: Do not edit (see com.ibm.icu.dev.tools.translit.UnicodeSetCloseOver)
static const UChar CASE_PAIRS[] = {
    0x0061,0x0041,0x0062,0x0042,0x0063,0x0043,0x0064,0x0044,0x0065,0x0045,
    0x0066,0x0046,0x0067,0x0047,0x0068,0x0048,0x0069,0x0049,0x006A,0x004A,
    0x006C,0x004C,0x006D,0x004D,0x006E,0x004E,0x006F,0x004F,0x0070,0x0050,
    0x0071,0x0051,0x0072,0x0052,0x0074,0x0054,0x0075,0x0055,0x0076,0x0056,
    0x0077,0x0057,0x0078,0x0058,0x0079,0x0059,0x007A,0x005A,0x00E0,0x00C0,
    0x00E1,0x00C1,0x00E2,0x00C2,0x00E3,0x00C3,0x00E4,0x00C4,0x00E6,0x00C6,
    0x00E7,0x00C7,0x00E8,0x00C8,0x00E9,0x00C9,0x00EA,0x00CA,0x00EB,0x00CB,
    0x00EC,0x00CC,0x00ED,0x00CD,0x00EE,0x00CE,0x00EF,0x00CF,0x00F0,0x00D0,
    0x00F1,0x00D1,0x00F2,0x00D2,0x00F3,0x00D3,0x00F4,0x00D4,0x00F5,0x00D5,
    0x00F6,0x00D6,0x00F8,0x00D8,0x00F9,0x00D9,0x00FA,0x00DA,0x00FB,0x00DB,
    0x00FC,0x00DC,0x00FD,0x00DD,0x00FE,0x00DE,0x00FF,0x0178,0x0101,0x0100,
    0x0103,0x0102,0x0105,0x0104,0x0107,0x0106,0x0109,0x0108,0x010B,0x010A,
    0x010D,0x010C,0x010F,0x010E,0x0111,0x0110,0x0113,0x0112,0x0115,0x0114,
    0x0117,0x0116,0x0119,0x0118,0x011B,0x011A,0x011D,0x011C,0x011F,0x011E,
    0x0121,0x0120,0x0123,0x0122,0x0125,0x0124,0x0127,0x0126,0x0129,0x0128,
    0x012B,0x012A,0x012D,0x012C,0x012F,0x012E,0x0133,0x0132,0x0135,0x0134,
    0x0137,0x0136,0x013A,0x0139,0x013C,0x013B,0x013E,0x013D,0x0140,0x013F,
    0x0142,0x0141,0x0144,0x0143,0x0146,0x0145,0x0148,0x0147,0x014B,0x014A,
    0x014D,0x014C,0x014F,0x014E,0x0151,0x0150,0x0153,0x0152,0x0155,0x0154,
    0x0157,0x0156,0x0159,0x0158,0x015B,0x015A,0x015D,0x015C,0x015F,0x015E,
    0x0161,0x0160,0x0163,0x0162,0x0165,0x0164,0x0167,0x0166,0x0169,0x0168,
    0x016B,0x016A,0x016D,0x016C,0x016F,0x016E,0x0171,0x0170,0x0173,0x0172,
    0x0175,0x0174,0x0177,0x0176,0x017A,0x0179,0x017C,0x017B,0x017E,0x017D,
    0x0183,0x0182,0x0185,0x0184,0x0188,0x0187,0x018C,0x018B,0x0192,0x0191,
    0x0195,0x01F6,0x0199,0x0198,0x019E,0x0220,0x01A1,0x01A0,0x01A3,0x01A2,
    0x01A5,0x01A4,0x01A8,0x01A7,0x01AD,0x01AC,0x01B0,0x01AF,0x01B4,0x01B3,
    0x01B6,0x01B5,0x01B9,0x01B8,0x01BD,0x01BC,0x01BF,0x01F7,0x01CE,0x01CD,
    0x01D0,0x01CF,0x01D2,0x01D1,0x01D4,0x01D3,0x01D6,0x01D5,0x01D8,0x01D7,
    0x01DA,0x01D9,0x01DC,0x01DB,0x01DD,0x018E,0x01DF,0x01DE,0x01E1,0x01E0,
    0x01E3,0x01E2,0x01E5,0x01E4,0x01E7,0x01E6,0x01E9,0x01E8,0x01EB,0x01EA,
    0x01ED,0x01EC,0x01EF,0x01EE,0x01F5,0x01F4,0x01F9,0x01F8,0x01FB,0x01FA,
    0x01FD,0x01FC,0x01FF,0x01FE,0x0201,0x0200,0x0203,0x0202,0x0205,0x0204,
    0x0207,0x0206,0x0209,0x0208,0x020B,0x020A,0x020D,0x020C,0x020F,0x020E,
    0x0211,0x0210,0x0213,0x0212,0x0215,0x0214,0x0217,0x0216,0x0219,0x0218,
    0x021B,0x021A,0x021D,0x021C,0x021F,0x021E,0x0223,0x0222,0x0225,0x0224,
    0x0227,0x0226,0x0229,0x0228,0x022B,0x022A,0x022D,0x022C,0x022F,0x022E,
    0x0231,0x0230,0x0233,0x0232,0x0253,0x0181,0x0254,0x0186,0x0256,0x0189,
    0x0257,0x018A,0x0259,0x018F,0x025B,0x0190,0x0260,0x0193,0x0263,0x0194,
    0x0268,0x0197,0x0269,0x0196,0x026F,0x019C,0x0272,0x019D,0x0275,0x019F,
    0x0280,0x01A6,0x0283,0x01A9,0x0288,0x01AE,0x028A,0x01B1,0x028B,0x01B2,
    0x0292,0x01B7,0x03AC,0x0386,0x03AD,0x0388,0x03AE,0x0389,0x03AF,0x038A,
    0x03B1,0x0391,0x03B3,0x0393,0x03B4,0x0394,0x03B6,0x0396,0x03B7,0x0397,
    0x03BB,0x039B,0x03BD,0x039D,0x03BE,0x039E,0x03BF,0x039F,0x03C4,0x03A4,
    0x03C5,0x03A5,0x03C7,0x03A7,0x03C8,0x03A8,0x03CA,0x03AA,0x03CB,0x03AB,
    0x03CC,0x038C,0x03CD,0x038E,0x03CE,0x038F,0x03D9,0x03D8,0x03DB,0x03DA,
    0x03DD,0x03DC,0x03DF,0x03DE,0x03E1,0x03E0,0x03E3,0x03E2,0x03E5,0x03E4,
    0x03E7,0x03E6,0x03E9,0x03E8,0x03EB,0x03EA,0x03ED,0x03EC,0x03EF,0x03EE,
    0x0430,0x0410,0x0431,0x0411,0x0432,0x0412,0x0433,0x0413,0x0434,0x0414,
    0x0435,0x0415,0x0436,0x0416,0x0437,0x0417,0x0438,0x0418,0x0439,0x0419,
    0x043A,0x041A,0x043B,0x041B,0x043C,0x041C,0x043D,0x041D,0x043E,0x041E,
    0x043F,0x041F,0x0440,0x0420,0x0441,0x0421,0x0442,0x0422,0x0443,0x0423,
    0x0444,0x0424,0x0445,0x0425,0x0446,0x0426,0x0447,0x0427,0x0448,0x0428,
    0x0449,0x0429,0x044A,0x042A,0x044B,0x042B,0x044C,0x042C,0x044D,0x042D,
    0x044E,0x042E,0x044F,0x042F,0x0450,0x0400,0x0451,0x0401,0x0452,0x0402,
    0x0453,0x0403,0x0454,0x0404,0x0455,0x0405,0x0456,0x0406,0x0457,0x0407,
    0x0458,0x0408,0x0459,0x0409,0x045A,0x040A,0x045B,0x040B,0x045C,0x040C,
    0x045D,0x040D,0x045E,0x040E,0x045F,0x040F,0x0461,0x0460,0x0463,0x0462,
    0x0465,0x0464,0x0467,0x0466,0x0469,0x0468,0x046B,0x046A,0x046D,0x046C,
    0x046F,0x046E,0x0471,0x0470,0x0473,0x0472,0x0475,0x0474,0x0477,0x0476,
    0x0479,0x0478,0x047B,0x047A,0x047D,0x047C,0x047F,0x047E,0x0481,0x0480,
    0x048B,0x048A,0x048D,0x048C,0x048F,0x048E,0x0491,0x0490,0x0493,0x0492,
    0x0495,0x0494,0x0497,0x0496,0x0499,0x0498,0x049B,0x049A,0x049D,0x049C,
    0x049F,0x049E,0x04A1,0x04A0,0x04A3,0x04A2,0x04A5,0x04A4,0x04A7,0x04A6,
    0x04A9,0x04A8,0x04AB,0x04AA,0x04AD,0x04AC,0x04AF,0x04AE,0x04B1,0x04B0,
    0x04B3,0x04B2,0x04B5,0x04B4,0x04B7,0x04B6,0x04B9,0x04B8,0x04BB,0x04BA,
    0x04BD,0x04BC,0x04BF,0x04BE,0x04C2,0x04C1,0x04C4,0x04C3,0x04C6,0x04C5,
    0x04C8,0x04C7,0x04CA,0x04C9,0x04CC,0x04CB,0x04CE,0x04CD,0x04D1,0x04D0,
    0x04D3,0x04D2,0x04D5,0x04D4,0x04D7,0x04D6,0x04D9,0x04D8,0x04DB,0x04DA,
    0x04DD,0x04DC,0x04DF,0x04DE,0x04E1,0x04E0,0x04E3,0x04E2,0x04E5,0x04E4,
    0x04E7,0x04E6,0x04E9,0x04E8,0x04EB,0x04EA,0x04ED,0x04EC,0x04EF,0x04EE,
    0x04F1,0x04F0,0x04F3,0x04F2,0x04F5,0x04F4,0x04F9,0x04F8,0x0501,0x0500,
    0x0503,0x0502,0x0505,0x0504,0x0507,0x0506,0x0509,0x0508,0x050B,0x050A,
    0x050D,0x050C,0x050F,0x050E,0x0561,0x0531,0x0562,0x0532,0x0563,0x0533,
    0x0564,0x0534,0x0565,0x0535,0x0566,0x0536,0x0567,0x0537,0x0568,0x0538,
    0x0569,0x0539,0x056A,0x053A,0x056B,0x053B,0x056C,0x053C,0x056D,0x053D,
    0x056E,0x053E,0x056F,0x053F,0x0570,0x0540,0x0571,0x0541,0x0572,0x0542,
    0x0573,0x0543,0x0574,0x0544,0x0575,0x0545,0x0576,0x0546,0x0577,0x0547,
    0x0578,0x0548,0x0579,0x0549,0x057A,0x054A,0x057B,0x054B,0x057C,0x054C,
    0x057D,0x054D,0x057E,0x054E,0x057F,0x054F,0x0580,0x0550,0x0581,0x0551,
    0x0582,0x0552,0x0583,0x0553,0x0584,0x0554,0x0585,0x0555,0x0586,0x0556,
    0x1E01,0x1E00,0x1E03,0x1E02,0x1E05,0x1E04,0x1E07,0x1E06,0x1E09,0x1E08,
    0x1E0B,0x1E0A,0x1E0D,0x1E0C,0x1E0F,0x1E0E,0x1E11,0x1E10,0x1E13,0x1E12,
    0x1E15,0x1E14,0x1E17,0x1E16,0x1E19,0x1E18,0x1E1B,0x1E1A,0x1E1D,0x1E1C,
    0x1E1F,0x1E1E,0x1E21,0x1E20,0x1E23,0x1E22,0x1E25,0x1E24,0x1E27,0x1E26,
    0x1E29,0x1E28,0x1E2B,0x1E2A,0x1E2D,0x1E2C,0x1E2F,0x1E2E,0x1E31,0x1E30,
    0x1E33,0x1E32,0x1E35,0x1E34,0x1E37,0x1E36,0x1E39,0x1E38,0x1E3B,0x1E3A,
    0x1E3D,0x1E3C,0x1E3F,0x1E3E,0x1E41,0x1E40,0x1E43,0x1E42,0x1E45,0x1E44,
    0x1E47,0x1E46,0x1E49,0x1E48,0x1E4B,0x1E4A,0x1E4D,0x1E4C,0x1E4F,0x1E4E,
    0x1E51,0x1E50,0x1E53,0x1E52,0x1E55,0x1E54,0x1E57,0x1E56,0x1E59,0x1E58,
    0x1E5B,0x1E5A,0x1E5D,0x1E5C,0x1E5F,0x1E5E,0x1E63,0x1E62,0x1E65,0x1E64,
    0x1E67,0x1E66,0x1E69,0x1E68,0x1E6B,0x1E6A,0x1E6D,0x1E6C,0x1E6F,0x1E6E,
    0x1E71,0x1E70,0x1E73,0x1E72,0x1E75,0x1E74,0x1E77,0x1E76,0x1E79,0x1E78,
    0x1E7B,0x1E7A,0x1E7D,0x1E7C,0x1E7F,0x1E7E,0x1E81,0x1E80,0x1E83,0x1E82,
    0x1E85,0x1E84,0x1E87,0x1E86,0x1E89,0x1E88,0x1E8B,0x1E8A,0x1E8D,0x1E8C,
    0x1E8F,0x1E8E,0x1E91,0x1E90,0x1E93,0x1E92,0x1E95,0x1E94,0x1EA1,0x1EA0,
    0x1EA3,0x1EA2,0x1EA5,0x1EA4,0x1EA7,0x1EA6,0x1EA9,0x1EA8,0x1EAB,0x1EAA,
    0x1EAD,0x1EAC,0x1EAF,0x1EAE,0x1EB1,0x1EB0,0x1EB3,0x1EB2,0x1EB5,0x1EB4,
    0x1EB7,0x1EB6,0x1EB9,0x1EB8,0x1EBB,0x1EBA,0x1EBD,0x1EBC,0x1EBF,0x1EBE,
    0x1EC1,0x1EC0,0x1EC3,0x1EC2,0x1EC5,0x1EC4,0x1EC7,0x1EC6,0x1EC9,0x1EC8,
    0x1ECB,0x1ECA,0x1ECD,0x1ECC,0x1ECF,0x1ECE,0x1ED1,0x1ED0,0x1ED3,0x1ED2,
    0x1ED5,0x1ED4,0x1ED7,0x1ED6,0x1ED9,0x1ED8,0x1EDB,0x1EDA,0x1EDD,0x1EDC,
    0x1EDF,0x1EDE,0x1EE1,0x1EE0,0x1EE3,0x1EE2,0x1EE5,0x1EE4,0x1EE7,0x1EE6,
    0x1EE9,0x1EE8,0x1EEB,0x1EEA,0x1EED,0x1EEC,0x1EEF,0x1EEE,0x1EF1,0x1EF0,
    0x1EF3,0x1EF2,0x1EF5,0x1EF4,0x1EF7,0x1EF6,0x1EF9,0x1EF8,0x1F00,0x1F08,
    0x1F01,0x1F09,0x1F02,0x1F0A,0x1F03,0x1F0B,0x1F04,0x1F0C,0x1F05,0x1F0D,
    0x1F06,0x1F0E,0x1F07,0x1F0F,0x1F10,0x1F18,0x1F11,0x1F19,0x1F12,0x1F1A,
    0x1F13,0x1F1B,0x1F14,0x1F1C,0x1F15,0x1F1D,0x1F20,0x1F28,0x1F21,0x1F29,
    0x1F22,0x1F2A,0x1F23,0x1F2B,0x1F24,0x1F2C,0x1F25,0x1F2D,0x1F26,0x1F2E,
    0x1F27,0x1F2F,0x1F30,0x1F38,0x1F31,0x1F39,0x1F32,0x1F3A,0x1F33,0x1F3B,
    0x1F34,0x1F3C,0x1F35,0x1F3D,0x1F36,0x1F3E,0x1F37,0x1F3F,0x1F40,0x1F48,
    0x1F41,0x1F49,0x1F42,0x1F4A,0x1F43,0x1F4B,0x1F44,0x1F4C,0x1F45,0x1F4D,
    0x1F51,0x1F59,0x1F53,0x1F5B,0x1F55,0x1F5D,0x1F57,0x1F5F,0x1F60,0x1F68,
    0x1F61,0x1F69,0x1F62,0x1F6A,0x1F63,0x1F6B,0x1F64,0x1F6C,0x1F65,0x1F6D,
    0x1F66,0x1F6E,0x1F67,0x1F6F,0x1F70,0x1FBA,0x1F71,0x1FBB,0x1F72,0x1FC8,
    0x1F73,0x1FC9,0x1F74,0x1FCA,0x1F75,0x1FCB,0x1F76,0x1FDA,0x1F77,0x1FDB,
    0x1F78,0x1FF8,0x1F79,0x1FF9,0x1F7A,0x1FEA,0x1F7B,0x1FEB,0x1F7C,0x1FFA,
    0x1F7D,0x1FFB,0x1FB0,0x1FB8,0x1FB1,0x1FB9,0x1FD0,0x1FD8,0x1FD1,0x1FD9,
    0x1FE0,0x1FE8,0x1FE1,0x1FE9,0x1FE5,0x1FEC,0x2170,0x2160,0x2171,0x2161,
    0x2172,0x2162,0x2173,0x2163,0x2174,0x2164,0x2175,0x2165,0x2176,0x2166,
    0x2177,0x2167,0x2178,0x2168,0x2179,0x2169,0x217A,0x216A,0x217B,0x216B,
    0x217C,0x216C,0x217D,0x216D,0x217E,0x216E,0x217F,0x216F,0x24D0,0x24B6,
    0x24D1,0x24B7,0x24D2,0x24B8,0x24D3,0x24B9,0x24D4,0x24BA,0x24D5,0x24BB,
    0x24D6,0x24BC,0x24D7,0x24BD,0x24D8,0x24BE,0x24D9,0x24BF,0x24DA,0x24C0,
    0x24DB,0x24C1,0x24DC,0x24C2,0x24DD,0x24C3,0x24DE,0x24C4,0x24DF,0x24C5,
    0x24E0,0x24C6,0x24E1,0x24C7,0x24E2,0x24C8,0x24E3,0x24C9,0x24E4,0x24CA,
    0x24E5,0x24CB,0x24E6,0x24CC,0x24E7,0x24CD,0x24E8,0x24CE,0x24E9,0x24CF,
    0xFF41,0xFF21,0xFF42,0xFF22,0xFF43,0xFF23,0xFF44,0xFF24,0xFF45,0xFF25,
    0xFF46,0xFF26,0xFF47,0xFF27,0xFF48,0xFF28,0xFF49,0xFF29,0xFF4A,0xFF2A,
    0xFF4B,0xFF2B,0xFF4C,0xFF2C,0xFF4D,0xFF2D,0xFF4E,0xFF2E,0xFF4F,0xFF2F,
    0xFF50,0xFF30,0xFF51,0xFF31,0xFF52,0xFF32,0xFF53,0xFF33,0xFF54,0xFF34,
    0xFF55,0xFF35,0xFF56,0xFF36,0xFF57,0xFF37,0xFF58,0xFF38,0xFF59,0xFF39,
    0xFF5A,0xFF3A,
};

// MACHINE-GENERATED: Do not edit (see com.ibm.icu.dev.tools.translit.UnicodeSetCloseOver)
static const CaseEquivClass CASE_NONPAIRS[] = {
    {{0x1E9A,0,  0x0061,0x02BE,0, 0}},
    {{0xFB00,0,  0x0066,0x0066,0, 0}},
    {{0xFB03,0,  0x0066,0x0066,0x0069,0, 0}},
    {{0xFB04,0,  0x0066,0x0066,0x006C,0, 0}},
    {{0xFB01,0,  0x0066,0x0069,0, 0}},
    {{0xFB02,0,  0x0066,0x006C,0, 0}},
    {{0x1E96,0,  0x0068,0x0331,0, 0}},
    {{0x0130,0,  0x0069,0x0307,0, 0}},
    {{0x01F0,0,  0x006A,0x030C,0, 0}},
    {{0x004B,0x006B,0x212A,0,  0}},
    {{0x0053,0x0073,0x017F,0,  0}},
    {{0x00DF,0,  0x0073,0x0073,0, 0}},
    {{0xFB05,0xFB06,0,  0x0073,0x0074,0, 0}},
    {{0x1E97,0,  0x0074,0x0308,0, 0}},
    {{0x1E98,0,  0x0077,0x030A,0, 0}},
    {{0x1E99,0,  0x0079,0x030A,0, 0}},
    {{0x00C5,0x00E5,0x212B,0,  0}},
    {{0x01C4,0x01C5,0x01C6,0,  0}},
    {{0x01C7,0x01C8,0x01C9,0,  0}},
    {{0x01CA,0x01CB,0x01CC,0,  0}},
    {{0x01F1,0x01F2,0x01F3,0,  0}},
    {{0x0149,0,  0x02BC,0x006E,0, 0}},
    {{0x1FB4,0,  0x03AC,0x03B9,0, 0}},
    {{0x1FC4,0,  0x03AE,0x03B9,0, 0}},
    {{0x1FB6,0,  0x03B1,0x0342,0, 0}},
    {{0x1FB7,0,  0x03B1,0x0342,0x03B9,0, 0}},
    {{0x1FB3,0x1FBC,0,  0x03B1,0x03B9,0, 0}},
    {{0x0392,0x03B2,0x03D0,0,  0}},
    {{0x0395,0x03B5,0x03F5,0,  0}},
    {{0x1FC6,0,  0x03B7,0x0342,0, 0}},
    {{0x1FC7,0,  0x03B7,0x0342,0x03B9,0, 0}},
    {{0x1FC3,0x1FCC,0,  0x03B7,0x03B9,0, 0}},
    {{0x0398,0x03B8,0x03D1,0x03F4,0,  0}},
    {{0x0345,0x0399,0x03B9,0x1FBE,0,  0}},
    {{0x1FD2,0,  0x03B9,0x0308,0x0300,0, 0}},
    {{0x0390,0x1FD3,0,  0x03B9,0x0308,0x0301,0, 0}},
    {{0x1FD7,0,  0x03B9,0x0308,0x0342,0, 0}},
    {{0x1FD6,0,  0x03B9,0x0342,0, 0}},
    {{0x039A,0x03BA,0x03F0,0,  0}},
    {{0x00B5,0x039C,0x03BC,0,  0}},
    {{0x03A0,0x03C0,0x03D6,0,  0}},
    {{0x03A1,0x03C1,0x03F1,0,  0}},
    {{0x1FE4,0,  0x03C1,0x0313,0, 0}},
    {{0x03A3,0x03C2,0x03C3,0x03F2,0,  0}},
    {{0x1FE2,0,  0x03C5,0x0308,0x0300,0, 0}},
    {{0x03B0,0x1FE3,0,  0x03C5,0x0308,0x0301,0, 0}},
    {{0x1FE7,0,  0x03C5,0x0308,0x0342,0, 0}},
    {{0x1F50,0,  0x03C5,0x0313,0, 0}},
    {{0x1F52,0,  0x03C5,0x0313,0x0300,0, 0}},
    {{0x1F54,0,  0x03C5,0x0313,0x0301,0, 0}},
    {{0x1F56,0,  0x03C5,0x0313,0x0342,0, 0}},
    {{0x1FE6,0,  0x03C5,0x0342,0, 0}},
    {{0x03A6,0x03C6,0x03D5,0,  0}},
    {{0x03A9,0x03C9,0x2126,0,  0}},
    {{0x1FF6,0,  0x03C9,0x0342,0, 0}},
    {{0x1FF7,0,  0x03C9,0x0342,0x03B9,0, 0}},
    {{0x1FF3,0x1FFC,0,  0x03C9,0x03B9,0, 0}},
    {{0x1FF4,0,  0x03CE,0x03B9,0, 0}},
    {{0x0587,0,  0x0565,0x0582,0, 0}},
    {{0xFB14,0,  0x0574,0x0565,0, 0}},
    {{0xFB15,0,  0x0574,0x056B,0, 0}},
    {{0xFB17,0,  0x0574,0x056D,0, 0}},
    {{0xFB13,0,  0x0574,0x0576,0, 0}},
    {{0xFB16,0,  0x057E,0x0576,0, 0}},
    {{0x1E60,0x1E61,0x1E9B,0,  0}},
    {{0x1F80,0x1F88,0,  0x1F00,0x03B9,0, 0}},
    {{0x1F81,0x1F89,0,  0x1F01,0x03B9,0, 0}},
    {{0x1F82,0x1F8A,0,  0x1F02,0x03B9,0, 0}},
    {{0x1F83,0x1F8B,0,  0x1F03,0x03B9,0, 0}},
    {{0x1F84,0x1F8C,0,  0x1F04,0x03B9,0, 0}},
    {{0x1F85,0x1F8D,0,  0x1F05,0x03B9,0, 0}},
    {{0x1F86,0x1F8E,0,  0x1F06,0x03B9,0, 0}},
    {{0x1F87,0x1F8F,0,  0x1F07,0x03B9,0, 0}},
    {{0x1F90,0x1F98,0,  0x1F20,0x03B9,0, 0}},
    {{0x1F91,0x1F99,0,  0x1F21,0x03B9,0, 0}},
    {{0x1F92,0x1F9A,0,  0x1F22,0x03B9,0, 0}},
    {{0x1F93,0x1F9B,0,  0x1F23,0x03B9,0, 0}},
    {{0x1F94,0x1F9C,0,  0x1F24,0x03B9,0, 0}},
    {{0x1F95,0x1F9D,0,  0x1F25,0x03B9,0, 0}},
    {{0x1F96,0x1F9E,0,  0x1F26,0x03B9,0, 0}},
    {{0x1F97,0x1F9F,0,  0x1F27,0x03B9,0, 0}},
    {{0x1FA0,0x1FA8,0,  0x1F60,0x03B9,0, 0}},
    {{0x1FA1,0x1FA9,0,  0x1F61,0x03B9,0, 0}},
    {{0x1FA2,0x1FAA,0,  0x1F62,0x03B9,0, 0}},
    {{0x1FA3,0x1FAB,0,  0x1F63,0x03B9,0, 0}},
    {{0x1FA4,0x1FAC,0,  0x1F64,0x03B9,0, 0}},
    {{0x1FA5,0x1FAD,0,  0x1F65,0x03B9,0, 0}},
    {{0x1FA6,0x1FAE,0,  0x1F66,0x03B9,0, 0}},
    {{0x1FA7,0x1FAF,0,  0x1F67,0x03B9,0, 0}},
    {{0x1FB2,0,  0x1F70,0x03B9,0, 0}},
    {{0x1FC2,0,  0x1F74,0x03B9,0, 0}},
    {{0x1FF2,0,  0x1F7C,0x03B9,0, 0}},
    {{0,  0xD801,0xDC00,0, 0xD801,0xDC28,0, 0}},
    {{0,  0xD801,0xDC01,0, 0xD801,0xDC29,0, 0}},
    {{0,  0xD801,0xDC02,0, 0xD801,0xDC2A,0, 0}},
    {{0,  0xD801,0xDC03,0, 0xD801,0xDC2B,0, 0}},
    {{0,  0xD801,0xDC04,0, 0xD801,0xDC2C,0, 0}},
    {{0,  0xD801,0xDC05,0, 0xD801,0xDC2D,0, 0}},
    {{0,  0xD801,0xDC06,0, 0xD801,0xDC2E,0, 0}},
    {{0,  0xD801,0xDC07,0, 0xD801,0xDC2F,0, 0}},
    {{0,  0xD801,0xDC08,0, 0xD801,0xDC30,0, 0}},
    {{0,  0xD801,0xDC09,0, 0xD801,0xDC31,0, 0}},
    {{0,  0xD801,0xDC0A,0, 0xD801,0xDC32,0, 0}},
    {{0,  0xD801,0xDC0B,0, 0xD801,0xDC33,0, 0}},
    {{0,  0xD801,0xDC0C,0, 0xD801,0xDC34,0, 0}},
    {{0,  0xD801,0xDC0D,0, 0xD801,0xDC35,0, 0}},
    {{0,  0xD801,0xDC0E,0, 0xD801,0xDC36,0, 0}},
    {{0,  0xD801,0xDC0F,0, 0xD801,0xDC37,0, 0}},
    {{0,  0xD801,0xDC10,0, 0xD801,0xDC38,0, 0}},
    {{0,  0xD801,0xDC11,0, 0xD801,0xDC39,0, 0}},
    {{0,  0xD801,0xDC12,0, 0xD801,0xDC3A,0, 0}},
    {{0,  0xD801,0xDC13,0, 0xD801,0xDC3B,0, 0}},
    {{0,  0xD801,0xDC14,0, 0xD801,0xDC3C,0, 0}},
    {{0,  0xD801,0xDC15,0, 0xD801,0xDC3D,0, 0}},
    {{0,  0xD801,0xDC16,0, 0xD801,0xDC3E,0, 0}},
    {{0,  0xD801,0xDC17,0, 0xD801,0xDC3F,0, 0}},
    {{0,  0xD801,0xDC18,0, 0xD801,0xDC40,0, 0}},
    {{0,  0xD801,0xDC19,0, 0xD801,0xDC41,0, 0}},
    {{0,  0xD801,0xDC1A,0, 0xD801,0xDC42,0, 0}},
    {{0,  0xD801,0xDC1B,0, 0xD801,0xDC43,0, 0}},
    {{0,  0xD801,0xDC1C,0, 0xD801,0xDC44,0, 0}},
    {{0,  0xD801,0xDC1D,0, 0xD801,0xDC45,0, 0}},
    {{0,  0xD801,0xDC1E,0, 0xD801,0xDC46,0, 0}},
    {{0,  0xD801,0xDC1F,0, 0xD801,0xDC47,0, 0}},
    {{0,  0xD801,0xDC20,0, 0xD801,0xDC48,0, 0}},
    {{0,  0xD801,0xDC21,0, 0xD801,0xDC49,0, 0}},
    {{0,  0xD801,0xDC22,0, 0xD801,0xDC4A,0, 0}},
    {{0,  0xD801,0xDC23,0, 0xD801,0xDC4B,0, 0}},
    {{0,  0xD801,0xDC24,0, 0xD801,0xDC4C,0, 0}},
    {{0,  0xD801,0xDC25,0, 0xD801,0xDC4D,0, 0}}
};

#define CASE_PAIRS_LENGTH (sizeof(CASE_PAIRS)/sizeof(CASE_PAIRS[0]))
#define CASE_NONPAIRS_LENGTH (sizeof(CASE_NONPAIRS)/sizeof(CASE_NONPAIRS[0]))

/**
 * Add to this set all members of the case fold equivalency class
 * that contains 'folded'.
 * @param folded a string within a case fold equivalency class.
 * It must have the property that UCharacter.foldCase(folded,
 * DEFAULT_CASE_MAP).equals(folded).
 */
void UnicodeSet::caseCloseOne(const UnicodeString& folded) {
    if (folded.length() == 1) {
        caseCloseOne(folded.charAt(0));
        return;
    }

    const CaseEquivClass* c = getCaseMapOf(folded);
    if (c != NULL) {
        caseCloseOne(*c);
        return;
    }

    // Add 'folded' itself; it belongs to no equivalency class.
    add(folded);
}

/**
 * Add to this set all members of the case fold equivalency class
 * that contains 'folded'.
 * @param folded a code UNIT within a case fold equivalency class.
 * It must have the property that uchar_foldCase(folded,
 * DEFAULT_CASE_MAP) == folded.
 */
void UnicodeSet::caseCloseOne(UChar folded) {
    // We must do a DOUBLE LOOKUP, first in the CompactByteArray that
    // indexes into CASE_NONPAIRS[] and then into the CASE_PAIRS[]
    // sorted array.  A character will occur in one or the other, or
    // neither, but not both.

    // Look in the CompactByteArray.
    const CaseEquivClass* c = getCaseMapOf(folded);
    if (c != NULL) {
        caseCloseOne(*c);
        return;
    }

    // Binary search in pairs array, looking at only even entries.
    // The indices low, high, and x will be halved with respect to
    // CASE_PAIRS[]; that is, they must be doubled before indexing.

    // CASE_PAIRS has 1312 elements, of 656 pairs, so the search
    // takes no more than 10 passes.
    int32_t low = 0;
    int32_t high = (CASE_PAIRS_LENGTH >> 1) - 1;
    int32_t x;
    do {
        x = (low + high) >> 1;
        UChar ch = CASE_PAIRS[(uint32_t)(x << 1)];
        if (folded < ch) {
            high = x - 1;
        } else if (folded > ch) {
            low = x + 1;
        } else {
            break;
        }
    } while (low < high);
    
    x = (low + high) & ~1; // ((low + high) >> 1) << 1
    if (folded == CASE_PAIRS[x]) {
        add(CASE_PAIRS[x]);
        add(CASE_PAIRS[x+1]);
    } else {
        // If the search fails, then add folded itself; it is a
        // case-unique code unit.
        add(folded);
    }
}

/**
 * Add to this set all members of the given CaseEquivClass object.
 */
void UnicodeSet::caseCloseOne(const CaseEquivClass& c) {
    const UChar* p = c.getSingles();
    while (*p) {
        add(*p++); // add all single code units
    }
    for (c.getStrings(p); *p; c.nextString(p)) {
        add(p); // add all strings
    }
}

/**
 * Given a folded string of length >= 2 code units, return the
 * CaseEquivClass containing this string, or NULL if none.
 */
const CaseEquivClass* UnicodeSet::getCaseMapOf(const UnicodeString& folded) {
    umtx_lock(NULL);
    UBool f = (CASE_EQUIV_HASH == NULL);
    umtx_unlock(NULL);

    if (f) {
        // Create the Hashtable, which maps UnicodeStrings to index
        // values into CASE_NONPAIRS.
        UErrorCode ec = U_ZERO_ERROR;
        Hashtable* hash = new Hashtable();
        if (hash != NULL) {
            int32_t i;
            for (i=0; i<(int32_t)CASE_NONPAIRS_LENGTH; ++i) {
                const CaseEquivClass* c = &CASE_NONPAIRS[i];
                const UChar* p;
                for (c->getStrings(p); *p; c->nextString(p)) {
                    hash->put(UnicodeString(p), (void*) c, ec);
                }
            }
            if (U_SUCCESS(ec)) {
                umtx_lock(NULL);
                if (CASE_EQUIV_HASH == NULL) {
                    CASE_EQUIV_HASH = hash;
                    hash = NULL;
                }
                umtx_unlock(NULL);
            }
            delete hash;
        }
    }

    return (CASE_EQUIV_HASH != NULL) ?
        (const CaseEquivClass*) CASE_EQUIV_HASH->get(folded) : NULL;
}

/**
 * Given a folded code unit, return the CaseEquivClass containing it,
 * or NULL if none.
 */
const CaseEquivClass* UnicodeSet::getCaseMapOf(UChar folded) {
    umtx_lock(NULL);
    UBool f = (CASE_EQUIV_CBA == NULL);
    umtx_unlock(NULL);

    if (f) {
        // Create the CompactByteArray, which maps single code units
        // to index values into CASE_NONPAIRS.
        CompactByteArray* cba = ucmp8_open(-1);
        if (ucmp8_isBogus(cba)) {
            ucmp8_close(cba);
            cba = NULL;
        } else {
            int32_t i;
            for (i=0; i<(int32_t)CASE_NONPAIRS_LENGTH; ++i) {
                const UChar* p = CASE_NONPAIRS[i].getSingles();
                UChar ch;
                while ((ch = *p++) != 0) {
                    ucmp8_set(cba, ch, (int8_t) i);
                }
            }
            ucmp8_compact(cba, 256);
        }

        umtx_lock(NULL);
        if (CASE_EQUIV_CBA == NULL) {
            CASE_EQUIV_CBA = cba;
            cba = NULL;
        }
        umtx_unlock(NULL);
        if (cba != NULL) {
            ucmp8_close(cba);
        }
    }

    if (CASE_EQUIV_CBA != NULL) {
        int32_t index = ucmp8_getu(CASE_EQUIV_CBA, folded);
        if (index != 255) {
            return &CASE_NONPAIRS[index];
        }
    }
    return NULL;
}

U_NAMESPACE_END
