/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * Collation Iterator tests.
 * (Let me reiterate my position...)
 */

#ifndef _ITERCOLL
#define _ITERCOLL

#ifndef _UTYPES
#include "unicode/utypes.h"
#endif

#ifndef _COLL
#include "unicode/coll.h"
#endif

#ifndef _TBLCOLL
#include "unicode/tblcoll.h"
#endif

#ifndef _COLEITR
#include "unicode/coleitr.h"
#endif

#ifndef _INTLTEST
#include "intltest.h"
#endif

#ifndef _UNISTR
#include "unicode/unistr.h"
#endif

class CollationIteratorTest: public IntlTest
{
public:

    enum EToken_Len { MAX_TOKEN_LEN = 128 };

    CollationIteratorTest();
    ~CollationIteratorTest();

    void runIndexedTest(int32_t index, bool_t exec, char* &name, char* par = NULL);

    /**
     * Test for CollationElementIterator.previous()
     *
     * @bug 4108758 - Make sure it works with contracting characters
     * 
     */
    void TestPrevious(char *par);
    
    /**
     * Test for getOffset() and setOffset()
     */
    void TestOffset(char *par);

    /**
     * Test for setText()
     */
    void TestSetText(char *par);
    
    /** @bug 4108762
     * Test for getMaxExpansion()
     */
    void TestMaxExpansion(char *par);

    /*
     * @bug 4157299
     */
    void TestClearBuffers(char *par);
    
    //------------------------------------------------------------------------
    // Internal utilities
    //

private:
    void backAndForth(CollationElementIterator &iter);

    struct ExpansionRecord
    {
        UChar character;
        int32_t count;
    };

    /**
     * Verify that getMaxExpansion works on a given set of collation rules
     */
    void verifyExpansion(UnicodeString rules, ExpansionRecord tests[], int32_t testCount);
    
    /**
     * Return an integer array containing all of the collation orders
     * returned by calls to next on the specified iterator
     */
    int32_t *getOrders(CollationElementIterator &iter, int32_t &orderLength);

    /**
     * Return a string containing all of the collation orders
     * returned by calls to next on the specified iterator
     */
    UnicodeString &orderString(CollationElementIterator &iter, UnicodeString &target);

    void assertEqual(CollationElementIterator &i1, CollationElementIterator &i2);

    RuleBasedCollator *en_us;
    static const UnicodeString test1;
    static const UnicodeString test2;

};

#endif
