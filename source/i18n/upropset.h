/*
**********************************************************************
*   Copyright (c) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
* $Source: /xsrl/Nsvn/icu/icu/source/i18n/Attic/upropset.h,v $
* $Date: 2001/10/17 19:20:41 $
* $Revision: 1.1 $
**********************************************************************
*/
#ifndef _UPROPSET_H_
#define _UPROPSET_H_

#include "unicode/utypes.h"
#include "unicode/uscript.h"

U_NAMESPACE_BEGIN

class UnicodeString;
class UnicodeSet;
class ParsePosition;
class Hashtable;

/**
 * INTERNAL CLASS implementing the UnicodeSet properties as outlined
 * at:
 *
 * http://oss.software.ibm.com/cvs/icu/~checkout~/icuhtml/design/unicodeset_properties.html
 *
 * Recognized syntax:
 *
 * [:foo:] [:^foo:] - white space not allowed within "[:" or ":]"
 * \p{foo} \P{foo}  - white space not allowed within "\p" or "\P"
 *
 * Other than the above restrictions, white space is ignored.  Case
 * is ignored except in "\p" and "\P".
 *
 * This class cannot be instantiated.  It has a public static method,
 * createPropertySet(), with takes a pattern to be parsed and returns
 * a new UnicodeSet.  Another public static method,
 * resemblesPattern(), returns true if a given pattern string appears
 * to be a property set pattern, and therefore should be passed in to
 * createPropertySet().
 *
 * NOTE: Current implementation is incomplete.  The following list
 * indicates which properties are supported.
 *
 *    + GeneralCategory
 *      CombiningClass
 *      BidiClass
 *      DecompositionType
 *    + NumericValue
 *      NumericType
 *      EastAsianWidth
 *      LineBreak
 *      JoiningType
 *    + Script
 *
 * '+' indicates a supported property.
 *
 * @author Alan Liu
 * @version $RCSfile: upropset.h,v $ $Revision: 1.1 $ $Date: 2001/10/17 19:20:41 $
 */
class UnicodePropertySet {

 public:

    //----------------------------------------------------------------
    // Public API
    //----------------------------------------------------------------

    /**
     * Return true if the given position, in the given pattern, appears
     * to be the start of a property set pattern [:foo:], \p{foo}, or
     * \P{foo}.
     */
    static UBool resemblesPattern(const UnicodeString& pattern, int32_t pos);

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
    static UnicodeSet* createFromPattern(const UnicodeString& pattern,
                                         ParsePosition& ppos);

 private:

    //----------------------------------------------------------------
    // Property set factory static methods
    // NOTE: This will change/go away when we implement UCharacter
    // based property retrieval.
    //----------------------------------------------------------------

    typedef UnicodeSet* (*SetFactory)(const UnicodeString& valueName);

    static UnicodeSet* createNumericValueSet(const UnicodeString& valueName);

    /**
     * Given a general category value name, create a corresponding
     * set and return it, or return null if the name is invalid.
     * @param valueName a pre-munged general category value name
     */
    static UnicodeSet* createCategorySet(const UnicodeString& valueName);

    /**
     * Given a script value name, create a corresponding set and
     * return it, or return null if the name is invalid.
     * @param valueName a pre-munged script value name
     */
    static UnicodeSet* createScriptSet(const UnicodeString& valueName);

    //----------------------------------------------------------------
    // Utility methods
    //----------------------------------------------------------------

    /**
     * Returns a UnicodeSet for the given category.  This set is
     * cached and returned again if this method is called again with
     * the same parameter.
     *
     * Callers MUST NOT MODIFY the returned set.
     */
    static const UnicodeSet& getCategorySet(int32_t cat);

    /**
     * Returns a UnicodeSet for the given script.  This set is
     * cached and returned again if this method is called again with
     * the same parameter.
     *
     * Callers MUST NOT MODIFY the returned set.
     */
    static const UnicodeSet& getScriptSet(UScriptCode script);

    /**
     * Given a string, munge it to upper case and lose the whitespace.
     * So "General Category " becomes "GENERALCATEGORY".  We munge all
     * type and value strings, and store all type and value keys
     * pre-munged.
     */
    static UnicodeString munge(const UnicodeString& str, int32_t start, int32_t limit);

    /**
     * Skip over a sequence of zero or more white space characters
     * at pos.  Return the index of the first non-white-space character
     * at or after pos, or str.length(), if there is none.
     */
    static int32_t skipWhitespace(const UnicodeString& str, int32_t pos);

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

    /**
     * A filter that returns TRUE if the given code point should be
     * included in the UnicodeSet being constructed.
     */
    typedef UBool (*Filter)(UChar32 codePoint, void* context);

    /**
     * Set the given UnicodeSet to contain all code points for which
     * filter returns TRUE.  The context parameter is passed unchanged
     * to the filter function.
     */
    static void initSetFromFilter(UnicodeSet& set, Filter filter,
                                  void* context);

    //----------------------------------------------------------------
    // Type and value name maps
    //----------------------------------------------------------------

    /**
     * Add a type mapping to the name map.
     */
    static void addType(const UnicodeString& shortName,
                        const UnicodeString& longName,
                        SetFactory factory);

    /**
     * Add a value mapping to the name map.
     */
    static void addValue(Hashtable* map,
                         const UnicodeString& shortName,
                         const UnicodeString& longName,
                         int32_t value);

    static void init();

 public:
    static void cleanup();

 private:
    //----------------------------------------------------------------
    // SetFactory <=> void*
    // I don't know why the compiler won't cast between these types.
    // They should be interconvertible.  Does C++ distinguish between
    // pointers into code and pointers into data?  In any case, we
    // convert between these types in a safe way here.
    //----------------------------------------------------------------
    
    union SetFactoryTok {
        void*       voidPointer;
        SetFactory  functionPointer;
    };

    inline static void* setFactoryToVoidPtr(SetFactory f) {
        SetFactoryTok tok;
        tok.functionPointer = f;
        return tok.voidPointer;
    }

    inline static SetFactory voidPtrToSetFactory(void* p) {
        SetFactoryTok tok;
        tok.voidPointer = p;
        return tok.functionPointer;
    }
};

U_NAMESPACE_END

#endif
