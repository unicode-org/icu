/*
 *******************************************************************************
 *
 *   Copyright (C) 2002, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  strprep.cpp
 *   encoding:   US-ASCII
 *   tab size:   8 (not used)
 *   indentation:4
 *
 *   created on: 2003feb1
 *   created by: Ram Viswanadha
 */

#ifndef TESTIDNA_H
#define TESTIDNA_H

#include "sprpimpl.h"
#include "intltest.h"
#include "unicode/parseerr.h"

typedef int32_t  
(*TestFunc) (   const UChar *src, int32_t srcLength,
                UChar *dest, int32_t destCapacity,
                int32_t options, UParseError *parseError,
                UErrorCode *status);
typedef int32_t  
(*CompareFunc) (const UChar *s1, int32_t s1Len,
                const UChar *s2, int32_t s2Len,
                int32_t options,
                UErrorCode *status);




// test the API


/**
 * @test
 * @summary General test of HexadecimalToUnicodeTransliterator
 */
class TestIDNA : public IntlTest {
public:
    void runIndexedTest(int32_t index, UBool exec, const char* &name, char* par=NULL);
    void TestDataFile();
    void TestToASCII();
    void TestToUnicode();
    void TestIDNToUnicode();
    void TestIDNToASCII();
    void TestCompare();
    void TestErrorCases();
    void TestChaining();
    void TestRootLabelSeparator();
    void TestCompareReferenceImpl();
    void TestRefIDNA();
    void TestIDNAMonkeyTest();
private:
    void testToASCII(const char* testName, TestFunc func);
    void testToUnicode(const char* testName, TestFunc func);
    void testIDNToUnicode(const char* testName, TestFunc func);
    void testIDNToASCII(const char* testName, TestFunc func);
    void testCompare(const char* testName, CompareFunc func);
    void testChaining(const char* toASCIIName, TestFunc toASCII,
                    const char* toUnicodeName, TestFunc toUnicode);

    // main testing functions
    void testAPI(const UChar *src, const UChar *expected, const char *testName, 
             UBool useSTD3ASCIIRules, UErrorCode expectedStatus,
             UBool doCompare, TestFunc func);

    void testCompare(const UChar* s1, int32_t s1Len,
                        const UChar* s2, int32_t s2Len,
                        const char* testName, CompareFunc func,
                        UBool isEqual);

    void testErrorCases(const char* toASCIIName, TestFunc toASCII,
                    const char* IDNToASCIIName, TestFunc IDNToASCII,
                    const char* IDNToUnicodeName, TestFunc IDNToUnicode);

    void testChaining(UChar* src,int32_t numIterations,const char* testName,
                  UBool useSTD3ASCIIRules, UBool caseInsensitive, TestFunc func);

    void testRootLabelSeparator(const char* testName, CompareFunc func, 
                            const char* IDNToASCIIName, TestFunc IDNToASCII,
                            const char* IDNToUnicodeName, TestFunc IDNToUnicode);

    void testCompareReferenceImpl(const UChar* src, int32_t srcLen);
};

// test the TRIE data structure
int testData(TestIDNA& test);

#endif
