/*
**********************************************************************
*   Copyright (C) 2000-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   file name:  ucnv2022.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000feb03
*   created by: Markus W. Scherer
*
*   Change history:
*
*   06/29/2000  helena  Major rewrite of the callback APIs.
*   08/08/2000  Ram     Included support for ISO-2022-JP-2
*                       Changed implementation of toUnicode
*                       function
*   08/21/2000  Ram     Added support for ISO-2022-KR
*   08/29/2000  Ram     Seperated implementation of EBCDIC to
*                       ucnvebdc.c
*   09/20/2000  Ram     Added support for ISO-2022-CN
*                       Added implementations for getNextUChar()
*                       for specific 2022 country variants.
*   10/31/2000  Ram     Implemented offsets logic functions
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_LEGACY_CONVERSION

#include "unicode/ucnv.h"
#include "unicode/uset.h"
#include "unicode/ucnv_err.h"
#include "unicode/ucnv_cb.h"
#include "ucnv_bld.h"
#include "ucnv_cnv.h"
#include "ucnvmbcs.h"
#include "cstring.h"
#include "cmemory.h"

static const char UCNV_SS2[] = "\x1B\x4E";
static const char UCNV_SS3[] = "\x1B\x4F";
#define UCNV_SS2_LEN 2
#define UCNV_SS3_LEN 2

#define CR      0x0D
#define LF      0x0A
#define H_TAB   0x09
#define V_TAB   0x0B
#define SPACE   0x20

/* for ISO-2022JP implementation*/
typedef enum  {
        ASCII = 0,
        ISO8859_1 = 1 ,
        ISO8859_7 = 2 ,
        JISX201  = 3,
        JISX208 = 4,
        JISX212 = 5,
        GB2312  =6,
        KSC5601 =7,
        HWKANA_7BIT=8,    /* Halfwidth Katakana 7 bit */
        INVALID_STATE=-1

} StateEnum;



typedef enum {
        ASCII1=0,
        LATIN1,
        SBCS,
        DBCS,
        MBCS

}Cnv2022Type;

#define UCNV_OPTIONS_VERSION_MASK 0xf
#define UCNV_2022_MAX_CONVERTERS 10

typedef struct{
    UConverter *currentConverter;
    UConverter *fromUnicodeConverter;
    UBool isFirstBuffer;
    StateEnum toUnicodeCurrentState;
    StateEnum fromUnicodeCurrentState;
    StateEnum toUnicodeSaveState;
    Cnv2022Type currentType;
    int plane;
    UConverter* myConverterArray[UCNV_2022_MAX_CONVERTERS];
    UBool isEscapeAppended;
    UBool isShiftAppended;
    UBool isLocaleSpecified;
    uint32_t key;
    uint32_t version;
    char locale[3];
    char name[30];
}UConverterDataISO2022;

/* ISO-2022 ----------------------------------------------------------------- */

/*Forward declaration */
U_CFUNC void 
T_UConverter_fromUnicode_UTF8 (UConverterFromUnicodeArgs * args,
                                            UErrorCode * err);
U_CFUNC void 
T_UConverter_fromUnicode_UTF8_OFFSETS_LOGIC (UConverterFromUnicodeArgs * args,
                                                          UErrorCode * err);
U_CFUNC void 
_MBCSFromUnicodeWithOffsets(UConverterFromUnicodeArgs *pArgs,
                            UErrorCode *pErrorCode);
U_CFUNC void 
_MBCSToUnicodeWithOffsets(UConverterToUnicodeArgs *pArgs,
                          UErrorCode *pErrorCode);

/* Protos */
/***************** ISO-2022 ********************************/
static void
_ISO_2022_GetUnicodeSet(const UConverter *cnv,
                    USet *set,
                    UConverterUnicodeSet which,
                    UErrorCode *pErrorCode);

static void 
T_UConverter_toUnicode_ISO_2022(UConverterToUnicodeArgs * args,
                                             UErrorCode * err);
static void 
T_UConverter_toUnicode_ISO_2022_OFFSETS_LOGIC (UConverterToUnicodeArgs * args,
                                                            UErrorCode * err);

static UChar32 
T_UConverter_getNextUChar_ISO_2022 (UConverterToUnicodeArgs * args,
                                                    UErrorCode * err);

/***************** ISO-2022-JP ********************************/

static void 
UConverter_fromUnicode_ISO_2022_JP_OFFSETS_LOGIC(UConverterFromUnicodeArgs* args, 
                                                UErrorCode* err);

static void 
UConverter_toUnicode_ISO_2022_JP_OFFSETS_LOGIC(UConverterToUnicodeArgs* args, 
                                                            UErrorCode* err);

/***************** ISO-2022-KR ********************************/

static void 
UConverter_fromUnicode_ISO_2022_KR_OFFSETS_LOGIC(UConverterFromUnicodeArgs* args, 
                                                              UErrorCode* err);

static void 
UConverter_toUnicode_ISO_2022_KR_OFFSETS_LOGIC(UConverterToUnicodeArgs* args, 
                                                            UErrorCode* err);
/* Special function for getting output from IBM-25546 code page*/
static void 
UConverter_toUnicode_ISO_2022_KR_OFFSETS_LOGIC_IBM(UConverterToUnicodeArgs *args,
                                                            UErrorCode* err);
static void 
UConverter_fromUnicode_ISO_2022_KR_OFFSETS_LOGIC_IBM(UConverterFromUnicodeArgs* args,
                                                                  UErrorCode* err);
/***************** ISO-2022-CN ********************************/

static void 
UConverter_fromUnicode_ISO_2022_CN_OFFSETS_LOGIC(UConverterFromUnicodeArgs* args, 
                                                UErrorCode* err);

static void 
UConverter_toUnicode_ISO_2022_CN_OFFSETS_LOGIC(UConverterToUnicodeArgs* args, 
                                                            UErrorCode* err);

#define ESC_2022 0x1B /*ESC*/

typedef enum
{
        INVALID_2022 = -1, /*Doesn't correspond to a valid iso 2022 escape sequence*/
        VALID_NON_TERMINAL_2022 = 0, /*so far corresponds to a valid iso 2022 escape sequence*/
        VALID_TERMINAL_2022 = 1, /*corresponds to a valid iso 2022 escape sequence*/
        VALID_MAYBE_TERMINAL_2022 = 2, /*so far matches one iso 2022 escape sequence, but by adding more characters might match another escape sequence*/
        VALID_SS2_SEQUENCE=3,
        VALID_SS3_SEQUENCE=4

} UCNV_TableStates_2022;

/*
* The way these state transition arrays work is:
* ex : ESC$B is the sequence for JISX208
*      a) First Iteration: char is ESC
*          i) Get the value of ESC from normalize_esq_chars_2022[] with int value of ESC as index
*             int x = normalize_esq_chars_2022[27] which is equal to 1
*         ii) Search for this value in escSeqStateTable_Key_2022[]
*             value of x is stored at escSeqStateTable_Key_2022[0]
*        iii) Save this index as offset
*         iv) Get state of this sequence from escSeqStateTable_Value_2022[]
*             escSeqStateTable_Value_2022[offset], which is VALID_NON_TERMINAL_2022
*     b) Switch on this state and continue to next char
*          i) Get the value of $ from normalize_esq_chars_2022[] with int value of $ as index
*             which is normalize_esq_chars_2022[36] == 4
*         ii) x is currently 1(from above)
*               x<<=5 -- x is now 32
*               x+=normalize_esq_chars_2022[36]
*               now x is 36
*        iii) Search for this value in escSeqStateTable_Key_2022[]
*             value of x is stored at escSeqStateTable_Key_2022[2], so offset is 2
*         iv) Get state of this sequence from escSeqStateTable_Value_2022[]
*             escSeqStateTable_Value_2022[offset], which is VALID_NON_TERMINAL_2022
*     c) Switch on this state and continue to next char
*        i)  Get the value of B from normalize_esq_chars_2022[] with int value of B as index
*        ii) x is currently 36 (from above)
*            x<<=5 -- x is now 1152
*            x+=normalize_esq_chars_2022[66]
*            now x is 1161
*       iii) Search for this value in escSeqStateTable_Key_2022[]
*            value of x is stored at escSeqStateTable_Key_2022[21], so offset is 21
*        iv) Get state of this sequence from escSeqStateTable_Value_2022[21]
*            escSeqStateTable_Value_2022[offset], which is VALID_TERMINAL_2022
*         v) Get the converter name form escSeqStateTable_Result_2022[21] which is JISX208
*/


/*Below are the 3 arrays depicting a state transition table*/
static const int8_t normalize_esq_chars_2022[256] = {
/*       0      1       2       3       4      5       6        7       8       9           */

         0     ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0     ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0     ,0      ,0      ,0      ,0      ,0      ,0      ,1      ,0      ,0
        ,0     ,0      ,0      ,0      ,0      ,0      ,4      ,7      ,29      ,0
        ,2     ,24     ,26     ,27     ,0      ,3      ,23     ,6      ,0      ,0
        ,0     ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0     ,0      ,0      ,0      ,5      ,8      ,9      ,10     ,11     ,12
        ,13    ,14     ,15     ,16     ,17     ,18     ,19     ,20     ,25     ,28
        ,0     ,0      ,21     ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,22    ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0     ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0     ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0     ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0     ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0     ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0     ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0     ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0     ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0     ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0     ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0     ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0     ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0     ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0     ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0     ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0     ,0      ,0      ,0      ,0      ,0
};

#define MAX_STATES_2022 74
static const int32_t escSeqStateTable_Key_2022[MAX_STATES_2022] = {
/*   0           1           2           3           4           5           6           7           8           9           */

     1          ,34         ,36         ,39         ,55         ,57         ,60         ,61         ,1093       ,1096
    ,1097       ,1098       ,1099       ,1100       ,1101       ,1102       ,1103       ,1104       ,1105       ,1106
    ,1109       ,1154       ,1157       ,1160       ,1161       ,1176       ,1178       ,1179       ,1254       ,1257
    ,1768       ,1773       ,1957       ,35105      ,36933      ,36936      ,36937      ,36938      ,36939      ,36940
    ,36942      ,36943      ,36944      ,36945      ,36946      ,36947      ,36948      ,37640      ,37642      ,37644
    ,37646      ,37711      ,37744      ,37745      ,37746      ,37747      ,37748      ,40133      ,40136      ,40138
    ,40139      ,40140      ,40141      ,1123363    ,35947624   ,35947625   ,35947626   ,35947627   ,35947629   ,35947630
    ,35947631   ,35947635   ,35947636   ,35947638
};


static const char* const escSeqStateTable_Result_2022[MAX_STATES_2022] = {
 /*  0                      1                        2                      3                   4                   5                        6                      7                       8                       9    */

     NULL                   ,NULL                   ,NULL                   ,NULL               ,NULL               ,NULL                   ,NULL                   ,NULL                   ,"latin1"               ,"latin1"
    ,"latin1"               ,"ibm-865"              ,"ibm-865"              ,"ibm-865"          ,"ibm-865"          ,"ibm-865"              ,"ibm-865"              ,"JISX-201"             ,"JISX-201"             ,"latin1"
    ,"latin1"               ,NULL                   ,"JISX-208"             ,"ibm-5478"         ,"JISX-208"         ,NULL                   ,NULL                   ,NULL                   ,NULL                   ,"UTF8"
    ,"ISO-8859-1"           ,"ISO-8859-7"           ,"JIS-X-208"            ,NULL               ,"ibm-955"          ,"ibm-367"              ,"ibm-952"              ,"ibm-949"              ,"JISX-212"             ,"ibm-1383"
    ,"ibm-952"              ,"ibm-964"              ,"ibm-964"              ,"ibm-964"          ,"ibm-964"          ,"ibm-964"              ,"ibm-964"              ,"ibm-5478"         ,"ibm-949"              ,"ISO-IR-165"
    ,"CNS-11643-1992,1"     ,"CNS-11643-1992,2"     ,"CNS-11643-1992,3"     ,"CNS-11643-1992,4" ,"CNS-11643-1992,5" ,"CNS-11643-1992,6"     ,"CNS-11643-1992,7"     ,"UTF16_PlatformEndian" ,"UTF16_PlatformEndian" ,"UTF16_PlatformEndian"
    ,"UTF16_PlatformEndian" ,"UTF16_PlatformEndian" ,"UTF16_PlatformEndian" ,NULL               ,"latin1"           ,"ibm-912"              ,"ibm-913"              ,"ibm-914"              ,"ibm-813"              ,"ibm-1089"
    ,"ibm-920"              ,"ibm-915"              ,"ibm-915"              ,"latin1"
};

static const UCNV_TableStates_2022 escSeqStateTable_Value_2022[MAX_STATES_2022] = {
/*          0                           1                         2                             3                           4                           5                               6                        7                          8                           9       */
     VALID_NON_TERMINAL_2022    ,VALID_NON_TERMINAL_2022    ,VALID_NON_TERMINAL_2022    ,VALID_NON_TERMINAL_2022     ,VALID_NON_TERMINAL_2022    ,VALID_SS2_SEQUENCE        ,VALID_SS3_SEQUENCE         ,VALID_NON_TERMINAL_2022    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022
    ,VALID_MAYBE_TERMINAL_2022  ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022
    ,VALID_TERMINAL_2022        ,VALID_NON_TERMINAL_2022    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_NON_TERMINAL_2022    ,VALID_NON_TERMINAL_2022    ,VALID_NON_TERMINAL_2022    ,VALID_NON_TERMINAL_2022    ,VALID_TERMINAL_2022
    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_NON_TERMINAL_2022    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022
    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022
    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022
    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_NON_TERMINAL_2022    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022
    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022
};



/*for 2022 looks ahead in the stream
 *to determine the longest possible convertible
 *data stream
 */
static const char* getEndOfBuffer_2022(const char** source,
                                       const char* sourceLimit,
                                       UBool flush);
/* Type def for refactoring changeState_2022 code*/
typedef enum{
    ISO_2022=0,
    ISO_2022_JP=1,
    ISO_2022_KR=2,
    ISO_2022_CN=3
} Variant2022;

/*runs through a state machine to determine the escape sequence - codepage correspondance
 *changes the pointer pointed to be _this->extraInfo
 */
static void
changeState_2022(UConverter* _this,
                    const char** source, 
                    const char* sourceLimit,
                    UBool flush,Variant2022 var,int* plane,
                    UErrorCode* err);


static UCNV_TableStates_2022 
getKey_2022(char source,
                int32_t* key,
                int32_t* offset);

/*********** ISO 2022 Converter Protos ***********/
static void 
_ISO2022Open(UConverter *cnv, const char *name, const char *locale,uint32_t options, UErrorCode *errorCode);

static void
 _ISO2022Close(UConverter *converter);

static void 
_ISO2022Reset(UConverter *converter, UConverterResetChoice choice);

static const char* 
_ISO2022getName(const UConverter* cnv);

static void 
_ISO_2022_WriteSub(UConverterFromUnicodeArgs *args, int32_t offsetIndex, UErrorCode *err);

static UConverter * 
_ISO_2022_SafeClone(const UConverter *cnv, void *stackBuffer, int32_t *pBufferSize, UErrorCode *status);

/************ protos of functions for setting the initial state *********************/
static void 
setInitialStateToUnicodeJPCN(UConverter* converter,UConverterDataISO2022 *myConverterData);

static void 
setInitialStateFromUnicodeJPCN(UConverter* converter,UConverterDataISO2022 *myConverterData);

static void 
setInitialStateToUnicodeKR(UConverter* converter,UConverterDataISO2022 *myConverterData);

static void 
setInitialStateFromUnicodeKR(UConverter* converter,UConverterDataISO2022 *myConverterData);

/*************** Converter implemenations ******************/
static const UConverterImpl _ISO2022Impl={
    UCNV_ISO_2022,

    NULL,
    NULL,

    _ISO2022Open,
    _ISO2022Close,
    _ISO2022Reset,

    T_UConverter_toUnicode_ISO_2022,
    T_UConverter_toUnicode_ISO_2022_OFFSETS_LOGIC,
    T_UConverter_fromUnicode_UTF8,
    T_UConverter_fromUnicode_UTF8_OFFSETS_LOGIC,
    T_UConverter_getNextUChar_ISO_2022,

    NULL,
    _ISO2022getName,
    _ISO_2022_WriteSub,
    _ISO_2022_SafeClone,
    _ISO_2022_GetUnicodeSet
};
static const UConverterStaticData _ISO2022StaticData={
    sizeof(UConverterStaticData),
    "ISO_2022",
    2022,
    UCNV_IBM,
    UCNV_ISO_2022,
    1,
    4,
    { 0x1a, 0, 0, 0 },
    1,
    FALSE,
    FALSE,
    0,
    0,
    { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 } /* reserved */
};
const UConverterSharedData _ISO2022Data={
    sizeof(UConverterSharedData),
    ~((uint32_t) 0),
    NULL,
    NULL,
    &_ISO2022StaticData,
    FALSE,
    &_ISO2022Impl,
    0
};

/*************JP****************/
static const UConverterImpl _ISO2022JPImpl={
    UCNV_ISO_2022,

    NULL,
    NULL,

    _ISO2022Open,
    _ISO2022Close,
    _ISO2022Reset,

    UConverter_toUnicode_ISO_2022_JP_OFFSETS_LOGIC,
    UConverter_toUnicode_ISO_2022_JP_OFFSETS_LOGIC,
    UConverter_fromUnicode_ISO_2022_JP_OFFSETS_LOGIC,
    UConverter_fromUnicode_ISO_2022_JP_OFFSETS_LOGIC,
    NULL,

    NULL,
    _ISO2022getName,
    _ISO_2022_WriteSub,
    _ISO_2022_SafeClone,
    _ISO_2022_GetUnicodeSet
};
static const UConverterStaticData _ISO2022JPStaticData={
    sizeof(UConverterStaticData),
    "ISO_2022_JP",
    0,
    UCNV_IBM,
    UCNV_ISO_2022,
    1,
    6,
    { 0x1a, 0, 0, 0 },
    1,
    FALSE,
    FALSE,
    0,
    0,
    { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 } /* reserved */
};
const UConverterSharedData _ISO2022JPData={
    sizeof(UConverterSharedData),
    ~((uint32_t) 0),
    NULL,
    NULL,
    &_ISO2022JPStaticData,
    FALSE,
    &_ISO2022JPImpl,
    0
};

/************* KR ***************/
static const UConverterImpl _ISO2022KRImpl={
    UCNV_ISO_2022,

    NULL,
    NULL,

    _ISO2022Open,
    _ISO2022Close,
    _ISO2022Reset,

    UConverter_toUnicode_ISO_2022_KR_OFFSETS_LOGIC,
    UConverter_toUnicode_ISO_2022_KR_OFFSETS_LOGIC,
    UConverter_fromUnicode_ISO_2022_KR_OFFSETS_LOGIC,
    UConverter_fromUnicode_ISO_2022_KR_OFFSETS_LOGIC,
    NULL,

    NULL,
    _ISO2022getName,
    _ISO_2022_WriteSub,
    _ISO_2022_SafeClone,
    _ISO_2022_GetUnicodeSet
};
static const UConverterStaticData _ISO2022KRStaticData={
    sizeof(UConverterStaticData),
    "ISO_2022_KR",
    0,
    UCNV_IBM,
    UCNV_ISO_2022,
    1,
    3,
    { 0x1a, 0, 0, 0 },
    1,
    FALSE,
    FALSE,
    0,
    0,
    { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 } /* reserved */
};
const UConverterSharedData _ISO2022KRData={
    sizeof(UConverterSharedData),
    ~((uint32_t) 0),
    NULL,
    NULL,
    &_ISO2022KRStaticData,
    FALSE,
    &_ISO2022KRImpl,
    0
};

/*************** CN ***************/
static const UConverterImpl _ISO2022CNImpl={

    UCNV_ISO_2022,

    NULL,
    NULL,

    _ISO2022Open,
    _ISO2022Close,
    _ISO2022Reset,

    UConverter_toUnicode_ISO_2022_CN_OFFSETS_LOGIC,
    UConverter_toUnicode_ISO_2022_CN_OFFSETS_LOGIC,
    UConverter_fromUnicode_ISO_2022_CN_OFFSETS_LOGIC,
    UConverter_fromUnicode_ISO_2022_CN_OFFSETS_LOGIC,
    NULL,

    NULL,
    _ISO2022getName,
    _ISO_2022_WriteSub,
    _ISO_2022_SafeClone,
    _ISO_2022_GetUnicodeSet
};
static const UConverterStaticData _ISO2022CNStaticData={
    sizeof(UConverterStaticData),
    "ISO_2022_CN",
    0,
    UCNV_IBM,
    UCNV_ISO_2022,
    2,
    8,
    { 0x1a, 0, 0, 0 },
    1,
    FALSE,
    FALSE,
    0,
    0,
    { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 } /* reserved */
};
const UConverterSharedData _ISO2022CNData={
    sizeof(UConverterSharedData),
    ~((uint32_t) 0),
    NULL,
    NULL,
    &_ISO2022CNStaticData,
    FALSE,
    &_ISO2022CNImpl,
    0
};


/**********/

static void 
_ISO2022Open(UConverter *cnv, const char *name, const char *locale,uint32_t options, UErrorCode *errorCode){

    char myLocale[6]={' ',' ',' ',' ',' ',' '};

    cnv->extraInfo = uprv_malloc (sizeof (UConverterDataISO2022));
    if(cnv->extraInfo != NULL) {
        UConverterDataISO2022 *myConverterData=(UConverterDataISO2022 *) cnv->extraInfo;
        myConverterData->currentConverter = NULL;
        myConverterData->fromUnicodeConverter = NULL;
        myConverterData->currentType= ASCII1;
        myConverterData->plane = -1;
        myConverterData->key =0;
        myConverterData->isFirstBuffer = TRUE;
        cnv->fromUnicodeStatus =FALSE;
        if(locale){
            uprv_strncpy(myLocale, locale, sizeof(myLocale));
            myConverterData->isLocaleSpecified = TRUE;
        }
        myConverterData->version= 0;
        myConverterData->myConverterArray[0] =NULL;
        if(myLocale[0]=='j' && (myLocale[1]=='a'|| myLocale[1]=='p') && 
            (myLocale[2]=='_' || myLocale[2]=='\0')){
            int len=0;
            /* open the required converters and cache them */
            myConverterData->myConverterArray[0]=   ucnv_open("ASCII", errorCode );
            myConverterData->myConverterArray[1]=   ucnv_open("ISO8859_1", errorCode);
            myConverterData->myConverterArray[2]=   ucnv_open("ISO8859_7", errorCode);
            myConverterData->myConverterArray[3]=   ucnv_open("jisx-201", errorCode);
            myConverterData->myConverterArray[4]=   ucnv_open("jisx-208", errorCode);
            myConverterData->myConverterArray[5]=   ucnv_open("jisx-212", errorCode);
            myConverterData->myConverterArray[6]=   ucnv_open("ibm-5478", errorCode);   /* gb_2312_80-1 */
            myConverterData->myConverterArray[7]=   ucnv_open("ksc_5601", errorCode);
            myConverterData->myConverterArray[8]=   ucnv_open("jisx-201", errorCode);
            myConverterData->myConverterArray[9]=   NULL;

            /* initialize the state variables */
            setInitialStateToUnicodeJPCN(cnv, myConverterData);
            setInitialStateFromUnicodeJPCN(cnv,myConverterData);

            /* set the function pointers to appropriate funtions */
            cnv->sharedData=(UConverterSharedData*)(&_ISO2022JPData);
            uprv_strcpy(myConverterData->locale,"ja");

            myConverterData->version =options & UCNV_OPTIONS_VERSION_MASK;
            uprv_strcpy(myConverterData->name,"ISO_2022,locale=ja,version=");
            len=strlen(myConverterData->name);
            myConverterData->name[len]=(char)(myConverterData->version+(int)'0');
            myConverterData->name[len+1]='\0';
        }
        else if(myLocale[0]=='k' && (myLocale[1]=='o'|| myLocale[1]=='r') && 
            (myLocale[2]=='_' || myLocale[2]=='\0')){

            /* initialize the state variables */
            setInitialStateToUnicodeKR(cnv, myConverterData);
            setInitialStateFromUnicodeKR(cnv,myConverterData);

            if ((options  & UCNV_OPTIONS_VERSION_MASK)==1){
                    myConverterData->version = 1;
                    myConverterData->currentConverter=myConverterData->fromUnicodeConverter=
                        ucnv_open("icu-internal-25546",errorCode);
                    uprv_strcpy(myConverterData->name,"ISO_2022,locale=ko,version=1");
            }else{
                    myConverterData->currentConverter=myConverterData->fromUnicodeConverter  = ucnv_open("ibm-949",errorCode);
                    myConverterData->version = 0;
                    uprv_strcpy(myConverterData->name,"ISO_2022,locale=ko,version=0");
            }

            /* set the function pointers to appropriate funtions */
            cnv->sharedData=(UConverterSharedData*)&_ISO2022KRData;
            cnv->mode=UCNV_SI;
            uprv_strcpy(myConverterData->locale,"ko");
        }
        else if(((myLocale[0]=='z' && myLocale[1]=='h') || (myLocale[0]=='c'&& myLocale[1]=='n'))&& 
            (myLocale[2]=='_' || myLocale[2]=='\0')){

            /* open the required converters and cache them */
            myConverterData->myConverterArray[0] = ucnv_open("ASCII",errorCode);
            myConverterData->myConverterArray[1] = ucnv_open("ibm-5478",errorCode);   /* gb_2312_80-1 */
            myConverterData->myConverterArray[2] = ucnv_open("iso-ir-165",errorCode);
            myConverterData->myConverterArray[3] = ucnv_open("cns-11643-1992",errorCode);
            myConverterData->myConverterArray[4] = NULL;


            /*initialize the state variables*/
            setInitialStateToUnicodeJPCN(cnv, myConverterData);
            setInitialStateFromUnicodeJPCN(cnv,myConverterData);

            /* set the function pointers to appropriate funtions */
            cnv->sharedData=(UConverterSharedData*)&_ISO2022CNData;
            uprv_strcpy(myConverterData->locale,"cn");

            if ((options  & UCNV_OPTIONS_VERSION_MASK)==1){
                myConverterData->version = 1;
                uprv_strcpy(myConverterData->name,"ISO_2022,locale=zh,version=1");
            }else{
                uprv_strcpy(myConverterData->name,"ISO_2022,locale=zh,version=0");
                myConverterData->version = 0;
            }
        }
        else{
            /* append the UTF-8 escape sequence */
            cnv->charErrorBufferLength = 3;
            cnv->charErrorBuffer[0] = 0x1b;
            cnv->charErrorBuffer[1] = 0x25;
            cnv->charErrorBuffer[2] = 0x42;

            cnv->sharedData=(UConverterSharedData*)&_ISO2022Data;
            /* initialize the state variables */
            myConverterData->isLocaleSpecified=FALSE;
            uprv_strcpy(myConverterData->name,"ISO_2022");
        }

    } else {
        *errorCode = U_MEMORY_ALLOCATION_ERROR;
    }

}


static void
_ISO2022Close(UConverter *converter) {
   UConverterDataISO2022* myData =(UConverterDataISO2022 *) (converter->extraInfo);
   UConverter **array = myData->myConverterArray;

    if (converter->extraInfo != NULL) {
        /*close the array of converter pointers and free the memory*/
        while(*array!=NULL){
            if(*array==myData->currentConverter){
                myData->currentConverter=NULL;
            }
            ucnv_close(*array++);
            
        }

        ucnv_close(myData->currentConverter); /* if not closed above */

        if(!converter->isExtraLocal){
            uprv_free (converter->extraInfo);
        }
    }
}

static void
_ISO2022Reset(UConverter *converter, UConverterResetChoice choice) {
    UConverterDataISO2022 *myConverterData=(UConverterDataISO2022 *) (converter->extraInfo);
    if(! myConverterData->isLocaleSpecified){
        if(choice<=UCNV_RESET_TO_UNICODE) {
            myConverterData->isFirstBuffer = TRUE;
            if (converter->mode == UCNV_SO){
                ucnv_close (myConverterData->currentConverter);
                myConverterData->currentConverter=NULL;
            }
            converter->mode = UCNV_SI; 
        }
        if(choice!=UCNV_RESET_TO_UNICODE) {
            /* re-append UTF-8 escape sequence */
            converter->charErrorBufferLength = 3;
            converter->charErrorBuffer[0] = 0x1b;
            converter->charErrorBuffer[1] = 0x28;
            converter->charErrorBuffer[2] = 0x42;
        }
    }
    else {
        /* reset the state variables */
        if(myConverterData->locale[0] == 'j' || myConverterData->locale[0] == 'c'){
            if(choice<=UCNV_RESET_TO_UNICODE) {
                setInitialStateToUnicodeJPCN(converter, myConverterData);
            }
            if(choice!=UCNV_RESET_TO_UNICODE) {
                setInitialStateFromUnicodeJPCN(converter,myConverterData);
            }
        }
        else if(myConverterData->locale[0] == 'k'){
            if(choice<=UCNV_RESET_TO_UNICODE) {
                setInitialStateToUnicodeKR(converter, myConverterData);
            }
            if(choice!=UCNV_RESET_TO_UNICODE) {
                setInitialStateFromUnicodeKR(converter, myConverterData);
            }
        }
    }
}

static const char* 
_ISO2022getName(const UConverter* cnv){
    if(cnv->extraInfo){
        UConverterDataISO2022* myData= (UConverterDataISO2022*)cnv->extraInfo;
        return myData->name;
    }
    return NULL;
}

static void 
setInitialStateToUnicodeJPCN(UConverter* converter,UConverterDataISO2022 *myConverterData ){
    myConverterData->toUnicodeCurrentState =ASCII;
    myConverterData->currentConverter = NULL;
    myConverterData->isFirstBuffer = TRUE;
    myConverterData->toUnicodeSaveState = INVALID_STATE;
    converter->mode = UCNV_SI;

}

static void 
setInitialStateFromUnicodeJPCN(UConverter* converter,UConverterDataISO2022 *myConverterData){
    myConverterData->fromUnicodeCurrentState= ASCII;
    myConverterData->isEscapeAppended=FALSE;
    myConverterData->isShiftAppended=FALSE;
    myConverterData->isLocaleSpecified=TRUE;
    myConverterData->currentType = ASCII1;
    converter->fromUnicodeStatus = FALSE;

}

static void 
setInitialStateToUnicodeKR(UConverter* converter, UConverterDataISO2022 *myConverterData){

    myConverterData->isLocaleSpecified=TRUE;
    converter->mode = UCNV_SI;
    myConverterData->currentConverter = myConverterData->fromUnicodeConverter;

}

static void 
setInitialStateFromUnicodeKR(UConverter* converter,UConverterDataISO2022 *myConverterData){
   /* in ISO-2022-KR the desginator sequence appears only once
    * in a file so we append it only once
    */
    if( converter->charErrorBufferLength==0){

        converter->charErrorBufferLength = 4;
        converter->charErrorBuffer[0] = 0x1b;
        converter->charErrorBuffer[1] = 0x24;
        converter->charErrorBuffer[2] = 0x29;
        converter->charErrorBuffer[3] = 0x43;
    }
    myConverterData->isLocaleSpecified=TRUE;
    myConverterData->isShiftAppended=FALSE;

}


static U_INLINE void 
CONCAT_ESCAPE_EX(UConverterFromUnicodeArgs* args, 
                               const UChar* source, 
                               unsigned char** target, 
                               const unsigned char* targetLimit,
                               int32_t** offsets, 
                               const char* strToAppend,
                               int len, 
                               UErrorCode* err);

static U_INLINE void 
MBCS_FROM_UCHAR32_ISO2022(UConverterSharedData* sharedData,
                                         UChar32 c,  
                                         uint32_t* value, 
                                         UBool useFallback, 
                                         int* length, 
                                         int outputType);

static U_INLINE void 
MBCS_SINGLE_FROM_UCHAR32(UConverterSharedData* sharedData,
                                       UChar32 c, 
                                       uint32_t* retval, 
                                       UBool useFallback);

static U_INLINE void 
CONCAT_ESCAPE_EX(UConverterFromUnicodeArgs* args, 
                               const UChar* source, 
                               unsigned char** target, 
                               const unsigned char* targetLimit,
                               int32_t** offsets, 
                               const char* strToAppend,
                               int len, 
                               UErrorCode* err)
{

    unsigned char* myTarget = *target;
    int32_t* myOffsets = *offsets;
    while(len-->0){
        if(myTarget < targetLimit){
            *(myTarget++) = (unsigned char) *(strToAppend++);
            if(myOffsets){
                *(myOffsets++) = source -  args->source -1;
            }
        }
        else{
            args->converter->charErrorBuffer[(int)args->converter->charErrorBufferLength++] = (unsigned char) *(strToAppend++);
            *err =U_BUFFER_OVERFLOW_ERROR;
        }
    }
    *target = myTarget;
    *offsets = myOffsets;
}

/* This inline function replicates code in _MBCSFromUChar32() function in ucnvmbcs.c
 * any future change in _MBCSFromUChar32() function should be reflected in 
 * this macro
 */
static U_INLINE void 
MBCS_FROM_UCHAR32_ISO2022(UConverterSharedData* sharedData,
                                         UChar32 c,  
                                         uint32_t* value, 
                                         UBool useFallback, 
                                         int* length, 
                                         int outputType)
{

    const uint16_t *table=sharedData->table->mbcs.fromUnicodeTable;
    uint32_t stage2Entry;
    uint32_t myValue=0;
    const uint8_t *p;
    /* BMP-only codepages are stored without stage 1 entries for supplementary code points */
    if(c<0x10000 || (sharedData->table->mbcs.unicodeMask&UCNV_HAS_SUPPLEMENTARY)) {
        stage2Entry=MBCS_STAGE_2_FROM_U(table, c);
        /* get the bytes and the length for the output */
        if(outputType==MBCS_OUTPUT_2){
            myValue=MBCS_VALUE_2_FROM_STAGE_2(sharedData->table->mbcs.fromUnicodeBytes, stage2Entry, c);
            if(myValue<=0xff) {
                *length=1;
            } else {
                *length=2;
            }
        }else if(outputType==MBCS_OUTPUT_3){
            p=MBCS_POINTER_3_FROM_STAGE_2(sharedData->table->mbcs.fromUnicodeBytes, stage2Entry, c);
            myValue=((uint32_t)*p<<16)|((uint32_t)p[1]<<8)|p[2];
            if(myValue<=0xff) {
                *length=1;
            } else if(myValue<=0xffff) {
                *length=2;
            } else {
                *length=3;
            }
        }
        /* is this code point assigned, or do we use fallbacks? */
        if( (stage2Entry&(1<<(16+(c&0xf))))!=0 ||
            (FROM_U_USE_FALLBACK(useFallback, c) && (myValue!=0 || c==0))
        ) {
            /*
             * We allow a 0 byte output if the Unicode code point is
             * U+0000 and also if the "assigned" bit is set for this entry.
             * There is no way with this data structure for fallback output
             * for other than U+0000 to be a zero byte.
             */
            /* assigned */
            *value=myValue;
        } else {
            *length=0;
        }
    }else{
        *length=0;
    }
}

/* This inline function replicates code in _MBCSSingleFromUChar32() function in ucnvmbcs.c
 * any future change in _MBCSSingleFromUChar32() function should be reflected in 
 * this macro
 */
static U_INLINE void 
MBCS_SINGLE_FROM_UCHAR32(UConverterSharedData* sharedData,
                                       UChar32 c, 
                                       uint32_t* retval, 
                                       UBool useFallback)
{
    const uint16_t *table; 
    int32_t value;
    /* BMP-only codepages are stored without stage 1 entries for supplementary code points */
    if(c>=0x10000 && !(sharedData->table->mbcs.unicodeMask&UCNV_HAS_SUPPLEMENTARY)) {
        value= -1;
    }
    /* convert the Unicode code point in c into codepage bytes (same as in _MBCSFromUnicodeWithOffsets) */
    table=sharedData->table->mbcs.fromUnicodeTable;
    /* get the byte for the output */
    value=MBCS_SINGLE_RESULT_FROM_U(table, (uint16_t *)sharedData->table->mbcs.fromUnicodeBytes, c);
    /* is this code point assigned, or do we use fallbacks? */
    if(useFallback ? value>=0x800 : value>=0xc00) {
        value &=0xff;
    } else {
        value= -1;
    }
    *retval=(uint16_t) value;
}

/**********************************************************************************
*  ISO-2022 Converter
*
*
*/

static UChar32 
T_UConverter_getNextUChar_ISO_2022(UConverterToUnicodeArgs* args,
                                                   UErrorCode* err){
    const char* mySourceLimit;
    int plane=0; /*dummy variable*/
    UConverterDataISO2022* myData =((UConverterDataISO2022*)(args->converter->extraInfo));
    /*Arguments Check*/
    if  (args->sourceLimit < args->source){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return 0xffff;
    }

    while(1){

        mySourceLimit = getEndOfBuffer_2022(&(args->source), args->sourceLimit, TRUE);
        /*Find the end of the buffer e.g : Next Escape Seq | end of Buffer*/
        if (args->converter->mode == UCNV_SO && mySourceLimit!=args->source) 
            /*Already doing some conversion*/{

            return ucnv_getNextUChar(myData->currentConverter,
                &(args->source),
                mySourceLimit,
                err);
        }
        /*-Done with buffer with entire buffer
        *-Error while converting
        */
        changeState_2022(args->converter,
               &(args->source), 
               args->sourceLimit,
               TRUE,
               ISO_2022,
               &plane,
               err);
        if(args->source >= args->sourceLimit){
            *err = U_INDEX_OUTOFBOUNDS_ERROR;
            break;
        }
    }

    if( (args->source == args->sourceLimit) && args->flush){
        _ISO2022Reset(args->converter,UCNV_RESET_TO_UNICODE);
    }
    return 0xffff;
}

static void 
T_UConverter_toUnicode_ISO_2022(UConverterToUnicodeArgs *args,
                                             UErrorCode* err){

    const char *mySourceLimit;
    char const* sourceStart;
    UConverter *saveThis;
    int plane =0; /*dummy variable*/
    UConverterDataISO2022* myData;

    if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    myData= ((UConverterDataISO2022*)(args->converter->extraInfo));
    while (args->source < args->sourceLimit) {

        /*Find the end of the buffer e.g : Next Escape Seq | end of Buffer*/
        mySourceLimit = getEndOfBuffer_2022(&(args->source), args->sourceLimit, args->flush);

        if (args->converter->mode == UCNV_SO) /*Already doing some conversion*/{

            saveThis = args->converter;
            args->offsets = NULL;
            args->converter = myData->currentConverter;
            ucnv_toUnicode(args->converter,
                &args->target,
                args->targetLimit,
                &args->source,
                mySourceLimit,
                args->offsets,
                args->flush,
                err);
            args->converter = saveThis;
            myData->isFirstBuffer = FALSE;
        }
        if((myData->isFirstBuffer) && (args->source[0]!=(char)ESC_2022)
            &&  (myData->currentConverter==NULL)){


            saveThis = args->converter;
            args->offsets = NULL;
            myData->currentConverter = ucnv_open("ASCII",err);

            if(U_FAILURE(*err)){
                break;
            }

            args->converter = myData->currentConverter;
            ucnv_toUnicode(args->converter,
                &args->target,
                args->targetLimit,
                &args->source,
                mySourceLimit,
                args->offsets,
                args->flush,
                err);
            args->converter = saveThis;
            args->converter->mode = UCNV_SO;
            myData->isFirstBuffer=FALSE;

        }

        /*-Done with buffer with entire buffer
        -Error while converting
        */

        if (U_FAILURE(*err) || (args->source == args->sourceLimit)) 
            return;

        sourceStart = args->source;
        changeState_2022(args->converter,
               &(args->source), 
               args->sourceLimit,
               TRUE,
               ISO_2022,
               &plane,
               err);
        /* args->source = sourceStart; */


    }

    myData->isFirstBuffer=FALSE;
    if( (args->source == args->sourceLimit) && args->flush){
        _ISO2022Reset(args->converter,UCNV_RESET_FROM_UNICODE);
    }
    
}   

static void 
T_UConverter_toUnicode_ISO_2022_OFFSETS_LOGIC(UConverterToUnicodeArgs* args,
                                                           UErrorCode* err){

    int32_t myOffset=0;
    int32_t base = 0;
    const char* mySourceLimit;
    char const* sourceStart;
    UConverter* saveThis;
    int plane =0;/*dummy variable*/
    UConverterDataISO2022* myData;

    if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    myData=((UConverterDataISO2022*)(args->converter->extraInfo));
    
    while (args->source < args->sourceLimit) {
        mySourceLimit = getEndOfBuffer_2022(&(args->source), args->sourceLimit, args->flush);
        /*Find the end of the buffer e.g : Next Escape Seq | end of Buffer*/

        if (args->converter->mode == UCNV_SO) /*Already doing some conversion*/{
            const UChar* myTargetStart = args->target;

            saveThis = args->converter;
            args->converter = myData->currentConverter;
            ucnv_toUnicode(args->converter, 
                &(args->target),
                args->targetLimit,
                &(args->source),
                mySourceLimit,
                args->offsets,
                args->flush,
                err);

            myData->isFirstBuffer = FALSE;

            args->converter = saveThis;
            {
                int32_t lim =  args->target - myTargetStart;
                int32_t i = 0;
                for (i=base; i < lim;i++){
                    args->offsets[i] += myOffset;
                }
                base += lim;
            }

        }
        if(myData->isFirstBuffer && args->source[0]!=ESC_2022
            && (myData->currentConverter==NULL)){

            const UChar* myTargetStart = args->target;
            saveThis = args->converter;
            args->offsets = NULL;
            myData->currentConverter = ucnv_open("ASCII",err);

            if(U_FAILURE(*err)){
                break;
            }

            args->converter = myData->currentConverter;
            ucnv_toUnicode(args->converter,
                &args->target,
                args->targetLimit,
                &args->source,
                mySourceLimit,
                args->offsets,
                args->flush,
                err);
            args->converter = saveThis;
            args->converter->mode = UCNV_SO;
            myData->isFirstBuffer=FALSE;
/*            args->converter = saveThis;*/
            {
                int32_t lim =  args->target - myTargetStart;
                int32_t i = 0;
                for (i=base; i < lim;i++){
                    args->offsets[i] += myOffset;
                }
                base += lim;
            }
        }
        /*-Done with buffer with entire buffer
        -Error while converting
        */

        if (U_FAILURE(*err) || (args->source == args->sourceLimit)) 
            return;

        sourceStart = args->source;
        changeState_2022(args->converter,
               &(args->source), 
               args->sourceLimit,
               TRUE,
               ISO_2022,
               &plane,
               err);
        myOffset += args->source - sourceStart;

    }
    if( (args->source == args->sourceLimit) && args->flush){
        _ISO2022Reset(args->converter,UCNV_RESET_TO_UNICODE);
    }
}

static UCNV_TableStates_2022 
getKey_2022(char c,int32_t* key,int32_t* offset){
    int32_t togo = *key;
    int32_t low = 0;
    int32_t hi = MAX_STATES_2022;
    int32_t oldmid=0;

    if (*key == 0){
        togo = (int8_t)normalize_esq_chars_2022[(int)c];
    }
    else{
        togo <<= 5;
        togo += (int8_t)normalize_esq_chars_2022[(int)c];
    }

    while (hi != low)  /*binary search*/{

        register int32_t mid = (hi+low) >> 1; /*Finds median*/

        if (mid == oldmid) 
            break;

        if (escSeqStateTable_Key_2022[mid] > togo){
            hi = mid;
        }
        else if (escSeqStateTable_Key_2022[mid] < togo){
            low = mid;
        }
        else /*we found it*/{
            *key = togo;
            *offset = mid;
            return escSeqStateTable_Value_2022[mid];
        }
        oldmid = mid;

    }

    *key = 0;
    *offset = 0;
    return INVALID_2022;
}



/*Checks the characters of the buffer against valid 2022 escape sequences
*if the match we return a pointer to the initial start of the sequence otherwise
*we return sourceLimit
*/
static const char* 
getEndOfBuffer_2022(const char** source,
                   const char* sourceLimit,
                   UBool flush){

    const char* mySource = *source;

    if (*source >= sourceLimit) 
        return sourceLimit;

    do{

        if (*mySource == ESC_2022){
            int8_t i;
            int32_t key = 0;
            int32_t offset;
            UCNV_TableStates_2022 value = VALID_NON_TERMINAL_2022;

            /* Kludge: I could not
            * figure out the reason for validating an escape sequence
            * twice - once here and once in changeState_2022().
            * is it possible to have an ESC character in a ISO2022
            * byte stream which is valid in a code page? Is it legal?
            */
            for (i=0; 
            (mySource+i < sourceLimit)&&(value == VALID_NON_TERMINAL_2022);
            i++) {
                value =  getKey_2022(*(mySource+i), &key, &offset);
            }
            if (value > 0 || *mySource==ESC_2022) 
                return mySource;

            if ((value == VALID_NON_TERMINAL_2022)&&(!flush) ) 
                return sourceLimit;
        }
    }while (++mySource < sourceLimit);

    return sourceLimit;
}

/*
 * From Unicode Callback helper function
 */
static void 
fromUnicodeCallback(UConverterFromUnicodeArgs* args,const UChar32 sourceChar,const UChar** pSource,
                    unsigned char** pTarget,int32_t** pOffsets,UConverterCallbackReason reason, UErrorCode* err){
                
    /*variables for callback */
    const UChar* saveSource =NULL;
    char* saveTarget =NULL;
    int32_t* saveOffsets =NULL;
    int currentOffset =0;
    int saveIndex =0;
    int32_t* offsets = *pOffsets;
    const UChar* source = *pSource;
    unsigned char* target = *pTarget;

    args->converter->invalidUCharLength = 0;
    
    if(sourceChar>0xffff){
        args->converter->invalidUCharBuffer[args->converter->invalidUCharLength++] =(uint16_t)(((sourceChar)>>10)+0xd7c0);
        args->converter->invalidUCharBuffer[args->converter->invalidUCharLength++] =(uint16_t)(((sourceChar)&0x3ff)|0xdc00);
    }
    else{
        args->converter->invalidUCharBuffer[args->converter->invalidUCharLength++] =(UChar)sourceChar;
    }
    if(offsets)
        currentOffset = *(offsets-1)+1;

    saveSource = args->source;
    saveTarget = args->target;
    saveOffsets = args->offsets;
    args->target = (char*)target;
    args->source = source;
    args->offsets = offsets;

    /*copies current values for the ErrorFunctor to update */
    /*Calls the ErrorFunctor */
    args->converter->fromUCharErrorBehaviour ( args->converter->fromUContext, 
                  args, 
                  args->converter->invalidUCharBuffer, 
                  args->converter->invalidUCharLength, 
                 (UChar32) (sourceChar), 
                  reason, 
                  err);

    saveIndex = args->target - (char*)target;
    if(args->offsets){
        args->offsets = saveOffsets;
        while(saveIndex-->0){
             *offsets = currentOffset;
              offsets++;
        }
    }
    target = (unsigned char*)args->target;
    *pTarget=target;
    *pOffsets=offsets;
    args->source=saveSource;
    args->target=saveTarget;
    args->offsets=saveOffsets;
    args->converter->fromUSurrogateLead=0x00;

}

/*
 * To Unicode Callback helper function
 */
static void 
toUnicodeCallback(UConverterToUnicodeArgs* args, const uint32_t sourceChar,const char** pSource,
                              const uint32_t targetUniChar,UChar** pTarget,UErrorCode* err){

    const char *saveSource = args->source;
    UChar *saveTarget = args->target;
    const char* source = *pSource;
    UChar* target = *pTarget;
    int32_t *saveOffsets = NULL;
    UConverterCallbackReason reason;
    int32_t currentOffset;
    int32_t saveIndex = target - args->target;

    args->converter->invalidCharLength=0;

    if(sourceChar>0xff){
        currentOffset= source - args->source - 2;
        args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = (char)(sourceChar>>8);
        args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = (char)sourceChar;
    }
    else{

        currentOffset= source - args->source -1;
        args->converter->invalidCharBuffer[args->converter->invalidCharLength++] =(char) sourceChar;
    }

    if(targetUniChar == (missingCharMarker-1/*0xfffe*/)){
        reason = UCNV_UNASSIGNED;
        *err = U_INVALID_CHAR_FOUND;
    }
    else{
        reason = UCNV_ILLEGAL;
        *err = U_ILLEGAL_CHAR_FOUND;
    }

    if(args->offsets){
        saveOffsets=args->offsets;
        args->offsets = args->offsets+(target - args->target);
    }

    args->target =target;
    target =saveTarget;
    args->source = source;

    args->converter->fromCharErrorBehaviour ( 
         args->converter->toUContext, 
         args, 
         args->converter->invalidCharBuffer, 
         args->converter->invalidCharLength, 
         reason, 
         err);

    if(args->offsets){
        args->offsets = saveOffsets;

        for (;saveIndex < (args->target - target);saveIndex++) {
          args->offsets[saveIndex] += currentOffset;
        }
    }
    target=args->target;
    *pTarget=target;
    args->source  = saveSource;
    args->target  = saveTarget;
}

/**************************************ISO-2022-JP*************************************************/

/************************************** IMPORTANT **************************************************
* The UConverter_fromUnicode_ISO2022_JP converter does not use ucnv_fromUnicode() functions for SBCS,DBCS and
* MBCS; instead, the values are obtained directly by calling _MBCSFromUChar32().
* The converter iterates over each Unicode codepoint 
* to obtain the equivalent codepoints from the codepages supported. Since the source buffer is 
* processed one char at a time it would make sense to reduce the extra processing a canned converter 
* would do as far as possible.
*
* If the implementation of these macros or structure of sharedData struct change in the future, make 
* sure that ISO-2022 is also changed. 
***************************************************************************************************
*/

/***************************************************************************************************
* Rules for ISO-2022-jp encoding
* (i)   Escape sequences must be fully contained within a line they should not 
*       span new lines or CRs
* (ii)  If the last character on a line is represented by two bytes then an ASCII or
*       JIS-Roman character escape sequence should follow before the line terminates
* (iii) If the first character on the line is represented by two bytes then a two 
*       byte character escape sequence should precede it    
* (iv)  If no escape sequence is encountered then the characters are ASCII
* (v)   Latin(ISO-8859-1) and Greek(ISO-8859-7) characters must be designated to G2,
*       and invoked with SS2 (ESC N).
* (vi)  If there is any G0 designation in text, there must be a switch to
*       ASCII or to JIS X 0201-Roman before a space character (but not
*       necessarily before "ESC 4/14 2/0" or "ESC N ' '") or control
*       characters such as tab or CRLF.
* (vi)  Supported encodings:
*          ASCII, JISX201, JISX208, JISX212, GB2312, KSC5601, ISO-8859-1,ISO-8859-7
*
*  source : RFC-1554
*
*          JISX201, JISX208,JISX212 : new .cnv data files created
*          KSC5601 : alias to ibm-949 mapping table
*          GB2312 : alias to ibm-1386 mapping table
*          ISO-8859-1 : Algorithmic implemented as LATIN1 case
*          ISO-8859-7 : alisas to ibm-9409 mapping table
*/
#define MAX_VALID_CP_JP 9
static const Cnv2022Type myConverterType[MAX_VALID_CP_JP]={
    ASCII1,
    LATIN1,
    SBCS,
    SBCS,
    DBCS,
    DBCS,
    DBCS,
    DBCS,
    SBCS,

};

static const StateEnum nextStateArray[5][MAX_VALID_CP_JP]= {
    {JISX201 ,INVALID_STATE,INVALID_STATE,JISX208,ASCII,INVALID_STATE,INVALID_STATE,INVALID_STATE,INVALID_STATE},
    {JISX201,INVALID_STATE,INVALID_STATE,JISX208,JISX212,ASCII,INVALID_STATE,INVALID_STATE,INVALID_STATE},
    {ISO8859_1,ISO8859_7,JISX201,JISX208,JISX212,GB2312,KSC5601,ASCII,INVALID_STATE},
    {JISX201,INVALID_STATE,INVALID_STATE,JISX208,JISX212,HWKANA_7BIT,INVALID_STATE,INVALID_STATE,ASCII},
    {JISX201,INVALID_STATE,INVALID_STATE,JISX208,JISX212,ASCII,INVALID_STATE,INVALID_STATE,INVALID_STATE},
};
static const char escSeqChars[MAX_VALID_CP_JP][6] ={
    "\x1B\x28\x42",         /* <ESC>(B  ASCII       */
    "\x1B\x2E\x41",         /* <ESC>.A  ISO-8859-1  */
    "\x1B\x2E\x46",         /* <ESC>.F  ISO-8859-7  */
    "\x1B\x28\x4A",         /* <ESC>(J  JISX-201    */
    "\x1B\x24\x42",         /* <ESC>$B  JISX-208    */
    "\x1B\x24\x28\x44",     /* <ESC>$(D JISX-212    */
    "\x1B\x24\x41",         /* <ESC>$A  GB2312      */
    "\x1B\x24\x28\x43",     /* <ESC>$(C KSC5601     */
    "\x1B\x28\x49"          /* <ESC>(I  HWKANA_7BIT */

};
static  const int32_t escSeqCharsLen[MAX_VALID_CP_JP] ={
    3, /* length of  <ESC>(B  ASCII      */
    3, /* length of <ESC>.A  ISO-8859-1  */
    3, /* length of <ESC>.F  ISO-8859-7  */
    3, /* length of <ESC>(J  JISX-201    */
    3, /* length of <ESC>$B  JISX-208    */
    4, /* length of <ESC>$(D JISX-212    */
    3, /* length of <ESC>$A  GB2312      */
    4, /* length of <ESC>$(C KSC5601     */
    3  /* length of <ESC>(I  HWKANA_7BIT */
};

/*
* The iteration over various code pages works this way:
* i)   Get the currentState from myConverterData->currentState
* ii)  Check if the character is mapped to a valid character in the currentState
*      Yes ->  a) set the initIterState to currentState
*       b) remain in this state until an invalid character is found
*      No  ->  a) go to the next code page and find the character
* iii) Before changing the state increment the current state check if the current state 
*      is equal to the intitIteration state
*      Yes ->  A character that cannot be represented in any of the supported encodings
*       break and return a U_INVALID_CHARACTER error
*      No  ->  Continue and find the character in next code page
*
*
* TODO: Implement a priority technique where the users are allowed to set the priority of code pages 
*/

static void 
UConverter_fromUnicode_ISO_2022_JP_OFFSETS_LOGIC(UConverterFromUnicodeArgs* args, UErrorCode* err){

    UConverterDataISO2022 *converterData;
    unsigned char* target = (unsigned char*) args->target;
    const unsigned char* targetLimit = (const unsigned char*) args->targetLimit;
    const UChar* source = args->source;
    const UChar* sourceLimit = args->sourceLimit;
    int32_t* offsets = args->offsets;
    int32_t offset = 0;
    uint32_t targetByteUnit = missingCharMarker;
    UChar32 sourceChar  =0x0000;
    const char* escSeq = NULL;
    int len =0; /*length of escSeq chars*/
    UConverterCallbackReason reason;
    UConverterSharedData* sharedData=NULL;
    UBool useFallback; 

    /* state variables*/
    StateEnum* currentState;       
    StateEnum initIterState;       
    UConverter** currentConverter; 
    Cnv2022Type* currentType;      
    UConverter** convArray;        
    
    /* arguments check*/
    if ((args->converter == NULL) || (targetLimit < target) || (sourceLimit < source)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    /* Initialize  */
    converterData       = (UConverterDataISO2022*)args->converter->extraInfo;
    useFallback         = args->converter->useFallback;
    currentState        = &converterData->fromUnicodeCurrentState;
    initIterState       = ASCII;
    currentConverter    = &converterData->fromUnicodeConverter;
    convArray           = converterData->myConverterArray;
    initIterState       = *currentState;
    currentType         = &converterData->currentType;
    
    /* check if the last codepoint of previous buffer was a lead surrogate*/
    if(args->converter->fromUSurrogateLead!=0 && target< targetLimit) {
        goto getTrail;
    }
    
    *currentConverter = convArray[(*currentConverter==NULL) ? 0 : (int)*currentState];
    sharedData= (*currentConverter)->sharedData;
    
    while( source < sourceLimit){

        targetByteUnit = missingCharMarker;

        if(target < targetLimit){
            sourceChar  = *(source++);
            if(sourceChar > SPACE) {
                do{
                    switch (*currentType){
                    /* most common case*/
                    case DBCS:
                        {
                            uint32_t value=0;
                            int length=0;
                            /*if(2 == _MBCSFromUChar32(sharedData,sourceChar, &value, useFallback)) {
                                targetByteUnit = (uint16_t)value;
                            }*/
                            MBCS_FROM_UCHAR32_ISO2022(sharedData,sourceChar,&value,useFallback,&length,MBCS_OUTPUT_2);
                            if(length==2){
                                targetByteUnit =  value;
                            }
                        }
                        break;
                    case ASCII1:
                        if(sourceChar < 0x7f){
                            targetByteUnit = sourceChar;
                        }
                        break;

                    case SBCS:
                        MBCS_SINGLE_FROM_UCHAR32(sharedData,sourceChar,&targetByteUnit,useFallback);
                        /*targetByteUnit=(uint16_t)_MBCSSingleFromUChar32(sharedData,sourceChar,useFallback);*/
                        /*
                         * If mySourceChar is unassigned, then _MBCSSingleFromUChar32() returns -1
                         * which becomes the same as missingCharMarker with the cast to uint16_t.
                         */
                        /* Check if the sourceChar is in the HW Kana range*/
                        if(0xFF9F-sourceChar<=(0xFF9F-0xFF61)){
                            if( converterData->version==3){
                                /*we get a1-df from _MBCSSingleFromUChar32 so subtract 0x80*/
                                targetByteUnit-=0x80; 
                                *currentState = HWKANA_7BIT;
                            }
                            else if( converterData->version==4){
                                *currentState = JISX201;
                            }
                            else{
                                targetByteUnit=missingCharMarker;
                            }
                            *currentConverter = convArray[(*currentConverter==NULL) ? 0 : (int)*currentState];
                            *currentType = (Cnv2022Type) myConverterType[*currentState];
                        }
                        break;

                    case LATIN1:
                        if(sourceChar <= 0x00FF){
                            targetByteUnit = sourceChar;
                        }

                        break;
                    default:
                        /*not expected */
                        break;
                    }
                    if(targetByteUnit==missingCharMarker){
                        *currentState = nextStateArray[converterData->version][*currentState];
                        *currentConverter = convArray[(*currentConverter==NULL) ? 0 : (int)*currentState];
                        *currentType = (Cnv2022Type) myConverterType[*currentState];
                        sharedData= (*currentConverter)->sharedData;
                   }
                   else
                       /*got the mapping so break from while loop*/
                       break;

                }while(initIterState != *currentState);

            }
            else{
                targetByteUnit = sourceChar;
                *currentState = ASCII;
                *currentType = (Cnv2022Type) myConverterType[*currentState];
            }

            if(targetByteUnit != missingCharMarker){

                if( *currentState != initIterState){

                    escSeq = escSeqChars[(int)*currentState];
                    len = escSeqCharsLen[(int)*currentState];

                    CONCAT_ESCAPE_EX(args,source, &target,targetLimit, &offsets, escSeq,len,err);

                    /* Append SSN for shifting to G2 */
                    if(*currentState==ISO8859_1 || *currentState==ISO8859_7){
                        escSeq = UCNV_SS2;
                        len = UCNV_SS2_LEN;
                        CONCAT_ESCAPE_EX(args, source, &target, targetLimit,&offsets, escSeq,len,err);
                    }
                }
                initIterState = *currentState;
                offset = source - args->source -1;
                /* write the targetByteUnit  to target */
                if(targetByteUnit <= 0x00FF){
                    if( target <targetLimit){
                        *(target++) = (unsigned char) targetByteUnit;
                        if(offsets){
                            *(offsets++) = offset;
                        }

                    }else{
                        args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (unsigned char) targetByteUnit;
                        *err = U_BUFFER_OVERFLOW_ERROR;
                    }
                }else{
                    if(target < targetLimit){
                        *(target++) =(unsigned char) (targetByteUnit>>8);
                        if(offsets){
                              *(offsets++) = offset;
                        }
                        if(target < targetLimit){
                            *(target++) =(unsigned char) (targetByteUnit);
                            if(offsets){
                                *(offsets++) = offset;
                            }

                        }else{
                            args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (unsigned char) (targetByteUnit);
                            *err = U_BUFFER_OVERFLOW_ERROR;
                        }
                    }else{
                        args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (unsigned char) (targetByteUnit>>8);
                        args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (unsigned char) (targetByteUnit);
                        *err = U_BUFFER_OVERFLOW_ERROR;
                    }
                }
            }
            else{

                /* if we cannot find the character after checking all codepages 
                 * then this is an error
                 */
                reason = UCNV_UNASSIGNED;
                *err = U_INVALID_CHAR_FOUND;

                /*check if the char is a First surrogate*/
                if(UTF_IS_SURROGATE(sourceChar)) {
                    if(UTF_IS_SURROGATE_FIRST(sourceChar)) {
                        args->converter->fromUSurrogateLead=(UChar)sourceChar;
getTrail:
                        /*look ahead to find the trail surrogate*/
                        if(source <  sourceLimit) {
                            /* test the following code unit */
                            UChar trail=(UChar) *source;
                            if(UTF_IS_SECOND_SURROGATE(trail)) {
                                source++;
                                sourceChar=UTF16_GET_PAIR_VALUE(args->converter->fromUSurrogateLead, trail);
                                args->converter->fromUSurrogateLead=0x00;
                                reason =UCNV_UNASSIGNED;
                                *err = U_INVALID_CHAR_FOUND;
                                /* convert this surrogate code point */
                                /* exit this condition tree */
                            } else {
                                /* this is an unmatched lead code unit (1st surrogate) */
                                /* callback(illegal) */
                                reason=UCNV_ILLEGAL;
                                *err=U_ILLEGAL_CHAR_FOUND;
                            }
                        } else {
                            /* no more input */
                            *err = U_ZERO_ERROR;
                            break;
                        }
                    } else {
                        /* this is an unmatched trail code unit (2nd surrogate) */
                        /* callback(illegal) */
                        reason=UCNV_ILLEGAL;
                        *err=U_ILLEGAL_CHAR_FOUND;
                    }
                }
                /* Call the callback function*/
                fromUnicodeCallback(args,sourceChar,&source,&target,&offsets,reason,err);
                initIterState = *currentState;
                if (U_FAILURE (*err)){
                    break;
                }
            }
        } /* end if(myTargetIndex<myTargetLength) */
        else{
            *err =U_BUFFER_OVERFLOW_ERROR;
            break;
        }

    }/* end while(mySourceIndex<mySourceLength) */


    /*If at the end of conversion we are still carrying state information
     *flush is TRUE, we can deduce that the input stream is truncated
     */
    if (args->converter->fromUSurrogateLead !=0 && (source == sourceLimit) && args->flush){
        *err = U_TRUNCATED_CHAR_FOUND;
    }
    /* Reset the state of converter if we consumed 
     * the source and flush is true
     */
    if( (source == sourceLimit) && args->flush){
        setInitialStateFromUnicodeJPCN(args->converter,converterData);
    }

    /*save the state and return */
    args->source = source;
    args->target = (char*)target;
}

/*************** to unicode *******************/

/****************************************************************************
 * Recognized escape sequences are
 * <ESC>(B  ASCII
 * <ESC>.A  ISO-8859-1
 * <ESC>.F  ISO-8859-7
 * <ESC>(J  JISX-201
 * <ESC>(I  JISX-201
 * <ESC>$B  JISX-208
 * <ESC>$@  JISX-208
 * <ESC>$(D JISX-212
 * <ESC>$A  GB2312
 * <ESC>$(C KSC5601
 */
static const StateEnum nextStateToUnicodeJP[5][MAX_STATES_2022]= {
    {
/*      0                1               2               3               4               5               6               7               8               9    */
    INVALID_STATE   ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,ASCII          ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,JISX201        ,HWKANA_7BIT    ,JISX201        ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,JISX208        ,INVALID_STATE  ,JISX208        ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,JISX208        ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    },
    {
/*      0                1               2               3               4               5               6               7               8               9    */
    INVALID_STATE   ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,ASCII          ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,JISX201        ,HWKANA_7BIT    ,JISX201        ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,JISX208        ,INVALID_STATE  ,JISX208        ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,JISX208        ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,JISX212        ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    },
    {
/*      0                1               2               3               4               5               6               7               8               9    */
    INVALID_STATE   ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,ASCII          ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,JISX201        ,HWKANA_7BIT    ,JISX201        ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,JISX208        ,GB2312         ,JISX208        ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,ISO8859_1      ,ISO8859_7      ,JISX208        ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,KSC5601        ,JISX212        ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    },
    {
/*      0                1               2               3               4               5               6               7               8               9    */
    INVALID_STATE   ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,ASCII          ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,JISX201        ,HWKANA_7BIT    ,JISX201        ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,JISX208        ,GB2312         ,JISX208        ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,ISO8859_1      ,ISO8859_7      ,JISX208        ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,KSC5601        ,JISX212        ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    },
    {
/*      0                1               2               3               4               5               6               7               8               9    */
    INVALID_STATE   ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,ASCII          ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,JISX201        ,HWKANA_7BIT    ,JISX201        ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,JISX208        ,GB2312         ,JISX208        ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,ISO8859_1      ,ISO8859_7      ,JISX208        ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,KSC5601        ,JISX212        ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE  ,INVALID_STATE
    }
};

static void 
UConverter_toUnicode_ISO_2022_JP_OFFSETS_LOGIC(UConverterToUnicodeArgs *args,
                                                            UErrorCode* err){
    char tempBuf[2];
    const char *mySource = ( char *) args->source;
    UChar *myTarget = args->target;
    const char *mySourceLimit = args->sourceLimit;
    uint32_t targetUniChar = 0x0000;
    uint32_t mySourceChar = 0x0000;
    UConverterDataISO2022* myData;
    StateEnum* currentState;
    uint32_t* toUnicodeStatus;
    int plane = 0; /*dummy variable*/

    if ((args->converter == NULL) || (myTarget < args->target) || (mySource < args->source)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    myData=(UConverterDataISO2022*)(args->converter->extraInfo);
    currentState =  &myData->toUnicodeCurrentState;
    toUnicodeStatus = &args->converter->toUnicodeStatus;
    while(mySource< args->sourceLimit){

        targetUniChar = missingCharMarker;

        if(myTarget < args->targetLimit){

            mySourceChar= (unsigned char) *mySource++;
            
            /* Consume the escape sequences and ascertain the state */
            if(mySourceChar==UCNV_SI){
                if(myData->version==3 && *toUnicodeStatus==0x00){
                    if(myData->toUnicodeSaveState!=INVALID_STATE){
                        *currentState = (StateEnum) myData->toUnicodeSaveState;
                        continue;
                    }
                    else{
                        *err =U_ILLEGAL_CHAR_FOUND;
                        goto CALLBACK;
                    }
        
                }
                else{
                    goto CALLBACK;
                }
            }else if(mySourceChar==UCNV_SO){
                if(myData->version==3 && *toUnicodeStatus==0x00){
                    myData->toUnicodeSaveState= (int) *currentState;
                    *currentState = HWKANA_7BIT;
                    continue;
                }
                else{
                    goto CALLBACK;
                }
            }else if(mySourceChar==ESC_2022 || myData->key!=0){
                if(*toUnicodeStatus== 0x00){
                        mySource--;
                        changeState_2022(args->converter,&(mySource), 
                            args->sourceLimit, args->flush,ISO_2022_JP,&plane, err);
                        /*Invalid or illegal escape sequence */
                        if(U_SUCCESS(*err)){
                            continue;

                        }
                        else{
                            args->target = myTarget;
                            args->source = mySource;
                            return;
                        }
                  }
                else{
                    goto CALLBACK;
                }           
            }

            switch(myConverterType[*currentState]){
            case DBCS:
                if(*toUnicodeStatus== 0x00){
                    *toUnicodeStatus= (UChar) mySourceChar;
                    continue;
                }
                else{
                    const char *pBuf;

                    tempBuf[0] = (char) args->converter->toUnicodeStatus;
                    tempBuf[1] = (char) mySourceChar;
                    mySourceChar+= (args->converter->toUnicodeStatus)<<8;
                    *toUnicodeStatus= 0;
                    pBuf = tempBuf;
                    targetUniChar = _MBCSSimpleGetNextUChar(myData->currentConverter->sharedData, &pBuf, tempBuf+2, args->converter->useFallback);
                }
                break;


            case ASCII1:
                if( mySourceChar < 0x7F){
                    targetUniChar = (UChar) mySourceChar;
                }
                else if((uint8_t)(mySourceChar - 0xa1) <= (0xdf - 0xa1) && myData->version==4) {
                    /* 8-bit halfwidth katakana in any single-byte mode for JIS8 */
                    targetUniChar = _MBCS_SINGLE_SIMPLE_GET_NEXT_BMP(myData->myConverterArray[JISX201]->sharedData, mySourceChar);
                }

                break;

            case SBCS:
                if((uint8_t)(mySourceChar - 0xa1) <= (0xdf - 0xa1) && myData->version==4) {
                    /* 8-bit halfwidth katakana in any single-byte mode for JIS8 */
                    targetUniChar = _MBCS_SINGLE_SIMPLE_GET_NEXT_BMP(myData->myConverterArray[JISX201]->sharedData, mySourceChar);
                }
                else if(*currentState==HWKANA_7BIT){
                    targetUniChar = _MBCS_SINGLE_SIMPLE_GET_NEXT_BMP(myData->myConverterArray[JISX201]->sharedData, mySourceChar+0x80);   
                }
                else {
                    targetUniChar = _MBCS_SINGLE_SIMPLE_GET_NEXT_BMP(myData->currentConverter->sharedData, mySourceChar);
                }

                break;

            case LATIN1:
                
                targetUniChar = (UChar) mySourceChar;
                break;

            case INVALID_STATE:
                *err = U_ILLEGAL_ESCAPE_SEQUENCE;
                args->target = myTarget;
                args->source = mySource;
                return;

            default:
                /* For non-valid state MBCS and others */
                break;
            }
            if(targetUniChar < (missingCharMarker-1/*0xfffe*/)){
                if(args->offsets){
                    args->offsets[myTarget - args->target]= mySource - args->source - 2 
                                                            +(myConverterType[*currentState] <= SBCS);

                }
                *(myTarget++)=(UChar)targetUniChar;
                targetUniChar=missingCharMarker;
            }
            else{
CALLBACK:

                /* Call the callback function*/
                toUnicodeCallback(args,mySourceChar,&mySource,targetUniChar,&myTarget,err);
                /*args->offsets = saveOffsets;*/
                if(U_FAILURE(*err))
                    break;

            }
        }
        else{
            *err =U_BUFFER_OVERFLOW_ERROR;
            break;
        }
    }
    if((args->flush==TRUE)
        && (mySource == mySourceLimit) 
        && ( *toUnicodeStatus!=0x00)){

        *err = U_TRUNCATED_CHAR_FOUND;
        *toUnicodeStatus= 0x00;
    }
    /* Reset the state of converter if we consumed 
     * the source and flush is true
     */
    if( (mySource == mySourceLimit) && args->flush){
        setInitialStateToUnicodeJPCN(args->converter,myData);
    }
    args->target = myTarget;
    args->source = mySource;
}



/***************************************************************
*   Rules for ISO-2022-KR encoding
*   i) The KSC5601 designator sequence should appear only once in a file, 
*      at the begining of a line before any KSC5601 characters. This usually
*      means that it appears by itself on the first line of the file
*  ii) There are only 2 shifting sequences SO to shift into double byte mode
*      and SI to shift into single byte mode
*/
static void 
UConverter_fromUnicode_ISO_2022_KR_OFFSETS_LOGIC_IBM(UConverterFromUnicodeArgs* args, UErrorCode* err){

     UConverter* saveConv = args->converter;
     UConverterDataISO2022 *myConverterData=(UConverterDataISO2022*)args->converter->extraInfo;
     args->converter=myConverterData->currentConverter;
     _MBCSFromUnicodeWithOffsets(args,err);
     if(U_FAILURE(*err)){
         if(args->converter->charErrorBufferLength!=0){
            uprv_memcpy(saveConv->charErrorBuffer, args->converter->charErrorBuffer,
                            args->converter->charErrorBufferLength);
            saveConv->charErrorBufferLength=args->converter->charErrorBufferLength;
            args->converter->charErrorBufferLength=0;
         }
         if(args->converter->invalidUCharLength!=0){
            uprv_memcpy(saveConv->invalidUCharBuffer, args->converter->invalidUCharBuffer,
                            args->converter->invalidUCharLength);
            saveConv->invalidUCharLength=args->converter->invalidUCharLength;
            args->converter->invalidCharLength=0;
         }
     }
     args->converter=saveConv;
}

static void 
UConverter_fromUnicode_ISO_2022_KR_OFFSETS_LOGIC(UConverterFromUnicodeArgs* args, UErrorCode* err){

    const UChar *source = args->source;
    const UChar *sourceLimit = args->sourceLimit;
    unsigned char *target = (unsigned char *) args->target;
    unsigned char *targetLimit = (unsigned char *) args->targetLimit;
    int32_t* offsets = args->offsets;
    uint32_t targetByteUnit = 0x0000;
    UChar32 sourceChar = 0x0000;
    UBool isTargetByteDBCS;
    UBool oldIsTargetByteDBCS;
    UConverterDataISO2022 *converterData;
    UConverterCallbackReason reason;
    UConverterSharedData* sharedData;
    UBool useFallback;
    int32_t length =0;

    if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    /* initialize data */
    converterData=(UConverterDataISO2022*)args->converter->extraInfo;
    sharedData = converterData->fromUnicodeConverter->sharedData;
    useFallback = args->converter->useFallback;
    isTargetByteDBCS=(UBool)args->converter->fromUnicodeStatus;
    oldIsTargetByteDBCS = isTargetByteDBCS;
    /* if the version is 1 then the user is requesting 
     * conversion with ibm-25546 pass the arguments to 
     * MBCS converter and return
     */
    if(converterData->version==1){
        UConverter_fromUnicode_ISO_2022_KR_OFFSETS_LOGIC_IBM(args,err);
        return;
    }
    
    isTargetByteDBCS   = (UBool) args->converter->fromUnicodeStatus;
    if(args->converter->fromUSurrogateLead!=0 && target <targetLimit) {
        goto getTrail;
    }
    while(source < sourceLimit){
        
        targetByteUnit = missingCharMarker;

        if(target < (unsigned char*) args->targetLimit){
            sourceChar = *source++;
           /* length= _MBCSFromUChar32(converterData->fromUnicodeConverter->sharedData,
                sourceChar,&targetByteUnit,args->converter->useFallback);*/
            MBCS_FROM_UCHAR32_ISO2022(sharedData,sourceChar,&targetByteUnit,useFallback,(int*)&length,MBCS_OUTPUT_2);
            /* only DBCS or SBCS characters are expected*/
            /* DB haracters with high bit set to 1 are expected */
            if(length > 2 || length==0 ||(((targetByteUnit & 0x8080) != 0x8080)&& length==2)){
                targetByteUnit=missingCharMarker;
            }
            if (targetByteUnit != missingCharMarker){

                oldIsTargetByteDBCS = isTargetByteDBCS;
                isTargetByteDBCS = (UBool)(targetByteUnit>0x00FF);
                  /* append the shift sequence */
                if (oldIsTargetByteDBCS != isTargetByteDBCS ){
                    
                    if (isTargetByteDBCS) 
                        *target++ = UCNV_SO;
                    else 
                        *target++ = UCNV_SI;
                    if(offsets)
                        *(offsets++)=   source - args->source-1;
                }
                /* write the targetUniChar  to target */
                if(targetByteUnit <= 0x00FF){
                    if( target < targetLimit){
                        *(target++) = (unsigned char) targetByteUnit;
                        if(offsets){
                            *(offsets++) = source - args->source-1;
                        }

                    }else{
                        args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (unsigned char) (targetByteUnit);
                        *err = U_BUFFER_OVERFLOW_ERROR;
                    }
                }else{
                    if(target < targetLimit){
                        *(target++) =(unsigned char) ((targetByteUnit>>8) -0x80);
                        if(offsets){
                            *(offsets++) = source - args->source-1;
                        }
                        if(target < targetLimit){
                            *(target++) =(unsigned char) (targetByteUnit -0x80);
                            if(offsets){
                                *(offsets++) = source - args->source-1;
                            }
                        }else{
                            args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (unsigned char) (targetByteUnit -0x80);
                            *err = U_BUFFER_OVERFLOW_ERROR;
                        }
                    }else{
                        args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (unsigned char) ((targetByteUnit>>8) -0x80);
                        args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (unsigned char) (targetByteUnit-0x80);
                        *err = U_BUFFER_OVERFLOW_ERROR;
                    }
                }

            }
            else{
                /* oops.. the code point is unassingned
                 * set the error and reason
                 */
                reason =UCNV_UNASSIGNED;
                *err =U_INVALID_CHAR_FOUND;

                /*check if the char is a First surrogate*/
                if(UTF_IS_SURROGATE(sourceChar)) {
                    if(UTF_IS_SURROGATE_FIRST(sourceChar)) {
                        args->converter->fromUSurrogateLead=(UChar)sourceChar;
getTrail:
                        /*look ahead to find the trail surrogate*/
                        if(source <  sourceLimit) {
                            /* test the following code unit */
                            UChar trail=(UChar) *source;
                            if(UTF_IS_SECOND_SURROGATE(trail)) {
                                source++;
                                sourceChar=UTF16_GET_PAIR_VALUE(args->converter->fromUSurrogateLead, trail);
                                args->converter->fromUSurrogateLead=0x00;
                                *err = U_INVALID_CHAR_FOUND;
                                reason =UCNV_UNASSIGNED;
                                /* convert this surrogate code point */
                                /* exit this condition tree */
                            } else {
                                /* this is an unmatched lead code unit (1st surrogate) */
                                /* callback(illegal) */
                                reason=UCNV_ILLEGAL;
                                *err=U_ILLEGAL_CHAR_FOUND;
                            }
                        } else {
                            /* no more input */
                            *err = U_ZERO_ERROR;
                            break;
                        }
                    } else {
                        /* this is an unmatched trail code unit (2nd surrogate) */
                        /* callback(illegal) */
                        reason=UCNV_ILLEGAL;
                        *err=U_ILLEGAL_CHAR_FOUND;
                    }
                }
                args->converter->fromUnicodeStatus = (int32_t)isTargetByteDBCS;
                /* Call the callback function*/
                fromUnicodeCallback(args,sourceChar,&source,&target,&offsets,reason,err);
                isTargetByteDBCS=(UBool)args->converter->fromUnicodeStatus;

                if (U_FAILURE (*err)){
                    break;
                }
            }
        } /* end if(myTargetIndex<myTargetLength) */
        else{
            *err =U_BUFFER_OVERFLOW_ERROR;
            break;
        }

    }/* end while(mySourceIndex<mySourceLength) */


    /*If at the end of conversion we are still carrying state information
     *flush is TRUE, we can deduce that the input stream is truncated
     */
    if (args->converter->fromUSurrogateLead !=0 && (source == sourceLimit) && args->flush){
        *err = U_TRUNCATED_CHAR_FOUND;
    }
    /* Reset the state of converter if we consumed 
     * the source and flush is true
     */
    if( (source == sourceLimit) && args->flush){
        setInitialStateFromUnicodeKR(args->converter,converterData);
    }

    /*save the state and return */
    args->source = source;
    args->target = (char*)target;
    args->converter->fromUnicodeStatus = (uint32_t)isTargetByteDBCS;
}

/************************ To Unicode ***************************************/

static void 
UConverter_toUnicode_ISO_2022_KR_OFFSETS_LOGIC_IBM(UConverterToUnicodeArgs *args,
                                                            UErrorCode* err){
    const char* mySourceLimit;
    char const* sourceStart;
    UConverter* saveThis;
    int plane =0; /*dummy variable */
    UConverterDataISO2022* myData=(UConverterDataISO2022*)(args->converter->extraInfo);
    do{

        /*Find the end of the buffer e.g : Next Escape Seq | end of Buffer*/
        mySourceLimit = getEndOfBuffer_2022(&(args->source), args->sourceLimit, args->flush);

        if (args->converter->mode == UCNV_SO) /*Already doing some conversion*/{
            saveThis = args->converter;
            args->offsets = NULL;
            args->converter = myData->currentConverter;
            _MBCSToUnicodeWithOffsets(args,err);
            if(U_FAILURE(*err)){
                uprv_memcpy(saveThis->invalidUCharBuffer, args->converter->invalidUCharBuffer, 
                                args->converter->invalidUCharLength);
                saveThis->invalidUCharLength=args->converter->invalidUCharLength;
            }
            args->converter = saveThis;
        }

        /*-Done with buffer with entire buffer
        -Error while converting
        */
        if (U_FAILURE(*err) || (args->source == args->sourceLimit)) 
            return;

        sourceStart = args->source;
        changeState_2022(args->converter,
               &(args->source), 
               args->sourceLimit,
               TRUE,
               ISO_2022_KR,
               &plane,
               err);
        /* args->source = sourceStart; */


    }while(args->source < args->sourceLimit);
    /* return*/
}

static void 
UConverter_toUnicode_ISO_2022_KR_OFFSETS_LOGIC(UConverterToUnicodeArgs *args,
                                                            UErrorCode* err){
    char tempBuf[3];
    const char* pBuf;
    const char *mySource = ( char *) args->source;
    UChar *myTarget = args->target;
    const char *mySourceLimit = args->sourceLimit;
    UChar32 targetUniChar = 0x0000;
    UChar mySourceChar = 0x0000;
    UConverterDataISO2022* myData;
    int plane =0; /*dummy variable */
    UConverterSharedData* sharedData ;
    UBool useFallback;


    if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    /* initialize state */
    myData=(UConverterDataISO2022*)(args->converter->extraInfo);
    sharedData = myData->fromUnicodeConverter->sharedData;
    useFallback = args->converter->useFallback;
    
    if(myData->version==1){
      UConverter_toUnicode_ISO_2022_KR_OFFSETS_LOGIC_IBM(args,err);
      return;
    }
    while(mySource< args->sourceLimit){

        targetUniChar = missingCharMarker;

        if(myTarget < args->targetLimit){

            mySourceChar= (unsigned char) *mySource++;

            if(mySourceChar==UCNV_SI){
                myData->currentType = SBCS;
                /*consume the source */
                continue;
            }else if(mySourceChar==UCNV_SO){
                myData->currentType = DBCS;
                /*consume the source */
                continue;
            }else if(mySourceChar==ESC_2022 || myData->key!=0){
                
                /*
                 * Commented out this part to be lenient and allow for
                 * more escape sequences in ISO-2022-KR byte stream
                 *
                 * Already doing some conversion and found escape Sequence
                 * if(args->converter->mode == UCNV_SO){
                 *   *err = U_ILLEGAL_ESCAPE_SEQUENCE;
                 * }
                 * else{
                 *
                 */

                    mySource--;
                    changeState_2022(args->converter,&(mySource), 
                                    args->sourceLimit, args->flush,ISO_2022_KR,&plane, err);
                /*}*/
                if(U_FAILURE(*err)){
                    args->target = myTarget;
                    args->source = mySource;
                    return;
                }
                continue;
            }   

            if(myData->currentType==DBCS){
                if(args->converter->toUnicodeStatus == 0x00){
                    args->converter->toUnicodeStatus = (UChar) mySourceChar;
                    continue;
                }
                else{
                    tempBuf[0] = (char) (args->converter->toUnicodeStatus+0x80);
                    tempBuf[1] = (char) (mySourceChar+0x80);
                    mySourceChar = (UChar)(mySourceChar + (args->converter->toUnicodeStatus<<8));
                    args->converter->toUnicodeStatus =0x00;
                    pBuf = tempBuf;
                    targetUniChar = _MBCSSimpleGetNextUChar(sharedData,
                        &pBuf,(pBuf+2),useFallback);
                }
            }
            else{
                if(args->converter->fromUnicodeStatus == 0x00){
                    targetUniChar = _MBCS_SINGLE_SIMPLE_GET_NEXT_BMP(sharedData, mySourceChar);

                }

            }
            if(targetUniChar != missingCharMarker){
                if(args->offsets)
                    args->offsets[myTarget - args->target]= mySource - args->source - 1-(myData->currentType==DBCS);
                *(myTarget++)=(UChar)targetUniChar;
            }
            else {
                
                /* Call the callback function*/
                toUnicodeCallback(args,mySourceChar,&mySource,targetUniChar,&myTarget,err);
                if(U_FAILURE(*err)){
                    break;
                }
            }
        }
        else{
            *err =U_BUFFER_OVERFLOW_ERROR;
            break;
        }
    }
    if((args->flush==TRUE)
        && (mySource == mySourceLimit) 
        && ( args->converter->toUnicodeStatus !=0x00)){

        *err = U_TRUNCATED_CHAR_FOUND;
        args->converter->toUnicodeStatus = 0x00;
    }
    /* Reset the state of converter if we consumed 
     * the source and flush is true
     */
    if( (mySource == mySourceLimit) && args->flush){
        setInitialStateToUnicodeKR(args->converter,myData);
    }
    args->target = myTarget;
    args->source = mySource;
}

/*************************** END ISO2022-KR *********************************/

/*************************** ISO-2022-CN *********************************
*
* Rules for ISO-2022-CN Encoding:
* i)   The desinator sequence must appear once on a line before any instance
*      of character set it designates.
* ii)  If two lines contain characters from the same character set, both lines
*      must include the designator sequence.
* iii) Once the designator sequence is know, a shifting sequnce has to be found
*      to invoke the  shifting
* iv)  All lines start in ASCII and end in ASCII.
* v)   Four shifting sequences are employed for this purpose:
*
*      Sequcence   ASCII Eq    Charsets
*      ----------  -------    ---------
*      SS2          <ESC>N      CNS-11643-1992 Planes 3-7
*      SS3          <ESC>O      CNS-11643-1992 Plane 2
*      SI           <SI>        
*      SO           <SO>        CNS-11643-1992 Plane 1, GB2312,ISO-IR-165
*
* vi)
*      SOdesignator  : ESC "$" ")" finalchar_for_SO
*      SS2designator : ESC "$" "*" finalchar_for_SS2
*      SS3designator : ESC "$" "+" finalchar_for_SS3
*
*      ESC $ ) A       Indicates the bytes following SO are Chinese
*       characters as defined in GB 2312-80, until
*       another SOdesignation appears
*
*
*      ESC $ ) E       Indicates the bytes following SO are as defined
*       in ISO-IR-165 (for details, see section 2.1),
*       until another SOdesignation appears
*
*      ESC $ ) G       Indicates the bytes following SO are as defined
*       in CNS 11643-plane-1, until another
*       SOdesignation appears
*
*      ESC $ * H       Indicates the two bytes immediately following
*       SS2 is a Chinese character as defined in CNS
*       11643-plane-2, until another SS2designation
*       appears
*       (Meaning <ESC>N must preceed every 2 byte 
*        sequence.)
*
*      ESC $ + I       Indicates the immediate two bytes following SS3
*       is a Chinese character as defined in CNS
*       11643-plane-3, until another SS3designation
*       appears
*       (Meaning <ESC>O must preceed every 2 byte 
*        sequence.)
*
*      ESC $ + J       Indicates the immediate two bytes following SS3
*       is a Chinese character as defined in CNS
*       11643-plane-4, until another SS3designation
*       appears
*       (In English: <ESC>N must preceed every 2 byte 
*        sequence.)
*
*      ESC $ + K       Indicates the immediate two bytes following SS3
*       is a Chinese character as defined in CNS
*       11643-plane-5, until another SS3designation
*       appears
*
*      ESC $ + L       Indicates the immediate two bytes following SS3
*       is a Chinese character as defined in CNS
*       11643-plane-6, until another SS3designation
*       appears
*
*      ESC $ + M       Indicates the immediate two bytes following SS3
*       is a Chinese character as defined in CNS
*       11643-plane-7, until another SS3designation
*       appears
*
*       As in ISO-2022-CN, each line starts in ASCII, and ends in ASCII, and
*       has its own designation information before any Chinese characters
*       appear
*
*/

/* The following are defined this way to make the strings truely readonly */
static const char EMPTY_STR[] = "";
static const char SHIFT_IN_STR[]  = "\x0F";
static const char SHIFT_OUT_STR[] = "\x0E";
static const char GB_2312_80_STR[] = "\x1B\x24\x29\x41";
static const char ISO_IR_165_STR[] = "\x1B\x24\x29\x45";
static const char CNS_11643_1992_Plane_1_STR[] = "\x1B\x24\x29\x47";
static const char CNS_11643_1992_Plane_2_STR[] = "\x1B\x24\x2A\x48";
static const char CNS_11643_1992_Plane_3_STR[] = "\x1B\x24\x2B\x49";
static const char CNS_11643_1992_Plane_4_STR[] = "\x1B\x24\x2B\x4A";
static const char CNS_11643_1992_Plane_5_STR[] = "\x1B\x24\x2B\x4B";
static const char CNS_11643_1992_Plane_6_STR[] = "\x1B\x24\x2B\x4C";
static const char CNS_11643_1992_Plane_7_STR[] = "\x1B\x24\x2B\x4D";

/********************** ISO2022-CN Data **************************/
static const char* const escSeqCharsCN[10] ={
        SHIFT_IN_STR,           /* ASCII */
        GB_2312_80_STR,
        ISO_IR_165_STR,
        CNS_11643_1992_Plane_1_STR,
        CNS_11643_1992_Plane_2_STR,
        CNS_11643_1992_Plane_3_STR,
        CNS_11643_1992_Plane_4_STR,
        CNS_11643_1992_Plane_5_STR,
        CNS_11643_1992_Plane_6_STR,
        CNS_11643_1992_Plane_7_STR
};
static const int escSeqCharsLenCN[10] = {
    1,      /* length of escSeq for ASCII */
    4,      /* length of escSeq for GB 2312-80 */
    4,      /* length of escSeq for ISO-IR-165 */
    4,      /* length of escSeq for CNS 11643-1992 Plane 1 */
    4,      /* length of escSeq for CNS 11643-1992 Plane 2 */
    4,      /* length of escSeq for CNS 11643-1992 Plane 3 */
    4,      /* length of escSeq for CNS 11643-1992 Plane 4 */
    4,      /* length of escSeq for CNS 11643-1992 Plane 5 */
    4,      /* length of escSeq for CNS 11643-1992 Plane 6 */
    4       /* length of escSeq for CNS 11643-1992 Plane 7 */
};
static const char* const shiftSeqCharsCN[10] ={
        EMPTY_STR,      /* ASCII */
        SHIFT_OUT_STR,  /* GB 2312-80 */
        SHIFT_OUT_STR,  /* ISO-IR-165 */
        SHIFT_OUT_STR,  /* CNS 11643-1992 Plane 1 */
        UCNV_SS2,       /* CNS 11643-1992 Plane 2 */
        UCNV_SS3,       /* CNS 11643-1992 Plane 3 */
        UCNV_SS3,       /* CNS 11643-1992 Plane 4 */
        UCNV_SS3,       /* CNS 11643-1992 Plane 5 */
        UCNV_SS3,       /* CNS 11643-1992 Plane 6 */
        UCNV_SS3        /* CNS 11643-1992 Plane 7 */
};
static const int shiftSeqCharsLenCN[10] ={
    0,      /* length of shiftSeq for ASCII */
    1,      /* length of shiftSeq for GB 2312-80 */
    1,      /* length of shiftSeq for ISO-IR-165 */
    1,      /* length of shiftSeq for CNS 11643-1992 Plane 1 */
    2,      /* length of shiftSeq for CNS 11643-1992 Plane 2 */
    2,      /* length of shiftSeq for CNS 11643-1992 Plane 3 */
    2,      /* length of shiftSeq for CNS 11643-1992 Plane 4 */
    2,      /* length of shiftSeq for CNS 11643-1992 Plane 5 */
    2,      /* length of shiftSeq for CNS 11643-1992 Plane 6 */
    2       /* length of shiftSeq for CNS 11643-1992 Plane 7 */
};

typedef enum  {
        ASCII_1=0,
        GB2312_1=1,
        ISO_IR_165=2,
        CNS_11643=3,
        INVALID_STATE_CN=-1
} StateEnumCN;

static const Cnv2022Type myConverterTypeCN[4]={
        ASCII1,
        DBCS,
        DBCS,
        MBCS
};


static void 
UConverter_fromUnicode_ISO_2022_CN_OFFSETS_LOGIC(UConverterFromUnicodeArgs* args, UErrorCode* err){

    UConverterDataISO2022 *converterData;
    unsigned char* target = (unsigned char*) args->target;
    const unsigned char* targetLimit = (const unsigned char*) args->targetLimit;
    const UChar* source = args->source;
    const UChar* sourceLimit = args->sourceLimit;
    int32_t* offsets = args->offsets;
    uint32_t targetByteUnit = missingCharMarker;
    uint32_t sourceChar  =0x0000;
    const char* escSeq = NULL;
    int len =0; /*length of escSeq chars*/
    uint32_t targetValue=0;
    uint8_t planeVal=0;
    UConverterCallbackReason reason;
    UConverterSharedData* sharedData=NULL;
    UBool useFallback;

    /* state variables*/
    StateEnumCN* currentState;     
    StateEnumCN initIterState;     
    UConverter** currentConverter; 
    UBool* isShiftAppended;        
    UBool* isEscapeAppended;       
    int*  plane;                   
    int   lPlane=0;                  

    /* arguments check*/
    if ((args->converter == NULL) || (targetLimit < target) || (sourceLimit < source)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    /* set up the state */
    converterData     = (UConverterDataISO2022*)args->converter->extraInfo;
    useFallback       = args->converter->useFallback;
    currentState      = (StateEnumCN*)&converterData->fromUnicodeCurrentState;
    initIterState     = ASCII_1;
    currentConverter  = &converterData->fromUnicodeConverter;
    isShiftAppended   = &converterData->isShiftAppended;
    isEscapeAppended  = &converterData->isEscapeAppended;
    plane             = &converterData->plane;
    initIterState     = *currentState;
    *currentConverter = converterData->myConverterArray[(*currentConverter==NULL) ? 0 : (int)*currentState];
    sharedData        = (*currentConverter)->sharedData;

    /* check if the last codepoint of previous buffer was a lead surrogate*/
    if(args->converter->fromUSurrogateLead!=0 && target< targetLimit) {
        goto getTrail;
    }


    while( source < sourceLimit){

        targetByteUnit =missingCharMarker;
        lPlane =0;

        if(target < targetLimit){

            sourceChar  = *(source++);
            /*check if the char is a First surrogate*/
             if(UTF_IS_SURROGATE(sourceChar)) {
                if(UTF_IS_SURROGATE_FIRST(sourceChar)) {
                    args->converter->fromUSurrogateLead=(UChar)sourceChar;
getTrail:
                    /*look ahead to find the trail surrogate*/
                    if(source < sourceLimit) {
                        /* test the following code unit */
                        UChar trail=(UChar) *source;
                        if(UTF_IS_SECOND_SURROGATE(trail)) {
                            source++;
                            /*(((args->converter->fromUSurrogateLead)<<10L)+(trail)-((0xd800<<10L)+0xdc00-0x10000))*/
                            sourceChar=UTF16_GET_PAIR_VALUE(args->converter->fromUSurrogateLead, trail);
                            args->converter->fromUSurrogateLead=0x00;
                            /* convert this surrogate code point */
                            /* exit this condition tree */
                        } else {
                            /* this is an unmatched lead code unit (1st surrogate) */
                            /* callback(illegal) */
                            reason=UCNV_ILLEGAL;
                            *err=U_ILLEGAL_CHAR_FOUND;
                            goto callback;
                        }
                    } else {
                        /* no more input */
                        break;
                    }
                } else {
                    /* this is an unmatched trail code unit (2nd surrogate) */
                    /* callback(illegal) */
                    reason=UCNV_ILLEGAL;
                    *err=U_ILLEGAL_CHAR_FOUND;
                    goto callback;
                }
            }

            /* do the conversion */
            if(sourceChar < 0x007f ){
                targetByteUnit = sourceChar;
                if(*currentState!= ASCII_1){
                    *currentState = ASCII_1;
                    *isEscapeAppended = FALSE;
                }

            }
            else{

                do{
                    if(myConverterTypeCN[*currentState] == MBCS){
                        /*len= _MBCSFromUChar32((*currentConverter)->sharedData,sourceChar,
                                                    &targetValue,args->converter->useFallback);*/
                        MBCS_FROM_UCHAR32_ISO2022(sharedData,sourceChar,&targetValue,useFallback,&len,MBCS_OUTPUT_3);
                         if(len==3){
                            targetByteUnit = (UChar32) targetValue;
                            planeVal = (uint8_t) ((targetValue)>>16);
                            if(planeVal >0x80 && planeVal<0x89){
                                lPlane = (int)(planeVal - 0x80);
                                targetByteUnit -= (planeVal<<16);
                            }else {
                                lPlane =-1;
                                targetByteUnit=missingCharMarker;
                            }
                            if(converterData->version == 0 && lPlane >2){
                                targetByteUnit = missingCharMarker;
                            }
                        }
                    }else if(myConverterTypeCN[*currentState] == DBCS){
                        MBCS_FROM_UCHAR32_ISO2022(sharedData,sourceChar,&targetValue,useFallback,&len,MBCS_OUTPUT_2);
                        if(len==2){    
                            if(( converterData->version) == 0 && *currentState ==ISO_IR_165){
                                targetByteUnit = missingCharMarker;
                            }else{
                                targetByteUnit = (UChar32) targetValue;
                            }
                        }
                     
                    }else{
                        if(sourceChar < 0x7f){
                             targetByteUnit = sourceChar;
                        }
                    }
                    if(targetByteUnit==missingCharMarker){

                        *currentState=(StateEnumCN)((*currentState<3)? *currentState+1:0);
                        *currentConverter =converterData->myConverterArray[(*currentConverter==NULL) ? 0 : (int)*currentState];
                        targetByteUnit =missingCharMarker;
                        *isEscapeAppended = FALSE;
                        *isShiftAppended = FALSE;
                        sharedData=(*currentConverter)->sharedData;
                    }
                    else
                        break;
                }while(initIterState != *currentState);

            }
            if(targetByteUnit != missingCharMarker){

                args->converter->fromUnicodeStatus=(UBool) (*currentState > ASCII_1);
                /* Append the escpace sequence */
                if(!*isEscapeAppended ||(*plane != lPlane)){
                    int temp =0;
                    temp =(*currentState==CNS_11643) ? ((int)*currentState+lPlane-1):(int)*currentState ;
                    escSeq = escSeqCharsCN[temp];
                    len =escSeqCharsLenCN[temp];
                    CONCAT_ESCAPE_EX(args,source, &target, targetLimit, &offsets, escSeq,len,err);
                    *plane=lPlane;
                    *isEscapeAppended=TRUE;
                    *isShiftAppended=FALSE;
                }

                /* Append Shift Sequences */
                if(*currentState == GB2312_1 || *currentState==ISO_IR_165){
                    if(!*isShiftAppended){
                        len =shiftSeqCharsLenCN[*currentState];
                        escSeq = shiftSeqCharsCN[*currentState];
                        CONCAT_ESCAPE_EX(args,source, &target, targetLimit, &offsets, escSeq,len,err);
                        *isShiftAppended=TRUE;
                    }
                }else if(*currentState!=ASCII1){
                    int temp =*currentState+*plane-1;
                    if(*plane ==1 && *isShiftAppended){
                        temp=0;
                    }
                    len =shiftSeqCharsLenCN[temp];
                    escSeq = shiftSeqCharsCN[temp];
                    CONCAT_ESCAPE_EX(args,source, &target, targetLimit, &offsets, escSeq,len,err);
                    *isShiftAppended=TRUE;
                }

                initIterState = *currentState;

                /* write the targetByteUnit  to target */
                if(targetByteUnit <= 0x00FF){
                    if( target <targetLimit){
                        *(target++) = (unsigned char) targetByteUnit;
                        if(offsets){
                            *(offsets++) =source-args->source-1;
                        }

                    }else{
                        args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (unsigned char) targetByteUnit;
                        *err = U_BUFFER_OVERFLOW_ERROR;
                    }
                }else{
                    if(target < targetLimit){
                        *(target++) =(unsigned char) (targetByteUnit>>8);
                        if(offsets){
                            *(offsets++) = source-args->source-1;
                        }
                        if(target < targetLimit){
                            *(target++) =(unsigned char) (targetByteUnit);
                            if(offsets){
                                *(offsets++) = source-args->source-1;
                            }
                        }else{
                            args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (unsigned char) (targetByteUnit);
                            *err = U_BUFFER_OVERFLOW_ERROR;
                        }
                    }else{
                        args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (unsigned char) (targetByteUnit>>8);
                        args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (unsigned char) (targetByteUnit);
                        *err = U_BUFFER_OVERFLOW_ERROR;
                    }
                }

            }
            else{

                /* if we cannot find the character after checking all codepages 
                 * then this is an error
                 */
                reason = UCNV_UNASSIGNED;
                *err = U_INVALID_CHAR_FOUND;
callback:

                fromUnicodeCallback(args,sourceChar,&source,&target,&offsets,reason,err);
                initIterState = *currentState;
               
                if (U_FAILURE (*err)){
                    break;
                }
            }
        } /* end if(myTargetIndex<myTargetLength) */
        else{
            *err =U_BUFFER_OVERFLOW_ERROR;
            break;
        }

    }/* end while(mySourceIndex<mySourceLength) */


    /*If at the end of conversion we are still carrying state information
     *flush is TRUE, we can deduce that the input stream is truncated
     */
    if (args->converter->fromUSurrogateLead !=0 && (source == sourceLimit) && args->flush){
        *err = U_TRUNCATED_CHAR_FOUND;
    }
    /* Reset the state of converter if we consumed 
     * the source and flush is true
     */
    if( (source == sourceLimit) && args->flush){
        setInitialStateFromUnicodeJPCN(args->converter,converterData);
    }

    /*save the state and return */
    args->source = source;
    args->target = (char*)target;
}

/*************** to unicode *******************/
static const StateEnumCN nextStateToUnicodeCN[2][MAX_STATES_2022]= {
    {
/*      0                1               2               3               4               5               6               7               8               9    */
     INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN
    ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN
    ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN
    ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN
    ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,GB2312_1          ,INVALID_STATE_CN  ,INVALID_STATE_CN
    ,CNS_11643         ,CNS_11643         ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN
    ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN
    ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN
    },
    {
/*      0                1               2               3               4               5               6               7               8               9    */
     INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN
    ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN
    ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN
    ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN
    ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,GB2312_1          ,INVALID_STATE_CN  ,ISO_IR_165
    ,CNS_11643         ,CNS_11643         ,CNS_11643         ,CNS_11643         ,CNS_11643          ,CNS_11643        ,CNS_11643         ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN
    ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN
    ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN  ,INVALID_STATE_CN
    }
};

static void 
changeState_2022(UConverter* _this,
                const char** source, 
                const char* sourceLimit,
                UBool flush,Variant2022 var,
                int* plane,
                UErrorCode* err){
    UConverter* myUConverter;
    UCNV_TableStates_2022 value;
    UConverterDataISO2022* myData2022 = ((UConverterDataISO2022*)_this->extraInfo);
    uint32_t key = myData2022->key;
    const char* chosenConverterName = NULL;
    int32_t offset;

    /*In case we were in the process of consuming an escape sequence
    we need to reprocess it */

    do{

        value = getKey_2022(**source,(int32_t *) &key, &offset);
        
        switch (value){

        case VALID_NON_TERMINAL_2022 : 
            break;

        case VALID_TERMINAL_2022:
            {
                (*source)++;
                chosenConverterName = escSeqStateTable_Result_2022[offset];
                key = 0;
                goto DONE;
            };
            break;

        case INVALID_2022:
            {
                myData2022->key = 0;
                *err = U_ILLEGAL_ESCAPE_SEQUENCE;
                return;
            }
        case VALID_SS2_SEQUENCE:
            /*falls through*/

        case VALID_SS3_SEQUENCE:
            {
                (*source)++;
                key = 0;
                goto DONE;
            }

        case VALID_MAYBE_TERMINAL_2022:
            {
                const char* mySource = (*source+1);
                int32_t myKey = key;
                UCNV_TableStates_2022 myValue = value;
                int32_t myOffset=0;
                if(*mySource==ESC_2022){
                    while ((mySource < sourceLimit) && 
                        ((myValue == VALID_MAYBE_TERMINAL_2022)||(myValue == VALID_NON_TERMINAL_2022))){
                        myValue = getKey_2022(*(mySource++), &myKey, &myOffset);
                    }
                }
                else{
                    (*source)++;
                    myValue=(UCNV_TableStates_2022) 1;
                    myOffset = 8;
                }

                switch (myValue){
                case INVALID_2022:
                    {
                        /*Backs off*/
                        chosenConverterName = escSeqStateTable_Result_2022[offset];
                        value = VALID_TERMINAL_2022;
                        goto DONE;
                    };
                    break;

                case VALID_TERMINAL_2022:
                    {
                        /*uses longer escape sequence*/
                        chosenConverterName = escSeqStateTable_Result_2022[myOffset];
                        key = 0;
                        value = VALID_TERMINAL_2022;
                        goto DONE;
                    };
                    break;

                /* Not expected. Added to make the gcc happy */
                case VALID_SS2_SEQUENCE:
                    /*falls through*/
                /* Not expected. Added to make the gcc happy */
                case VALID_SS3_SEQUENCE:
                    {
                        (*source)++;
                        key = 0;
                        goto DONE;
                    }

                case VALID_NON_TERMINAL_2022: 
                    /*falls through*/
                case VALID_MAYBE_TERMINAL_2022:
                    {
                        if (flush){
                            /*Backs off*/
                            chosenConverterName = escSeqStateTable_Result_2022[offset];
                            value = VALID_TERMINAL_2022;
                            key = 0;
                            goto DONE;
                        }
                        else{
                            key = myKey;
                            value = VALID_NON_TERMINAL_2022;
                        }
                    };
                    break;
                };
                break;
            };
            break;
        }
    }while (++(*source) < sourceLimit);

DONE:
    myData2022->key = key;
    if(offset<57 && offset>49){
        *plane = offset-49;
    }

    if ((value == VALID_NON_TERMINAL_2022) || (value == VALID_MAYBE_TERMINAL_2022)) {
        return;
    }
    else if (value != INVALID_2022 ) {
        if(value==3 || value==4 ){
            _this->mode = UCNV_SI;
            myUConverter =myData2022->currentConverter;
        }
        else{
            switch(var){
            case ISO_2022:
                _this->mode = UCNV_SI;
                ucnv_close(myData2022->currentConverter);
                myData2022->currentConverter = myUConverter = ucnv_open(chosenConverterName, err);
                break;
            case ISO_2022_JP:
                {
                     StateEnum tempState=nextStateToUnicodeJP[myData2022->version][offset];
                    _this->mode = UCNV_SI;
                    myData2022->currentConverter = myUConverter = 
                        (tempState!=INVALID_STATE)? myData2022->myConverterArray[tempState]:NULL;
                    myData2022->toUnicodeCurrentState = tempState;
                    *err= (tempState==INVALID_STATE)?U_ILLEGAL_ESCAPE_SEQUENCE :U_ZERO_ERROR;
                }
                break;
            case ISO_2022_CN:
                {
                     StateEnumCN tempState=nextStateToUnicodeCN[myData2022->version][offset];
                    _this->mode = UCNV_SI;
                    myData2022->currentConverter = myUConverter = 
                        (tempState!=INVALID_STATE)? myData2022->myConverterArray[tempState]:NULL;
                    myData2022->toUnicodeCurrentState =(StateEnum) tempState;
                    *err= (tempState==INVALID_STATE)?U_ILLEGAL_ESCAPE_SEQUENCE :U_ZERO_ERROR;
                }
                break;
            case ISO_2022_KR:
                if(offset==0x30){
                    _this->mode = UCNV_SI;
                    myUConverter = myData2022->currentConverter=myData2022->fromUnicodeConverter;
                    break;
                }

            default:
                myUConverter=NULL;
                *err = U_ILLEGAL_ESCAPE_SEQUENCE;
            }
        }
        if (U_SUCCESS(*err)){
            /*Customize the converter with the attributes set on the 2022 converter*/
            myUConverter->fromUCharErrorBehaviour = _this->fromUCharErrorBehaviour;
            myUConverter->fromUContext = _this->fromUContext;
            myUConverter->fromCharErrorBehaviour = _this->fromCharErrorBehaviour;
            myUConverter->toUContext = _this->toUContext;

            uprv_memcpy(myUConverter->subChar, 
                _this->subChar,
                myUConverter->subCharLen = _this->subCharLen);
            myUConverter->subChar1 = 0;

            _this->mode = UCNV_SO;
        }
    }
}


static void 
UConverter_toUnicode_ISO_2022_CN_OFFSETS_LOGIC(UConverterToUnicodeArgs *args,
                                               UErrorCode* err){
    char tempBuf[3];
    int plane=0;
    const char* pBuf;
    const char *mySource = ( char *) args->source;
    UChar *myTarget = args->target;
    char *tempLimit = &tempBuf[3];
    const char *mySourceLimit = args->sourceLimit;
    uint32_t targetUniChar = 0x0000;
    uint32_t mySourceChar = 0x0000;
    UConverterDataISO2022* myData;

    if ((args->converter == NULL) || (args->targetLimit < myTarget) || (args->sourceLimit < mySource)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
   
   myData=(UConverterDataISO2022*)(args->converter->extraInfo);
   plane=myData->plane;
   while(mySource< args->sourceLimit){

        targetUniChar =missingCharMarker;

        if(myTarget < args->targetLimit){

            mySourceChar= (unsigned char) *mySource++;

            
            switch(mySourceChar){
            case UCNV_SI:
                if(args->converter->toUnicodeStatus != 0x00){
                    break;
                }
                myData->currentType = ASCII1;
                myData->plane=plane = 0;
                continue;

            case UCNV_SO:
                if(args->converter->toUnicodeStatus != 0x00){
                    break;
                }

                myData->currentType = MBCS;
                continue;

            case CR:
                /*falls through*/
            case LF:
                if(args->converter->toUnicodeStatus != 0x00){
                    break;
                }
                myData->currentType = ASCII1;
                myData->plane=plane = 0;
                /* falls through */
            default:
                /* if we are in the middle of consuming an escape sequence 
                 * we fall through else we process the input
                 */
                if(myData->key==0){
                     if(myData->currentType != ASCII1){
                        if(args->converter->toUnicodeStatus == 0x00){
                            args->converter->toUnicodeStatus = (UChar) mySourceChar;
                            continue;
                        }
                        else{
                            if(plane >0){
                                tempBuf[0] = (char) (0x80+plane);
                                tempBuf[1] = (char) (args->converter->toUnicodeStatus);
                                tempBuf[2] = (char) (mySourceChar);
                                tempLimit  = &tempBuf[2]+1;

                            }else{
                                tempBuf[0] = (char) args->converter->toUnicodeStatus;
                                tempBuf[1] = (char) mySourceChar;
                                tempLimit  = &tempBuf[2];
                            }
                            mySourceChar+= (uint32_t) args->converter->toUnicodeStatus<<8;
                            args->converter->toUnicodeStatus = 0;
                            pBuf = tempBuf;
                            if(myData->currentConverter!=NULL){
                                targetUniChar = _MBCSSimpleGetNextUChar(myData->currentConverter->sharedData, &pBuf, tempLimit, FALSE);
                            }else{
                                *err=U_INVALID_CHAR_FOUND;
                                break;
                            }
                        }
                    }
                    else{
                        if(args->converter->toUnicodeStatus == 0x00 && mySourceChar < 0x7f){
                            targetUniChar = (UChar) mySourceChar;
                        }
                    }
                    break;
                }
            case ESC_2022:
                if(args->converter->toUnicodeStatus != 0x00){
                   break;
                }
                mySource--;
                changeState_2022(args->converter,&(mySource), 
                    args->sourceLimit, args->flush,ISO_2022_CN,&plane,err);

                myData->plane=plane;
                if(plane>0){
                    myData->currentType = MBCS;
                }else{
                    myData->currentType=DBCS;
                }

                /* invalid or illegal escape sequence */
                if(U_FAILURE(*err)){
                    args->target = myTarget;
                    args->source = mySource;
                    return;
                }
                continue;

            }
            if(targetUniChar < (missingCharMarker-1/*0xfffe*/)){
                if(args->offsets){
                   args->offsets[myTarget - args->target]= mySource - args->source - 2 
                                                            +(myData->currentType==ASCII);
                }
                *(myTarget++)=(UChar)targetUniChar;
            }
            else if(targetUniChar > missingCharMarker){
                /* disassemble the surrogate pair and write to output*/
                targetUniChar-=0x0010000;
                *(myTarget++) = (UChar)(0xd800+(UChar)(targetUniChar>>10));
                if(args->offsets){
                    args->offsets[myTarget - args->target]= mySource - args->source - 2 
                                                            +(myData->currentType==ASCII);
                }
                if(myTarget< args->targetLimit){ 
                    *(myTarget)++ = (UChar)(0xdc00+(UChar)(targetUniChar&0x3ff));
                    if(args->offsets){
                        args->offsets[myTarget - args->target]= mySource - args->source - 2 
                                                            +(myData->currentType==ASCII);
                    }
                }else{
                    args->converter->UCharErrorBuffer[args->converter->UCharErrorBufferLength++]=
                                    (UChar)(0xdc00+(UChar)(targetUniChar&0x3ff));
                }

            }
            else{
                /* Call the callback function*/
                toUnicodeCallback(args,mySourceChar,&mySource,targetUniChar,&myTarget,err);
                /*args->offsets = saveOffsets;*/
                if(U_FAILURE(*err))
                    break;

            }
        }
        else{
            *err =U_BUFFER_OVERFLOW_ERROR;
            break;
        }
    }
    if((args->flush==TRUE)
        && (mySource == mySourceLimit) 
        && ( args->converter->toUnicodeStatus !=0x00)){

        *err = U_TRUNCATED_CHAR_FOUND;
        args->converter->toUnicodeStatus = 0x00;
    }
    /* Reset the state of converter if we consumed 
     * the source and flush is true
     */
    if( (mySource == mySourceLimit) && args->flush){
        setInitialStateToUnicodeJPCN(args->converter,myData);
    }
    args->target = myTarget;
    args->source = mySource;
}

static void
_ISO_2022_WriteSub(UConverterFromUnicodeArgs *args, int32_t offsetIndex, UErrorCode *err) {
    UConverter *cnv = args->converter;
    UConverterDataISO2022 *myConverterData=(UConverterDataISO2022 *) cnv->extraInfo;
    char *p;
    char buffer[4];

    p = buffer;
    switch(myConverterData->locale[0]){
    case 'j':
           if(myConverterData->fromUnicodeCurrentState!= ASCII){
                myConverterData->fromUnicodeCurrentState= ASCII;
                myConverterData->currentType = (Cnv2022Type) myConverterType[myConverterData->fromUnicodeCurrentState];
                *p++ = '\x1b';
                *p++ = '\x28';
                *p++ = '\x42';

            }
            *p++ = cnv->subChar[0];
            break;
    case 'c':
        if(args->converter->fromUnicodeStatus) {
                /* DBCS mode and SBCS sub char: change to SBCS */
                myConverterData->fromUnicodeCurrentState=ASCII;
                *p++ = UCNV_SI;
            }
            *p++ = cnv->subChar[0];
        break;
    case 'k':
        if(args->converter->fromUnicodeStatus){
            args->converter->fromUnicodeStatus=0x00;
            *p++= UCNV_SI;
        }

        *p++ =  cnv->subChar[0];

    default:
        /* not expected */
        break;
    }
    ucnv_cbFromUWriteBytes(args,
                           buffer, (int32_t)(p - buffer),
                           offsetIndex, err);
}

/* structure for SafeClone calculations */
struct cloneStruct
{
    UConverter cnv;
    UConverterDataISO2022 mydata;
    UConverter currentCnv; /**< for ISO_2022 converter if the current converter is open */

    UConverter clonedConverters[1]; /* Actually a variable sized array for all of the sub converters to be cloned. */
};


static UConverter * 
_ISO_2022_SafeClone(
            const UConverter *cnv, 
            void *stackBuffer, 
            int32_t *pBufferSize, 
            UErrorCode *status)
{
    struct cloneStruct * localClone;
    int32_t bufferSizeNeeded = sizeof(struct cloneStruct);
    UConverterDataISO2022* cnvData = (UConverterDataISO2022*)cnv->extraInfo;
    int32_t i;
    int32_t sizes[UCNV_2022_MAX_CONVERTERS];
    int32_t numConverters = 0;
    int32_t currentConverterIndex = -1;
    int32_t fromUnicodeConverterIndex = -1;
    int32_t currentConverterSize = 0;
    char *ptr; /* buffer pointer */

    if (U_FAILURE(*status)) {
        return 0;
    }

    for(i=0;(i<UCNV_2022_MAX_CONVERTERS)&&cnvData->myConverterArray[i];i++) {
        int32_t size;

        size = 0;
        ucnv_safeClone(cnvData->myConverterArray[i], NULL, &size, status);
        bufferSizeNeeded += size;
        sizes[i] = size;
        numConverters++;

        if(cnvData->currentConverter == cnvData->myConverterArray[i]) {
            currentConverterIndex = i;
        }

        if(cnvData->fromUnicodeConverter == cnvData->myConverterArray[i]) {
            fromUnicodeConverterIndex = i;
        }
    }

    if(currentConverterIndex == -1) {  /* -1 means - not found in array. Clone separately */
        currentConverterSize = 0;
        if(cnvData->currentConverter) { 
            ucnv_safeClone(cnvData->currentConverter, NULL, &currentConverterSize, status);
            bufferSizeNeeded += currentConverterSize;
        }
    }

    for(;i<UCNV_2022_MAX_CONVERTERS;i++) { /* zero the other sizes */
        sizes[i]=0;
    }

    if (*pBufferSize == 0) { /* 'preflighting' request - set needed size into *pBufferSize */
        *pBufferSize = bufferSizeNeeded;
        return 0;
    }

    if(*pBufferSize < bufferSizeNeeded) {
        *status = U_BUFFER_OVERFLOW_ERROR;
        return 0;
    }

    localClone = (struct cloneStruct *)stackBuffer;
    uprv_memcpy(&localClone->cnv, cnv, sizeof(UConverter));

    uprv_memcpy(&localClone->mydata, cnv->extraInfo, sizeof(UConverterDataISO2022));

    /* clone back sub cnvs */

    ptr = (char*)&localClone->clonedConverters;
    for(i=0;i<numConverters;i++) {
        int32_t size;
        size = sizes[i];
        localClone->mydata.myConverterArray[i] = ucnv_safeClone(cnvData->myConverterArray[i], (UConverter*)ptr, &size, status);
        ptr += size;
    }
    for(;i<UCNV_2022_MAX_CONVERTERS;i++) {
        localClone->mydata.myConverterArray[i] = NULL;
    }

    if(currentConverterIndex == -1) { /* -1 = not found in list */
        /* KR version 1 also uses the state in currentConverter for preserving state
         * so we need to clone it too!
         */
        if(cnvData->currentConverter) { 
            localClone->mydata.currentConverter = ucnv_safeClone(cnvData->currentConverter, ptr, &currentConverterSize, status);
            ptr += currentConverterSize;
        } else {
            localClone->mydata.currentConverter = NULL;
        }
    } else {
        localClone->mydata.currentConverter = localClone->mydata.myConverterArray[currentConverterIndex];
    }

    if(fromUnicodeConverterIndex != -1) {
        /* fromUnicodeConverter is in the list */
        localClone->mydata.fromUnicodeConverter = localClone->mydata.myConverterArray[fromUnicodeConverterIndex];
    } else if(cnvData->currentConverter == cnvData->fromUnicodeConverter) {
        /* fromUnicodeConverter is the same as currentConverter */
        localClone->mydata.fromUnicodeConverter = localClone->mydata.currentConverter;
    } else {
        /* fromUnicodeConverter is NULL */
        localClone->mydata.fromUnicodeConverter = NULL;
    }

    localClone->cnv.extraInfo = &localClone->mydata; /* set pointer to extra data */

    return &localClone->cnv;
}

static void
_ISO_2022_GetUnicodeSet(const UConverter *cnv,
                    USet *set,
                    UConverterUnicodeSet which,
                    UErrorCode *pErrorCode)
{
    int32_t i;
    USet *cnvSet;
    UConverterDataISO2022* cnvData;

    if (U_FAILURE(*pErrorCode)) {
        return;
    }
    if (cnv->sharedData == &_ISO2022Data) {
        /* We use UTF-8 in this case */
        uset_addRange(set, 0, 0xd7FF);
        uset_addRange(set, 0xE000, 0x10FFFF);
        return;
    }

    cnvData = (UConverterDataISO2022*)cnv->extraInfo;
    if (cnv->sharedData == &_ISO2022KRData && cnvData->currentConverter != NULL) {
        ucnv_getUnicodeSet(cnvData->currentConverter, set, which, pErrorCode);
        return;
    }

    cnvSet = uset_open(0, 0);
    if (!cnvSet) {
        *pErrorCode =U_MEMORY_ALLOCATION_ERROR;
        return;
    }

    for (i=0; (i<UCNV_2022_MAX_CONVERTERS) && cnvData->myConverterArray[i]; i++) {
        ucnv_getUnicodeSet(cnvData->myConverterArray[i], cnvSet, which, pErrorCode);
        uset_addAll(set, cnvSet /* pErrorCode */);
    }
    uset_close(cnvSet);
}

#endif /* #if !UCONFIG_NO_LEGACY_CONVERSION */
