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

#define U_SCRIPT_NAMES_ARRAY_SIZE 38
#define U_SCRIPT_ABBR_ARRAY_SIZE 41

static const char kLocaleScript[] = "LocaleScript";

static const char * const scriptNames[]={
        "ARABIC",     /* U_ARABIC     */
        "ARMENIAN",   /* U_ARMENIAN   */
        "BENGALI",    /* U_BENGALI    */
        "BOPOMOFO",   /* U_BOPOMOFO   */
        "CHEROKEE",   /* U_CHEROKEE   */
        "CYRILLIC",   /* U_CYRILLIC   */
        "DESERET",    /* U_DESERET    */
        "DEVANAGARI", /* U_DEVANAGARI */
        "ETHIOPIC",   /* U_ETHIOPIC   */
        "GEORGIAN",   /* U_GEORGIAN   */
        "GOTHIC",     /* U_GOTHIC     */
        "GREEK",      /* U_GREEK      */
        "GUJARATI",   /* U_GUJARATI   */
        "GURMUKHI",   /* U_GURMUKHI   */
        "HAN",        /* U_HAN        */
        "HANGUL",     /* U_HANGUL     */
        "HEBREW",     /* U_HEBREW     */
        "HIRAGANA",   /* U_HIRAGANA   */
        "INHERITED",  /* U_INHERITED  */
        "KANNADA",    /* U_KANNADA    */
        "KATAKANA",   /* U_KATAKANA   */
        "KHMER",      /* U_KHMER      */
        "LATIN",      /* U_LATIN      */
        "MALAYALAM",  /* U_MALAYALAM  */
        "MONGOLIAN",  /* U_MONGOLIAN  */
        "MYANMAR",    /* U_MYANMAR    */
        "OGHAM",      /* U_OGHAM      */
        "OLD_ITALIC", /* U_OLD_ITALIC */
        "ORIYA",      /* U_ORIYA      */
        "RUNIC",      /* U_RUNIC      */
        "SINHALA",    /* U_SINHALA    */
        "SYRIAC",     /* U_SYRIAC     */
        "TAMIL",      /* U_TAMIL      */
        "TELUGU",     /* U_TELUGU     */
        "THAANA",     /* U_THANA      */
        "THAI",       /* U_THAI       */
        "TIBETAN",    /* U_TIBETAN    */
        "UCAS",       /* U_UCAS       */
    
};

static const char * const scriptAbbr[]= {
        "Arab",       /* U_ARABIC     */
        "Armn",       /* U_ARMENIAN   */
        "Beng",       /* U_BENGALI    */
        "Bopo",       /* U_BOPOMOFO   */
        "Cans",       /* U_UCAS       */
        "Cher",       /* U_CHEROKEE   */
        "Cyrl",       /* U_CYRILLIC   */
       /* "Cyrs",       */ /* U_CYRILLIC   */
        "Deva",       /* U_DEVANAGARI */
        "Dsrt",       /* U_DESERET    */
        "Ethi",       /* U_ETHIOPIC   */
       /* "Geoa",       */ /* U_GEORGIAN   */
       /* "Geon",       */ /* U_GEORGIAN   */
        "Geor",       /* U_GEORGIAN   */
        "Goth",       /* U_GOTHIC     */
        "Grek",       /* U_GREEK      */
        "Gujr",       /* U_GUJARATI   */
        "Guru",       /* U_GURMUKHI   */
        "Hang",       /* U_HANGUL     */
        "Hani",       /* U_HAN        */
        "Hebr",       /* U_HEBREW     */
        "Hira",       /* U_HIRAGANA   */
        "Ital",       /* U_OLD_ITALIC */
        "Kana",       /* U_KATAKANA   */
        "Khmr",       /* U_KHMER      */
        "Knda",       /* U_KANNADA    */
        "Lao",        /* U_LAO        */
        /*"Laoo",       */ /* U_LAO        */
        /*"Latf",       */ /* U_LATIN      */
        /*"Latg",       */ /* U_LATIN      */
        "Latn",       /* U_LATIN      */
        "Mlym",       /* U_MALAYALAM  */
        "Mong",       /* U_MONGOLIAN  */
        "Mymr",       /* U_MYANMAR    */
        "Ogam",       /* U_OGHAM      */
        "Orya",       /* U_ORIYA      */
        "Qaac",       /* U_COPTIC     */
        "Qaai",       /* U_INHERITED  */
        "Runr",       /* U_RUNIC      */
        "Sinh",       /* U_SINHALA    */
        "Syrc",       /* U_SYRIAC     */
       /* "Syre",       */ /* U_SYRIAC     */
       /* "Syrj",       */ /* U_SYRIAC     */
       /* "Syrn",       */ /* U_SYRIAC     */
        "Taml",       /* U_TAMIL      */
        "Telu",       /* U_TELUGU     */
        "Thaa",       /* U_THANA      */
        "Thai",       /* U_THAI       */
        "Tibt",       /* U_TIBETAN    */
        "Yiii",       /* U_YI         */
        "Zyyy",       /* U_COMMON     */    
    };


static const UScriptCode scriptNameCodes[]= {
         U_ARABIC     ,
         U_ARMENIAN   ,
         U_BENGALI    ,
         U_BOPOMOFO   ,
         U_CHEROKEE   ,
         U_CYRILLIC   ,
         U_DESERET    ,
         U_DEVANAGARI ,
         U_ETHIOPIC   ,
         U_GEORGIAN   ,
         U_GOTHIC     ,
         U_GREEK      ,
         U_GUJARATI   ,
         U_GURMUKHI   ,
         U_HAN        ,
         U_HANGUL     ,
         U_HEBREW     ,
         U_HIRAGANA   ,
         U_INHERITED  ,
         U_KANNADA    ,
         U_KATAKANA   ,
         U_KHMER      ,
         U_LATIN      ,
         U_MALAYALAM  ,
         U_MONGOLIAN  ,
         U_MYANMAR    ,
         U_OGHAM      ,
         U_OLD_ITALIC ,
         U_ORIYA      ,
         U_RUNIC      ,
         U_SINHALA    ,
         U_SYRIAC     ,
         U_TAMIL      ,
         U_TELUGU     ,
         U_THAANA     ,
         U_THAI       ,
         U_TIBETAN    ,
         U_UCAS       ,
        
};


static const UScriptCode scriptAbbrCodes[] = {
        U_ARABIC     ,
        U_ARMENIAN   ,
        U_BENGALI    ,
        U_BOPOMOFO   ,
        U_UCAS       ,
        U_CHEROKEE   ,
        U_CYRILLIC   ,
       /* U_CYRILLIC   ,*/
        U_DEVANAGARI ,
        U_DESERET    ,
        U_ETHIOPIC   ,
      /*  U_GEORGIAN   , */
      /*  U_GEORGIAN   , */
        U_GEORGIAN   ,
        U_GOTHIC     ,
        U_GREEK      ,
        U_GUJARATI   ,
        U_GURMUKHI   ,
        U_HANGUL     ,
        U_HAN        ,
        U_HEBREW     ,
        U_HIRAGANA   ,
        U_OLD_ITALIC ,
        U_KATAKANA   ,
        U_KHMER      ,
        U_KANNADA    ,
        U_LAO        ,
      /*  U_LAO        , */
      /*  U_LATIN      , */
      /*  U_LATIN      , */
        U_LATIN      ,
        U_MALAYALAM  ,
        U_MONGOLIAN  ,
        U_MYANMAR    ,
        U_OGHAM      ,
        U_ORIYA      ,
        U_COPTIC     ,
        U_INHERITED  ,
        U_RUNIC      ,
        U_SINHALA    ,
        U_SYRIAC     ,
      /*  U_SYRIAC     , */
      /*  U_SYRIAC     , */
        U_SYRIAC     ,
        U_TAMIL      ,
        U_TELUGU     ,
        U_THAANA     ,
        U_THAI       ,
        U_TIBETAN    ,
        U_YI         ,
        U_COMMON     , 
};
static const char * const scriptCodeName[]={
    "U_INVALID_CODE",
    "U_COMMON",    
    "U_INHERITED", 
    "U_ARABIC",    
    "U_ARMENIAN",  
    "U_BENGALI",   
    "U_BOPOMOFO",  
    "U_CHEROKEE",  
    "U_COPTIC",    
    "U_CYRILLIC",  
    "U_DESERET",   
    "U_DEVANAGARI",
    "U_ETHIOPIC",  
    "U_GEORGIAN",  
    "U_GOTHIC",    
    "U_GREEK",     
    "U_GUJARATI",  
    "U_GURMUKHI",  
    "U_HAN",       
    "U_HANGUL",   
    "U_HEBREW",    
    "U_HIRAGANA",  
    "U_KANNADA",   
    "U_KATAKANA",  
    "U_KHMER",     
    "U_LAO",       
    "U_LATIN",     
    "U_MALAYALAM", 
    "U_MONGOLIAN", 
    "U_MYANMAR",   
    "U_OGHAM",     
    "U_OLD_ITALIC",
    "U_ORIYA",     
    "U_RUNIC",     
    "U_SINHALA",   
    "U_SYRIAC",    
    "U_TAMIL",     
    "U_TELUGU",    
    "U_THAANA",    
    "U_THAI",      
    "U_TIBETAN",   
    "U_UCAS",      
    "U_YI",  
    "U_SCRIPT_CODE_LIMIT",
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
uchar_getScriptCode(const char* nameOrAbbrOrLocale, UErrorCode* err){
    UScriptCode code = U_INVALID_SCRIPT_CODE;
    int strIndex=0;

    /* check arguments */
    if(U_FAILURE(*err)){
        return code;
    }
    /* try the Names array first */
    strIndex = findStringIndex(scriptNames, nameOrAbbrOrLocale, U_SCRIPT_NAMES_ARRAY_SIZE);
    
    if(strIndex>=0 && strIndex < U_SCRIPT_NAMES_ARRAY_SIZE){ 
        code = (UScriptCode) scriptNameCodes[strIndex];
    }
    /* we did not find in names array so try abbr array*/
    if(code ==U_INVALID_SCRIPT_CODE){
        strIndex = findStringIndex(scriptAbbr, nameOrAbbrOrLocale, U_SCRIPT_ABBR_ARRAY_SIZE);
        if(strIndex>=0 && strIndex < U_SCRIPT_NAMES_ARRAY_SIZE){ 
            code = (UScriptCode) scriptAbbrCodes[strIndex];
        }
    }
    /* we still haven't found it try locale */
    if(code==U_INVALID_SCRIPT_CODE){
        UResourceBundle* resB = ures_open(u_getDataDirectory(),nameOrAbbrOrLocale,err);
        if(U_SUCCESS(*err)&& *err != U_USING_DEFAULT_ERROR){
            int32_t len=0;
            UResourceBundle* resD = ures_getByKey(resB,kLocaleScript,NULL,err);
            int index =0;
            const UChar* name = ures_getStringByIndex(resD,0,&len,err);
            if(U_SUCCESS(*err) ){
                char cName[50] = {'\0'};
                u_UCharsToChars(name,cName,len);
                index = findStringIndex(scriptNames, cName, U_SCRIPT_NAMES_ARRAY_SIZE);
                code = (UScriptCode) scriptNameCodes[index];
            }
            ures_close(resD);
        
        }
        ures_close(resB);
    }
    return code;
}

U_CAPI const char* 
uchar_getScriptName(UScriptCode scriptCode){
    int index = -1;
    if(scriptCode > U_SCRIPT_CODE_LIMIT){
        return "";
    }
    index = findCodeIndex(scriptNameCodes,scriptCode,U_SCRIPT_NAMES_ARRAY_SIZE);
    if(index >=0){
        return scriptNames[index];
    }else{
       return "";
    }

}
U_CAPI const char* 
uchar_getScriptAbbr(UScriptCode scriptCode){
    int index = -1;
    if(scriptCode > U_SCRIPT_CODE_LIMIT){
        return "";
    }
    index = findCodeIndex(scriptAbbrCodes,scriptCode,U_SCRIPT_ABBR_ARRAY_SIZE);
    if(index >=0){
        return scriptAbbr[index];
    }else{
       return "";
    }
}

U_CAPI const char* 
uchar_scriptCodeName(UScriptCode code){
    if(code>=0 && code<U_SCRIPT_CODE_LIMIT) {
        return scriptCodeName[code+1];
    } else{
        return scriptCodeName[U_INVALID_SCRIPT_CODE+1];
    }
}
