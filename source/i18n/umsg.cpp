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
#include "unicode/ustring.h"
#include "unicode/fmtable.h"
#include "cpputils.h"
#include "unicode/msgfmt.h"
#include "unicode/unistr.h"

U_NAMESPACE_USE

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

    if(status==0 || U_FAILURE(*status))
    {
        return -1;
    }
    if(resultLength<0 || (resultLength>0 && result==0)) {
        *status=U_ILLEGAL_ARGUMENT_ERROR;
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

    return resultStr.extract(result, resultLength, *status);
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


