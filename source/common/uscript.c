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
#include "cmemory.h"
#include "cstring.h"

#define ARRAY_SIZE(array) (sizeof array  / sizeof array[0])

struct ParenStackEntry
{
    int32_t pairIndex;
    UScriptCode scriptCode;
};

struct UScriptRun
{
    int32_t textLength;
    const UChar *textArray;

    int32_t scriptStart;
    int32_t scriptLimit;
    UScriptCode scriptCode;

    struct ParenStackEntry parenStack[128];
    int32_t parenSP;
};

static int8_t highBit(int32_t value);

static const char kLocaleScript[] = "LocaleScript";

static const char * const scriptNames[]={
        "ARABIC",               /* USCRIPT_ARABIC     */
        "ARMENIAN",             /* USCRIPT_ARMENIAN   */
        "BENGALI",              /* USCRIPT_BENGALI    */
        "BOPOMOFO",             /* USCRIPT_BOPOMOFO   */
        "CANADIAN-ABORIGINAL",  /* USCRIPT_UCAS       */
        "CHEROKEE",             /* USCRIPT_CHEROKEE   */
        "COMMON",               /* USCRIPT_COMMON     */
        "CYRILLIC",             /* USCRIPT_CYRILLIC   */
        "DESERET",              /* USCRIPT_DESERET    */
        "DEVANAGARI",           /* USCRIPT_DEVANAGARI */
        "ETHIOPIC",             /* USCRIPT_ETHIOPIC   */
        "GEORGIAN",             /* USCRIPT_GEORGIAN   */
        "GOTHIC",               /* USCRIPT_GOTHIC     */
        "GREEK",                /* USCRIPT_GREEK      */
        "GUJARATI",             /* USCRIPT_GUJARATI   */
        "GURMUKHI",             /* USCRIPT_GURMUKHI   */
        "HAN",                  /* USCRIPT_HAN        */
        "HANGUL",               /* USCRIPT_HANGUL     */
        "HEBREW",               /* USCRIPT_HEBREW     */
        "HIRAGANA",             /* USCRIPT_HIRAGANA   */
        "INHERITED",            /* USCRIPT_INHERITED  */
        "KANNADA",              /* USCRIPT_KANNADA    */
        "KATAKANA",             /* USCRIPT_KATAKANA   */
        "KHMER",                /* USCRIPT_KHMER      */
        "LATIN",                /* USCRIPT_LATIN      */
        "MALAYALAM",            /* USCRIPT_MALAYALAM  */
        "MONGOLIAN",            /* USCRIPT_MONGOLIAN  */
        "MYANMAR",              /* USCRIPT_MYANMAR    */
        "OGHAM",                /* USCRIPT_OGHAM      */
        "OLD-ITALIC",           /* USCRIPT_OLD_ITALIC */
        "ORIYA",                /* USCRIPT_ORIYA      */
        "RUNIC",                /* USCRIPT_RUNIC      */
        "SINHALA",              /* USCRIPT_SINHALA    */
        "SYRIAC",               /* USCRIPT_SYRIAC     */
        "TAMIL",                /* USCRIPT_TAMIL      */
        "TELUGU",               /* USCRIPT_TELUGU     */
        "THAANA",               /* USCRIPT_THANA      */
        "THAI",                 /* USCRIPT_THAI       */
        "TIBETAN",              /* USCRIPT_TIBETAN    */
        "UCAS",                 /* USCRIPT_UCAS       */
        "YI"                    /* USCRIPT_YI         */
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
        "Zyyy"        /* USCRIPT_COMMON     */    
};


static const UScriptCode scriptNameCodes[]= {
         USCRIPT_ARABIC     ,
         USCRIPT_ARMENIAN   ,
         USCRIPT_BENGALI    ,
         USCRIPT_BOPOMOFO   ,
         USCRIPT_UCAS       ,
         USCRIPT_CHEROKEE   ,
         USCRIPT_COMMON     ,
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
         USCRIPT_YI
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
      /*  USCRIPT_SYRIAC     , */
        USCRIPT_TAMIL      ,
        USCRIPT_TELUGU     ,
        USCRIPT_THAANA     ,
        USCRIPT_THAI       ,
        USCRIPT_TIBETAN    ,
        USCRIPT_YI         ,
        USCRIPT_COMMON
};

static const UChar32 pairedChars[] = {
    0x0028, 0x0029, /* ascii paired punctuation */
    0x003c, 0x003e,
    0x005b, 0x005d,
    0x007b, 0x007d,
    0x00ab, 0x00bb, /* guillemets */
    0x2018, 0x2019, /* general punctuation */
    0x201c, 0x201d,
    0x2039, 0x203a,
    0x3008, 0x3009, /* chinese paired punctuation */
    0x300a, 0x300b,
    0x300c, 0x300d,
    0x300e, 0x300f,
    0x3010, 0x3011,
    0x3014, 0x3015,
    0x3016, 0x3017,
    0x3018, 0x3019,
    0x301a, 0x301b
};

#if 0
static const int32_t pairedCharCount = ARRAY_SIZE(pairedChars);
static const int32_t pairedCharPower = 1 << highBit(pairedCharCount);
static const int32_t pairedCharExtra = pairedCharCount - pairedCharPower;
#endif

/* binary search the string array */
U_INLINE static int 
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
U_INLINE static int
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

static int8_t
highBit(int32_t value)
{
    int8_t bit = 0;

    if (value <= 0) {
        return -32;
    }

    if (value >= 1 << 16) {
        value >>= 16;
        bit += 16;
    }

    if (value >= 1 << 8) {
        value >>= 8;
        bit += 8;
    }

    if (value >= 1 << 4) {
        value >>= 4;
        bit += 4;
    }

    if (value >= 1 << 2) {
        value >>= 2;
        bit += 2;
    }

    if (value >= 1 << 1) {
        value >>= 1;
        bit += 1;
    }

    return bit;
}

static int32_t
getPairIndex(UChar32 ch)
{
    int32_t pairedCharCount = ARRAY_SIZE(pairedChars);
    int32_t pairedCharPower = 1 << highBit(pairedCharCount);
    int32_t pairedCharExtra = pairedCharCount - pairedCharPower;

    int32_t probe = pairedCharPower;
    int32_t index = 0;

    if (ch >= pairedChars[pairedCharExtra]) {
        index = pairedCharExtra;
    }

    while (probe > (1 << 0)) {
        probe >>= 1;

        if (ch >= pairedChars[index + probe]) {
            index += probe;
        }
    }

    if (pairedChars[index] != ch) {
        index = -1;
    }

    return index;
}

static UBool
sameScript(UScriptCode scriptOne, UScriptCode scriptTwo)
{
    return scriptOne <= USCRIPT_INHERITED || scriptTwo <= USCRIPT_INHERITED || scriptOne == scriptTwo;
}

U_CAPI int32_t  U_EXPORT2
uscript_getCode(const char* nameOrAbbrOrLocale,
                UScriptCode* fillIn,
                int32_t capacity,
                UErrorCode* err){

    UScriptCode code = USCRIPT_INVALID_CODE;
    int strIndex=0;
    int32_t numFilled=0;
    int32_t len=0;
    /* check arguments */
    if(err==NULL ||U_FAILURE(*err)){
        return numFilled;
    }
    if(nameOrAbbrOrLocale==NULL || fillIn == NULL || capacity<0){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return numFilled;
    }
    /* try the Names array first */
    strIndex = findStringIndex(scriptNames, nameOrAbbrOrLocale, sizeof(scriptNames)/sizeof(*scriptNames));
    
    if(strIndex>=0){ 
        code = (UScriptCode) scriptNameCodes[strIndex];
        len = 1;
    }
    /* we did not find in names array so try abbr array*/
    if(code ==USCRIPT_INVALID_CODE){
        strIndex = findStringIndex(scriptAbbr, nameOrAbbrOrLocale, sizeof(scriptAbbr)/sizeof(*scriptAbbr));
        if(strIndex>=0){ 
            code = (UScriptCode) scriptAbbrCodes[strIndex];
            len = 1;
        }
    }

    /* we still haven't found it try locale */
    if(code==USCRIPT_INVALID_CODE){
        UResourceBundle* resB = ures_open(u_getDataDirectory(),nameOrAbbrOrLocale,err);
        if(U_SUCCESS(*err)&& *err != U_USING_DEFAULT_ERROR){
            UResourceBundle* resD = ures_getByKey(resB,kLocaleScript,NULL,err);
            int index =0;
            if(U_SUCCESS(*err) ){
                len =0;
                while(ures_hasNext(resD)){
                    const UChar* name = ures_getNextString(resD,&len,NULL,err);
                    if(U_SUCCESS(*err)){
                        char cName[50] = {'\0'};
                        u_UCharsToChars(name,cName,len);
                        index = findStringIndex(scriptAbbr, cName, sizeof(scriptAbbr)/sizeof(*scriptAbbr));
                        code = (UScriptCode) scriptAbbrCodes[index];
                        /* got the script code now fill in the buffer */
                        if(numFilled<=capacity){ 
                            *(fillIn)++=code;
                            numFilled++;
                        }else{
                            ures_close(resD);
                            ures_close(resB);
                            *err=U_BUFFER_OVERFLOW_ERROR;
                            return len;
                        }
                    }
                }
            }
            ures_close(resD);
        
        }
        ures_close(resB);
    }else{
        /* we found it */
        if(numFilled<=capacity){ 
            *(fillIn)++=code;
            numFilled++;
        }else{
            *err=U_BUFFER_OVERFLOW_ERROR;
            return len;
        }
    }
    return numFilled;
}

U_CAPI const char*  U_EXPORT2
uscript_getName(UScriptCode scriptCode){
    int index = -1;
    if(scriptCode > USCRIPT_CODE_LIMIT){
        return "";
    }
    index = findCodeIndex(scriptNameCodes,scriptCode,sizeof(scriptNameCodes)/sizeof(*scriptNameCodes));
    if(index >=0){
        return scriptNames[index];
    }else{
       return "";
    }

}
U_CAPI const char*  U_EXPORT2
uscript_getShortName(UScriptCode scriptCode){
    int index = -1;
    if(scriptCode > USCRIPT_CODE_LIMIT){
        return "";
    }
    index = findCodeIndex(scriptAbbrCodes,scriptCode,sizeof(scriptAbbrCodes)/sizeof(*scriptAbbrCodes));
    if(index >=0){
        return scriptAbbr[index];
    }else{
       return "";
    }
}

U_CAPI UScriptRun * U_EXPORT2
uscript_openRun(const UChar *src, int32_t length, UErrorCode *pErrorCode)
{
    UScriptRun *result = NULL;

    if (pErrorCode == NULL || U_FAILURE(*pErrorCode)) {
        return NULL;
    }

    result = uprv_malloc(sizeof (UScriptRun));

    if (result == NULL) {
        *pErrorCode = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }

    uscript_setRunText(result, src, length, pErrorCode);

    /* FIXME: should we free result if setRunText fails? */
    return result;
}

U_CAPI void U_EXPORT2
uscript_closeRun(UScriptRun *scriptRun)
{
    if (scriptRun != NULL) {
        uprv_free(scriptRun);
    }
}

U_CAPI void U_EXPORT2
uscript_resetRun(UScriptRun *scriptRun)
{
    if (scriptRun != NULL) {
        scriptRun->scriptStart = 0;
        scriptRun->scriptLimit = 0;
        scriptRun->scriptCode  = USCRIPT_INVALID_CODE;
        scriptRun->parenSP     = -1;
    }
}

U_CAPI void U_EXPORT2
uscript_setRunText(UScriptRun *scriptRun, const UChar *src, int32_t length, UErrorCode *pErrorCode)
{
    if (pErrorCode == NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    if (scriptRun == NULL || length < 0 || ((src == NULL) != (length == 0))) {
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    scriptRun->textArray  = src;
    scriptRun->textLength = length;

    uscript_resetRun(scriptRun);
}

U_CAPI UBool U_EXPORT2
uscript_nextRun(UScriptRun *scriptRun, int32_t *pRunStart, int32_t *pRunLimit, UScriptCode *pRunScript)
{
    int32_t startSP  = -1;  /* used to find the first new open character */
    UErrorCode error = U_ZERO_ERROR;

    /* if we've fallen off the end of the text, we're done */
    if (scriptRun == NULL || scriptRun->scriptLimit >= scriptRun->textLength) {
        return FALSE;
    }
    
    startSP = scriptRun->parenSP;
    scriptRun->scriptCode = USCRIPT_COMMON;

    for (scriptRun->scriptStart = scriptRun->scriptLimit; scriptRun->scriptLimit < scriptRun->textLength; scriptRun->scriptLimit += 1) {
        UChar   high = scriptRun->textArray[scriptRun->scriptLimit];
        UChar32 ch   = high;
        UScriptCode sc;
        int32_t pairIndex;

        /*
         * if the character is a high surrogate and it's not the last one
         * in the text, see if it's followed by a low surrogate
         */
        if (high >= 0xD800 && high <= 0xDBFF && scriptRun->scriptLimit < scriptRun->textLength - 1)
        {
            UChar low = scriptRun->textArray[scriptRun->scriptLimit + 1];

            /*
             * if it is followed by a low surrogate,
             * consume it and form the full character
             */
            if (low >= 0xDC00 && low <= 0xDFFF) {
                ch = (high - 0xD800) * 0x0400 + low - 0xDC00 + 0x10000;
                scriptRun->scriptLimit += 1;
            }
        }

        sc = uscript_getScript(ch, &error);
        pairIndex = getPairIndex(ch);

        /*
         * Paired character handling:
         *
         * if it's an open character, push it onto the stack.
         * if it's a close character, find the matching open on the
         * stack, and use that script code. Any non-matching open
         * characters above it on the stack will be poped.
         */
        if (pairIndex >= 0) {
            if ((pairIndex & 1) == 0) {
                scriptRun->parenStack[++scriptRun->parenSP].pairIndex = pairIndex;
                scriptRun->parenStack[scriptRun->parenSP].scriptCode  = scriptRun->scriptCode;
            } else if (scriptRun->parenSP >= 0) {
                int32_t pi = pairIndex & ~1;

                while (scriptRun->parenSP >= 0 && scriptRun->parenStack[scriptRun->parenSP].pairIndex != pi) {
                    scriptRun->parenSP -= 1;
                }

                if (scriptRun->parenSP < startSP) {
                    startSP = scriptRun->parenSP;
                }

                if (scriptRun->parenSP >= 0) {
                    sc = scriptRun->parenStack[scriptRun->parenSP].scriptCode;
                }
            }
        }

        if (sameScript(scriptRun->scriptCode, sc)) {
            if (scriptRun->scriptCode <= USCRIPT_INHERITED && sc > USCRIPT_INHERITED) {
                scriptRun->scriptCode = sc;

                /*
                 * now that we have a final script code, fix any open
                 * characters we pushed before we knew the script code.
                 */
                while (startSP < scriptRun->parenSP) {
                    scriptRun->parenStack[++startSP].scriptCode = scriptRun->scriptCode;
                }
            }

            /*
             * if this character is a close paired character,
             * pop it from the stack
             */
            if (pairIndex >= 0 && (pairIndex & 1) != 0 && scriptRun->parenSP >= 0) {
                scriptRun->parenSP -= 1;
                startSP -= 1;
            }
        } else {
            /*
             * if the run broke on a surrogate pair,
             * end it before the high surrogate
             */
            if (ch >= 0x10000) {
                scriptRun->scriptLimit -= 1;
            }

            break;
        }
    }


    if (pRunStart != NULL) {
        *pRunStart = scriptRun->scriptStart;
    }

    if (pRunLimit != NULL) {
        *pRunLimit = scriptRun->scriptLimit;
    }

    if (pRunScript != NULL) {
        *pRunScript = scriptRun->scriptCode;
    }

    return TRUE;
}
