/*  
**********************************************************************
*   Copyright (C) 2000, International Business Machines
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
*   06/29/2000  helena	    Major rewrite of the callback APIs.
*   08/08/2000  Ram	    Included support for ISO-2022-JP-2
*		    Changed	implementation of toUnicode
*		    function
*   08/21/2000  Ram	    Added support for ISO-2022-KR
*   08/29/2000  Ram	    Seperated implementation of EBCDIC to 
*		    ucnvebdc.c
*   09/20/2000  Ram	    Added support for ISO-2022-CN
*		    Added implementations for getNextUChar()
*		    for specific 2022 country variants.
*   10/31/2000  Ram     Implemented offsets logic functions 
*/

#include "unicode/utypes.h"
#include "cmemory.h"
#include "ucmp16.h"
#include "ucmp8.h"
#include "unicode/ucnv_err.h"
#include "ucnv_bld.h"
#include "unicode/ucnv.h"
#include "ucnv_cnv.h"
#include "unicode/ustring.h"
#include "cstring.h"

#define TEST_ERROR_CONDITION(args,myTargetIndex, mySourceIndex, isTargetUCharDBCS, myConverterData, err){ \
    if(*err ==U_BUFFER_OVERFLOW_ERROR){ \
    /*save the state and return */ \
    args->target += myTargetIndex; \
    args->source += mySourceIndex; \
    myConverterData->sourceIndex = 0; \
    myConverterData->targetIndex = 0; \
    args->converter->fromUnicodeStatus = isTargetUCharDBCS; \
    return; \
    } \
}

#define TEST_ERROR_CONDITION_CN(args,myTargetIndex, mySourceIndex, myConverterData, err){ \
    if(*err ==U_BUFFER_OVERFLOW_ERROR){ \
    /*save the state and return */ \
    args->target += myTargetIndex; \
    args->source += mySourceIndex; \
    myConverterData->sourceIndex = 0; \
    myConverterData->targetIndex = 0; \
    return; \
    } \
}
#define UCNV_SS2 "\x1B\x4E"
#define UCNV_SS3 "\x1B\x4F"

#define ESC 0x0B

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
        INVALID_STATE
        
} StateEnum;



typedef enum {
        ASCII1=0,
        LATIN1,
        SBCS,
        DBCS,
        MBCS
        
}Cnv2022Type;


typedef struct{
    UConverter *currentConverter;
    UConverter *fromUnicodeConverter;
    UBool isFirstBuffer;
    StateEnum toUnicodeCurrentState;
    StateEnum fromUnicodeCurrentState;
    int plane;
    uint8_t escSeq2022[10];
    UConverter* myConverterArray[9];
    int32_t targetIndex;
    int32_t sourceIndex;
    UBool isEscapeAppended;
    UBool isShiftAppended;
    UBool isLocaleSpecified;
    uint32_t key;
    uint32_t version;
}UConverterDataISO2022;

/* ISO-2022 ----------------------------------------------------------------- */

/*Forward declaration */
U_CFUNC void T_UConverter_fromUnicode_UTF8 (UConverterFromUnicodeArgs * args,
                                            UErrorCode * err);

U_CFUNC void T_UConverter_fromUnicode_UTF8_OFFSETS_LOGIC (UConverterFromUnicodeArgs * args,
                                                          UErrorCode * err);


/* Protos */
/***************** ISO-2022 ********************************/
U_CFUNC void T_UConverter_toUnicode_ISO_2022(UConverterToUnicodeArgs * args,
                                             UErrorCode * err);

U_CFUNC void T_UConverter_toUnicode_ISO_2022_OFFSETS_LOGIC (UConverterToUnicodeArgs * args,
                                                            UErrorCode * err);

U_CFUNC void T_UConverter_fromUnicode_ISO_2022(UConverterFromUnicodeArgs * args,
                                               UErrorCode * err);

U_CFUNC void T_UConverter_fromUnicode_ISO_2022_OFFSETS_LOGIC (UConverterFromUnicodeArgs * args,
                                                              UErrorCode * err);

U_CFUNC UChar32 T_UConverter_getNextUChar_ISO_2022 (UConverterToUnicodeArgs * args,
                                                    UErrorCode * err);

/***************** ISO-2022-JP ********************************/
U_CFUNC void UConverter_fromUnicode_ISO_2022_JP(UConverterFromUnicodeArgs* args, 
                                                UErrorCode* err);

U_CFUNC void UConverter_fromUnicode_ISO_2022_JP_OFFSETS_LOGIC(UConverterFromUnicodeArgs* args, 
                                                UErrorCode* err);

U_CFUNC void UConverter_toUnicode_ISO_2022_JP(UConverterToUnicodeArgs* args, 
                                              UErrorCode* err);

U_CFUNC void UConverter_toUnicode_ISO_2022_JP_OFFSETS_LOGIC(UConverterToUnicodeArgs* args, 
                                                            UErrorCode* err);

U_CFUNC UChar32 UConverter_getNextUChar_ISO_2022_JP (UConverterToUnicodeArgs * args,
                                                     UErrorCode * err);

/***************** ISO-2022-KR ********************************/
U_CFUNC void UConverter_fromUnicode_ISO_2022_KR(UConverterFromUnicodeArgs* args, 
                                                UErrorCode* err);

U_CFUNC void UConverter_fromUnicode_ISO_2022_KR_OFFSETS_LOGIC(UConverterFromUnicodeArgs* args, 
                                                              UErrorCode* err);

U_CFUNC void UConverter_toUnicode_ISO_2022_KR(UConverterToUnicodeArgs* args, 
                                              UErrorCode* err);

U_CFUNC void UConverter_toUnicode_ISO_2022_KR_OFFSETS_LOGIC(UConverterToUnicodeArgs* args, 
                                                            UErrorCode* err);

U_CFUNC UChar32 UConverter_getNextUChar_ISO_2022_KR (UConverterToUnicodeArgs * args,
                                                     UErrorCode * err);

/***************** ISO-2022-CN ********************************/
U_CFUNC void UConverter_fromUnicode_ISO_2022_CN(UConverterFromUnicodeArgs* args, 
                                                UErrorCode* err);

U_CFUNC void UConverter_fromUnicode_ISO_2022_CN_OFFSETS_LOGIC(UConverterFromUnicodeArgs* args, 
                                                UErrorCode* err);

U_CFUNC void UConverter_toUnicode_ISO_2022_CN(UConverterToUnicodeArgs* args, 
                                              UErrorCode* err);

U_CFUNC void UConverter_toUnicode_ISO_2022_CN_OFFSETS_LOGIC(UConverterToUnicodeArgs* args, 
                                                            UErrorCode* err);

U_CFUNC UChar32 UConverter_getNextUChar_ISO_2022_CN (UConverterToUnicodeArgs * args,
                                                     UErrorCode * err);

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
*	          int x = normalize_esq_chars_2022[27] which is equal to 1
*         ii) Search for this value in escSeqStateTable_Key_2022[]
*	          value of	x is stored at escSeqStateTable_Key_2022[0]
*        iii) Save this index as offset
*         iv) Get state of this sequence from escSeqStateTable_Value_2022[]
*	          escSeqStateTable_Value_2022[offset], which is VALID_NON_TERMINAL_2022
*     b) Switch on this state and continue to next char
*          i) Get the value of $ from normalize_esq_chars_2022[] with int value of $ as index
*	          which is	normalize_esq_chars_2022[36] == 4
*         ii) x is currently 1(from above) 
*	            x<<=5 -- x is now 32
*	            x+=normalize_esq_chars_2022[36]
*	            now x is 36
*        iii) Search for this value in escSeqStateTable_Key_2022[]
*	          value of	x is stored at escSeqStateTable_Key_2022[2], so offset is 2
*         iv) Get state of this sequence from escSeqStateTable_Value_2022[]
*	          escSeqStateTable_Value_2022[offset], which is VALID_NON_TERMINAL_2022
*     c) Switch on this state and continue to next char
*        i)  Get the value of B from normalize_esq_chars_2022[] with int value of B as index
*        ii) x is currently 36 (from above) 
*	         x<<=5 -- x is now 1152
*	         x+=normalize_esq_chars_2022[66]
*	         now x is 1161
*       iii) Search for this value in escSeqStateTable_Key_2022[]
*	         value of	x is stored at escSeqStateTable_Key_2022[21], so offset is 21
*        iv) Get state of this sequence from escSeqStateTable_Value_2022[21]
*	         escSeqStateTable_Value_2022[offset], which is VALID_TERMINAL_2022
*         v) Get the converter name form escSeqStateTable_Result_2022[21] which is JISX208
*/     


/*Below are the 3 arrays depicting a state transition table*/
int8_t normalize_esq_chars_2022[256] = {
/*       0	    1	    2       3	    4	   5       6	    7	    8       9	        */

         0	   ,0      ,0      ,0	   ,0      ,0      ,0	   ,0      ,0      ,0
        ,0	   ,0      ,0      ,0	   ,0      ,0      ,0	   ,0      ,0      ,0
        ,0	   ,0      ,0      ,0	   ,0      ,0      ,0	   ,1      ,0      ,0
        ,0	   ,0      ,0      ,0	   ,0      ,0      ,4	   ,7      ,0      ,0
        ,2	   ,24     ,26     ,27	   ,0      ,3      ,23	   ,6      ,0      ,0
        ,0	   ,0      ,0      ,0	   ,0      ,0      ,0	   ,0      ,0      ,0
        ,0	   ,0      ,0      ,0	   ,5      ,8      ,9	   ,10     ,11     ,12
        ,13	   ,14     ,15     ,16	   ,17     ,18     ,19	   ,20     ,25     ,28
        ,0	   ,0      ,21     ,0	   ,0      ,0      ,0	   ,0      ,0      ,0
        ,22	   ,0      ,0      ,0	   ,0      ,0      ,0	   ,0      ,0      ,0
        ,0	   ,0      ,0      ,0	   ,0      ,0      ,0	   ,0      ,0      ,0
        ,0	   ,0      ,0      ,0	   ,0      ,0      ,0	   ,0      ,0      ,0
        ,0	   ,0      ,0      ,0	   ,0      ,0      ,0	   ,0      ,0      ,0
        ,0	   ,0      ,0      ,0	   ,0      ,0      ,0	   ,0      ,0      ,0
        ,0	   ,0      ,0      ,0	   ,0      ,0      ,0	   ,0      ,0      ,0
        ,0	   ,0      ,0      ,0	   ,0      ,0      ,0	   ,0      ,0      ,0
        ,0	   ,0      ,0      ,0	   ,0      ,0      ,0	   ,0      ,0      ,0
        ,0	   ,0      ,0      ,0	   ,0      ,0      ,0	   ,0      ,0      ,0
        ,0	   ,0      ,0      ,0	   ,0      ,0      ,0	   ,0      ,0      ,0
        ,0	   ,0      ,0      ,0	   ,0      ,0      ,0	   ,0      ,0      ,0
        ,0	   ,0      ,0      ,0	   ,0      ,0      ,0	   ,0      ,0      ,0
        ,0	   ,0      ,0      ,0	   ,0      ,0      ,0	   ,0      ,0      ,0
        ,0	   ,0      ,0      ,0	   ,0      ,0      ,0	   ,0      ,0      ,0
        ,0	   ,0      ,0      ,0	   ,0      ,0      ,0	   ,0      ,0      ,0
        ,0	   ,0      ,0      ,0	   ,0      ,0      ,0	   ,0      ,0      ,0
        ,0	   ,0      ,0      ,0	   ,0      ,0
};

#define MAX_STATES_2022 74
int32_t escSeqStateTable_Key_2022[MAX_STATES_2022] = {
    /*      0	        1	        2	        3	        4	        5	        6	        7	        8	        9	        */
    
         1	        ,34	        ,36	        ,39	        ,55	        ,57	        ,60	        ,1093       ,1096       ,1097       
        ,1098       ,1099       ,1100       ,1101       ,1102       ,1103       ,1104       ,1105       ,1106       ,1109       
        ,1154       ,1157       ,1160       ,1161       ,1176       ,1178       ,1179       ,1254       ,1257       ,1768       
        ,1773       ,35105      ,36933      ,36936      ,36937      ,36938      ,36939      ,36940      ,36942      ,36943      
        ,36944      ,36945      ,36946      ,36947      ,36948      ,37640      ,37642      ,37644      ,37646      ,37711      
        ,37744      ,37745      ,37746      ,37747      ,37748      ,40133      ,40136      ,40138      ,40139      ,40140      
        ,40141      ,1123363    ,35947624   ,35947625   ,35947626   ,35947627   ,35947629   ,35947630   ,35947631   ,35947635   
        ,35947636   ,35947638
};
    
    
const char* escSeqStateTable_Result_2022[MAX_STATES_2022] = {
 /*      0		                1		             2	                    3		            4		        5		                    6		                   7		            8		            9	 */
    
         NULL		            ,NULL		        ,NULL	             ,NULL	            ,NULL		        ,NULL		            ,NULL	    	        ,"latin1"		        ,"latin1"		        ,"latin1"	            
        ,"ibm-865"	            ,"ibm-865"	        ,"ibm-865"	        ,"ibm-865"	        ,"ibm-865"	        ,"ibm-865"		        ,"JISX-201"	            ,"JISX-201"	            ,"latin1"		        ,"latin1"	            
        ,NULL		            ,"JISX-208"	        ,"gb_2312_80-1"     ,"JISX-208"	        ,NULL		        ,NULL		            ,NULL		            ,NULL		            ,"UTF8"		            ,"ISO-8859-1"           
        ,"ISO-8859-7"	        ,NULL		        ,"ibm-955"	        ,"ibm-367"	        ,"ibm-952"	        ,"ibm-949"		        ,"JISX-212"	            ,"ibm-1383"	            ,"ibm-952"		        ,"ibm-964"	            
        ,"ibm-964"	            ,"ibm-964"	        ,"ibm-964"	        ,"ibm-964"	        ,"ibm-964"	        ,"gb_2312_80-1"	        ,"ibm-949"		        ,"ISO-IR-165"           ,"CNS-11643-1992,1"	    ,"CNS-11643-1992,2"     
        ,"CNS-11643-1992,3"	    ,"CNS-11643-1992,4" ,"CNS-11643-1992,5" ,"CNS-11643-1992,6" ,"CNS-11643-1992,7" ,"UTF16_PlatformEndian" ,"UTF16_PlatformEndian" ,"UTF16_PlatformEndian" ,"UTF16_PlatformEndian" ,"UTF16_PlatformEndian" 
        ,"UTF16_PlatformEndian" ,NULL		        ,"latin1"	        ,"ibm-912"	        ,"ibm-913"	        ,"ibm-914"		        ,"ibm-813"		        ,"ibm-1089"	            ,"ibm-920"		        ,"ibm-915"	            
        ,"ibm-915"	            ,"latin1"
};
           
UCNV_TableStates_2022 escSeqStateTable_Value_2022[MAX_STATES_2022] = {
    /*          0		                    1			              2		                        3		                    4			                5		                        6		                 7			                8		                    9	    */
    
        VALID_NON_TERMINAL_2022    ,VALID_NON_TERMINAL_2022    ,VALID_NON_TERMINAL_2022    ,VALID_NON_TERMINAL_2022     ,VALID_NON_TERMINAL_2022    ,VALID_SS2_SEQUENCE	        ,VALID_SS2_SEQUENCE         ,VALID_TERMINAL_2022	    ,VALID_TERMINAL_2022        ,VALID_MAYBE_TERMINAL_2022  
        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022	    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022	    ,VALID_TERMINAL_2022	    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022	    ,VALID_TERMINAL_2022	    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        
        ,VALID_NON_TERMINAL_2022    ,VALID_TERMINAL_2022	    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022	    ,VALID_NON_TERMINAL_2022    ,VALID_NON_TERMINAL_2022    ,VALID_NON_TERMINAL_2022	,VALID_NON_TERMINAL_2022    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        
        ,VALID_TERMINAL_2022        ,VALID_NON_TERMINAL_2022    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022	    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022	    ,VALID_TERMINAL_2022	    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        
        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022	    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022	    ,VALID_TERMINAL_2022	    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022	    ,VALID_TERMINAL_2022	    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        
        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022	    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022	    ,VALID_TERMINAL_2022	    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022	    ,VALID_TERMINAL_2022	    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        
        ,VALID_TERMINAL_2022        ,VALID_NON_TERMINAL_2022    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022	    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022	    ,VALID_TERMINAL_2022	    ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022        
        ,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022
};
    
    
            
/*for 2022 looks ahead in the stream
*to determine the longest possible convertible
*data stream
*/
static const char* getEndOfBuffer_2022(const char** source,
                                       const char* sourceLimit,
                                       UBool flush); 
/*runs through a state machine to determine the escape sequence - codepage correspondance
*changes the pointer pointed to be _this->extraInfo
*/
static  void changeState_2022(UConverter* _this,
                                const char** source, 
                                const char* sourceLimit,
                                UBool flush,
                                UErrorCode* err); 

UCNV_TableStates_2022 getKey_2022(char source,
                                    int32_t* key,
                                    int32_t* offset);
            
/*********** ISO 2022 Converter Protos ***********/
static void _ISO2022Open(UConverter *cnv, const char *name, const char *locale,uint32_t* version, UErrorCode *errorCode);
static void _ISO2022Close(UConverter *converter);
static void _ISO2022Reset(UConverter *converter);   

static UConverterImpl _ISO2022Impl={
    UCNV_ISO_2022,
    
    NULL,
    NULL,
    
    _ISO2022Open,
    _ISO2022Close,
    _ISO2022Reset,
    
    T_UConverter_toUnicode_ISO_2022,
    T_UConverter_toUnicode_ISO_2022_OFFSETS_LOGIC,
    T_UConverter_fromUnicode_ISO_2022,
    T_UConverter_fromUnicode_ISO_2022_OFFSETS_LOGIC,
    T_UConverter_getNextUChar_ISO_2022,
    
    NULL
};
            
const UConverterStaticData _ISO2022StaticData={
    sizeof(UConverterStaticData),
        "ISO_2022",
        2022, UCNV_IBM, UCNV_ISO_2022, 1, 4,
    { 0x1a, 0, 0, 0 },1, FALSE, FALSE,
    { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0} /* reserved */
};
            
            
const UConverterSharedData _ISO2022Data={
    sizeof(UConverterSharedData), ~((uint32_t) 0),
        NULL, NULL, &_ISO2022StaticData, FALSE, &_ISO2022Impl, 
        0
};

static void _ISO2022Open(UConverter *cnv, const char *name, const char *locale,uint32_t* version, UErrorCode *errorCode){
    
    char myLocale[6]={' ',' ',' ',' ',' ',' '};
    
    cnv->extraInfo = uprv_malloc (sizeof (UConverterDataISO2022));
    if(cnv->extraInfo != NULL) {
        UConverterDataISO2022 *myConverterData=(UConverterDataISO2022 *) cnv->extraInfo; 
        myConverterData->currentConverter = NULL;
        myConverterData->fromUnicodeConverter = NULL;
        myConverterData->plane = -1;
        myConverterData->key =0;
        cnv->fromUnicodeStatus =FALSE;
        if(locale){
            uprv_strcpy(myLocale,locale);
            myConverterData->isLocaleSpecified = TRUE;
        }
        myConverterData->version= 0;
        myConverterData->myConverterArray[0] =NULL;
        if(myLocale[0]=='j' && (myLocale[1]=='a'|| myLocale[1]=='p') && 
            (myLocale[2]=='_' || myLocale[2]=='\0')){
            
            /* open the required converters and cache them */
            myConverterData->myConverterArray[0]=   ucnv_open("ASCII", errorCode );
            myConverterData->myConverterArray[1]=   ucnv_open("ISO8859_1", errorCode);
            myConverterData->myConverterArray[2]=   ucnv_open("ISO8859_7", errorCode);
            myConverterData->myConverterArray[3]=   ucnv_open("jisx-201", errorCode);
            myConverterData->myConverterArray[4]=   ucnv_open("jisx-208", errorCode);
            myConverterData->myConverterArray[5]=   ucnv_open("jisx-212", errorCode);
            myConverterData->myConverterArray[6]=   ucnv_open("gb_2312_80-1", errorCode);
            myConverterData->myConverterArray[7]=   ucnv_open("ksc_5601_1", errorCode);
            myConverterData->myConverterArray[8]=   NULL;
            
            /* initialize the state variables */
            myConverterData->toUnicodeCurrentState =ASCII;
            myConverterData->fromUnicodeCurrentState= ASCII;
            myConverterData->targetIndex = 0;
            myConverterData->sourceIndex =0;
            myConverterData->isEscapeAppended=FALSE;
            myConverterData->isShiftAppended=FALSE;
            myConverterData->isFirstBuffer = TRUE;
            myConverterData->isLocaleSpecified=TRUE;

            /*set the substitution chars*/
            ucnv_setSubstChars(cnv,"\x1b\x28\x42\x1A", 4, errorCode);
            
            /* set the function pointers to appropriate funtions */
            _ISO2022Impl.toUnicode		= UConverter_toUnicode_ISO_2022_JP;
            _ISO2022Impl.toUnicodeWithOffsets   = UConverter_toUnicode_ISO_2022_JP_OFFSETS_LOGIC;
            _ISO2022Impl.fromUnicode            = UConverter_fromUnicode_ISO_2022_JP;
            _ISO2022Impl.fromUnicodeWithOffsets = UConverter_fromUnicode_ISO_2022_JP_OFFSETS_LOGIC;
            _ISO2022Impl.getNextUChar           = UConverter_getNextUChar_ISO_2022_JP;
            
            if(version){
                switch (*version){
                    case '0':
                        myConverterData->version = 0;
                        break;
                    case '1':
                        myConverterData->version = 1;
                        break;
                    case '2':
                        myConverterData->version = 2;
                        break;
                    default:
                        myConverterData->version = 0;
                }
            }
        }
        else if(myLocale[0]=='k' && (myLocale[1]=='o'|| myLocale[1]=='r') && 
            (myLocale[2]=='_' || myLocale[2]=='\0')){
            
            /* in ISO-2022-KR the desginator sequence appears only once
            * in a file so we append it only once
            */
            cnv->charErrorBufferLength = 4;
            cnv->charErrorBuffer[0] = 0x1b;
            cnv->charErrorBuffer[1] = 0x24;
            cnv->charErrorBuffer[2] = 0x29;
            cnv->charErrorBuffer[3] = 0x43;
            
            /* initialize the state variables */
            myConverterData->fromUnicodeConverter  = ucnv_open("ibm-949",errorCode);
            myConverterData->isLocaleSpecified=TRUE;
            
            /*set the substitution chars*/
            ucnv_setSubstChars(cnv,"\x0F\x1A", 2, errorCode);
            
            /* set the function pointers to appropriate funtions */
            _ISO2022Impl.toUnicode		= UConverter_toUnicode_ISO_2022_KR;
            _ISO2022Impl.toUnicodeWithOffsets   = UConverter_toUnicode_ISO_2022_KR_OFFSETS_LOGIC;
            _ISO2022Impl.fromUnicode            = UConverter_fromUnicode_ISO_2022_KR;
            _ISO2022Impl.fromUnicodeWithOffsets = UConverter_fromUnicode_ISO_2022_KR_OFFSETS_LOGIC;
            _ISO2022Impl.getNextUChar           = UConverter_getNextUChar_ISO_2022_KR;
            
        }
        else if((myLocale[0]=='z'|| myLocale[0]=='c') && (myLocale[1]=='h'|| myLocale[1]=='n') && 
            (myLocale[2]=='_' || myLocale[2]=='\0')){
            
            /* open the required converters and cache them */
            myConverterData->myConverterArray[0] = ucnv_open("ASCII",errorCode);
            myConverterData->myConverterArray[1] = ucnv_open("gb_2312_80-1",errorCode);
            myConverterData->myConverterArray[2] = ucnv_open("ISO-IR-165",errorCode);
            myConverterData->myConverterArray[3] = ucnv_open("CNS-11643-1992",errorCode);
            myConverterData->myConverterArray[4] = NULL;
            
            /*initialize the state variables*/
            myConverterData->toUnicodeCurrentState =ASCII;
            myConverterData->fromUnicodeCurrentState =ASCII;
            myConverterData->targetIndex = 0;
            myConverterData->sourceIndex =0;
            myConverterData->isEscapeAppended=FALSE;
            myConverterData->isShiftAppended=FALSE;
            myConverterData->isLocaleSpecified=TRUE;
            /*set the substitution chars*/
            ucnv_setSubstChars(cnv,"\x0F\x1A", 2, errorCode);
            /* set the function pointers to appropriate funtions */
            _ISO2022Impl.toUnicode		        = UConverter_toUnicode_ISO_2022_CN;
            _ISO2022Impl.toUnicodeWithOffsets   = UConverter_toUnicode_ISO_2022_CN_OFFSETS_LOGIC;
            _ISO2022Impl.fromUnicode            = UConverter_fromUnicode_ISO_2022_CN;
            _ISO2022Impl.fromUnicodeWithOffsets = UConverter_fromUnicode_ISO_2022_CN_OFFSETS_LOGIC;
            _ISO2022Impl.getNextUChar           = UConverter_getNextUChar_ISO_2022_CN;
            if(version){
                switch (*version){
                    case '0':
                        myConverterData->version = 0;
                        break;
                    case '1':
                        myConverterData->version = 1;
                        break;
                    default:
                        myConverterData->version = 0;
                }
            }
            
        }
        else{
            /* append the UTF-8 escape sequence */
            cnv->charErrorBufferLength = 3;
            cnv->charErrorBuffer[0] = 0x1b;
            cnv->charErrorBuffer[1] = 0x25;
            cnv->charErrorBuffer[2] = 0x42;
            
            /* initialize the state variables */
            myConverterData->isLocaleSpecified=FALSE;
        }
                    
    } else {
        *errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    
}


static void
_ISO2022Close(UConverter *converter) {
   UConverter **array = ((UConverterDataISO2022 *) (converter->extraInfo))->myConverterArray;
    
    if (converter->extraInfo != NULL) {
        /*ucnv_close (((UConverterDataISO2022 *) (converter->extraInfo))->currentConverter);*/
        /*close the array of converter pointers and free the memory*/
        while(*array!=NULL){
            if(*array==((UConverterDataISO2022 *) (converter->extraInfo))->currentConverter){
               ((UConverterDataISO2022 *) (converter->extraInfo))->currentConverter=NULL;
            }
            ucnv_close(*array++);
            
        }
        if(((UConverterDataISO2022 *) (converter->extraInfo))->currentConverter){
            ucnv_close(((UConverterDataISO2022 *) (converter->extraInfo))->currentConverter);
        }
        uprv_free (converter->extraInfo);
    }
}

static void
_ISO2022Reset(UConverter *converter) {
    UConverterDataISO2022 *myConverterData=(UConverterDataISO2022 *) (converter->extraInfo);
    if(! myConverterData->isLocaleSpecified){
        
        /* re-append UTF-8 escape sequence */
        converter->charErrorBufferLength = 3;
        converter->charErrorBuffer[0] = 0x1b;
        converter->charErrorBuffer[1] = 0x28;
        converter->charErrorBuffer[2] = 0x42;
    }
    else {
        /* reset the state variables */
        myConverterData->toUnicodeCurrentState =ASCII;
        myConverterData->fromUnicodeCurrentState =ASCII;
        myConverterData->targetIndex = 0;
        myConverterData->sourceIndex =0;
        myConverterData->isEscapeAppended=FALSE;
        myConverterData->isShiftAppended=FALSE;
        
    }
    if (converter->mode == UCNV_SO){
        
        ucnv_close (myConverterData->currentConverter);
        myConverterData->currentConverter = NULL;
        converter->mode = UCNV_SI;
    }
    myConverterData->isFirstBuffer=TRUE;
    
}



/**********************************************************************************
*  ISO-2022 Converter
*
*
*/

U_CFUNC UChar32 T_UConverter_getNextUChar_ISO_2022(UConverterToUnicodeArgs* args,
                                                   UErrorCode* err){
    const char* mySourceLimit;
    /*Arguments Check*/
    if  (args->sourceLimit < args->source){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return 0xffff;
    }
    
    do{
        
        mySourceLimit =	 getEndOfBuffer_2022(&(args->source), args->sourceLimit, TRUE); 
        /*Find the end of the buffer e.g : Next Escape Seq | end of Buffer*/
        if (args->converter->mode == UCNV_SO) /*Already doing some conversion*/{
            
            return ucnv_getNextUChar(((UConverterDataISO2022*)(args->converter->extraInfo))->currentConverter,
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
               err);
    }while(args->source < args->sourceLimit);
    
    return 0xffff;
}


U_CFUNC void T_UConverter_fromUnicode_ISO_2022(UConverterFromUnicodeArgs *args,
                                               UErrorCode* err){
    
    T_UConverter_fromUnicode_UTF8(args, err);
    
}


U_CFUNC void T_UConverter_fromUnicode_ISO_2022_OFFSETS_LOGIC(UConverterFromUnicodeArgs* args,
                                                             UErrorCode* err){
    
    char const* targetStart = args->target;
    T_UConverter_fromUnicode_UTF8_OFFSETS_LOGIC(args, err);
    {
        
        int32_t len = args->target - targetStart;
        int32_t i;
        /* uprv_memmove(offsets+3, offsets, len);   MEMMOVE SEEMS BROKEN --srl*/ 
        
        for(i=len-1;i>=0;i--)       args->offsets[i] = args->offsets[i];
    }
    
}


U_CFUNC void T_UConverter_toUnicode_ISO_2022(UConverterToUnicodeArgs *args,
                                             UErrorCode* err){
    
    const char *mySourceLimit;
    char const* sourceStart;
    UConverter *saveThis;
    
    /*Arguments Check*/
    if (U_FAILURE(*err)) 
        return;
    
    if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    
    do{
        
        /*Find the end of the buffer e.g : Next Escape Seq | end of Buffer*/
        mySourceLimit =	 getEndOfBuffer_2022(&(args->source), args->sourceLimit, args->flush); 
        
        if (args->converter->mode == UCNV_SO) /*Already doing some conversion*/{
            
            saveThis = args->converter;
            args->offsets = NULL;
            args->converter = ((UConverterDataISO2022*)(args->converter->extraInfo))->currentConverter;
            ucnv_toUnicode(args->converter,
                &args->target,
                args->targetLimit,
                &args->source,
                mySourceLimit,
                args->offsets,
                args->flush,
                err);
            args->converter = saveThis;
        }
        if((((UConverterDataISO2022 *)args->converter->extraInfo)->isFirstBuffer) && (args->source[0]!=(char)ESC_2022)
            &&  (((UConverterDataISO2022*)(args->converter->extraInfo))->currentConverter==NULL)){
            
            
            saveThis = args->converter;
            args->offsets = NULL;
            ((UConverterDataISO2022*)(args->converter->extraInfo))->currentConverter = ucnv_open("ASCII",err);
            
            if(U_FAILURE(*err)){
                break;
            }
            
            args->converter = ((UConverterDataISO2022*)(args->converter->extraInfo))->currentConverter;
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
            ((UConverterDataISO2022*)(args->converter->extraInfo))->isFirstBuffer=FALSE;
            
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
            args->flush,
            err);
        /* args->source = sourceStart; */
        
        
    }while(args->source < args->sourceLimit);
    
    ((UConverterDataISO2022*)(args->converter->extraInfo))->isFirstBuffer=FALSE;
    
    return;
}

U_CFUNC void T_UConverter_toUnicode_ISO_2022_OFFSETS_LOGIC(UConverterToUnicodeArgs* args,
                                                           UErrorCode* err){
    
    int32_t myOffset=0;
    int32_t base = 0;
    const char* mySourceLimit;
    char const* sourceStart;
    UConverter* saveThis = NULL;
    
    /*Arguments Check*/
    if (U_FAILURE(*err)) 
        return;
    if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    
    do{
        mySourceLimit =	 getEndOfBuffer_2022(&(args->source), args->sourceLimit, args->flush); 
        /*Find the end of the buffer e.g : Next Escape Seq | end of Buffer*/
        
        if (args->converter->mode == UCNV_SO) /*Already doing some conversion*/{
            const UChar* myTargetStart = args->target;
            
            saveThis = args->converter;
            args->converter = ((UConverterDataISO2022*)(saveThis->extraInfo))->currentConverter;
            ucnv_toUnicode(args->converter, 
                &(args->target),
                args->targetLimit,
                &(args->source),
                mySourceLimit,
                args->offsets,
                args->flush,
                err);
            
            args->converter = saveThis;
            {
                int32_t	lim =  args->target - myTargetStart;
                int32_t	i = 0;
                for (i=base; i < lim;i++){   
                    args->offsets[i] += myOffset;
                }
                base +=	lim;
            }
            
        }
        if(((UConverterDataISO2022 *)args->converter->extraInfo)->isFirstBuffer && args->source[0]!=ESC_2022
            && ((UConverterDataISO2022*)(args->converter->extraInfo))->currentConverter==NULL){
            
            const UChar* myTargetStart = args->target;
            UConverter* saveThis = args->converter;
            args->offsets = NULL;
            ((UConverterDataISO2022*)(args->converter->extraInfo))->currentConverter = ucnv_open("ASCII",err);
            
            if(U_FAILURE(*err)){
                break;
            }
            
            args->converter = ((UConverterDataISO2022*)(args->converter->extraInfo))->currentConverter;
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
            ((UConverterDataISO2022*)(args->converter->extraInfo))->isFirstBuffer=FALSE;
            args->converter = saveThis;
            {
                int32_t	lim =  args->target - myTargetStart;
                int32_t	i = 0;
                for (i=base; i < lim;i++){   
                    args->offsets[i] += myOffset;
                }
                base +=	lim;
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
            args->flush,
            err);
        myOffset += args->source - sourceStart;
        
    }while(mySourceLimit != args->sourceLimit);
    
    return;
}
UCNV_TableStates_2022 getKey_2022(char c,
                                  int32_t* key,
                                  int32_t* offset){
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



static void changeState_2022(UConverter* _this,
                             const char** source, 
                             const char* sourceLimit,
                             UBool flush,
                             UErrorCode* err){
    UConverter* myUConverter;
    UCNV_TableStates_2022 value;
    UConverterDataISO2022* myData2022 = ((UConverterDataISO2022*)_this->extraInfo);
    uint32_t key = myData2022->key;
    const char* chosenConverterName = NULL;
    const char* sourceStart =*source;
    int32_t offset;
    
    /*In case we were in the process of consuming an escape sequence
    we need to reprocess it */
    
    do{
        
        /* Needed explicit cast for key on MVS to make compiler happy - JJD */
        if(**source == UCNV_SI || **source==UCNV_SO){
            if(sourceStart == *source)
                (*source)++;
            return;
        }
        value = getKey_2022(**source,(int32_t *) &key, &offset);
        switch (value){
        case VALID_NON_TERMINAL_2022 : 
            break;
            
        case VALID_TERMINAL_2022:
            {
                (*source)++;
                chosenConverterName = escSeqStateTable_Result_2022[offset];
                key	= 0;
                goto DONE;
            };
            break;
            
        case INVALID_2022:
            {
                myData2022->key = key;
                *err = U_ILLEGAL_ESCAPE_SEQUENCE;
                return;
            }
        case VALID_SS2_SEQUENCE:
            {
                (*source)++;
                key	= 0;
                goto DONE;
            }
            
        case VALID_MAYBE_TERMINAL_2022:
            {
                const char* mySource = (*source+1);
                int32_t myKey =	key;
                UCNV_TableStates_2022 myValue = value;
                int32_t myOffset=0;
                if(*mySource==ESC_2022){
                    while ((mySource < sourceLimit) && 
                        ((myValue == VALID_MAYBE_TERMINAL_2022)||(myValue ==	VALID_NON_TERMINAL_2022))){
                        myValue	= getKey_2022(*(mySource++), &myKey, &myOffset);
                    }
                }
                else{
                    (*source)++;
                    myValue=(UCNV_TableStates_2022) 1;
                    myOffset = 7;
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
                        key	= 0;
                        value = VALID_TERMINAL_2022;
                        goto DONE;
                    };
                    break;
                    
                    /* Not expected. Added to make the gcc happy */
                case VALID_SS2_SEQUENCE:
                    {
                        (*source)++;
                        key	= 0;
                        goto DONE;
                    }
                    
                case VALID_NON_TERMINAL_2022: 
                    
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
    
    if ((value == VALID_NON_TERMINAL_2022) || (value == VALID_MAYBE_TERMINAL_2022)) {
        return;
    }
    if (value > 0 ) {
        if(value==3){
            _this->mode = UCNV_SI;
            myUConverter =myData2022->currentConverter;
        }
        else{
            _this->mode = UCNV_SI;
            ucnv_close(myData2022->currentConverter);
            myData2022->currentConverter = myUConverter = ucnv_open(chosenConverterName, err);
            
        }
        if (U_SUCCESS(*err)){
            /*Customize the converter with the attributes set on the 2022 converter*/
            myUConverter->fromUCharErrorBehaviour = _this->fromUCharErrorBehaviour;
            myUConverter->fromUContext = _this->fromUContext;
            myUConverter->fromCharErrorBehaviour = _this->fromCharErrorBehaviour;
            myUConverter->toUContext = _this->toUContext;
            
            uprv_memcpy(myUConverter->subChar, 
                _this->subChar,
                myUConverter->subCharLen	= _this->subCharLen);
            
            _this->mode = UCNV_SO;
        }
    }
    
    return;
}

/*Checks the characters of the buffer against valid 2022 escape sequences
*if the match we return a pointer to the initial start of the sequence otherwise
*we return sourceLimit
*/
static const char* getEndOfBuffer_2022(const char** source,
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
            * is it possible to have an ESC character in a	ISO2022
            * byte stream which is	valid in a code page? Is it legal?
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
        else if(*mySource == (char)UCNV_SI || *mySource==(char)UCNV_SO){
            return mySource;
            
        }
        
    }while (mySource++ < sourceLimit);
    
    return sourceLimit;
}


/**************************************ISO-2022-JP*************************************************/

/************************************** IMPORTANT **************************************************
* The T_fromUnicode_ISO2022_JP converter doesnot use ucnv_fromUnicode() functions for SBCS,DBCS and
* MBCS instead the values are obtained directly by accessing the sharedData structs through ucmp8_getU() 
* ucmp16_getU() macros,and for MBCS by emulating the Markus's code to increase speed, reduce the 
* overhead of function call and make it efficient.The converter iterates over each Unicode codepoint 
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

static Cnv2022Type myConverterType[8]={
    ASCII1,
    LATIN1,
    SBCS,
    SBCS,
    DBCS,
    DBCS,
    DBCS,
    DBCS,
        
};

static StateEnum nextStateArray[3][8]= {
    {JISX201,INVALID_STATE,INVALID_STATE,JISX208,ASCII,INVALID_STATE,INVALID_STATE,INVALID_STATE},
    {JISX201,INVALID_STATE,INVALID_STATE,JISX208,JISX212,ASCII,INVALID_STATE,INVALID_STATE},
    {ISO8859_1,ISO8859_7,JISX201,JISX208,JISX212,GB2312,KSC5601,ASCII}
};
static  char* escSeqChars[8] ={
    "\x1B\x28\x42",         /* <ESC>(B  ASCII       */
    "\x1B\x2E\x41",         /* <ESC>.A  ISO-8859-1  */
    "\x1B\x2E\x46",         /* <ESC>.F  ISO-8859-7  */
    "\x1B\x28\x4A",         /* <ESC>(J  JISX-201    */
    "\x1B\x24\x42",         /* <ESC>$B  JISX-208    */
    "\x1B\x24\x28\x44",     /* <ESC>$(D JISX-212    */
    "\x1B\x24\x41",         /* <ESC>$A  GB2312      */
    "\x1B\x24\x28\x43",     /* <ESC>$(C KSC5601     */
        
};


static void concatChar(UConverterFromUnicodeArgs* args, int32_t *targetIndex, int32_t *targetLength,
                       int8_t charToAppend,UErrorCode* err,int32_t *sourceIndex);

static void concatEscape(UConverterFromUnicodeArgs* args, int32_t *targetIndex, int32_t *targetLength,
                         const	char* strToAppend,UErrorCode* err,int len,int32_t *sourceIndex);

static void concatString(UConverterFromUnicodeArgs* args, int32_t *targetIndex, int32_t *targetLength,
                         const	UChar32* strToAppend,UErrorCode* err,int32_t *sourceIndex);

/*
* The iteration over various code pages works this way:
* i)   Get the currentState from myConverterData->currentState
* ii)  Check if the character is mapped to a valid character in the currentState
*      Yes ->  a) set the initIterState to currentState
*	    b) remain in this state until an invalid character is found
*      No  ->  a) go to the next code page and find the character
* iii) Before changing the state increment the current state check if the current state 
*      is equal to the intitIteration state
*      Yes ->  A character that cannot be represented in any of the supported encodings
*	    break and return a U_INVALID_CHARACTER error
*      No  ->  Continue and find the character in next code page
*
* Offsets Logic is handled by utility functions concatChar(), concatEscape() and concatString()
*
* TODO: Implement a priority technique where the users are allowed to set the priority of code pages 
*/


U_CFUNC void UConverter_fromUnicode_ISO_2022_JP(UConverterFromUnicodeArgs* args, UErrorCode* err){
    UChar* mySource =(UChar*)args->source;
    
    UConverterDataISO2022 *myConverterData=(UConverterDataISO2022*)args->converter->extraInfo;
    UConverterCallbackReason reason;
    UBool isEscapeAppended = FALSE;
    StateEnum initIterState;
    unsigned char *myTarget = (unsigned char *) args->target; 
    const UChar *saveSource;
    char *saveTarget;
    uint32_t targetValue=0;
    
    int32_t myTargetLength = args->targetLimit - args->target;
    int32_t mySourceLength = args->sourceLimit - args->source;
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    int32_t length  =0;
    CompactShortArray *myFromUnicodeDBCS = NULL;
    CompactShortArray *myFromUnicodeDBCSFallback = NULL;
    CompactByteArray  *myFromUnicodeSBCS = NULL;
    CompactByteArray  *myFromUnicodeSBCSFallback = NULL;
    UChar32 targetUniChar = missingCharMarker;
    StateEnum currentState=ASCII;
    Cnv2022Type myType =ASCII1;
    UChar32 mySourceChar = 0x0000;
    int iterCount = 0;
    const char *escSeq = NULL;
    UBool isShiftAppended = FALSE;
    UBool isTargetUCharDBCS=FALSE,oldIsTargetUCharDBCS=FALSE; 
    isEscapeAppended =(UBool) myConverterData->isEscapeAppended;
    isShiftAppended =(UBool) myConverterData->isShiftAppended;
    initIterState =ASCII;

    /* arguments check*/
    if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    initIterState = myConverterData->fromUnicodeCurrentState;
    if(args->converter->fromUSurrogateLead!=0 && myTargetIndex < myTargetLength) {
        goto getTrail;
    }
    while(mySourceIndex <  mySourceLength){
        currentState = myConverterData->fromUnicodeCurrentState;
        myConverterData->fromUnicodeConverter = (myConverterData->fromUnicodeConverter == NULL) ?
            myConverterData->myConverterArray[0] :
        myConverterData->myConverterArray[(int)myConverterData->fromUnicodeCurrentState];
        isTargetUCharDBCS   = (UBool) args->converter->fromUnicodeStatus;
        
        if(myTargetIndex < myTargetLength){
            
            mySourceChar = (UChar) args->source[mySourceIndex++];
            
            myType= (Cnv2022Type) myConverterType[currentState];
            
            /* I am handling surrogates in the begining itself so that I donot have to go through 8 
            * iterations on codepages that we support. Adapted from MBCS 
            */
            if(UTF_IS_SURROGATE(mySourceChar)) {
                if(UTF_IS_SURROGATE_FIRST(mySourceChar)) {
                    args->converter->fromUSurrogateLead=(UChar)mySourceChar;
getTrail:
                    /*look ahead to find the trail surrogate*/
                    if(mySourceIndex <  mySourceLength) {
                        /* test the following code unit */
                        UChar trail=(UChar) args->source[mySourceIndex];
                        if(UTF_IS_SECOND_SURROGATE(trail)) {
                            ++mySourceIndex;
                            mySourceChar=UTF16_GET_PAIR_VALUE(mySourceChar, trail);
                            args->converter->fromUSurrogateLead=0x00;
                            /* convert this surrogate code point */
                            /* exit this condition tree */
                        } else {
                            /* this is an unmatched lead code unit (1st surrogate) */
                            /* callback(illegal) */
                            reason=UCNV_ILLEGAL;
                            *err=U_ILLEGAL_CHAR_FOUND;
                            goto CALLBACK;
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
                    goto CALLBACK;
                }
            }
            /*Do the conversion*/
            if(mySourceChar == 0x0020){
                
                if(currentState > 2){
                    concatEscape(args, &myTargetIndex, &myTargetLength,	escSeqChars[0],err,strlen(escSeqChars[0]),&mySourceIndex);
                    
                    isTargetUCharDBCS=FALSE;
                }
                concatString(args, &myTargetIndex, &myTargetLength,&mySourceChar,err,&mySourceIndex);
                myConverterData->isEscapeAppended=isEscapeAppended =FALSE;
                TEST_ERROR_CONDITION(args,myTargetIndex, mySourceIndex,	isTargetUCharDBCS,myConverterData, err);
                continue;
            }
            /* if the source character is CR or LF then append the ASCII escape sequence*/
            else if(mySourceChar== 0x000A || mySourceChar== 0x000D || mySourceChar==0x0009 || mySourceChar==0x000B){
                
                if((isTargetUCharDBCS || currentState==JISX201) &&  mySource[mySourceIndex-2]!=0x000D){
                    concatEscape(args, &myTargetIndex, &myTargetLength,	escSeqChars[0],err,strlen(escSeqChars[0]),&mySourceIndex);
                    isTargetUCharDBCS=FALSE;
                    isShiftAppended	=FALSE;
                    myConverterData->isEscapeAppended=isEscapeAppended=FALSE;
                    myConverterData->isShiftAppended=FALSE;
                    
                }
                
                concatString(args, &myTargetIndex, &myTargetLength,&mySourceChar,err,&mySourceIndex);
                
                if(currentState==ISO8859_1 || currentState ==ISO8859_7)
                    isEscapeAppended =FALSE;
                
                TEST_ERROR_CONDITION(args,myTargetIndex, mySourceIndex,	isTargetUCharDBCS,myConverterData, err);
                
                continue;
            }
            else{
                
                do{
                    switch (myType){
                    
                        case SBCS:
                            if( mySourceChar <0xffff) {
                                myFromUnicodeSBCS =	&myConverterData->fromUnicodeConverter->sharedData->table->sbcs.fromUnicode;
                                myFromUnicodeSBCSFallback = &myConverterData->fromUnicodeConverter->sharedData->table->sbcs.fromUnicodeFallback;
                    
                                targetUniChar = (UChar32) ucmp8_getu (myFromUnicodeSBCS, mySourceChar);
                    
                                if ((targetUniChar==0)&&(args->converter->useFallback == TRUE) &&
                                    (myConverterData->fromUnicodeConverter->sharedData->staticData->hasFromUnicodeFallback == TRUE)){
                                    targetUniChar = (UChar32) ucmp8_getu (myFromUnicodeSBCSFallback, mySourceChar);
                                }
                                /* ucmp8_getU returns 0 for missing char so	explicitly set it missingCharMarker*/
                                targetUniChar=(UChar)((targetUniChar==0) ? (UChar) missingCharMarker : targetUniChar);
                            }
                            break;
                    
                        case DBCS:
                            if(mySourceChar < 0xffff){
                                myFromUnicodeDBCS =	&myConverterData->fromUnicodeConverter->sharedData->table->dbcs.fromUnicode;
                                myFromUnicodeDBCSFallback = &myConverterData->fromUnicodeConverter->sharedData->table->dbcs.fromUnicodeFallback;
                                targetUniChar = (UChar) ucmp16_getu (myFromUnicodeDBCS,	mySourceChar);
                        
                                if ((targetUniChar==missingCharMarker)&&(args->converter->useFallback== TRUE) &&
                                    (myConverterData->fromUnicodeConverter->sharedData->staticData->hasFromUnicodeFallback == TRUE)){
                                    targetUniChar = (UChar) ucmp16_getu (myFromUnicodeDBCSFallback, mySourceChar);
                                }
                            }
                            break;
                    
                        case MBCS:
                            length= _MBCSFromUChar32(myConverterData->fromUnicodeConverter->sharedData,
                                mySourceChar,&targetValue,args->converter->useFallback);
                    
                            targetUniChar = (UChar32) targetValue;
                    
                            if(length==0x0000){
                                targetUniChar = missingCharMarker;
                                *err =U_ZERO_ERROR;
                            } 
                            /* only DBCS or SBCS characters are	expected*/
                            else if(length > 2){
                                reason =UCNV_ILLEGAL;
                                *err =U_INVALID_CHAR_FOUND;
                                goto CALLBACK;
                            }
                            break;
                    
                        case LATIN1:
                            if(mySourceChar < 0x0100){
                                targetUniChar = mySourceChar;
                            } 
                            else 
                                targetUniChar = missingCharMarker;
                            break;

                        case ASCII1:
                            if(mySourceChar < 0x7f){
                                targetUniChar = mySourceChar;
                            }
                            else 
                                targetUniChar = missingCharMarker;
                            break;
                        default:
                            /*not expected */ 
                            break;
                    }
                    if(targetUniChar==missingCharMarker){
                        isEscapeAppended = FALSE; 
                        /* save the state */
                        myConverterData->fromUnicodeCurrentState=nextStateArray[myConverterData->version][currentState];
                        myConverterData->isEscapeAppended = isEscapeAppended;
                        myConverterData->isShiftAppended =isShiftAppended;
                        args->converter->fromUnicodeStatus = isTargetUCharDBCS;
                       /* myConverterData->sourceIndex = mySourceIndex;
                        myConverterData->targetIndex = myTargetIndex;*/
                        currentState = myConverterData->fromUnicodeCurrentState;
                        myConverterData->fromUnicodeConverter = (myConverterData->fromUnicodeConverter == NULL) ?
                        myConverterData->myConverterArray[0] :
                                        myConverterData->myConverterArray[(int)myConverterData->fromUnicodeCurrentState];
                        isTargetUCharDBCS   = (UBool) args->converter->fromUnicodeStatus;
                        myType= (Cnv2022Type) myConverterType[currentState];
                    }

                }while(targetUniChar==missingCharMarker && initIterState != currentState);

            }
            
            if(targetUniChar!= missingCharMarker){
                
                oldIsTargetUCharDBCS = isTargetUCharDBCS;
                isTargetUCharDBCS =(UBool) (targetUniChar >0x00FF);
                args->converter->fromUnicodeStatus= isTargetUCharDBCS;
                /* set the iteration state and iteration count  */	    
                initIterState = currentState;
                iterCount =0;
                /* Append the escpace sequence */
                if(!isEscapeAppended){
                    escSeq = escSeqChars[(int)currentState];
                    concatEscape(args, &myTargetIndex, &myTargetLength, 
                        escSeqChars[(int)currentState],
                        err,strlen(escSeqChars[(int)currentState]),&mySourceIndex);
                    
                    isEscapeAppended =TRUE;
                    myConverterData->isEscapeAppended=TRUE;
                    
                    /* Append SSN for shifting to G2 */
                    if(currentState==ISO8859_1 || currentState==ISO8859_7){

                        concatEscape(args, &myTargetIndex, &myTargetLength,
                            UCNV_SS2,err,strlen(UCNV_SS2),&mySourceIndex);
                    }
                }

                concatString(args, &myTargetIndex, &myTargetLength,
                    &targetUniChar,err, &mySourceIndex);
                
                TEST_ERROR_CONDITION(args,myTargetIndex, mySourceIndex,	isTargetUCharDBCS,myConverterData, err);
                
            }/* end of end if(targetUniChar==missingCharMarker)*/
            else{
                
                /* if we cannot	find the character after checking all codepages 
                 * then this is	an error
                 */
                    reason = UCNV_UNASSIGNED;
                    *err = U_INVALID_CHAR_FOUND;
                    args->converter->invalidUCharBuffer[0]=(UChar)mySourceChar;
                    args->converter->invalidUCharLength++;
                    
CALLBACK:
                    saveSource = args->source;
                    saveTarget = args->target;
     
                    args->target = (char*)myTarget + myTargetIndex;
                    args->source = mySource + mySourceIndex;
                    myConverterData->isShiftAppended =isShiftAppended;
                    args->converter->fromUnicodeStatus = isTargetUCharDBCS;
                            
                    args->converter->fromUnicodeStatus = (int32_t)isTargetUCharDBCS;

                    FromU_CALLBACK_MACRO(args->converter->fromUContext,
                        args,
                        args->converter->invalidUCharBuffer,
                        args->converter->invalidUCharLength,
                        (UChar32) (args->converter->invalidUCharLength == 2 ? 
                        UTF16_GET_PAIR_VALUE(args->converter->invalidUCharBuffer[0], 
                        args->converter->invalidUCharBuffer[1]) 
                        : args->converter->invalidUCharBuffer[0]),
                        reason,
                        err);

                    myConverterData->isEscapeAppended = isEscapeAppended=FALSE;
                    args->source=saveSource;
                    args->target=saveTarget;
                    args->converter->fromUSurrogateLead=0x00;
                    initIterState = myConverterData->fromUnicodeCurrentState;
                    isTargetUCharDBCS  = (UBool)(args->converter->fromUnicodeStatus);
                    args->converter->invalidUCharLength = 0;
                    if (U_FAILURE (*err)){
                        break;
                    }
                    
            }
            targetUniChar =missingCharMarker;
        } /* end if(myTargetIndex<myTargetLength) */
        else{
            *err =U_BUFFER_OVERFLOW_ERROR;
            break;
        } 
        
    }/* end while(mySourceIndex<mySourceLength) */

    if (args->converter->fromUSurrogateLead !=0 && (mySourceIndex == mySourceLength) && args->flush){
        if (U_SUCCESS(*err) ){
            *err = U_TRUNCATED_CHAR_FOUND;
            args->converter->toUnicodeStatus = 0x00;
        }
    }

    /*save the state and return */
    args->target += myTargetIndex;
    args->source += mySourceIndex;
    myConverterData->sourceIndex = 0;
    myConverterData->targetIndex = 0;
    args->converter->fromUnicodeStatus = isTargetUCharDBCS;
    
}
U_CFUNC void UConverter_fromUnicode_ISO_2022_JP_OFFSETS_LOGIC(UConverterFromUnicodeArgs* args, UErrorCode* err){
    UChar* mySource =(UChar*)args->source;
    
    UConverterDataISO2022 *myConverterData=(UConverterDataISO2022*)args->converter->extraInfo;
    UConverterCallbackReason reason;
    UBool isEscapeAppended = FALSE;
    StateEnum initIterState;
    unsigned char *myTarget = (unsigned char *) args->target; 
    const UChar *saveSource;
    char *saveTarget;
    uint32_t targetValue=0;
    int32_t *saveOffsets ;
    int32_t myTargetLength = args->targetLimit - args->target;
    int32_t mySourceLength = args->sourceLimit - args->source;
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    int32_t length  =0;
    CompactShortArray *myFromUnicodeDBCS = NULL;
    CompactShortArray *myFromUnicodeDBCSFallback = NULL;
    CompactByteArray  *myFromUnicodeSBCS = NULL;
    CompactByteArray  *myFromUnicodeSBCSFallback = NULL;
    UChar32 targetUniChar = missingCharMarker;
    StateEnum currentState=ASCII;
    Cnv2022Type myType=ASCII1;
    UChar32 mySourceChar = 0x0000;
    int iterCount = 0;
    int32_t currentOffset;
    const char *escSeq = NULL;
    UBool isShiftAppended = FALSE;
    UBool isTargetUCharDBCS=FALSE,oldIsTargetUCharDBCS=FALSE; 
    isEscapeAppended =(UBool) myConverterData->isEscapeAppended;
    isShiftAppended =(UBool) myConverterData->isShiftAppended;
    initIterState =ASCII;

    /* arguments check*/
    if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    initIterState = myConverterData->fromUnicodeCurrentState;
    if(args->converter->fromUSurrogateLead!=0 && myTargetIndex < myTargetLength) {
        goto getTrail;
    }
    while(mySourceIndex <  mySourceLength){
        currentState = myConverterData->fromUnicodeCurrentState;
        myConverterData->fromUnicodeConverter = (myConverterData->fromUnicodeConverter == NULL) ?
            myConverterData->myConverterArray[0] :
        myConverterData->myConverterArray[(int)myConverterData->fromUnicodeCurrentState];
        isTargetUCharDBCS   = (UBool) args->converter->fromUnicodeStatus;
        
        if(myTargetIndex < myTargetLength){

            mySourceChar = (UChar) args->source[mySourceIndex++];
            
            myType= (Cnv2022Type) myConverterType[currentState];
            
            /* I am handling surrogates in the begining itself so that I donot have to go through 8 
            * iterations on codepages that we support. 
            */
            if(UTF_IS_SURROGATE(mySourceChar)) {
                if(UTF_IS_SURROGATE_FIRST(mySourceChar)) {
                    args->converter->fromUSurrogateLead=(UChar)mySourceChar;
getTrail:
                    /*look ahead to find the trail surrogate*/
                    if(mySourceIndex <  mySourceLength) {
                        /* test the following code unit */
                        UChar trail=(UChar) args->source[mySourceIndex];
                        if(UTF_IS_SECOND_SURROGATE(trail)) {
                            ++mySourceIndex;
                            mySourceChar=UTF16_GET_PAIR_VALUE(mySourceChar, trail);
                            args->converter->fromUSurrogateLead=0x00;
                            /* convert this surrogate code point */
                            /* exit this condition tree */
                        } else {
                            /* this is an unmatched lead code unit (1st surrogate) */
                            /* callback(illegal) */
                            reason=UCNV_ILLEGAL;
                            *err=U_ILLEGAL_CHAR_FOUND;
                            goto CALLBACK;
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
                    goto CALLBACK;
                }
            }
            /*Do the conversion*/
            if(mySourceChar == 0x0020){
                
                if(currentState > 2){
                    concatEscape(args, &myTargetIndex, &myTargetLength,	escSeqChars[0],err,strlen(escSeqChars[0]),&mySourceIndex);
                    
                    isTargetUCharDBCS=FALSE;
                }
                concatString(args, &myTargetIndex, &myTargetLength,&mySourceChar,err,&mySourceIndex);
                myConverterData->isEscapeAppended=isEscapeAppended =FALSE;
                TEST_ERROR_CONDITION(args,myTargetIndex, mySourceIndex,	isTargetUCharDBCS,myConverterData, err);
                continue;
            }
            /* if the source character is CR or LF then append the ASCII escape sequence*/
            else if(mySourceChar== 0x000A || mySourceChar== 0x000D || mySourceChar==0x0009 || mySourceChar==0x000B){
                
                if((isTargetUCharDBCS || currentState==JISX201) &&  mySource[mySourceIndex-2]!=0x000D){
                    concatEscape(args, &myTargetIndex, &myTargetLength,	escSeqChars[0],err,strlen(escSeqChars[0]),&mySourceIndex);
                    isTargetUCharDBCS=FALSE;
                    isShiftAppended	=FALSE;
                    myConverterData->isEscapeAppended=isEscapeAppended=FALSE;
                    myConverterData->isShiftAppended=FALSE;
                    
                }
                
                concatString(args, &myTargetIndex, &myTargetLength,&mySourceChar,err,&mySourceIndex);
                
                if(currentState==ISO8859_1 || currentState ==ISO8859_7)
                    isEscapeAppended =FALSE;
                
                TEST_ERROR_CONDITION(args,myTargetIndex, mySourceIndex,	isTargetUCharDBCS,myConverterData, err);
                
                continue;
            }
            else{
                
                do{
                    switch (myType){
                    
                        case SBCS:
                            if(mySourceChar < 0xffff){
                                myFromUnicodeSBCS =	&myConverterData->fromUnicodeConverter->sharedData->table->sbcs.fromUnicode;
                                myFromUnicodeSBCSFallback = &myConverterData->fromUnicodeConverter->sharedData->table->sbcs.fromUnicodeFallback;
                    
                                targetUniChar = (UChar32) ucmp8_getu (myFromUnicodeSBCS, mySourceChar);
                    
                                if ((targetUniChar==0)&&(args->converter->useFallback == TRUE) &&
                                    (myConverterData->fromUnicodeConverter->sharedData->staticData->hasFromUnicodeFallback == TRUE)){
                                    targetUniChar = (UChar32) ucmp8_getu (myFromUnicodeSBCSFallback, mySourceChar);
                                }
                                /* ucmp8_getU returns 0 for missing char so	explicitly set it missingCharMarker*/
                                targetUniChar=(UChar)((targetUniChar==0) ? (UChar) missingCharMarker : targetUniChar);
                            }
                            break;
                    
                        case DBCS:
                            if(mySourceChar < 0xffff){
                                myFromUnicodeDBCS =	&myConverterData->fromUnicodeConverter->sharedData->table->dbcs.fromUnicode;
                                myFromUnicodeDBCSFallback = &myConverterData->fromUnicodeConverter->sharedData->table->dbcs.fromUnicodeFallback;
                                targetUniChar = (UChar) ucmp16_getu (myFromUnicodeDBCS,	mySourceChar);
                        
                                if ((targetUniChar==missingCharMarker)&&(args->converter->useFallback== TRUE) &&
                                    (myConverterData->fromUnicodeConverter->sharedData->staticData->hasFromUnicodeFallback == TRUE)){
                                    targetUniChar = (UChar) ucmp16_getu (myFromUnicodeDBCSFallback, mySourceChar);
                                }
                            }
                            break;
                    
                        case MBCS:
                            length= _MBCSFromUChar32(myConverterData->fromUnicodeConverter->sharedData,
                                mySourceChar,&targetValue,args->converter->useFallback);
                    
                            targetUniChar = (UChar32) targetValue;
                    
                            if(length==0x0000){
                                targetUniChar = missingCharMarker;
                                *err =U_ZERO_ERROR;
                            } 
                            /* only DBCS or SBCS characters are	expected*/
                            else if(length > 2){
                                reason =UCNV_ILLEGAL;
                                *err =U_INVALID_CHAR_FOUND;
                                goto CALLBACK;
                            }
                            break;
                    
                        case LATIN1:
                            if(mySourceChar < 0x0100){
                                targetUniChar = mySourceChar;
                            } 
                            else 
                                targetUniChar = missingCharMarker;
                            break;

                        case ASCII1:
                            if(mySourceChar < 0x7f){
                                targetUniChar = mySourceChar;
                            }
                            else 
                                targetUniChar = missingCharMarker;
                            break;
                        default:
                            /*not expected */ 
                            break;
                    }
                    if(targetUniChar==missingCharMarker){
                        isEscapeAppended = FALSE; 
                        /* save the state */
                        myConverterData->fromUnicodeCurrentState=nextStateArray[myConverterData->version][currentState];
                        myConverterData->isEscapeAppended = isEscapeAppended;
                        myConverterData->isShiftAppended =isShiftAppended;
                        args->converter->fromUnicodeStatus = isTargetUCharDBCS;
                        currentState = myConverterData->fromUnicodeCurrentState;
                        myConverterData->fromUnicodeConverter = (myConverterData->fromUnicodeConverter == NULL) ?
                        myConverterData->myConverterArray[0] :
                                        myConverterData->myConverterArray[(int)myConverterData->fromUnicodeCurrentState];
                        isTargetUCharDBCS   = (UBool) args->converter->fromUnicodeStatus;
                        myType= (Cnv2022Type) myConverterType[currentState];
                    }

                }while(targetUniChar==missingCharMarker && initIterState != currentState);

            }
            
            if(targetUniChar!= missingCharMarker){
                
                oldIsTargetUCharDBCS = isTargetUCharDBCS;
                isTargetUCharDBCS =(UBool) (targetUniChar >0x00FF);
                args->converter->fromUnicodeStatus= isTargetUCharDBCS;
                /* set the iteration state and iteration count  */	    
                initIterState = currentState;
                iterCount =0;
                /* Append the escpace sequence */
                if(!isEscapeAppended){
                    escSeq = escSeqChars[(int)currentState];
                    concatEscape(args, &myTargetIndex, &myTargetLength, 
                        escSeqChars[(int)currentState],
                        err,strlen(escSeqChars[(int)currentState]),&mySourceIndex);
                    
                    isEscapeAppended =TRUE;
                    myConverterData->isEscapeAppended=TRUE;
                    
                    /* Append SSN for shifting to G2 */
                    if(currentState==ISO8859_1 || currentState==ISO8859_7){

                        concatEscape(args, &myTargetIndex, &myTargetLength,
                            UCNV_SS2,err,strlen(UCNV_SS2),&mySourceIndex);
                    }
                }

                concatString(args, &myTargetIndex, &myTargetLength,
                    &targetUniChar,err, &mySourceIndex);
                
                TEST_ERROR_CONDITION(args,myTargetIndex, mySourceIndex,	isTargetUCharDBCS,myConverterData, err);
                
            }/* end of end if(targetUniChar==missingCharMarker)*/
            else{
                
                /* if we cannot	find the character after checking all codepages 
                 * then this is	an error
                 */
                    reason = UCNV_UNASSIGNED;
                    *err = U_INVALID_CHAR_FOUND;
                    
CALLBACK:
                    args->converter->invalidUCharBuffer[args->converter->invalidUCharLength++]=(UChar)mySourceChar;
                    currentOffset = args->offsets[myTargetIndex-1]+1;
                    saveSource = args->source;
                    saveTarget = args->target;
                    saveOffsets = args->offsets;
                    args->target = (char*)myTarget + myTargetIndex;
                    args->source = mySource + mySourceIndex;

                    myConverterData->isEscapeAppended = isEscapeAppended;
                    myConverterData->isShiftAppended =isShiftAppended;
                    args->converter->fromUnicodeStatus = isTargetUCharDBCS;
                    myConverterData->sourceIndex = mySourceIndex;
                    myConverterData->targetIndex = myTargetIndex;
                               
                    args->converter->fromUnicodeStatus = (int32_t)isTargetUCharDBCS;

                    args->offsets = args->offsets?args->offsets+myTargetIndex:0;
                    FromU_CALLBACK_OFFSETS_LOGIC_MACRO(args->converter->fromUContext,
                        args,
                        args->converter->invalidUCharBuffer,
                        args->converter->invalidUCharLength,
                        (UChar32) (args->converter->invalidUCharLength == 2 ? 
                        UTF16_GET_PAIR_VALUE(args->converter->invalidUCharBuffer[0], 
                        args->converter->invalidUCharBuffer[1]) 
                        : args->converter->invalidUCharBuffer[0]),
                        reason,
                        err);
             
                    args->source=saveSource;
                    args->target=saveTarget;
                    args->offsets=saveOffsets;
                    initIterState = myConverterData->fromUnicodeCurrentState;
                    isTargetUCharDBCS  = (UBool)(args->converter->fromUnicodeStatus);
                    myConverterData->isEscapeAppended = isEscapeAppended=FALSE;
                    args->converter->invalidUCharLength = 0;
                    args->converter->fromUSurrogateLead=0x00;
                    if (U_FAILURE (*err)){
                        break;
                    }
                    
            }
            targetUniChar =missingCharMarker;
        } /* end if(myTargetIndex<myTargetLength) */
        else{
            *err =U_BUFFER_OVERFLOW_ERROR;
            break;
        } 
        
    }/* end while(mySourceIndex<mySourceLength) */
    /*If at the end of conversion we are still carrying state information
    *flush is TRUE, we can deduce that the input stream is truncated
    */
    if (args->converter->fromUSurrogateLead !=0 && (mySourceIndex == mySourceLength) && args->flush){
        if (U_SUCCESS(*err)){
            *err = U_TRUNCATED_CHAR_FOUND;
            args->converter->toUnicodeStatus = 0x00;
        }
    }
    /*save the state and return */
    args->target += myTargetIndex;
    args->source += mySourceIndex;
    myConverterData->sourceIndex = 0;
    myConverterData->targetIndex = 0;
    args->converter->fromUnicodeStatus = isTargetUCharDBCS;
    
}

static void concatString(UConverterFromUnicodeArgs* args, int32_t *targetIndex, int32_t *targetLength,
                         const	UChar32* strToAppend,UErrorCode* err, int32_t *sourceIndex){
    
    if(*strToAppend < 0x00FF){
        if( (*targetIndex)+1 >= *targetLength){
            args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (unsigned char) *strToAppend;
            *err = U_BUFFER_OVERFLOW_ERROR;
        }else{
            args->target[*targetIndex] = (unsigned char) *strToAppend;
            
            if(args->offsets!=NULL){
                args->offsets[*targetIndex] = *sourceIndex-1;
            }
            (*targetIndex)++;
            
        }
    }
    else{
        if(*targetIndex < *targetLength){
            args->target[*targetIndex] =(unsigned char) (*strToAppend>>8);
            if(args->offsets!=NULL){
                args->offsets[*targetIndex] = *sourceIndex-1;
            }
            (*targetIndex)++;
            
            if(*targetIndex < *targetLength){
                args->target[(*targetIndex)] =(unsigned char) (*strToAppend & 0x00FF);
                
                if(args->offsets!=NULL){
                    args->offsets[*targetIndex]	= *sourceIndex-1;
                }
                (*targetIndex)++;
            }
            else{
                args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (unsigned char) (*strToAppend & 0x00FF);
                *err = U_BUFFER_OVERFLOW_ERROR;
                
            }
            
        }
        else{
            args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (unsigned char) (*strToAppend>>8);
            args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (unsigned char) (*strToAppend & 0x00FF);
            *err = U_BUFFER_OVERFLOW_ERROR;
            if(args->offsets!=NULL){
                args->offsets[*targetIndex] = *sourceIndex-1;
                
            }
        }
    }
    
}


static void concatEscape(UConverterFromUnicodeArgs* args, int32_t *targetIndex, int32_t *targetLength,
                         const	char* strToAppend,UErrorCode* err,int len,int32_t *sourceIndex){
    while(len-->0){
        if(*targetIndex < *targetLength){
            args->target[*targetIndex] = (unsigned char) *strToAppend;
            if(args->offsets!=NULL){
                args->offsets[*targetIndex] = *sourceIndex-1;
            }
            (*targetIndex)++;
        }
        else{
            args->converter->charErrorBuffer[(int)args->converter->charErrorBufferLength++] = (unsigned char) *strToAppend;
            *err =U_BUFFER_OVERFLOW_ERROR;
        }
        strToAppend++;
    }
}

static void concatChar(UConverterFromUnicodeArgs* args, int32_t *targetIndex, int32_t *targetLength,
                       int8_t charToAppend,UErrorCode* err,int32_t *sourceIndex){
    if( *targetIndex < *targetLength){
        args->target[(*targetIndex)++] = (unsigned char) charToAppend;
        if(args->offsets!=NULL){
                args->offsets[*targetIndex] = *sourceIndex-1;
        }
    }else{
        args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (unsigned char) charToAppend;
        *err = U_BUFFER_OVERFLOW_ERROR;
    }
}

/*************** to unicode *******************/

/*
* This is a simple, interim implementation of GetNextUChar()
* that allows to concentrate on testing one single implementation
* of the ToUnicode conversion before it gets copied to
* multiple version that are then optimized for their needs
* (with vs. without offsets and getNextUChar).
*/

U_CFUNC UChar32
UConverter_getNextUChar_ISO_2022_JP(UConverterToUnicodeArgs *pArgs,
                                    UErrorCode *pErrorCode) {
    UChar buffer[UTF_MAX_CHAR_LENGTH];
    const char *realLimit=pArgs->sourceLimit;
    
    pArgs->target=buffer;
    pArgs->targetLimit=buffer+UTF_MAX_CHAR_LENGTH;
    
    while(pArgs->source<realLimit) {
        /* feed in one byte at a time to make sure to get only one character out */
        pArgs->sourceLimit=pArgs->source+1;
        pArgs->flush= (UBool)(pArgs->sourceLimit==realLimit);
        UConverter_toUnicode_ISO_2022_JP(pArgs, pErrorCode);
        if(U_FAILURE(*pErrorCode) && *pErrorCode!=U_BUFFER_OVERFLOW_ERROR) {
            return 0xffff;
        } else if(pArgs->target!=buffer) {
            if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR) {
                *pErrorCode=U_ZERO_ERROR;
            }
            return ucnv_getUChar32KeepOverflow(pArgs->converter, buffer, pArgs->target-buffer);
        }
    }
    
    /* no output because of empty input or only state changes and skipping callbacks */
    *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
    return 0xffff;
}

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
static StateEnum nextStateToUnicodeJP[3][MAX_STATES_2022]= {

    {
/*      0		                1		             2	                    3		            4		        5		                    6		                   7		            8		            9	 */
            
        INVALID_STATE		    ,INVALID_STATE		,INVALID_STATE      ,INVALID_STATE	    ,INVALID_STATE      ,INVALID_STATE          ,INVALID_STATE          ,INVALID_STATE		    ,INVALID_STATE		    ,ASCII1	            
        ,INVALID_STATE          ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE          ,JISX201	            ,JISX201	            ,INVALID_STATE          ,INVALID_STATE          
        ,INVALID_STATE		    ,JISX208	        ,INVALID_STATE      ,JISX208	        ,INVALID_STATE		,INVALID_STATE		    ,INVALID_STATE		    ,INVALID_STATE		    ,INVALID_STATE          ,INVALID_STATE          
        ,INVALID_STATE	        ,INVALID_STATE		,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE	    ,INVALID_STATE		    ,INVALID_STATE	        ,INVALID_STATE          ,INVALID_STATE	        ,INVALID_STATE          
        ,INVALID_STATE          ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE	        ,INVALID_STATE		    ,INVALID_STATE          ,INVALID_STATE  	    ,INVALID_STATE     
        ,INVALID_STATE	        ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE          ,INVALID_STATE          ,INVALID_STATE          ,INVALID_STATE          ,INVALID_STATE
        ,INVALID_STATE          ,INVALID_STATE		,INVALID_STATE      ,INVALID_STATE	    ,INVALID_STATE	    ,INVALID_STATE		    ,INVALID_STATE		    ,INVALID_STATE	        ,INVALID_STATE		    ,INVALID_STATE	            
        ,INVALID_STATE          ,INVALID_STATE
    },
    
    {
/*      0		                1		             2	                    3		            4		        5		                    6		                   7		            8		            9	 */
            
        INVALID_STATE		    ,INVALID_STATE		,INVALID_STATE      ,INVALID_STATE	    ,INVALID_STATE      ,INVALID_STATE          ,INVALID_STATE          ,INVALID_STATE		    ,INVALID_STATE		    ,ASCII1	            
        ,INVALID_STATE          ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE          ,JISX201	            ,JISX201	            ,INVALID_STATE          ,INVALID_STATE          
        ,INVALID_STATE		    ,JISX208	        ,INVALID_STATE      ,JISX208	        ,INVALID_STATE		,INVALID_STATE		    ,INVALID_STATE		    ,INVALID_STATE		    ,INVALID_STATE          ,INVALID_STATE          
        ,INVALID_STATE	        ,INVALID_STATE		,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE	    ,INVALID_STATE		    ,JISX212	            ,INVALID_STATE          ,INVALID_STATE	        ,INVALID_STATE          
        ,INVALID_STATE          ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE	        ,INVALID_STATE		    ,INVALID_STATE          ,INVALID_STATE  	    ,INVALID_STATE     
        ,INVALID_STATE	        ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE          ,INVALID_STATE          ,INVALID_STATE          ,INVALID_STATE          ,INVALID_STATE
        ,INVALID_STATE          ,INVALID_STATE		,INVALID_STATE      ,INVALID_STATE	    ,INVALID_STATE	    ,INVALID_STATE		    ,INVALID_STATE		    ,INVALID_STATE	        ,INVALID_STATE		    ,INVALID_STATE	            
        ,INVALID_STATE          ,INVALID_STATE
    },
        
    {
/*      0		                1		             2	                    3		            4		        5		                    6		                   7		            8		            9	 */
            
        INVALID_STATE		    ,INVALID_STATE		,INVALID_STATE      ,INVALID_STATE	    ,INVALID_STATE      ,INVALID_STATE          ,INVALID_STATE          ,INVALID_STATE		    ,INVALID_STATE		    ,ASCII1	            
        ,INVALID_STATE          ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE          ,JISX201	            ,JISX201	            ,INVALID_STATE          ,INVALID_STATE          
        ,INVALID_STATE		    ,JISX208	        ,GB2312             ,JISX208	        ,INVALID_STATE		,INVALID_STATE		    ,INVALID_STATE		    ,INVALID_STATE		    ,INVALID_STATE          ,ISO8859_1          
        ,ISO8859_7  	        ,INVALID_STATE		,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE	    ,KSC5601		        ,JISX212	            ,INVALID_STATE          ,INVALID_STATE	        ,INVALID_STATE          
        ,INVALID_STATE          ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE  	    ,INVALID_STATE		    ,INVALID_STATE          ,INVALID_STATE  	    ,INVALID_STATE     
        ,INVALID_STATE	        ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE      ,INVALID_STATE          ,INVALID_STATE          ,INVALID_STATE          ,INVALID_STATE          ,INVALID_STATE
        ,INVALID_STATE          ,INVALID_STATE		,INVALID_STATE      ,INVALID_STATE	    ,INVALID_STATE	    ,INVALID_STATE		    ,INVALID_STATE		    ,INVALID_STATE	        ,INVALID_STATE		    ,INVALID_STATE	            
        ,INVALID_STATE          ,INVALID_STATE
    }
};
static void changeState_2022_JP(UConverter* _this,
                             const char** source, 
                             const char* sourceLimit,
                             UBool flush,
                             UErrorCode* err){
    UConverter* myUConverter;
    UCNV_TableStates_2022 value;
    UConverterDataISO2022* myData2022 = ((UConverterDataISO2022*)_this->extraInfo);
    uint32_t key = myData2022->key;
    const char* chosenConverterName = NULL;
    const char* sourceStart =*source;
    int32_t offset;
    
    /*In case we were in the process of consuming an escape sequence
    we need to reprocess it */
    
    do{
        
        /* Needed explicit cast for key on MVS to make compiler happy - JJD */
        if(**source == UCNV_SI || **source==UCNV_SO){
            if(sourceStart == *source)
                (*source)++;
            return;
        }
        value = getKey_2022(**source,(int32_t *) &key, &offset);
        switch (value){
        case VALID_NON_TERMINAL_2022 : 
            break;
            
        case VALID_TERMINAL_2022:
            {
                (*source)++;
                chosenConverterName = escSeqStateTable_Result_2022[offset];
                key	= 0;
                goto DONE;
            };
            break;
            
        case INVALID_2022:
            {
                myData2022->key = key;
                *err = U_ILLEGAL_ESCAPE_SEQUENCE;
                return;
            }
        case VALID_SS2_SEQUENCE:
            {
                (*source)++;
                key	= 0;
                goto DONE;
            }
            
        case VALID_MAYBE_TERMINAL_2022:
            {
                const char* mySource = (*source+1);
                int32_t myKey =	key;
                UCNV_TableStates_2022 myValue = value;
                int32_t myOffset=0;
                if(*mySource==ESC_2022){
                    while ((mySource < sourceLimit) && 
                        ((myValue == VALID_MAYBE_TERMINAL_2022)||(myValue ==	VALID_NON_TERMINAL_2022))){
                        myValue	= getKey_2022(*(mySource++), &myKey, &myOffset);
                    }
                }
                else{
                    (*source)++;
                    myValue=(UCNV_TableStates_2022) 1;
                    myOffset = 9;
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
                        key	= 0;
                        value = VALID_TERMINAL_2022;
                        goto DONE;
                    };
                    break;
                    
                    /* Not expected. Added to make the gcc happy */
                case VALID_SS2_SEQUENCE:
                    {
                        (*source)++;
                        key	= 0;
                        goto DONE;
                    }
                    
                case VALID_NON_TERMINAL_2022: 
                    
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
    
    if ((value == VALID_NON_TERMINAL_2022) || (value == VALID_MAYBE_TERMINAL_2022)) {
        return;
    }
    if (value > 0 ) {
        if(value==3){
            _this->mode = UCNV_SI;
            myUConverter =myData2022->currentConverter;
        }
        else{
            _this->mode = UCNV_SI;
            myData2022->currentConverter = myUConverter = 
                myData2022->myConverterArray[nextStateToUnicodeJP[myData2022->version][offset]]; 
            myData2022->toUnicodeCurrentState = nextStateToUnicodeJP[myData2022->version][offset];   
               
            
        }
        if (U_SUCCESS(*err)){
            /*Customize the converter with the attributes set on the 2022 converter*/
            myUConverter->fromUCharErrorBehaviour = _this->fromUCharErrorBehaviour;
            myUConverter->fromUContext = _this->fromUContext;
            myUConverter->fromCharErrorBehaviour = _this->fromCharErrorBehaviour;
            myUConverter->toUContext = _this->toUContext;
            
            uprv_memcpy(myUConverter->subChar, 
                _this->subChar,
                myUConverter->subCharLen	= _this->subCharLen);
            
            _this->mode = UCNV_SO;
        }
    }
    
    return;
}

U_CFUNC void UConverter_toUnicode_ISO_2022_JP(UConverterToUnicodeArgs *args,
                                              UErrorCode* err){
    char tempBuf[2] ;
    const char* pBuf;
    const char *mySource = ( char *) args->source;
    UChar *myTarget = args->target;
    char *tempLimit = &tempBuf[2]+1; 
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    const char *mySourceLimit = args->sourceLimit;
    UChar32 targetUniChar = 0x0000;
    UChar mySourceChar = 0x0000;
    UConverterDataISO2022* myData=(UConverterDataISO2022*)(args->converter->extraInfo);
    CompactShortArray *myToUnicodeDBCS=NULL, *myToUnicodeFallbackDBCS = NULL; 
    UChar *myToUnicodeSBCS = NULL, *myToUnicodeFallbackSBCS = NULL;
    pBuf = &tempBuf[0];
    /*Arguments Check*/
    if (U_FAILURE(*err)) 
        return;
    
    if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    
    while(mySource< args->sourceLimit){
        
        if(myTarget < args->targetLimit){
            
            mySourceChar= (unsigned char) *mySource++;
            
            if(args->converter->mode==UCNV_SI){
                    
               /* if there	are no escape sequences in the first buffer then they
                * are assumed to be ASCII according to RFC-1554
                */    
                myData->toUnicodeCurrentState = ASCII1;
             }
            
            switch(mySourceChar){
                case 0x0A:
                    if(args->converter->toUnicodeStatus !=	0x00){
                        goto SAVE_STATE;
                    }
                     myData->toUnicodeCurrentState = ASCII1; 
                    break;
                
                case 0x0D:
                    if(args->converter->toUnicodeStatus !=	0x00){
                        goto SAVE_STATE;
                    }
                     myData->toUnicodeCurrentState = ASCII1;

                    break;
                                
                case 0x20:
                    if(args->converter->toUnicodeStatus !=	0x00){
                        goto SAVE_STATE;
                    }
                    myData->toUnicodeCurrentState = ASCII1;

                    break;
                            
                default:
                    /* if we are in the middle of consuming an escape sequence 
                     * we continue to next switch tag else we break
                     */
                    if(myData->key==0){
                        break;
                    }

                case ESC_2022:
                    if(args->converter->toUnicodeStatus != 0x00){
                        goto SAVE_STATE;
                    }
                    mySource--;
                    changeState_2022_JP(args->converter,&(mySource), 
                        args->sourceLimit, args->flush,err);
                    if(U_FAILURE(*err)){
                        goto SAVE_STATE;
                    }
                    continue;
            }
            
            switch(myConverterType[myData->toUnicodeCurrentState]){
                
                 case ASCII1:
                    if(args->converter->toUnicodeStatus == 0x00 && mySourceChar < 0x7F){
                        targetUniChar = (UChar)	 mySourceChar;
                    }
                    else{
                        goto SAVE_STATE;
                    }
                    break;
                case SBCS:
                    if(args->converter->toUnicodeStatus == 0x00){
                        myToUnicodeSBCS	= myData->currentConverter->sharedData->table->sbcs.toUnicode;
                        myToUnicodeFallbackSBCS = myData->currentConverter->sharedData->table->sbcs.toUnicodeFallback;
                        targetUniChar = myToUnicodeSBCS[(unsigned char) mySourceChar];
                        if(targetUniChar> 0xfffe){
                            if((args->converter->useFallback == TRUE) && 
                                (myData->currentConverter->sharedData->staticData->hasFromUnicodeFallback == TRUE)){
                            
                                targetUniChar =	myToUnicodeFallbackSBCS[(unsigned char) mySource[mySourceIndex-1]];
                            }
                        }
                        
                    }
                    else{
                        goto SAVE_STATE;
                    }
                    break;
                
                case DBCS:
                    myToUnicodeDBCS	= &myData->currentConverter->sharedData->table->dbcs.toUnicode;
                    myToUnicodeFallbackDBCS = &myData->currentConverter->sharedData->table->dbcs.toUnicodeFallback;
                
                    if(args->converter->toUnicodeStatus == 0x00){
                        args->converter->toUnicodeStatus = (UChar) mySourceChar;
                        continue;
                    }
                    else{
                        tempBuf[0] =	(char) args->converter->toUnicodeStatus ;
                        tempBuf[1] =	(char) mySourceChar;
                        mySourceChar= (UChar)((args->converter->toUnicodeStatus << 8) | (mySourceChar & 0x00ff));
                        args->converter->toUnicodeStatus =0x00;
                    
                        targetUniChar = ucmp16_getu(myToUnicodeDBCS,mySourceChar);
                        if(targetUniChar> 0xfffe){
                            if((args->converter->useFallback == TRUE) && 
                                (myData->currentConverter->sharedData->staticData->hasFromUnicodeFallback == TRUE)){
                            
                                targetUniChar =	(UChar) ucmp16_getu(myToUnicodeFallbackDBCS, mySourceChar);
                            }
                        }
                    }
                
                    break;
                
                case MBCS:
                
                    if(args->converter->toUnicodeStatus == 0x00){
                        args->converter->toUnicodeStatus = (UChar) mySourceChar;
                        continue;
                    }
                    else{
                        tempBuf[0] = (char) (args->converter->toUnicodeStatus);
                        tempBuf[1] = (char) (mySourceChar);
                        args->converter->toUnicodeStatus = 0x00;
                        mySourceChar= (UChar)((args->converter->toUnicodeStatus << 8) | (mySourceChar & 0x00ff));
                        pBuf = &tempBuf[0];
                        tempLimit = &tempBuf[2]+1;
                        targetUniChar	= _MBCSSimpleGetNextUChar(myData->currentConverter->sharedData,
                             &pBuf,tempLimit,args->converter->useFallback);
                    }
                    break;
                    
                case LATIN1:
                    if(args->converter->fromUnicodeStatus == 0x00 && mySourceChar < 0x100){
                        targetUniChar = (UChar)	 mySourceChar;
                    }
                    else{
                        goto SAVE_STATE;
                    }
                    break;
                
                case INVALID_STATE:
                    *err = U_ILLEGAL_ESCAPE_SEQUENCE;
                    goto SAVE_STATE;

            }
            if(targetUniChar < 0xfffe){
                *(myTarget++)=(UChar)targetUniChar;
                targetUniChar=missingCharMarker;
            }
            else if(targetUniChar>=0xfffe){
SAVE_STATE:
                {
                    const char *saveSource = args->source;
                    UChar *saveTarget = args->target;
                    UConverterCallbackReason reason;
                
                    if(targetUniChar == 0xfffe){
                        reason = UCNV_UNASSIGNED;
                        *err = U_INVALID_CHAR_FOUND;
                    }
                    else{
                        reason = UCNV_ILLEGAL;
                        *err = U_ILLEGAL_CHAR_FOUND;
                    }
                    
                    if(myConverterType[myData->toUnicodeCurrentState] == SBCS || 
                        myConverterType[myData->toUnicodeCurrentState]== ASCII1 ||
                        myConverterType[myData->toUnicodeCurrentState]== LATIN1){
                    
                        args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = (char) mySourceChar;
                    }
                    else{
                        
                        args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = tempBuf[0];
                        args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = tempBuf[1];
    
                    }

                    args->target = myTarget;
                    args->source = mySource;
                    ToU_CALLBACK_MACRO( args->converter->toUContext,
                        args,
                        args->converter->invalidCharBuffer,
                        args->converter->invalidCharLength,
                        reason,
                        err);
                    myTarget += args->target - myTarget;
                    args->source = saveSource;
                    args->target = saveTarget;
                    args->converter->invalidCharLength=0;
                    if(U_FAILURE(*err))
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
        if(U_SUCCESS(*err)){
            *err = U_TRUNCATED_CHAR_FOUND;
            args->converter->toUnicodeStatus = 0x00;
        }
    }
    args->target = myTarget;
    args->source = mySource;
}

U_CFUNC void UConverter_toUnicode_ISO_2022_JP_OFFSETS_LOGIC(UConverterToUnicodeArgs *args,
                                                            UErrorCode* err){
    char tempBuf[2];
    const char* pBuf;
    const char *mySource = ( char *) args->source;
    UChar *myTarget = args->target;
    char *tempLimit = &tempBuf[2]+1; 
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    const char *mySourceLimit = args->sourceLimit;
    UChar32 targetUniChar = 0x0000;
    UChar mySourceChar = 0x0000;
    UConverterDataISO2022* myData=(UConverterDataISO2022*)(args->converter->extraInfo);
    CompactShortArray *myToUnicodeDBCS=NULL, *myToUnicodeFallbackDBCS = NULL; 
    UChar *myToUnicodeSBCS = NULL, *myToUnicodeFallbackSBCS = NULL;
    
    /*Arguments Check*/
    if (U_FAILURE(*err)) 
        return;
    
    if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    
    while(mySource< args->sourceLimit){
        
        if(myTarget < args->targetLimit){
            
            mySourceChar= (unsigned char) *mySource++;
            
            if(args->converter->mode==UCNV_SI){
                    
               /* if there	are no escape sequences in the first buffer then they
                * are assumed to be ASCII according to RFC-1554
                */    
                myData->toUnicodeCurrentState = ASCII1;
             }
            
            switch(mySourceChar){
                case 0x0A:
                    if(args->converter->toUnicodeStatus !=	0x00){
                        goto SAVE_STATE;
                    }
                     myData->toUnicodeCurrentState = ASCII1; 
                    break;
                
                case 0x0D:
                    if(args->converter->toUnicodeStatus !=	0x00){
                        goto SAVE_STATE;
                    }
                    myData->toUnicodeCurrentState = ASCII1;

                    break;

                case 0x20:
                    if(args->converter->toUnicodeStatus !=	0x00){
                        goto SAVE_STATE;
                    }
                    myData->toUnicodeCurrentState = ASCII1; 
                    break;
                            
                default:
                    /* if we are in the middle of consuming an escape sequence 
                     * we continue to next switch tag else we break
                     */
                    if(myData->key==0){
                        break;
                    }

                case ESC_2022:
                    if(args->converter->toUnicodeStatus != 0x00){
                        goto SAVE_STATE;
                    }
                    mySource--;
                    changeState_2022_JP(args->converter,&(mySource), 
                        args->sourceLimit, args->flush,err);
                    if(U_FAILURE(*err)){
                        goto SAVE_STATE;
                    }
                    continue;
            }
            
            switch(myConverterType[myData->toUnicodeCurrentState]){
                
                 case ASCII1:
                    if(args->converter->toUnicodeStatus == 0x00 && mySourceChar < 0x7F){
                        targetUniChar = (UChar)	 mySourceChar;
                    }
                    else{
                        goto SAVE_STATE;
                    }
                    break;
                case SBCS:
                    if(args->converter->toUnicodeStatus == 0x00){
                        myToUnicodeSBCS	= myData->currentConverter->sharedData->table->sbcs.toUnicode;
                        myToUnicodeFallbackSBCS = myData->currentConverter->sharedData->table->sbcs.toUnicodeFallback;
                        targetUniChar = myToUnicodeSBCS[(unsigned char) mySourceChar];
                        if(targetUniChar> 0xfffe){
                            if((args->converter->useFallback == TRUE) && 
                                (myData->currentConverter->sharedData->staticData->hasFromUnicodeFallback == TRUE)){
                            
                                targetUniChar =	myToUnicodeFallbackSBCS[(unsigned char) mySource[mySourceIndex-1]];
                            }
                        }
                        
                    }
                    else{
                        goto SAVE_STATE;
                    }
                    break;
                
                case DBCS:
                    myToUnicodeDBCS	= &myData->currentConverter->sharedData->table->dbcs.toUnicode;
                    myToUnicodeFallbackDBCS = &myData->currentConverter->sharedData->table->dbcs.toUnicodeFallback;
                
                    if(args->converter->toUnicodeStatus == 0x00){
                        args->converter->toUnicodeStatus = (UChar) mySourceChar;
                        continue;
                    }
                    else{
                        tempBuf[0] =	(char) args->converter->toUnicodeStatus ;
                        tempBuf[1] =	(char) mySourceChar;
                        mySourceChar= (UChar)((args->converter->toUnicodeStatus << 8) | (mySourceChar & 0x00ff));
                        args->converter->toUnicodeStatus =0x00;
                    
                        targetUniChar = ucmp16_getu(myToUnicodeDBCS,mySourceChar);
                        if(targetUniChar> 0xfffe){
                            if((args->converter->useFallback == TRUE) && 
                                (myData->currentConverter->sharedData->staticData->hasFromUnicodeFallback == TRUE)){
                            
                                targetUniChar =	(UChar) ucmp16_getu(myToUnicodeFallbackDBCS, mySourceChar);
                            }
                        }
                    }
                
                    break;
                
                case MBCS:
                
                    if(args->converter->toUnicodeStatus == 0x00){
                        args->converter->toUnicodeStatus = (UChar) mySourceChar;
                        continue;
                    }
                    else{
                        tempBuf[0] = (char) (args->converter->toUnicodeStatus);
                        tempBuf[1] = (char) (mySourceChar);
                        args->converter->toUnicodeStatus = 0x00;
                        mySourceChar= (UChar)((args->converter->toUnicodeStatus << 8) | (mySourceChar & 0x00ff));
                        pBuf = &tempBuf[0];
                        tempLimit = &tempBuf[2]+1;
                        targetUniChar	= _MBCSSimpleGetNextUChar(myData->currentConverter->sharedData,
                            &pBuf,tempLimit,args->converter->useFallback);
                    }
                    break;
                    
                case LATIN1:
                    if(args->converter->fromUnicodeStatus == 0x00 && mySourceChar < 0x100){
                        targetUniChar = (UChar)	 mySourceChar;
                    }
                    else{
                        goto SAVE_STATE;
                    }
                    break;
                
                case INVALID_STATE:
                    *err = U_ILLEGAL_ESCAPE_SEQUENCE;
                    goto SAVE_STATE;

            }
            if(targetUniChar < 0xfffe){
                if(myConverterType[myData->toUnicodeCurrentState] == SBCS || 
                    myConverterType[myData->toUnicodeCurrentState]== ASCII1 ||
                    myConverterType[myData->toUnicodeCurrentState]== LATIN1){

                    args->offsets[myTarget - args->target]=	mySource - args->source -1;
                }
                else{
                    args->offsets[myTarget - args->target]=	mySource - args->source - 2;
                }
                *(myTarget++)=(UChar)targetUniChar;
                targetUniChar=missingCharMarker;
            }
            else if(targetUniChar>=0xfffe){
SAVE_STATE:
                {
                    const char *saveSource = args->source;
                    UChar *saveTarget = args->target;

                    int32_t *saveOffsets = args->offsets;
                    
                    UConverterCallbackReason reason;
                    int32_t currentOffset ;
                    int32_t My_i = myTarget - args->target;
                    if(myConverterType[myData->toUnicodeCurrentState] == SBCS || 
                        myConverterType[myData->toUnicodeCurrentState]== ASCII1 ||
                        myConverterType[myData->toUnicodeCurrentState]== LATIN1){

                        currentOffset=	mySource - args->source -1;
                        args->converter->invalidCharBuffer[args->converter->invalidCharLength++] =(char) mySourceChar;
                    }
                    else{
                        currentOffset=	mySource - args->source - 2;
                        args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = tempBuf[0];
                        args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = tempBuf[1];
    
                    }
                    
                    
                    if(targetUniChar == 0xfffe){
                        reason = UCNV_UNASSIGNED;
                        *err = U_INVALID_CHAR_FOUND;
                    }
                    else{
                        reason = UCNV_ILLEGAL;
                        *err = U_ILLEGAL_CHAR_FOUND;
                    }

                   
                    args->offsets = args->offsets?args->offsets+(myTarget - args->target):0;
                    args->target =myTarget;
                    myTarget =saveTarget;
                    args->source = mySource;
                    ToU_CALLBACK_OFFSETS_LOGIC_MACRO( args->converter->toUContext,
                        args,
                        args->converter->invalidCharBuffer,
                        args->converter->invalidCharLength,
                        reason,
                        err);
                    args->converter->invalidCharLength=0;
                    myTarget=args->target;
                    args->source  = saveSource;
                    args->target  = saveTarget;
                    /*args->offsets = saveOffsets;*/
                    if(U_FAILURE(*err))
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
        if(U_SUCCESS(*err)){
            *err = U_TRUNCATED_CHAR_FOUND;
            args->converter->toUnicodeStatus = 0x00;
        }
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
const char* getEndOfBuffer_2022_KR(UConverterToUnicodeArgs* args, UErrorCode* err);


U_CFUNC void UConverter_fromUnicode_ISO_2022_KR(UConverterFromUnicodeArgs* args, UErrorCode* err){
    
    const UChar *mySource = args->source;
    unsigned char *myTarget = (unsigned char *) args->target;
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    int32_t targetLength = args->targetLimit - args->target;
    int32_t sourceLength = args->sourceLimit - args->source;
    int32_t length=0;
    UChar32 targetUniChar = 0x0000;
    UChar32 mySourceChar = 0x0000;
    UBool isTargetUCharDBCS = (UBool)args->converter->fromUnicodeStatus;
    UBool oldIsTargetUCharDBCS = isTargetUCharDBCS;
    UConverterDataISO2022 *myConverterData=(UConverterDataISO2022*)args->converter->extraInfo;
    UConverterCallbackReason reason;
    
    isTargetUCharDBCS   = (UBool) args->converter->fromUnicodeStatus;
    
    if(args->converter->fromUSurrogateLead!=0 && myTargetIndex < targetLength) {
        goto getTrail;
    }
    /*writing the char to the output stream */
    while (mySourceIndex < sourceLength){
        
        if (myTargetIndex < targetLength){
            
            mySourceChar = (UChar) args->source[mySourceIndex++];
            
            /*Handle surrogates */
            if(UTF_IS_SURROGATE(mySourceChar)) {
                if(UTF_IS_SURROGATE_FIRST(mySourceChar)) {
                    args->converter->fromUSurrogateLead=(UChar)mySourceChar;
getTrail:
                    /*look ahead to find the trail surrogate*/
                    if(mySourceIndex <  sourceLength) {
                        /* test the following code unit */
                        UChar trail=(UChar) args->source[mySourceIndex];
                        if(UTF_IS_SECOND_SURROGATE(trail)) {
                            ++mySourceIndex;
                            mySourceChar=UTF16_GET_PAIR_VALUE(mySourceChar, trail);
                            isTargetUCharDBCS=TRUE;
                            args->converter->fromUSurrogateLead=0x00;
                            /* convert this surrogate code point */
                            /* exit this condition tree */
                        } else {
                            /* this is an unmatched lead code unit (1st surrogate) */
                            /* callback(illegal) */
                            reason=UCNV_ILLEGAL;
                            *err=U_ILLEGAL_CHAR_FOUND;
                            goto CALLBACK;
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
                    goto CALLBACK;
                }
            }
            length= _MBCSFromUChar32(myConverterData->fromUnicodeConverter->sharedData,
                mySourceChar,&targetUniChar,args->converter->useFallback);
           
            
            /* only DBCS or SBCS characters are expected*/
            if(length > 2 || length==0){
                reason =UCNV_ILLEGAL;
                *err =U_INVALID_CHAR_FOUND;
                goto CALLBACK;
            }
            /* DB haracters with high bit set to 1 are expected */
            if(((targetUniChar & 0x8080) != 0x8080)&& length==2){
                reason =UCNV_ILLEGAL;
                *err =U_INVALID_CHAR_FOUND;
                goto CALLBACK;
            }
            
            oldIsTargetUCharDBCS = isTargetUCharDBCS;
            isTargetUCharDBCS = (UBool)(targetUniChar>0x00FF);
            
            if (targetUniChar != missingCharMarker){
                
                if (oldIsTargetUCharDBCS != isTargetUCharDBCS || !myConverterData->isShiftAppended){
                    
                    if (isTargetUCharDBCS) 
                        args->target[myTargetIndex++] = UCNV_SO;
                    else 
                        args->target[myTargetIndex++] = UCNV_SI;

                    myConverterData->isShiftAppended=TRUE;

                    if ((!isTargetUCharDBCS)&&(myTargetIndex+1 >= targetLength)){
                        args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (char) targetUniChar;
                        *err = U_BUFFER_OVERFLOW_ERROR;
                        break;
                    }
                    else if (myTargetIndex+1 >= targetLength){
                        
                        args->converter->charErrorBuffer[0] =	(char) ((targetUniChar >> 8) -0x80);
                        args->converter->charErrorBuffer[1] =	(char)((targetUniChar & 0x00FF) -0x80);
                        args->converter->charErrorBufferLength = 2;
                        *err = U_BUFFER_OVERFLOW_ERROR;
                        break;
                    }
                    
                }
                
                if (!isTargetUCharDBCS){
                    
                    args->target[myTargetIndex++]	= (char) targetUniChar;
                }
                else{
                    args->target[myTargetIndex++]	= (char) ((targetUniChar >> 8) - 0x80);
                    if (myTargetIndex	< targetLength){
                        args->target[myTargetIndex++] = (char)((targetUniChar & 0x00FF) -0x80);
                    }
                    else{
                        args->converter->charErrorBuffer[0] =	(char)((targetUniChar & 0x00FF) -0x80);
                        args->converter->charErrorBufferLength = 1;
                        *err = U_BUFFER_OVERFLOW_ERROR;
                        break;
                    }
                }
            }
            else{
                
CALLBACK:
                {
                    const	UChar* saveSource = args->source;
                    char*	saveTarget = args->target;
                    int32_t *saveOffsets = args->offsets;
                
                    isTargetUCharDBCS	= oldIsTargetUCharDBCS;
                    *err = U_INVALID_CHAR_FOUND;
                    args->converter->invalidUCharBuffer[0] = (UChar) mySourceChar;
                    args->converter->invalidUCharLength = 1;
                
                    args->converter->fromUnicodeStatus = (int32_t)isTargetUCharDBCS;
                    args->target += myTargetIndex;
                    args->source += mySourceIndex;
                    FromU_CALLBACK_MACRO(args->converter->fromUContext,
                        args,
                        args->converter->invalidUCharBuffer,
                        1,
                        (UChar32) mySourceChar,
                        UCNV_UNASSIGNED,
                        err);
                    args->source = saveSource;
                    args->target = saveTarget;
                    args->offsets = saveOffsets;
                    args->converter->invalidUCharLength = 0;
                    isTargetUCharDBCS=(UBool)args->converter->fromUnicodeStatus;
                    myConverterData->isShiftAppended =FALSE;
                    args->converter->fromUSurrogateLead=0x00;
                    if (U_FAILURE (*err)) 
                        break;
                
                }
            }
            targetUniChar=missingCharMarker;
        }
        else{
            *err = U_BUFFER_OVERFLOW_ERROR;
            break;
        }
        
    }
    /*If at the end of conversion we are still carrying state information
    *flush is TRUE, we can deduce that the input stream is truncated
    */
    if (args->converter->fromUSurrogateLead !=0 && (mySourceIndex == sourceLength) && args->flush){
        if (U_SUCCESS(*err)){
            *err = U_TRUNCATED_CHAR_FOUND;
            args->converter->toUnicodeStatus = 0x00;
        }
    }

    args->target += myTargetIndex;
    args->source += mySourceIndex;
    args->converter->fromUnicodeStatus = (int32_t)isTargetUCharDBCS;
    
    return;
}


U_CFUNC void UConverter_fromUnicode_ISO_2022_KR_OFFSETS_LOGIC(UConverterFromUnicodeArgs* args, UErrorCode* err){
    
    const UChar *mySource = args->source;
    unsigned char *myTarget = (unsigned char *) args->target;
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    int32_t targetLength = args->targetLimit - args->target;
    int32_t sourceLength = args->sourceLimit - args->source;
    UChar32 targetUniChar = 0x0000;
    UChar32 mySourceChar = 0x0000;
    UBool isTargetUCharDBCS = (UBool)args->converter->fromUnicodeStatus;
    UBool oldIsTargetUCharDBCS = isTargetUCharDBCS;
    UConverterDataISO2022 *myConverterData=(UConverterDataISO2022*)args->converter->extraInfo;
    UConverterCallbackReason reason;
    int32_t length =0;
    
    isTargetUCharDBCS   = (UBool) args->converter->fromUnicodeStatus;
    if(args->converter->fromUSurrogateLead!=0 && myTargetIndex <targetLength) {
        goto getTrail;
    }
    /*writing the char to the output stream */
    while (mySourceIndex < sourceLength){
        
        if (myTargetIndex < targetLength){
            
            mySourceChar = (UChar) args->source[mySourceIndex++];
            
            /*Handle surrogates */
              if(UTF_IS_SURROGATE(mySourceChar)) {
                if(UTF_IS_SURROGATE_FIRST(mySourceChar)) {
                    args->converter->fromUSurrogateLead=(UChar) mySourceChar;
getTrail:
                    /*look ahead to find the trail surrogate*/
                    if(mySourceIndex <  sourceLength) {
                        /* test the following code unit */
                        UChar trail=(UChar) args->source[mySourceIndex];
                        if(UTF_IS_SECOND_SURROGATE(trail)) {
                            ++mySourceIndex;
                            mySourceChar=UTF16_GET_PAIR_VALUE(mySourceChar, trail);
                            isTargetUCharDBCS=TRUE;
                            args->converter->fromUSurrogateLead=0x00;
                            /* convert this surrogate code point */
                            /* exit this condition tree */
                        } else {
                            /* this is an unmatched lead code unit (1st surrogate) */
                            /* callback(illegal) */
                            reason=UCNV_ILLEGAL;
                            *err=U_ILLEGAL_CHAR_FOUND;
                            goto CALLBACK;
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
                    goto CALLBACK;
                }
            }
            
            length= _MBCSFromUChar32(myConverterData->fromUnicodeConverter->sharedData,
                mySourceChar,&targetUniChar,args->converter->useFallback);
            
            /* only DBCS or SBCS characters are expected*/
            if(length > 2 || length==0){
                reason =UCNV_ILLEGAL;
                *err =U_INVALID_CHAR_FOUND;
                goto CALLBACK;
            }
            /* DB haracters with high bit set to 1 are expected */
            if(((targetUniChar & 0x8080) != 0x8080)&& length==2){
                reason =UCNV_ILLEGAL;
                *err =U_INVALID_CHAR_FOUND;
                goto CALLBACK;
            }

            oldIsTargetUCharDBCS = isTargetUCharDBCS;
            isTargetUCharDBCS = (UBool)(targetUniChar>0x00FF);
            if (targetUniChar != missingCharMarker){
                
                if (oldIsTargetUCharDBCS != isTargetUCharDBCS || !myConverterData->isShiftAppended){
                    
                    args->offsets[myTargetIndex] = mySourceIndex-1;

                    if (isTargetUCharDBCS) 
                        args->target[myTargetIndex++] = UCNV_SO;
                    else
                        args->target[myTargetIndex++] = UCNV_SI;

                    myConverterData->isShiftAppended=TRUE;
                    if ((!isTargetUCharDBCS)&&(myTargetIndex+1 >= targetLength)){
                        
                        args->converter->charErrorBuffer[0]	= (char) targetUniChar;
                        args->converter->charErrorBufferLength = 1;
                        *err = U_BUFFER_OVERFLOW_ERROR;
                        break;
                    }
                    else if (myTargetIndex+1 >=	targetLength){
                        
                        args->converter->charErrorBuffer[0]	= (char) ((targetUniChar >> 8)-0x80);
                        args->converter->charErrorBuffer[1]	= (char) ((targetUniChar & 0x00FF)-0x80);
                        args->converter->charErrorBufferLength = 2;
                        *err = U_BUFFER_OVERFLOW_ERROR;
                        break;
                    }
                }
                
                if (!isTargetUCharDBCS){
                    
                    args->offsets[myTargetIndex] = mySourceIndex-1;
                    args->target[myTargetIndex++] = (char) targetUniChar;
                }
                else{
                    args->offsets[myTargetIndex] = mySourceIndex-1;
                    args->target[myTargetIndex++] = (char) ((targetUniChar >> 8)-0x80);
                    
                    if (myTargetIndex < targetLength){
                        args->offsets[myTargetIndex] = mySourceIndex-1;
                        args->target[myTargetIndex++] = (char)((targetUniChar & 0x00FF)-0x80);
                    }
                    else{
                        args->converter->charErrorBuffer[0]	=(char) ((targetUniChar & 0x00FF)-0x80);
                        args->converter->charErrorBufferLength = 1;
                        *err = U_BUFFER_OVERFLOW_ERROR;
                        break;
                    }
                }
            }
            else{
CALLBACK:	    
                {
                    int32_t currentOffset = args->offsets[myTargetIndex-1]+1;
                    char * saveTarget = args->target;
                    const UChar* saveSource = args->source;
                    int32_t *saveOffsets = args->offsets;
                    *err = U_INVALID_CHAR_FOUND;
                    args->converter->invalidUCharBuffer[0] = (UChar) mySourceChar;
                    args->converter->invalidUCharLength = 1;
                
                    /* Breaks out of the loop since behaviour was set to stop */
                    args->converter->fromUnicodeStatus = (int32_t)isTargetUCharDBCS;
                    args->target +=	myTargetIndex;
                    args->source +=	mySourceIndex;
                    args->offsets =	args->offsets?args->offsets+myTargetIndex:0;
                    FromU_CALLBACK_OFFSETS_LOGIC_MACRO(args->converter->fromUContext,
                        args,
                        args->converter->invalidUCharBuffer,
                        1,
                        (UChar32)mySourceChar,
                        UCNV_UNASSIGNED,
                        err);
                    isTargetUCharDBCS=(UBool)args->converter->fromUnicodeStatus;
                    myConverterData->isShiftAppended =FALSE;
                    args->source = saveSource;
                    args->target = saveTarget;
                    args->offsets =	saveOffsets;
                    args->converter->invalidUCharLength = 0;
                    args->converter->fromUSurrogateLead=0x00;
                    if (U_FAILURE (*err))     
                        break;

                }
            }
            targetUniChar=missingCharMarker;
        }
        else{
            
            *err = U_BUFFER_OVERFLOW_ERROR;
            break;
        }
        
    }
    
    /*If at the end of conversion we are still carrying state information
    *flush is TRUE, we can deduce that the input stream is truncated
    */
    if (args->converter->fromUSurrogateLead !=0 && (mySourceIndex == sourceLength) && args->flush){
        if (U_SUCCESS(*err)){
            *err = U_TRUNCATED_CHAR_FOUND;
            args->converter->toUnicodeStatus = 0x00;
        }
    }

    args->target += myTargetIndex;
    args->source += mySourceIndex;
    
    
}

/************************ To Unicode ***************************************/

U_CFUNC void UConverter_toUnicode_ISO_2022_KR(UConverterToUnicodeArgs *args,
                                              UErrorCode* err){
    char tempBuf[3];
    const char* pBuf;
    const char *mySource = ( char *) args->source;
    UChar *myTarget = args->target;
    char *tempLimit = &tempBuf[2]+1; 
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    const char *mySourceLimit = args->sourceLimit;
    UChar32 targetUniChar = 0x0000;
    UChar mySourceChar = 0x0000;
    UConverterDataISO2022* myData=(UConverterDataISO2022*)(args->converter->extraInfo);
    
    
    /*Arguments Check*/
    if (U_FAILURE(*err)) 
        return;
    
    if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    
    while(mySource< args->sourceLimit){
        
        if(myTarget < args->targetLimit){
            
            mySourceChar= (unsigned char) *mySource++;

            switch(mySourceChar){
            
                case UCNV_SI:
                    myData->toUnicodeCurrentState = SBCS;
                    continue;
                case UCNV_SO:
                    myData->toUnicodeCurrentState =DBCS;
                    /*consume the source */
                    continue;
                       
                default: 
                    if(myData->key==0){
                        break;
                    }
                case ESC_2022:
                {
                    /* Already doing some conversion and found escape Sequence*/
                    if(args->converter->mode ==	UCNV_SO){
                        *err = U_ILLEGAL_ESCAPE_SEQUENCE;
                        goto SAVE_STATE;
                    }
                    else{
                        mySource--;
                        changeState_2022(args->converter,
                                            &mySource, 
                                            args->sourceLimit,
                                            args->flush,
                                            err);
                       
                    }
                    if(U_FAILURE(*err)){
                       *err = U_ILLEGAL_ESCAPE_SEQUENCE;
                        goto SAVE_STATE;
                    }
                    continue;
                }
            }
             
            if(myData->toUnicodeCurrentState==DBCS){
                if(args->converter->toUnicodeStatus == 0x00){
                    args->converter->toUnicodeStatus = (UChar) mySourceChar;
                    continue;
                }
                else{
                    tempBuf[0] =	(char) (args->converter->toUnicodeStatus+0x80) ;
                    tempBuf[1] =	(char) (mySourceChar+0x80);
                    mySourceChar= (UChar)(((args->converter->toUnicodeStatus << 8)+0x80) | ((mySourceChar & 0x00ff)+0x80));
                    args->converter->toUnicodeStatus =0x00;
                    pBuf = &tempBuf[0];
                    tempLimit = &tempBuf[2]+1;
                    targetUniChar = _MBCSSimpleGetNextUChar(myData->fromUnicodeConverter->sharedData,
                        &pBuf,tempLimit,args->converter->useFallback);
                }
            }
            else{
                if(args->converter->fromUnicodeStatus == 0x00){
                    tempBuf[0] = (char) mySourceChar;
                    pBuf = &tempBuf[0];
                    tempLimit = &tempBuf[1];
                    targetUniChar = _MBCSSimpleGetNextUChar(myData->fromUnicodeConverter->sharedData,
                        &pBuf,tempLimit,args->converter->useFallback);
                }
                else{
                    goto SAVE_STATE;
                }

            }
            if(targetUniChar < 0xfffe){
                *(myTarget++)=(UChar)targetUniChar;
            }
            else if(targetUniChar>=0xfffe){
SAVE_STATE:
                {
                    const char *saveSource = args->source;
                    UChar *saveTarget = args->target;
                    int32_t *saveOffsets = args->offsets;
                    UConverterCallbackReason reason;
                
                    if(targetUniChar == 0xfffe){
                        reason = UCNV_UNASSIGNED;
                        *err = U_INVALID_CHAR_FOUND;
                    }
                    else{
                        reason = UCNV_ILLEGAL;
                        *err = U_ILLEGAL_CHAR_FOUND;
                    }
                    args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = (char)(tempBuf[0]-0x80);
                    args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = (char)(tempBuf[1]-0x80);
                    args->target = myTarget;
                    args->source = mySource;
                    ToU_CALLBACK_MACRO( args->converter->toUContext,
                        args,
                        args->converter->invalidCharBuffer,
                        args->converter->invalidCharLength,
                        reason,
                        err);
                    args->source  =	saveSource;
                    myTarget = args->target;
                    args->target  =	saveTarget;
                    args->offsets =	saveOffsets;
                    args->converter->invalidCharLength=0;
                    if(U_FAILURE(*err))
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
        if(U_SUCCESS(*err)){
            *err = U_TRUNCATED_CHAR_FOUND;
            args->converter->toUnicodeStatus = 0x00;
        }
    }
    args->target = myTarget;
    args->source = mySource;
}

U_CFUNC void UConverter_toUnicode_ISO_2022_KR_OFFSETS_LOGIC(UConverterToUnicodeArgs *args,
                                                            UErrorCode* err){
    char tempBuf[3];
    const char* pBuf;
    const char *mySource = ( char *) args->source;
    UChar *myTarget = args->target;
    char *tempLimit = &tempBuf[2]+1; 
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    const char *mySourceLimit = args->sourceLimit;
    UChar32 targetUniChar = 0x0000;
    UChar mySourceChar = 0x0000;
    UConverterDataISO2022* myData=(UConverterDataISO2022*)(args->converter->extraInfo);
    
    
    /*Arguments Check*/
    if (U_FAILURE(*err)) 
        return;
    
    if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    
    while(mySource< args->sourceLimit){
        
        if(myTarget < args->targetLimit){
            
            mySourceChar= (unsigned char) *mySource++;
            
            switch(mySourceChar){
            
                case UCNV_SI:
                    myData->toUnicodeCurrentState = SBCS;
                    continue;
                case UCNV_SO:
                    myData->toUnicodeCurrentState =DBCS;
                    /*consume the source */
                    continue;
                       
                default:
                    /* If we are in the process of consuming an escape sequence 
                     * we fall through execute the the statements of next switch 
                     * tag else we break;
                     */
                    if(myData->key==0){
                        break;
                    }
                case ESC_2022:
                {
                    /* Already doing some conversion and found escape Sequence*/
                    if(args->converter->mode ==	UCNV_SO){
                        *err = U_ILLEGAL_ESCAPE_SEQUENCE;
                        goto SAVE_STATE;
                    }
                    else{
                        mySource--;
                        changeState_2022(args->converter,
                                            &mySource, 
                                            args->sourceLimit,
                                            args->flush,
                                            err);
                       
                    }
                    if(U_FAILURE(*err)){
                       *err = U_ILLEGAL_ESCAPE_SEQUENCE;
                        goto SAVE_STATE;
                    }
                    continue;
                }
            }
             
            if(myData->toUnicodeCurrentState==DBCS){
                if(args->converter->toUnicodeStatus == 0x00){
                    args->converter->toUnicodeStatus = (UChar) mySourceChar;
                    continue;
                }
                else{
                    tempBuf[0] =	(char) (args->converter->toUnicodeStatus+0x80) ;
                    tempBuf[1] =	(char) (mySourceChar+0x80);
                    mySourceChar= (UChar)(((args->converter->toUnicodeStatus+0x80) << 8) | ((mySourceChar & 0x00ff)+0x80));
                    args->converter->toUnicodeStatus =0x00;
                    pBuf = &tempBuf[0];
                    tempLimit = &tempBuf[2]+1;
                    targetUniChar = _MBCSSimpleGetNextUChar(myData->fromUnicodeConverter->sharedData,
                        &pBuf,tempLimit,args->converter->useFallback);
                }
            }
            else{
                if(args->converter->fromUnicodeStatus == 0x00){
                    tempBuf[0] = (char) mySourceChar;
                    pBuf = &tempBuf[0];
                    tempLimit = &tempBuf[1];
                    targetUniChar = _MBCSSimpleGetNextUChar(myData->currentConverter->sharedData,
                        &pBuf,tempLimit,args->converter->useFallback);
                }
                else{
                    goto SAVE_STATE;
                }

            }
            if(targetUniChar < 0xfffe){
                 if(myData->toUnicodeCurrentState==DBCS){
                    args->offsets[myTarget - args->target]=	mySource - args->source - 2;
                }
                else{
                    args->offsets[myTarget - args->target]=	mySource - args->source - 1;
                }
                *(myTarget++)=(UChar)targetUniChar;
            }
            else if(targetUniChar>=0xfffe){
SAVE_STATE:
                {
                    const char *saveSource = args->source;
                    UChar *saveTarget = args->target;
                    int32_t *saveOffsets = args->offsets;
                    
                    UConverterCallbackReason reason;
                    int32_t currentOffset ;
                    int32_t My_i = myTarget - args->target;
                    
                    if(targetUniChar == 0xfffe){
                        reason = UCNV_UNASSIGNED;
                        *err = U_INVALID_CHAR_FOUND;
                    }
                    else{
                        reason = UCNV_ILLEGAL;
                        *err = U_ILLEGAL_CHAR_FOUND;
                    }
                    if(myData->toUnicodeCurrentState== DBCS){

                        args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = (char)(tempBuf[0]-0x80);
                        args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = (char)(tempBuf[1]-0x80);
                        currentOffset=	mySource - args->source -2;
                    
                    }
                    else{
                        args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = (char)mySourceChar;
                        currentOffset=	mySource - args->source -1;
                    }
                    args->offsets = args->offsets?args->offsets+(myTarget - args->target):0;
                    args->target = myTarget;
                    args->source = mySource;
                    myTarget = saveTarget;
                    ToU_CALLBACK_OFFSETS_LOGIC_MACRO( args->converter->toUContext,
                        args,
                        args->converter->invalidCharBuffer,
                        args->converter->invalidCharLength,
                        reason,
                        err);
                    args->converter->invalidCharLength=0;
                    args->source  =	saveSource;
                    myTarget = args->target;
                    args->target  =	saveTarget;
                    args->offsets =	saveOffsets;
                    if(U_FAILURE(*err))
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
        if(U_SUCCESS(*err)){
            *err = U_TRUNCATED_CHAR_FOUND;
            args->converter->toUnicodeStatus = 0x00;
        }
    }
    args->target = myTarget;
    args->source = mySource;
}

/*
* This is a simple, interim implementation of GetNextUChar()
* that allows to concentrate on testing one single implementation
* of the ToUnicode conversion before it gets copied to
* multiple version that are then optimized for their needs
* (with vs. without offsets and getNextUChar).
*/

U_CFUNC UChar32
UConverter_getNextUChar_ISO_2022_KR(UConverterToUnicodeArgs *pArgs,
                                    UErrorCode *pErrorCode) {
    UChar buffer[UTF_MAX_CHAR_LENGTH];
    const char *realLimit=pArgs->sourceLimit;
    
    pArgs->target=buffer;
    pArgs->targetLimit=buffer+UTF_MAX_CHAR_LENGTH;
    
    while(pArgs->source<realLimit) {
        /* feed in one byte at a time to make sure to get only one character out */
        pArgs->sourceLimit=pArgs->source+1;
        pArgs->flush= (UBool)(pArgs->sourceLimit==realLimit);
        UConverter_toUnicode_ISO_2022_KR(pArgs, pErrorCode);
        if(U_FAILURE(*pErrorCode) && *pErrorCode!=U_BUFFER_OVERFLOW_ERROR) {
            return 0xffff;
        } else if(pArgs->target!=buffer) {
            if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR) {
                *pErrorCode=U_ZERO_ERROR;
            }
            return ucnv_getUChar32KeepOverflow(pArgs->converter, buffer, pArgs->target-buffer);
        }
    }
    
    /* no output because of empty input or only state changes and skipping callbacks */
    *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
    return 0xffff;
}

/*************************** END ISO2022-KR *********************************/



/*************************** ISO-2022-CN *********************************
* 
* Rules for ISO-2022-CN Encoding:
* i)   The desinator sequence must appear once on a line before any instance
*      of character set it designates.
* ii)  If two lines contain characters from the same character set, both lines
*      must include the designator sequence.
* iii) Once the designator sequence is know, a shifting sequnce has to found
*      to invoke the  shifting
* iv)  All lines start in ASCII and end in ASCII.  
* v)   Four shifting sequences are employed for this purpose:
*
*      Sequcence   ASCII Eq    Charsets
*      ----------  -------    ---------     
*      SS2	        <ESC>N      CNS-11643-1992 Planes 3-7
*      SS3	        <ESC>O      CNS-11643-1992 Plane 2
*      SI	        <SI>        
*      SO	        <SO>        CNS-11643-1992 Plane 1, GB2312,ISO-IR-165
*
* vi)   
*      SOdesignator  : ESC "$" ")" finalchar_for_SO
*      SS2designator : ESC "$" "*" finalchar_for_SS2
*      SS3designator : ESC "$" "+" finalchar_for_SS3
*
*      ESC $ ) A       Indicates the bytes following SO are Chinese
*		characters as defined in GB 2312-80, until
*		another SOdesignation appears
*	            
*
*      ESC $ ) E       Indicates the bytes following SO are as defined
*		in ISO-IR-165 (for details, see section 2.1),
*		until another SOdesignation appears
*
*      ESC $ ) G       Indicates the bytes following SO are as defined
*		in CNS 11643-plane-1, until another
*		SOdesignation appears
*
*      ESC $ * H       Indicates the two bytes immediately following
*		SS2 is a Chinese character as defined in CNS
*		11643-plane-2, until another SS2designation
*		appears
*		(Meaning <ESC>N must preceed every 2 byte 
*		 sequence.)	  
*
*      ESC $ + I       Indicates the immediate two bytes following SS3
*		is a Chinese character as defined in CNS
*		11643-plane-3, until another SS3designation
*		appears
*		(Meaning <ESC>O must preceed every 2 byte 
*		 sequence.)	  
*
*      ESC $ + J       Indicates the immediate two bytes following SS3
*		is a Chinese character as defined in CNS
*		11643-plane-4, until another SS3designation
*		appears
*		(In English: <ESC>N must preceed every 2 byte 
*		 sequence.)	  
*
*      ESC $ + K       Indicates the immediate two bytes following SS3
*		is a Chinese character as defined in CNS
*		11643-plane-5, until another SS3designation
*		appears
*
*      ESC $ + L       Indicates the immediate two bytes following SS3
*		is a Chinese character as defined in CNS
*		11643-plane-6, until another SS3designation
*		appears
*
*      ESC $ + M       Indicates the immediate two bytes following SS3
*		is a Chinese character as defined in CNS
*		11643-plane-7, until another SS3designation
*		appears
*
*       As in ISO-2022-CN, each line starts in ASCII, and ends in ASCII, and
*       has its own designation information before any Chinese characters
*       appear
*
*/


/********************** ISO2022-CN Data **************************/
static const char* escSeqCharsCN[10] ={
        "\x0F",             /* ASCII */
        "\x1B\x24\x29\x41", /* GB 2312-80 */
        "\x1B\x24\x29\x45", /* ISO-IR-165 */
        "\x1B\x24\x29\x47", /* CNS 11643-1992 Plane 1 */
        "\x1B\x24\x2A\x48", /* CNS 11643-1992 Plane 2 */
        "\x1B\x24\x2B\x49", /* CNS 11643-1992 Plane 3 */
        "\x1B\x24\x2B\x4A", /* CNS 11643-1992 Plane 4 */
        "\x1B\x24\x2B\x4B", /* CNS 11643-1992 Plane 5 */
        "\x1B\x24\x2B\x4C", /* CNS 11643-1992 Plane 6 */
        "\x1B\x24\x2B\x4D"  /* CNS 11643-1992 Plane 7 */
};

static const char* shiftSeqCharsCN[10] ={
        "",
        (const char*) "\x0E",
        (const char*) "\x0E",
        (const char*) "\x0E",
        (const char*) UCNV_SS2,
        (const char*) UCNV_SS3,
        (const char*) UCNV_SS3,
        (const char*) UCNV_SS3,
        (const char*) UCNV_SS3,
        (const char*) UCNV_SS3
};

typedef enum  {
        ASCII_1=0,
        GB2312_1=1,
        ISO_IR_165=2,
        CNS_11643=3
} StateEnumCN;

static Cnv2022Type myConverterTypeCN[4]={
        ASCII1,
        DBCS,
        DBCS,
        MBCS
};


U_CFUNC void UConverter_fromUnicode_ISO_2022_CN(UConverterFromUnicodeArgs* args, UErrorCode* err){
    
    UChar* mySource =(UChar*)args->source;
    UConverterDataISO2022 *myConverterData=(UConverterDataISO2022*)args->converter->extraInfo;
    UConverterCallbackReason reason;
    UBool isEscapeAppended = FALSE;
    StateEnumCN initIterState;
    unsigned char *myTarget = (unsigned char *) args->target; 
    const UChar *saveSource;
    uint32_t targetValue=0;
    char *saveTarget;
    int32_t myTargetLength = args->targetLimit - args->target;
    int32_t mySourceLength = args->sourceLimit - args->source;
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    int32_t length  =0;
    int plane = 0;
    CompactShortArray *myFromUnicodeDBCS = NULL;
    CompactShortArray *myFromUnicodeDBCSFallback = NULL;
    CompactByteArray  *myFromUnicodeSBCS = NULL;
    CompactByteArray  *myFromUnicodeSBCSFallback = NULL;
    UChar32 targetUniChar = missingCharMarker;
    
    StateEnumCN currentState=ASCII;
    
    UChar32 mySourceChar = 0x0000;
    int iterCount = 0;
    const char *escSeq = NULL;
    UBool isShiftAppended = FALSE;

    isEscapeAppended =(UBool) myConverterData->isEscapeAppended;
    isShiftAppended =(UBool) myConverterData->isShiftAppended;
    initIterState = myConverterData->fromUnicodeCurrentState;
    /* arguments check*/
    if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    if(args->converter->fromUSurrogateLead!=0 && myTargetIndex < myTargetLength) {
        goto getTrail;
    }
    while(mySourceIndex <  mySourceLength){
        currentState =(StateEnumCN) myConverterData->fromUnicodeCurrentState;
        myConverterData->fromUnicodeConverter = (myConverterData->fromUnicodeConverter == NULL) ?
            myConverterData->myConverterArray[0] :
        myConverterData->myConverterArray[(int)myConverterData->fromUnicodeCurrentState];
        
        if(myTargetIndex < myTargetLength){
            
            mySourceChar = (UChar) args->source[mySourceIndex++];
            
            /* I am handling surrogates in the begining itself so that I donot have to go through 4
             * iterations on codepages that we support. 
             */
            if(UTF_IS_SURROGATE(mySourceChar)) {
                if(UTF_IS_SURROGATE_FIRST(mySourceChar)) {
                    args->converter->fromUSurrogateLead=(UChar)mySourceChar;
getTrail:
                    /*look ahead to find the trail surrogate*/
                    if(mySourceIndex <  mySourceLength) {
                        /* test the following code unit */
                        UChar trail=(UChar) args->source[mySourceIndex];
                        if(UTF_IS_SECOND_SURROGATE(trail)) {
                            ++mySourceIndex;
                            mySourceChar=UTF16_GET_PAIR_VALUE(mySourceChar, trail);
                            args->converter->fromUSurrogateLead=0x00;
                            /* convert this surrogate code point */
                            /* exit this condition tree */
                        } else {
                            /* this is an unmatched lead code unit (1st surrogate) */
                            /* callback(illegal) */
                            reason=UCNV_ILLEGAL;
                            *err=U_ILLEGAL_CHAR_FOUND;
                            goto CALLBACK;
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
                    goto CALLBACK;
                }
            }
            /* if the source character is CR or LF then append the ASCII escape sequence*/
            if(mySourceChar== 0x000A || mySourceChar== 0x000D){
                
                if((int)currentState > 0 && isShiftAppended){
                    concatChar(args, &myTargetIndex, &myTargetLength, UCNV_SI,err,&mySourceIndex);
                    isShiftAppended=myConverterData->isShiftAppended=FALSE;
                    TEST_ERROR_CONDITION_CN(args,myTargetIndex, mySourceIndex,myConverterData, err);
                }
                myConverterData->isEscapeAppended=isEscapeAppended=FALSE;
                targetUniChar = mySourceChar;
                concatString(args, &myTargetIndex, &myTargetLength,&targetUniChar,err,&mySourceIndex);
                TEST_ERROR_CONDITION_CN(args,myTargetIndex,	mySourceIndex,myConverterData, err);
                
                continue;
            }
            else{
                do{
                    switch (myConverterTypeCN[currentState]){
                    
                        case SBCS:
                            if(mySourceChar<0xffff){
                                myFromUnicodeSBCS =	&myConverterData->fromUnicodeConverter->sharedData->table->sbcs.fromUnicode;
                                myFromUnicodeSBCSFallback = &myConverterData->fromUnicodeConverter->sharedData->table->sbcs.fromUnicodeFallback;
                        
                                targetUniChar = (UChar32) ucmp8_getu (myFromUnicodeSBCS, mySourceChar);
                        
                                if ((targetUniChar==0)&&(args->converter->useFallback == TRUE) &&
                                    (myConverterData->fromUnicodeConverter->sharedData->staticData->hasFromUnicodeFallback == TRUE)){
                                    targetUniChar = (UChar32) ucmp8_getu (myFromUnicodeSBCSFallback, mySourceChar);
                                }
                        
                            }
                            /* ucmp8_getU returns 0	for missing char so explicitly set it missingCharMarker*/
                            targetUniChar=(UChar)((targetUniChar==0) ? (UChar) missingCharMarker : targetUniChar);
                            break;
                    
                        case DBCS:
                            if(mySourceChar<0xffff){
                                myFromUnicodeDBCS =	&myConverterData->fromUnicodeConverter->sharedData->table->dbcs.fromUnicode;
                                myFromUnicodeDBCSFallback = &myConverterData->fromUnicodeConverter->sharedData->table->dbcs.fromUnicodeFallback;
                                targetUniChar = (UChar) ucmp16_getu (myFromUnicodeDBCS,	mySourceChar);
                        
                                if ((targetUniChar==missingCharMarker)&&(args->converter->useFallback	== TRUE) &&
                                    (myConverterData->fromUnicodeConverter->sharedData->staticData->hasFromUnicodeFallback == TRUE)){
                                    targetUniChar = (UChar) ucmp16_getu (myFromUnicodeDBCSFallback, mySourceChar);
                                }
                            }
                            if(( myConverterData->version) == 0 && currentState==ISO_IR_165){
                                targetUniChar=missingCharMarker;
                            }
                            break;
                    
                        case MBCS:
                    
                            length= _MBCSFromUChar32(myConverterData->fromUnicodeConverter->sharedData,
                                mySourceChar,&targetValue,args->converter->useFallback);
                    
                            targetUniChar = (UChar32) targetValue;
                    
                            if(length==0){
                                targetUniChar = missingCharMarker;
                            } 
                            else if(length==3){
                                uint8_t	planeVal = (uint8_t) ((targetValue)>>16);
                                if(planeVal >0x80 && planeVal<0x89){
                                    plane = (int)(planeVal - 0x80);
                                    targetUniChar -= (planeVal<<16);
                                }else 
                                    plane =-1;
                            }
                            else if(length >3){
                                reason =UCNV_ILLEGAL;
                                *err =U_INVALID_CHAR_FOUND;
                                goto CALLBACK;
                            }
                            if(myConverterData->version == 0 && plane >2){
                                    targetUniChar = missingCharMarker;
                            }
                            break;

                        case ASCII1:
                            if(mySourceChar < 0x7f){
                                targetUniChar = mySourceChar;
                            }
                            else 
                                targetUniChar = missingCharMarker;
                            break;

                        case LATIN1:
                            /*not expected*/
                              break;
                    
                        default:
                            /*not expected */ 
                            break;
                    }
                    if(targetUniChar==missingCharMarker){
                        iterCount = (iterCount<3)? iterCount+1 : 0;
                        myConverterData->fromUnicodeCurrentState=currentState=(StateEnum)(currentState<3)? currentState+1:0;
                        currentState =(StateEnumCN) myConverterData->fromUnicodeCurrentState;
                        myConverterData->fromUnicodeConverter = (myConverterData->fromUnicodeConverter == NULL) ?
                                                        myConverterData->myConverterArray[0] :
                                                    myConverterData->myConverterArray[(int)myConverterData->fromUnicodeCurrentState];
                        targetUniChar =missingCharMarker;
                        isEscapeAppended = FALSE; 
                        /* save the state */
                        myConverterData->isEscapeAppended = isEscapeAppended;
                        myConverterData->isShiftAppended =isShiftAppended;
                        myConverterData->sourceIndex = mySourceIndex;
                        myConverterData->targetIndex = myTargetIndex;
                    }
                }while(targetUniChar==missingCharMarker && initIterState != currentState);
            }
            
            if(targetUniChar!= missingCharMarker){
                
                /* set the iteration state and iteration count  */	    
                initIterState = currentState;
                iterCount =0;
                if(myConverterData->plane != plane){
                    isEscapeAppended=myConverterData->isEscapeAppended=FALSE;
                    myConverterData->plane = plane;
                }
                /* Append the escpace sequence */
                if(!isEscapeAppended){
                    escSeq = (currentState==CNS_11643) ? escSeqCharsCN[(int)currentState+plane-1]:escSeqCharsCN[(int)currentState];
                    concatEscape(args, &myTargetIndex, &myTargetLength, 
                        escSeq,err,strlen(escSeq),&mySourceIndex);
                    isEscapeAppended=myConverterData->isEscapeAppended=TRUE;
                    
                    
                    
                }
                /* Append Shift Sequences */
                if(currentState!=ASCII){
                    
                    if(currentState!=CNS_11643 ){
                        if(!isShiftAppended){
                            concatEscape(args,&myTargetIndex,&myTargetLength,
                                shiftSeqCharsCN[currentState],err,
                                strlen(shiftSeqCharsCN[currentState]),&mySourceIndex);
                            myConverterData->isShiftAppended =isShiftAppended=TRUE;
                        }
                       
                    }
                    else{
                        concatEscape(args,&myTargetIndex,&myTargetLength,shiftSeqCharsCN[currentState+plane],
                            err,strlen(shiftSeqCharsCN[currentState+plane]),&mySourceIndex);
                        
                        myConverterData->isShiftAppended =isShiftAppended=FALSE;
                    
                    }
                    
                }
                
                concatString(args, &myTargetIndex, &myTargetLength,
                    &targetUniChar,err, &mySourceIndex);
                TEST_ERROR_CONDITION_CN(args,myTargetIndex,	mySourceIndex,myConverterData, err);
                
            }/* end of end if(targetUniChar==missingCharMarker)*/
            else{

                /* if we cannot	find the character after checking all codepages 
                 * then this is	an error
                 */
                    reason = UCNV_UNASSIGNED;
                    *err = U_INVALID_CHAR_FOUND;
                    args->converter->invalidUCharBuffer[0]=(UChar)mySourceChar;
                    args->converter->invalidUCharLength++;
                    
CALLBACK:
                    saveSource = args->source;
                    saveTarget = args->target;
     
                    args->target = (char*)myTarget + myTargetIndex;
                    args->source = mySource + mySourceIndex;
                    myConverterData->isShiftAppended =isShiftAppended;

                    FromU_CALLBACK_MACRO(args->converter->fromUContext,
                        args,
                        args->converter->invalidUCharBuffer,
                        args->converter->invalidUCharLength,
                        (UChar32) (args->converter->invalidUCharLength == 2 ? 
                        UTF16_GET_PAIR_VALUE(args->converter->invalidUCharBuffer[0], 
                        args->converter->invalidUCharBuffer[1]) 
                        : args->converter->invalidUCharBuffer[0]),
                        reason,
                        err);
                    args->converter->invalidUCharLength=0;
                    myConverterData->isEscapeAppended = isEscapeAppended=FALSE;
                    args->source=saveSource;
                    args->target=saveTarget;
                    args->converter->fromUSurrogateLead=0x00;
                    initIterState = myConverterData->fromUnicodeCurrentState;

                    if (U_FAILURE (*err)){
                        break;
                    }
                  
            }
            targetUniChar =missingCharMarker;
        } /* end if(myTargetIndex<myTargetLength) */
        else{
            *err =U_BUFFER_OVERFLOW_ERROR;
            break;
        }
        
    }/* end while(mySourceIndex<mySourceLength) */
    
    /*If at the end of conversion we are still carrying state information
    *flush is TRUE, we can deduce that the input stream is truncated
    */
    if (args->converter->fromUSurrogateLead !=0 && (mySourceIndex == mySourceLength) && args->flush){
        if (U_SUCCESS(*err)){
            *err = U_TRUNCATED_CHAR_FOUND;
            args->converter->toUnicodeStatus = 0x00;
        }
    }

    /*save the state and return */
    args->target += myTargetIndex;
    args->source += mySourceIndex;
    myConverterData->sourceIndex = 0;
    myConverterData->targetIndex = 0;
}

U_CFUNC void UConverter_fromUnicode_ISO_2022_CN_OFFSETS_LOGIC(UConverterFromUnicodeArgs* args, UErrorCode* err){
    
    UChar* mySource =(UChar*)args->source;
    UConverterDataISO2022 *myConverterData=(UConverterDataISO2022*)args->converter->extraInfo;
    UConverterCallbackReason reason;
    UBool isEscapeAppended = FALSE;
    StateEnumCN initIterState;
    unsigned char *myTarget = (unsigned char *) args->target; 
    const UChar *saveSource;
    uint32_t targetValue=0;
    char *saveTarget;
    int32_t *saveOffsets ;
    int32_t myTargetLength = args->targetLimit - args->target;
    int32_t mySourceLength = args->sourceLimit - args->source;
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    int32_t length  =0;
    int plane = 0;
    CompactShortArray *myFromUnicodeDBCS = NULL;
    CompactShortArray *myFromUnicodeDBCSFallback = NULL;
    CompactByteArray  *myFromUnicodeSBCS = NULL;
    CompactByteArray  *myFromUnicodeSBCSFallback = NULL;
    UChar32 targetUniChar = missingCharMarker;
    int32_t currentOffset=0;
    StateEnumCN currentState=ASCII;
    
    UChar32 mySourceChar = 0x0000;
    int iterCount = 0;
    const char *escSeq = NULL;
    UBool isShiftAppended = FALSE;

    isEscapeAppended =(UBool) myConverterData->isEscapeAppended;
    isShiftAppended =(UBool) myConverterData->isShiftAppended;
    initIterState = myConverterData->fromUnicodeCurrentState;
    /* arguments check*/
    if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    if(args->converter->fromUSurrogateLead!=0 && myTargetIndex < myTargetLength) {
        goto getTrail;
    }
    while(mySourceIndex <  mySourceLength){
        currentState =(StateEnumCN) myConverterData->fromUnicodeCurrentState;
        myConverterData->fromUnicodeConverter = (myConverterData->fromUnicodeConverter == NULL) ?
            myConverterData->myConverterArray[0] :
        myConverterData->myConverterArray[(int)myConverterData->fromUnicodeCurrentState];
        
        if(myTargetIndex < myTargetLength){
            
            mySourceChar = (UChar) args->source[mySourceIndex++];
            
            /* I am handling surrogates in the begining itself so that I donot have to go through 4
             * iterations on codepages that we support. 
             */
            if(UTF_IS_SURROGATE(mySourceChar)) {
                if(UTF_IS_SURROGATE_FIRST(mySourceChar)) {
                    args->converter->fromUSurrogateLead=(UChar)mySourceChar;
getTrail:
                    /*look ahead to find the trail surrogate*/
                    if(mySourceIndex <  mySourceLength) {
                        /* test the following code unit */
                        UChar trail=(UChar) args->source[mySourceIndex];
                        if(UTF_IS_SECOND_SURROGATE(trail)) {
                            ++mySourceIndex;
                            mySourceChar=UTF16_GET_PAIR_VALUE(mySourceChar, trail);
                            args->converter->fromUSurrogateLead=0x00;
                            /* convert this surrogate code point */
                            /* exit this condition tree */
                        } else {
                            /* this is an unmatched lead code unit (1st surrogate) */
                            /* callback(illegal) */
                            reason=UCNV_ILLEGAL;
                            *err=U_ILLEGAL_CHAR_FOUND;
                            goto CALLBACK;
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
                    goto CALLBACK;
                }
            }
            /* if the source character is CR or LF then append the ASCII escape sequence*/
            if(mySourceChar== 0x000A || mySourceChar== 0x000D){
                
                if((int)currentState > 0 && isShiftAppended){
                    concatChar(args, &myTargetIndex, &myTargetLength, UCNV_SI,err,&mySourceIndex);
                    isShiftAppended=myConverterData->isShiftAppended=FALSE;
                    TEST_ERROR_CONDITION_CN(args,myTargetIndex, mySourceIndex,myConverterData, err);
                }
                myConverterData->isEscapeAppended=isEscapeAppended=FALSE;
                targetUniChar = mySourceChar;
                concatString(args, &myTargetIndex, &myTargetLength,&targetUniChar,err,&mySourceIndex);
                TEST_ERROR_CONDITION_CN(args,myTargetIndex,	mySourceIndex,myConverterData, err);
                
                continue;
            }
            else{
                do{
                    switch (myConverterTypeCN[currentState]){
                    
                        case SBCS:
                            if(mySourceChar<0xffff){
                                myFromUnicodeSBCS =	&myConverterData->fromUnicodeConverter->sharedData->table->sbcs.fromUnicode;
                                myFromUnicodeSBCSFallback = &myConverterData->fromUnicodeConverter->sharedData->table->sbcs.fromUnicodeFallback;
                        
                                targetUniChar = (UChar32) ucmp8_getu (myFromUnicodeSBCS, mySourceChar);
                        
                                if ((targetUniChar==0)&&(args->converter->useFallback == TRUE) &&
                                    (myConverterData->fromUnicodeConverter->sharedData->staticData->hasFromUnicodeFallback == TRUE)){
                                    targetUniChar = (UChar32) ucmp8_getu (myFromUnicodeSBCSFallback, mySourceChar);
                                }
                        
                            }
                            /* ucmp8_getU returns 0	for missing char so explicitly set it missingCharMarker*/
                            targetUniChar=(UChar)((targetUniChar==0) ? (UChar) missingCharMarker : targetUniChar);
                            break;
                    
                        case DBCS:
                            if(mySourceChar<0xffff){
                                myFromUnicodeDBCS =	&myConverterData->fromUnicodeConverter->sharedData->table->dbcs.fromUnicode;
                                myFromUnicodeDBCSFallback = &myConverterData->fromUnicodeConverter->sharedData->table->dbcs.fromUnicodeFallback;
                                targetUniChar = (UChar) ucmp16_getu (myFromUnicodeDBCS,	mySourceChar);
                        
                                if ((targetUniChar==missingCharMarker)&&(args->converter->useFallback	== TRUE) &&
                                    (myConverterData->fromUnicodeConverter->sharedData->staticData->hasFromUnicodeFallback == TRUE)){
                                    targetUniChar = (UChar) ucmp16_getu (myFromUnicodeDBCSFallback, mySourceChar);
                                }
                            }
                            if(( myConverterData->version) == 0 && currentState==ISO_IR_165){
                                targetUniChar=missingCharMarker;
                            }
                            break;
                    
                        case MBCS:
                    
                            length= _MBCSFromUChar32(myConverterData->fromUnicodeConverter->sharedData,
                                mySourceChar,&targetValue,args->converter->useFallback);
                    
                            targetUniChar = (UChar32) targetValue;
                    
                            if(length==0){
                                targetUniChar = missingCharMarker;
                            } 
                            else if(length==3){
                                uint8_t	planeVal = (uint8_t) ((targetValue)>>16);
                                if(planeVal >0x80 && planeVal<0x89){
                                    plane = (int)(planeVal - 0x80);
                                    targetUniChar -= (planeVal<<16);
                                }else 
                                    plane =-1;
                            }
                            else if(length >3){
                                reason =UCNV_ILLEGAL;
                                *err =U_INVALID_CHAR_FOUND;
                                goto CALLBACK;
                            }
                            if(myConverterData->version == 0 && plane >2){
                                    targetUniChar = missingCharMarker;
                            }
                            break;

                        case ASCII1:
                            if(mySourceChar < 0x7f){
                                targetUniChar = mySourceChar;
                            }
                            else 
                                targetUniChar = missingCharMarker;
                            break;

                        case LATIN1:
                            /*not expected*/
                              break;
                        default:
                            /*not expected */ 
                            break;
                    }
                    if(targetUniChar==missingCharMarker){
                        iterCount = (iterCount<3)? iterCount+1 : 0;
                        myConverterData->fromUnicodeCurrentState=currentState=(StateEnum)(currentState<3)? currentState+1:0;
                        currentState =(StateEnumCN) myConverterData->fromUnicodeCurrentState;
                        myConverterData->fromUnicodeConverter = (myConverterData->fromUnicodeConverter == NULL) ?
                                                        myConverterData->myConverterArray[0] :
                                                    myConverterData->myConverterArray[(int)myConverterData->fromUnicodeCurrentState];
                        targetUniChar =missingCharMarker;
                        isEscapeAppended = FALSE; 
                        /* save the state */
                        myConverterData->isEscapeAppended = isEscapeAppended;
                        myConverterData->isShiftAppended =isShiftAppended;
                        myConverterData->sourceIndex = mySourceIndex;
                        myConverterData->targetIndex = myTargetIndex;
                    }
                }while(targetUniChar==missingCharMarker && initIterState != currentState);
            }
            
            if(targetUniChar!= missingCharMarker){
                
                /* set the iteration state and iteration count  */	    
                initIterState = currentState;
                iterCount =0;
                if(myConverterData->plane != plane){
                    isEscapeAppended=myConverterData->isEscapeAppended=FALSE;
                    myConverterData->plane = plane;
                }
                /* Append the escpace sequence */
                if(!isEscapeAppended){
                    escSeq = (currentState==CNS_11643) ? escSeqCharsCN[(int)currentState+plane-1]:escSeqCharsCN[(int)currentState];
                    concatEscape(args, &myTargetIndex, &myTargetLength, 
                        escSeq,err,strlen(escSeq),&mySourceIndex);
                    isEscapeAppended=myConverterData->isEscapeAppended=TRUE;
                    
                    
                    
                }
                /* Append Shift Sequences */
                if(currentState!=ASCII){
                    
                    if(currentState!=CNS_11643 ){
                        if(!isShiftAppended){
                            concatEscape(args,&myTargetIndex,&myTargetLength,
                                shiftSeqCharsCN[currentState],err,
                                strlen(shiftSeqCharsCN[currentState]),&mySourceIndex);
                            myConverterData->isShiftAppended =isShiftAppended=TRUE;
                        }
                       
                    }
                    else{
                        concatEscape(args,&myTargetIndex,&myTargetLength,shiftSeqCharsCN[currentState+plane],
                            err,strlen(shiftSeqCharsCN[currentState+plane]),&mySourceIndex);
                        
                        myConverterData->isShiftAppended =isShiftAppended=FALSE;
                    
                    }
                    
                }
                
                concatString(args, &myTargetIndex, &myTargetLength,
                    &targetUniChar,err, &mySourceIndex);
                TEST_ERROR_CONDITION_CN(args,myTargetIndex,	mySourceIndex,myConverterData, err);
                
            }/* end of end if(targetUniChar==missingCharMarker)*/
            else{

               
                /* if we cannot	find the character after checking all codepages 
                 * then this is	an error
                 */
                    reason = UCNV_UNASSIGNED;
                    *err = U_INVALID_CHAR_FOUND;
                    
CALLBACK:
                    args->converter->invalidUCharBuffer[args->converter->invalidUCharLength++]=(UChar)mySourceChar;
                    currentOffset = args->offsets[myTargetIndex-1]+1;
                    saveSource = args->source;
                    saveTarget = args->target;
                    saveOffsets = args->offsets;
                    args->target = (char*)myTarget + myTargetIndex;
                    args->source = mySource + mySourceIndex;

                    myConverterData->isEscapeAppended = isEscapeAppended;
                    myConverterData->isShiftAppended =isShiftAppended;
                    myConverterData->sourceIndex = mySourceIndex;
                    myConverterData->targetIndex = myTargetIndex;

                    args->offsets = args->offsets?args->offsets+myTargetIndex:0;
                    FromU_CALLBACK_OFFSETS_LOGIC_MACRO(args->converter->fromUContext,
                        args,
                        args->converter->invalidUCharBuffer,
                        args->converter->invalidUCharLength,
                        (UChar32) (args->converter->invalidUCharLength == 2 ? 
                        UTF16_GET_PAIR_VALUE(args->converter->invalidUCharBuffer[0], 
                        args->converter->invalidUCharBuffer[1]) 
                        : args->converter->invalidUCharBuffer[0]),
                        reason,
                        err);
                    args->converter->invalidUCharLength=0;
                    args->source=saveSource;
                    args->target=saveTarget;
                    args->offsets=saveOffsets;
                    initIterState = myConverterData->fromUnicodeCurrentState;
                    myConverterData->isEscapeAppended=isEscapeAppended=FALSE;
                    args->converter->fromUSurrogateLead=0x00;

                    if (U_FAILURE (*err)){
                        break;
                    }
            }
            targetUniChar =missingCharMarker;
        } /* end if(myTargetIndex<myTargetLength) */
        else{
            *err =U_BUFFER_OVERFLOW_ERROR;
            break;
        }
        
    }/* end while(mySourceIndex<mySourceLength) */
    /*If at the end of conversion we are still carrying state information
    *flush is TRUE, we can deduce that the input stream is truncated
    */
    if (args->converter->fromUSurrogateLead !=0 && (mySourceIndex == mySourceLength) && args->flush){
        if (U_SUCCESS(*err)){
            *err = U_TRUNCATED_CHAR_FOUND;
            args->converter->toUnicodeStatus = 0x00;
        }
    }

    
    /*save the state and return */
    args->target += myTargetIndex;
    args->source += mySourceIndex;
    myConverterData->sourceIndex = 0;
    myConverterData->targetIndex = 0;
}
/*************** to unicode *******************/

static void changeState_2022_CN(UConverter* _this,
                                const char** source, 
                                const char* sourceLimit,
                                UBool flush,int* plane,
                                UErrorCode* err){
    UConverter* myUConverter;
    UCNV_TableStates_2022 value;
    UConverterDataISO2022* myData2022 = ((UConverterDataISO2022*)_this->extraInfo);
    uint32_t key = myData2022->key;
    const char* chosenConverterName = NULL;
    const char* sourceStart =*source;
    char c;
    char cnvName[20];
    int32_t offset;
    
    /*In case we were in the process of consuming an escape sequence
    we need to reprocess it */
    
    do{
        
        /* Needed explicit cast for key on MVS to make compiler happy - JJD */
        if(**source == UCNV_SI ){
            if(sourceStart == *source){
                (*source)++;
                value=(UCNV_TableStates_2022) 1;
                offset	= 6;
                goto DONE;
            }
        }
        if(**source ==UCNV_SO){
            if(sourceStart == *source){
                (*source)++;
                return;
            }
        }
        value = getKey_2022(**source,(int32_t *) &key, &offset);
        switch (value){
        case VALID_NON_TERMINAL_2022 : 
            break;
            
        case VALID_TERMINAL_2022:
            {
                (*source)++;
                chosenConverterName = escSeqStateTable_Result_2022[offset];
                key	= 0;
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
            {
                (*source)++;
                key	= 0;
                goto DONE;
            }
            
        case VALID_MAYBE_TERMINAL_2022:
            {
                const char* mySource = (*source+1);
                int32_t myKey =	key;
                UCNV_TableStates_2022 myValue = value;
                int32_t myOffset=0;
                if(*mySource==ESC_2022){
                    while ((mySource < sourceLimit) && 
                        ((myValue == VALID_MAYBE_TERMINAL_2022)||(myValue ==	VALID_NON_TERMINAL_2022))){
                        myValue	= getKey_2022(*(mySource++), &myKey, &myOffset);
                    }
                }
                else{
                    (*source)++;
                    myValue=(UCNV_TableStates_2022) 1;
                    myOffset = 7;
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
                        key	= 0;
                        value = VALID_TERMINAL_2022;
                        goto DONE;
                    };
                    break;
                    
                    /* Not expected. Added to make the gcc happy */
                case VALID_SS2_SEQUENCE:
                    {
                        (*source)++;
                        key	= 0;
                        goto DONE;
                    }
                    
                case VALID_NON_TERMINAL_2022: 
                    
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
    if(chosenConverterName){
        if(uprv_strstr(chosenConverterName,"CNS")!=NULL){
            int i=0;
            while((c=*chosenConverterName)!=0 && c!=UCNV_OPTION_SEP_CHAR ) {
                
                cnvName[i++]=c;
                ++chosenConverterName;
            }
            cnvName[i]=0;
            if(c==UCNV_OPTION_SEP_CHAR){
                chosenConverterName++;
                *plane = atoi(chosenConverterName);
            }
            
        }
        else{
            uprv_strcpy(cnvName,chosenConverterName);
            *plane=0;
        }
    }
    if ((value == VALID_NON_TERMINAL_2022) || (value == VALID_MAYBE_TERMINAL_2022)) {
        return;
    }
    if (value > 0 ) {
        if(value==3 || value==4 ){
            _this->mode = UCNV_SI;
            myUConverter =myData2022->currentConverter;
        }
        else{
            _this->mode = UCNV_SI;
           /* ucnv_close(myData2022->currentConverter);
              myData2022->currentConverter = myUConverter = ucnv_open(cnvName, err);*/
            if( cnvName[0] == 'l' ){
                myData2022->currentConverter = myUConverter = myData2022->myConverterArray[0];
            }
            else if( cnvName[0] == 'g' ){
                myData2022->currentConverter = myUConverter = myData2022->myConverterArray[1];
            }
            else if(cnvName[0] == 'I' ){
              myData2022->currentConverter = myUConverter = myData2022->myConverterArray[2];
            }  
            else if(  cnvName[0] == 'C'){
                myData2022->currentConverter = myUConverter = myData2022->myConverterArray[3];
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
                myUConverter->subCharLen	= _this->subCharLen);
            
            _this->mode = UCNV_SO;
        }
    }
    
    return;
}  

U_CFUNC void UConverter_toUnicode_ISO_2022_CN(UConverterToUnicodeArgs *args,
                                              UErrorCode* err){
    char tempBuf[3];
    int plane=0;
    const char* pBuf;
    const char *mySource = ( char *) args->source;
    UChar *myTarget = args->target;
    char *tempLimit = &tempBuf[2]+1; 
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    const char *mySourceLimit = args->sourceLimit;
    UChar32 targetUniChar = 0x0000;
    UChar mySourceChar = 0x0000;
    UConverterDataISO2022* myData=(UConverterDataISO2022*)(args->converter->extraInfo);
    CompactShortArray *myToUnicodeDBCS=NULL, *myToUnicodeFallbackDBCS = NULL; 
    
    plane=myData->plane;
    /*Arguments Check*/
    if (U_FAILURE(*err)) 
        return;
    
    if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    
    while(mySource< args->sourceLimit){
        
        if(myTarget < args->targetLimit){
            
            mySourceChar= (unsigned char) *mySource++;
            
            if(args->converter->mode==UCNV_SI){
                    
               /* if there	are no escape sequences in the first buffer then they
                * are assumed to be ASCII according to RFC-1922
                */
                    
                    myData->toUnicodeCurrentState = SBCS;
                    myData->plane=plane = 0;
             }
            
            switch(mySourceChar){
                case 0x0A:
                    if(args->converter->toUnicodeStatus !=	0x00){
                        goto SAVE_STATE;
                    }
                    myData->toUnicodeCurrentState = SBCS;
                    myData->plane=plane = 0;
                    break;
                
                case 0x0D:
                    if(args->converter->toUnicodeStatus !=	0x00){
                        goto SAVE_STATE;
                    }
                    myData->toUnicodeCurrentState = SBCS;
                    myData->plane=plane = 0;
                    break;
                
                case UCNV_SI:
                    if(args->converter->toUnicodeStatus != 0x00){
                        goto SAVE_STATE;
                    }
                    myData->toUnicodeCurrentState = SBCS;
                    myData->plane=plane = 0;
                    continue;
                
                case UCNV_SO:
                    if(args->converter->toUnicodeStatus != 0x00){
                        goto SAVE_STATE;
                    }
                
                    myData->toUnicodeCurrentState = (plane>0) ? MBCS: DBCS;
                    continue;
                            
                default:
                    /* if we are in the middle of consuming an escape sequence 
                     * we continue to next switch tag else we break
                     */
                    if(myData->key==0){
                        break;
                    }
                case ESC_2022:
                    if(args->converter->toUnicodeStatus != 0x00){
                        goto SAVE_STATE;
                    }
                    mySource--;
                    changeState_2022_CN(args->converter,&(mySource), 
                        args->sourceLimit, args->flush,&plane,err);
                
                    myData->plane=plane;
                    if(plane>0){
                        myData->toUnicodeCurrentState = MBCS;
                    }
                    else if(myData->currentConverter &&  
                                uprv_stricmp("latin_1", 
                                myData->currentConverter->sharedData->staticData->name)==0){
                    
                        myData->toUnicodeCurrentState=SBCS;
                    }
                    if(U_FAILURE(*err)){
                        goto SAVE_STATE;
                    }
                    continue;
            }
            
            switch(myData->toUnicodeCurrentState){
                
                case SBCS:
                
                    if(args->converter->fromUnicodeStatus == 0x00){
                        targetUniChar = (UChar)	 mySourceChar;
                    }
                    else{
                        goto SAVE_STATE;
                    }
                    break;
                
                case DBCS:
                    myToUnicodeDBCS	= &myData->currentConverter->sharedData->table->dbcs.toUnicode;
                    myToUnicodeFallbackDBCS = &myData->currentConverter->sharedData->table->dbcs.toUnicodeFallback;
                
                    if(args->converter->toUnicodeStatus == 0x00){
                        args->converter->toUnicodeStatus = (UChar) mySourceChar;
                        continue;
                    }
                    else{
                        tempBuf[0] =	(char) args->converter->toUnicodeStatus ;
                        tempBuf[1] =	(char) mySourceChar;
                        mySourceChar= (UChar)((args->converter->toUnicodeStatus << 8) | (mySourceChar & 0x00ff));
                        args->converter->toUnicodeStatus =0x00;
                    
                        targetUniChar = ucmp16_getu(myToUnicodeDBCS,mySourceChar);
                        if(targetUniChar> 0xfffe){
                            if((args->converter->useFallback == TRUE) && 
                                (myData->currentConverter->sharedData->staticData->hasFromUnicodeFallback == TRUE)){
                            
                                targetUniChar =	(UChar) ucmp16_getu(myToUnicodeFallbackDBCS, mySourceChar);
                            }
                        }
                    }
                
                    break;
                
                case MBCS:
                
                    if(args->converter->toUnicodeStatus == 0x00){
                        args->converter->toUnicodeStatus = (UChar) mySourceChar;
                        continue;
                    }
                    else{
                        tempBuf[0] = (char)( 0x80+plane);
                        tempBuf[1] = (char) (args->converter->toUnicodeStatus);
                        tempBuf[2] = (char) (mySourceChar);
                        args->converter->toUnicodeStatus = 0;
                    
                        pBuf = &tempBuf[0];
                        tempLimit = &tempBuf[2]+1;
                        targetUniChar	= _MBCSSimpleGetNextUChar(myData->currentConverter->sharedData,
                            &pBuf,tempLimit,args->converter->useFallback);
                    }
                    break;
                
                case LATIN1:
                    break;
            }
            if(targetUniChar < 0xfffe){
                *(myTarget++)=(UChar)targetUniChar;
            }
            else if(targetUniChar>=0xfffe){
SAVE_STATE:
                {
                    const char *saveSource = args->source;
                    UChar *saveTarget = args->target;
                    int32_t *saveOffsets = args->offsets;
                    UConverterCallbackReason reason;
                
                    if(targetUniChar == 0xfffe){
                        reason = UCNV_UNASSIGNED;
                        *err = U_INVALID_CHAR_FOUND;
                    }
                    else{
                        reason = UCNV_ILLEGAL;
                        *err = U_ILLEGAL_CHAR_FOUND;
                    }
                    switch(myData->toUnicodeCurrentState){
                        case SBCS:
                            args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = (char)mySourceChar;
                            break;

                        case DBCS:
                            args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = tempBuf[0];
                            args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = tempBuf[1];
                            break;

                        case MBCS:
                            args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = tempBuf[1];
                            args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = tempBuf[2];
                    }

                    args->target = myTarget;
                    args->source = mySource;
                    ToU_CALLBACK_MACRO( args->converter->toUContext,
                        args,
                        args->converter->invalidCharBuffer,
                        args->converter->invalidCharLength,
                        reason,
                        err);
                    myTarget += args->target - myTarget;
                    args->source = saveSource;
                    args->target = saveTarget;
                    args->offsets = saveOffsets;
                    args->converter->invalidCharLength=0;

                    if(U_FAILURE(*err))
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
        if(U_SUCCESS(*err)){
            *err = U_TRUNCATED_CHAR_FOUND;
            args->converter->toUnicodeStatus = 0x00;
        }
    }
    
    args->target = myTarget;
    args->source = mySource;
}

U_CFUNC void UConverter_toUnicode_ISO_2022_CN_OFFSETS_LOGIC(UConverterToUnicodeArgs *args,
                                                            UErrorCode* err){
    char tempBuf[3];
    int plane=0;
    const char* pBuf;
    const char *mySource = ( char *) args->source;
    UChar *myTarget = args->target;
    char *tempLimit = &tempBuf[3]; 
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    const char *mySourceLimit = args->sourceLimit;
    UChar32 targetUniChar = 0x0000;
    UChar mySourceChar = 0x0000;
    UConverterDataISO2022* myData=(UConverterDataISO2022*)(args->converter->extraInfo);
    CompactShortArray *myToUnicodeDBCS=NULL, *myToUnicodeFallbackDBCS = NULL; 
    
    /*Arguments Check*/
    if (U_FAILURE(*err)) 
        return;
    
    if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    
    while(mySource< args->sourceLimit){
        
        if(myTarget < args->targetLimit){
            
            mySourceChar= (unsigned char) *mySource++;
            
            
            switch(mySourceChar){
            case 0x0A:
                if(args->converter->toUnicodeStatus !=	0x00){
                    goto SAVE_STATE;
                }
                myData->toUnicodeCurrentState = SBCS;
                myData->plane=plane = 0;
                break;
                
            case 0x0D:
                if(args->converter->toUnicodeStatus !=	0x00){
                    goto SAVE_STATE;
                }
                myData->toUnicodeCurrentState = SBCS;
                myData->plane=plane = 0;
                break;
                
            case UCNV_SI:
                if(args->converter->toUnicodeStatus != 0x00){
                    goto SAVE_STATE;
                }
                myData->toUnicodeCurrentState = SBCS;
                myData->plane=plane = 0;
                continue;
                
            case UCNV_SO:
                if(args->converter->toUnicodeStatus != 0x00){
                    goto SAVE_STATE;
                }
                
                myData->toUnicodeCurrentState = (plane>0) ? MBCS: DBCS;
                continue;
                
            default:
                /* if we are in the middle of consuming an escape sequence 
                 * we continue to next switch tag else we break
                 */
                if(myData->key==0){
                    break;
                }
            case ESC_2022:
                if(args->converter->toUnicodeStatus != 0x00){
                    goto SAVE_STATE;
                }
                mySource--;
                changeState_2022_CN(args->converter,&(mySource), 
                    args->sourceLimit, args->flush,&plane,err);
                
                myData->plane=plane;
                if(plane>0){
                    myData->toUnicodeCurrentState = MBCS;
                }
                else if(myData->currentConverter &&  
                            uprv_stricmp("latin_1", 
                            myData->currentConverter->sharedData->staticData->name)==0){
                    
                    myData->toUnicodeCurrentState=SBCS;
                }
                if(U_FAILURE(*err)){
                    goto SAVE_STATE;
                }
                continue;
            
            }
            
            switch(myData->toUnicodeCurrentState){
                
            case SBCS:
                
                if(args->converter->fromUnicodeStatus == 0x00){
                    targetUniChar = (UChar)	 mySourceChar;
                }
                else{
                    goto SAVE_STATE;
                }
                break;
                
            case DBCS:
                myToUnicodeDBCS	= &myData->currentConverter->sharedData->table->dbcs.toUnicode;
                myToUnicodeFallbackDBCS = &myData->currentConverter->sharedData->table->dbcs.toUnicodeFallback;
                
                if(args->converter->toUnicodeStatus == 0x00){
                    args->converter->toUnicodeStatus = (UChar) mySourceChar;
                    continue;
                }
                else{
                    tempBuf[0] =	(char) args->converter->toUnicodeStatus ;
                    tempBuf[1] =	(char) mySourceChar;
                    mySourceChar= (UChar)((args->converter->toUnicodeStatus << 8) | (mySourceChar & 0x00ff));
                    args->converter->toUnicodeStatus =0x00;
                    
                    targetUniChar = ucmp16_getu(myToUnicodeDBCS,mySourceChar);
                    if(targetUniChar> 0xfffe){
                        if((args->converter->useFallback == TRUE) && 
                            (myData->currentConverter->sharedData->staticData->hasFromUnicodeFallback == TRUE)){
                            
                            targetUniChar =	(UChar) ucmp16_getu(myToUnicodeFallbackDBCS, mySourceChar);
                        }
                    }
                }
                
                break;
                
            case MBCS:
                
                if(args->converter->toUnicodeStatus == 0x00){
                    args->converter->toUnicodeStatus = (UChar) mySourceChar;
                    continue;
                }
                else{
                    tempBuf[0] = (char) (0x80+plane);
                    tempBuf[1] = (char) (args->converter->toUnicodeStatus);
                    tempBuf[2] = (char) (mySourceChar);
                    args->converter->toUnicodeStatus = 0x00;
                    pBuf = &tempBuf[0];
                    tempLimit = &tempBuf[2]+1;
                    targetUniChar	= _MBCSSimpleGetNextUChar(myData->currentConverter->sharedData,
                        &pBuf,tempLimit,args->converter->useFallback);
                }
                break;
                
            case LATIN1:
                break;
            }
            if(targetUniChar < 0xfffe){
                if(myData->toUnicodeCurrentState == SBCS){
                    args->offsets[myTarget - args->target]=	mySource - args->source - 1;
                }
                else{
                    args->offsets[myTarget - args->target]=	mySource - args->source - 2;
                }
                *(myTarget++)=(UChar)targetUniChar;
            }
            else if(targetUniChar>=0xfffe){
SAVE_STATE:
                {
                    const char *saveSource = args->source;
                    UChar *saveTarget = args->target;
                    int32_t *saveOffsets = args->offsets;
                    UConverterCallbackReason reason;
                    int32_t currentOffset ;
                    int32_t My_i = myTarget - args->target;
                    
                    
                    switch(myData->toUnicodeCurrentState){
                        case SBCS:
                            currentOffset=	mySource - args->source -1;
                            args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = (char)mySourceChar;
                            break;

                        case DBCS:
                            currentOffset=	mySource - args->source -2;
                            args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = tempBuf[0];
                            args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = tempBuf[1];
                            break;

                        case MBCS:
                            currentOffset=	mySource - args->source -2;
                            args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = tempBuf[1];
                            args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = tempBuf[2];
                            break;

                        default:
                            currentOffset = mySource - args->source -1;
                    }

                    /*reason = (targetUniChar == 0xfffe) ? UCNV_UNASSIGNED:UCNV_ILLEGAL;
                    *err = (targetUniChar == 0xfffe) ? U_INVALID_CHAR_FOUND : U_ILLEGAL_CHAR_FOUND;*/

                    if(targetUniChar == 0xfffe){
                        reason = UCNV_UNASSIGNED;
                        *err = U_INVALID_CHAR_FOUND;
                    }
                    else{
                        reason = UCNV_ILLEGAL;
                        *err = U_ILLEGAL_CHAR_FOUND;
                    }

                    
                    args->offsets = args->offsets?args->offsets+(myTarget - args->target):0;
                    args->target =myTarget;
                    myTarget =saveTarget;
                    ToU_CALLBACK_OFFSETS_LOGIC_MACRO( args->converter->toUContext,
                        args,
                        args->converter->invalidCharBuffer,
                        args->converter->invalidCharLength,
                        reason,
                        err);
                    args->converter->invalidCharLength=0;
                    myTarget=args->target;
                    args->source  = saveSource;
                    args->target  = saveTarget;
                    args->offsets = saveOffsets;

                    if(U_FAILURE(*err))
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
        if(U_SUCCESS(*err)){
            *err = U_TRUNCATED_CHAR_FOUND;
            args->converter->toUnicodeStatus = 0x00;
        }
    }
    args->target = myTarget;
    args->source = mySource;
}

/*
* This is a simple, interim implementation of GetNextUChar()
* that allows to concentrate on testing one single implementation
* of the ToUnicode conversion before it gets copied to
* multiple version that are then optimized for their needs
* (with vs. without offsets and getNextUChar).
*/

U_CFUNC UChar32
UConverter_getNextUChar_ISO_2022_CN(UConverterToUnicodeArgs *pArgs,
                                    UErrorCode *pErrorCode) {
    UChar buffer[UTF_MAX_CHAR_LENGTH];
    const char *realLimit=pArgs->sourceLimit;
    
    pArgs->target=buffer;
    pArgs->targetLimit=buffer+UTF_MAX_CHAR_LENGTH;
    
    while(pArgs->source<realLimit) {
        /* feed in one byte at a time to make sure to get only one character out */
        pArgs->sourceLimit=pArgs->source+1;
        pArgs->flush= (UBool)(pArgs->sourceLimit==realLimit);
        UConverter_toUnicode_ISO_2022_CN(pArgs, pErrorCode);
        if(U_FAILURE(*pErrorCode) && *pErrorCode!=U_BUFFER_OVERFLOW_ERROR) {
            return 0xffff;
        } else if(pArgs->target!=buffer) {
            if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR) {
                *pErrorCode=U_ZERO_ERROR;
            }
            return ucnv_getUChar32KeepOverflow(pArgs->converter, buffer, pArgs->target-buffer);
        }
    }
    
    /* no output because of empty input or only state changes and skipping callbacks */
    *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
    return 0xffff;
}
