/*
 *******************************************************************************
 * Copyright (C) 1996-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

#ifndef CANITER_H
#define CANITER_H

#include "unicode/utypes.h"
#include "unicode/uobject.h"
#include "unicode/unistr.h"

#define SKIP_ZEROES TRUE

U_NAMESPACE_BEGIN

class Hashtable;

/**
 * This class allows one to iterate through all the strings that are canonically equivalent to a given
 * string. For example, here are some sample results:
Results for: {LATIN CAPITAL LETTER A WITH RING ABOVE}{LATIN SMALL LETTER D}{COMBINING DOT ABOVE}{COMBINING CEDILLA}
1: \u0041\u030A\u0064\u0307\u0327
 = {LATIN CAPITAL LETTER A}{COMBINING RING ABOVE}{LATIN SMALL LETTER D}{COMBINING DOT ABOVE}{COMBINING CEDILLA}
2: \u0041\u030A\u0064\u0327\u0307
 = {LATIN CAPITAL LETTER A}{COMBINING RING ABOVE}{LATIN SMALL LETTER D}{COMBINING CEDILLA}{COMBINING DOT ABOVE}
3: \u0041\u030A\u1E0B\u0327
 = {LATIN CAPITAL LETTER A}{COMBINING RING ABOVE}{LATIN SMALL LETTER D WITH DOT ABOVE}{COMBINING CEDILLA}
4: \u0041\u030A\u1E11\u0307
 = {LATIN CAPITAL LETTER A}{COMBINING RING ABOVE}{LATIN SMALL LETTER D WITH CEDILLA}{COMBINING DOT ABOVE}
5: \u00C5\u0064\u0307\u0327
 = {LATIN CAPITAL LETTER A WITH RING ABOVE}{LATIN SMALL LETTER D}{COMBINING DOT ABOVE}{COMBINING CEDILLA}
6: \u00C5\u0064\u0327\u0307
 = {LATIN CAPITAL LETTER A WITH RING ABOVE}{LATIN SMALL LETTER D}{COMBINING CEDILLA}{COMBINING DOT ABOVE}
7: \u00C5\u1E0B\u0327
 = {LATIN CAPITAL LETTER A WITH RING ABOVE}{LATIN SMALL LETTER D WITH DOT ABOVE}{COMBINING CEDILLA}
8: \u00C5\u1E11\u0307
 = {LATIN CAPITAL LETTER A WITH RING ABOVE}{LATIN SMALL LETTER D WITH CEDILLA}{COMBINING DOT ABOVE}
9: \u212B\u0064\u0307\u0327
 = {ANGSTROM SIGN}{LATIN SMALL LETTER D}{COMBINING DOT ABOVE}{COMBINING CEDILLA}
10: \u212B\u0064\u0327\u0307
 = {ANGSTROM SIGN}{LATIN SMALL LETTER D}{COMBINING CEDILLA}{COMBINING DOT ABOVE}
11: \u212B\u1E0B\u0327
 = {ANGSTROM SIGN}{LATIN SMALL LETTER D WITH DOT ABOVE}{COMBINING CEDILLA}
12: \u212B\u1E11\u0307
 = {ANGSTROM SIGN}{LATIN SMALL LETTER D WITH CEDILLA}{COMBINING DOT ABOVE}
 *<br>Note: the code is intended for use with small strings, and is not suitable for larger ones,
 * since it has not been optimized for that situation.
 *@author M. Davis
 *@draft
 */
class U_I18N_API CanonicalIterator : public UObject {
public:
    /**
     *@param source    string to get results for
     *@param status    Fill-in parameter which receives the status of this operation.
     */
    CanonicalIterator(const UnicodeString &source, UErrorCode &status);    

    /** Destructor
     *  Cleans pieces
     */
    ~CanonicalIterator();

    /**
     *@return gets the source: NOTE: it is the NFD form of source
     */
    UnicodeString getSource();    

    /**
     * Resets the iterator so that one can start again from the beginning.
     */
    void reset();    

    /**
     *@return the next string that is canonically equivalent. The value null is returned when
     * the iteration is done.
     */
    UnicodeString next();    

    /**
     *@param newSource     the source string to iterate against. This allows the same iterator to be used
     *                     while changing the source string, saving object creation.
     *@param status        Fill-in parameter which receives the status of this operation.
     */
    void setSource(const UnicodeString &newSource, UErrorCode &status);    

    /**
     * Dumb recursive implementation of permutation. 
     * TODO: optimize
     * @param source     the string to find permutations for
     * @param skipZeros  determine if skip zeros
     * @param result     the results in a set.
     * @param status       Fill-in parameter which receives the status of this operation.
     */
    static void permute(UnicodeString &source, UBool skipZeros, Hashtable *result, UErrorCode &status);     
    
    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @draft ICU 2.2
     */
    virtual inline UClassID getDynamicClassID() const { return getStaticClassID(); }

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @draft ICU 2.2
     */
    static inline UClassID getStaticClassID() { return (UClassID)&fgClassID; }

private:
    // ===================== PRIVATES ==============================
    
    // fields
    UnicodeString source;
    UBool done;

    // 2 dimensional array holds the pieces of the string with
    // their different canonically equivalent representations
    UnicodeString **pieces;
    int32_t pieces_length;
    int32_t *pieces_lengths;

    // current is used in iterating to combine pieces
    int32_t *current;
    int32_t current_length;
    
    // transient fields
    UnicodeString buffer;
    
    // we have a segment, in NFD. Find all the strings that are canonically equivalent to it.
    UnicodeString *getEquivalents(const UnicodeString &segment, int32_t &result_len, UErrorCode &status); //private String[] getEquivalents(String segment)
    
    //Set getEquivalents2(String segment);
    Hashtable *getEquivalents2(const UChar *segment, int32_t segLen, UErrorCode &status);
    //Hashtable *getEquivalents2(const UnicodeString &segment, int32_t segLen, UErrorCode &status);
    
    /**
     * See if the decomposition of cp2 is at segment starting at segmentPos 
     * (with canonical rearrangment!)
     * If so, take the remainder, and return the equivalents 
     */
    //Set extract(int comp, String segment, int segmentPos, StringBuffer buffer);
    Hashtable *extract(UChar32 comp, const UChar *segment, int32_t segLen, int32_t segmentPos, UErrorCode &status);
    //Hashtable *extract(UChar32 comp, const UnicodeString &segment, int32_t segLen, int32_t segmentPos, UErrorCode &status);

    void cleanPieces();

    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;
};
U_NAMESPACE_END
#endif
