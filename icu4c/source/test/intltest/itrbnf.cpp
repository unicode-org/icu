/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
#include "itrbnf.h"

#include "unicode/tblcoll.h"
#include "unicode/coleitr.h"

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

void IntlTestRBNF::runIndexedTest(int32_t index, UBool exec, const char* &name, char* par)
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
    default:
      name = "";
      break;
    }
}

void 
IntlTestRBNF::TestEnglishSpellout() 
{
#if 0
  // temporary test code
  {
	int32_t result = 0;
    UErrorCode status = U_ZERO_ERROR;
	Collator* temp = Collator::createInstance(Locale::US, status);
    if (U_SUCCESS(status) &&
        temp->getDynamicClassID() == RuleBasedCollator::getStaticClassID()) {
		
        RuleBasedCollator* collator = (RuleBasedCollator*)temp;
        UnicodeString rules(collator->getRules());
		UnicodeString tailoring("&'\\u0000' << ' ' << '-'\n");
		tailoring = tailoring.unescape();
        rules.append(tailoring);
		
        collator = new RuleBasedCollator(rules, status);
        if (U_SUCCESS(status)) {
            collator->setDecomposition(Normalizer::DECOMP);
			
			UnicodeString prefix(" hundred");
			UnicodeString str("hundred-fifty");
			
			CollationElementIterator* strIter = collator->createCollationElementIterator(str);
			CollationElementIterator* prefixIter = collator->createCollationElementIterator(prefix);
						
			// match collation elements between the strings
			int32_t oStr = strIter->next(status);
			int32_t oPrefix = prefixIter->next(status);
			
			while (oPrefix != CollationElementIterator::NULLORDER) {
				// skip over ignorable characters in the target string
				while (CollationElementIterator::primaryOrder(oStr) == 0 
					&& oStr != CollationElementIterator::NULLORDER) {
					oStr = strIter->next(status);
				}
				
				// skip over ignorable characters in the prefix
				while (CollationElementIterator::primaryOrder(oPrefix) == 0 
					&& oPrefix != CollationElementIterator::NULLORDER) {
					oPrefix = prefixIter->next(status);
				}
				
				// if skipping over ignorables brought us to the end
				// of the target string, we didn't match and return 0
				if (oStr == CollationElementIterator::NULLORDER) {
					result = -1;
					break;
				}
				
				// if skipping over ignorables brought to the end of
				// the prefix, we DID match: drop out of the loop
				else if (oPrefix == CollationElementIterator::NULLORDER) {
					break;
				}
				
				// match collation elements from the two strings
				// (considering only primary differences).  If we
				// get a mismatch, dump out and return 0
				if (CollationElementIterator::primaryOrder(oStr) 
					!= CollationElementIterator::primaryOrder(oPrefix)) {
					result = -1;
					break;
					
					// otherwise, advance to the next character in each string
					// and loop (we drop out of the loop when we exhaust
					// collation elements in the prefix)
				} else {
					oStr = strIter->next(status);
					oPrefix = prefixIter->next(status);
				}
			}
			if (result == 0) {
				result = strIter->getOffset();
			}
			delete prefixIter;
			delete strIter;
        }
		delete collator;
	}
	delete temp;

	printf("result: %d\n", result);
  }
#endif

  UErrorCode status = U_ZERO_ERROR;
  RuleBasedNumberFormat* formatter
    = new RuleBasedNumberFormat(URBNF_SPELLOUT, Locale::US, status);

  if (U_FAILURE(status)) {
    errln("FAIL: could not construct formatter");
  } else {
    const char* testData[][2] = {
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
      NULL
    };

    doTest(formatter, testData, TRUE);

    formatter->setLenient(TRUE);
    const char* lpTestData[][2] = {
      { "2 thousand six HUNDRED fifty-7", "2,657" },
      { "fifteen hundred and zero", "1,500" },
      { "FOurhundred     thiRTY six", "436" },
      NULL
    };
    doLenientParseTest(formatter, lpTestData);
  }
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
    const char* testData[][2] = {
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
      NULL
    };

    doTest(formatter, testData, FALSE);
  }
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
    const char* testData[][2] = {
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
      NULL
    };

    doTest(formatter, testData, TRUE);

    formatter->setLenient(TRUE);
    const char* lpTestData[][2] = {
      { "2-51-33", "10,293" },
      NULL
    };
    doLenientParseTest(formatter, lpTestData);
  }
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
    const char* testData[][2] = {
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
      NULL
    };

    doTest(formatter, testData, TRUE);
  }
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
    const char* testData[][2] = {
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
      NULL
    };

    doTest(formatter, testData, TRUE);

    formatter->setLenient(TRUE);
    const char* lpTestData[][2] = {
      { "trente-un", "31" },
      { "un cents quatre vingt dix huit", "198" },
      NULL
    };
    doLenientParseTest(formatter, lpTestData);
  }
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
    const char* testData[][2] = {
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
      NULL
    };

    doTest(formatter, testData, TRUE);
  }
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
    const char* testData[][2] = {
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
      NULL
    };

    doTest(formatter, testData, TRUE);
  }
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
    const char* testData[][2] = {
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
      NULL
    };

    doTest(formatter, testData, TRUE);

    formatter->setLenient(TRUE);
    const char* lpTestData[][2] = {
      { "ein Tausend sechs Hundert fuenfunddreissig", "1,635" },
      NULL
    };
    doLenientParseTest(formatter, lpTestData);
  }
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
    const char* testData[][2] = {
      { "0", "\\u0e28\\u0e39\\u0e19\\u0e22\\u0e4c" },
      { "1", "\\u0e2b\\u0e19\\u0e36\\u0e48\\u0e07" },
      { "10", "\\u0e2a\\u0e34\\u0e1a" },
      { "11", "\\u0e2a\\u0e34\\u0e1a\\u0e40\\u0e2d\\u0e47\\u0e14" },
	  { "21", "\\u0e22\\u0e35\\u0e48\\u0e2a\\u0e34\\u0e1a\\u0e40\\u0e2d\\u0e47\\u0e14" },
      { "101", "\\u0e2b\\u0e19\\u0e36\\u0e48\\u0e07\\u0e23\\u0e49\\u0e2d\\u0e22\\u0e2b\\u0e19\\u0e36\\u0e48\\u0e07" },
      { "1.234", "\\u0e2b\\u0e19\\u0e36\\u0e48\\u0e07\\u0e08\\u0e38\\u0e14\\u0e2a\\u0e2d\\u0e07\\u0e2a\\u0e32\\u0e21\\u0e2a\\u0e35\\u0e48" },
      NULL
    };

    doTest(formatter, testData, TRUE);
  }
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
              sprintf(buffer, "0x%x\0", status);
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

