/*
**********************************************************************
*   Copyright (C) 1997-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*
* File USCRIPT.C
*
* Modification History:
*
*   Date        Name        Description
*   07/06/2001    Ram         Creation.
******************************************************************************
*/

#include "unicode/uscript.h"
#include "unicode/ures.h"
#include "cstring.h"

#define USCRIPT_NAMES_ARRAY_SIZE 38
#define USCRIPT_ABBR_ARRAY_SIZE 41

static const char kLocaleScript[] = "LocaleScript";

static const char * const scriptNames[]={
        "ARABIC",     /* USCRIPT_ARABIC     */
        "ARMENIAN",   /* USCRIPT_ARMENIAN   */
        "BENGALI",    /* USCRIPT_BENGALI    */
        "BOPOMOFO",   /* USCRIPT_BOPOMOFO   */
        "CHEROKEE",   /* USCRIPT_CHEROKEE   */
        "CYRILLIC",   /* USCRIPT_CYRILLIC   */
        "DESERET",    /* USCRIPT_DESERET    */
        "DEVANAGARI", /* USCRIPT_DEVANAGARI */
        "ETHIOPIC",   /* USCRIPT_ETHIOPIC   */
        "GEORGIAN",   /* USCRIPT_GEORGIAN   */
        "GOTHIC",     /* USCRIPT_GOTHIC     */
        "GREEK",      /* USCRIPT_GREEK      */
        "GUJARATI",   /* USCRIPT_GUJARATI   */
        "GURMUKHI",   /* USCRIPT_GURMUKHI   */
        "HAN",        /* USCRIPT_HAN        */
        "HANGUL",     /* USCRIPT_HANGUL     */
        "HEBREW",     /* USCRIPT_HEBREW     */
        "HIRAGANA",   /* USCRIPT_HIRAGANA   */
        "INHERITED",  /* USCRIPT_INHERITED  */
        "KANNADA",    /* USCRIPT_KANNADA    */
        "KATAKANA",   /* USCRIPT_KATAKANA   */
        "KHMER",      /* USCRIPT_KHMER      */
        "LATIN",      /* USCRIPT_LATIN      */
        "MALAYALAM",  /* USCRIPT_MALAYALAM  */
        "MONGOLIAN",  /* USCRIPT_MONGOLIAN  */
        "MYANMAR",    /* USCRIPT_MYANMAR    */
        "OGHAM",      /* USCRIPT_OGHAM      */
        "OLD_ITALIC", /* USCRIPT_OLD_ITALIC */
        "ORIYA",      /* USCRIPT_ORIYA      */
        "RUNIC",      /* USCRIPT_RUNIC      */
        "SINHALA",    /* USCRIPT_SINHALA    */
        "SYRIAC",     /* USCRIPT_SYRIAC     */
        "TAMIL",      /* USCRIPT_TAMIL      */
        "TELUGU",     /* USCRIPT_TELUGU     */
        "THAANA",     /* USCRIPT_THANA      */
        "THAI",       /* USCRIPT_THAI       */
        "TIBETAN",    /* USCRIPT_TIBETAN    */
        "UCAS",       /* USCRIPT_UCAS       */
    
};

static const char * const scriptAbbr[]= {
        "Arab",       /* USCRIPT_ARABIC     */
        "Armn",       /* USCRIPT_ARMENIAN   */
        "Beng",       /* USCRIPT_BENGALI    */
        "Bopo",       /* USCRIPT_BOPOMOFO   */
        "Cans",       /* USCRIPT_UCAS       */
        "Cher",       /* USCRIPT_CHEROKEE   */
        "Cyrl",       /* USCRIPT_CYRILLIC   */
       /* "Cyrs",  */ /* USCRIPT_CYRILLIC   */
        "Deva",       /* USCRIPT_DEVANAGARI */
        "Dsrt",       /* USCRIPT_DESERET    */
        "Ethi",       /* USCRIPT_ETHIOPIC   */
       /* "Geoa",  */ /* USCRIPT_GEORGIAN   */
       /* "Geon",  */ /* USCRIPT_GEORGIAN   */
        "Geor",       /* USCRIPT_GEORGIAN   */
        "Goth",       /* USCRIPT_GOTHIC     */
        "Grek",       /* USCRIPT_GREEK      */
        "Gujr",       /* USCRIPT_GUJARATI   */
        "Guru",       /* USCRIPT_GURMUKHI   */
        "Hang",       /* USCRIPT_HANGUL     */
        "Hani",       /* USCRIPT_HAN        */
        "Hebr",       /* USCRIPT_HEBREW     */
        "Hira",       /* USCRIPT_HIRAGANA   */
        "Ital",       /* USCRIPT_OLD_ITALIC */
        "Kana",       /* USCRIPT_KATAKANA   */
        "Khmr",       /* USCRIPT_KHMER      */
        "Knda",       /* USCRIPT_KANNADA    */
        "Lao",        /* USCRIPT_LAO        */
        /*"Laoo",  */ /* USCRIPT_LAO        */
        /*"Latf",  */ /* USCRIPT_LATIN      */
        /*"Latg",  */ /* USCRIPT_LATIN      */
        "Latn",       /* USCRIPT_LATIN      */
        "Mlym",       /* USCRIPT_MALAYALAM  */
        "Mong",       /* USCRIPT_MONGOLIAN  */
        "Mymr",       /* USCRIPT_MYANMAR    */
        "Ogam",       /* USCRIPT_OGHAM      */
        "Orya",       /* USCRIPT_ORIYA      */
        "Qaac",       /* USCRIPT_COPTIC     */
        "Qaai",       /* USCRIPT_INHERITED  */
        "Runr",       /* USCRIPT_RUNIC      */
        "Sinh",       /* USCRIPT_SINHALA    */
        "Syrc",       /* USCRIPT_SYRIAC     */
       /* "Syre",  */ /* USCRIPT_SYRIAC     */
       /* "Syrj",  */ /* USCRIPT_SYRIAC     */
       /* "Syrn",  */ /* USCRIPT_SYRIAC     */
        "Taml",       /* USCRIPT_TAMIL      */
        "Telu",       /* USCRIPT_TELUGU     */
        "Thaa",       /* USCRIPT_THANA      */
        "Thai",       /* USCRIPT_THAI       */
        "Tibt",       /* USCRIPT_TIBETAN    */
        "Yiii",       /* USCRIPT_YI         */
        "Zyyy",       /* USCRIPT_COMMON     */    
    };


static const UScriptCode scriptNameCodes[]= {
         USCRIPT_ARABIC     ,
         USCRIPT_ARMENIAN   ,
         USCRIPT_BENGALI    ,
         USCRIPT_BOPOMOFO   ,
         USCRIPT_CHEROKEE   ,
         USCRIPT_CYRILLIC   ,
         USCRIPT_DESERET    ,
         USCRIPT_DEVANAGARI ,
         USCRIPT_ETHIOPIC   ,
         USCRIPT_GEORGIAN   ,
         USCRIPT_GOTHIC     ,
         USCRIPT_GREEK      ,
         USCRIPT_GUJARATI   ,
         USCRIPT_GURMUKHI   ,
         USCRIPT_HAN        ,
         USCRIPT_HANGUL     ,
         USCRIPT_HEBREW     ,
         USCRIPT_HIRAGANA   ,
         USCRIPT_INHERITED  ,
         USCRIPT_KANNADA    ,
         USCRIPT_KATAKANA   ,
         USCRIPT_KHMER      ,
         USCRIPT_LATIN      ,
         USCRIPT_MALAYALAM  ,
         USCRIPT_MONGOLIAN  ,
         USCRIPT_MYANMAR    ,
         USCRIPT_OGHAM      ,
         USCRIPT_OLD_ITALIC ,
         USCRIPT_ORIYA      ,
         USCRIPT_RUNIC      ,
         USCRIPT_SINHALA    ,
         USCRIPT_SYRIAC     ,
         USCRIPT_TAMIL      ,
         USCRIPT_TELUGU     ,
         USCRIPT_THAANA     ,
         USCRIPT_THAI       ,
         USCRIPT_TIBETAN    ,
         USCRIPT_UCAS       ,
        
};


static const UScriptCode scriptAbbrCodes[] = {
        USCRIPT_ARABIC     ,
        USCRIPT_ARMENIAN   ,
        USCRIPT_BENGALI    ,
        USCRIPT_BOPOMOFO   ,
        USCRIPT_UCAS       ,
        USCRIPT_CHEROKEE   ,
        USCRIPT_CYRILLIC   ,
       /* USCRIPT_CYRILLIC   , */
        USCRIPT_DEVANAGARI ,
        USCRIPT_DESERET    ,
        USCRIPT_ETHIOPIC   ,
      /*  USCRIPT_GEORGIAN   , */
      /*  USCRIPT_GEORGIAN   , */
        USCRIPT_GEORGIAN   ,
        USCRIPT_GOTHIC     ,
        USCRIPT_GREEK      ,
        USCRIPT_GUJARATI   ,
        USCRIPT_GURMUKHI   ,
        USCRIPT_HANGUL     ,
        USCRIPT_HAN        ,
        USCRIPT_HEBREW     ,
        USCRIPT_HIRAGANA   ,
        USCRIPT_OLD_ITALIC ,
        USCRIPT_KATAKANA   ,
        USCRIPT_KHMER      ,
        USCRIPT_KANNADA    ,
        USCRIPT_LAO        ,
      /*  USCRIPT_LAO        , */
      /*  USCRIPT_LATIN      , */
      /*  USCRIPT_LATIN      , */
        USCRIPT_LATIN      ,
        USCRIPT_MALAYALAM  ,
        USCRIPT_MONGOLIAN  ,
        USCRIPT_MYANMAR    ,
        USCRIPT_OGHAM      ,
        USCRIPT_ORIYA      ,
        USCRIPT_COPTIC     ,
        USCRIPT_INHERITED  ,
        USCRIPT_RUNIC      ,
        USCRIPT_SINHALA    ,
        USCRIPT_SYRIAC     ,
      /*  USCRIPT_SYRIAC     , */
      /*  USCRIPT_SYRIAC     , */
        USCRIPT_SYRIAC     ,
        USCRIPT_TAMIL      ,
        USCRIPT_TELUGU     ,
        USCRIPT_THAANA     ,
        USCRIPT_THAI       ,
        USCRIPT_TIBETAN    ,
        USCRIPT_YI         ,
        USCRIPT_COMMON     , 
};
/* binary search the string array */
static int 
findStringIndex(const char* const *sortedArr, const char* target, int size){
    int left, middle, right,rc;
    left =0;
    right= size-1;
    
    while(left <= right){
        middle = (left+right)/2;
        rc=uprv_stricmp(sortedArr[middle],target);
        if(rc<0){
            left = middle+1;
        }else if(rc >0){
            right = middle -1;
        }else{
            return middle;
        }
    }
    return -1;
}
/*
static int 
findCodeIndex(const UScriptCode sorted[], const UScriptCode target, int size){
    int left, middle, right;
    left =0;
    right= size-1;
    while(left <= right){
        middle = (left+right)/2;
        if(sorted[middle] < target){
            left=middle+1;
        }else if(sorted[middle]>target){
            right=middle-1;
        }else{
            return middle;
        }
    }
    return -1;
}
*/
/* linearly search the array and return the index */
static int
findCodeIndex(const UScriptCode unsorted[], const UScriptCode target, int size){
    int i=0;
    while(i<size){
        if(target == unsorted[i]){
            return i;
        }
        i++;
    }
    return -1;
}

U_CAPI UScriptCode 
uscript_getCode(const char* nameOrAbbrOrLocale, UErrorCode* err){
    UScriptCode code = USCRIPT_INVALID_CODE;
    int strIndex=0;

    /* check arguments */
    if(U_FAILURE(*err)){
        return code;
    }
    /* try the Names array first */
    strIndex = findStringIndex(scriptNames, nameOrAbbrOrLocale, USCRIPT_NAMES_ARRAY_SIZE);
    
    if(strIndex>=0 && strIndex < USCRIPT_NAMES_ARRAY_SIZE){ 
        code = (UScriptCode) scriptNameCodes[strIndex];
    }
    /* we did not find in names array so try abbr array*/
    if(code ==USCRIPT_INVALID_CODE){
        strIndex = findStringIndex(scriptAbbr, nameOrAbbrOrLocale, USCRIPT_ABBR_ARRAY_SIZE);
        if(strIndex>=0 && strIndex < USCRIPT_NAMES_ARRAY_SIZE){ 
            code = (UScriptCode) scriptAbbrCodes[strIndex];
        }
    }
    /* we still haven't found it try locale */
    if(code==USCRIPT_INVALID_CODE){
        UResourceBundle* resB = ures_open(u_getDataDirectory(),nameOrAbbrOrLocale,err);
        if(U_SUCCESS(*err)&& *err != U_USING_DEFAULT_ERROR){
            int32_t len=0;
            UResourceBundle* resD = ures_getByKey(resB,kLocaleScript,NULL,err);
            int index =0;
            const UChar* name = ures_getStringByIndex(resD,0,&len,err);
            if(U_SUCCESS(*err) ){
                char cName[50] = {'\0'};
                u_UCharsToChars(name,cName,len);
                index = findStringIndex(scriptNames, cName, USCRIPT_NAMES_ARRAY_SIZE);
                code = (UScriptCode) scriptNameCodes[index];
            }
            ures_close(resD);
        
        }
        ures_close(resB);
    }
    return code;
}

U_CAPI const char* 
uscript_getName(UScriptCode scriptCode){
    int index = -1;
    if(scriptCode > USCRIPT_CODE_LIMIT){
        return "";
    }
    index = findCodeIndex(scriptNameCodes,scriptCode,USCRIPT_NAMES_ARRAY_SIZE);
    if(index >=0){
        return scriptNames[index];
    }else{
       return "";
    }

}
U_CAPI const char* 
uscript_getShortName(UScriptCode scriptCode){
    int index = -1;
    if(scriptCode > USCRIPT_CODE_LIMIT){
        return "";
    }
    index = findCodeIndex(scriptAbbrCodes,scriptCode,USCRIPT_ABBR_ARRAY_SIZE);
    if(index >=0){
        return scriptAbbr[index];
    }else{
       return "";
    }
}

