/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
#include "itrbnf.h"

#include "unicode/tblcoll.h"
#include "unicode/coleitr.h"
#include "unicode/ures.h"

// import com.ibm.text.RuleBasedNumberFormat;
// import com.ibm.test.TestFmwk;

// import java.util.Locale;
// import java.text.NumberFormat;

// current macro not in icu1.8.1
#define TESTCASE(id,test)             \
    case id:                          \
        name = #test;                 \
        if (exec) {                   \
            logln(#test "---");       \
            logln((UnicodeString)""); \
            test();                   \
        }                             \
        break

void IntlTestRBNF::runIndexedTest(int32_t index, UBool exec, const char* &name, char* /*par*/)
{
    if (exec) logln("TestSuite RuleBasedNumberFormat");
    switch (index) {
      TESTCASE(0, TestEnglishSpellout);
      TESTCASE(1, TestOrdinalAbbreviations);
      TESTCASE(2, TestDurations);
      TESTCASE(3, TestSpanishSpellout);
      TESTCASE(4, TestFrenchSpellout);
      TESTCASE(5, TestSwissFrenchSpellout);
      TESTCASE(6, TestItalianSpellout);
      TESTCASE(7, TestGermanSpellout);
      TESTCASE(8, TestThaiSpellout);
      TESTCASE(9, TestAPI);
      TESTCASE(10, TestFractionalRuleSet);
    default:
      name = "";
      break;
    }
}

void 
IntlTestRBNF::TestAPI() {
  // This test goes through the APIs that were not tested before. 
  // These tests are too small to have separate test classes/functions

  UErrorCode status = U_ZERO_ERROR;
  RuleBasedNumberFormat* formatter
      = new RuleBasedNumberFormat(URBNF_SPELLOUT, Locale::US, status);

  logln("RBNF API test starting");
  // test clone
  {
    logln("Testing Clone");
    RuleBasedNumberFormat* rbnfClone = (RuleBasedNumberFormat *)formatter->clone();
    if(rbnfClone != NULL) {
      if(!(*rbnfClone == *formatter)) {
        errln("Clone should be semantically equivalent to the original!");
      }
      delete rbnfClone;
    } else {
      errln("Cloning failed!");
    }
  }

  // test assignment
  {
    logln("Testing assignment operator");
    RuleBasedNumberFormat assignResult(URBNF_SPELLOUT, Locale("es", "ES", ""), status);
    assignResult = *formatter;
    if(!(assignResult == *formatter)) {
      errln("Assignment result should be semantically equivalent to the original!");
    }
  }

  // test rule constructor
  {
    logln("Testing rule constructor");
    UResourceBundle *en = ures_open(NULL, "en", &status);
    if(U_FAILURE(status)) {
      errln("Unable to access resource bundle with data!");
    } else {
      int32_t ruleLen = 0;
      const UChar *spelloutRules = ures_getStringByKey(en, "SpelloutRules", &ruleLen, &status);
      if(U_FAILURE(status) || ruleLen == 0 || spelloutRules == NULL) {
        errln("Unable to access the rules string!");
      } else {
        UParseError perror;
        RuleBasedNumberFormat ruleCtorResult(spelloutRules, Locale::US, perror, status);
        if(!(ruleCtorResult == *formatter)) {
          errln("Formatter constructed from the original rules should be semantically equivalent to the original!");
        }
      }
      ures_close(en);
    }
  }

  // test getRules
  {
    logln("Testing getRules function");
    UnicodeString rules = formatter->getRules();
    UParseError perror;
    RuleBasedNumberFormat fromRulesResult(rules, Locale::US, perror, status);

    if(!(fromRulesResult == *formatter)) {
      errln("Formatter constructed from rules obtained by getRules should be semantically equivalent to the original!");
    }
  }


  {
    logln("Testing copy constructor");
    RuleBasedNumberFormat copyCtorResult(*formatter);
    if(!(copyCtorResult == *formatter)) {
      errln("Copy constructor result result should be semantically equivalent to the original!");
    }
  }

  // test ruleset names
  {
    logln("Testing getNumberOfRuleSetNames, getRuleSetName and format using rule set names");
    int32_t noOfRuleSetNames = formatter->getNumberOfRuleSetNames();
    if(noOfRuleSetNames == 0) {
      errln("Number of rule set names should be more than zero");
    }
    UnicodeString ruleSetName;
    int32_t i = 0;
    int32_t intFormatNum = 34567;
    double doubleFormatNum = 893411.234;
    logln("number of rule set names is %i", noOfRuleSetNames);
    for(i = 0; i < noOfRuleSetNames; i++) {
      FieldPosition pos1, pos2;
      UnicodeString intFormatResult, doubleFormatResult; 
      Formattable intParseResult, doubleParseResult;

      ruleSetName = formatter->getRuleSetName(i);
      log("Rule set name %i is ", i);
      log(ruleSetName);
      logln(". Format results are: ");
      intFormatResult = formatter->format(intFormatNum, ruleSetName, intFormatResult, pos1, status);
      doubleFormatResult = formatter->format(doubleFormatNum, ruleSetName, doubleFormatResult, pos2, status);
      if(U_FAILURE(status)) {
        errln("Format using a rule set failed");
        break;
      }
      logln(intFormatResult);
      logln(doubleFormatResult);
      formatter->setLenient(TRUE);
      formatter->parse(intFormatResult, intParseResult, status);
      formatter->parse(doubleFormatResult, doubleParseResult, status);

      logln("Parse results for lenient = TRUE, %i, %f", intParseResult.getLong(), doubleParseResult.getDouble());

      formatter->setLenient(FALSE);
      formatter->parse(intFormatResult, intParseResult, status);
      formatter->parse(doubleFormatResult, doubleParseResult, status);

      logln("Parse results for lenient = FALSE, %i, %f", intParseResult.getLong(), doubleParseResult.getDouble());

      if(U_FAILURE(status)) {
        errln("Error during parsing");
      }

      intFormatResult = formatter->format(intFormatNum, "BLABLA", intFormatResult, pos1, status);
      if(U_SUCCESS(status)) {
        errln("Using invalid rule set name should have failed");
        break;
      }
      status = U_ZERO_ERROR;
      doubleFormatResult = formatter->format(doubleFormatNum, "TRUC", doubleFormatResult, pos2, status);
      if(U_SUCCESS(status)) {
        errln("Using invalid rule set name should have failed");
        break;
      }
      status = U_ZERO_ERROR;
    }   
    status = U_ZERO_ERROR;
  }

  // clean up
  logln("Cleaning up");
  delete formatter;
}

void IntlTestRBNF::TestFractionalRuleSet()
{
    UnicodeString fracRules(
        "%main:\n"
               // this rule formats the number if it's 1 or more.  It formats
               // the integral part using a DecimalFormat ("#,##0" puts
               // thousands separators in the right places) and the fractional
               // part using %%frac.  If there is no fractional part, it
               // just shows the integral part.
        "    x.0: <#,##0<[ >%%frac>];\n"
               // this rule formats the number if it's between 0 and 1.  It
               // shows only the fractional part (0.5 shows up as "1/2," not
               // "0 1/2")
        "    0.x: >%%frac>;\n"
        // the fraction rule set.  This works the same way as the one in the
        // preceding example: We multiply the fractional part of the number
        // being formatted by each rule's base value and use the rule that
        // produces the result closest to 0 (or the first rule that produces 0).
        // Since we only provide rules for the numbers from 2 to 10, we know
        // we'll get a fraction with a denominator between 2 and 10.
        // "<0<" causes the numerator of the fraction to be formatted
        // using numerals
        "%%frac:\n"
        "    2: 1/2;\n"
        "    3: <0</3;\n"
        "    4: <0</4;\n"
        "    5: <0</5;\n"
        "    6: <0</6;\n"
        "    7: <0</7;\n"
        "    8: <0</8;\n"
        "    9: <0</9;\n"
        "   10: <0</10;\n");

    UErrorCode status = U_ZERO_ERROR;
    UParseError perror;
    RuleBasedNumberFormat formatter(fracRules, Locale::ENGLISH, perror, status);
    if (U_FAILURE(status)) {
        errln("FAIL: could not construct formatter");
    } else {
        static const char* testData[][2] = {
			{ "0", "0" },
            { ".1", "1/10" },
			{ ".11", "1/9" },
            { ".125", "1/8" },
			{ ".1428", "1/7" },
			{ ".1667", "1/6" },
			{ ".2", "1/5" },
			{ ".25", "1/4" },
            { ".333", "1/3" },
            { ".5", "1/2" },
			{ "1.1", "1 1/10" },
			{ "2.11", "2 1/9" },
            { "3.125", "3 1/8" },
			{ "4.1428", "4 1/7" },
			{ "5.1667", "5 1/6" },
			{ "6.2", "6 1/5" },
			{ "7.25", "7 1/4" },
            { "8.333", "8 1/3" },
            { "9.5", "9 1/2" },
			{ ".2222", "2/9" },
			{ ".4444", "4/9" },
			{ ".5555", "5/9" },
			{ "1.2856", "1 2/7" },
			{ NULL, NULL }
		};
       doTest(&formatter, testData, FALSE); // exact values aren't parsable from fractions
	}
}

void 
IntlTestRBNF::TestEnglishSpellout() 
{
    UErrorCode status = U_ZERO_ERROR;
    RuleBasedNumberFormat* formatter
        = new RuleBasedNumberFormat(URBNF_SPELLOUT, Locale::US, status);

    if (U_FAILURE(status)) {
        errln("FAIL: could not construct formatter");
    } else {
        static const char* testData[][2] = {
            { "1", "one" },
            { "2", "two" },
            { "15", "fifteen" },
            { "20", "twenty" },
            { "23", "twenty-three" },
            { "73", "seventy-three" },
            { "88", "eighty-eight" },
            { "100", "one hundred" },
            { "106", "one hundred and six" },
            { "127", "one hundred and twenty-seven" },
            { "200", "two hundred" },
            { "579", "five hundred and seventy-nine" },
            { "1,000", "one thousand" },
            { "2,000", "two thousand" },
            { "3,004", "three thousand and four" },
            { "4,567", "four thousand five hundred and sixty-seven" },
            { "15,943", "fifteen thousand nine hundred and forty-three" },
            { "2,345,678", "two million, three hundred and forty-five thousand, six hundred and seventy-eight" },
            { "-36", "minus thirty-six" },
            { "234.567", "two hundred and thirty-four point five six seven" },
            { NULL, NULL}
        };

        doTest(formatter, testData, TRUE);

        formatter->setLenient(TRUE);
        static const char* lpTestData[][2] = {
			{ "fifty-7", "57" },
			{ " fifty-7", "57" },
			{ "  fifty-7", "57" },
            { "2 thousand six    HUNDRED fifty-7", "2,657" },
            { "fifteen hundred and zero", "1,500" },
            { "FOurhundred     thiRTY six", "436" },
            { NULL, NULL}
        };
        doLenientParseTest(formatter, lpTestData);
    }
    delete formatter;
}

void 
IntlTestRBNF::TestOrdinalAbbreviations() 
{
    UErrorCode status = U_ZERO_ERROR;
    RuleBasedNumberFormat* formatter
        = new RuleBasedNumberFormat(URBNF_ORDINAL, Locale::US, status);
    
    if (U_FAILURE(status)) {
        errln("FAIL: could not construct formatter");
    } else {
        static const char* testData[][2] = {
            { "1", "1st" },
            { "2", "2nd" },
            { "3", "3rd" },
            { "4", "4th" },
            { "7", "7th" },
            { "10", "10th" },
            { "11", "11th" },
            { "13", "13th" },
            { "20", "20th" },
            { "21", "21st" },
            { "22", "22nd" },
            { "23", "23rd" },
            { "24", "24th" },
            { "33", "33rd" },
            { "102", "102nd" },
            { "312", "312th" },
            { "12,345", "12,345th" },
            { NULL, NULL}
        };
        
        doTest(formatter, testData, FALSE);
    }
    delete formatter;
}

void 
IntlTestRBNF::TestDurations() 
{
    UErrorCode status = U_ZERO_ERROR;
    RuleBasedNumberFormat* formatter
        = new RuleBasedNumberFormat(URBNF_DURATION, Locale::US, status);
    
    if (U_FAILURE(status)) {
        errln("FAIL: could not construct formatter");
    } else {
        static const char* testData[][2] = {
            { "3,600", "1:00:00" },		//move me and I fail
            { "0", "0 sec." },
            { "1", "1 sec." },
            { "24", "24 sec." },
            { "60", "1:00" },
            { "73", "1:13" },
            { "145", "2:25" },
            { "666", "11:06" },
            //            { "3,600", "1:00:00" },
            { "3,740", "1:02:20" },
            { "10,293", "2:51:33" },
            { NULL, NULL}
        };
        
        doTest(formatter, testData, TRUE);
        
        formatter->setLenient(TRUE);
        static const char* lpTestData[][2] = {
            { "2-51-33", "10,293" },
            { NULL, NULL}
        };
        doLenientParseTest(formatter, lpTestData);
    }
    delete formatter;
}

void 
IntlTestRBNF::TestSpanishSpellout() 
{
    UErrorCode status = U_ZERO_ERROR;
    RuleBasedNumberFormat* formatter
        = new RuleBasedNumberFormat(URBNF_SPELLOUT, Locale("es", "ES", ""), status);
    
    if (U_FAILURE(status)) {
        errln("FAIL: could not construct formatter");
    } else {
        static const char* testData[][2] = {
            { "1", "uno" },
            { "6", "seis" },
            { "16", "diecis\\u00e9is" },
            { "20", "veinte" },
            { "24", "veinticuatro" },
            { "26", "veintis\\u00e9is" },
            { "73", "setenta y tres" },
            { "88", "ochenta y ocho" },
            { "100", "cien" },
            { "106", "ciento seis" },
            { "127", "ciento veintisiete" },
            { "200", "doscientos" },
            { "579", "quinientos setenta y nueve" },
            { "1,000", "mil" },
            { "2,000", "dos mil" },
            { "3,004", "tres mil cuatro" },
            { "4,567", "cuatro mil quinientos sesenta y siete" },
            { "15,943", "quince mil novecientos cuarenta y tres" },
            { "2,345,678", "dos mill\\u00f3n trescientos cuarenta y cinco mil seiscientos setenta y ocho"},
            { "-36", "menos treinta y seis" },
            { "234.567", "doscientos treinta y cuatro punto cinco seis siete" },
            { NULL, NULL}
        };
        
        doTest(formatter, testData, TRUE);
    }
    delete formatter;
}

void 
IntlTestRBNF::TestFrenchSpellout() 
{
    UErrorCode status = U_ZERO_ERROR;
    RuleBasedNumberFormat* formatter
        = new RuleBasedNumberFormat(URBNF_SPELLOUT, Locale::FRANCE, status);
    
    if (U_FAILURE(status)) {
        errln("FAIL: could not construct formatter");
    } else {
        static const char* testData[][2] = {
            { "1", "un" },
            { "15", "quinze" },
            { "20", "vingt" },
            { "21", "vingt-et-un" },
            { "23", "vingt-trois" },
            { "62", "soixante-deux" },
            { "70", "soixante-dix" },
            { "71", "soixante et onze" },
            { "73", "soixante-treize" },
            { "80", "quatre-vingts" },
            { "88", "quatre-vingt-huit" },
            { "100", "cent" },
            { "106", "cent six" },
            { "127", "cent vingt-sept" },
            { "200", "deux cents" },
            { "579", "cinq cents soixante-dix-neuf" },
            { "1,000", "mille" },
            { "1,123", "onze cents vingt-trois" },
            { "1,594", "mille cinq cents quatre-vingt-quatorze" },
            { "2,000", "deux mille" },
            { "3,004", "trois mille quatre" },
            { "4,567", "quatre mille cinq cents soixante-sept" },
            { "15,943", "quinze mille neuf cents quarante-trois" },
            { "2,345,678", "deux million trois cents quarante-cinq mille six cents soixante-dix-huit" },
            { "-36", "moins trente-six" },
            { "234.567", "deux cents trente-quatre virgule cinq six sept" },
            { NULL, NULL}
        };
        
        doTest(formatter, testData, TRUE);
        
        formatter->setLenient(TRUE);
        static const char* lpTestData[][2] = {
            { "trente-un", "31" },
            { "un cents quatre vingt dix huit", "198" },
            { NULL, NULL}
        };
        doLenientParseTest(formatter, lpTestData);
    }
    delete formatter;
}

void 
IntlTestRBNF::TestSwissFrenchSpellout() 
{
    UErrorCode status = U_ZERO_ERROR;
    RuleBasedNumberFormat* formatter
        = new RuleBasedNumberFormat(URBNF_SPELLOUT, Locale("fr", "CH", ""), status);
    
    if (U_FAILURE(status)) {
        errln("FAIL: could not construct formatter");
    } else {
        static const char* testData[][2] = {
            { "1", "un" },
            { "15", "quinze" },
            { "20", "vingt" },
            { "21", "vingt-et-un" },
            { "23", "vingt-trois" },
            { "62", "soixante-deux" },
            { "70", "septante" },
            { "71", "septante-et-un" },
            { "73", "septante-trois" },
            { "80", "octante" },
            { "88", "octante-huit" },
            { "100", "cent" },
            { "106", "cent six" },
            { "127", "cent vingt-sept" },
            { "200", "deux cents" },
            { "579", "cinq cents septante-neuf" },
            { "1,000", "mille" },
            { "1,123", "onze cents vingt-trois" },
            { "1,594", "mille cinq cents nonante-quatre" },
            { "2,000", "deux mille" },
            { "3,004", "trois mille quatre" },
            { "4,567", "quatre mille cinq cents soixante-sept" },
            { "15,943", "quinze mille neuf cents quarante-trois" },
            { "2,345,678", "deux million trois cents quarante-cinq mille six cents septante-huit" },
            { "-36", "moins trente-six" },
            { "234.567", "deux cents trente-quatre virgule cinq six sept" },
            { NULL, NULL}
        };
        
        doTest(formatter, testData, TRUE);
    }
    delete formatter;
}

void 
IntlTestRBNF::TestItalianSpellout() 
{
    UErrorCode status = U_ZERO_ERROR;
    RuleBasedNumberFormat* formatter
        = new RuleBasedNumberFormat(URBNF_SPELLOUT, Locale::ITALIAN, status);
    
    if (U_FAILURE(status)) {
        errln("FAIL: could not construct formatter");
    } else {
        static const char* testData[][2] = {
            { "1", "uno" },
            { "15", "quindici" },
            { "20", "venti" },
            { "23", "ventitre" },
            { "73", "settantatre" },
            { "88", "ottantotto" },
            { "100", "cento" },
            { "106", "centosei" },
            { "108", "centotto" },
            { "127", "centoventisette" },
            { "181", "centottantuno" },
            { "200", "duecento" },
            { "579", "cinquecentosettantanove" },
            { "1,000", "mille" },
            { "2,000", "duemila" },
            { "3,004", "tremilaquattro" },
            { "4,567", "quattromilacinquecentosessantasette" },
            { "15,943", "quindicimilanovecentoquarantatre" },
            { "-36", "meno trentisei" },
            { "234.567", "duecentotrentiquattro virgola cinque sei sette" },
            { NULL, NULL}
        };
        
        doTest(formatter, testData, TRUE);
    }
    delete formatter;
}

void 
IntlTestRBNF::TestGermanSpellout() 
{
    UErrorCode status = U_ZERO_ERROR;
    RuleBasedNumberFormat* formatter
        = new RuleBasedNumberFormat(URBNF_SPELLOUT, Locale::GERMANY, status);
    
    if (U_FAILURE(status)) {
        errln("FAIL: could not construct formatter");
    } else {
        static const char* testData[][2] = {
            { "1", "eins" },
            { "15", "f\\u00fcnfzehn" },
            { "20", "zwanzig" },
            { "23", "dreiundzwanzig" },
            { "73", "dreiundsiebzig" },
            { "88", "achtundachtzig" },
            { "100", "hundert" },
            { "106", "hundertsechs" },
            { "127", "hundertsiebenundzwanzig" },
            { "200", "zweihundert" },
            { "579", "f\\u00fcnfhundertneunundsiebzig" },
            { "1,000", "tausend" },
            { "2,000", "zweitausend" },
            { "3,004", "dreitausendvier" },
            { "4,567", "viertausendf\\u00fcnfhundertsiebenundsechzig" },
            { "15,943", "f\\u00fcnfzehntausendneunhundertdreiundvierzig" },
            { "2,345,678", "zwei Millionen dreihundertf\\u00fcnfundvierzigtausendsechshundertachtundsiebzig" },
            { NULL, NULL}
        };
        
        doTest(formatter, testData, TRUE);
        
        formatter->setLenient(TRUE);
        static const char* lpTestData[][2] = {
            { "ein Tausend sechs Hundert fuenfunddreissig", "1,635" },
            { NULL, NULL}
        };
        doLenientParseTest(formatter, lpTestData);
    }
    delete formatter;
}

void 
IntlTestRBNF::TestThaiSpellout() 
{
    UErrorCode status = U_ZERO_ERROR;
    RuleBasedNumberFormat* formatter
        = new RuleBasedNumberFormat(URBNF_SPELLOUT, Locale("th"), status);
    
    if (U_FAILURE(status)) {
        errln("FAIL: could not construct formatter");
    } else {
        static const char* testData[][2] = {
            { "0", "\\u0e28\\u0e39\\u0e19\\u0e22\\u0e4c" },
            { "1", "\\u0e2b\\u0e19\\u0e36\\u0e48\\u0e07" },
            { "10", "\\u0e2a\\u0e34\\u0e1a" },
            { "11", "\\u0e2a\\u0e34\\u0e1a\\u0e40\\u0e2d\\u0e47\\u0e14" },
            { "21", "\\u0e22\\u0e35\\u0e48\\u0e2a\\u0e34\\u0e1a\\u0e40\\u0e2d\\u0e47\\u0e14" },
            { "101", "\\u0e2b\\u0e19\\u0e36\\u0e48\\u0e07\\u0e23\\u0e49\\u0e2d\\u0e22\\u0e2b\\u0e19\\u0e36\\u0e48\\u0e07" },
            { "1.234", "\\u0e2b\\u0e19\\u0e36\\u0e48\\u0e07\\u0e08\\u0e38\\u0e14\\u0e2a\\u0e2d\\u0e07\\u0e2a\\u0e32\\u0e21\\u0e2a\\u0e35\\u0e48" },
            { NULL, NULL}
        };
        
        doTest(formatter, testData, TRUE);
    }
    delete formatter;
}

void 
IntlTestRBNF::doTest(RuleBasedNumberFormat* formatter, const char* testData[][2], UBool testParsing) 
{
  // man, error reporting would be easier with printf-style syntax for unicode string and formattable

    UErrorCode status = U_ZERO_ERROR;
    NumberFormat* decFmt = NumberFormat::createInstance(Locale::US, status);
    if (U_FAILURE(status)) {
        errln("FAIL: could not create NumberFormat");
    } else {
        for (int i = 0; testData[i][0]; ++i) {
            const char* numString = testData[i][0];
            const char* expectedWords = testData[i][1];

            Formattable expectedNumber;
            decFmt->parse(numString, expectedNumber, status);
            if (U_FAILURE(status)) {
                errln("FAIL: decFmt could not parse %s", numString);
                break;
            } else {
                UnicodeString actualString;
                FieldPosition pos;
                formatter->format(expectedNumber, actualString/* , pos*/, status);
                if (U_FAILURE(status)) {
                    UnicodeString msg = "Fail: formatter could not format ";
                    decFmt->format(expectedNumber, msg, status);
                    errln(msg);
                    break;
                } else {
                    UnicodeString expectedString = UnicodeString(expectedWords).unescape();
                    if (actualString != expectedString) {
                        UnicodeString msg = "FAIL: check failed for ";
                        decFmt->format(expectedNumber, msg, status);
                        msg.append(", expected ");
                        msg.append(expectedString);
                        msg.append(" but got ");
                        msg.append(actualString);
                        errln(msg);
                        break;
                    } else if (testParsing) {
                        Formattable parsedNumber;
                        formatter->parse(actualString, parsedNumber, status);
                        if (U_FAILURE(status)) {
                            UnicodeString msg = "FAIL: formatter could not parse ";
                            msg.append(actualString);
                            msg.append(" status code: " );
                            char buffer[32];
                            sprintf(buffer, "0x%x", status);
                            msg.append(buffer);
                            errln(msg);
                            break;
                        } else {
                            if (parsedNumber != expectedNumber) {
                                UnicodeString msg = "FAIL: parse failed for ";
                                msg.append(actualString);
                                msg.append(", expected ");
                                decFmt->format(expectedNumber, msg, status);
                                msg.append(", but got ");
                                decFmt->format(parsedNumber, msg, status);
                                errln(msg);
                                break;
                            }
                        }
                    }
                }
            }
        }
        delete decFmt;
    }
}

void 
IntlTestRBNF::doLenientParseTest(RuleBasedNumberFormat* formatter, const char* testData[][2]) 
{
    UErrorCode status = U_ZERO_ERROR;
    NumberFormat* decFmt = NumberFormat::createInstance(Locale::US, status);
    if (U_FAILURE(status)) {
        errln("FAIL: could not create NumberFormat");
    } else {
        for (int i = 0; testData[i][0]; ++i) {
            const char* spelledNumber = testData[i][0]; // spelled-out number
            const char* asciiUSNumber = testData[i][1]; // number as ascii digits formatted for US locale
            
            UnicodeString spelledNumberString = UnicodeString(spelledNumber).unescape();
            Formattable actualNumber;
            formatter->parse(spelledNumberString, actualNumber, status);
            if (U_FAILURE(status)) {
                UnicodeString msg = "FAIL: formatter could not parse ";
                msg.append(spelledNumberString);
                errln(msg);
                break;
            } else {
                // I changed the logic of this test somewhat from Java-- instead of comparing the
                // strings, I compare the Formattables.  Hmmm, but the Formattables don't compare,
                // so change it back.

                UnicodeString asciiUSNumberString = asciiUSNumber;
                Formattable expectedNumber;
                decFmt->parse(asciiUSNumberString, expectedNumber, status);
                if (U_FAILURE(status)) {
                    UnicodeString msg = "FAIL: decFmt could not parse ";
                    msg.append(asciiUSNumberString);
                    errln(msg);
                    break;
                } else {
                    UnicodeString actualNumberString;
                    UnicodeString expectedNumberString;
                    decFmt->format(actualNumber, actualNumberString, status);
                    decFmt->format(expectedNumber, expectedNumberString, status);
                    if (actualNumberString != expectedNumberString) {
                        UnicodeString msg = "FAIL: parsing";
                        msg.append(asciiUSNumberString);
                        msg.append("\n");
                        msg.append("  lenient parse failed for ");
                        msg.append(spelledNumberString);
                        msg.append(", expected ");
                        msg.append(expectedNumberString);
                        msg.append(", but got ");
                        msg.append(actualNumberString);
                        errln(msg);
                        break;
                    }
                }
            }
        }
        delete decFmt;
    }
}

