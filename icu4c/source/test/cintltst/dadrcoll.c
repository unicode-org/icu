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

#include "dadrcoll.h"

#include "unicode/utypes.h"
#include "unicode/uchar.h"

#include "cstring.h"
#include "ucol_tok.h"

#include "callcoll.h"

static UResourceBundle *parsing = NULL;
static UResourceBundle *purpose = NULL;
static UResourceBundle *currentTest = NULL;
static UResourceBundle *testData = NULL;
static UResourceBundle *testBundle = NULL;
static int32_t numberOfTests = 0;
static UBool dataTestValid = TRUE;

/* sequence that is read for testing */
static const UChar *currentSequence = NULL;
static int32_t currentSequenceLen = 0;

/* TODO: this is bad - be smarter, although if on level with IntlTest, it might be good */
static char* _testDataPath=NULL;


/* Get test data from ResourceBundles */
static UResourceBundle* 
getTestBundle(const char* bundleName) 
{
    UErrorCode status = U_ZERO_ERROR;
    UResourceBundle *testBundle = NULL;
    const char* icu_data = (char*)loadTestData(&status);
    char testBundlePath[256] = {'\0'};
    strcpy(testBundlePath, icu_data);
    /*strcat(testBundlePath, U_FILE_SEP_STRING".."U_FILE_SEP_STRING"build"U_FILE_SEP_STRING);*/
    if (testBundle == NULL) {
        testBundle = ures_openDirect(testBundlePath, bundleName, &status);
        if (status != U_ZERO_ERROR) {
            log_err("Failed: could not load test data from resourcebundle: %s", bundleName);
            dataTestValid = FALSE;
        }
    }
    return testBundle;
}


static void 
OpenDataDrivenCollatorTest(void) 
{
  UErrorCode status = U_ZERO_ERROR;
  parsing = NULL;
  purpose = NULL;
  currentTest = NULL;
  testData = NULL;
  testBundle = NULL;
  numberOfTests = 0;
  dataTestValid = TRUE;
  testBundle = getTestBundle("DataDrivenCollationTest");
  if(dataTestValid) {
    purpose = ures_getByKey(testBundle, "TestPurpose", NULL, &status);
    parsing = ures_getByKey(testBundle, "TestDataParsing", NULL, &status);
    testData = ures_getByKey(testBundle, "TestData", NULL, &status);
    numberOfTests = ures_getSize(testData);
    if(status != U_ZERO_ERROR) {
      log_err("Unable to initalize test data - missing mandatory description resources!");
      dataTestValid = FALSE;
    }
    currentTest = NULL;
  }
}

static void 
CloseDataDrivenCollatorTest(void) 
{
  ures_close(parsing);
  ures_close(purpose);
  ures_close(currentTest);
  ures_close(testData);
  ures_close(testBundle);
}

/* Reads the options string and sets appropriate attributes in collator */
static void 
processArguments(UCollator *col, const UChar *start, int32_t optLen, UErrorCode *status) {
  const UChar *end = start+optLen;
  UColAttribute attrib;
  UColAttributeValue value;

  start = ucol_tok_getNextArgument(start, end, &attrib, &value, status);
  while(start != NULL) {
    if(U_SUCCESS(*status)) {
      ucol_setAttribute(col, attrib, value, status);
    }
    start = ucol_tok_getNextArgument(start, end, &attrib, &value, status);
  }
}

/* Parses the sequence to be tested */
static UBool 
getNextInSequence(UChar *source, UCollationResult *relation, UErrorCode *status) {
  UBool quoted = FALSE;
  UBool quotedsingle = FALSE;
  UBool done = FALSE;
  UChar32 currChar = 0;
  int32_t i = 0;
  int32_t offset = 0;
  *source = 0;
  /*source.truncate(0);*/
  /*while(currChar != CharacterIterator::DONE) {*/
  while(i<currentSequenceLen && !done) {
    /*currChar= seq.next32PostInc();*/
    UTF_NEXT_CHAR(currentSequence, i, currentSequenceLen, currChar);
    if(!quoted) {
      if(u_isWhitespace(currChar)) {
        continue;
      }
      switch(currChar) {
      case 0x003C /* < */:
        *relation = UCOL_LESS;
        done = TRUE;
        break;
      case 0x003D /* = */:
        *relation = UCOL_EQUAL;
        done = TRUE;
        break;
      case 0x003E /* > */:
        *relation = UCOL_GREATER;
        done = TRUE;
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
        UTF_APPEND_CHAR(source, offset, 256, currChar);
        /*source.append(currChar);*/
      }
    } else {
      if(currChar == 0x0027) {
        quoted = FALSE;
      } else {
        UTF_APPEND_CHAR(source, offset, 256, currChar);
        /*source.append(currChar);*/
      }
      if(quotedsingle) {
        quoted = FALSE;
      }
    }
  }
  if(quoted == TRUE) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    log_err("Quote in sequence not closed!");
    return FALSE;
  }


  source[offset] = 0;
  currentSequence += i;
  currentSequenceLen -= i;

  return (currentSequenceLen > 0);

}

static UBool
setTestSequence(const UChar *setSequence, int32_t len, UChar *source, UCollationResult *relation, UErrorCode *status) {
  /*seq.setText(setSequence);*/
  currentSequence = setSequence;
  currentSequenceLen = len;
  return getNextInSequence(source, relation, status);
}

static void 
processSequence(UCollator* col, const UChar *sequence, int32_t len, UErrorCode *status) {
  UChar sourceBuff[256];
  UChar targetBuff[256];
  UChar *source = sourceBuff;
  UChar *target = targetBuff;
  UCollationResult relation;
  UCollationResult nextRelation;
  UBool hasNext;

  setTestSequence(sequence, len, source, &relation, status);

  /* TODO: have a smarter tester that remembers the sequence and ensures that */
  /* the complete sequence is in order. That is why I have made a constraint  */
  /* in the sequence format.                                                  */
  do {
    hasNext = getNextInSequence(target, &nextRelation, status);
    doTest(col, source, target, relation);
    /*source = target;*/
    if(source == sourceBuff) {
      source = targetBuff;
      target = sourceBuff;
    } else {
      source = sourceBuff;
      target = targetBuff;
    }
    relation = nextRelation;
  } while(hasNext);
}

/* Goes through the list of sequences */
/* and runs test for each sequence    */
static void
processReadyCollator(UResourceBundle *test, UCollator *col, UErrorCode *status) {
  UResourceBundle *testData = ures_getByKey(test, "TestData", NULL, status);
  const char *key = NULL;
  const UChar *sequence = NULL;
  int32_t len = 0;
  if(*status == U_ZERO_ERROR) {
    while(ures_hasNext(testData)) {
      sequence = ures_getNextString(testData, &len, &key, status);
      processSequence(col, sequence, len, status);
    }
    ures_close(testData);
  } else {
    log_err("Unable to get test data!");
  }
}

/* Goes through the list of options and executes test data for each individual option.  */
/* If there are no options, just does the test data.                                    */
static void
processCollatorTests(UResourceBundle *test, UCollator *col, UErrorCode *status) {
  const UChar *options = NULL;
  const char *key = NULL;
  int32_t len = 0;
  UResourceBundle *optionsRes = NULL;
  *status = U_ZERO_ERROR;
  optionsRes = ures_getByKey(test, "Arguments", NULL, status);
  if(*status == U_ZERO_ERROR) {
    while(ures_hasNext(optionsRes)) {
      options = ures_getNextString(optionsRes, &len, &key, status);
      processArguments(col, options, len, status);
      if(U_SUCCESS(*status)) {
        processReadyCollator(test, col, status);      
      } else {
        log_err("Error processing arguments!");
      }
    } 
    ures_close(optionsRes);
  } else {
    processReadyCollator(test, col, status);      
  }
}

static void 
processTest(UResourceBundle *test, UErrorCode *status) {
  UCollator *col = NULL;
  const UChar *colString = NULL;
  const char* key = NULL;
  int32_t len = 0;
  UParseError parseError;
  UResourceBundle *type = NULL;

  type = ures_getByKey(test, "TestLocale", NULL, status);
  if(*status == U_ZERO_ERROR) {
    /* this is a case where we have locale */
    while(ures_hasNext(type)) {
      const UChar* locale = ures_getNextString(type, &len, &key, status);
      char localeName[256];
      u_UCharsToChars(locale, localeName, len);
      localeName[len] = 0;
      col = ucol_open(localeName, status);
      if(U_SUCCESS(*status)) {
        log_verbose("Testing collator for locale %s\n", localeName);
        processCollatorTests(test, col, status);
        ucol_close(col);
      } else {
        log_err("Unable to instantiate collator for locale %s\n", localeName);
      }
    }
  }
  *status = U_ZERO_ERROR;
  type = ures_getByKey(test, "TestRules", type, status);
  if(*status == U_ZERO_ERROR) {
    /* here we deal with rules */
    while(ures_hasNext(type)) {
      colString = ures_getNextString(type, &len, &key, status);
      col = ucol_openRules(colString, len, UCOL_DEFAULT, UCOL_DEFAULT, &parseError, status);
      if(U_SUCCESS(*status)) {
        log_verbose("Testing collator for rules "); /*+UnicodeString(colString));*/
        processCollatorTests(test, col, status);
        ucol_close(col);
      } else {
        log_err("Unable to instantiate collator for rules "); /*+UnicodeString(colString));*/
      }
    }
  }
  ures_close(type);
}

static void 
DataDrivenTest(void) 
{
  UErrorCode status = U_ZERO_ERROR;
  UResourceBundle *test = NULL;
  const char* testName = NULL;
  OpenDataDrivenCollatorTest();
  ures_resetIterator(testData);
  while(ures_hasNext(testData)) {
    test = ures_getNextResource(testData, test, &status);
    testName = ures_getKey(test);
    log_verbose("%s\n", testName);
    processTest(test, &status);
  }
  ures_close(test);
  CloseDataDrivenCollatorTest();
}



#if 0
/* scraps */
static const char* 
loadTestData(UErrorCode *err){
    const char*      directory=NULL;
    UResourceBundle* test =NULL;
    char* tdpath=NULL;
    const char* tdrelativepath = ".."U_FILE_SEP_STRING"test"U_FILE_SEP_STRING"testdata"U_FILE_SEP_STRING"out"U_FILE_SEP_STRING;
    if( _testDataPath == NULL){
        directory= u_getDataDirectory();
    
        tdpath = new char[(( strlen(directory) * strlen(tdrelativepath)) + 10)]; /* (char*) ctst_malloc(sizeof(char) *(( strlen(directory) * strlen(tdrelativepath)) + 10)); */


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
                log_err("construction of NULL did not succeed  :  %s \n", u_errorName(err));
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
#endif


void addDataDrivenTest(TestNode** root)
{
    
    addTest(root, &DataDrivenTest, "tscoll/DataDrivenTest");
}

