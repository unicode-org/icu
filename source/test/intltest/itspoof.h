/*
**********************************************************************
* Copyright (C) 2009, International Business Machines Corporation 
* and others.  All Rights Reserved.
**********************************************************************
*/

/**
 * IntlTestSpoof is the top level test class for the Unicode Spoof detection tests
 */

#ifndef INTLTESTSPOOF_H
#define INTLTESTSPOOF_H

#include "unicode/utypes.h"
#if !UCONFIG_NO_REGULAR_EXPRESSIONS
#include "unicode/uspoof.h"
#include "intltest.h"


class IntlTestSpoof: public IntlTest {
public:
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );
    
    // Test the USpoofDetector API functions that require C++
    // The pure C part of the API, which is most of it, is tested in cintltst
    void  testSpoofAPI();

    void  testSkeleton();

    void testAreConfusable();
    
    void testInvisible();

    // Internal function to run a single skeleton test case.
    void  checkSkeleton(const USpoofChecker *sc, uint32_t flags, 
                        const char *input, const char *expected, int32_t lineNum);
};

#endif  // !UCONFIG_NO_REGULAR_EXPRESSIONS
#endif
