/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * IntlTestCollator is the medium level test class for everything in the directory "collate".
 */

/***********************************************************************
* Modification history
* Date        Name        Description
* 02/14/2001  synwee      Compare with cintltst and commented away tests 
*                         that are not run.
***********************************************************************/

#include "unicode/utypes.h"
#include "unicode/uchar.h"

#include "cstring.h"
#include "ucol_tok.h"

#include "tscoll.h"

#include "dadrcoll.h"

DataDrivenCollatorTest::DataDrivenCollatorTest() 
: seq(StringCharacterIterator(""))
{
  UErrorCode status = U_ZERO_ERROR;
  TestLog testLog;

  driver = TestDataModule::getTestDataModule("DataDrivenCollationTest", testLog, status);
}

DataDrivenCollatorTest::~DataDrivenCollatorTest() 
{
  delete driver;
}

void DataDrivenCollatorTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* par )
{
  if (exec)
  {
      logln("TestSuite Collator: ");
  }
  UErrorCode status = U_ZERO_ERROR;
  const DataMap *info = NULL;
  TestData *testData = driver->createTestData(index, status);
  if(U_SUCCESS(status)) {
    name = testData->getName();
    if(testData->getInfo(info, status)) {
      log(info->getString("Description", status));
    }
    if(exec) {
      log(name);
        logln("---");
        logln("");
        processTest(testData, status);
    }
    delete testData;
  } else {
    name = "";
  }


}

UBool
DataDrivenCollatorTest::setTestSequence(const UnicodeString &setSequence, UnicodeString &source, Collator::EComparisonResult &relation, UErrorCode &status) {
  seq.setText(setSequence);
  return getNextInSequence(source, relation, status);
}

// Parses the sequence to be tested
UBool 
DataDrivenCollatorTest::getNextInSequence(UnicodeString &source, Collator::EComparisonResult &relation, UErrorCode &status) {
  source.truncate(0);
  // TODO: add quoting support - will need it pretty soon!
  UBool quoted = FALSE;
  UBool quotedsingle = FALSE;
  UChar32 currChar = 0;

  while(currChar != CharacterIterator::DONE) {
    currChar= seq.next32PostInc();
    if(!quoted) {
      if(u_isWhitespace(currChar)) {
        continue;
      }
      switch(currChar) {
      case CharacterIterator::DONE:
        break;
      case 0x003C /* < */:
        relation = Collator::LESS;
        currChar = CharacterIterator::DONE;
        break;
      case 0x003D /* = */:
        relation = Collator::EQUAL;
        currChar = CharacterIterator::DONE;
        break;
      case 0x003E /* > */:
        relation = Collator::GREATER;
        currChar = CharacterIterator::DONE;
        break;
      case 0x0027 /* ' */: /* very basic quoting */
        quoted = TRUE;
        quotedsingle = FALSE;
        break;
      case 0x005c /* \ */: /* single quote */
        quoted = TRUE;
        quotedsingle = TRUE;
        break;
      default:
        source.append(currChar);
      }
    } else {
      if(currChar == CharacterIterator::DONE) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        errln("Quote in sequence not closed!");
        return FALSE;
      } else if(currChar == 0x0027) {
        quoted = FALSE;
      } else {
        source.append(currChar);
      }
      if(quotedsingle) {
        quoted = FALSE;
      }
    }
  }
  return seq.hasNext();
}

// Reads the options string and sets appropriate attributes in collator
void 
DataDrivenCollatorTest::processArguments(Collator *col, const UChar *start, int32_t optLen, UErrorCode &status) {
  const UChar *end = start+optLen;
  UColAttribute attrib;
  UColAttributeValue value;

  if(optLen == 0) {
    return;
  }

  start = ucol_tok_getNextArgument(start, end, &attrib, &value, &status);
  while(start != NULL) {
    if(U_SUCCESS(status)) {
      col->setAttribute(attrib, value, status);
    }
    start = ucol_tok_getNextArgument(start, end, &attrib, &value, &status);
  }
}

void 
DataDrivenCollatorTest::processTest(TestData *testData, UErrorCode &status) {
  Collator *col = NULL;
/*
  UnicodeString testInit[256];
  const char **testNames = new const char*[256];
  int32_t settingsSetSize = 0;
  const UChar *arguments = NULL;
  int32_t argLen = 0;
  int32_t i = 0;
  while(settingsSetSize = driver->getNextSettingsSet(testNames, testInit, 256, status)) {
    argLen = 0;
    for(i = 0; i < settingsSetSize; i++) {
      if(strcmp(testNames[i], "Locale") == 0) { // Make the locale dependent collator
        if(col == NULL) {
          char localeName[256];
          testInit[i].extract(0, testInit[i].length(), localeName, "");
          col = Collator::createInstance(localeName, status);
          if(U_SUCCESS(status)) {
            logln("Testing collator for locale "+testInit[i]);
          } else {
            errln("Unable to instantiate collator for locale "+testInit[i]);
            return;
          }
        } else {
          errln("Collator defined more than once!");
          return;
        }
      } else if(strcmp(testNames[i], "Rules") == 0) {
        if(col == NULL) {
          col = new RuleBasedCollator(testInit[i], status);
          if(U_SUCCESS(status)) {
            logln("Testing collator for rules "+UnicodeString(testInit[i]));
          } else {
            errln("Unable to instantiate collator for rules "+UnicodeString(testInit[i]));
            return;
          }
        } else {
          errln("Collator defined more than once!");
          return;
        }
      } else if(strcmp(testNames[i], "Attributes") == 0) {
        logln("Arguments: "+testInit[i]);
        argLen = testInit[i].length();
        arguments = testInit[i].getBuffer();
      } else {
        errln("I don't understand the setting "+UnicodeString(testNames[i]));
      }
    }
    if(col != NULL) {
      if(argLen > 0) {
        processArguments(col, arguments, argLen, status);
        if(U_SUCCESS(status)) {
          UnicodeString sequence[1];
          while(driver->getNextTestCase(sequence, 1, status)) {
            processSequence(col, *sequence, status);
          }
        } else {
          errln("Couldn't process arguments");
        }
      }
    } else {
      errln("Couldn't instantiate a collator!");
    }
    delete col;
  }
  delete testNames;
*/
  const UChar *arguments = NULL;
  int32_t argLen = 0;
  const DataMap *settings = NULL;
  const DataMap *currentCase = NULL;
  UErrorCode intStatus = U_ZERO_ERROR;
  UnicodeString testSetting;
  while(testData->nextSettings(settings, status)) {
    // try to get a locale
    testSetting = settings->getString("TestLocale", intStatus);
    if(U_SUCCESS(intStatus)) {
      char localeName[256];
      testSetting.extract(0, testSetting.length(), localeName, "");
      col = Collator::createInstance(localeName, status);
      if(U_SUCCESS(status)) {
        logln("Testing collator for locale "+testSetting);
      } else {
        errln("Unable to instantiate collator for locale "+testSetting);
        return;
      }
    } else {
      // if no locale, try from rules
      intStatus = U_ZERO_ERROR;
      testSetting = settings->getString("Rules", intStatus);
      if(U_SUCCESS(intStatus)) {
        col = new RuleBasedCollator(testSetting, status);
        if(U_SUCCESS(status)) {
          logln("Testing collator for rules "+testSetting);
        } else {
          errln("Unable to instantiate collator for rules "+testSetting);
          return;
        }
      } else {
        errln("No collator definition!");
      }
    }
    if(col != NULL) {
      // get attributes
      testSetting = settings->getString("Arguments", intStatus);
      if(U_SUCCESS(intStatus)) {
        logln("Arguments: "+testSetting);
        argLen = testSetting.length();
        arguments = testSetting.getBuffer();
        processArguments(col, arguments, argLen, intStatus);
        if(U_FAILURE(intStatus)) {
          errln("Couldn't process arguments");
          break;
        }
      }
      // Start the processing
      while(testData->nextCase(currentCase, status)) {
        UnicodeString sequence = currentCase->getString("sequence", status);
        if(U_SUCCESS(status)) {
            processSequence(col, sequence, status);
        }
      }
    } else {
      errln("Couldn't instantiate a collator!");
    }
    delete col;
  }
}

void 
DataDrivenCollatorTest::processSequence(Collator* col, const UnicodeString &sequence, UErrorCode &status) {
  UnicodeString source;
  UnicodeString target;
  UnicodeString temp;
  Collator::EComparisonResult relation;
  Collator::EComparisonResult nextRelation;
  UBool hasNext;

  setTestSequence(sequence, source, relation, status);

  // TODO: have a smarter tester that remembers the sequence and ensures that
  // the complete sequence is in order. That is why I have made a constraint
  // in the sequence format.
  do {
    hasNext = getNextInSequence(target, nextRelation, status);
    doTest(col, source, target, relation);
    source = target;
    relation = nextRelation;
  } while(hasNext);
}

