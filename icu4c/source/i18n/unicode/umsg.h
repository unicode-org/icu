/*
*******************************************************************************
* Copyright © {1996-1999}, International Business Machines Corporation and others. All Rights Reserved.
*******************************************************************************
*/

#ifndef UMSG_H
#define UMSG_H

#include "unicode/utypes.h"
#include <stdarg.h>
/**
 * @name Message Format C API
 *
 * Provides means to produce concatenated messages in language-neutral way.
 * Use this for all concatenations that show up to end users.
 * <P>
 * Takes a set of objects, formats them, then inserts the formatted
 * strings into the pattern at the appropriate places.
 * <P>
 * Here are some examples of usage:
 * Example 1:
 * <pre>
 * .    UChar *result, *tzID, *str;
 * .    UChar pattern[100];
 * .    t_int32 resultLengthOut, resultlength;
 * .    UCalendar *cal;
 * .    UDate d1;
 * .    UDateFormat *def1;
 * .    UErrorCode status = U_ZERO_ERROR;
 * .    str=(UChar*)malloc(sizeof(UChar) * (strlen("disturbance in force") +1));
 * .    u_uastrcpy(str, "disturbance in force");
 * .    tzID=(UChar*)malloc(sizeof(UChar) * 4);
 * .    u_uastrcpy(tzID, "PST");
 * .    cal=ucal_open(tzID, u_strlen(tzID), "en_US", UCAL_TRADITIONAL, &status);
 * .    ucal_setDateTime(cal, 1999, UCAL_MARCH, 18, 0, 0, 0, &status);
 * .    d1=ucal_getMillis(cal, &status);
 * .    u_uastrcpy(pattern, "On {0, date, long}, there was a {1} on planet {2,number,integer}");
 * .    resultlength=0;
 * .    resultLengthOut=u_formatMessage( "en_US", pattern, u_strlen(pattern), NULL, resultlength, &status, d1, str, 7);
 * .    if(status==U_BUFFER_OVERFLOW_ERROR){
 * .        status=U_ZERO_ERROR;
 * .        resultlength=resultLengthOut+1;
 * .        result=(UChar*)realloc(result, sizeof(UChar) * resultlength);
 * .        u_formatMessage( "en_US", pattern, u_strlen(pattern), result, resultlength, &status, d1, str, 7);
 * .    }
 * .    printf("%s\n", austrdup(result) );//austrdup( a function used to convert UChar* to char*)
 * .    //output>: "On March 18, 1999, there was a disturbance in force on planet 7
 * </pre>  
 * Typically, the message format will come from resources, and the
 * arguments will be dynamically set at runtime.
 * <P>
 * Example 2:
 * <pre>
 * .    UChar* str;
 * .    UErrorCode status = U_ZERO_ERROR;
 * .    UChar *result;
 * .    UChar pattern[100];
 * .    t_int32 resultlength,resultLengthOut, i;
 * .    double testArgs= { 100.0, 1.0, 0.0};
 * .    str=(UChar*)malloc(sizeof(UChar) * 10);
 * .    u_uastrcpy(str, "MyDisk");
 * .    u_uastrcpy(pattern, "The disk {1} contains {0,choice,0#no files|1#one file|1<{0,number,integer} files}");
 * .    for(i=0; i<3; i++){
 * .      resultlength=0;
 * .    resultLengthOut=u_formatMessage( "en_US", pattern, u_strlen(pattern), NULL, resultlength, &status, testArgs[i], str);
 * .    if(status==U_BUFFER_OVERFLOW_ERROR){
 * .        status=U_ZERO_ERROR;
 * .        resultlength=resultLengthOut+1;
 * .        result=(UChar*)malloc(sizeof(UChar) * resultlength);
 * .        u_formatMessage( "en_US", pattern, u_strlen(pattern), result, resultlength, &status, testArgs[i], str);
 * .    }
 * .    printf("%s\n", austrdup(result) );  //austrdup( a function used to convert UChar* to char*)
 * .    free(result);
 * .    }
 * .    // output, with different testArgs:
 * .    // output: The disk "MyDisk" contains 100 files.
 * .    // output: The disk "MyDisk" contains one file.
 * .    // output: The disk "MyDisk" contains no files.
 *  </pre>
 *
 *  The pattern is of the following form.  Legend:
 *  <pre>
 * .      {optional item}
 * .      (group that may be repeated)*
 *  </pre>
 *  Do not confuse optional items with items inside quotes braces, such
 *  as this: "{".  Quoted braces are literals.
 *  <pre>
 * .      messageFormatPattern := string ( "{" messageFormatElement "}" string )*
 * .       
 * .      messageFormatElement := argument { "," elementFormat }
 * .       
 * .      elementFormat := "time" { "," datetimeStyle }
 * .                     | "date" { "," datetimeStyle }
 * .                     | "number" { "," numberStyle }
 * .                     | "choice" "," choiceStyle
 * .  
 * .      datetimeStyle := "short"
 * .                     | "medium"
 * .                     | "long"
 * .                     | "full"
 * .                     | dateFormatPattern
 * .
 * .      numberStyle :=   "currency"
 * .                     | "percent"
 * .                     | "integer"
 * .                     | numberFormatPattern
 * . 
 * .      choiceStyle :=   choiceFormatPattern
 * </pre>
 * If there is no elementFormat, then the argument must be a string,
 * which is substituted. If there is no dateTimeStyle or numberStyle,
 * then the default format is used (e.g.  NumberFormat.getInstance(),
 * DateFormat.getDefaultTime() or DateFormat.getDefaultDate(). For
 * a ChoiceFormat, the pattern must always be specified, since there
 * is no default.
 * <P>
 * In strings, single quotes can be used to quote the "{" sign if
 * necessary. A real single quote is represented by ''.  Inside a
 * messageFormatElement, quotes are [not] removed. For example,
 * {1,number,$'#',##} will produce a number format with the pound-sign
 * quoted, with a result such as: "$#31,45".
 * <P>
 * If a pattern is used, then unquoted braces in the pattern, if any,
 * must match: that is, "ab {0} de" and "ab '}' de" are ok, but "ab
 * {0'}' de" and "ab } de" are not.
 * <P>
 * The argument is a number from 0 to 9, which corresponds to the
 * arguments presented in an array to be formatted.
 * <P>
 * It is ok to have unused arguments in the array.  With missing
 * arguments or arguments that are not of the right class for the
 * specified format, a failing UErrorCode result is set.
 * <P>
 
 * <P>
 * [Note:] As we see above, the string produced by a choice Format in
 * MessageFormat is treated specially; occurances of '{' are used to
 * indicated subformats.  
 * <P>
 * [Note:] Formats are numbered by order of variable in the string.
 * This is [not] the same as the argument numbering!
 * <pre>
 * .   For example: with "abc{2}def{3}ghi{0}...",
 * .   
 * .   format0 affects the first variable {2}
 * .   format1 affects the second variable {3}
 * .   format2 affects the second variable {0}
 * </pre>
 * and so on.
 */

/**
* Format a message for a locale.
* This function may perform re-ordering of the arguments depending on the
* locale. For all numeric arguments, double is assumed unless the type is
* explicitly integer.  All choice format arguments must be of type double.
* @param locale The locale for which the message will be formatted
* @param pattern The pattern specifying the message's format
* @param patternLength The length of pattern
* @param result A pointer to a buffer to receive the formatted message.
* @param resultLength The maximum size of result.
* @param status A pointer to an UErrorCode to receive any errors
* @param ... A variable-length argument list containing the arguments specified
* in pattern.
* @return The total buffer size needed; if greater than resultLength, the 
* output was truncated.
* @see u_parseMessage
*/
U_CAPI int32_t
u_formatMessage(    const    char        *locale,
            const    UChar        *pattern,
                int32_t        patternLength,
                UChar        *result,
                int32_t        resultLength,
                UErrorCode    *status,
                ...);
 
/**
* Format a message for a locale.
* This function may perform re-ordering of the arguments depending on the
* locale. For all numeric arguments, double is assumed unless the type is
* explicitly integer.  All choice format arguments must be of type double.
* @param locale The locale for which the message will be formatted
* @param pattern The pattern specifying the message's format
* @param patternLength The length of pattern
* @param result A pointer to a buffer to receive the formatted message.
* @param resultLength The maximum size of result.
* @param ap A variable-length argument list containing the arguments specified
* @param status A pointer to an UErrorCode to receive any errors
* in pattern.
* @return The total buffer size needed; if greater than resultLength, the 
* output was truncated.
* @see u_parseMessage
*/
U_CAPI int32_t
u_vformatMessage(    const    char        *locale,
            const    UChar        *pattern,
                int32_t        patternLength,
                UChar        *result,
                int32_t        resultLength,
                va_list       ap,
                UErrorCode    *status);
/**
* Parse a message.
* For numeric arguments, this function will always use doubles.  Integer types
* should not be passed.  
* This function is not able to parse all output from \Ref{u_formatMessage}.
* @param locale The locale for which the message is formatted
* @param pattern The pattern specifying the message's format
* @param patternLength The length of pattern
* @param source The text to parse.
* @param sourceLength The length of source, or -1 if null-terminated.
* @param status A pointer to an UErrorCode to receive any errors
* @param ... A variable-length argument list containing the arguments
* specified in pattern.
* @see u_formatMessage
*/
U_CAPI void 
u_parseMessage(    const    char        *locale,
        const    UChar        *pattern,
            int32_t        patternLength,
        const    UChar        *source,
            int32_t        sourceLength,
            UErrorCode    *status,
            ...);

/**
* Parse a message.
* For numeric arguments, this function will always use doubles.  Integer types
* should not be passed.  
* This function is not able to parse all output from \Ref{u_formatMessage}.
* @param locale The locale for which the message is formatted
* @param pattern The pattern specifying the message's format
* @param patternLength The length of pattern
* @param source The text to parse.
* @param sourceLength The length of source, or -1 if null-terminated.
* @param ap A variable-length argument list containing the arguments
* @param status A pointer to an UErrorCode to receive any errors
* specified in pattern.
* @see u_formatMessage
*/
U_CAPI void 
u_vparseMessage(    const    char        *locale,
        const    UChar        *pattern,
            int32_t        patternLength,
        const    UChar        *source,
            int32_t        sourceLength,
            va_list       ap,
            UErrorCode    *status);

#endif
