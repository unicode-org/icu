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
#include "uprops.h"
#include "cmemory.h"
#include "cstring.h"

#define ARRAY_SIZE(array) (sizeof array  / sizeof array[0])

static const char kLocaleScript[] = "LocaleScript";

/*
 * Use pairs of (name, code) instead of separate arrays to simplify maintenance
 * of these arrays.
 */
struct NameCodePair {
    const char *name;
    int32_t code;
};
typedef struct NameCodePair NameCodePair;

/* script names with codes, sorted by names */
static const NameCodePair
scriptNames[]={
   { "ARABIC",              USCRIPT_ARABIC      },
   { "ARMENIAN",            USCRIPT_ARMENIAN    },
   { "BENGALI",             USCRIPT_BENGALI     },
   { "BOPOMOFO",            USCRIPT_BOPOMOFO    },
   { "BUHID",               USCRIPT_BUHID       },
   { "CANADIAN_ABORIGINAL", USCRIPT_UCAS        },
   { "CHEROKEE",            USCRIPT_CHEROKEE    },
   { "COMMON",              USCRIPT_COMMON      },
   { "COPTIC",              USCRIPT_COPTIC      },
   { "CYRILLIC",            USCRIPT_CYRILLIC    },
   { "DESERET",             USCRIPT_DESERET     },
   { "DEVANAGARI",          USCRIPT_DEVANAGARI  },
   { "ETHIOPIC",            USCRIPT_ETHIOPIC    },
   { "GEORGIAN",            USCRIPT_GEORGIAN    },
   { "GOTHIC",              USCRIPT_GOTHIC      },
   { "GREEK",               USCRIPT_GREEK       },
   { "GUJARATI",            USCRIPT_GUJARATI    },
   { "GURMUKHI",            USCRIPT_GURMUKHI    },
   { "HAN",                 USCRIPT_HAN         },
   { "HANGUL",              USCRIPT_HANGUL      },
   { "HANUNOO",             USCRIPT_HANUNOO     },
   { "HEBREW",              USCRIPT_HEBREW      },
   { "HIRAGANA",            USCRIPT_HIRAGANA    },
   { "INHERITED",           USCRIPT_INHERITED   },
   { "KANNADA",             USCRIPT_KANNADA     },
   { "KATAKANA",            USCRIPT_KATAKANA    },
   { "KHMER",               USCRIPT_KHMER       },
   { "LAO",                 USCRIPT_LAO         },
   { "LATIN",               USCRIPT_LATIN       },
   { "MALAYALAM",           USCRIPT_MALAYALAM   },
   { "MONGOLIAN",           USCRIPT_MONGOLIAN   },
   { "MYANMAR",             USCRIPT_MYANMAR     },
   { "OGHAM",               USCRIPT_OGHAM       },
   { "OLD_ITALIC",          USCRIPT_OLD_ITALIC  },
   { "ORIYA",               USCRIPT_ORIYA       },
   { "RUNIC",               USCRIPT_RUNIC       },
   { "SINHALA",             USCRIPT_SINHALA     },
   { "SYRIAC",              USCRIPT_SYRIAC      },
   { "TAGALOG",             USCRIPT_TAGALOG     },
   { "TAGBANWA",            USCRIPT_TAGBANWA    },
   { "TAMIL",               USCRIPT_TAMIL       },
   { "TELUGU",              USCRIPT_TELUGU      },
   { "THAANA",              USCRIPT_THAANA      },
   { "THAI",                USCRIPT_THAI        },
   { "TIBETAN",             USCRIPT_TIBETAN     },
   { "UCAS",                USCRIPT_UCAS        },
   { "YI",                  USCRIPT_YI          }
};

/* script abbreviations with codes, sorted by abbreviations */
static const NameCodePair
scriptAbbr[]= {
    { "Arab",       USCRIPT_ARABIC      },
    { "Armn",       USCRIPT_ARMENIAN    },
    { "Beng",       USCRIPT_BENGALI     },
    { "Bopo",       USCRIPT_BOPOMOFO    },
    { "Buhd",       USCRIPT_BUHID       },
    { "Cans",       USCRIPT_UCAS        },
    { "Cher",       USCRIPT_CHEROKEE    },
    { "Cyrl",       USCRIPT_CYRILLIC    },
 /* { "Cyrs",       USCRIPT_CYRILLIC    }, */
    { "Deva",       USCRIPT_DEVANAGARI  },
    { "Dsrt",       USCRIPT_DESERET     },
    { "Ethi",       USCRIPT_ETHIOPIC    },
 /* { "Geoa",       USCRIPT_GEORGIAN    }, */
 /* { "Geon",       USCRIPT_GEORGIAN    }, */
    { "Geor",       USCRIPT_GEORGIAN    },
    { "Goth",       USCRIPT_GOTHIC      },
    { "Grek",       USCRIPT_GREEK       },
    { "Gujr",       USCRIPT_GUJARATI    },
    { "Guru",       USCRIPT_GURMUKHI    },
    { "Hang",       USCRIPT_HANGUL      },
    { "Hani",       USCRIPT_HAN         },
    { "Hano",       USCRIPT_HANUNOO     },
    { "Hebr",       USCRIPT_HEBREW      },
    { "Hira",       USCRIPT_HIRAGANA    },
    { "Ital",       USCRIPT_OLD_ITALIC  },
    { "Kana",       USCRIPT_KATAKANA    },
    { "Khmr",       USCRIPT_KHMER       },
    { "Knda",       USCRIPT_KANNADA     },
    { "Lao",        USCRIPT_LAO         },
 /* { "Laoo",       USCRIPT_LAO         }, */
 /* { "Latf",       USCRIPT_LATIN       }, */
 /* { "Latg",       USCRIPT_LATIN       }, */
    { "Latn",       USCRIPT_LATIN       },
    { "Mlym",       USCRIPT_MALAYALAM   },
    { "Mong",       USCRIPT_MONGOLIAN   },
    { "Mymr",       USCRIPT_MYANMAR     },
    { "Ogam",       USCRIPT_OGHAM       },
    { "Orya",       USCRIPT_ORIYA       },
    { "Qaac",       USCRIPT_COPTIC      },
    { "Qaai",       USCRIPT_INHERITED   },
    { "Runr",       USCRIPT_RUNIC       },
    { "Sinh",       USCRIPT_SINHALA     },
    { "Syrc",       USCRIPT_SYRIAC      },
 /* { "Syre",       USCRIPT_SYRIAC      }, */
 /* { "Syrj",       USCRIPT_SYRIAC      }, */
 /* { "Syrn",       USCRIPT_SYRIAC      }, */
    { "Tagb",       USCRIPT_TAGBANWA    },
    { "Taml",       USCRIPT_TAMIL       },
    { "Telu",       USCRIPT_TELUGU      },
    { "Tglg",       USCRIPT_TAGALOG     },
    { "Thaa",       USCRIPT_THAANA      },
    { "Thai",       USCRIPT_THAI        },
    { "Tibt",       USCRIPT_TIBETAN     },
    { "Yiii",       USCRIPT_YI          },
    { "Zyyy",       USCRIPT_COMMON      }
};

/* binary search the string array */
U_INLINE static int 
findStringIndex(const NameCodePair sortedArr[], const char *target, int32_t size) {
    int32_t left, middle, right, rc;

    left =0;
    right= size-1;
    
    while(left <= right){
        middle = (left+right)/2;
        rc=uprv_comparePropertyNames(sortedArr[middle].name, target);
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

/* linearly search the array and return the index */
U_INLINE static int
findCodeIndex(const NameCodePair unsorted[], const UScriptCode target, int size){
    int i=0;
    while(i<size){
        if(target == unsorted[i].code){
            return i;
        }
        i++;
    }
    return -1;
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
    strIndex = findStringIndex(scriptNames, nameOrAbbrOrLocale, ARRAY_SIZE(scriptNames));
    
    if(strIndex>=0){ 
        code = (UScriptCode) scriptNames[strIndex].code;
        len = 1;
    }
    /* we did not find in names array so try abbr array*/
    if(code ==USCRIPT_INVALID_CODE){
        strIndex = findStringIndex(scriptAbbr, nameOrAbbrOrLocale, ARRAY_SIZE(scriptAbbr));
        if(strIndex>=0){ 
            code = (UScriptCode) scriptAbbr[strIndex].code;
            len = 1;
        }
    }

    /* we still haven't found it try locale */
    if(code==USCRIPT_INVALID_CODE){
        /* Do not propagate error codes from just not finding a locale bundle. */
        UErrorCode localErrorCode = U_ZERO_ERROR;
        UResourceBundle* resB = ures_open(u_getDataDirectory(),nameOrAbbrOrLocale,&localErrorCode);
        if(U_SUCCESS(localErrorCode)&& localErrorCode != U_USING_DEFAULT_WARNING){
            UResourceBundle* resD = ures_getByKey(resB,kLocaleScript,NULL,&localErrorCode);
            int index =0;
            if(U_SUCCESS(localErrorCode) ){
                len =0;
                while(ures_hasNext(resD)){
                    const UChar* name = ures_getNextString(resD,&len,NULL,&localErrorCode);
                    if(U_SUCCESS(localErrorCode)){
                        char cName[50] = {'\0'};
                        u_UCharsToChars(name,cName,len);
                        index = findStringIndex(scriptAbbr, cName, ARRAY_SIZE(scriptAbbr));
                        code = (UScriptCode) scriptAbbr[index].code;
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
    index = findCodeIndex(scriptNames, scriptCode, ARRAY_SIZE(scriptNames));
    if(index >=0){
        return scriptNames[index].name;
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
    index = findCodeIndex(scriptAbbr, scriptCode, ARRAY_SIZE(scriptAbbr));
    if(index >=0){
        return scriptAbbr[index].name;
    }else{
       return "";
    }
}

