/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/23/00    aliu        Creation.
**********************************************************************
*/
#include "unicode/utypes.h"
#include "unicode/translit.h"
#include "unicode/rbt.h"
#include "unicode/uniset.h"
#include "unicode/unicode.h"
#include "unicode/normlzr.h"
#include "unicode/uchar.h"
#include "unicode/parseerr.h"
#include "transrt.h"
#include "testutil.h"
#include <string.h>

#define CASE(id,test) case id:                          \
                          name = #test;                 \
                          if (exec) {                   \
                              logln(#test "---");       \
                              logln((UnicodeString)""); \
                              test();                   \
                          }                             \
                          break

#define EXHAUSTIVE(id,test) case id:                            \
                              if(quick==FALSE){                 \
                                  name = #test;                 \
                                  if (exec){                    \
                                      logln(#test "---");       \
                                      logln((UnicodeString)""); \
                                      test();                   \
                                  }                             \
                              }else{                            \
                                name="";                        \
                              }                                 \
                              break
void
TransliteratorRoundTripTest::runIndexedTest(int32_t index, UBool exec,
                                   const char* &name, char* /*par*/) {
    switch (index) {
        CASE(0, TestCyrillic);
        // CASE(0,TestKana);
        CASE(1,TestHiragana);
        CASE(2,TestKatakana);
        CASE(3,TestJamo);
        CASE(4,TestHangul);
        CASE(5,TestGreek);
        CASE(6,TestGreekUNGEGN);
        CASE(7,Testel);
        CASE(8,TestCyrillic);
        CASE(9,TestDevanagariLatin);
        CASE(10,TestInterIndic);
        default: name = ""; break;
    }
}

//--------------------------------------------------------------------
// Legal
//--------------------------------------------------------------------

class Legal {
public:
    Legal() {}
    virtual ~Legal() {}
    virtual UBool is(const UnicodeString& /*sourceString*/) const {return TRUE;}
};

class LegalJamo : public Legal {
    // any initial must be followed by a medial (or initial)
    // any medial must follow an initial (or medial)
    // any final must follow a medial (or final)
public:
    LegalJamo() {}
    virtual ~LegalJamo() {}
    virtual UBool is(const UnicodeString& sourceString) const;
            int   getType(UChar c) const;
};

UBool LegalJamo::is(const UnicodeString& sourceString) const {
    int t;
    UnicodeString decomp;
    UErrorCode ec = U_ZERO_ERROR;
    Normalizer::decompose(sourceString, FALSE, 0, decomp, ec); 
    if (U_FAILURE(ec)) {
        return FALSE;
    }      
    for (int i = 0; i < decomp.length(); ++i) { // don't worry about surrogates             
        switch (getType(decomp.charAt(i))) {
        case 0: t = getType(decomp.charAt(i+1));
                if (t != 0 && t != 1) { return FALSE; }
                break;
        case 1: t = getType(decomp.charAt(i-1));
                if (t != 0 && t != 1) { return FALSE; }
                break;
        case 2: t = getType(decomp.charAt(i-1));
                if (t != 1 && t != 2) { return FALSE; }
                break;
        }
    }              
    return TRUE;
}

int LegalJamo::getType(UChar c) const {
    if (0x1100 <= c && c <= 0x1112) 
        return 0;
    else if (0x1161 <= c && c  <= 0x1175) 
             return 1;
         else if (0x11A8 <= c && c  <= 0x11C2) 
                  return 2;
    return -1; // other
}

class LegalGreek : public Legal {
    UBool full;
public:
    LegalGreek(UBool _full) { full = _full; }
    virtual ~LegalGreek() {}

    virtual UBool is(const UnicodeString& sourceString) const;

    static UBool isVowel(UChar c);
    
    static UBool isRho(UChar c);
};

UBool LegalGreek::is(const UnicodeString& sourceString) const { 
    UnicodeString decomp;
    UErrorCode ec = U_ZERO_ERROR;
    Normalizer::decompose(sourceString, FALSE, 0, decomp, ec);
                
    // modern is simpler: don't care about anything but a grave
    if (full == FALSE) {
        if (sourceString == UnicodeString("\\u039C\\u03C0", "")) {
            return FALSE;
        }       
        for (int32_t i = 0; i < decomp.length(); ++i) {
            UChar c = decomp.charAt(i);
            // exclude all the accents
            if (c == 0x0313 || c == 0x0314 || c == 0x0300 || c == 0x0302
                || c == 0x0342 || c == 0x0345
                ) return FALSE;
        }
        return TRUE;
    }

    // Legal greek has breathing marks IFF there is a vowel or RHO at the start
    // IF it has them, it has exactly one.
    // IF it starts with a RHO, then the breathing mark must come before the second letter.
    // Since there are no surrogates in greek, don't worry about them
    UBool firstIsVowel = FALSE;
    UBool firstIsRho = FALSE;
    UBool noLetterYet = TRUE;
    int32_t breathingCount = 0;
    int32_t letterCount = 0;
    for (int32_t i = 0; i < decomp.length(); ++i) {
        UChar c = decomp.charAt(i);
        if (u_isalpha(c)) {
            ++letterCount;
            if (noLetterYet) {
                noLetterYet =  FALSE;
                firstIsVowel = isVowel(c);
                firstIsRho = isRho(c);
            }
            if (firstIsRho && letterCount == 2 && breathingCount == 0) {
                return FALSE;
            }
        }
        if (c == 0x0313 || c == 0x0314) {
            ++breathingCount;
        }
    }
    
    if (firstIsVowel || firstIsRho) return breathingCount == 1;
    return breathingCount == 0;
}

UBool LegalGreek::isVowel(UChar c) {
    switch (c) {
    case 0x03B1:
    case 0x03B5:
    case 0x03B7:
    case 0x03B9:
    case 0x03BF:
    case 0x03C5:
    case 0x03C9:
    case 0x0391:
    case 0x0395:
    case 0x0397:
    case 0x0399:
    case 0x039F:
    case 0x03A5:
    case 0x03A9:
        return TRUE;
    }
    return FALSE;
}

UBool LegalGreek::isRho(UChar c) {
    switch (c) {
    case 0x03C1:
    case 0x03A1:
        return TRUE;
    }
    return FALSE;
}

// UnicodeSetIterator Interface ---------------------------------------------

class UnicodeSetIterator {

public :
    UnicodeSet set;

    UnicodeSetIterator(UnicodeSet& set, UBool abb);
    UnicodeSetIterator(UnicodeSet& set);
    UnicodeSetIterator();
    ~UnicodeSetIterator();
    void setAbbreviated(UBool value);
    UBool getAbbreviated();
    int next();
    void reset(UnicodeSet& set, UBool abb);
    void reset(UnicodeSet& set);
    void reset();
    // tests whether a string is in a set.
    // should be in UnicodeSet
    static UBool containsSome(const UnicodeSet& set, const UnicodeString& s);
    // tests whether a string is in a set.
    // should be in UnicodeSet
    static UBool containsAll(const UnicodeSet& set, const UnicodeString& s);

private :
    int   endRange;
    int   range;
    int   startElement;
    int   endElement;
    int   element;
    UBool abbreviated;

    void resetInternal();
};

// UnicodeSetIterator Implementation ---------------------------------------

UnicodeSetIterator::UnicodeSetIterator(UnicodeSet& newSet, UBool abb) {
    reset(newSet, abb);
}
    
UnicodeSetIterator::UnicodeSetIterator(UnicodeSet& newSet) {
    reset(newSet);
}
        
UnicodeSetIterator::UnicodeSetIterator() {
    reset();
}

UnicodeSetIterator::~UnicodeSetIterator() {
}
        
void UnicodeSetIterator::setAbbreviated(UBool value) {
    abbreviated = value;
}
    
UBool UnicodeSetIterator::getAbbreviated() {
     return abbreviated;
}
        
/* returns -1 when done */
int UnicodeSetIterator::next() {
    if (abbreviated) {
        if (element >= startElement + 50 && element <= endElement - 50) {
            element = endElement - 50;
        }
    }
    if (element < endElement) {
        return ++element;
    }
    if (range >= endRange) {
        return -1;
    }
    ++range;
    endElement = set.getRangeEnd(range);
    startElement = set.getRangeStart(range);
    element = set.getRangeStart(range);
    return element;
}
        
void UnicodeSetIterator::reset(UnicodeSet& newSet, UBool abb) {
     abbreviated = abb;
     this->set = newSet;
     endRange = set.getRangeCount() - 1;
     resetInternal();
}
        
void UnicodeSetIterator::reset(UnicodeSet& newSet) {
    reset(newSet, FALSE);
}
        
void UnicodeSetIterator::reset() {
    abbreviated = FALSE;
    set.clear();
    endRange = set.getRangeCount() - 1;
    resetInternal();
}
        
void UnicodeSetIterator::resetInternal() {
    range = 0;
    endElement = 0;
    element = 0;            
    startElement = 0;
    if (endRange >= 0) {
        element = set.getRangeStart(range);
        endElement = set.getRangeEnd(range);
        startElement = set.getRangeStart(range);
    }
}
        
// tests whether a string is in a set.
// should be in UnicodeSet
UBool UnicodeSetIterator::containsSome(const UnicodeSet& set, 
                                       const UnicodeString& s) {
    int cp;
    for (int i = 0; i < s.length(); i += UTF_CHAR_LENGTH(i)) {
         cp = s.char32At(i);
         if (set.contains(cp)) {
             return TRUE;
         }
    }
    return FALSE;
}
        
// tests whether a string is in a set.
// should be in UnicodeSet
UBool UnicodeSetIterator::containsAll(const UnicodeSet& set, 
                                      const UnicodeString& s) {
    int cp;
    for (int i = 0; i < s.length(); i += UTF_CHAR_LENGTH(i)) {
        cp = s.char32At(i);
        if (set.contains(cp) == FALSE) {
            return FALSE;
        }
    }
    return TRUE;
}

//--------------------------------------------------------------------
// RTTest Interface
//--------------------------------------------------------------------

class RTTest : IntlTest {

    // PrintWriter out;

    UnicodeString transliteratorID; 
    int32_t errorLimit;
    int32_t errorCount;
    int32_t pairLimit;
    UnicodeSet sourceRange;
    UnicodeSet targetRange;
    UnicodeSet toSource;
    UnicodeSet toTarget;
    UnicodeSet roundtripExclusionsSet;
    IntlTest* log;
    Legal* legalSource; // NOT owned
    UnicodeSet badCharacters;

public:

    /*
     * create a test for the given script transliterator.
     */
    RTTest(const UnicodeString& transliteratorIDStr);

    virtual ~RTTest();

    void setErrorLimit(int32_t limit);

    void setPairLimit(int32_t limit);

    void test(const UnicodeString& sourceRange,
              const UnicodeString& targetRange,
              const char* roundtripExclusions,
              IntlTest* log,
              UBool     quick,
              Legal* adoptedLegal);

private:

    // Added to do better equality check.
        
    static UBool isSame(const UnicodeString& a, const UnicodeString& b);
             
    static UBool isCamel(const UnicodeString& a);

    UBool checkIrrelevants(Transliterator *t, const UnicodeString& irrelevants);

    void test2(UBool quick);

    void logWrongScript(const UnicodeString& label,
                        const UnicodeString& from,
                        const UnicodeString& to);
   
    void logNotCanonical(const UnicodeString& label, 
                         const UnicodeString& from, 
                         const UnicodeString& to, 
                         const UnicodeString& toCan);

    void logFails(const UnicodeString& label);

    void logToRulesFails(const UnicodeString& label, 
                         const UnicodeString& from, 
                         const UnicodeString& to, 
                         const UnicodeString& toCan);

    void logRoundTripFailure(const UnicodeString& from,
                             const UnicodeString& to,
                             const UnicodeString& back);
};

//--------------------------------------------------------------------
// RTTest Implementation
//--------------------------------------------------------------------

/*
 * create a test for the given script transliterator.
 */
RTTest::RTTest(const UnicodeString& transliteratorIDStr) {
    transliteratorID = transliteratorIDStr;
    errorLimit = 500;
    errorCount = 0;
    pairLimit  = 0x10000;
}

RTTest::~RTTest() {
}

void RTTest::setErrorLimit(int32_t limit) {
    errorLimit = limit;
}

void RTTest::setPairLimit(int32_t limit) {
    pairLimit = limit;
}

UBool RTTest::isSame(const UnicodeString& a, const UnicodeString& b) {
    if (a == b) return TRUE;
    if (a.caseCompare(b, U_FOLD_CASE_DEFAULT)==0 && isCamel(a)) return TRUE;
    UnicodeString aa, bb;
    UErrorCode ec = U_ZERO_ERROR;
    Normalizer::decompose(a, FALSE, 0, aa, ec);
    Normalizer::decompose(b, FALSE, 0, bb, ec);
    if (aa == bb) return TRUE;
    if (aa.caseCompare(bb, U_FOLD_CASE_DEFAULT)==0 && isCamel(aa)) return TRUE;
    return FALSE;
}

UBool RTTest::isCamel(const UnicodeString& a) {
    // see if string is of the form aB; e.g. lower, then upper or title
    UChar32 cp;
    UBool haveLower = FALSE;
    for (int32_t i = 0; i < a.length(); i += UTF_CHAR_LENGTH(cp)) {
        cp = a.char32At(i);
        int8_t t = u_charType(cp);
        switch (t) {
        case U_UPPERCASE_LETTER:
            if (haveLower) return TRUE;
            break;
        case U_TITLECASE_LETTER:
            if (haveLower) return TRUE;
            // drop through, since second letter is lower.
        case U_LOWERCASE_LETTER:
            haveLower = TRUE;
            break;
        }
    }
    return FALSE;
}

void RTTest::test(const UnicodeString& sourceRangeVal,
                  const UnicodeString& targetRangeVal,
                  const char* roundtripExclusions,
                  IntlTest* logVal, UBool quick, 
                  Legal* adoptedLegal) {

    UErrorCode status = U_ZERO_ERROR;

    this->log = logVal;
    this->legalSource = adoptedLegal;
    
    UnicodeSet neverOk("[:Other:]", status);
    UnicodeSet okAnyway("[^[:Letter:]]", status);

    if (U_FAILURE(status)) {
        log->errln("FAIL: Initializing UnicodeSet with [:Other:] or [^[:Letter:]]");
        return;
    }

    this->sourceRange.clear();
    this->sourceRange.applyPattern(sourceRangeVal, status);
    if (U_FAILURE(status)) {
        log->errln("FAIL: UnicodeSet::applyPattern(" +
                   sourceRangeVal + ")");
        return;
    }
    this->sourceRange.removeAll(neverOk);
    
    this->targetRange.clear();
    this->targetRange.applyPattern(targetRangeVal, status);
    if (U_FAILURE(status)) {
        log->errln("FAIL: UnicodeSet::applyPattern(" +
                   targetRangeVal + ")");
        return;
    }
    this->targetRange.removeAll(neverOk);
    
    this->toSource.clear();
    this->toSource.applyPattern(sourceRangeVal, status);
    if (U_FAILURE(status)) {
        log->errln("FAIL: UnicodeSet::applyPattern(" +
                   sourceRangeVal + ")");
        return;
    }
    this->toSource.addAll(okAnyway);

    this->toTarget.clear();
    this->toTarget.applyPattern(targetRangeVal, status);
    if (U_FAILURE(status)) {
        log->errln("FAIL: UnicodeSet::applyPattern(" +
                   targetRangeVal + ")");
        return;
    }
    this->toTarget.addAll(okAnyway);

    this->roundtripExclusionsSet.clear();
    if (roundtripExclusions != NULL && strlen(roundtripExclusions) > 0) {
        this->roundtripExclusionsSet.applyPattern(roundtripExclusions, status);
        if (U_FAILURE(status)) {
            log->errln("FAIL: UnicodeSet::applyPattern(%s)", roundtripExclusions);
            return;
        }
    }
    
    badCharacters.clear();
    badCharacters.applyPattern("[:Other:]", status);
    if (U_FAILURE(status)) {
        log->errln("FAIL: UnicodeSet::applyPattern([:Other:])");
        return;
    }

    test2(quick);

    if (errorCount > 0) {
        char str[100];
        int32_t length = transliteratorID.extract(str, 100, NULL, status);
        str[length] = 0;
        log->errln("%s errors: %d %s", str, errorCount, (errorCount > errorLimit ? " (at least!)" : " ")); // + ", see " + logFileName);
    } else {
        char str[100];
        int32_t length = transliteratorID.extract(str, 100, NULL, status);
        str[length] = 0;
        log->logln("%s ok", str);
    }
}

UBool RTTest::checkIrrelevants(Transliterator *t, 
                               const UnicodeString& irrelevants) {
    for (int i = 0; i < irrelevants.length(); ++i) {
        UChar c = irrelevants.charAt(i);
        UnicodeString cs(c);
        UnicodeString targ = cs;
        t->transliterate(targ);
        if (cs == targ) return TRUE;
    }
    return FALSE;
}

void RTTest::test2(UBool quick) {

    UnicodeString cs, targ, reverse;
    UErrorCode status = U_ZERO_ERROR;
    UParseError parseError ;
    Transliterator* sourceToTarget = 
        Transliterator::createInstance(transliteratorID, UTRANS_FORWARD, parseError,
                                       status);
    if (sourceToTarget == NULL) {
        log->errln("Fail: createInstance(" + transliteratorID +
                   ") returned NULL. Error: " + u_errorName(status)
                   + "\n\tpreContext : " + prettify(parseError.preContext) 
                   + "\n\tpostContext : " + prettify(parseError.postContext));
        
                return;
    }
    Transliterator* targetToSource = sourceToTarget->createInverse(status);
    if (targetToSource == NULL) {
        log->errln("Fail: " + transliteratorID +
                   ".createInverse() returned NULL");
        delete sourceToTarget;
        return;
    }

    UnicodeSetIterator usi;
    UnicodeSetIterator usi2;

    log->logln("Checking that at least one irrevant characters is not NFC'ed");
    // string is from NFC_NO in the UCD
    UnicodeString irrelevants = CharsToUnicodeString("\\u2000\\u2001\\u2126\\u212A\\u212B\\u2329"); 

    if (checkIrrelevants(sourceToTarget, irrelevants) == FALSE) {
        logFails("Source-Target, irrelevants");
    }
    if (checkIrrelevants(targetToSource, irrelevants) == FALSE) {
        logFails("Target-Source, irrelevants");
    }
            
    if (quick == FALSE){
      log->logln("Checking that toRules works");
      UnicodeString rules = "";
       
      UParseError parseError;
      rules = sourceToTarget->toRules(rules, FALSE);
      Transliterator *sourceToTarget2 = Transliterator::createFromRules(
                                                       "s2t2", rules, 
                                                       UTRANS_FORWARD,
                                                       parseError, status);
      if (U_FAILURE(status)) {
          log->errln("Failed opening from rules %s\n", u_errorName(status));
          return;
      }

      rules = targetToSource->toRules(rules, FALSE);
      Transliterator *targetToSource2 = Transliterator::createFromRules(
                                                       "t2s2", rules, 
                                                       UTRANS_FORWARD,
                                                       parseError, status);
      if (U_FAILURE(status)) {
          log->errln("Failed opening from rules %s\n", u_errorName(status));
          return;
      }

      usi.reset(sourceRange);
      for (;;) {
          int c = usi.next();
          if (c < 0) break;
                    
          UnicodeString cs((UChar32)c);
          UnicodeString targ = cs;
          sourceToTarget->transliterate(targ);
          UnicodeString targ2 = cs;
          sourceToTarget2->transliterate(targ2);
          if (targ != targ2) {
              logToRulesFails("Source-Target, toRules", cs, targ, targ2);
          }
      }
      
      usi.reset(targetRange);
      for (;;) {
          int c = usi.next();
          if (c < 0) break;
              
          UnicodeString cs((UChar32)c);
          UnicodeString targ = cs;
          targetToSource->transliterate(targ);
          UnicodeString targ2 = cs;
          targetToSource2->transliterate(targ2);
          if (targ != targ2) {
              logToRulesFails("Target-Source, toRules", cs, targ, targ2);
          }
      }
      delete sourceToTarget2;
      delete targetToSource2;
    }      

    log->logln("Checking that all source characters convert to target - Singles");

    UnicodeSet failSourceTarg;
    usi.reset(sourceRange);
    for (;;) {
        int c = usi.next();
        if (c < 0) break;
                
        UnicodeString cs((UChar32)c);
        UnicodeString targ = cs;
        sourceToTarget->transliterate(targ);
        if (UnicodeSetIterator::containsAll(toTarget, targ) == FALSE
            || UnicodeSetIterator::containsSome(badCharacters, targ) == TRUE) {
            UnicodeString targD;
            Normalizer::decompose(targ, FALSE, 0, targD, status);
            if (U_FAILURE(status)) {
                log->errln("Failed decomposation %s\n", u_errorName(status));
                return;
            }
            if (UnicodeSetIterator::containsAll(toTarget, targD) == FALSE || 
                UnicodeSetIterator::containsSome(badCharacters, targD) == 
                TRUE) {
                logWrongScript("Source-Target", cs, targ);
                failSourceTarg.add(c);
                continue;
            }
        }

        UnicodeString cs2;
        Normalizer::decompose(cs, FALSE, 0, cs2, status);
        if (U_FAILURE(status)) {
            log->errln("Failed decomposation %s\n", u_errorName(status));
            return;
        }
        UnicodeString targ2 = cs2;
        sourceToTarget->transliterate(targ2);
        if (targ != targ2) {
            logNotCanonical("Source-Target", cs, targ, targ2);
        }
    }

    log->logln("Checking that all source characters convert to target - Doubles");

    UnicodeSet sourceRangeMinusFailures(sourceRange);
    sourceRangeMinusFailures.removeAll(failSourceTarg);
            
    usi.reset(sourceRangeMinusFailures, quick);
    for (;;) {
        int c = usi.next();
        if (c < 0) break;
             
        usi2.reset(sourceRangeMinusFailures, quick);
        for (;;) {
            int d = usi2.next();
            if (d < 0) break;
                    
            UnicodeString cs;
            cs += (UChar32)c;
            cs += (UChar32)d;
            UnicodeString targ = cs;
            sourceToTarget->transliterate(targ);
            if (UnicodeSetIterator::containsAll(toTarget,targ) == FALSE || 
                UnicodeSetIterator::containsSome(badCharacters, targ) 
                == TRUE)
            {
                UnicodeString targD;
                Normalizer::decompose(targ, FALSE, 0, targD, status);
                if (U_FAILURE(status)) {
                    log->errln("Failed decomposation %s\n", u_errorName(status));
                    return;
                }
                if (UnicodeSetIterator::containsAll(toTarget,targD) == FALSE ||
                    UnicodeSetIterator::containsSome(badCharacters, targD) 
                    == TRUE) {
                    logWrongScript("Source-Target", cs, targ);
                    continue;
                }
            }
            UnicodeString cs2;
            Normalizer::decompose(cs, FALSE, 0, cs2, status);
            if (U_FAILURE(status)) {
                log->errln("Failed decomposition %s\n", u_errorName(status));
                return;
            }
            UnicodeString targ2 = cs2;
            sourceToTarget->transliterate(targ2);
            if (targ != targ2) {
                logNotCanonical("Source-Target", cs, targ, targ2);
            }
        }
    }

    log->logln("Checking that target characters convert to source and back - Singles");

    UnicodeSet failTargSource;
    UnicodeSet failRound;

    usi.reset(targetRange);
    for (;;) {
        int c = usi.next();
        if (c < 0) { 
            break;
        }

        UnicodeString cs((UChar32)c);
        targ = cs;
        targetToSource->transliterate(targ);
        reverse = targ;
        sourceToTarget->transliterate(reverse);

        if (UnicodeSetIterator::containsAll(toSource, targ) == FALSE ||
            UnicodeSetIterator::containsSome(badCharacters, targ) == TRUE) {
            UnicodeString targD;
            Normalizer::decompose(targ, FALSE, 0, targD, status);
            if (U_FAILURE(status)) {
                log->errln("Failed decomposation %s\n", u_errorName(status));
                return;
            }
            if (UnicodeSetIterator::containsAll(toSource, targD) == FALSE || 
                UnicodeSetIterator::containsSome(badCharacters, targD) 
                == TRUE) {
                logWrongScript("Target-Source", cs, targ);
                failTargSource.add((UChar32)c);
                continue;
            }
        }
        if (isSame(cs, reverse) == FALSE && 
            roundtripExclusionsSet.contains(c) == FALSE) {
            logRoundTripFailure(cs, targ, reverse);
            failRound.add((UChar32)c);
            continue;
        } 
        
        UnicodeString targ2;
        Normalizer::decompose(targ, FALSE, 0, targ2, status);
        if (U_FAILURE(status)) {
            log->errln("Failed decomposation %s\n", u_errorName(status));
            return;
        }
        UnicodeString reverse2 = targ2;
        sourceToTarget->transliterate(reverse2);
        if (reverse != reverse2) {
            logNotCanonical("Target-Source", cs, targ, targ2);
        }
    }

    log->logln("Checking that target characters convert to source and back - Doubles");
    int32_t count = 0;

    UnicodeSet targetRangeMinusFailures(targetRange);
    targetRangeMinusFailures.removeAll(failTargSource);
    targetRangeMinusFailures.removeAll(failRound);

    usi.reset(targetRangeMinusFailures, quick);
    for (;;) {
        int c = usi.next();
        if (c < 0) {
            break;
        }
        if (++count > pairLimit) {
            //throw new TestTruncated("Test truncated at " + pairLimit + " x 64k pairs");
            log->logln("");
            log->logln((UnicodeString)"Test truncated at " + pairLimit + " x 64k pairs");
            return;
        }

        usi2.reset(targetRangeMinusFailures, quick);
        for (;;) {
            int d = usi.next();
            if (d < 0) {
                break;
            }
            UnicodeString cs;
            cs += (UChar32)c;
            cs += (UChar32)d;

            targ = cs;
            targetToSource->transliterate(targ);
            reverse = targ;
            sourceToTarget->transliterate(reverse);

            if (UnicodeSetIterator::containsAll(toSource, targ) == FALSE || 
                UnicodeSetIterator::containsSome(badCharacters, targ) == TRUE) 
            {
                UnicodeString targD;
                Normalizer::decompose(targ, FALSE, 0, targD, status);
                if (U_FAILURE(status)) {
                    log->errln("Failed decomposation %s\n", 
                               u_errorName(status));
                    return;
                }
                if (UnicodeSetIterator::containsAll(toSource, targD) == FALSE 
                    || UnicodeSetIterator::containsSome(badCharacters, targD)
                       == TRUE) {
                    logWrongScript("Target-Source", cs, targ);
                    continue;
                }
            }
            if (isSame(cs, reverse) == FALSE && 
                roundtripExclusionsSet.contains(c) == FALSE&&
                roundtripExclusionsSet.contains(d) == FALSE) {
                logRoundTripFailure(cs, targ, reverse);
                continue;
            } 
        
            UnicodeString targ2;
            Normalizer::decompose(targ, FALSE, 0, targ2, status);
            if (U_FAILURE(status)) {
                log->errln("Failed decomposation %s\n", u_errorName(status));
                return;
            }
            UnicodeString reverse2 = targ2;
            sourceToTarget->transliterate(reverse2);
            if (reverse != reverse2) {
                logNotCanonical("Target-Source", cs, targ, targ2);
            }
        }
    }
    log->logln("");
    delete sourceToTarget;
    delete targetToSource;
}

void RTTest::logWrongScript(const UnicodeString& label,
                            const UnicodeString& from,
                            const UnicodeString& to) {
    log->errln((UnicodeString)"Fail " +
               label + ": " +
               from + "(" + TestUtility::hex(from) + ") => " +
               to + "(" + TestUtility::hex(to) + ")");
    ++errorCount;
}

void RTTest::logNotCanonical(const UnicodeString& label,
                             const UnicodeString& from,
                             const UnicodeString& to,
                             const UnicodeString& toCan) {
    log->errln((UnicodeString)"Fail (can.equiv)" +
               label + ": " +
               from + "(" + TestUtility::hex(from) + ") => " +
               to + "(" + TestUtility::hex(to) + ")" +
               toCan + " (" +
               TestUtility::hex(toCan) + ")"
               );
    ++errorCount;
}

void RTTest::logFails(const UnicodeString& label) {
    log->errln((UnicodeString)"<br>Fail (can.equiv) " + label);
    ++errorCount;
}

void RTTest::logToRulesFails(const UnicodeString& label, 
                             const UnicodeString& from, 
                             const UnicodeString& to, 
                             const UnicodeString& toCan) {
    log->errln((UnicodeString)"Fail (can.equiv)" +
               label + ": " +
               from + "(" + TestUtility::hex(from) + ") => " +
               to + "(" + TestUtility::hex(to) + ")" +
               toCan + " (" +
               TestUtility::hex(toCan) + ")"
               );
    ++errorCount;
}


void RTTest::logRoundTripFailure(const UnicodeString& from,
                                 const UnicodeString& to,
                                 const UnicodeString& back) {
    if (legalSource->is(from) == FALSE) return; // skip illegals

    log->errln((UnicodeString)"Fail Roundtrip: " +
               from + "(" + TestUtility::hex(from) + ") => " +
               to + "(" + TestUtility::hex(to) + ") => " +
               back + "(" + TestUtility::hex(back) + ") => ");
    ++errorCount;
}

//--------------------------------------------------------------------
// Specific Tests
//--------------------------------------------------------------------

void TransliteratorRoundTripTest::TestKana() {
    RTTest test("Katakana-Hiragana");
    Legal *legal = new Legal();
    test.test(UnicodeString("[[:katakana:]\\u30A1-\\u30FA\\u30FC]", ""), 
              UnicodeString("[[:hiragana:]\\u3040-\\u3094\\u30FC]", ""),
              "[\\u30FC\\u309D\\u309E\\uFF66-\\uFF9D]", this, 
              quick, legal);
    delete legal;
}

void TransliteratorRoundTripTest::TestHiragana() {
    RTTest test("Latin-Hiragana");
    Legal *legal = new Legal();
    test.test(UnicodeString("[a-zA-Z]", ""), 
              UnicodeString("[[:hiragana:]\\u3040-\\u3094]", ""), 
              "[\\u309D\\u309E]", this, quick, legal);
    delete legal;
}

void TransliteratorRoundTripTest::TestKatakana() {
    RTTest test("Latin-Katakana");
    Legal *legal = new Legal();
    test.test(UnicodeString("[a-zA-Z]", ""), 
              UnicodeString("[[:katakana:]\\u30A1-\\u30FA\\u30FC]", ""),
              "[\\u30FD\\u30FE\\uFF66-\\uFF9D]", this, quick, legal);
    delete legal;
}

void TransliteratorRoundTripTest::TestJamo() {
    RTTest t("Latin-Jamo");
    Legal *legal = new Legal();
    t.test(UnicodeString("[a-zA-Z]", ""), 
           UnicodeString("[\\u1100-\\u1112 \\u1161-\\u1175 \\u11A8-\\u11C2]", 
                         ""), 
           NULL, this, quick, new LegalJamo());
    delete legal;
}

void TransliteratorRoundTripTest::TestHangul() {
    RTTest t("Latin-Hangul");
    Legal *legal = new Legal();
    t.test(UnicodeString("[a-zA-Z]", ""), 
           UnicodeString("[\\uAC00-\\uD7A4]", ""), 
           NULL, this, quick, legal);
    delete legal;
}

void TransliteratorRoundTripTest::TestGreek() {
    RTTest test("Latin-Greek");
    LegalGreek *legal = new LegalGreek(TRUE);
    test.test(UnicodeString("[a-zA-Z]", ""), 
              UnicodeString("[\\u003B\\u00B7[:Greek:]-[\\u03D7-\\u03EF]]", 
                            ""),
              "[\\u00B5\\u037A\\u03D0-\\u03F5]", /* exclusions */
              this, quick, legal);
    delete legal;
}


void TransliteratorRoundTripTest::TestGreekUNGEGN() {
    RTTest test("Latin-Greek/UNGEGN");
    LegalGreek *legal = new LegalGreek(FALSE);
    test.test(UnicodeString("[a-zA-Z]", ""), 
              UnicodeString("[\\u003B\\u00B7[:Greek:]-[\\u03D7-\\u03EF]]", 
                            ""), 
              "[\\u00B5\\u037A\\u03D0-\\uFFFF]", /* roundtrip exclusions */
              this, quick, legal);
    delete legal;
}

void TransliteratorRoundTripTest::Testel() {
    RTTest test("Latin-el");
    LegalGreek *legal = new LegalGreek(FALSE);
    test.test(UnicodeString("[a-zA-Z]", ""), 
              UnicodeString("[\\u003B\\u00B7[:Greek:]-[\\u03D7-\\u03EF]]", 
                            ""), 
              "[\\u00B5\\u037A\\u03D0-\\uFFFF]", /* exclusions */
              this, quick, legal);
    delete legal;
}

void TransliteratorRoundTripTest::TestCyrillic() {
    RTTest test("Latin-Cyrillic");
    Legal *legal = new Legal();
    test.test(UnicodeString("[a-zA-Z\\u0110\\u0111]", ""), 
              UnicodeString("[\\u0400-\\u045F]", ""), NULL, this, quick, 
              legal);
    delete legal;
}


// Inter-Indic Tests ----------------------------------

void TransliteratorRoundTripTest::TestDevanagariLatin() {
    RTTest test("Latin-DEVANAGARI");
    Legal *legal = new Legal();
    test.test(UnicodeString("[a-zA-Z]", ""), 
              UnicodeString("[:Devanagari:]", ""), NULL, this, quick, 
              legal);
    delete legal;
}

static const char * array[][4] = {
    {"BENGALI-DEVANAGARI", "[:BENGALI:]", "[:Devanagari:]", 
    "[\\u0951-\\u0954\\u0943-\\u0949\\u094a\\u0962\\u0963\\u090D\\u090e\\u0911\\u0912\\u0929\\u0933\\u0934\\u0935\\u093d\\u0950\\u0958\\u0959\\u095a\\u095b\\u095e\\u09f0\\u09f1]"}, /*roundtrip exclusions*/

    {"DEVANAGARI-BENGALI", "[:Devanagari:]", "[:BENGALI:]",
    "[\\u0951-\\u0954\\u09D7\\u090D\\u090e\\u0911\\u0912\\u0929\\u0933\\u0934\\u0935\\u093d\\u0950\\u0958\\u0959\\u095a\\u095b\\u095e\\u09f0\\u09f1]"}, /*roundtrip exclusions*/

    {"GURMUKHI-DEVANAGARI", "[:GURMUKHI:]", "[:Devanagari:]", 
    "[\\u0936\\u0933\\u0951-\\u0954\\u0902\\u0903\\u0943-\\u0949\\u094a\\u0962\\u0963\\u090B\\u090C\\u090D\\u090e\\u0911\\u0912\\u0934\\u0937\\u093D\\u0950\\u0960\\u0961\\u0a72\\u0a73\\u0a74]"}, /*roundtrip exclusions*/

    {"DEVANAGARI-GURMUKHI", "[:Devanagari:]", "[:GURMUKHI:]",
    "[\\u0946\\u0A5C\\u0951-\\u0954\\u0A70\\u0A71\\u090B\\u090C\\u090D\\u090e\\u0911\\u0912\\u0934\\u0937\\u093D\\u0950\\u0960\\u0961\\u0a72\\u0a73\\u0a74]"}, /*roundtrip exclusions*/

    {"GUJARATI-DEVANAGARI", "[:GUJARATI:]", "[:Devanagari:]", 
    "[\\u0946\\u094A\\u0962\\u0963\\u0951-\\u0954\\u0961\\u090c\\u090e\\u0912]"}, /*roundtrip exclusions*/

    {"DEVANAGARI-GUJARATI", "[:Devanagari:]", "[:GUJARATI:]",
    "[\\u0951-\\u0954\\u0961\\u090c\\u090e\\u0912]"}, /*roundtrip exclusions*/

    {"ORIYA-DEVANAGARI", "[:ORIYA:]", "[:Devanagari:]", 
    "[\\u0943-\\u094a\\u0962\\u0963\\u0951-\\u0954\\u0950\\u090D\\u090e\\u0912\\u0911\\u0931\\u0935]"}, /*roundtrip exclusions*/

    {"DEVANAGARI-ORIYA", "[:Devanagari:]", "[:ORIYA:]",
    "[\\u0b5f\\u0b56\\u0b57\\u0950\\u090D\\u090e\\u0912\\u0911\\u0931\\u0935]"}, /*roundtrip exclusions*/

    {"Tamil-DEVANAGARI", "[:tamil:]", "[:Devanagari:]", 
    "[\\u093c\\u0943-\\u094a\\u0951-\\u0954\\u0962\\u0963\\u090B\\u090C\\u090D\\u0911\\u0916\\u0917\\u0918\\u091B\\u091D\\u0920\\u0921\\u0922\\u0925\\u0926\\u0927\\u092B\\u092C\\u092D\\u0936\\u093d\\u0950[\\u0958-\\u0961]]"}, /*roundtrip exclusions*/

    {"DEVANAGARI-Tamil", "[:Devanagari:]", "[:tamil:]", 
    "[\\u0bd7]"}, /*roundtrip exclusions*/

    {"Telugu-DEVANAGARI", "[:telugu:]", "[:Devanagari:]", 
    "[\\u093c\\u0950\\u0945\\u0949\\u0951-\\u0954\\u0962\\u0963\\u090D\\u0911\\u093d\\u0929\\u0934[\\u0958-\\u095f]]"}, /*roundtrip exclusions*/

    {"DEVANAGARI-TELUGU", "[:Devanagari:]", "[:TELUGU:]",
    "[\\u0c55\\u0c56\\u0950\\u090D\\u0911\\u093d\\u0929\\u0934[\\u0958-\\u095f]]"}, /*roundtrip exclusions*/

    {"KANNADA-DEVANAGARI", "[:KANNADA:]", "[:Devanagari:]", 
    "[\\u0946\\u093c\\u0950\\u0945\\u0949\\u0951-\\u0954\\u0962\\u0963\\u0950\\u090D\\u0911\\u093d\\u0929\\u0934[\\u0958-\\u095f]]"}, /*roundtrip exclusions*/

    {"DEVANAGARI-KANNADA", "[:Devanagari:]", "[:KANNADA:]",
    "[\\u0cde\\u0cd5\\u0cd6\\u0950\\u090D\\u0911\\u093d\\u0929\\u0934[\\u0958-\\u095f]]"}, /*roundtrip exclusions*/ 

    {"MALAYALAM-DEVANAGARI", "[:MALAYALAM:]", "[:Devanagari:]", 
    "[\\u094a\\u094b\\u094c\\u093c\\u0950\\u0944\\u0945\\u0949\\u0951-\\u0954\\u0962\\u0963\\u090D\\u0911\\u093d\\u0929\\u0934[\\u0958-\\u095f]]"}, /*roundtrip exclusions*/

    {"DEVANAGARI-MALAYALAM", "[:Devanagari:]", "[:MALAYALAM:]",
    "[\\u0d4c\\u0d57\\u0950\\u090D\\u0911\\u093d\\u0929\\u0934[\\u0958-\\u095f]]"}, /*roundtrip exclusions*/

    {"GURMUKHI-BENGALI", "[:GURMUKHI:]", "[:BENGALI:]",  
    "[\\u09b6\\u09e2\\u09e3\\u09c3\\u09c4\\u09d7\\u098B\\u098C\\u09B7\\u09E0\\u09E1\\u09F0\\u09F1]"}, /*roundtrip exclusions*/

    {"BENGALI-GURMUKHI", "[:BENGALI:]", "[:GURMUKHI:]",
    "[\\u0a5c\\u0a47\\u0a70\\u0a71\\u0A33\\u0A35\\u0A59\\u0A5A\\u0A5B\\u0A5E\\u0A72\\u0A73\\u0A74]"}, /*roundtrip exclusions*/

    {"GUJARATI-BENGALI", "[:GUJARATI:]", "[:BENGALI:]", 
    "[\\u09d7\\u09e2\\u09e3\\u098c\\u09e1\\u09f0\\u09f1]"}, /*roundtrip exclusions*/

    {"BENGALI-GUJARATI", "[:BENGALI:]", "[:GUJARATI:]",
    "[\\u0A82\\u0a83\\u0Ac9\\u0Ac5\\u0ac7\\u0A8D\\u0A91\\u0AB3\\u0AB5\\u0ABD\\u0AD0]"}, /*roundtrip exclusions*/

    {"ORIYA-BENGALI", "[:ORIYA:]", "[:BENGALI:]", 
    "[\\u09c4\\u09e2\\u09e3\\u09f0\\u09f1]"}, /*roundtrip exclusions*/

    {"BENGALI-ORIYA", "[:BENGALI:]", "[:ORIYA:]",
    "[\\u0b5f\\u0b56\\u0b33\\u0b3d]"}, /*roundtrip exclusions*/

    {"Tamil-BENGALI", "[:tamil:]", "[:BENGALI:]", 
    "[\\u09bc\\u09c3\\u09c4\\u09e2\\u09e3\\u09f0\\u09f1\\u098B\\u098C\\u0996\\u0997\\u0998\\u099B\\u099D\\u09A0\\u09A1\\u09A2\\u09A5\\u09A6\\u09A7\\u09AB\\u09AC\\u09AD\\u09B6\\u09DC\\u09DD\\u09DF\\u09E0\\u09E1]"}, /*roundtrip exclusions*/

    {"BENGALI-Tamil", "[:BENGALI:]", "[:tamil:]",
    "[\\u0bc6\\u0bc7\\u0bca\\u0B8E\\u0B92\\u0BA9\\u0BB1\\u0BB3\\u0BB4\\u0BB5]"}, /*roundtrip exclusions*/

    {"Telugu-BENGALI", "[:telugu:]", "[:BENGALI:]", 
    "[\\u09e2\\u09e3\\u09bc\\u09d7\\u09f0\\u09f1\\u09dc\\u09dd\\u09df]"}, /*roundtrip exclusions*/

    {"BENGALI-TELUGU", "[:BENGALI:]", "[:TELUGU:]",
    "[\\u0c55\\u0c56\\u0c47\\u0c46\\u0c4a\\u0C0E\\u0C12\\u0C31\\u0C33\\u0C35]"}, /*roundtrip exclusions*/

    {"KANNADA-BENGALI", "[:KANNADA:]", "[:BENGALI:]", 
    "[\\u09e2\\u09e3\\u09bc\\u09d7\\u09f0\\u09f1\\u09dc\\u09dd\\u09df]"}, /*roundtrip exclusions*/

    {"BENGALI-KANNADA", "[:BENGALI:]", "[:KANNADA:]",
    "[\\u0cc6\\u0cca\\u0cd5\\u0cd6\\u0cc7\\u0C8E\\u0C92\\u0CB1\\u0cb3\\u0cb5\\u0cde]"}, /*roundtrip exclusions*/ 

    {"MALAYALAM-BENGALI", "[:MALAYALAM:]", "[:BENGALI:]", 
    "[\\u09e2\\u09e3\\u09bc\\u09c4\\u09f0\\u09f1\\u09dc\\u09dd\\u09df]"}, /*roundtrip exclusions*/

    {"BENGALI-MALAYALAM", "[:BENGALI:]", "[:MALAYALAM:]",
    "[\\u0d46\\u0d4a\\u0d47\\u0d31-\\u0d35\\u0d0e\\u0d12]"}, /*roundtrip exclusions*/
       
    {"GUJARATI-GURMUKHI", "[:GUJARATI:]", "[:GURMUKHI:]", 
    "[\\u0ab3\\u0ab6\\u0A70\\u0a71\\u0a82\\u0a83\\u0ac3\\u0ac4\\u0ac5\\u0ac9\\u0a5c\\u0a72\\u0a73\\u0a74\\u0a8b\\u0a8d\\u0a91\\u0abd]"}, /*roundtrip exclusions*/

    {"GURMUKHI-GUJARATI", "[:GURMUKHI:]", "[:GUJARATI:]",
    "[\\u0ab3\\u0ab6\\u0A70\\u0a71\\u0a82\\u0a83\\u0ac3\\u0ac4\\u0ac5\\u0ac9\\u0a5c\\u0a72\\u0a73\\u0a74\\u0a8b\\u0a8d\\u0a91\\u0ab7\\u0abd\\u0ad0\\u0ae0]"}, /*roundtrip exclusions*/

    {"ORIYA-GURMUKHI", "[:ORIYA:]", "[:GURMUKHI:]", 
    "[\\u0a21\\u0a47\\u0a71\\u0b02\\u0b03\\u0b33\\u0b36\\u0b43\\u0b56\\u0b57\\u0B0B\\u0B0C\\u0B37\\u0B3D\\u0B5F\\u0B60\\u0B61\\u0a35\\u0a72\\u0a73\\u0a74]"}, /*roundtrip exclusions*/

    {"GURMUKHI-ORIYA", "[:GURMUKHI:]", "[:ORIYA:]",
    "[\\u0a71\\u0b02\\u0b03\\u0b33\\u0b36\\u0b43\\u0b56\\u0b57\\u0B0B\\u0B0C\\u0B37\\u0B3D\\u0B5F\\u0B60\\u0B61]"}, /*roundtrip exclusions*/

    {"TAMIL-GURMUKHI", "[:TAMIL:]", "[:GURMUKHI:]", 
    "[\\u0a33\\u0a36\\u0a3c\\u0a70\\u0a71\\u0a47\\u0A16\\u0A17\\u0A18\\u0A1B\\u0A1D\\u0A20\\u0A21\\u0A22\\u0A25\\u0A26\\u0A27\\u0A2B\\u0A2C\\u0A2D\\u0A59\\u0A5A\\u0A5B\\u0A5C\\u0A5E\\u0A72\\u0A73\\u0A74]"}, /*roundtrip exclusions*/

    {"GURMUKHI-TAMIL", "[:GURMUKHI:]", "[:TAMIL:]",
    "[\\u0bc6\\u0bca\\u0bd7\\u0bb7\\u0bb3\\u0b83\\u0B8E\\u0B92\\u0BA9\\u0BB1\\u0BB4]"}, /*roundtrip exclusions*/

    {"TELUGU-GURMUKHI", "[:TELUGU:]", "[:GURMUKHI:]", 
    "[\\u0a33\\u0a36\\u0a3c\\u0a70\\u0a71\\u0A59\\u0A5A\\u0A5B\\u0A5C\\u0A5E\\u0A72\\u0A73\\u0A74]"}, /*roundtrip exclusions*/

    {"GURMUKHI-TELUGU", "[:GURMUKHI:]", "[:TELUGU:]",
    "[\\u0c02\\u0c03\\u0c33\\u0c36\\u0c44\\u0c43\\u0c46\\u0c4a\\u0c56\\u0c55\\u0C0B\\u0C0C\\u0C0E\\u0C12\\u0C31\\u0C37\\u0C60\\u0C61]"}, /*roundtrip exclusions*/

    {"KANNADA-GURMUKHI", "[:KANNADA:]", "[:GURMUKHI:]", 
    "[\\u0a33\\u0a36\\u0a3c\\u0a70\\u0a71\\u0A59\\u0A5A\\u0A5B\\u0A5C\\u0A5E\\u0A72\\u0A73\\u0A74]"}, /*roundtrip exclusions*/

    {"GURMUKHI-KANNADA", "[:GURMUKHI:]", "[:KANNADA:]",
    "[\\u0c83\\u0cb3\\u0cb6\\u0cc4\\u0cc3\\u0cc6\\u0cca\\u0cd5\\u0cd6\\u0C8B\\u0C8C\\u0C8E\\u0C92\\u0CB1\\u0CB7\\u0CE0\\u0CE1]"}, /*roundtrip exclusions*/

    {"MALAYALAM-GURMUKHI", "[:MALAYALAM:]", "[:GURMUKHI:]", 
    "[\\u0a4b\\u0a4c\\u0a33\\u0a36\\u0a3c\\u0a70\\u0a71\\u0A59\\u0A5A\\u0A5B\\u0A5C\\u0A5E\\u0A72\\u0A73\\u0A74]"}, /*roundtrip exclusions*/

    {"GURMUKHI-MALAYALAM", "[:GURMUKHI:]", "[:MALAYALAM:]",
    "[\\u0d03\\u0d33\\u0d36\\u0d43\\u0d46\\u0d4a\\u0d4c\\u0d57\\u0D0B\\u0D0C\\u0D0E\\u0D12\\u0D31\\u0D34\\u0D37\\u0D60\\u0D61]"}, /*roundtrip exclusions*/

    {"GUJARATI-ORIYA", "[:GUJARATI:]", "[:ORIYA:]", 
    "[\\u0b56\\u0b57\\u0B0C\\u0B5F\\u0B61]"}, /*roundtrip exclusions*/

    {"ORIYA-GUJARATI", "[:ORIYA:]", "[:GUJARATI:]",
    "[\\u0Ac4\\u0Ac5\\u0Ac9\\u0Ac7\\u0A8D\\u0A91\\u0AB5\\u0Ad0]"}, /*roundtrip exclusions*/

    {"TAMIL-GUJARATI", "[:TAMIL:]", "[:GUJARATI:]", 
    "[\\u0abc\\u0ac3\\u0Ac4\\u0Ac5\\u0Ac9\\u0Ac7\\u0A8B\\u0A8D\\u0A91\\u0A96\\u0A97\\u0A98\\u0A9B\\u0A9D\\u0AA0\\u0AA1\\u0AA2\\u0AA5\\u0AA6\\u0AA7\\u0AAB\\u0AAC\\u0AAD\\u0AB6\\u0ABD\\u0AD0\\u0AE0]"}, /*roundtrip exclusions*/

    {"GUJARATI-TAMIL", "[:GUJARATI:]", "[:TAMIL:]",
    "[\\u0Bc6\\u0Bca\\u0Bd7\\u0B8E\\u0B92\\u0BA9\\u0BB1\\u0BB4]"}, /*roundtrip exclusions*/

    {"TELUGU-GUJARATI", "[:TELUGU:]", "[:GUJARATI:]", 
    "[\\u0abc\\u0Ac5\\u0Ac9\\u0A8D\\u0A91\\u0ABD\\u0Ad0]"}, /*roundtrip exclusions*/

    {"GUJARATI-TELUGU", "[:GUJARATI:]", "[:TELUGU:]",
    "[\\u0c46\\u0c4a\\u0c55\\u0c56\\u0C0C\\u0C0E\\u0C12\\u0C31\\u0C61]"}, /*roundtrip exclusions*/

    {"KANNADA-GUJARATI", "[:KANNADA:]", "[:GUJARATI:]", 
    "[\\u0abc\\u0Ac5\\u0Ac9\\u0A8D\\u0A91\\u0ABD\\u0Ad0]"}, /*roundtrip exclusions*/

    {"GUJARATI-KANNADA", "[:GUJARATI:]", "[:KANNADA:]",
    "[\\u0cc6\\u0cca\\u0cd5\\u0cd6\\u0C8C\\u0C8E\\u0C92\\u0CB1\\u0CDE\\u0CE1]"}, /*roundtrip exclusions*/

    {"MALAYALAM-GUJARATI", "[:MALAYALAM:]", "[:GUJARATI:]", 
    "[\\u0ac4\\u0acb\\u0acc\\u0abc\\u0Ac5\\u0Ac9\\u0A8D\\u0A91\\u0ABD\\u0Ad0]"}, /*roundtrip exclusions*/

    {"GUJARATI-MALAYALAM", "[:GUJARATI:]", "[:MALAYALAM:]",
    "[\\u0d46\\u0d4a\\u0d4c\\u0d55\\u0d57\\u0D0C\\u0D0E\\u0D12\\u0D31\\u0D34\\u0D61]"}, /*roundtrip exclusions*/

    {"TAMIL-ORIYA", "[:TAMIL:]", "[:ORIYA:]", 
    "[\\u0b3c\\u0b43\\u0b56\\u0B0B\\u0B0C\\u0B16\\u0B17\\u0B18\\u0B1B\\u0B1D\\u0B20\\u0B21\\u0B22\\u0B25\\u0B26\\u0B27\\u0B2B\\u0B2C\\u0B2D\\u0B36\\u0B3D\\u0B5C\\u0B5D\\u0B5F\\u0B60\\u0B61]"}, /*roundtrip exclusions*/

    {"ORIYA-TAMIL", "[:ORIYA:]", "[:TAMIL:]",
    "[\\u0bc6\\u0bca\\u0bc7\\u0B8E\\u0B92\\u0BA9\\u0BB1\\u0BB4\\u0BB5]"}, /*roundtrip exclusions*/

    {"TELUGU-ORIYA", "[:TELUGU:]", "[:ORIYA:]", 
    "[\\u0b3c\\u0b57\\u0b56\\u0B3D\\u0B5C\\u0B5D\\u0B5F]"}, /*roundtrip exclusions*/

    {"ORIYA-TELUGU", "[:ORIYA:]", "[:TELUGU:]",
    "[\\u0c44\\u0c46\\u0c4a\\u0c55\\u0c47\\u0C0E\\u0C12\\u0C31\\u0C35]"}, /*roundtrip exclusions*/

    {"KANNADA-ORIYA", "[:KANNADA:]", "[:ORIYA:]", 
    "[\\u0b3c\\u0b57\\u0B3D\\u0B5C\\u0B5D\\u0B5F]"}, /*roundtrip exclusions*/

    {"ORIYA-KANNADA", "[:ORIYA:]", "[:KANNADA:]",
    "[\\u0cc4\\u0cc6\\u0cca\\u0cd5\\u0cc7\\u0C8E\\u0C92\\u0CB1\\u0CB5\\u0CDE]"}, /*roundtrip exclusions*/

    {"MALAYALAM-ORIYA", "[:MALAYALAM:]", "[:ORIYA:]", 
    "[\\u0b3c\\u0b56\\u0B3D\\u0B5C\\u0B5D\\u0B5F]"}, /*roundtrip exclusions*/

    {"ORIYA-MALAYALAM", "[:ORIYA:]", "[:MALAYALAM:]",
    "[\\u0D47\\u0D46\\u0D4a\\u0D0E\\u0D12\\u0D31\\u0D34\\u0D35]"}, /*roundtrip exclusions*/

    {"TELUGU-TAMIL", "[:TELUGU:]", "[:TAMIL:]", 
    "[\\u0bd7\\u0ba9\\u0bb4]"}, /*roundtrip exclusions*/

    {"TAMIL-TELUGU", "[:TAMIL:]", "[:TELUGU:]",
    "[\\u0c43\\u0c44\\u0c46\\u0c47\\u0c55\\u0c56\\u0c66\\u0C0B\\u0C0C\\u0C16\\u0C17\\u0C18\\u0C1B\\u0C1D\\u0C20\\u0C21\\u0C22\\u0C25\\u0C26\\u0C27\\u0C2B\\u0C2C\\u0C2D\\u0C36\\u0C60\\u0C61]"}, /*roundtrip exclusions*/

    {"KANNADA-TAMIL", "[:KANNADA:]", "[:TAMIL:]", 
    "[\\u0bd7\\u0bc6\\u0ba9\\u0bb4]"}, /*roundtrip exclusions*/

    {"TAMIL-KANNADA", "[:TAMIL:]", "[:KANNADA:]",
    "[\\u0cc3\\u0cc4\\u0cc6\\u0cc7\\u0cd5\\u0cd6\\u0C8B\\u0C8C\\u0C96\\u0C97\\u0C98\\u0C9B\\u0C9D\\u0CA0\\u0CA1\\u0CA2\\u0CA5\\u0CA6\\u0CA7\\u0CAB\\u0CAC\\u0CAD\\u0CB6\\u0CDE\\u0CE0\\u0CE1]"}, /*roundtrip exclusions*/

    {"MALAYALAM-TAMIL", "[:MALAYALAM:]", "[:TAMIL:]", 
    "[\\u0ba9]"}, /*roundtrip exclusions*/

    {"TAMIL-MALAYALAM", "[:TAMIL:]", "[:MALAYALAM:]",
    "[\\u0d43\\u0d12\\u0D0B\\u0D0C\\u0D16\\u0D17\\u0D18\\u0D1B\\u0D1D\\u0D20\\u0D21\\u0D22\\u0D25\\u0D26\\u0D27\\u0D2B\\u0D2C\\u0D2D\\u0D36\\u0D60\\u0D61]"}, /*roundtrip exclusions*/

    {"KANNADA-TELUGU", "[:KANNADA:]", "[:TELUGU:]", 
    "[\\u0c3f\\u0c46\\u0c48\\u0c4a]"}, /*roundtrip exclusions*/

    {"TELUGU-KANNADA", "[:TELUGU:]", "[:KANNADA:]",
    "[\\u0cc8\\u0cd5\\u0cd6\\u0CDE]"}, /*roundtrip exclusions*/

    {"MALAYALAM-TELUGU", "[:MALAYALAM:]", "[:TELUGU:]", 
    "[\\u0c44\\u0c4a\\u0c4c\\u0c4b\\u0c55\\u0c56]"}, /*roundtrip exclusions*/

    {"TELUGU-MALAYALAM", "[:TELUGU:]", "[:MALAYALAM:]",
    "[\\u0d4c\\u0d57\\u0D34]"}, /*roundtrip exclusions*/

    {"MALAYALAM-KANNADA", "[:MALAYALAM:]", "[:KANNADA:]", 
    "[\\u0cc4\\u0cc6\\u0cca\\u0ccc\\u0ccb\\u0cd5\\u0cd6\\u0cDe]"}, /*roundtrip exclusions*/

    {"KANNADA-MALAYALAM", "[:KANNADA:]", "[:MALAYALAM:]",
    "[\\u0d4c\\u0d57\\u0d46\\u0D34]"} /*roundtrip exclusions*/
};

void TransliteratorRoundTripTest::TestInterIndic() {
    int num = sizeof(array) / 4;
    if(quick==TRUE){
        logln("Testing only 5 of %i. Skipping rest (use -e for exhaustive)",num);
        num = 5;
    }
    for(int i = 0; i < num;i ++){
        RTTest test(array[i][0]);
        Legal *legal = new Legal();
        test.test(UnicodeString(array[i][1], ""), 
                  UnicodeString(array[i][2], ""), 
                  array[i][3], /* roundtrip exclusions */
                  this, quick, legal);
        delete legal;
    }
}

// end indic tests ----------------------------------------------------------
