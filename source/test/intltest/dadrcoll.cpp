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

// TODO: this is bad - be smarter, although if on level with IntlTest, it might be good
static char* _testDataPath=NULL;

DataDrivenCollatorTest::DataDrivenCollatorTest() 
: seq(StringCharacterIterator("")),
  parsing(NULL),
  purpose(NULL),
  currentTest(NULL),
  testData(NULL),
  testBundle(NULL),
  numberOfTests(0)
{
  UErrorCode status = U_ZERO_ERROR;
  dataTestValid = TRUE;
  testBundle = getTestBundle("DataDrivenCollationTest");
  if(dataTestValid) {
    purpose = ures_getByKey(testBundle, "TestPurpose", NULL, &status);
    parsing = ures_getByKey(testBundle, "TestDataParsing", NULL, &status);
    testData = ures_getByKey(testBundle, "TestData", NULL, &status);
    numberOfTests = ures_getSize(testData);
    if(status != U_ZERO_ERROR) {
      errln("Unable to initalize test data - missing mandatory description resources!");
      dataTestValid = FALSE;
    }
    currentTest = NULL;
  }
}

DataDrivenCollatorTest::~DataDrivenCollatorTest() 
{
  ures_close(parsing);
  ures_close(purpose);
  ures_close(currentTest);
  ures_close(testData);
  ures_close(testBundle);
}

void DataDrivenCollatorTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* par )
{
  if (exec)
  {
      logln("TestSuite Collator: ");
  }

  if(!dataTestValid) {
    errln("Test data is invalid!");
    numberOfTests = 0;
  }

  if(index >= numberOfTests) {
    name = "";
  } else {
    UErrorCode status = U_ZERO_ERROR;
    currentTest = ures_getByIndex(testData, index, currentTest, &status);
    if(U_SUCCESS(status)) {
      name = ures_getKey(currentTest);
      if(exec) {
        log(name);
        logln("---");
        logln("");
        processTest(currentTest, status);
      }
    }
  }
}

void
DataDrivenCollatorTest::DataDrivenTest(char *par) {
  //printf("%s %s\n", path, par);
  UErrorCode status = U_ZERO_ERROR;
  ures_resetIterator(testData);
  UResourceBundle *test = NULL;
  const char* testName = NULL;
  while(ures_hasNext(testData)) {
    test = ures_getNextResource(testData, test, &status);
    testName = ures_getKey(test);
    logln(testName);
    processTest(test, status);
  }
  ures_close(test);
}

const char* 
DataDrivenCollatorTest::loadTestData(UErrorCode& err){
    const char*      directory=NULL;
    UResourceBundle* test =NULL;
    char* tdpath=NULL;
    const char* tdrelativepath = ".."U_FILE_SEP_STRING"test"U_FILE_SEP_STRING"testdata"U_FILE_SEP_STRING"out"U_FILE_SEP_STRING;
    if( _testDataPath == NULL){
        directory= u_getDataDirectory();
    
        tdpath = new char[(( strlen(directory) * strlen(tdrelativepath)) + 10)];//(char*) ctst_malloc(sizeof(char) *(( strlen(directory) * strlen(tdrelativepath)) + 10));


        /* u_getDataDirectory shoul return \source\data ... set the
         * directory to ..\source\data\..\test\testdata\out\testdata
         *
         * Fallback: When Memory mapped file is built
         * ..\source\data\out\..\..\test\testdata\out\testdata
         */
        strcpy(tdpath, directory);
        strcat(tdpath, tdrelativepath);
        strcat(tdpath,"testdata");

    
        test=ures_open(tdpath, "testtypes", &err);
    
        /* we could not find the data in tdpath 
         * try tdpathFallback
         */
        if(U_FAILURE(err))
        {
            strcpy(tdpath,directory);
            strcat(tdpath,".."U_FILE_SEP_STRING);
            strcat(tdpath, tdrelativepath);
            strcat(tdpath,"testdata");
            err =U_ZERO_ERROR;
            test=ures_open(tdpath, "ja_data", &err);
            /* Fall back did not succeed either so return */
            if(U_FAILURE(err)){
                err = U_FILE_ACCESS_ERROR;
                errln("construction of NULL did not succeed  :  %s \n", u_errorName(err));
                return "";
            }
            ures_close(test);
            _testDataPath = tdpath;
            return _testDataPath;
        }
        ures_close(test);
        _testDataPath = tdpath;
        return _testDataPath;
    }
    return _testDataPath;
}

//Get test data from ResourceBundles
UResourceBundle* 
DataDrivenCollatorTest::getTestBundle(const char* bundleName) {
    UErrorCode status = U_ZERO_ERROR;
    UResourceBundle *testBundle = NULL;
    const char* icu_data = (char*)loadTestData(status);
    char testBundlePath[256] = {'\0'};
    strcpy(testBundlePath, icu_data);
    //strcat(testBundlePath, U_FILE_SEP_STRING".."U_FILE_SEP_STRING"build"U_FILE_SEP_STRING);
    if (testBundle == NULL) {
        testBundle = ures_openDirect(testBundlePath, bundleName, &status);
        if (status != U_ZERO_ERROR) {
            errln(UnicodeString("Failed: could not load test data from resourcebundle: ") + UnicodeString(bundleName));
            dataTestValid = FALSE;
        }
    }
    return testBundle;
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

  start = ucol_tok_getNextArgument(start, end, &attrib, &value, &status);
  while(start != NULL) {
    if(U_SUCCESS(status)) {
      col->setAttribute(attrib, value, status);
    }
    start = ucol_tok_getNextArgument(start, end, &attrib, &value, &status);
  }
}

// Goes through the list of sequences
// and runs test for each sequence
void
DataDrivenCollatorTest::processReadyCollator(UResourceBundle *test, Collator *col, UErrorCode &status) {
  UResourceBundle *testData = ures_getByKey(test, "TestData", NULL, &status);
  const char *key = NULL;
  UnicodeString sequence;
  if(status == U_ZERO_ERROR) {
    while(ures_hasNext(testData)) {
      sequence = ures_getNextUnicodeString(testData, &key, &status);
      processSequence(col, sequence, status);
    }
    ures_close(testData);
  } else {
    errln("Unable to get test data!");
  }
}

// Goes through the list of options and executes test data for each individual option.
// If there are no options, just does the test data.
void
DataDrivenCollatorTest::processCollatorTests(UResourceBundle *test, Collator *col, UErrorCode &status) {
  const UChar *options = NULL;
  const char *key = NULL;
  int32_t len = 0;
  status = U_ZERO_ERROR;
  UResourceBundle *optionsRes = ures_getByKey(test, "Arguments", NULL, &status);
  if(status == U_ZERO_ERROR) {
    while(ures_hasNext(optionsRes)) {
      options = ures_getNextString(optionsRes, &len, &key, &status);
      processArguments(col, options, len, status);
      if(U_SUCCESS(status)) {
        processReadyCollator(test, col, status);      
      } else {
        errln("Error processing arguments!");
      }
    } 
    ures_close(optionsRes);
  } else {
    processReadyCollator(test, col, status);      
  }
}


void 
DataDrivenCollatorTest::processTest(UResourceBundle *test, UErrorCode &status) {
  Collator *col = NULL;
  const UChar *colString = NULL;
  const char* key = NULL;
  int32_t len = 0;
  //int32_t i = 0;
  UResourceBundle *type = ures_getByKey(test, "TestLocale", NULL, &status);
  if(status == U_ZERO_ERROR) {
    // this is a case where we have locale
    //for(i = 0; i < ures_getSize(type); i++) {
    while(ures_hasNext(type)) {
      //UnicodeString locale = ures_getUnicodeStringByIndex(type, i, &status);
      UnicodeString locale = ures_getNextUnicodeString(type, &key, &status);
      char localeName[256];
      locale.extract(0, locale.length(), localeName, "");
      col = Collator::createInstance(localeName, status);
      if(U_SUCCESS(status)) {
        logln("Testing collator for locale "+locale);
        processCollatorTests(test, col, status);
        delete col;
      } else {
        errln("Unable to instantiate collator for locale "+locale);
      }
    }
  }
  status = U_ZERO_ERROR;
  type = ures_getByKey(test, "TestRules", type, &status);
  if(status == U_ZERO_ERROR) {
    //for(i = 0; i < ures_getSize(type); i++) {
    while(ures_hasNext(type)) {
      //colString = ures_getStringByIndex(type, i, &len, &status);
      colString = ures_getNextString(type, &len, &key, &status);
      col = new RuleBasedCollator(colString, status);
      if(U_SUCCESS(status)) {
        logln("Testing collator for rules "+UnicodeString(colString));
        processCollatorTests(test, col, status);
        delete col;
      } else {
        errln("Unable to instantiate collator for rules "+UnicodeString(colString));
      }
    }
  }
  status = U_ZERO_ERROR;
  ures_close(type);
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

