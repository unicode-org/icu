/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
//===============================================================================
//
// File apitest.cpp
//
// 
//
// Created by: Helena Shih
//
// Modification History:
//
//  Date         Name          Description
//  2/5/97      aliu        Added streamIn and streamOut methods.  Added
//                          constructor which reads RuleBasedCollator object from
//                          a binary file.  Added writeToFile method which streams
//                          RuleBasedCollator out to a binary file.  The streamIn
//                          and streamOut methods use istream and ostream objects
//                          in binary mode.
//  6/30/97     helena      Added tests for CollationElementIterator::setText, getOffset
//                          setOffset and DecompositionIterator::getOffset, setOffset.  
//                          DecompositionIterator is made public so add class scope
//                          testing.
//  02/10/98    damiba      Added test for compare(UnicodeString&, UnicodeString&, int32_t)
//===============================================================================

#include "unicode/coll.h"
#include "unicode/tblcoll.h"
#include "unicode/coleitr.h"
#include "unicode/sortkey.h"
#include "apicoll.h"
#include "unicode/chariter.h"
#include "unicode/schriter.h"
#include "unicode/ustring.h"

#include "sfwdchit.h"
#include "cmemory.h"

void
CollationAPITest::doAssert(UBool condition, const char *message)
{
    if (!condition) {
        errln(UnicodeString("ERROR : ") + message);
    }
}

/*
 * Test Collator::createInstance(... version...) for some locale. Called by TestProperty().
 */
static void
TestOpenVersion(IntlTest &test, const Locale &locale) {
    UVersionInfo version1, version2;
    Collator *collator1, *collator2;
    UErrorCode errorCode;

    errorCode=U_ZERO_ERROR;
    collator1=Collator::createInstance(locale, errorCode);
    if(U_SUCCESS(errorCode)) {
        /* get the current version */
        collator1->getVersion(version1);
        delete collator1;

        /* try to get that same version again */
        collator2=Collator::createInstance(locale, version1, errorCode);
        if(U_SUCCESS(errorCode)) {
            collator2->getVersion(version2);
            if(0!=uprv_memcmp(version1, version2, sizeof(UVersionInfo))) {
                test.errln("error: Collator::createInstance(\"%s\", (%s collator)->getVersion()) returns a different collator\n", locale.getName(), locale.getName());
            }
            delete collator2;
        } else {
            test.errln("error: Collator::createInstance(\"%s\", (%s collator)->getVersion()) fails: %s\n", locale.getName(), locale.getName(), u_errorName(errorCode));
        }
    }
}

// Collator Class Properties
// ctor, dtor, createInstance, compare, getStrength/setStrength
// getDecomposition/setDecomposition, getDisplayName
void
CollationAPITest::TestProperty(/* char* par */)
{
    UErrorCode success = U_ZERO_ERROR;
    Collator *col = 0;
    UVersionInfo minVersionArray = {0x01, 0x00, 0x00, 0x00};
    UVersionInfo maxVersionArray = {0x01, 0x09, 0x09, 0x09};
    UVersionInfo versionArray;
    int i = 0;

    logln("The property tests begin : ");
    logln("Test ctors : ");
    col = Collator::createInstance(Locale::ENGLISH, success);

    if (U_FAILURE(success))
    {
        errln("Default Collator creation failed.");
        return;
    }
    col->getVersion(versionArray);

    for (i=0; i<4; ++i) {
      if (versionArray[i] < minVersionArray[i] ||
          versionArray[i] > maxVersionArray[i]) {
              errln("Testing Collator::getVersion() failed - unexpected result received");
        break;
      }
    }

    doAssert((col->compare("ab", "abc") == Collator::LESS), "ab < abc comparison failed");
    doAssert((col->compare("ab", "AB") == Collator::LESS), "ab < AB comparison failed");
    doAssert((col->compare("blackbird", "black-bird") == Collator::GREATER), "black-bird > blackbird comparison failed");
    doAssert((col->compare("black bird", "black-bird") == Collator::LESS), "black bird > black-bird comparison failed");
    doAssert((col->compare("Hello", "hello") == Collator::GREATER), "Hello > hello comparison failed");


    /*start of update [Bertrand A. D. 02/10/98]*/
    doAssert((col->compare("ab", "abc", 2) == Collator::EQUAL), "ab = abc with length 2 comparison failed");
    doAssert((col->compare("ab", "AB", 2) == Collator::LESS), "ab < AB  with length 2 comparison failed");
    doAssert((col->compare("ab", "Aa", 1) == Collator::LESS), "ab < Aa  with length 1 comparison failed");
    doAssert((col->compare("ab", "Aa", 2) == Collator::GREATER), "ab > Aa  with length 2 comparison failed");
    doAssert((col->compare("black-bird", "blackbird", 5) == Collator::EQUAL), "black-bird = blackbird with length of 5 comparison failed");
    doAssert((col->compare("black bird", "black-bird", 10) == Collator::LESS), "black bird < black-bird with length 10 comparison failed");
    doAssert((col->compare("Hello", "hello", 5) == Collator::GREATER), "Hello > hello with length 5 comparison failed");
    /*end of update [Bertrand A. D. 02/10/98]*/


    logln("Test ctors ends.");
    logln("testing Collator::getStrength() method ...");
    doAssert((col->getStrength() == Collator::TERTIARY), "collation object has the wrong strength");
    doAssert((col->getStrength() != Collator::PRIMARY), "collation object's strength is primary difference");

        
    logln("testing Collator::setStrength() method ...");
    col->setStrength(Collator::SECONDARY);
    doAssert((col->getStrength() != Collator::TERTIARY), "collation object's strength is secondary difference");
    doAssert((col->getStrength() != Collator::PRIMARY), "collation object's strength is primary difference");
    doAssert((col->getStrength() == Collator::SECONDARY), "collation object has the wrong strength");

    logln("testing Collator::setDecomposition() method ...");
    col->setDecomposition(Normalizer::NO_OP);
    doAssert((col->getDecomposition() != Normalizer::DECOMP), "collation object's strength is secondary difference");
    doAssert((col->getDecomposition() != Normalizer::DECOMP_COMPAT), "collation object's strength is primary difference");
    doAssert((col->getDecomposition() == Normalizer::NO_OP), "collation object has the wrong strength");

    UnicodeString name;

    logln("Get display name for the US English collation in German : ");
    logln(Collator::getDisplayName(Locale::US, Locale::GERMAN, name));
    doAssert((name == UnicodeString("Englisch (Vereinigte Staaten)")), "getDisplayName failed");

    logln("Get display name for the US English collation in English : ");
    logln(Collator::getDisplayName(Locale::US, Locale::ENGLISH, name)); 
    doAssert((name == UnicodeString("English (United States)")), "getDisplayName failed");
#if 0
    // weiv : this test is bogus if we're running on any machine that has different default locale than English.
    // Therefore, it is banned!
    logln("Get display name for the US English in default locale language : ");
    logln(Collator::getDisplayName(Locale::US, name)); 
    doAssert((name == UnicodeString("English (United States)")), "getDisplayName failed if this is an English machine");
#endif
    delete col; col = 0;
    RuleBasedCollator *rcol = (RuleBasedCollator *)Collator::createInstance("da_DK", 
                                                                            success);
    doAssert(rcol->getRules().length() != 0, "da_DK rules does not have length 0");
    delete rcol;

    col = Collator::createInstance(Locale::FRENCH, success);
    if (U_FAILURE(success))
    {
        errln("Creating French collation failed.");
        return;
    }

    col->setStrength(Collator::PRIMARY);
    logln("testing Collator::getStrength() method again ...");
    doAssert((col->getStrength() != Collator::TERTIARY), "collation object has the wrong strength");
    doAssert((col->getStrength() == Collator::PRIMARY), "collation object's strength is not primary difference");
        
    logln("testing French Collator::setStrength() method ...");
    col->setStrength(Collator::TERTIARY);
    doAssert((col->getStrength() == Collator::TERTIARY), "collation object's strength is not tertiary difference");
    doAssert((col->getStrength() != Collator::PRIMARY), "collation object's strength is primary difference");
    doAssert((col->getStrength() != Collator::SECONDARY), "collation object's strength is secondary difference");

    logln("Create junk collation: ");
    Locale abcd("ab", "CD", "");
    success = U_ZERO_ERROR;
    Collator *junk = 0;
    junk = Collator::createInstance(abcd, success);

    if (U_FAILURE(success))
    {
        errln("Junk collation creation failed, should at least return default.");
        delete col;
        return;
    }

    delete col;
    col = Collator::createInstance(success);
    if (U_FAILURE(success))
    {
        errln("Creating default collator failed.");
        delete junk;
        return;
    }

    doAssert(((RuleBasedCollator *)col)->getRules() == ((RuleBasedCollator *)junk)->getRules(), 
               "The default collation should be returned.");
    Collator *frCol = Collator::createInstance(Locale::FRANCE, success);
    if (U_FAILURE(success))
    {
        errln("Creating French collator failed.");
        delete col; delete junk;
        return;
    }

    doAssert((*frCol != *junk), "The junk is the same as the French collator.");
    Collator *aFrCol = frCol->clone();
    doAssert((*frCol == *aFrCol), "The cloning of a French collator failed.");
    logln("Collator property test ended.");

    delete col;
    delete frCol;
    delete aFrCol;
    delete junk;

    /* test Collator::createInstance(...version...) */
    TestOpenVersion(*this, "");
    TestOpenVersion(*this, "da");
    TestOpenVersion(*this, "fr");
    TestOpenVersion(*this, "ja");

    /* try some bogus version */
    versionArray[0]=0;
    versionArray[1]=0x99;
    versionArray[2]=0xc7;
    versionArray[3]=0xfe;
    col=Collator::createInstance(Locale(), versionArray, success);
    if(U_SUCCESS(success)) {
        errln("error: ucol_openVersion(bogus version) succeeded");
        delete col;
    }
}

void 
CollationAPITest::TestRuleBasedColl()
{
    RuleBasedCollator *col1, *col2, *col3, *col4;
    UErrorCode status = U_ZERO_ERROR;

    UnicodeString ruleset1("&9 < a, A < b, B < c, C; ch, cH, Ch, CH < d, D, e, E");
    UnicodeString ruleset2("&9 < a, A < b, B < c, C < d, D, e, E");

    col1 = new RuleBasedCollator(ruleset1, status);
    if (U_FAILURE(status)) {
        errln("RuleBased Collator creation failed.\n");
        return;
    }
    else {
        logln("PASS: RuleBased Collator creation passed\n");
    }

    status = U_ZERO_ERROR;
    col2 = new RuleBasedCollator(ruleset2, status);
    if (U_FAILURE(status)) {
        errln("RuleBased Collator creation failed.\n");
        return;
    }
    else {
        logln("PASS: RuleBased Collator creation passed\n");
    }

    status = U_ZERO_ERROR;
    Locale locale("aa", "AA");
    col3 = (RuleBasedCollator *)Collator::createInstance(locale, status);
    if (U_FAILURE(status)) {
        errln("Fallback Collator creation failed.: %s\n");
        return;
    }
    else {
        logln("PASS: Fallback Collator creation passed\n");
    }
    delete col3;

    status = U_ZERO_ERROR;
    col3 = (RuleBasedCollator *)Collator::createInstance(status);
    if (U_FAILURE(status)) {
        errln("Default Collator creation failed.: %s\n");
        return;
    }
    else {
        logln("PASS: Default Collator creation passed\n");
    }

    UnicodeString rule1 = col1->getRules();
    UnicodeString rule2 = col2->getRules();
    UnicodeString rule3 = col3->getRules();

    doAssert(rule1 != rule2, "Default collator getRules failed");
    doAssert(rule2 != rule3, "Default collator getRules failed");
    doAssert(rule1 != rule3, "Default collator getRules failed");
    
    col4 = new RuleBasedCollator(rule2, status);
    if (U_FAILURE(status)) {
        errln("RuleBased Collator creation failed.\n");
        return;
    }

    UnicodeString rule4 = col4->getRules();
    doAssert(rule2 == rule4, "Default collator getRules failed");
    int32_t length4 = 0;
    uint8_t *clonedrule4 = col4->cloneRuleData(length4, status);
    if (U_FAILURE(status)) {
        errln("Cloned rule data failed.\n");
        return;
    }

    free(clonedrule4);
    delete col1;
    delete col2;
    delete col3;
    delete col4;
}

void 
CollationAPITest::TestDecomposition() {
  UErrorCode status = U_ZERO_ERROR;
  Collator *en_US = Collator::createInstance("en_US", status),
    *el_GR = Collator::createInstance("el_GR", status),
    *vi_VN = Collator::createInstance("vi_VN", status);

  if (U_FAILURE(status)) {
    errln("ERROR: collation creation failed.\n");
    return;
  }

  /* there is no reason to have canonical decomposition in en_US OR default locale */
  if (vi_VN->getDecomposition() != Normalizer::DECOMP)
  {
    errln("ERROR: vi_VN collation did not have cannonical decomposition for normalization!\n");
  }

  if (el_GR->getDecomposition() != Normalizer::DECOMP)
  {
    errln("ERROR: el_GR collation did not have cannonical decomposition for normalization!\n");
  }

  if (en_US->getDecomposition() != Normalizer::NO_OP)
  {
    errln("ERROR: en_US collation had cannonical decomposition for normalization!\n");
  }

  delete en_US;
  delete el_GR;
  delete vi_VN;
}

void 
CollationAPITest::TestSafeClone() {
    static const int CLONETEST_COLLATOR_COUNT = 3;
    Collator *someCollators [CLONETEST_COLLATOR_COUNT];
    Collator *col;
    UErrorCode err = U_ZERO_ERROR;
    int index;

    UnicodeString test1("abCda");
    UnicodeString test2("abcda");

    /* one default collator & two complex ones */
    someCollators[0] = Collator::createInstance(err);
    someCollators[1] = Collator::createInstance("ko", err);
    someCollators[2] = Collator::createInstance("ja_JP", err);

    /* change orig & clone & make sure they are independent */

    for (index = 0; index < CLONETEST_COLLATOR_COUNT; index++)
    {
        col = someCollators[index]->safeClone();
        if (col == 0) {
            errln("SafeClone of collator should not return null\n");
            break;
        }
        col->setStrength(Collator::TERTIARY);
        someCollators[index]->setStrength(Collator::PRIMARY);
        col->setAttribute(UCOL_CASE_LEVEL, UCOL_OFF, err);
        someCollators[index]->setAttribute(UCOL_CASE_LEVEL, UCOL_OFF, err);

        doAssert(col->greater(test1, test2), "Result should be \"abCda\" >>> \"abcda\" ");
        doAssert(someCollators[index]->equals(test1, test2), "Result should be \"abcda\" == \"abCda\"");
        delete col;
        delete someCollators[index];
    }
}

void 
CollationAPITest::TestHashCode(/* char* par */)
{
    logln("hashCode tests begin.");
    UErrorCode success = U_ZERO_ERROR;
    Collator *col1 = 0;
    col1 = Collator::createInstance(Locale::ENGLISH, success);
    if (U_FAILURE(success))
    {
        errln("Default collation creation failed.");
        return;
    }

    Collator *col2 = 0;
    Locale dk("da", "DK", "");
    col2 = Collator::createInstance(dk, success);
    if (U_FAILURE(success))
    {
        errln("Danish collation creation failed.");
        return;
    }

    Collator *col3 = 0;
    col3 = Collator::createInstance(Locale::ENGLISH, success);
    if (U_FAILURE(success))
    {
        errln("2nd default collation creation failed.");
        return;
    }

    logln("Collator::hashCode() testing ...");
    
    doAssert(col1->hashCode() != col2->hashCode(), "Hash test1 result incorrect" );                 
    doAssert(!(col1->hashCode() == col2->hashCode()), "Hash test2 result incorrect" );              
    doAssert(col1->hashCode() == col3->hashCode(), "Hash result not equal" );               

    logln("hashCode tests end.");
    delete col1;
    delete col2;

    UnicodeString test1("Abcda");
    UnicodeString test2("abcda");
    
    CollationKey sortk1, sortk2, sortk3;
    UErrorCode status = U_ZERO_ERROR;
                
    col3->getCollationKey(test1, sortk1, status);
    col3->getCollationKey(test2, sortk2, status); 
    col3->getCollationKey(test2, sortk3, status); 
    
    doAssert(sortk1.hashCode() != sortk2.hashCode(), "Hash test1 result incorrect");               
    doAssert(sortk2.hashCode() == sortk3.hashCode(), "Hash result not equal" );

    delete col3;
}

//----------------------------------------------------------------------------
// CollationKey -- Tests the CollationKey methods
//
void
CollationAPITest::TestCollationKey(/* char* par */)
{       
    logln("testing CollationKey begins...");
    Collator *col = 0;
    UErrorCode success=U_ZERO_ERROR;
    col = Collator::createInstance(Locale::ENGLISH, success);
    col->setStrength(Collator::TERTIARY);
    if (U_FAILURE(success))
    {
        errln("Default collation creation failed.");
        return;
    }

    CollationKey sortk1, sortk2;
    UnicodeString test1("Abcda"), test2("abcda");
    UErrorCode key1Status = U_ZERO_ERROR, key2Status = U_ZERO_ERROR;

    logln("Testing weird arguments");
    col->getCollationKey(NULL, 0, sortk1, key1Status);
    // key gets reset here
    int32_t length;
    sortk1.getByteArray(length);
    doAssert(sortk1.isBogus() == FALSE && length == 0, 
             "Empty string should return an empty collation key");
    // bogus key returned here
    key1Status = U_ILLEGAL_ARGUMENT_ERROR;
    col->getCollationKey(NULL, 0, sortk1, key1Status);
    doAssert(sortk1.getByteArray(length) == NULL && length == 0, 
        "Error code should return bogus collation key");

    key1Status = U_ZERO_ERROR;
    logln("Use tertiary comparison level testing ....");

    col->getCollationKey(test1, sortk1, key1Status);
    doAssert((sortk1.compareTo(col->getCollationKey(test2, sortk2, key2Status))) 
                 == Collator::GREATER, 
                "Result should be \"Abcda\" >>> \"abcda\"");

    CollationKey sortk3(sortk2), sortkNew, sortkEmpty;

    
    sortkNew = sortk1;
    doAssert((sortk1 != sortk2), "The sort keys should be different");
    doAssert((sortk1.hashCode() != sortk2.hashCode()), "sort key hashCode() failed");
    doAssert((sortk2 == sortk3), "The sort keys should be the same");
    doAssert((sortk1 == sortkNew), "The sort keys assignment failed");
    doAssert((sortk1.hashCode() == sortkNew.hashCode()), "sort key hashCode() failed");
    doAssert((sortkNew != sortk3), "The sort keys should be different");
    doAssert(sortk1.compareTo(sortk3) == Collator::GREATER, "Result should be \"Abcda\" >>> \"abcda\"");
    doAssert(sortk2.compareTo(sortk3) == Collator::EQUAL, "Result should be \"abcda\" == \"abcda\"");
    doAssert(sortkEmpty.compareTo(sortk1) == Collator::LESS, "Result should be (empty key) <<< \"Abcda\"");
    doAssert(sortk1.compareTo(sortkEmpty) == Collator::GREATER, "Result should be \"Abcda\" >>> (empty key)");
    doAssert(sortkEmpty.compareTo(sortkEmpty) == Collator::EQUAL, "Result should be (empty key) == (empty key)");

    int32_t    cnt1, cnt2, cnt3, cnt4;
    uint8_t* byteArray1 = 0;

    byteArray1 = sortk1.toByteArray(cnt1);
    uint8_t* byteArray2 = 0;
    
    byteArray2 = sortk2.toByteArray(cnt2);
    /* 
    this is a bad test since it is dependent on the version of uca data,
    which changes
    will remove it.
    const char sortk2_compat[] = { 
        // this is a 1.8 sortkey
        0x17, 0x19, 0x1B, 0x1D, 0x17, 0x01, 0x08, 0x01, 0x08, 0x00
    };
    */

    const uint8_t* byteArray3 = 0;
    byteArray3 = sortk1.getByteArray(cnt3);

    const uint8_t* byteArray4 = 0;
    byteArray4 = sortk2.getByteArray(cnt4);

    CollationKey sortk4(byteArray1, cnt1), sortk5(byteArray2, cnt2);
    CollationKey sortk6(byteArray3, cnt3), sortk7(byteArray4, cnt4);

    /*
    doAssert(memcmp(byteArray2, sortk2_compat, strlen(sortk2_compat)) == 0, 
             "Binary format for 'abcda' sortkey different!");
    */
    doAssert(sortk1.compareTo(sortk4) == Collator::EQUAL, "CollationKey::toByteArray(sortk1) Failed.");
    doAssert(sortk2.compareTo(sortk5) == Collator::EQUAL, "CollationKey::toByteArray(sortk2) Failed.");
    doAssert(sortk4.compareTo(sortk5) == Collator::GREATER, "sortk4 >>> sortk5 Failed");
    doAssert(sortk1.compareTo(sortk6) == Collator::EQUAL, "CollationKey::getByteArray(sortk1) Failed.");
    doAssert(sortk2.compareTo(sortk7) == Collator::EQUAL, "CollationKey::getByteArray(sortk2) Failed.");
    doAssert(sortk6.compareTo(sortk7) == Collator::GREATER, "sortk6 >>> sortk7 Failed");

    logln("Equality tests : ");
    doAssert(sortk1 == sortk4, "sortk1 == sortk4 Failed.");
    doAssert(sortk2 == sortk5, "sortk2 == sortk5 Failed.");
    doAssert(sortk1 != sortk5, "sortk1 != sortk5 Failed.");
    doAssert(sortk1 == sortk6, "sortk1 == sortk6 Failed.");
    doAssert(sortk2 == sortk7, "sortk2 == sortk7 Failed.");
    doAssert(sortk1 != sortk7, "sortk1 != sortk7 Failed.");

    delete [] byteArray1; byteArray1 = 0;
    delete [] byteArray2; byteArray2 = 0;

    sortk3 = sortk1;
    doAssert(sortk1 == sortk3, "sortk1 = sortk3 assignment Failed.");
    doAssert(sortk2 != sortk3, "sortk2 != sortk3 Failed.");
    logln("testing sortkey ends...");

    col->setStrength(Collator::SECONDARY);
    doAssert(col->getCollationKey(test1, sortk1, key1Status).compareTo(
                                  col->getCollationKey(test2, sortk2, key2Status)) 
                                  == Collator::EQUAL, 
                                  "Result should be \"Abcda\" == \"abcda\"");
    delete col;
}

//----------------------------------------------------------------------------
// Tests the CollatorElementIterator class.
// ctor, RuleBasedCollator::createCollationElementIterator(), operator==, operator!=
// 
void
CollationAPITest::TestElemIter(/* char* par */)
{       
    logln("testing sortkey begins...");
    Collator *col = 0;
    UErrorCode success = U_ZERO_ERROR;
    col = Collator::createInstance(Locale::ENGLISH, success);
    if (U_FAILURE(success))
    {
        errln("Default collation creation failed.");
        return;
    }
       
    UnicodeString testString1("XFILE What subset of all possible test cases has the highest probability of detecting the most errors?");
    UnicodeString testString2("Xf_ile What subset of all possible test cases has the lowest probability of detecting the least errors?");
    logln("Constructors and comparison testing....");
    CollationElementIterator *iterator1 = ((RuleBasedCollator*)col)->createCollationElementIterator(testString1);
    
    CharacterIterator *chariter=new StringCharacterIterator(testString1);
    CollationElementIterator *coliter=((RuleBasedCollator*)col)->createCollationElementIterator(*chariter);
    
    // copy ctor
    CollationElementIterator *iterator2 = ((RuleBasedCollator*)col)->createCollationElementIterator(testString1);
    CollationElementIterator *iterator3 = ((RuleBasedCollator*)col)->createCollationElementIterator(testString2);

    UTextOffset offset = iterator1->getOffset();
    if (offset != 0) {
        errln("Error in getOffset for collation element iterator\n");
        return;
    }
    iterator1->setOffset(6, success);
    if (U_FAILURE(success)) {
        errln("Error in setOffset for collation element iterator\n");
        return;
    }
    iterator1->setOffset(0, success);
    int32_t order1, order2, order3;
    doAssert((*iterator1 == *iterator2), "The two iterators should be the same"); 
    doAssert((*iterator1 != *iterator3), "The two iterators should be different");
    
    doAssert((*coliter == *iterator1), "The two iterators should be the same");
    doAssert((*coliter == *iterator2), "The two iterators should be the same");
    doAssert((*coliter != *iterator3), "The two iterators should be different");

    order1 = iterator1->next(success);
    if (U_FAILURE(success))
    {
        errln("Somehow ran out of memory stepping through the iterator.");
        return;
    }
    
    doAssert((*iterator1 != *iterator2), "The first iterator advance failed");
    order2 = iterator2->getOffset();
    doAssert((order1 != order2), "The order result should not be the same");
    order2 = iterator2->next(success);
    if (U_FAILURE(success))
    {
        errln("Somehow ran out of memory stepping through the iterator.");
        return;
    }

    doAssert((*iterator1 == *iterator2), "The second iterator advance failed"); 
    doAssert((order1 == order2), "The order result should be the same");
    order3 = iterator3->next(success);
    if (U_FAILURE(success))
    {
        errln("Somehow ran out of memory stepping through the iterator.");
        return;
    }

    doAssert((CollationElementIterator::primaryOrder(order1) == 
        CollationElementIterator::primaryOrder(order3)), "The primary orders should be the same");
    doAssert((CollationElementIterator::secondaryOrder(order1) == 
        CollationElementIterator::secondaryOrder(order3)), "The secondary orders should be the same");
    doAssert((CollationElementIterator::tertiaryOrder(order1) == 
        CollationElementIterator::tertiaryOrder(order3)), "The tertiary orders should be the same");

    order1 = iterator1->next(success); order3 = iterator3->next(success);
    if (U_FAILURE(success)) 
    {
        errln("Somehow ran out of memory stepping through the iterator.");
        return;
    }

    doAssert((CollationElementIterator::primaryOrder(order1) == 
        CollationElementIterator::primaryOrder(order3)), "The primary orders should be identical");
    doAssert((CollationElementIterator::tertiaryOrder(order1) != 
        CollationElementIterator::tertiaryOrder(order3)), "The tertiary orders should be different");

    order1 = iterator1->next(success); 
    order3 = iterator3->next(success);
    doAssert((CollationElementIterator::secondaryOrder(order1) != 
        CollationElementIterator::secondaryOrder(order3)), "The secondary orders should not be the same");
    doAssert((order1 != CollationElementIterator::NULLORDER), "Unexpected end of iterator reached");

    iterator1->reset(); iterator2->reset(); iterator3->reset();
    order1 = iterator1->next(success);
    if (U_FAILURE(success))
    {
        errln("Somehow ran out of memory stepping through the iterator.");
        return;
    }

    doAssert((*iterator1 != *iterator2), "The first iterator advance failed");

    order2 = iterator2->next(success);
    if (U_FAILURE(success))
    {
        errln("Somehow ran out of memory stepping through the iterator.");
        return;
    }

    doAssert((*iterator1 == *iterator2), "The second iterator advance failed");
    doAssert((order1 == order2), "The order result should be the same");

    order3 = iterator3->next(success);
    if (U_FAILURE(success))
    {
        errln("Somehow ran out of memory stepping through the iterator.");
        return;
    }

    doAssert((CollationElementIterator::primaryOrder(order1) == 
        CollationElementIterator::primaryOrder(order3)), "The primary orders should be the same");
    doAssert((CollationElementIterator::secondaryOrder(order1) == 
        CollationElementIterator::secondaryOrder(order3)), "The secondary orders should be the same");
    doAssert((CollationElementIterator::tertiaryOrder(order1) == 
        CollationElementIterator::tertiaryOrder(order3)), "The tertiary orders should be the same");

    order1 = iterator1->next(success); order2 = iterator2->next(success); order3 = iterator3->next(success);
    if (U_FAILURE(success))
    {
        errln("Somehow ran out of memory stepping through the iterator.");
        return;
    }

    doAssert((CollationElementIterator::primaryOrder(order1) == 
        CollationElementIterator::primaryOrder(order3)), "The primary orders should be identical");
    doAssert((CollationElementIterator::tertiaryOrder(order1) != 
        CollationElementIterator::tertiaryOrder(order3)), "The tertiary orders should be different");

    order1 = iterator1->next(success); order3 = iterator3->next(success);
    if (U_FAILURE(success))
    {
        errln("Somehow ran out of memory stepping through the iterator.");
        return;
    }

    doAssert((CollationElementIterator::secondaryOrder(order1) != 
        CollationElementIterator::secondaryOrder(order3)), "The secondary orders should not be the same");
    doAssert((order1 != CollationElementIterator::NULLORDER), "Unexpected end of iterator reached");
    doAssert((*iterator2 != *iterator3), "The iterators should be different");

    
    //test error values
    success=U_UNSUPPORTED_ERROR;
    Collator *colerror=NULL;
    colerror=Collator::createInstance(Locale::ENGLISH, success);
    if (colerror != 0 || success == U_ZERO_ERROR){
        errln("Error: createInstance(UErrorCode != U_ZERO_ERROR) should just return and not create an instance\n");
    }
    int32_t position=coliter->previous(success);
    if(position != CollationElementIterator::NULLORDER){
        errln((UnicodeString)"Expected NULLORDER got" + position);
    }
    coliter->reset();
    coliter->setText(*chariter, success);
    if(!U_FAILURE(success)){
        errln("Expeceted error");
    }
    iterator1->setText((UnicodeString)"hello there", success);
    if(!U_FAILURE(success)){
        errln("Expeceted error");
    }
    
    delete chariter;
    delete coliter;
    delete iterator1;
    delete iterator2;
    delete iterator3;
    delete col;

    

    logln("testing CollationElementIterator ends...");
}

// Test RuleBasedCollator ctor, dtor, operator==, operator!=, clone, copy, and getRules
void
CollationAPITest::TestOperators(/* char* par */)
{
    UErrorCode success = U_ZERO_ERROR;
    UnicodeString ruleset1("< a, A < b, B < c, C; ch, cH, Ch, CH < d, D, e, E");
    UnicodeString ruleset2("< a, A < b, B < c, C < d, D, e, E");
    RuleBasedCollator *col1 = new RuleBasedCollator(ruleset1, success);
    if (U_FAILURE(success)) {
        errln("RuleBasedCollator creation failed.");
        return;
    }
    success = U_ZERO_ERROR;
    RuleBasedCollator *col2 = new RuleBasedCollator(ruleset2, success);
    if (U_FAILURE(success)) {
        errln("The RuleBasedCollator constructor failed when building with the 2nd rule set.");
        return;
    }
    logln("The operator tests begin : ");
    logln("testing operator==, operator!=, clone  methods ...");
    doAssert((*col1 != *col2), "The two different table collations compared equal");
    *col1 = *col2;
    doAssert((*col1 == *col2), "Collator objects not equal after assignment (operator=)");
        
    success = U_ZERO_ERROR;
    Collator *col3 = Collator::createInstance(Locale::ENGLISH, success);
    if (U_FAILURE(success)) {
        errln("Default collation creation failed.");
        return;
    }
    doAssert((*col1 != *col3), "The two different table collations compared equal");
    Collator* col4 = col1->clone();
    Collator* col5 = col3->clone();
    doAssert((*col1 == *col4), "Cloned collation objects not equal");
    doAssert((*col3 != *col4), "Two different table collations compared equal");
    doAssert((*col3 == *col5), "Cloned collation objects not equal");
    doAssert((*col4 != *col5), "Two cloned collations compared equal");

    const UnicodeString& defRules = ((RuleBasedCollator*)col3)->getRules();
    RuleBasedCollator* col6 = new RuleBasedCollator(defRules, success);
    if (U_FAILURE(success)) {
        errln("Creating default collation with rules failed.");
        return;
    }
    doAssert((((RuleBasedCollator*)col3)->getRules() == col6->getRules()), "Default collator getRules failed");

    success = U_ZERO_ERROR;
    RuleBasedCollator *col7 = new RuleBasedCollator(ruleset2, Collator::TERTIARY, success);
    if (U_FAILURE(success)) {
        errln("The RuleBasedCollator constructor failed when building with the 2nd rule set with tertiary strength.");
        return;
    }
    success = U_ZERO_ERROR;
    RuleBasedCollator *col8 = new RuleBasedCollator(ruleset2, Normalizer::NO_OP, success);
    if (U_FAILURE(success)) {
        errln("The RuleBasedCollator constructor failed when building with the 2nd rule set with Normalizer::NO_OP.");
        return;
    }
    success = U_ZERO_ERROR;
    RuleBasedCollator *col9 = new RuleBasedCollator(ruleset2, Collator::PRIMARY, Normalizer::DECOMP_COMPAT, success);
    if (U_FAILURE(success)) {
        errln("The RuleBasedCollator constructor failed when building with the 2nd rule set with tertiary strength and Normalizer::NO_OP.");
        return;
    }
  //  doAssert((*col7 == *col8), "The two equal table collations compared different");
    doAssert((*col7 != *col9), "The two different table collations compared equal");
    doAssert((*col8 != *col9), "The two different table collations compared equal");



    logln("operator tests ended.");
    delete col1;
    delete col2;
    delete col3;
    delete col4;
    delete col5;
    delete col6;
    delete col7;
    delete col8;
    delete col9;
}

// test clone and copy
void 
CollationAPITest::TestDuplicate(/* char* par */)
{
    UErrorCode status = U_ZERO_ERROR;
    Collator *col1 = Collator::createInstance(Locale::ENGLISH, status);
    if (U_FAILURE(status)) {
        logln("Default collator creation failed.");
        return;
    }
    Collator *col2 = col1->clone();
    doAssert((*col1 == *col2), "Cloned object is not equal to the orginal");
    UnicodeString ruleset("< a, A < b, B < c, C < d, D, e, E");
    RuleBasedCollator *col3 = new RuleBasedCollator(ruleset, status);
    doAssert((*col1 != *col3), "Cloned object is equal to some dummy");
    *col3 = *((RuleBasedCollator*)col1);
    doAssert((*col1 == *col3), "Copied object is not equal to the orginal");
    delete col1;
    delete col2;
    delete col3;
}   

void
CollationAPITest::TestCompare(/* char* par */)
{
    logln("The compare tests begin : ");
    Collator *col = 0;
    UErrorCode success = U_ZERO_ERROR;
    col = Collator::createInstance(Locale::ENGLISH, success);
    if (U_FAILURE(success)) {
        errln("Default collation creation failed.");
        return;
    }
    UnicodeString test1("Abcda"), test2("abcda");
    logln("Use tertiary comparison level testing ....");
                
    doAssert((!col->equals(test1, test2) ), "Result should be \"Abcda\" != \"abcda\"");
    doAssert((col->greater(test1, test2) ), "Result should be \"Abcda\" >>> \"abcda\"");
    doAssert((col->greaterOrEqual(test1, test2) ), "Result should be \"Abcda\" >>> \"abcda\"");    

    col->setStrength(Collator::SECONDARY);
    logln("Use secondary comparison level testing ....");
                
    doAssert((col->equals(test1, test2) ), "Result should be \"Abcda\" == \"abcda\"");
    doAssert((!col->greater(test1, test2) ), "Result should be \"Abcda\" == \"abcda\"");
    doAssert((col->greaterOrEqual(test1, test2) ), "Result should be \"Abcda\" == \"abcda\""); 

    col->setStrength(Collator::PRIMARY);
    logln("Use primary comparison level testing ....");
    
    doAssert((col->equals(test1, test2) ), "Result should be \"Abcda\" == \"abcda\"");
    doAssert((!col->greater(test1, test2) ), "Result should be \"Abcda\" == \"abcda\"");
    doAssert((col->greaterOrEqual(test1, test2) ), "Result should be \"Abcda\" == \"abcda\""); 
    logln("The compare tests end.");
    delete col;
}

void
CollationAPITest::TestGetAll(/* char* par */)
{
    int32_t count;
    const Locale* list = Collator::getAvailableLocales(count);
    for (int32_t i = 0; i < count; ++i) {
        UnicodeString locName, dispName;
        log("Locale name: "); 
        log(list[i].getName());
        log(" , the display name is : ");
        logln(list[i].getDisplayName(dispName));
    }
}

void CollationAPITest::TestSortKey()
{
    UErrorCode status = U_ZERO_ERROR;
    /* 
    this is supposed to open default date format, but later on it treats 
    it like it is "en_US" 
    - very bad if you try to run the tests on machine where default 
      locale is NOT "en_US" 
    */
    Collator *col = Collator::createInstance(Locale::ENGLISH, status);
    if (U_FAILURE(status)) {
        errln("ERROR: Default collation creation failed.: %s\n", u_errorName(status));
        return;
    }

    if (col->getStrength() != Collator::TERTIARY)
    {
        errln("ERROR: default collation did not have UCOL_DEFAULT_STRENGTH !\n");
    }

    /* Need to use identical strength */
    col->setAttribute(UCOL_STRENGTH, UCOL_IDENTICAL, status);

    uint8_t key2compat[] = { 
        /* 2.0 key */
        0x19, 0x1B, 0x1D, 0x1F, 0x19, 
        0x01, 0x09, 0x01, 0x09, 0x01,
        0x18, 0x01, 
        0x92, 0x93, 0x94, 0x95, 0x92, 
        0x00
        /* 1.8.1 key.*/
        /*
        0x19, 0x1B, 0x1D, 0x1F, 0x19, 
        0x01, 0x0A, 0x01, 0x0A, 0x01, 
        0x92, 0x93, 0x94, 0x95, 0x92, 
        0x00 
        */
    };
    
    UChar test1[6] = {0x41, 0x62, 0x63, 0x64, 0x61, 0}, 
          test2[6] = {0x61, 0x62, 0x63, 0x64, 0x61, 0}, 
          test3[6] = {0x61, 0x62, 0x63, 0x64, 0x61, 0};

    uint8_t sortkey1[64];
    uint8_t sortkey2[64];
    uint8_t sortkey3[64];

    logln("Use tertiary comparison level testing ....\n");

    CollationKey key1;
    col->getCollationKey(test1, u_strlen(test1), key1, status);

    CollationKey key2;
    col->getCollationKey(test2, u_strlen(test2), key2, status);

    CollationKey key3;
    col->getCollationKey(test3, u_strlen(test3), key3, status);

    doAssert(key1.compareTo(key2) == Collator::GREATER, 
        "Result should be \"Abcda\" > \"abcda\"");
    doAssert(key2.compareTo(key1) == Collator::LESS, 
        "Result should be \"abcda\" < \"Abcda\"");
    doAssert(key2.compareTo(key3) == Collator::EQUAL, 
        "Result should be \"abcda\" ==  \"abcda\"");

    int32_t keylength = 0;
    doAssert(strcmp((const char *)(key2.getByteArray(keylength)), 
                    (const char *)key2compat) == 0, 
        "Binary format for 'abcda' sortkey different!");

    col->getSortKey(test1, sortkey1, 64);
    col->getSortKey(test2, sortkey2, 64);
    col->getSortKey(test3, sortkey3, 64);

    const uint8_t *tempkey = key1.getByteArray(keylength);
    doAssert(memcmp(tempkey, sortkey1, keylength) == 0, 
        "Test1 string should have the same collation key and sort key");
    tempkey = key2.getByteArray(keylength);
    doAssert(memcmp(tempkey, sortkey2, keylength) == 0, 
        "Test2 string should have the same collation key and sort key");
    tempkey = key3.getByteArray(keylength);
    doAssert(memcmp(tempkey, sortkey3, keylength) == 0, 
        "Test3 string should have the same collation key and sort key");

    col->getSortKey(test1, 5, sortkey1, 64);
    col->getSortKey(test2, 5, sortkey2, 64);
    col->getSortKey(test3, 5, sortkey3, 64);

    tempkey = key1.getByteArray(keylength);
    doAssert(memcmp(tempkey, sortkey1, keylength) == 0, 
        "Test1 string should have the same collation key and sort key");
    tempkey = key2.getByteArray(keylength);
    doAssert(memcmp(tempkey, sortkey2, keylength) == 0, 
        "Test2 string should have the same collation key and sort key");
    tempkey = key3.getByteArray(keylength);
    doAssert(memcmp(tempkey, sortkey3, keylength) == 0, 
        "Test3 string should have the same collation key and sort key");

    UnicodeString strtest1(test1);
    col->getSortKey(strtest1, sortkey1, 64);
    UnicodeString strtest2(test2);
    col->getSortKey(strtest2, sortkey2, 64);
    UnicodeString strtest3(test3);
    col->getSortKey(strtest3, sortkey3, 64);

    tempkey = key1.getByteArray(keylength);
    doAssert(memcmp(tempkey, sortkey1, keylength) == 0, 
        "Test1 string should have the same collation key and sort key");
    tempkey = key2.getByteArray(keylength);
    doAssert(memcmp(tempkey, sortkey2, keylength) == 0, 
        "Test2 string should have the same collation key and sort key");
    tempkey = key3.getByteArray(keylength);
    doAssert(memcmp(tempkey, sortkey3, keylength) == 0, 
        "Test3 string should have the same collation key and sort key");

    logln("Use secondary comparision level testing ...\n");
    col->setStrength(Collator::SECONDARY);
    
    col->getCollationKey(test1, u_strlen(test1), key1, status);
    col->getCollationKey(test2, u_strlen(test2), key2, status);
    col->getCollationKey(test3, u_strlen(test3), key3, status);
    
    doAssert(key1.compareTo(key2) == Collator::EQUAL, 
        "Result should be \"Abcda\" == \"abcda\"");
    doAssert(key2.compareTo(key3) == Collator::EQUAL, 
        "Result should be \"abcda\" ==  \"abcda\"");

    tempkey = key2.getByteArray(keylength);
    doAssert(memcmp(tempkey, key2compat, keylength - 1) == 0, 
             "Binary format for 'abcda' sortkey different!");

    col->getSortKey(test1, sortkey1, 64);
    col->getSortKey(test2, sortkey2, 64);
    col->getSortKey(test3, sortkey3, 64);

    tempkey = key1.getByteArray(keylength);
    doAssert(memcmp(tempkey, sortkey1, keylength) == 0, 
        "Test1 string should have the same collation key and sort key");
    tempkey = key2.getByteArray(keylength);
    doAssert(memcmp(tempkey, sortkey2, keylength) == 0, 
        "Test2 string should have the same collation key and sort key");
    tempkey = key3.getByteArray(keylength);
    doAssert(memcmp(tempkey, sortkey3, keylength) == 0, 
        "Test3 string should have the same collation key and sort key");

    col->getSortKey(test1, 5, sortkey1, 64);
    col->getSortKey(test2, 5, sortkey2, 64);
    col->getSortKey(test3, 5, sortkey3, 64);

    tempkey = key1.getByteArray(keylength);
    doAssert(memcmp(tempkey, sortkey1, keylength) == 0, 
        "Test1 string should have the same collation key and sort key");
    tempkey = key2.getByteArray(keylength);
    doAssert(memcmp(tempkey, sortkey2, keylength) == 0, 
        "Test2 string should have the same collation key and sort key");
    tempkey = key3.getByteArray(keylength);
    doAssert(memcmp(tempkey, sortkey3, keylength) == 0, 
        "Test3 string should have the same collation key and sort key");

    col->getSortKey(strtest1, sortkey1, 64);
    col->getSortKey(strtest2, sortkey2, 64);
    col->getSortKey(strtest3, sortkey3, 64);

    tempkey = key1.getByteArray(keylength);
    doAssert(memcmp(tempkey, sortkey1, keylength) == 0, 
        "Test1 string should have the same collation key and sort key");
    tempkey = key2.getByteArray(keylength);
    doAssert(memcmp(tempkey, sortkey2, keylength) == 0, 
        "Test2 string should have the same collation key and sort key");
    tempkey = key3.getByteArray(keylength);
    doAssert(memcmp(tempkey, sortkey3, keylength) == 0, 
        "Test3 string should have the same collation key and sort key");

    logln("testing sortkey ends...");
    delete col;
}

void CollationAPITest::TestMaxExpansion()
{
    UErrorCode          status = U_ZERO_ERROR;
    UChar               ch     = 0;
    UChar               supplementary[2] = {0xD800, 0xDC00};
    uint32_t            sorder = 0;
    uint32_t            temporder = 0;

    UnicodeString rule("&a < ab < c/aba < d < z < ch");
    RuleBasedCollator coll(rule, status);
    UnicodeString str(ch);
    CollationElementIterator *iter = 
                                  coll.createCollationElementIterator(str);

    while (ch < 0xFFFF && U_SUCCESS(status)) {
        int      count = 1;
        uint32_t order;
        int32_t  size = 0;

        ch ++;

        str.setCharAt(0, ch);
        iter->setText(str, status);
        order = iter->previous(status);

        /* thai management */
        if (order == 0)
            order = iter->previous(status);

        while (U_SUCCESS(status) &&
            ((uint32_t)iter->previous(status) != UCOL_NULLORDER)) {
            count ++;
        }

        size = coll.getMaxExpansion(order);
        if (U_FAILURE(status) || size < count) {
            errln("Failure at codepoint %d, maximum expansion count < %d\n",
                  ch, count);
        }
    }

    /* testing for exact max expansion */
    ch = 0;
    while (ch < 0x61) {
        uint32_t order;
        int32_t  size;
        str.setCharAt(0, ch);
        iter->setText(str, status);
        order = iter->previous(status);
        size  = coll.getMaxExpansion(order);
        if (U_FAILURE(status) || size != 1) {
            errln("Failure at codepoint %d, maximum expansion count < %d\n",
                ch, 1);
        }
        ch ++;
    }

    ch = 0x63;
    str.setTo(ch); 
    iter->setText(str, status);
    temporder = iter->previous(status);

    if (U_FAILURE(status) || coll.getMaxExpansion(temporder) != 3) {
        errln("Failure at codepoint %d, maximum expansion count != %d\n",
              ch, 3);
    }

    ch = 0x64;
    str.setTo(ch); 
    iter->setText(str, status);
    temporder = iter->previous(status);

    if (U_FAILURE(status) || coll.getMaxExpansion(temporder) != 1) {
        errln("Failure at codepoint %d, maximum expansion count != %d\n",
                ch, 3);
    }

    str.setTo(supplementary, 2);
    iter->setText(str, status);
    sorder = iter->previous(status);

    if (U_FAILURE(status) || coll.getMaxExpansion(sorder) != 2) {
        errln("Failure at supplementary codepoints, maximum expansion count < %d\n",
              2);
    }

    /* testing jamo */
    ch = 0x1165;
    str.setTo(ch);
    iter->setText(str, status);
    temporder = iter->previous(status);
    if (U_FAILURE(status) || coll.getMaxExpansion(temporder) > 3) {
        errln("Failure at codepoint %d, maximum expansion count > %d\n",
              ch, 3);
    }

    delete iter;

    /* testing special jamo &a<\u1160 */
    rule = CharsToUnicodeString("\\u0026\\u0071\\u003c\\u1165\\u002f\\u0071\\u0071\\u0071\\u0071");
    
    RuleBasedCollator jamocoll(rule, status);
    iter = jamocoll.createCollationElementIterator(str);
    temporder = iter->previous(status);
    if (U_FAILURE(status) || iter->getMaxExpansion(temporder) != 6) {
        errln("Failure at codepoint %d, maximum expansion count > %d\n",
              ch, 5);
    }

    delete iter;
}

void CollationAPITest::TestDisplayName()
{
    UErrorCode error = U_ZERO_ERROR;
    Collator *coll = Collator::createInstance("en_US", error);
    if (U_FAILURE(error)) {
        errln("Failure creating english collator");
        return;
    }
    UnicodeString name;
    UnicodeString result;
    coll->getDisplayName(Locale::CANADA_FRENCH, result);
    Locale::CANADA_FRENCH.getDisplayName(name);
    if (result.compare(name)) {
        errln("Failure getting the correct name for locale en_US");
    }

    coll->getDisplayName(Locale::SIMPLIFIED_CHINESE, result);
    Locale::SIMPLIFIED_CHINESE.getDisplayName(name);
    if (result.compare(name)) {
        errln("Failure getting the correct name for locale zh_SG");
    }
    delete coll;
}

void CollationAPITest::TestAttribute()
{
    UErrorCode error = U_ZERO_ERROR;
    Collator *coll = Collator::createInstance(error);
    
    if (U_FAILURE(error)) {
        errln("Creation of default collator failed");
        return;
    }
    
    coll->setAttribute(UCOL_FRENCH_COLLATION, UCOL_OFF, error);
    if (coll->getAttribute(UCOL_FRENCH_COLLATION, error) != UCOL_OFF ||
        U_FAILURE(error)) {
        errln("Setting and retrieving of the french collation failed");
    }

    coll->setAttribute(UCOL_FRENCH_COLLATION, UCOL_ON, error);
    if (coll->getAttribute(UCOL_FRENCH_COLLATION, error) != UCOL_ON ||
        U_FAILURE(error)) {
        errln("Setting and retrieving of the french collation failed");
    }

    coll->setAttribute(UCOL_ALTERNATE_HANDLING, UCOL_SHIFTED, error);
    if (coll->getAttribute(UCOL_ALTERNATE_HANDLING, error) != UCOL_SHIFTED ||
        U_FAILURE(error)) {
        errln("Setting and retrieving of the alternate handling failed");
    }

    coll->setAttribute(UCOL_ALTERNATE_HANDLING, UCOL_NON_IGNORABLE, error);
    if (coll->getAttribute(UCOL_ALTERNATE_HANDLING, error) != UCOL_NON_IGNORABLE ||
        U_FAILURE(error)) {
        errln("Setting and retrieving of the alternate handling failed");
    }

    coll->setAttribute(UCOL_CASE_FIRST, UCOL_LOWER_FIRST, error);
    if (coll->getAttribute(UCOL_CASE_FIRST, error) != UCOL_LOWER_FIRST ||
        U_FAILURE(error)) {
        errln("Setting and retrieving of the case first attribute failed");
    }

    coll->setAttribute(UCOL_CASE_FIRST, UCOL_UPPER_FIRST, error);
    if (coll->getAttribute(UCOL_CASE_FIRST, error) != UCOL_UPPER_FIRST ||
        U_FAILURE(error)) {
        errln("Setting and retrieving of the case first attribute failed");
    }

    coll->setAttribute(UCOL_CASE_LEVEL, UCOL_ON, error);
    if (coll->getAttribute(UCOL_CASE_LEVEL, error) != UCOL_ON ||
        U_FAILURE(error)) {
        errln("Setting and retrieving of the case level attribute failed");
    }

    coll->setAttribute(UCOL_CASE_LEVEL, UCOL_OFF, error);
    if (coll->getAttribute(UCOL_CASE_LEVEL, error) != UCOL_OFF ||
        U_FAILURE(error)) {
        errln("Setting and retrieving of the case level attribute failed");
    }

    coll->setAttribute(UCOL_NORMALIZATION_MODE, UCOL_ON, error);
    if (coll->getAttribute(UCOL_NORMALIZATION_MODE, error) != UCOL_ON ||
        U_FAILURE(error)) {
        errln("Setting and retrieving of the normalization on/off attribute failed");
    }

    coll->setAttribute(UCOL_NORMALIZATION_MODE, UCOL_OFF, error);
    if (coll->getAttribute(UCOL_NORMALIZATION_MODE, error) != UCOL_OFF ||
        U_FAILURE(error)) {
        errln("Setting and retrieving of the normalization on/off attribute failed");
    }
    
    coll->setAttribute(UCOL_STRENGTH, UCOL_PRIMARY, error);
    if (coll->getAttribute(UCOL_STRENGTH, error) != UCOL_PRIMARY ||
        U_FAILURE(error)) {
        errln("Setting and retrieving of the collation strength failed");
    }

    coll->setAttribute(UCOL_STRENGTH, UCOL_SECONDARY, error);
    if (coll->getAttribute(UCOL_STRENGTH, error) != UCOL_SECONDARY ||
        U_FAILURE(error)) {
        errln("Setting and retrieving of the collation strength failed");
    }

    coll->setAttribute(UCOL_STRENGTH, UCOL_TERTIARY, error);
    if (coll->getAttribute(UCOL_STRENGTH, error) != UCOL_TERTIARY ||
        U_FAILURE(error)) {
        errln("Setting and retrieving of the collation strength failed");
    }

    coll->setAttribute(UCOL_STRENGTH, UCOL_QUATERNARY, error);
    if (coll->getAttribute(UCOL_STRENGTH, error) != UCOL_QUATERNARY ||
        U_FAILURE(error)) {
        errln("Setting and retrieving of the collation strength failed");
    }

    coll->setAttribute(UCOL_STRENGTH, UCOL_IDENTICAL, error);
    if (coll->getAttribute(UCOL_STRENGTH, error) != UCOL_IDENTICAL ||
        U_FAILURE(error)) {
        errln("Setting and retrieving of the collation strength failed");
    }

    delete coll;
}

void CollationAPITest::TestVariableTopSetting() {
  UErrorCode status = U_ZERO_ERROR;

  UChar vt[256] = { 0 };

  Collator *coll = Collator::createInstance(status);

  uint32_t oldVarTop = coll->getVariableTop(status);

  vt[0] = 0x0041;

  uint32_t newVarTop = coll->setVariableTop(vt, 1, status);

  if((newVarTop & 0xFFFF0000) != (coll->getVariableTop(status) & 0xFFFF0000)) {
    errln("Didn't set vartop properly\n");
  }

  coll->setVariableTop(oldVarTop, status);

  uint32_t newerVarTop = coll->setVariableTop(UnicodeString(vt, 1), status);

  if((newVarTop & 0xFFFF0000) != (newerVarTop & 0xFFFF0000)) {
    errln("Didn't set vartop properly from UnicodeString!\n");
  }

  delete coll;

}

void CollationAPITest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par */)
{
    if (exec) logln("TestSuite CollationAPITest: ");
    switch (index) {
        case 0: name = "TestProperty";  if (exec)   TestProperty(/* par */); break;
        case 1: name = "TestOperators"; if (exec)   TestOperators(/* par */); break;
        case 2: name = "TestDuplicate"; if (exec)   TestDuplicate(/* par */); break;
        case 3: name = "TestCompare";   if (exec)   TestCompare(/* par */); break;
        case 4: name = "TestHashCode";  if (exec)   TestHashCode(/* par */); break;
        case 5: name = "TestCollationKey";  if (exec)   TestCollationKey(/* par */); break;
        case 6: name = "TestElemIter";  if (exec)   TestElemIter(/* par */); break;
        case 7: name = "TestGetAll";    if (exec)   TestGetAll(/* par */); break;
        case 8: name = "TestRuleBasedColl"; if (exec)   TestRuleBasedColl(/* par */); break;
        case 9: name = "TestDecomposition"; if (exec)   TestDecomposition(/* par */); break;
        case 10: name = "TestSafeClone"; if (exec)   TestSafeClone(/* par */); break;
        case 11: name = "TestSortKey";   if (exec)   TestSortKey(); break;
        case 12: name = "TestMaxExpansion";   if (exec)   TestMaxExpansion(); break;
        case 13: name = "TestDisplayName";   if (exec)   TestDisplayName(); break;
        case 14: name = "TestAttribute";   if (exec)   TestAttribute(); break;
        case 15: name = "TestVariableTopSetting"; if (exec) TestVariableTopSetting(); break;
        default: name = ""; break;
    }
}

