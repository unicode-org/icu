/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CMSGTST.C
*
* Modification History:
*        Name                     Description
*     Madhu Katragadda              Creation
*********************************************************************************
*/
/* C API TEST FOR MESSAGE FORMAT */

#include "unicode/uloc.h"
#include "unicode/utypes.h"
#include "unicode/umsg.h"
#include "unicode/udat.h"
#include "unicode/umsg.h"
#include "unicode/ustring.h"
#include "cintltst.h"
#include "cmsgtst.h"
#include<stdio.h>
#include <string.h>
#include "cformtst.h"

static const char* txt_testCasePatterns[] = {
   "Quotes '', '{', a {0,number,integer} '{'0}",
   "Quotes '', '{', a {0,number,integer} '{'0}",
   "You deposited {0,number,integer} times an amount of {1,number,currency} on {2,date,short}",
    "'{'2,time,full}, for {1, number, integer}, {0,number,integer} is {2,time,full} and full date is {2,date,full}",
   "'{'1,number,percent} for {0,number,integer} is {1,number,percent}",
};

static const char* txt_testResultStrings[] = {
    "Quotes ', {, a 1 {0}",
    "Quotes ', {, a 1 {0}",
    "You deposited 1 times an amount of $3,456.00 on 1/12/70",
    "{2,time,full}, for 3,456, 1 is 5:46:40 AM PST and full date is Monday, January 12, 1970",
    "{1,number,percent} for 1 is 345,600%"
};

const int32_t cnt_testCases = 5;
static UChar* testCasePatterns[5];

static UChar* testResultStrings[5];

static UBool strings_initialized = FALSE;
/* function used to create the test patterns for testing Message formatting */
static void InitStrings( void )
{
    int32_t i;
    if (strings_initialized) return;
    strings_initialized = TRUE;

    for (i=0; i < cnt_testCases; i++ ) {
        testCasePatterns[i]=(UChar*)malloc(sizeof(UChar) * (strlen(txt_testCasePatterns[i]) + 1));
        u_uastrcpy(testCasePatterns[i], txt_testCasePatterns[i] );
    }
    for (i=0; i < cnt_testCases; i++ ) {
        testResultStrings[i] = (UChar*)malloc(sizeof(UChar) * (strlen(txt_testResultStrings[i]) + 1));
        u_uastrcpy(testResultStrings[i], txt_testResultStrings[i] );
    }
}
/* Test u_formatMessage() with various test patterns() */
void MessageFormatTest( void ) 
{

    UChar *str;
    UChar* result;
    int32_t resultLengthOut,resultlength,i, patternlength;
    UErrorCode status = U_ZERO_ERROR;
    UDate d1=1000000000.0;
    str=(UChar*)malloc(sizeof(UChar) * 7);
    u_uastrcpy(str, "MyDisk");
    resultlength=1;
    result=(UChar*)malloc(sizeof(UChar) * 1);
    log_verbose("Testing u_formatMessage90\n");
    InitStrings();
    for (i = 0; i < cnt_testCases; i++) {
        status=U_ZERO_ERROR;
        patternlength=u_strlen(testCasePatterns[i]);
        resultLengthOut=u_formatMessage( "en_US",testCasePatterns[i], patternlength, result, resultlength, 
            &status, 1, 3456.00, d1);
        if(status== U_BUFFER_OVERFLOW_ERROR)
        {
        status=U_ZERO_ERROR;
        resultlength=resultLengthOut+1;
        result=(UChar*)realloc(result,sizeof(UChar) * resultlength);
        u_formatMessage( "en_US",testCasePatterns[i], patternlength, result, resultlength, 
            &status, 1, 3456.00, d1);
    }
    if(U_FAILURE(status)){
        log_err("ERROR: failure in message format on testcase %d:  %s\n", i, myErrorName(status) );
    }
    if(u_strcmp(result, testResultStrings[i])==0){
        log_verbose("PASS: MessagFormat successful on testcase : %d\n", i);
    }
    else{
        log_err("FAIL: Error in MessageFormat on testcase : %d\n GOT %s EXPECTED %s\n", i, 
            austrdup(result), austrdup(testResultStrings[i]) );
    }
    }
    free(result);
    free(str);
}


/*test u_formatMessage() with sample patterns */
void TestSampleMessageFormat()
{
  UChar *str;
  UChar *result;
  UChar pattern[100], expected[100];
  int32_t resultLengthOut, resultlength;
  UDate d = 837039928046.0;
  UErrorCode status = U_ZERO_ERROR;
  str=(UChar*)malloc(sizeof(UChar) * 15);
  u_uastrcpy(str, "abc");    
    
  u_uastrcpy(pattern, "There are {0} files on {1,date}");
  u_uastrcpy(expected, "There are abc files on Jul 10, 1996");
  result=(UChar*)malloc(sizeof(UChar) * 1);
  log_verbose("\nTesting a sample for Message format test#1\n");
  resultlength=1;
  resultLengthOut=u_formatMessage( "en_US", pattern, u_strlen(pattern), result, resultlength, &status, str, d);
  if(status==U_BUFFER_OVERFLOW_ERROR)
    {
      status=U_ZERO_ERROR;
      resultlength=resultLengthOut+1;
      result=(UChar*)realloc(result, sizeof(UChar) * resultlength);
      u_formatMessage( "en_US", pattern, u_strlen(pattern), result, resultlength, &status, str, d);
    }
  if(U_FAILURE(status)){
    log_err("Error: failure in message format on test#1: %s\n", myErrorName(status));
  }
  if(u_strcmp(result, expected)==0)
    log_verbose("PASS: MessagFormat successful on test#1\n");
  else{
    log_err("FAIL: Error in MessageFormat on test#1 \n GOT: %s EXPECTED: %s\n", 
        austrdup(result), austrdup(expected) );
  }

       
  log_verbose("\nTesting message format with another pattern test#2\n");
  u_uastrcpy(pattern, "The disk \"{0}\" contains {1,number,integer} file(s)");
  u_uastrcpy(expected, "The disk \"MyDisk\" contains 23 file(s)");
  u_uastrcpy(str, "MyDisk");
    
  resultLengthOut=u_formatMessage( "en_US", 
                   pattern, 
                   u_strlen(pattern),
                   result,
                   resultlength,
                   &status, 
                   str,
                   235);
  if(status==U_BUFFER_OVERFLOW_ERROR)
    {
      status=U_ZERO_ERROR;
      resultlength=resultLengthOut+1;
      result=(UChar*)realloc(result, sizeof(UChar) * (resultlength+1));
      u_formatMessage( "en_US", pattern, u_strlen(pattern), result, resultlength, &status, str, 23);
    }
  if(U_FAILURE(status)){
    log_err("Error: failure in message format on test#2 : %s\n", myErrorName(status));
  }
  if(u_strcmp(result, expected)==0)
    log_verbose("PASS: MessagFormat successful on test#2\n");
  else{
    log_err("FAIL: Error in MessageFormat on test#2\n GOT: %s EXPECTED: %s\n", 
        austrdup(result), austrdup(expected) );
  }

   

  log_verbose("\nTesting message format with another pattern test#3\n");
  u_uastrcpy(pattern, "You made a {0} of {1,number,currency}");
  u_uastrcpy(expected, "You made a deposit of $500.00");
  u_uastrcpy(str, "deposit");
  resultlength=0;
  resultLengthOut=u_formatMessage( "en_US", pattern, u_strlen(pattern), NULL, resultlength, &status, str, 500.00);
  if(status==U_BUFFER_OVERFLOW_ERROR)
    {
      status=U_ZERO_ERROR;
      resultlength=resultLengthOut+1;
      result=(UChar*)realloc(result, sizeof(UChar) * resultlength);
      u_formatMessage( "en_US", pattern, u_strlen(pattern), result, resultlength, &status, str, 500.00);
    }
  if(U_FAILURE(status)){
    log_err("Error: failure in message format on test#3 : %s\n", myErrorName(status));
  }
  if(u_strcmp(result, expected)==0)
    log_verbose("PASS: MessagFormat successful on test#3\n");
  else{
    log_err("FAIL: Error in MessageFormat on test#3\n GOT: %s EXPECTED %s\n", austrdup(result), 
        austrdup(expected) );
  }

  free(result);
  free(str);
    

    
}
/* Test u_formatMessage() and u_parseMessage() , format and parse sequence and round trip */
void TestSampleFormatAndParse()
{

    UChar *result, *tzID, *str;
    UChar pattern[100];
    UChar expected[100];
    int32_t resultLengthOut, resultlength;
    UCalendar *cal;
    UDate d1,d;
    UDateFormat *def1;
    UErrorCode status = U_ZERO_ERROR;
    double value;
    UChar ret[30];
    log_verbose("Testing format and parse\n");

    str=(UChar*)malloc(sizeof(UChar) * 25);
    u_uastrcpy(str, "disturbance in force");
    tzID=(UChar*)malloc(sizeof(UChar) * 4);
    u_uastrcpy(tzID, "PST");
    cal=ucal_open(tzID, u_strlen(tzID), "en_US", UCAL_TRADITIONAL, &status);
    if(U_FAILURE(status)){
        log_err("error in ucal_open caldef : %s\n", myErrorName(status) );
    }
    ucal_setDateTime(cal, 1999, UCAL_MARCH, 18, 0, 0, 0, &status);
    d1=ucal_getMillis(cal, &status);
    if(U_FAILURE(status)){
            log_err("Error: failure in get millis: %s\n", myErrorName(status) );
    }
    
    log_verbose("\nTesting with pattern test#4");
    u_uastrcpy(pattern, "On {0, date, long}, there was a {1} on planet {2,number,integer}");
    u_uastrcpy(expected, "On March 18, 1999, there was a disturbance in force on planet 7"); 
    resultlength=1;
    result=(UChar*)malloc(sizeof(UChar) * resultlength);
    resultLengthOut=u_formatMessage( "en_US", pattern, u_strlen(pattern), result, resultlength, &status, d1, str, 7);
    if(status==U_BUFFER_OVERFLOW_ERROR)
    {
        status=U_ZERO_ERROR;
        resultlength=resultLengthOut+1;
        result=(UChar*)realloc(result, sizeof(UChar) * resultlength);
        u_formatMessage( "en_US", pattern, u_strlen(pattern), result, resultlength, &status, d1, str, 7);
        
    }
    if(U_FAILURE(status)){
        log_err("ERROR: failure in message format test#4: %s\n", myErrorName(status));
    }
    if(u_strcmp(result, expected)==0)
        log_verbose("PASS: MessagFormat successful on test#4\n");
    else{
        log_err("FAIL: Error in MessageFormat on test#4\n GOT: %s EXPECTED: %s\n", austrdup(result),
            austrdup(expected) );
    }
    
    
    /*try to parse this and check*/
    log_verbose("\nTesting the parse Message test#5\n");
    def1 = udat_open(UDAT_DEFAULT,UDAT_DEFAULT ,NULL, NULL, 0, &status);
    if(U_FAILURE(status))
    {
        log_err("error in creating the dateformat using short date and time style:\n %s\n", myErrorName(status));
    }
    u_parseMessage("en_US", pattern, u_strlen(pattern), result, u_strlen(result), &status, &d, ret, &value);
    if(U_FAILURE(status)){
        log_err("ERROR: error in parsing: test#5: %s\n", myErrorName(status));
    }
    if(value!=7.00 && u_strcmp(str,ret)!=0)
        log_err("FAIL: Error in parseMessage on test#5 \n");
    else
        log_verbose("PASS: parseMessage successful on test#5\n");
        


    if(u_strcmp(myDateFormat(def1, d), myDateFormat(def1, d1))==0)
        log_verbose("PASS: parseMessage successful test#5\n");
    else{
        log_err("FAIL: parseMessage didn't parse the date successfully\n GOT: %s EXPECTED %s\n", 
            austrdup(myDateFormat(def1,d)), austrdup(myDateFormat(def1,d1)) );
        }

    udat_close(def1);
    ucal_close(cal);

    free(result);
    free(str);
    free(tzID);
    
}
/* test message format with a choice option */
void TestMsgFormatChoice()
{
    UChar* str;
    UErrorCode status = U_ZERO_ERROR;
    UChar *result;
    UChar pattern[100];
    UChar expected[100];
    int32_t resultlength,resultLengthOut;
    
    str=(UChar*)malloc(sizeof(UChar) * 25);
    u_uastrcpy(str, "MyDisk");
    log_verbose("Testing message format with choice test #6\n:");
    /*There {0,choice,0#are no files|1#is one file|1<are {0,number,integer} files}.*/
    u_uastrcpy(pattern, "The disk {1} contains {0,choice,0#no files|1#one file|1<{0,number,integer} files}");
    u_uastrcpy(expected, "The disk MyDisk contains 100 files");
    resultlength=0;
    resultLengthOut=u_formatMessage( "en_US", pattern, u_strlen(pattern), NULL, resultlength, &status, 100., str);
    if(status==U_BUFFER_OVERFLOW_ERROR)
    {
        status=U_ZERO_ERROR;
        resultlength=resultLengthOut+1;
        result=(UChar*)malloc(sizeof(UChar) * resultlength);
        u_formatMessage( "en_US", pattern, u_strlen(pattern), result, resultlength, &status, 100., str);
        if(u_strcmp(result, expected)==0)
            log_verbose("PASS: MessagFormat successful on test#6\n");
        else{
            log_err("FAIL: Error in MessageFormat on test#6\n GOT %s EXPECTED %s\n", austrdup(result),
                austrdup(expected) );
        }
        free(result);
    }
    if(U_FAILURE(status)){
        log_err("ERROR: failure in message format on test#6 : %s\n", myErrorName(status));
    }

    log_verbose("Testing message format with choice test #7\n:");
    u_uastrcpy(expected, "The disk MyDisk contains no files");
    resultlength=0;
    resultLengthOut=u_formatMessage( "en_US", pattern, u_strlen(pattern), NULL, resultlength, &status, 0., str);
    if(status==U_BUFFER_OVERFLOW_ERROR)
    {
        status=U_ZERO_ERROR;
        resultlength=resultLengthOut+1;
        result=(UChar*)malloc(sizeof(UChar) * resultlength);
        u_formatMessage( "en_US", pattern, u_strlen(pattern), result, resultlength, &status, 0., str);

        if(u_strcmp(result, expected)==0)
            log_verbose("PASS: MessagFormat successful on test#7\n");
        else{
            log_err("FAIL: Error in MessageFormat on test#7\n GOT: %s EXPECTED %s\n", austrdup(result), 
                austrdup(expected) );
        }
        free(result);
    }
    if(U_FAILURE(status)){
        log_err("ERROR: failure in message format on test#7 : %s\n", myErrorName(status));
    }

    log_verbose("Testing message format with choice test #8\n:");
    u_uastrcpy(expected, "The disk MyDisk contains one file");
    resultlength=0;
    resultLengthOut=u_formatMessage( "en_US", pattern, u_strlen(pattern), NULL, resultlength, &status, 1., str);
    if(status==U_BUFFER_OVERFLOW_ERROR)
    {
        status=U_ZERO_ERROR;
        resultlength=resultLengthOut+1;
        result=(UChar*)malloc(sizeof(UChar) * resultlength);
        u_formatMessage( "en_US", pattern, u_strlen(pattern), result, resultlength, &status, 1., str);

        if(u_strcmp(result, expected)==0)
            log_verbose("PASS: MessagFormat successful on test#8\n");
        else{
            log_err("FAIL: Error in MessageFormat on test#8\n GOT %s EXPECTED: %s\n", austrdup(result), 
                austrdup(expected) );
        }

        free(result);
    }
    if(U_FAILURE(status)){
        log_err("ERROR: failure in message format on test#8 : %s\n", myErrorName(status));
    }

    free(str);

}
/*test u_parseMessage() with various test patterns */
void TestParseMessage()
{
    UChar pattern[100];
    UChar source[100];
    UErrorCode status = U_ZERO_ERROR;
    double value;
    UChar str[10];
    UChar res[10];
        
    log_verbose("\nTesting a sample for parse Message test#9\n");
    
    u_uastrcpy(source, "You deposited an amount of $500.00");
    u_uastrcpy(pattern, "You {0} an amount of {1,number,currency}");
    u_uastrcpy(res,"deposited");
        
    u_parseMessage( "en_US", pattern, u_strlen(pattern), source, u_strlen(source), &status, str, &value);
    if(U_FAILURE(status)){
        log_err("ERROR: failure in parse Message on test#9: %s\n", myErrorName(status));
    }
    if(value==500.00  && u_strcmp(str,res)==0)
        log_verbose("PASS: parseMessage successful on test#9\n");
    else
        log_err("FAIL: Error in parseMessage on test#9 \n");

    
    
    log_verbose("\nTesting a sample for parse Message test#10\n");
    
    u_uastrcpy(source, "There are 123 files on MyDisk created");
    u_uastrcpy(pattern, "There are {0,number,integer} files on {1} created");
    u_uastrcpy(res,"MyDisk");
        
    u_parseMessage( "en_US", pattern, u_strlen(pattern), source, u_strlen(source), &status, &value, str);
    if(U_FAILURE(status)){
        log_err("ERROR: failure in parse Message on test#10: %s\n", myErrorName(status));
    }
    if(value==123.00 && u_strcmp(str,res)==0)
        log_verbose("PASS: parseMessage successful on test#10\n");
    else
        log_err("FAIL: Error in parseMessage on test#10 \n");


    
}

int32_t CallFormatMessage(const char* locale, UChar* testCasePattern, int32_t patternLength, 
                       UChar* result, int32_t resultLength, UErrorCode *status, ...)
{
    int32_t len = 0;
    va_list ap;
    va_start(ap, status);
    len = u_vformatMessage(locale, testCasePattern, patternLength, result, resultLength, ap, status);
    va_end(ap);
    return len;
}

/* Test u_vformatMessage() with various test patterns. */
void TestMessageFormatWithValist( void ) 
{

    UChar *str;
    UChar* result;
    int32_t resultLengthOut,resultlength,i, patternlength;
    UErrorCode status = U_ZERO_ERROR;
    UDate d1=1000000000.0;
    str=(UChar*)malloc(sizeof(UChar) * 7);
    u_uastrcpy(str, "MyDisk");
    resultlength=1;
    result=(UChar*)malloc(sizeof(UChar) * 1);
    log_verbose("Testing u_formatMessage90\n");
    InitStrings();
    for (i = 0; i < cnt_testCases; i++) {
        status=U_ZERO_ERROR;
        patternlength=u_strlen(testCasePatterns[i]);
        resultLengthOut=CallFormatMessage( "en_US",testCasePatterns[i], patternlength, result, resultlength, 
            &status, 1, 3456.00, d1);
        if(status== U_BUFFER_OVERFLOW_ERROR)
        {
            status=U_ZERO_ERROR;
            resultlength=resultLengthOut+1;
            result=(UChar*)realloc(result,sizeof(UChar) * resultlength);
            CallFormatMessage( "en_US",testCasePatterns[i], patternlength, result, resultlength, 
                &status, 1, 3456.00, d1);
    }
    if(U_FAILURE(status)){
        log_err("ERROR: failure in message format on testcase %d:  %s\n", i, myErrorName(status) );
    }
    if(u_strcmp(result, testResultStrings[i])==0){
        log_verbose("PASS: MessagFormat successful on testcase : %d\n", i);
    }
    else{
        log_err("FAIL: Error in MessageFormat on testcase : %d\n GOT %s EXPECTED %s\n", i, 
            austrdup(result), austrdup(testResultStrings[i]) );
    }
    }
    free(result);
    free(str);
}

void CallParseMessage(const char* locale, UChar* pattern, int32_t patternLength, 
                       UChar* source, int32_t sourceLength, UErrorCode *status, ...)
{
    va_list ap;
    va_start(ap, status);
    u_vparseMessage(locale, pattern, patternLength, source, sourceLength, ap, status);
    va_end(ap);
}
/*test u_vparseMessage() with various test patterns */
void TestParseMessageWithValist()
{
    UChar pattern[100];
    UChar source[100];
    UErrorCode status = U_ZERO_ERROR;
    double value;
    UChar str[10];
    UChar res[10];
        
    log_verbose("\nTesting a sample for parse Message test#9\n");
    
    u_uastrcpy(source, "You deposited an amount of $500.00");
    u_uastrcpy(pattern, "You {0} an amount of {1,number,currency}");
    u_uastrcpy(res,"deposited");
        
    CallParseMessage( "en_US", pattern, u_strlen(pattern), source, u_strlen(source), &status, str, &value);
    if(U_FAILURE(status)){
        log_err("ERROR: failure in parse Message on test#9: %s\n", myErrorName(status));
    }
    if(value==500.00  && u_strcmp(str,res)==0)
        log_verbose("PASS: parseMessage successful on test#9\n");
    else
        log_err("FAIL: Error in parseMessage on test#9 \n");

    
    
    log_verbose("\nTesting a sample for parse Message test#10\n");
    
    u_uastrcpy(source, "There are 123 files on MyDisk created");
    u_uastrcpy(pattern, "There are {0,number,integer} files on {1} created");
    u_uastrcpy(res,"MyDisk");
        
    CallParseMessage( "en_US", pattern, u_strlen(pattern), source, u_strlen(source), &status, &value, str);
    if(U_FAILURE(status)){
        log_err("ERROR: failure in parse Message on test#10: %s\n", myErrorName(status));
    }
    if(value==123.00 && u_strcmp(str,res)==0)
        log_verbose("PASS: parseMessage successful on test#10\n");
    else
        log_err("FAIL: Error in parseMessage on test#10 \n");    
}
void addMsgForTest(TestNode** root)
{
    addTest(root, &MessageFormatTest, "tsformat/cmsgtst/MessageFormatTest");
    addTest(root, &TestSampleMessageFormat, "tsformat/cmsgtst/TestSampleMessageFormat");
    addTest(root, &TestSampleFormatAndParse, "tsformat/cmsgtst/TestSampleFormatAndParse");
    addTest(root, &TestMsgFormatChoice, "tsformat/cmsgtst/TestMsgFormatChoice");
    addTest(root, &TestParseMessage, "tsformat/cmsgtst/TestParseMessage");
    addTest(root, &TestMessageFormatWithValist, "tsformat/cmsgtst/TestMessageFormatWithValist");
    addTest(root, &TestParseMessageWithValist, "tsformat/cmsgtst/TestParseMessageWithValist");

}
