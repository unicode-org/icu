/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * G7CollationTest is a third level test class.  This test performs the examples 
 * mentioned on the Taligent international demos web site.  
 * Sample Rules: & Z < p , P 
 * Effect :  Making P sort after Z.
 *
 * Sample Rules: & c < ch , cH, Ch, CH 
 * Effect : As well as adding sequences of characters that act as a single character (this is
 * known as contraction), you can also add characters that act like a sequence of
 * characters (this is known as expansion).  
 * 
 * Sample Rules: & Question'-'mark ; '?' & Hash'-'mark ; '#' & Ampersand ; '&' 
 * Effect : Expansion and contraction can actually be combined.  
 * 
 * Sample Rules: & aa ; a'-' & ee ; e'-' & ii ; i'-' & oo ; o'-' & uu ; u'-'
 * Effect : sorted sequence as the following,
 * aardvark  
 * a-rdvark  
 * abbot  
 * coop  
 * co-p  
 * cop 
 */

#ifndef _G7COLL
#define _G7COLL

#ifndef _UTYPES
#include "unicode/utypes.h"
#endif

#ifndef _COLL
#include "unicode/coll.h"
#endif

#ifndef _TBLCOLL
#include "unicode/tblcoll.h"
#endif

#ifndef _INTLTEST
#include "intltest.h"
#endif

class G7CollationTest: public IntlTest {
public:
    // static constants
    enum EToken_Len { MAX_TOKEN_LEN = 128 };
    enum ETotal_Locales { TESTLOCALES = 12 };
    enum ETotal_Fixed { FIXEDTESTSET = 15 };
    enum ETotal_Test { TOTALTESTSET = 30 };

    G7CollationTest() {}
    virtual ~G7CollationTest() {}
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );

    // main test routine, tests comparisons for a set of strings against sets of expected results
    void doTest( Collator* col, UnicodeString source, UnicodeString target, Collator::EComparisonResult result);

    // perform test for G7 locales
    void TestG7Locales( char* par );

    // perform test with added rules " & Z < p, P"
    void TestDemo1( char* par );

    // perorm test with added rules "& C < ch , cH, Ch, CH"
    void TestDemo2( char* par );

    // perform test with added rules 
    // "& Question'-'mark ; '?' & Hash'-'mark ; '#' & Ampersand ; '&'"
    void TestDemo3( char* par );

    // perform test with added rules 
    // " & aa ; a'-' & ee ; e'-' & ii ; i'-' & oo ; o'-' & uu ; u'-' "
    void TestDemo4( char* par );

private:
    static const UChar testCases[][MAX_TOKEN_LEN];
    static const int32_t results[TESTLOCALES][TOTALTESTSET];

    static const Locale locales[];
};
#endif
