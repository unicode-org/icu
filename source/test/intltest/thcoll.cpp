/*
**********************************************************************
*   Copyright (C) 1999-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   12/09/99    aliu        Ported from Java.
**********************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_COLLATION

#include "thcoll.h"
#include "unicode/utypes.h"
#include "unicode/coll.h"
#include "unicode/sortkey.h"
#include "unicode/ustring.h"
#include "cstring.h"
#include "filestrm.h"

/**
 * The TestDictionary test expects a file of this name, with this
 * encoding, to be present in the directory $ICU/source/test/testdata.
 */
//#define TEST_FILE           "th18057.txt"
#define TEST_FILE           "riwords.txt"
#define TEST_FILE_ENCODING  "UTF8"

/**
 * This is the most failures we show in TestDictionary.  If this number
 * is < 0, we show all failures.
 */
#define MAX_FAILURES_TO_SHOW -1

#define CASE(id,test)                 \
    case id:                          \
        name = #test;                 \
        if (exec) {                   \
            logln(#test "---");       \
            logln((UnicodeString)""); \
            test();                   \
        }                             \
        break;

CollationThaiTest::CollationThaiTest() {
    UErrorCode status = U_ZERO_ERROR;
    coll = Collator::createInstance(Locale("th", "TH", ""), status);
    if (coll && U_SUCCESS(status)) {
        //coll->setStrength(Collator::TERTIARY);
    } else {
        delete coll;
        coll = 0;
    }
}

CollationThaiTest::~CollationThaiTest() {
    delete coll;
}

void CollationThaiTest::runIndexedTest(int32_t index, UBool exec, const char* &name,
                                       char* /*par*/) {

    if((!coll) && exec) {
      errln(__FILE__ " cannot test - failed to create collator.");
      name = "";
      return;
    }

    switch (index) {
        CASE(0,TestDictionary)
        CASE(1,TestCornerCases)
        CASE(2,TestNamesList)
        CASE(3,TestInvalidThai)
        CASE(4,TestReordering)
        default: name = ""; break;
    }
}

/**
 * Read a line terminated by a single ^J or ^M, and convert it from
 * the TEST_FILE_ENCODING to Unicode.  ASSUMES FILE LINES ARE 127
 * characters long or less.  This is true for th18057.txt, which
 * has 80-char or shorter lines.  DOES NOT HANDLE ^M^J sequence.
 */
static UBool readLine(FileStream *in, UnicodeString& line, const char* encoding) {
    if (T_FileStream_eof(in)) {
        return FALSE;
    }
    char buffer[1024];
    char* p = buffer;
    char* limit = p + sizeof(buffer) - 1; // Leave space for 0
    while (p<limit) {
        int c = T_FileStream_getc(in);
        if (c < 0 || c == 0xD || c == 0xA) {
            break;
        }
        *p++ = c;
    }
    *p = 0;
    line = UnicodeString(buffer, encoding);
    return TRUE;
}


/**
 * Read the external names list, and confirms that the collator 
 * gets the same results when comparing lines one to another
 * using regular and iterative comparison.
 */
void CollationThaiTest::TestNamesList(void) {
    if (coll == 0) {
        errln("Error: could not construct Thai collator");
        return;
    }
 
    // Read in a dictionary of Thai words
    UErrorCode status = U_ZERO_ERROR;
    char buffer[1024];
    uprv_strcpy(buffer,IntlTest::loadTestData(status) );
    char* index = 0;
   
    if (U_FAILURE(status)) {
        errln("ERROR: could not open test data %s", u_errorName(status));
	    return;
    }
    index=strrchr(buffer,(char)U_FILE_SEP_CHAR);

    if((unsigned int)(index-buffer) != (strlen(buffer)-1)){
            *(index+1)=0;
    }
    uprv_strcat(buffer,".."U_FILE_SEP_STRING);
    uprv_strcat(buffer, "TestNames_Thai.txt");

    FileStream *in = T_FileStream_open(buffer, "rb");
    if (in == 0) {
        logln((UnicodeString)"Could not find file: " + buffer +" will not do this test");
        return;        
    }

    //
    // Loop through each word in the dictionary and compare it to the previous
    // word.  They should be in sorted order.
    //
    UnicodeString lastWord, word;
    int32_t line = 0;
    //int32_t failed = 0;
    int32_t wordCount = 0;
    while (readLine(in, word, "UTF16LE")) {
        line++;

        // Skip comments and blank lines
        if (word.charAt(0) == 0x23 || word.length() == 0) {
            continue;
        }

        // Show the first 8 words being compared, so we can see what's happening
        ++wordCount;
        if (wordCount <= 8) {
            UnicodeString str;
            logln((UnicodeString)"Word " + wordCount + ": " + IntlTest::prettify(word, str));
        }

        if (lastWord.length() > 0) {
            Collator::EComparisonResult result = coll->compare(lastWord, word);
            doTest(coll, lastWord, word, result);
        }
        lastWord = word;
    }


    logln((UnicodeString)"Words checked: " + wordCount);
    T_FileStream_close(in);
}

/**
 * Read the external dictionary file, which is already in proper
 * sorted order, and confirm that the collator compares each line as
 * preceding the following line.
 */
void CollationThaiTest::TestDictionary(void) {
    if (coll == 0) {
        errln("Error: could not construct Thai collator");
        return;
    }
 
    // Read in a dictionary of Thai words
    UErrorCode status = U_ZERO_ERROR;
    char buffer[1024];
    uprv_strcpy(buffer,IntlTest::loadTestData(status) );
    char* index = 0;
   
    if (U_FAILURE(status)) {
        errln("ERROR: could not open test data %s", u_errorName(status));
	    return;
    }
    index=strrchr(buffer,(char)U_FILE_SEP_CHAR);

    if((unsigned int)(index-buffer) != (strlen(buffer)-1)){
            *(index+1)=0;
    }
    uprv_strcat(buffer,".."U_FILE_SEP_STRING);
    uprv_strcat(buffer, TEST_FILE);

    FileStream *in = T_FileStream_open(buffer, "rb");
    if (in == 0) {
        infoln((UnicodeString)"INFO: could not open test file " + buffer + ". Aborting test.");
        return;        
    }

    //
    // Loop through each word in the dictionary and compare it to the previous
    // word.  They should be in sorted order.
    //
    UnicodeString lastWord, word;
    int32_t line = 0;
    int32_t failed = 0;
    int32_t wordCount = 0;
    while (readLine(in, word, "UTF8")) {
        line++;

        // Skip comments and blank lines
        if (word.charAt(0) == 0x23 || word.length() == 0) {
            continue;
        }

        // Show the first 8 words being compared, so we can see what's happening
        ++wordCount;
        if (wordCount <= 8) {
            UnicodeString str;
            logln((UnicodeString)"Word " + wordCount + ": " + IntlTest::prettify(word, str));
        }

        if (lastWord.length() > 0) {
            // line enabled for j2720 
            doTest(coll, lastWord, word, Collator::LESS);
            int32_t result = coll->compare(lastWord, word);

            if (result >= 0) {
                failed++;
                if (MAX_FAILURES_TO_SHOW < 0 || failed <= MAX_FAILURES_TO_SHOW) {
                    UnicodeString str;
                    UnicodeString msg =
                        UnicodeString("--------------------------------------------\n")
                        + line
                        + " compare(" + IntlTest::prettify(lastWord, str);
                    msg += UnicodeString(", ")
                        + IntlTest::prettify(word, str) + ") returned " + result
                        + ", expected -1\n";
                    UErrorCode status = U_ZERO_ERROR;
                    CollationKey k1, k2;
                    coll->getCollationKey(lastWord, k1, status);
                    coll->getCollationKey(word, k2, status);
                    if (U_FAILURE(status)) {
                        errln((UnicodeString)"Fail: getCollationKey returned " + u_errorName(status));
                        return;
                    }
                    msg.append("key1: ").append(prettify(k1, str)).append("\n");
                    msg.append("key2: ").append(prettify(k2, str));
                    errln(msg);
                }
            }
        }
        lastWord = word;
    }

    if (failed != 0) {
        if (failed > MAX_FAILURES_TO_SHOW) {
            errln((UnicodeString)"Too many failures; only the first " +
                  MAX_FAILURES_TO_SHOW + " failures were shown");
        }
        errln((UnicodeString)"Summary: " + failed + " of " + (line - 1) +
              " comparisons failed");
    }

    logln((UnicodeString)"Words checked: " + wordCount);
    T_FileStream_close(in);
}

/**
 * Odd corner conditions taken from "How to Sort Thai Without Rewriting Sort",
 * by Doug Cooper, http://seasrc.th.net/paper/thaisort.zip
 */
void CollationThaiTest::TestCornerCases(void) {
    const char* TESTS[] = {
        // Shorter words precede longer
        "\\u0e01",                               "<",    "\\u0e01\\u0e01",

        // Tone marks are considered after letters (i.e. are primary ignorable)
        "\\u0e01\\u0e32",                        "<",    "\\u0e01\\u0e49\\u0e32",

        // ditto for other over-marks
        "\\u0e01\\u0e32",                        "<",    "\\u0e01\\u0e32\\u0e4c",

        // commonly used mark-in-context order.
        // In effect, marks are sorted after each syllable.
        "\\u0e01\\u0e32\\u0e01\\u0e49\\u0e32",   "<",    "\\u0e01\\u0e48\\u0e32\\u0e01\\u0e49\\u0e32",

        // Hyphens and other punctuation follow whitespace but come before letters
        "\\u0e01\\u0e32",                        "<",    "\\u0e01\\u0e32-",
        "\\u0e01\\u0e32-",                       "<",    "\\u0e01\\u0e32\\u0e01\\u0e32",

        // Doubler follows an indentical word without the doubler
        "\\u0e01\\u0e32",                        "<",    "\\u0e01\\u0e32\\u0e46",
        "\\u0e01\\u0e32\\u0e46",                 "<",    "\\u0e01\\u0e32\\u0e01\\u0e32",


        // \\u0e45 after either \\u0e24 or \\u0e26 is treated as a single
        // combining character, similar to "c < ch" in traditional spanish.
        // TODO: beef up this case
        "\\u0e24\\u0e29\\u0e35",                 "<",    "\\u0e24\\u0e45\\u0e29\\u0e35",
        "\\u0e26\\u0e29\\u0e35",                 "<",    "\\u0e26\\u0e45\\u0e29\\u0e35",

        // Vowels reorder, should compare \\u0e2d and \\u0e34
        "\\u0e40\\u0e01\\u0e2d",                 "<",    "\\u0e40\\u0e01\\u0e34",

        // Tones are compared after the rest of the word (e.g. primary ignorable)
        "\\u0e01\\u0e32\\u0e01\\u0e48\\u0e32",   "<",    "\\u0e01\\u0e49\\u0e32\\u0e01\\u0e32",

        // Periods are ignored entirely
        "\\u0e01.\\u0e01.",                      "<",    "\\u0e01\\u0e32",
    };
    const int32_t TESTS_length = (int32_t)(sizeof(TESTS)/sizeof(TESTS[0]));

    if (coll == 0) {
        errln("Error: could not construct Thai collator");
        return;
    }
    compareArray(*coll, TESTS, TESTS_length);
}

//------------------------------------------------------------------------
// Internal utilities
//------------------------------------------------------------------------

void CollationThaiTest::compareArray(Collator& c, const char* tests[],
                                     int32_t testsLength) {
    UErrorCode status = U_ZERO_ERROR;
    for (int32_t i = 0; i < testsLength; i += 3) {

        Collator::EComparisonResult expect;
        if (tests[i+1][0] == '<') {
          expect = Collator::LESS;
        } else if (tests[i+1][0] == '>') {
          expect = Collator::GREATER;
        } else if (tests[i+1][0] == '=') {
          expect = Collator::EQUAL;
        } else {
            // expect = Integer.decode(tests[i+1]).intValue();
            errln((UnicodeString)"Error: unknown operator " + tests[i+1]);
            return;
        }

        UnicodeString s1, s2;
        parseChars(s1, tests[i]);
        parseChars(s2, tests[i+2]);

        doTest(&c, s1, s2, expect);
#if 0
        int32_t result = c.compare(s1, s2);
        if (sign(result) != sign(expect))
        {
            UnicodeString t1, t2;
            errln(UnicodeString("") +
                  i/3 + ": compare(" + IntlTest::prettify(s1, t1)
                  + " , " + IntlTest::prettify(s2, t2)
                  + ") got " + result + "; expected " + expect);

            CollationKey k1, k2;
            c.getCollationKey(s1, k1, status);
            c.getCollationKey(s2, k2, status);
            if (U_FAILURE(status)) {
                errln((UnicodeString)"Fail: getCollationKey returned " + u_errorName(status));
                return;
            }
            errln((UnicodeString)"  key1: " + prettify(k1, t1) );
            errln((UnicodeString)"  key2: " + prettify(k2, t2) );
        }
        else
        {
            // Collator.compare worked OK; now try the collation keys
            CollationKey k1, k2;
            c.getCollationKey(s1, k1, status);
            c.getCollationKey(s2, k2, status);
            if (U_FAILURE(status)) {
                errln((UnicodeString)"Fail: getCollationKey returned " + u_errorName(status));
                return;
            }

            result = k1.compareTo(k2);
            if (sign(result) != sign(expect)) {
                UnicodeString t1, t2;
                errln(UnicodeString("") +
                      i/3 + ": key(" + IntlTest::prettify(s1, t1)
                      + ").compareTo(key(" + IntlTest::prettify(s2, t2)
                      + ")) got " + result + "; expected " + expect);
                
                errln((UnicodeString)"  " + prettify(k1, t1) + " vs. " + prettify(k2, t2));
            }
        }
#endif
    }
}

int8_t CollationThaiTest::sign(int32_t i) {
    if (i < 0) return -1;
    if (i > 0) return 1;
    return 0;
}

/**
 * Set a UnicodeString corresponding to the given string.  Use
 * UnicodeString and the default converter, unless we see the sequence
 * "\\u", in which case we interpret the subsequent escape.
 */
UnicodeString& CollationThaiTest::parseChars(UnicodeString& result,
                                             const char* chars) {
    return result = CharsToUnicodeString(chars);
}

UCollator *thaiColl = NULL;

U_CDECL_BEGIN
static int U_CALLCONV
StrCmp(const void *p1, const void *p2) {
  return ucol_strcoll(thaiColl, *(UChar **) p1, -1,  *(UChar **)p2, -1);
}
U_CDECL_END


#define LINES 6

void CollationThaiTest::TestInvalidThai(void) {
  const char *tests[LINES] = {
    "\\u0E44\\u0E01\\u0E44\\u0E01",
    "\\u0E44\\u0E01\\u0E01\\u0E44",
    "\\u0E01\\u0E44\\u0E01\\u0E44",
    "\\u0E01\\u0E01\\u0E44\\u0E44",
    "\\u0E44\\u0E44\\u0E01\\u0E01",
    "\\u0E01\\u0E44\\u0E44\\u0E01",
  };

  UChar strings[LINES][20];

  UChar *toSort[LINES];

  int32_t i = 0, j = 0, len = 0;

  UErrorCode coll_status = U_ZERO_ERROR;
  UnicodeString iteratorText;

  thaiColl = ucol_open ("th_TH", &coll_status);
  if (U_FAILURE(coll_status)) {
    errln("Error opening Thai collator: %s", u_errorName(coll_status));
    return;
  }

  CollationElementIterator* c = ((RuleBasedCollator *)coll)->createCollationElementIterator( iteratorText );

  for(i = 0; i < sizeof(tests)/sizeof(tests[0]); i++) {
    len = u_unescape(tests[i], strings[i], 20);
    strings[i][len] = 0;
    toSort[i] = strings[i];
  }

  qsort (toSort, LINES, sizeof (UChar *), StrCmp);

  for (i=0; i < LINES; i++)
  {
    logln("%i", i);
	  for (j=i+1; j < LINES; j++) {
		  if (ucol_strcoll (thaiColl, toSort[i], -1, toSort[j], -1) == UCOL_GREATER)
		  {
			  // inconsistency ordering found!
            errln("Inconsistent ordering between strings %i and %i", i, j);
		  }
	  }
      iteratorText.setTo(toSort[i]);
      c->setText(iteratorText, coll_status);
      backAndForth(*c);
  }

  
  ucol_close(thaiColl);
  delete c;
}

void CollationThaiTest::TestReordering(void) {
  const char *tests[] = { 
                          "\\u0E41c\\u0301",       "=", "\\u0E41\\u0107", // composition
                          "\\u0E41\\uD835\\uDFCE", "<", "\\u0E41\\uD835\\uDFCF", // supplementaries
                          "\\u0E41\\uD834\\uDD5F", "=", "\\u0E41\\uD834\\uDD58\\uD834\\uDD65", // supplementary composition decomps to supplementary
                          "\\u0E41\\uD87E\\uDC02", "=", "\\u0E41\\u4E41", // supplementary composition decomps to BMP
                          "\\u0E41\\u0301",        "=", "\\u0E41\\u0301", // unsafe (just checking backwards iteration)
                          "\\u0E41\\u0301\\u0316", "=", "\\u0E41\\u0316\\u0301",
                          "\\u0e24\\u0e41",        "=", "\\u0e41\\u0e24", // exiting contraction bug
                          "\\u0e3f\\u0e3f\\u0e24\\u0e41", "=", "\\u0e3f\\u0e3f\\u0e41\\u0e24",

                          "abc\\u0E41c\\u0301",       "=", "abc\\u0E41\\u0107", // composition
                          "abc\\u0E41\\uD834\\uDC00", "<", "abc\\u0E41\\uD834\\uDC01", // supplementaries
                          "abc\\u0E41\\uD834\\uDD5F", "=", "abc\\u0E41\\uD834\\uDD58\\uD834\\uDD65", // supplementary composition decomps to supplementary
                          "abc\\u0E41\\uD87E\\uDC02", "=", "abc\\u0E41\\u4E41", // supplementary composition decomps to BMP
                          "abc\\u0E41\\u0301",        "=", "abc\\u0E41\\u0301", // unsafe (just checking backwards iteration)
                          "abc\\u0E41\\u0301\\u0316", "=", "abc\\u0E41\\u0316\\u0301",

                          "\\u0E41c\\u0301abc",       "=", "\\u0E41\\u0107abc", // composition
                          "\\u0E41\\uD834\\uDC00abc", "<", "\\u0E41\\uD834\\uDC01abc", // supplementaries
                          "\\u0E41\\uD834\\uDD5Fabc", "=", "\\u0E41\\uD834\\uDD58\\uD834\\uDD65abc", // supplementary composition decomps to supplementary
                          "\\u0E41\\uD87E\\uDC02abc", "=", "\\u0E41\\u4E41abc", // supplementary composition decomps to BMP
                          "\\u0E41\\u0301abc",        "=", "\\u0E41\\u0301abc", // unsafe (just checking backwards iteration)
                          "\\u0E41\\u0301\\u0316abc", "=", "\\u0E41\\u0316\\u0301abc",

                          "abc\\u0E41c\\u0301abc",       "=", "abc\\u0E41\\u0107abc", // composition
                          "abc\\u0E41\\uD834\\uDC00abc", "<", "abc\\u0E41\\uD834\\uDC01abc", // supplementaries
                          "abc\\u0E41\\uD834\\uDD5Fabc", "=", "abc\\u0E41\\uD834\\uDD58\\uD834\\uDD65abc", // supplementary composition decomps to supplementary
                          "abc\\u0E41\\uD87E\\uDC02abc", "=", "abc\\u0E41\\u4E41abc", // supplementary composition decomps to BMP
                          "abc\\u0E41\\u0301abc",        "=", "abc\\u0E41\\u0301abc", // unsafe (just checking backwards iteration)
                          "abc\\u0E41\\u0301\\u0316abc", "=", "abc\\u0E41\\u0316\\u0301abc",
                        };

  compareArray(*coll, tests, sizeof(tests)/sizeof(tests[0]));
 
  const char *rule = "& c < ab";
  const char *testcontraction[] = { "\\u0E41ab", "<", "\\u0E41c"};
  UnicodeString rules;
  UErrorCode status = U_ZERO_ERROR;
  parseChars(rules, rule);
  RuleBasedCollator *rcoll = new RuleBasedCollator(rules, status);
  if(U_SUCCESS(status)) {
    compareArray(*rcoll, testcontraction, 3);
    delete rcoll;
  } else {
    errln("Couldn't instantiate collator from rules");
  }

}


#endif /* #if !UCONFIG_NO_COLLATION */
