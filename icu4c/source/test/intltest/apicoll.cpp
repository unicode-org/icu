/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
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

#ifndef COLL_H
#include "unicode/coll.h"
#endif

#ifndef TBLCOLL_H
#include "unicode/tblcoll.h"
#endif

#ifndef COLEITR_H
#include "unicode/coleitr.h"
#endif

#ifndef SORTKEY_H
#include "unicode/sortkey.h"
#endif

#ifndef _APICOLL
#include "apicoll.h"
#endif

#include "unicode/chariter.h"
#include "unicode/schriter.h"

void
CollationAPITest::doAssert(UBool condition, const char *message)
{
    if (!condition) {
        errln(UnicodeString("ERROR : ") + message);
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
    col = Collator::createInstance(success);

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
    doAssert((col->compare("black-bird", "blackbird") == Collator::GREATER), "black-bird > blackbird comparison failed");
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

    logln("Get display name for the US English in default locale language : ");
    logln(Collator::getDisplayName(Locale::US, name)); 
    doAssert((name == UnicodeString("English (United States)")), "getDisplayName failed");

    logln("Default collation property test ended.");
    logln("Collator::getRules() testing ...");  
    doAssert(((RuleBasedCollator*)col)->getRules().length() != 0, "getRules() result incorrect" );                
    logln("getRules tests end.");

    delete col; col = 0;
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

    doAssert((*col == *junk), "The default collation should be returned.");
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
}

void 
CollationAPITest::TestHashCode(/* char* par */)
{
    logln("hashCode tests begin.");
    UErrorCode success = U_ZERO_ERROR;
    Collator *col1 = 0;
    col1 = Collator::createInstance(success);
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
    col3 = Collator::createInstance(success);
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
    col = Collator::createInstance(success);
    if (U_FAILURE(success))
    {
        errln("Default collation creation failed.");
        return;
    }

    CollationKey sortk1, sortk2;
    UnicodeString test1("Abcda"), test2("abcda");
    UErrorCode key1Status = U_ZERO_ERROR, key2Status = U_ZERO_ERROR;
                
    logln("Use tertiary comparison level testing ....");

    doAssert((col->getCollationKey(test1, sortk1, key1Status).compareTo(col->getCollationKey(test2, sortk2, key2Status))) 
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

    const uint8_t* byteArray3 = 0;
    byteArray3 = sortk1.getByteArray(cnt3);

    const uint8_t* byteArray4 = 0;
    byteArray4 = sortk2.getByteArray(cnt4);

    CollationKey sortk4(byteArray1, cnt1), sortk5(byteArray2, cnt2);
    CollationKey sortk6(byteArray3, cnt3), sortk7(byteArray4, cnt4);

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
    col = Collator::createInstance(success);
    if (U_FAILURE(success))
    {
        errln("Default collation creation failed.");
        return;
    }
       
    UnicodeString testString1("XFILE What subset of all possible test cases has the highest probability of detecting the most errors?");
    UnicodeString testString2("Xf ile What subset of all possible test cases has the lowest probability of detecting the least errors?");
    logln("Constructors and comparison testing....");
    CollationElementIterator *iterator1 = ((RuleBasedCollator*)col)->createCollationElementIterator(testString1);
    
    CharacterIterator *chariter=new StringCharacterIterator(testString1);
    CollationElementIterator *coliter=((RuleBasedCollator*)col)->createCollationElementIterator(*chariter);
    
    // copy ctor
    CollationElementIterator *iterator2 = new CollationElementIterator(*iterator1);
    CollationElementIterator *iterator3 = ((RuleBasedCollator*)col)->createCollationElementIterator(testString2);
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

    order1 = iterator1->next(success); order3 = iterator3->next(success);
    doAssert((CollationElementIterator::secondaryOrder(order1) != 
        CollationElementIterator::secondaryOrder(order3)), "The secondary orders should be different");
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
        CollationElementIterator::secondaryOrder(order3)), "The secondary orders should be different");
    doAssert((order1 != CollationElementIterator::NULLORDER), "Unexpected end of iterator reached");
    doAssert((*iterator2 != *iterator3), "The iterators should be different");

    
    //test error values
    success=U_UNSUPPORTED_ERROR;
    Collator *colerror=NULL;
    colerror=Collator::createInstance(success);
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
    UnicodeString ruleset1("< a, A < b, B < c, C < d, D, e, E");
    RuleBasedCollator *colerror1 = new RuleBasedCollator(ruleset1, success);
    if (U_SUCCESS(success)) {
        errln("RuleBasedCollator is expected to failed.");
    }
    colerror1 = new RuleBasedCollator(ruleset1, Collator::PRIMARY, success);
    if (U_SUCCESS(success)) {
        errln("RuleBasedCollator is expected to failed.");
    }
    colerror1 = new RuleBasedCollator(ruleset1, Normalizer::NO_OP, success);
    if (U_SUCCESS(success)) {
        errln("RuleBasedCollator is expected to failed.");
    }
    colerror1 = new RuleBasedCollator(ruleset1, Collator::SECONDARY, Normalizer::NO_OP, success);
    if (U_SUCCESS(success)) {
        errln("RuleBasedCollator is expected to failed.");
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
    Collator *col3 = Collator::createInstance(success);
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
    Collator *col1 = Collator::createInstance(status);
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
    col = Collator::createInstance(success);
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
        default: name = ""; break;
    }
}

