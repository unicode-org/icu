/*
*******************************************************************************
*
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  umsg.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
* This is a C wrapper to MessageFormat C++ API.
*
*   Change history:
*
*   08/5/2001  Ram         Added C wrappers for C++ API. Changed implementation of old API's
*                          Removed pattern parser.
* 
*/

#include "unicode/umsg.h"
#include "mutex.h"
#include "unicode/ustring.h"
#include "unicode/fmtable.h"
#include "cpputils.h"
#include "unicode/msgfmt.h"
#include "unicode/unistr.h"
#include "unicode/numfmt.h"
#include "unicode/ustring.h"

/*
#define COMMA             ((UChar)0x002C)
#define SINGLE_QUOTE      ((UChar)0x0027)
#define LEFT_CURLY_BRACE  ((UChar)0x007B)
#define RIGHT_CURLY_BRACE ((UChar)0x007D)

// Determine if a keyword belongs to a list of keywords
static int32_t
findKeyword(const     UnicodeString&     s, 
        const     UChar    **list,
                int32_t&    kwLen)
{
    if (s.length() == 0)
        return 0;

    UnicodeString buffer = s;
    
    // Determine if there is a ','
    // If so, the string contains a modifier, and we only want to 
    // parse the type
    int32_t commaPos = buffer.indexOf(COMMA);
    commaPos = (commaPos == -1 ? buffer.length() : commaPos);
    buffer.truncate(commaPos);

    // Trims the space characters and turns all characters
    // in s to lower case.
    buffer.trim().toLower();

    for(int32_t i = 0; i < g_umsgListLength; ++i) {
        if (list[i]) {
            kwLen = u_strlen(list[i]);
            if (!buffer.compare(list[i], kwLen)) {
                return i;
            }
        }
    }
    
    kwLen = 0;
    return - 1;
}

// Match the type of argument in a message format pattern
// The type consists of a type indicator and an optional modifier
// Possible types : number, date, time, choice
// Possible modifiers : currency, percent, integer, full, long, short
// We only worry about parsing the types and the "integer" modifier
static Formattable::Type
matchType(const UChar         *pat,
          int32_t     openBrace,
          int32_t     closeBrace)
{
  int32_t len = (closeBrace - openBrace) - 1;
  Formattable::Type result = Formattable::kString;

  // Strings like "{0}" are strings
  if(len == 1) {
    result = Formattable::kString;
    return result;
  }
  // Assume the input is well-formed
  else {
    UnicodeString type((UChar*)pat + openBrace + 1 + 2, len - 2, len - 2);
    int32_t matchLen, kw;

    kw = findKeyword(type, g_umsgTypeList, matchLen);

    // there is a modifier if type contains a ','
    UBool hasModifier = (type.indexOf(COMMA) != -1);
    
    switch(kw) {

        // number
    case 1: case 2:

        result = Formattable::kDouble;
        if(hasModifier) {
            UnicodeString modifier((UChar*)pat + openBrace + 1 + 1 + 2 + matchLen, 
                                   len - 2 - matchLen - 1, 
                                   len - 2 - matchLen - 1);
    
            switch(findKeyword(modifier, g_umsgModifierList, matchLen)) {
     
                // integer
            case 5: case 6:
                result = Formattable::kLong;
                break;
            }
        }
        break;
      
        // date
    case 3: case 4:
        // time
    case 5: case 6:
        result = Formattable::kDate;
        break;

        // choice      
    case 7: case 8:
        result = Formattable::kDouble;
        break;
    }
  }
  
  return result;
}

  
// ==========
// This code section is entirely bogus.  I just need an eeasy way to
// convert from string to an int, and I can't use the standard library

static NumberFormat *fgNumberFormat = 0;

static NumberFormat* 
umsg_getNumberFormat(UErrorCode& status)
{
  NumberFormat *theFormat = 0;
  
  if(fgNumberFormat != 0) {
    Mutex lock;
    
    if(fgNumberFormat != 0) {
      theFormat = fgNumberFormat;
      fgNumberFormat = 0; // We have exclusive right to this formatter.
    }
  }
  
  if(theFormat == 0) {
    theFormat = NumberFormat::createInstance(Locale::US, status);
    if(U_FAILURE(status))
      return 0;
    theFormat->setParseIntegerOnly(TRUE);
  }
  
  return theFormat;
}

static void 
umsg_releaseNumberFormat(NumberFormat *adopt)
{
  if(fgNumberFormat == 0) {
    Mutex lock;
    
    if(fgNumberFormat == 0) {
      fgNumberFormat = adopt;
      adopt = 0;
    }
  }
  
  delete adopt;
}

static int32_t
umsg_stoi(const UnicodeString& string,
      UErrorCode& status)
{
  NumberFormat *myFormat = umsg_getNumberFormat(status);
  
  if(U_FAILURE(status))
    return -1; // OK?
  
  Formattable result;
  // Uses the global number formatter to parse the string.
  // Note: We assume here that parse() is thread-safe.
  myFormat->parse(string, result, status);
  umsg_releaseNumberFormat(myFormat);
  
  int32_t value = 0;
  if(U_SUCCESS(status) && result.getType() == Formattable::kLong)
    value = result.getLong();
  
  return value;
}


UnicodeString&
umsg_itos(int32_t i,
      UnicodeString& string)
{
  UErrorCode status = U_ZERO_ERROR;
  NumberFormat *myFormat = umsg_getNumberFormat(status);
  
  if(U_FAILURE(status))
    return (string = "<ERROR>");
  
  myFormat->format(i, string);
  umsg_releaseNumberFormat(myFormat);
  
  return string;
}

void
umsg_parseFormattableArgs(const UChar* pattern,
                          int32_t patLen, 
                          Formattable* args,
                          Formattable::Type *argTypes, 
                          int32_t *count,
                          va_list     ap,
                          UErrorCode* status)
{
    // ========================================
    // Begin pseudo-parser
    
    // This is a simplified version of the C++ pattern parser
    // All it does is look for an unquoted '{' and read the type
    
    int32_t     part           = 0;
    UBool       inQuote        = FALSE;
    int32_t     braceStack     = 0;
    const UChar *pat           = pattern;
    const UChar *patLimit      = pattern + patLen;
    int32_t     bracePos       = 0;

     // pseudo-parse the pattern
    while(pat < patLimit) {
        if(part == 0) {
            if(*pat == SINGLE_QUOTE) {
                // handle double quotes
                if( (pat + 1) < patLimit && *(pat + 1) == SINGLE_QUOTE)
                    pat++;
                else
                    inQuote = ! inQuote;
            }  
            else if(*pat == LEFT_CURLY_BRACE && ! inQuote) {
                part = 1;
                bracePos = (pat - pattern);
            }
        }
        else if(inQuote) {              // just copy quotes in parts
            if(*pat == SINGLE_QUOTE)
                inQuote = FALSE;
        } 
        else {
            switch (*pat) {
                
            case COMMA : //','
                if(part < 3)
                    part += 1;
                break;
                
            case LEFT_CURLY_BRACE: //'{'
                ++braceStack;
                break;
                
            case RIGHT_CURLY_BRACE //'}'
                if(braceStack == 0) {
                    part = 0;
                    // found a close brace, determine the argument type enclosed
                    // and the numeric ID of the argument
                    Formattable::Type type = 
                        matchType(pattern, bracePos, (pat - pattern));
                    
                    // the numeric ID is important, because if the pattern has a 
                    // section like "{0} {0} {0}" we only want to get one argument
                    // from the variable argument list, despite the fact that
                    // it is in the pattern three times
                    int32_t argNum = umsg_stoi(pattern + bracePos + 1, *status);
                    
                    if(argNum >= MAX_ARGS) {
                        *status = U_INTERNAL_PROGRAM_ERROR;
                     //   return -1;
                    }
                    
                    // register the type of this argument in our list
                    argTypes[argNum] = type;
                    
                    // adjust argument count
                    *count = ( (argNum + 1) > *count ? (argNum + 1) : *count);
                }
                else
                    --braceStack;
                break;
                
            case SINGLE_QUOTE:  //'\''
                inQuote = TRUE;
                break;
            }
        }
        
        // increment position in pattern
        pat++;
    } 
    
    // detect any unmatched braces in the pattern
    if(braceStack == 0 && part != 0) {
        *status = U_INVALID_FORMAT_ERROR;
        //return -1;
    }
        // iterate through the vararg list, and get the arguments out
    for(int32_t i = 0; i < *count; ++i) {
        
        UChar *stringVal;
        
        switch(argTypes[i]) {
        case Formattable::kDate:
            args[i].setDate(va_arg(ap, UDate));
            break;
            
        case Formattable::kDouble:
            args[i].setDouble(va_arg(ap, double));
            break;
            
        case Formattable::kLong:
            args[i].setLong(va_arg(ap, int32_t));
            break;
            
        case Formattable::kString:
            // For some reason, a temporary is needed
            stringVal = va_arg(ap, UChar*);
            args[i].setString(stringVal);
            break;
            
        case Formattable::kArray:
            // throw away this argument
            // this is highly platform-dependent, and probably won't work
            // so, if you try to skip arguments in the list (and not use them)
            // you'll probably crash
            va_arg(ap, int);
            break;
        }
    }
    
    // End pseudo-parser
    // ========================================

}
U_CAPI int32_t
u_vformatMessage(   const char  *locale,
                    const UChar *pattern,
                    int32_t     patternLength,
                    UChar       *result,
                    int32_t     resultLength,
                    va_list     ap,
                    UErrorCode  *status)

{
    if(U_FAILURE(*status)) return -1;
    
    int32_t patLen = (patternLength == -1 ? u_strlen(pattern) : patternLength);
    
    int32_t     count          = 0;
    Formattable       args     [ MAX_ARGS ];
    Formattable::Type argTypes [ MAX_ARGS ];
    
    
    // set the types to a bogus value initially (no such type as kArray from C)
    for(int32_t j = 0; j < MAX_ARGS; ++j)
        argTypes[j] = Formattable::kArray;
    
    umsg_parseFormattableArgs(pattern,patLen,args,argTypes,&count,ap,status);
    
    if(U_FAILURE(*status))
    {
        return -1;
    }
    
    
    // just call through to the C++ implementation
    UnicodeString patString((UChar*)pattern, patLen, patLen);
    MessageFormat fmt(patString, Locale(locale), *status);
    UnicodeString res(result, 0, resultLength);
    FieldPosition fp;
    fmt.format(args, count, res, fp, *status);
    
    return uprv_fillOutputString(res, result, resultLength, status);
}
U_CAPI void
u_vparseMessage(const char  *locale,
                const UChar *pattern,
                int32_t     patternLength,
                const UChar *source,
                int32_t     sourceLength,
                va_list     ap,
                UErrorCode  *status)
{
  if(U_FAILURE(*status)) return;

  int32_t patLen = (patternLength == -1 ? u_strlen(pattern) : patternLength);
  int32_t srcLen = (sourceLength == -1 ? u_strlen(source) : sourceLength);
  
  UnicodeString patString((UChar*)pattern, patLen, patLen);
  MessageFormat fmt(patString, Locale(locale), *status);
  UnicodeString srcString((UChar*)source, srcLen, srcLen);
  int32_t count = 0;
  Formattable *args = fmt.parse(srcString, count, *status);

  UDate *aDate;
  double *aDouble;
  UChar *aString;
  UnicodeString temp;

  // assign formattables to varargs
  for(int32_t i = 0; i < count; i++) {
    switch(args[i].getType()) {
      
    case Formattable::kDate:
      aDate = va_arg(ap, UDate*);
      *aDate = args[i].getDate();
      break;
      
    case Formattable::kDouble:
      aDouble = va_arg(ap, double*);
      *aDouble = args[i].getDouble();
      break;
      
    case Formattable::kLong:
      // always assume doubles for parsing
      aDouble = va_arg(ap, double*);
      *aDouble = (double) args[i].getLong();
      break;
      
    case Formattable::kString:
      aString = va_arg(ap, UChar*);
      args[i].getString(temp);
      u_strcpy(aString, temp.getUChars());
      break;
      
      // better not happen!
    case Formattable::kArray:
      // DIE
      break;
    }
  }
  
  // clean up
  delete [] args;
}
*/

// ==========
// Eventually, message format should be rewritten natively in C.
// For now, this is a hack that should work:
//  1. Parse the pattern, determining the argument types
//  2. Create a Formattable array with the varargs
//  3. Call through to the existing C++ code
//
// Right now this imposes the same limit as MessageFormat in C++
// Namely, only MAX_ARGS arguments are supported
U_CAPI int32_t
u_formatMessage(const char  *locale,
                const UChar *pattern,
                int32_t     patternLength,
                UChar       *result,
                int32_t     resultLength,
                UErrorCode  *status,
                ...)
{
  va_list    ap;
  int32_t actLen;
  if(U_FAILURE(*status)) return -1;

  // start vararg processing
  va_start(ap, status);

  actLen = u_vformatMessage(locale,pattern,patternLength,result,resultLength,ap,status);

  // end vararg processing
  va_end(ap);

  return actLen;
}

U_CAPI int32_t
u_vformatMessage(   const char  *locale,
                    const UChar *pattern,
                    int32_t     patternLength,
                    UChar       *result,
                    int32_t     resultLength,
                    va_list     ap,
                    UErrorCode  *status)

{
    UMessageFormat *fmt = umsg_open(pattern,patternLength,locale,NULL,status);
    int32_t retVal = umsg_vformat(fmt,result,resultLength,ap,status);
    umsg_close(fmt);
    return retVal;
}

U_CAPI int32_t
u_formatMessageWithError(const char *locale,
                        const UChar *pattern,
                        int32_t     patternLength,
                        UChar       *result,
                        int32_t     resultLength,
                        UParseError *parseError,
                        UErrorCode  *status,
                        ...)
{
  va_list    ap;
  int32_t actLen;
  if(U_FAILURE(*status)) return -1;

  // start vararg processing
  va_start(ap, status);

  actLen = u_vformatMessageWithError(locale,pattern,patternLength,result,resultLength,parseError,ap,status);

  // end vararg processing
  va_end(ap);

  return actLen;
}

U_CAPI int32_t
u_vformatMessageWithError(  const char  *locale,
                            const UChar *pattern,
                            int32_t     patternLength,
                            UChar       *result,
                            int32_t     resultLength,
                            UParseError *parseError,
                            va_list     ap,
                            UErrorCode  *status)

{
    UMessageFormat *fmt = umsg_open(pattern,patternLength,locale,parseError,status);
    int32_t retVal = umsg_vformat(fmt,result,resultLength,ap,status);
    umsg_close(fmt);
    return retVal;
}


// For parse, do the reverse of format:
//  1. Call through to the C++ APIs
//  2. Just assume the user passed in enough arguments.
//  3. Iterate through each formattable returned, and assign to the arguments
U_CAPI void
u_parseMessage( const char   *locale,
                const UChar  *pattern,
                int32_t      patternLength,
                const UChar  *source,
                int32_t      sourceLength,
                UErrorCode   *status,
                ...)
{
  va_list    ap;

  if(U_FAILURE(*status)) return;

  // start vararg processing
  va_start(ap, status);

  u_vparseMessage(locale,pattern,patternLength,source,sourceLength,ap,status);

  // end vararg processing
  va_end(ap);

}

U_CAPI void
u_vparseMessage(const char  *locale,
                const UChar *pattern,
                int32_t     patternLength,
                const UChar *source,
                int32_t     sourceLength,
                va_list     ap,
                UErrorCode  *status)
{
    UMessageFormat *fmt = umsg_open(pattern,patternLength,locale,NULL,status);
    int32_t count = 0;
    umsg_vparse(fmt,source,sourceLength,&count,ap,status);
    umsg_close(fmt);
}

U_CAPI void
u_parseMessageWithError(const char  *locale,
                        const UChar *pattern,
                        int32_t     patternLength,
                        const UChar *source,
                        int32_t     sourceLength,
                        UParseError *error,
                        UErrorCode  *status,
                        ...)
{
  va_list    ap;

  if(U_FAILURE(*status)) return;

  // start vararg processing
  va_start(ap, status);

  u_vparseMessageWithError(locale,pattern,patternLength,source,sourceLength,ap,error,status);

  // end vararg processing
  va_end(ap);
}
U_CAPI void
u_vparseMessageWithError(const char  *locale,
                         const UChar *pattern,
                         int32_t     patternLength,
                         const UChar *source,
                         int32_t     sourceLength,
                         va_list     ap,
                         UParseError *error,
                         UErrorCode* status)
{
    UMessageFormat *fmt = umsg_open(pattern,patternLength,locale,error,status);
    int32_t count = 0;
    umsg_vparse(fmt,source,sourceLength,&count,ap,status);
    umsg_close(fmt);
}
//////////////////////////////////////////////////////////////////////////////////
//
//  Message format C API
//
/////////////////////////////////////////////////////////////////////////////////


U_CAPI UMessageFormat*
umsg_open(  const UChar     *pattern,
            int32_t         patternLength,
            const  char     *locale,
            UParseError     *parseError,
            UErrorCode      *status)
{
    if(U_FAILURE(*status))
    {
      return 0;
    }
    UParseError tErr;
    
    if(!parseError)
    {
        parseError = &tErr;
    }
        
    UMessageFormat* retVal = 0;

    int32_t len = (patternLength == -1 ? u_strlen(pattern) : patternLength);
    
    UnicodeString patString((patternLength == -1 ? TRUE:FALSE), pattern,len);

    retVal = (UMessageFormat*) new MessageFormat(pattern,Locale(locale),*parseError,*status);
    
    if(retVal == 0) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return 0;
    }
    return retVal;
}

U_CAPI void
umsg_close(UMessageFormat* format)
{
    delete (MessageFormat*) format;
}

U_CAPI UMessageFormat
umsg_clone(const UMessageFormat *fmt,
           UErrorCode *status)
{
    UMessageFormat retVal = (UMessageFormat)((MessageFormat*)fmt)->clone();
    if(retVal == 0) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return 0;
    }
    return retVal;    
}

U_CAPI void 
umsg_setLocale(UMessageFormat *fmt, const char* locale)
{
    ((MessageFormat*)fmt)->setLocale(Locale(locale));   
}

U_CAPI const char* 
umsg_getLocale(UMessageFormat *fmt)
{
    return ((MessageFormat*)fmt)->getLocale().getName();
}

U_CAPI void 
umsg_applyPattern(UMessageFormat *fmt,
                           const UChar* pattern,
                           int32_t patternLength,
                           UParseError* parseError,
                           UErrorCode* status)
{
  UParseError tErr;
  if(!parseError)
  {
      parseError = &tErr;
  }
  
  ((MessageFormat*)fmt)->applyPattern(UnicodeString(pattern,patternLength),*parseError,*status);  
}

U_CAPI int32_t 
umsg_toPattern(UMessageFormat *fmt,
               UChar* result, 
               int32_t resultLength,
               UErrorCode* status)
{


    UnicodeString res(result, 0, resultLength);
    ((MessageFormat*)fmt)->toPattern(res);
    return res.extract(result, resultLength, *status);
}

U_CAPI int32_t
umsg_format(    UMessageFormat *fmt,
                UChar          *result,
                int32_t        resultLength,
                UErrorCode     *status,
                ...)
{
    va_list    ap;
    int32_t actLen;
    
    if(U_FAILURE(*status))
    {
        return -1;
    }

    // start vararg processing
    va_start(ap, status);

    actLen = umsg_vformat(fmt,result,resultLength,ap,status);

    // end vararg processing
    va_end(ap);

    return actLen;
}

U_CAPI int32_t
umsg_vformat(   UMessageFormat *fmt,
                UChar          *result,
                int32_t        resultLength,
                va_list        ap,
                UErrorCode     *status)
{

    if(U_FAILURE(*status))
    {
        return -1;
    }
    int32_t count =0;
    const Formattable::Type* argTypes = ((MessageFormat*)fmt)->getFormatTypeList(count);
    Formattable args[MessageFormat::kMaxFormat];

    // iterate through the vararg list, and get the arguments out
    for(int32_t i = 0; i < count; ++i) {
        
        UChar *stringVal;
        double tDouble=0;
        int32_t tInt =0;
        UDate tempDate = 0;
        switch(argTypes[i]) {
        case Formattable::kDate:
            tempDate = va_arg(ap, UDate);
            args[i].setDate(tempDate);
            break;
            
        case Formattable::kDouble:
            tDouble =va_arg(ap, double);
            args[i].setDouble(tDouble);
            break;
            
        case Formattable::kLong:
            tInt = va_arg(ap, int32_t);
            args[i].setLong(tInt);
            break;
            
        case Formattable::kString:
            // For some reason, a temporary is needed
            stringVal = va_arg(ap, UChar*);
            args[i].setString(stringVal);
            break;
            
        case Formattable::kArray:
            // throw away this argument
            // this is highly platform-dependent, and probably won't work
            // so, if you try to skip arguments in the list (and not use them)
            // you'll probably crash
            va_arg(ap, int);
            break;

        }
    }
    UnicodeString resultStr;
    FieldPosition fieldPosition(0);
    
    /* format the message */
    ((MessageFormat*)fmt)->format(args,count,resultStr,fieldPosition,*status);

    if(U_FAILURE(*status)){
        return -1;
    }

    if(result ==NULL || resultLength < resultStr.length()){
        *status = U_BUFFER_OVERFLOW_ERROR;
    }else{
        /* copy the resultStr to target buffer */
        //u_strcpy(result, resultStr.getUChars());
        int len = resultStr.length();
        resultStr.extract(0,len,result);
        result[len]=0;/* null terminate the string */
    }
    return resultStr.length();
}

U_CAPI void
umsg_parse( UMessageFormat *fmt,
            const UChar    *source,
            int32_t        sourceLength,
            int32_t        *count,
            UErrorCode     *status,
            ...)
{
    va_list    ap;

    if(U_FAILURE(*status))
    {
        return;
    }
    // start vararg processing
    va_start(ap, status);

    umsg_vparse(fmt,source,sourceLength,count,ap,status);

    // end vararg processing
    va_end(ap);
}

U_CAPI void
umsg_vparse(UMessageFormat *fmt,
            const UChar    *source,
            int32_t        sourceLength,
            int32_t        *count,
            va_list        ap,
            UErrorCode     *status)
{
    UnicodeString srcString(source,sourceLength);
    Formattable *args = ((MessageFormat*)fmt)->parse(source,*count,*status);
    if(U_FAILURE(*status))
    {
        return;
    }

    UDate *aDate;
    double *aDouble;
    UChar *aString;
    UnicodeString temp;
    int len =0;
    // assign formattables to varargs
    for(int32_t i = 0; i < *count; i++) {
        switch(args[i].getType()) {

        case Formattable::kDate:
            aDate = va_arg(ap, UDate*);
            *aDate = args[i].getDate();
            break;

        case Formattable::kDouble:
            aDouble = va_arg(ap, double*);
            *aDouble = args[i].getDouble();
            break;

        case Formattable::kLong:
            // always assume doubles for parsing
            aDouble = va_arg(ap, double*);
            *aDouble = (double) args[i].getLong();
            break;

        case Formattable::kString:
            aString = va_arg(ap, UChar*);
            args[i].getString(temp);
            //u_strcpy(aString, temp.getUChars());
            len = temp.length();
            temp.extract(0,len,aString);
            aString[len]=0;
            break;

        // better not happen!
        case Formattable::kArray:
            // DIE
            break;
        }
    }

    // clean up
    delete [] args;
}


