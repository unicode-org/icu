/*
*******************************************************************************
*
*   Copyright (C) 2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File prscmnts.cpp
*
* Modification History:
*
*   Date          Name        Description
*   08/22/2003    ram         Creation.
*******************************************************************************
*/
#include "unicode/regex.h"
#include "unicode/unistr.h"
#include "unicode/parseerr.h"
#include "prscmnts.h"

U_CFUNC int32_t 
remove(UChar *source, int32_t srcLen, UnicodeString patString,uint32_t options,  UErrorCode *status){
    if(status == NULL || U_FAILURE(*status)){
        return 0;
    }

    UnicodeString src(source, srcLen);

    RegexMatcher    myMatcher(patString, src, options, *status);
    if(U_FAILURE(*status)){
        return 0;
    }
    UnicodeString dest;


    dest = myMatcher.replaceAll("",*status);
    
    
    return dest.extract(source, srcLen, *status);

}
U_CFUNC int32_t
trim(UChar *src, int32_t srcLen, UErrorCode *status){
     srcLen = remove(src, srcLen, "^[ \\r\\n]+ ", 0,  status); // remove leading new lines
     srcLen = remove(src, srcLen, "^\\s+", 0, status); // remove leading spaces
     srcLen = remove(src, srcLen, "\\s+$", 0,  status); // remvoe trailing spcaes
     return srcLen;
}

U_CFUNC int32_t 
removeCmtText(UChar* source, int32_t srcLen, UErrorCode* status){
    srcLen = trim(source, srcLen, status);
    UnicodeString     patString = "^\\s*?\\*\\s*?";     // remove pattern like " * " at the begining of the line
    return remove(source, srcLen, patString, UREGEX_MULTILINE, status);
}

U_CFUNC int32_t 
getText(const UChar* source, int32_t srcLen,
        UChar** dest, int32_t destCapacity,
        UnicodeString patternString, 
        UErrorCode* status){
    
    if(status == NULL || U_FAILURE(*status)){
        return NULL;
    }

    UnicodeString     stringArray[3];
    RegexPattern      *pattern = RegexPattern::compile("@", 0, *status);
    UnicodeString src = source;
    
    if (U_FAILURE(*status)) {
        return 0;
    }
    pattern->split(src, stringArray, 3, *status);
    
    RegexMatcher matcher(patternString, 0, *status);
    if (U_FAILURE(*status)) {
        return 0;
    }
    for(int32_t i=0; i<3; i++){
        matcher.reset(stringArray[i]);
        if(matcher.lookingAt(*status)){
            UnicodeString out = matcher.group(1, *status);

            return out.extract(*dest, destCapacity,*status);
        }
    }
    return 0;
}


#define AT_SIGN  0x0040

U_CFUNC int32_t
getDescription( const UChar* source, int32_t srcLen,
                UChar** dest, int32_t destCapacity,
                UErrorCode* status){
    if(status == NULL || U_FAILURE(*status)){
        return NULL;
    }

    UnicodeString     stringArray[3];
    RegexPattern      *pattern = RegexPattern::compile("@", 0, *status);
    UnicodeString src = source;
    
    if (U_FAILURE(*status)) {
        return 0;
    }
    pattern->split(src, stringArray, 3, *status);

    if(stringArray[0].indexOf((UChar)AT_SIGN)==-1){
        int32_t destLen =  stringArray[0].extract(*dest, destCapacity, *status);
        return trim(*dest, destLen, status);
    }
    return 0;
}

U_CFUNC int32_t
getTranslate( const UChar* source, int32_t srcLen,
              UChar** dest, int32_t destCapacity,
              UErrorCode* status){
    UnicodeString     notePatternString = "^translate\\s*?(.*)"; 
    
    int32_t destLen = getText(source, srcLen, dest, destCapacity, notePatternString, status);
    return trim(*dest, destLen, status);
}

U_CFUNC int32_t 
getNote(const UChar* source, int32_t srcLen,
        UChar** dest, int32_t destCapacity,
        UErrorCode* status){

    UnicodeString     notePatternString = "^note\\s*?(.*)"; 
    int32_t destLen =  getText(source, srcLen, dest, destCapacity, notePatternString, status);
    return trim(*dest, destLen, status);

}