/*
**********************************************************************
*   Copyright (C) 1999 Alan Liu and others. All rights reserved.
**********************************************************************
*   Date        Name        Description
*   10/20/99    alan        Creation.
*   03/22/2000  Madhu       Added additional tests
**********************************************************************
*/

#include "unicode/utypes.h"
#include "usettest.h"
#include "unicode/uniset.h"

#define CASE(id,test) case id:                          \
                          name = #test;                 \
                          if (exec) {                   \
                              logln(#test "---");       \
                              logln((UnicodeString)""); \
                              test();                   \
                          }                             \
                          break;

void
UnicodeSetTest::runIndexedTest(int32_t index, bool_t exec,
                               char* &name, char* par) {
    // if (exec) logln((UnicodeString)"TestSuite UnicodeSetTest");
    switch (index) {
        CASE(0,TestPatterns)
        CASE(1,TestAddRemove)
        CASE(2,TestCategories)
		CASE(3,TestCloneEqualHash)
		default: name = ""; break;
    }
}

void
UnicodeSetTest::TestPatterns(void) {
    UnicodeSet set;
    expectPattern(set, "[[a-m]&[d-z]&[k-y]]",  "km");
    expectPattern(set, "[[a-z]-[m-y]-[d-r]]",  "aczz");
    expectPattern(set, "[a\\-z]",  "--aazz");
    expectPattern(set, "[-az]",  "--aazz");
    expectPattern(set, "[az-]",  "--aazz");
    expectPattern(set, "[[[a-z]-[aeiou]i]]", "bdfnptvz");

    // Throw in a test of complement
    set.complement();
    UnicodeString exp;
    exp.append((UChar)0x0000).append("aeeoouu").append((UChar)('z'+1)).append((UChar)0xFFFF);
    expectPairs(set, exp);
}

void
UnicodeSetTest::TestCategories(void) {
    UErrorCode status = U_ZERO_ERROR;
    const char* pat = " [:Lu:] "; // Whitespace ok outside [:..:]
    UnicodeSet set(pat, status);
    if (U_FAILURE(status)) {
        errln((UnicodeString)"Fail: Can't construct set with " + pat);
    } else {
        expectContainment(set, pat, "ABC", "abc");
    }
}
void
UnicodeSetTest::TestCloneEqualHash(void) {
	UErrorCode status = U_ZERO_ERROR;
	int8_t category=Unicode::LOWERCASE_LETTER;
	UnicodeSet *set1=new UnicodeSet(category, status); //  :Li: Letter, lowercase
	if (U_FAILURE(status)){
		errln((UnicodeString)"FAIL: Can't construst set with cateegory->Ll");
		return;
	}
	category=Unicode::DECIMAL_DIGIT_NUMBER;
	UnicodeSet *set2=new UnicodeSet(category, status);   //Number, Decimal digit
	if (U_FAILURE(status)){
		errln((UnicodeString)"FAIL: Can't construct set with cateegory->Nd");
		return;
	}
	
	logln("Testing copy construction");
	UnicodeSet *set1copy=new UnicodeSet(*set1);
	if(*set1 != *set1copy || *set1 == *set2 || 
		set1->getPairs() != set1copy->getPairs() ||
		set1->hashCode() != set1copy->hashCode()){
		errln("FAIL : Error in copy construction");
		return;
	}

	logln("Testing =operator");
	UnicodeSet set1equal=*set1;
	UnicodeSet set2equal=*set2;
	if(set1equal != *set1 || set1equal != *set1copy || set2equal != *set2 || 
		set2equal == *set1 || set2equal == *set1copy || set2equal == set1equal){
		errln("FAIL: Error in =operator");
	}
	
	logln("Testing clone()");
	UnicodeSet *set1clone=(UnicodeSet*)set1->clone();
	UnicodeSet *set2clone=(UnicodeSet*)set2->clone();
	if(*set1clone != *set1 || *set1clone != *set1copy || *set1clone != set1equal || 
		*set2clone != *set2 || *set2clone == *set1copy || *set2clone != set2equal || 
		*set2clone == *set1 || *set2clone == set1equal || *set2clone == *set1clone){
		errln("FAIL: Error in clone");
	}
	
	logln("Testing hashcode");
    if(set1->hashCode() != set1equal.hashCode() || set1->hashCode() != set1clone->hashCode() ||
		set2->hashCode() != set2equal.hashCode() || set2->hashCode() != set2clone->hashCode() ||
		set1copy->hashCode() != set1equal.hashCode() || set1copy->hashCode() != set1clone->hashCode() ||
		set1->hashCode() == set2->hashCode()  || set1copy->hashCode() == set2->hashCode() ||
		set2->hashCode() == set1clone->hashCode() || set2->hashCode() == set1equal.hashCode() ){
		errln("FAIL: Error in hashCode()");
	}

	delete set1;
	delete set1copy;
	delete set2;
    


}
void
UnicodeSetTest::TestAddRemove(void) {
	UErrorCode status = U_ZERO_ERROR;
    UnicodeSet set; // Construct empty set
	doAssert(set.isEmpty() == TRUE, "set should be empty");
	doAssert(set.size() == 0, "size should be 0");
    set.add('a', 'z');
    expectPairs(set, "az");
	doAssert(set.isEmpty() == FALSE, "set should not be empty");
    doAssert(set.size() != 0, "size should not be equal to 0");
	doAssert(set.size() == 26, "size should be equal to 26");
    set.remove('m', 'p');
	expectPairs(set, "alqz");
	doAssert(set.size() == 22, "size should be equal to 22");
    set.remove('e', 'g');
    expectPairs(set, "adhlqz");
	doAssert(set.size() == 19, "size should be equal to 19");
    set.remove('d', 'i');
    expectPairs(set, "acjlqz");
	doAssert(set.size() == 16, "size should be equal to 16");
    set.remove('c', 'r');
    expectPairs(set, "absz");
	doAssert(set.size() == 10, "size should be equal to 10");
    set.add('f', 'q');
    expectPairs(set, "abfqsz");
	doAssert(set.size() == 22, "size should be equal to 22");
    set.remove('a', 'g');
    expectPairs(set, "hqsz");
    set.remove('a', 'z');
    expectPairs(set, "");
	doAssert(set.isEmpty() == TRUE, "set should be empty");
	doAssert(set.size() == 0, "size should be 0");
	set.add('a');
	doAssert(set.isEmpty() == FALSE, "set should not be empty");
    doAssert(set.size() == 1, "size should not be equal to 1");
	set.add('b');
	set.add('c');
	expectPairs(set, "ac");
	doAssert(set.size() == 3, "size should not be equal to 3");
	set.add('p');
	set.add('q');
	expectPairs(set, "acpq");
	doAssert(set.size() == 5, "size should not be equal to 5");
	set.clear();
    expectPairs(set, "");
	doAssert(set.isEmpty() == TRUE, "set should be empty");
	doAssert(set.size() == 0, "size should be 0");

    // Try removing an entire set from another set
    expectPattern(set, "[c-x]", "cx");
    UnicodeSet set2;
    expectPattern(set2, "[f-ky-za-bc[vw]]", "acfkvwyz");
    set.removeAll(set2);
    expectPairs(set, "deluxx");

    // Try adding an entire set to another set
    expectPattern(set, "[jackiemclean]", "aacceein");
    expectPattern(set2, "[hitoshinamekatajamesanderson]", "aadehkmort");
    set.addAll(set2);
    expectPairs(set, "aacehort");
    doAssert(set.containsAll(set2) == TRUE, "set should contain all the elements in set2");

	// Try retaining an set of elements contained in another set (intersection)
	UnicodeSet set3;
	expectPattern(set3, "[a-c]", "ac");
	doAssert(set.containsAll(set3) == FALSE, "set doesn't contain all the elements in set3");
	set3.remove('b');
	expectPairs(set3, "aacc");
	doAssert(set.containsAll(set3) == TRUE, "set should contain all the elements in set3");
	set.retainAll(set3);
	expectPairs(set, "aacc");
	doAssert(set.size() == set3.size(), "set.size() should be set3.size()");
	doAssert(set.containsAll(set3) == TRUE, "set should contain all the elements in set3");
	set.clear();
	doAssert(set.size() != set3.size(), "set.size() != set3.size()");

    // Test commutativity
    expectPattern(set, "[hitoshinamekatajamesanderson]", "aadehkmort");
    expectPattern(set2, "[jackiemclean]", "aacceein");
    set.addAll(set2);
    expectPairs(set, "aacehort");
	doAssert(set.containsAll(set2) == TRUE, "set should contain all the elements in set2");




}
void
UnicodeSetTest::doAssert(bool_t condition, const char *message)
{
    if (!condition) {
        errln(UnicodeString("ERROR : ") + message);
    }
}
void
UnicodeSetTest::expectContainment(const UnicodeSet& set,
                                  const UnicodeString& setName,
                                  const UnicodeString& charsIn,
                                  const UnicodeString& charsOut) {
    UnicodeString bad;
    int32_t i;
    for (i=0; i<charsIn.length(); ++i) {
        UChar c = charsIn.charAt(i);
        if (!set.contains(c)) {
            bad.append(c);
        }
    }
    if (bad.length() > 0) {
        logln((UnicodeString)"Fail: set " + setName + " does not contain " + bad +
              ", expected containment of " + charsIn);
    } else {
        logln((UnicodeString)"Ok: set " + setName + " contains " + charsIn);
    }

    bad.truncate(0);
    for (i=0; i<charsOut.length(); ++i) {
        UChar c = charsOut.charAt(i);
        if (set.contains(c)) {
            bad.append(c);
        }
    }
    if (bad.length() > 0) {
        logln((UnicodeString)"Fail: set " + setName + " contains " + bad +
              ", expected non-containment of " + charsOut);
    } else {
        logln((UnicodeString)"Ok: set " + setName + " does not contain " + charsOut);
    }
}

void
UnicodeSetTest::expectPattern(UnicodeSet& set,
                              const UnicodeString& pattern,
                              const UnicodeString& expectedPairs){
    UErrorCode status = U_ZERO_ERROR;
	set.applyPattern(pattern, status);
	if (U_FAILURE(status)) {
		errln(UnicodeString("FAIL: applyPattern(\"") + pattern +
              "\") failed");
		return;
	} else {
        if (set.getPairs() != expectedPairs ) {
            errln(UnicodeString("FAIL: applyPattern(\"") + pattern +
                  "\") => pairs \"" +
                  escape(set.getPairs()) + "\", expected \"" +
                  escape(expectedPairs) + "\"");
        } else {
            logln(UnicodeString("Ok:   applyPattern(\"") + pattern +
                  "\") => pairs \"" +
                  escape(set.getPairs()) + "\"");
        }
    }
	// the result of calling set.toPattern(), which is the string representation of
	// this set(set), is passed to a  UnicodeSet constructor, and tested that it 
	// will produce another set that is equal to this one.
	UnicodeString temppattern;
	set.toPattern(temppattern);
	UnicodeSet *tempset=new UnicodeSet(temppattern, status);
	if (U_FAILURE(status)) {
		errln(UnicodeString("FAIL: Construction with the pattern derived from toPattern() failed"));
        return;
	}
	if(*tempset != set || tempset->getPairs() != set.getPairs()){
		errln(UnicodeString("FAIL: "+ pattern + "!=>" + temppattern + ". Pairs \""+ escape(tempset->getPairs()) + "\" expected->\"" +
			escape(set.getPairs()) + "\""));
	} else{
		logln(UnicodeString("OK:  "+ pattern + "==>" + temppattern + ".  Pairs \"" + escape(tempset->getPairs()) + "\""));
	}
	
	delete tempset;
    	
}

void
UnicodeSetTest::expectPairs(const UnicodeSet& set, const UnicodeString& expectedPairs) {
    if (set.getPairs() != expectedPairs) {
        errln(UnicodeString("FAIL: Expected pair list \"") +
              escape(expectedPairs) + "\", got \"" +
              escape(set.getPairs()) + "\"");
    }
}

static UChar toHexString(int32_t i) { return i + (i < 10 ? 0x30 : (0x41 - 10)); }

UnicodeString
UnicodeSetTest::escape(const UnicodeString& s) {
    UnicodeString buf;
    for (int32_t i=0; i<s.length(); ++i)
    {
        UChar c = s[(UTextOffset)i];
        if (' ' <= c && c <= (UChar)0x7F) {
            buf += c;
        } else {
            buf += (UChar)0x5c; buf += (UChar)0x55;
            buf += toHexString((c & 0xF000) >> 12);
            buf += toHexString((c & 0x0F00) >> 8);
            buf += toHexString((c & 0x00F0) >> 4);
            buf += toHexString(c & 0x000F);
        }
    }
    return buf;
}
