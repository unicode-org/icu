/*
**********************************************************************
*   Copyright (C) 1999 Alan Liu and others. All rights reserved.
**********************************************************************
*   Date        Name        Description
*   10/20/99    alan        Creation.
*   03/22/2000  Madhu       Added additional tests
**********************************************************************
*/

#ifndef _TESTUNISET
#define _TESTUNISET

#include "unicode/utypes.h"
#include "intltest.h"

class UnicodeSet;
class UnicodeString;

/**
 * UnicodeSet test
 */
class UnicodeSetTest: public IntlTest {

    void runIndexedTest(int32_t index, UBool exec, const char* &name, char* par=NULL);
    
private:

    void TestPatterns(void);
    void TestCategories(void);
    void TestAddRemove(void);
    void TestCloneEqualHash(void);

    /**
     * Make sure minimal representation is maintained.
     */
    void TestMinimalRep(void);

    void TestAPI(void);

    void TestExhaustive(void);

private:

    void _testComplement(int32_t a, UnicodeSet&, UnicodeSet&);

    void _testAdd(int32_t a, int32_t b, UnicodeSet&, UnicodeSet&, UnicodeSet&);

    void _testRetain(int32_t a, int32_t b, UnicodeSet&, UnicodeSet&, UnicodeSet&);

    void _testRemove(int32_t a, int32_t b, UnicodeSet&, UnicodeSet&, UnicodeSet&);

    void _testXor(int32_t a, int32_t b, UnicodeSet&, UnicodeSet&, UnicodeSet&);

    /**
     * Check that ranges are monotonically increasing and non-
     * overlapping.
     */
    void checkCanonicalRep(const UnicodeSet& set, const UnicodeString& msg);

    /**
     * Convert a bitmask to a UnicodeSet.
     */
    static void bitsToSet(int32_t a, UnicodeSet&);

    /**
     * Convert a UnicodeSet to a bitmask.  Only the characters
     * U+0000 to U+0020 are represented in the bitmask.
     */
    static int32_t setToBits(const UnicodeSet& x);

    /**
     * Return the representation of an inversion list based UnicodeSet
     * as a pairs list.  Ranges are listed in ascending Unicode order.
     * For example, the set [a-zA-M3] is represented as "33AMaz".
     */
    static UnicodeString getPairs(const UnicodeSet& set);

    void expectContainment(const UnicodeSet& set,
                           const UnicodeString& setName,
                           const UnicodeString& charsIn,
                           const UnicodeString& charsOut);
    void expectPattern(UnicodeSet& set,
                       const UnicodeString& pattern,
                       const UnicodeString& expectedPairs);
    void expectPairs(const UnicodeSet& set,
                     const UnicodeString& expectedPairs);
    void doAssert(UBool, const char*);
public:
    static UnicodeString escape(const UnicodeString& s);
};

#endif
