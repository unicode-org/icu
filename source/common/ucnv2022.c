/*  
**********************************************************************
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   file name:  ucnv2022.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000feb03
*   created by: Markus W. Scherer
*
*   Change history:
*
*   06/29/2000  helena      Major rewrite of the callback APIs.
*   08/08/2000  Ram			Included support for ISO-2022-JP-2
*							Changed implementation of toUnicode
*							function
*   08/21/2000  Ram         Added support for ISO-2022-KR
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



/* Added by ram for ISO-2022JP implementation*/
typedef enum  {
	ASCII = 0,
	ISO8859_1 ,
	ISO8859_7 ,
	JISX201 ,
	JISX208 ,
	JISX212 ,
	GB2312  ,
	KSC5601

} StateEnum;


typedef enum {
	SBCS = 0,
	DBCS,
	MBCS,
	LATIN1
}Cnv2022Type;


typedef struct
  {
    UConverter *currentConverter;
	UConverter *previousConverter;
	UConverter *fromUnicodeConverter;
	UBool isFirstBuffer;
	StateEnum currentState;
    uint8_t escSeq2022[10];
    int8_t escSeq2022Length;
	UConverter* myConverterArray[8];
	int32_t targetIndex;
	int32_t sourceIndex;
	UBool isEscapeAppended;
	UBool soAppended;
	char *currentLocale;
  }
UConverterDataISO2022;

/* ISO-2022 ----------------------------------------------------------------- */

U_CFUNC void T_UConverter_fromUnicode_UTF8 (UConverterFromUnicodeArgs * args,
				    UErrorCode * err);

U_CFUNC void T_UConverter_fromUnicode_UTF8_OFFSETS_LOGIC (UConverterFromUnicodeArgs * converter,
				    UErrorCode * err);

U_CFUNC void T_UConverter_fromUnicode_ISO_2022_JP(UConverterFromUnicodeArgs* args, 
												  UErrorCode* err);
U_CFUNC void UConverter_fromUnicode_ISO_2022_KR(UConverterFromUnicodeArgs* args, 
												  UErrorCode* err);
U_CFUNC void UConverter_toUnicode_ISO_2022_KR(UConverterToUnicodeArgs* args, UErrorCode* err);

#define ESC_2022 0x1B /*ESC*/

typedef enum 
{
  INVALID_2022 = -1, /*Doesn't correspond to a valid iso 2022 escape sequence*/
  VALID_NON_TERMINAL_2022 = 0, /*so far corresponds to a valid iso 2022 escape sequence*/
  VALID_TERMINAL_2022 = 1, /*corresponds to a valid iso 2022 escape sequence*/
  VALID_MAYBE_TERMINAL_2022 = 2 /*so far matches one iso 2022 escape sequence, but by adding more characters might match another escape sequence*/

} UCNV_TableStates_2022;

/*
 * The way these state transition arrays work is:
 * ex : ESC$B is the sequence for JISX208
 *		a) First Iteration: char is ESC
 *			i) Get the value of ESC from normalize_esq_chars_2022[] with int value of ESC as index
 *			   int x = normalize_esq_chars_2022[27] which is equal to 1
 *		   ii) Search for this value in escSeqStateTable_Key_2022[]
 *			   value of x is stored at escSeqStateTable_Key_2022[0]
 *		  iii) Save this index as offset
 *		   iv) Get state of this sequence from escSeqStateTable_Value_2022[]
 *			   escSeqStateTable_Value_2022[offset], which is VALID_NON_TERMINAL_2022
 *     b) Switch on this state and continue to next char
 *			i) Get the value of $ from normalize_esq_chars_2022[] with int value of $ as index
 *			   which is normalize_esq_chars_2022[36] == 4
 *         ii) x is currently 1(from above) 
 *				x<<=5 -- x is now 32
 *		  	    x+=normalize_esq_chars_2022[36]
 *				now x is 36
 *        iii) Search for this value in escSeqStateTable_Key_2022[]
 *			   value of x is stored at escSeqStateTable_Key_2022[2], so offset is 2
 *		   iv) Get state of this sequence from escSeqStateTable_Value_2022[]
 *			   escSeqStateTable_Value_2022[offset], which is VALID_NON_TERMINAL_2022
 *	   c) Switch on this state and continue to next char
 *        i) Get the value of B from normalize_esq_chars_2022[] with int value of B as index
 *         ii) x is currently 36 (from above) 
 *				x<<=5 -- x is now 1152
 *		  	    x+=normalize_esq_chars_2022[66]
 *				now x is 1161
 *        iii) Search for this value in escSeqStateTable_Key_2022[]
 *			   value of x is stored at escSeqStateTable_Key_2022[21], so offset is 21
 *		   iv) Get state of this sequence from escSeqStateTable_Value_2022[21]
 *			   escSeqStateTable_Value_2022[offset], which is VALID_TERMINAL_2022
 *		    v) Get the converter name form escSeqStateTable_Result_2022[21] which is JISX208
 */     


/*Below are the 3 arrays depicting a state transition table*/
int8_t normalize_esq_chars_2022[256] = {
/*		0		1		2		3		4		5		6		7		8		9			*/

        0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
       ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
       ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,1      ,0      ,0
       ,0      ,0      ,0      ,0      ,0      ,0      ,4      ,7      ,0      ,0
       ,2      ,24     ,0      ,0      ,0      ,3      ,23     ,6      ,0      ,0
       ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
       ,0      ,0      ,0      ,0      ,5      ,8      ,9      ,10     ,11     ,12
       ,13     ,14     ,15     ,16     ,17     ,18     ,19     ,20     ,0      ,0
       ,0      ,0      ,21     ,0      ,0      ,0      ,0      ,0      ,0      ,0
       ,22     ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
       ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
       ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
       ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
       ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
       ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
       ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
       ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
       ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
       ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
       ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
       ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
       ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
       ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
       ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
       ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
       ,0      ,0      ,0      ,0      ,0      ,0
	};

#define MAX_STATES_2022 60
int32_t escSeqStateTable_Key_2022[MAX_STATES_2022] = {
/*		0			1			2			3			4			5			6			7			8			9			*/

         1			,34			,36			,39			,55			,1093		,1096		,1097		,1098		,1099   
		,1100		,1101		,1102		,1103		,1104		,1105		,1106		,1109		,1154		,1157		
		,1160		,1161		,1176       ,1254		,1257		,1768		,1773		,35105		,36933		,36936		
        ,36937		,36938		,36939		,36940		,36942		,36943		,36944		,36945		,36946		,36947		
        ,36948		,37642      ,40133		,40136		,40138		,40139		,40140		,40141		,1123363    ,35947624	
        ,35947625   ,35947626   ,35947627   ,35947629   ,35947630	,35947631   ,35947635   ,35947636   ,35947638};


const char* escSeqStateTable_Result_2022[MAX_STATES_2022] = {
/*		0		 1			 2						3						4						5						6		            	7			                8			9			*/

     NULL		,NULL		,NULL					,NULL					,NULL					,"latin1"				,"latin1"               ,"latin1"	            ,"ibm-865"  ,"ibm-865"    
	,"ibm-865"	,"ibm-865"	,"ibm-865"				,"ibm-865"				,"ibm-895"				,"JISX-201"				,"latin1"               ,"latin1"	            ,NULL		,"ibm-955"    
	,"GB2312"	,"JISX-208"	,NULL                   ,NULL					,"UTF8"					,"ISO-8859-1"			,"ISO-8859-7"			,NULL		            ,"ibm-955"  ,"bm-367"   
    ,"ibm-952"  ,"ibm-949"	,"JISX-212"				,"ibm-1383"				,"ibm-952"				,"ibm-964"				,"ibm-964"				,"ibm-964"              ,"ibm-964"  ,"ibm-964"     
    ,"ibm-964"  ,"ibm-949"  ,"UTF16_PlatformEndian" ,"UTF16_PlatformEndian" ,"UTF16_PlatformEndian"	,"UTF16_PlatformEndian"	,"UTF16_PlatformEndian"	,"UTF16_PlatformEndian"	,NULL		,"latin1"               
    ,"ibm-912"  ,"ibm-913"  ,"ibm-914"				,"ibm-813"				,"ibm-1089"             ,"ibm-920"	            ,"ibm-915"				,"ibm-915"				,"latin1"};


UCNV_TableStates_2022 escSeqStateTable_Value_2022[MAX_STATES_2022] = {
/*			0								1							2							3						4								5								6						7							8							9			*/
	
         VALID_NON_TERMINAL_2022	,VALID_NON_TERMINAL_2022    ,VALID_NON_TERMINAL_2022    ,VALID_NON_TERMINAL_2022	,VALID_NON_TERMINAL_2022	,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_MAYBE_TERMINAL_2022	,VALID_TERMINAL_2022		,VALID_TERMINAL_2022    
		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_NON_TERMINAL_2022    ,VALID_TERMINAL_2022    
		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_NON_TERMINAL_2022    ,VALID_NON_TERMINAL_2022    ,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_NON_TERMINAL_2022    ,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		
        ,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		
        ,VALID_TERMINAL_2022		,VALID_TERMINAL_2022        ,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_NON_TERMINAL_2022    ,VALID_TERMINAL_2022		
        ,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022		,VALID_TERMINAL_2022};



/*for 2022 looks ahead in the stream
 *to determine the longest possible convertible
 *data stream*/
static const char* getEndOfBuffer_2022(const char* source,
                                       const char* sourceLimit,
                                       UBool flush); 
/*runs through a state machine to determine the escape sequence - codepage correspondance
 *changes the pointer pointed to be _this->extraInfo*/
static  void changeState_2022(UConverter* _this,
                             const char** source, 
                             const char* sourceLimit,
                             UBool flush,
                             UErrorCode* err); 

UCNV_TableStates_2022 getKey_2022(char source,
                                  int32_t* key,
                                  int32_t* offset);

static void
_ISO2022Open(UConverter *cnv, const char *name, const char *locale, UErrorCode *errorCode) {
	void *oldContext, *newContext=NULL;
	UConverterFromUCallback *oldAction=malloc(sizeof(UConverterFromUCallback));
/*	cnv->charErrorBufferLength = 3;
    cnv->charErrorBuffer[0] = 0x1b;
    cnv->charErrorBuffer[1] = 0x25;
    cnv->charErrorBuffer[2] = 0x42;*/
    cnv->extraInfo = uprv_malloc (sizeof (UConverterDataISO2022));
    if(cnv->extraInfo != NULL) {
		UConverterDataISO2022 *myConverterData=(UConverterDataISO2022 *) cnv->extraInfo; 
		UConverter **array= myConverterData->myConverterArray;
        ((UConverterDataISO2022 *) cnv->extraInfo)->currentConverter = NULL;
		((UConverterDataISO2022 *) cnv->extraInfo)->previousConverter = NULL;
		((UConverterDataISO2022 *) cnv->extraInfo)->fromUnicodeConverter = NULL;
        ((UConverterDataISO2022 *) cnv->extraInfo)->escSeq2022Length = 0;
		cnv->fromUnicodeStatus =FALSE;
		if( (locale != NULL) && (uprv_strlen(locale) > 0) ) {
			myConverterData->currentLocale = (char*) uprv_malloc((sizeof(char) * uprv_strlen(locale)) + 1);
			uprv_strcpy(myConverterData->currentLocale,locale);
		}
		else {
			myConverterData->currentLocale = NULL;
		}

        myConverterData->myConverterArray[0] =NULL;
		if(locale && uprv_stricmp(locale,"jp")==0){
			myConverterData->myConverterArray[0]=	ucnv_open("ASCII", errorCode );
			myConverterData->myConverterArray[1]=	ucnv_open("ISO8859_1", errorCode);
			myConverterData->myConverterArray[2]=	ucnv_open("ISO8859_7", errorCode);
			myConverterData->myConverterArray[3]=	ucnv_open("jisx-201", errorCode);
			myConverterData->myConverterArray[4]=	ucnv_open("jisx-208", errorCode);
			myConverterData->myConverterArray[5]=	ucnv_open("jisx-212", errorCode);
			myConverterData->myConverterArray[6]=	ucnv_open("GB2312", errorCode);
			myConverterData->myConverterArray[7]=	ucnv_open("KSC5601", errorCode);

			myConverterData->myConverterArray[8]=	NULL;
			myConverterData->currentState =0;
			myConverterData->targetIndex = 0;
			myConverterData->sourceIndex =0;
			myConverterData->isEscapeAppended=FALSE;
			myConverterData->soAppended=FALSE;
			while(*array!=NULL){
				ucnv_setFromUCallBack(*array,UCNV_FROM_U_CALLBACK_STOP, 
				newContext, oldAction,&oldContext, errorCode);
				*array++;
			}
			myConverterData->isFirstBuffer = TRUE;
		}
        else if(locale && uprv_stricmp(locale,"kr")==0){
            cnv->charErrorBufferLength = 4;
            cnv->charErrorBuffer[0] = 0x1b;
            cnv->charErrorBuffer[1] = 0x24;
            cnv->charErrorBuffer[2] = 0x29;
            cnv->charErrorBuffer[3] = 0x43;
            ((UConverterDataISO2022 *) cnv->extraInfo)->fromUnicodeConverter  = ucnv_open("ibm-949",errorCode);
        }
        else{
            cnv->charErrorBufferLength = 3;
            cnv->charErrorBuffer[0] = 0x1b;
            cnv->charErrorBuffer[1] = 0x25;
            cnv->charErrorBuffer[2] = 0x42;
            myConverterData->currentLocale=NULL;
        }


		free(oldAction);
    } else {
        *errorCode = U_MEMORY_ALLOCATION_ERROR;
    }

}


static void
_ISO2022Close(UConverter *converter) {
	UConverter **array = ((UConverterDataISO2022 *) (converter->extraInfo))->myConverterArray;
    if (converter->extraInfo != NULL) {
        ucnv_close (((UConverterDataISO2022 *) (converter->extraInfo))->currentConverter);
		/*close the array of converter pointers and free the memory*/
        
		while(*array!=NULL){
			ucnv_close(*array++);
		}
		uprv_free(((UConverterDataISO2022 *) (converter->extraInfo))->currentLocale);
        uprv_free (converter->extraInfo);
    }
}
/*** ??? why are we going to UTF-8??*/
static void
_ISO2022Reset(UConverter *converter) {
    if(!((UConverterDataISO2022 *) (converter->extraInfo))->currentLocale){
    	converter->charErrorBufferLength = 3;
        converter->charErrorBuffer[0] = 0x1b;
        converter->charErrorBuffer[1] = 0x28;
        converter->charErrorBuffer[2] = 0x42;
    }
  if (converter->mode == UCNV_SO)
    {
 /*     converter->charErrorBufferLength = 3;
      converter->charErrorBuffer[0] = 0x1b;
      converter->charErrorBuffer[1] = 0x25;
      converter->charErrorBuffer[2] = 0x42;*/
      ucnv_close (((UConverterDataISO2022 *) (converter->extraInfo))->currentConverter);
      ((UConverterDataISO2022 *) (converter->extraInfo))->currentConverter = NULL;
      ((UConverterDataISO2022 *) (converter->extraInfo))->escSeq2022Length = 0;
      converter->mode = UCNV_SI;
    }
}


U_CFUNC void T_UConverter_fromUnicode_ISO_2022(UConverterFromUnicodeArgs *args,
                                       UErrorCode* err)
{	
	UConverterDataISO2022 *myConverterData=(UConverterDataISO2022*)args->converter->extraInfo;
	const char *locale =myConverterData->currentLocale;
	if(locale && uprv_stricmp(locale,"jp")==0){
			T_UConverter_fromUnicode_ISO_2022_JP(args,err);
    }
    else if(locale && uprv_stricmp(locale,"kr")==0){
        UConverter_fromUnicode_ISO_2022_KR(args,err);
    }
    else{
        T_UConverter_fromUnicode_UTF8(args, err);
    }
}


U_CFUNC void T_UConverter_fromUnicode_ISO_2022_OFFSETS_LOGIC(UConverterFromUnicodeArgs* args,
                                                                    UErrorCode* err)
{
    
	  char const* targetStart = args->target;
	UConverterDataISO2022 *myConverterData=(UConverterDataISO2022*)args->converter->extraInfo;
	const char *locale =myConverterData->currentLocale;
	if(locale && uprv_stricmp(locale,"jp")==0){
		T_UConverter_fromUnicode_ISO_2022_JP(args,err);
    }
	else{
      T_UConverter_fromUnicode_UTF8_OFFSETS_LOGIC(args, err);
      {

		int32_t len = args->target - targetStart;
		int32_t i;
		/* uprv_memmove(offsets+3, offsets, len);   MEMMOVE SEEMS BROKEN --srl*/ 

		for(i=len-1;i>=0;i--)       args->offsets[i] = args->offsets[i];
      }

  }
}

/*************************** BEGIN ****************************/

/************************************** IMPORTANT **************************************************
 * The T_fromUnicode_ISO2022_JP converter doesnot use ucnv_fromUnicode() functions for SBCS and DBCS, 
 * instead the values are obtained directly by accessing the sharedData structs through ucmp8_getU() 
 * and ucmp16_getU() macros to increase speed, reduce the overhead of function call and make it 
 * efficient.The converter iterates over each Unicode codepoint to obtain the equivalent codepoints 
 * from the codepages supported. Since the source buffer is processed one char at a time it would 
 * make sense to reduce the extra processing a canned converter would do as far as possible.
 *
 * If the implementation of these macros or structure of sharedData struct change in the future, make 
 * sure that ISO-2022-JP is also changed. 
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
 *	     and invoked with SS2 (ESC N).
 * (vi)	 If there is any G0 designation in text, there must be a switch to
 *	     ASCII or to JIS X 0201-Roman before a space character (but not
 *       necessarily before "ESC 4/14 2/0" or "ESC N ' '") or control
 *       characters such as tab or CRLF.
 * (vi)  Supported encodings:
 *			ASCII, JISX201, JISX208, JISX212, GB2312, KSC5601, ISO-8859-1,ISO-8859-7
 *
 *	source : RFC-1554
 *
 *			JISX201, JISX208,JISX212 : new .cnv data files created
 *			KSC5601 : alias to ibm-949 mapping table
 *			GB2312 : alias to ibm-1386 mapping table	
 *			ISO-8859-1 : Algorithmic implemented as LATIN1 case
 *          ISO-8859-7 : alisas to ibm-9409 mapping table
 */


static Cnv2022Type myConverterType[8]={
	SBCS,
	LATIN1,
	SBCS,
	SBCS,
	DBCS,
	DBCS,
	MBCS,
	MBCS,

};
#define UCNV_SS2 "\x0BN"

#define ESC 0x0B

static const char* escSeqChars[8] ={
	"\x1B\x28\x42",
	"\x1B\x2E\x41",
	"\x1B\x2E\x46",
	"\x1B\x28\x4A", 
	"\x1B\x24\x42",
	"\x1B\x24\x28\x44",
	"\x1B\x24\x41", 
	"\x1B\x24\x29\x43", 

};


static void concatChar(UConverterFromUnicodeArgs* args, int32_t *targetIndex, int32_t *targetLength,
				  int8_t charToAppend,UErrorCode* err);

static void concatEscape(UConverterFromUnicodeArgs* args, int32_t *targetIndex, int32_t *targetLength,
				  const char* strToAppend,UErrorCode* err,int len);

static void concatString(UConverterFromUnicodeArgs* args, int32_t *targetIndex, int32_t *targetLength,
				  const UChar* strToAppend,UErrorCode* err,int32_t *sourceIndex);

/*
 * The iteration over various code pages works this way:
 * i)   Get the currentState from myConverterData->currentState
 * ii)  Check if the character is mapped to a valid character in the currentState
 *		Yes ->  a) set the initIterState to currentState
 *				b) remain in this state until an invalid character is found
 *		No  ->  a) go to the next code page and find the character
 * iii) Before changing the state increment the current state check if the current state 
 *      is equal to the intitIteration state
 *		Yes ->  A character that cannot be represented in any of the supported encodings
 *				break and return a U_INVALID_CHARACTER error
 *		No  ->  Continue and find the character in next code page
 *
 * Offsets Logic is handled by utility functions concatChar(), concatEscape() and concatString()
 *
 */


U_CFUNC void T_UConverter_fromUnicode_ISO_2022_JP(UConverterFromUnicodeArgs* args, UErrorCode* err){
	UChar* mySource =(UChar*)args->source;

	UConverterDataISO2022 *myConverterData=(UConverterDataISO2022*)args->converter->extraInfo;
	UConverterCallbackReason reason;
	UBool isEscapeAppended = FALSE;
	StateEnum initIterState;
	unsigned char *myTarget = (unsigned char *) args->target;
	
	char *uBuf =(char*) malloc(sizeof(char) * 4);
	char *targetChar;
	char *targetLimit; 
	
	const UChar *saveSource;
	char *saveTarget;
	int32_t *saveOffsets ;
	const UChar* mySourceLimit;
	int32_t myTargetLength = args->targetLimit - args->target;
	int32_t mySourceLength = args->sourceLimit - args->source;
	int32_t mySourceIndex = 0;
	int32_t myTargetIndex = 0;

	CompactShortArray *myFromUnicodeDBCS = NULL;
	CompactShortArray *myFromUnicodeDBCSFallback = NULL;
	CompactByteArray  *myFromUnicodeSBCS = NULL;
	CompactByteArray  *myFromUnicodeSBCSFallback = NULL;
	UChar targetUniChar = missingCharMarker;
	
	StateEnum currentState=0;
	Cnv2022Type myType;
	UChar mySourceChar = 0x0000;
	const UChar *sourceCharPtr=NULL;
	int iterCount = 0;
	const char *escSeq = NULL;
	UBool soAppended = FALSE;
	UBool isTargetUCharDBCS=FALSE,oldIsTargetUCharDBCS=FALSE; 

	mySourceIndex = myConverterData->sourceIndex;
	myTargetIndex = myConverterData->targetIndex;
	isEscapeAppended =(UBool) myConverterData->isEscapeAppended;
	soAppended =(UBool) myConverterData->soAppended;
	initIterState =0;
	/* arguments check*/
	if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source)){
	  *err = U_ILLEGAL_ARGUMENT_ERROR;
	  return;
	}

	while(mySourceIndex <  mySourceLength){
		currentState = myConverterData->currentState;
		myConverterData->fromUnicodeConverter = (myConverterData->fromUnicodeConverter == NULL) ?
											myConverterData->myConverterArray[0] :
											myConverterData->myConverterArray[(int)myConverterData->currentState];
		isTargetUCharDBCS	= (UBool) args->converter->fromUnicodeStatus;

		if(myTargetIndex < myTargetLength){

			mySourceChar = (UChar) args->source[mySourceIndex++];

			myType= (Cnv2022Type) myConverterType[currentState];

			/* I am handling surrogates in the begining itself so that I donot have to go through 8 
			 * iterations on codepages that we support. 
			 */
			if(UTF_IS_LEAD(mySourceChar)){
					
				args->converter->invalidUCharBuffer[0] = (UChar)mySource[mySourceIndex - 1];
				args->converter->invalidUCharLength = 1;

				/*mySourceIndex has already been incremented*/
				if(mySourceIndex < mySourceLength){
					if(UTF_IS_TRAIL(mySource[mySourceIndex])){
						  args->converter->invalidUCharBuffer[1] = (UChar)mySource[mySourceIndex];
                          args->converter->invalidUCharLength++;
                          mySourceIndex++;
						  continue;
					}
					else if (args->flush == TRUE){
						reason = UCNV_ILLEGAL;
						*err = U_TRUNCATED_CHAR_FOUND;
						goto CALLBACK;
					} 
					else{
						reason=UCNV_ILLEGAL;
						goto CALLBACK;
					}
				}
				else{
					args->converter->invalidUCharBuffer[1] = (UChar)mySource[mySourceIndex];
					continue;
				}
			}
			else if(UTF_IS_TRAIL(mySourceChar)){
				if(args->converter->fromUSurrogateLead == 0){
					reason = UCNV_ILLEGAL;
					goto CALLBACK;
				}
				else{
					/* the only way we can arrive here is if UTF lead surrogate was found
					 * at the end of previous buffer. So we need to check if the current
					 * current source index is 0 or not 
					 */
					if(mySourceIndex-1 ==0){
						  args->converter->invalidUCharBuffer[1] = (UChar)mySource[mySourceIndex];
                          args->converter->invalidUCharLength++;
                          mySourceIndex++;
						  continue;
					}
					else{
						reason=UCNV_ILLEGAL;
						goto CALLBACK;
					}
				}
			}

			/*Do the conversion*/
			else if(mySourceChar == 0x0020){
				targetUniChar = mySourceChar;
				if(currentState > 2){
					concatEscape(args, &myTargetIndex, &myTargetLength, escSeqChars[0],err,strlen(escSeqChars[0]));
					
					if(*err ==U_BUFFER_OVERFLOW_ERROR){
						/*save the state and return */
						args->target += myTargetIndex;
						args->source += mySourceIndex;
						myConverterData->sourceIndex = 0;
						myConverterData->targetIndex = 0;
						args->converter->fromUnicodeStatus = isTargetUCharDBCS;

						return;
					}
				}
			}
			/* if the source character is CR or LF then append the ASCII escape sequence*/
			else if(mySourceChar== 0x000A || mySourceChar== 0x000D){

				if(isTargetUCharDBCS && mySource[mySourceIndex-2]!=0x000A){
					concatEscape(args, &myTargetIndex, &myTargetLength, escSeqChars[0],err,strlen(escSeqChars[0]));
					isTargetUCharDBCS=FALSE;
					soAppended =FALSE;
					myConverterData->soAppended=FALSE;

					if(*err ==U_BUFFER_OVERFLOW_ERROR){
						/*save the state and return */
						args->target += myTargetIndex;
						args->source += mySourceIndex;
						myConverterData->sourceIndex = 0;
						myConverterData->targetIndex = 0;
						args->converter->fromUnicodeStatus = isTargetUCharDBCS;

						return;
					}
				}

				targetUniChar = mySourceChar;
				concatString(args, &myTargetIndex, &myTargetLength,&targetUniChar,err,&mySourceIndex);

				if(currentState==ISO8859_1 || currentState ==ISO8859_7)
					isEscapeAppended =FALSE;
				
				if(*err ==U_BUFFER_OVERFLOW_ERROR)
					break;
				continue;
			}
			else{
			
				switch (myType){

					case SBCS:
						myFromUnicodeSBCS = &myConverterData->fromUnicodeConverter->sharedData->table->sbcs.fromUnicode;
						myFromUnicodeSBCSFallback = &myConverterData->fromUnicodeConverter->sharedData->table->sbcs.fromUnicodeFallback;

						targetUniChar = (UChar) ucmp8_getu (myFromUnicodeSBCS, mySourceChar);

						if ((targetUniChar==0)&&(myConverterData->fromUnicodeConverter->useFallback == TRUE) &&
							(myConverterData->fromUnicodeConverter->sharedData->staticData->hasFromUnicodeFallback == TRUE)){
							 targetUniChar = (UChar) ucmp8_getu (myFromUnicodeSBCSFallback, mySourceChar);
						}
						/* ucmp8_getU returns 0 for missing char so explicitly set it missingCharMarker*/
						targetUniChar=(UChar)((targetUniChar==0) ? (UChar) missingCharMarker : targetUniChar);
						break;
						
					case DBCS:
						
						myFromUnicodeDBCS = &myConverterData->fromUnicodeConverter->sharedData->table->dbcs.fromUnicode;
						myFromUnicodeDBCSFallback = &myConverterData->fromUnicodeConverter->sharedData->table->dbcs.fromUnicodeFallback;
						targetUniChar = (UChar) ucmp16_getu (myFromUnicodeDBCS, mySourceChar);
						
						if ((targetUniChar==missingCharMarker)&&(myConverterData->fromUnicodeConverter->useFallback == TRUE) &&
								(myConverterData->fromUnicodeConverter->sharedData->staticData->hasFromUnicodeFallback == TRUE)){
							 targetUniChar = (UChar) ucmp16_getu (myFromUnicodeDBCSFallback, mySourceChar);
						}
						break;

					case MBCS:
						
						sourceCharPtr =&mySourceChar;
						mySourceLimit= sourceCharPtr+1;
						targetChar =uBuf;
						targetLimit = uBuf+4;

						ucnv_fromUnicode(myConverterData->fromUnicodeConverter,
										&targetChar,targetLimit,
										&sourceCharPtr,
										mySourceLimit,args->offsets,args->flush,err);
						if(U_FAILURE(*err)){
							targetUniChar = missingCharMarker;
							*err =U_ZERO_ERROR;
						}else{
							/*convert to targetUniChar*/						
							uint8_t len=(uint8_t)(targetChar-uBuf);
							targetChar=uBuf;
							targetUniChar=0;
					
							/* the below switch structure is not required since IS2022-JP-2
							 * supports only DBCS char sets. Reverse engineered from DBCS code.
							 * the cases fall through without break
							 */
							switch(len){
								case 4:
									targetUniChar+=(uint32_t)((uint8_t)(*targetChar++))<<24;
								case 3:
									targetUniChar+=(uint32_t)((uint8_t)(*targetChar++))<<16;
								case 2:
									targetUniChar+=(uint32_t)((uint8_t)(*targetChar++))<<8;
								case 1:
									targetUniChar+=(uint8_t)(*targetChar);
								default:
									break;
							}

						}

						break;

					case LATIN1:
						if(mySourceChar < 0x0100){
							targetUniChar = mySourceChar;
						} else targetUniChar = missingCharMarker;
						break;

					default:
						/*not expected */ 
						break;
				}
			}

			if(targetUniChar!= missingCharMarker){
				
				oldIsTargetUCharDBCS = isTargetUCharDBCS;
				isTargetUCharDBCS =(UBool) (targetUniChar >0x00FF);
				args->converter->fromUnicodeStatus= isTargetUCharDBCS;
				/* set the iteration state and iteration count	*/			
				initIterState = currentState;
				iterCount =0;
				/* Append the escpace sequence */
				if(!isEscapeAppended){
					escSeq = escSeqChars[(int)currentState];
					concatEscape(args, &myTargetIndex, &myTargetLength, 
										escSeqChars[(int)currentState],
										err,strlen(escSeqChars[(int)currentState]));
					
					isEscapeAppended =TRUE;
					myConverterData->isEscapeAppended=TRUE;
					if(*err ==U_BUFFER_OVERFLOW_ERROR){
						/*save the state and return */
						args->target += myTargetIndex;
						args->source += mySourceIndex;
						myConverterData->sourceIndex = 0;
						myConverterData->targetIndex = 0;
						args->converter->fromUnicodeStatus = isTargetUCharDBCS;

						return;
					}

					/* Append SSN for shifting to G2 */
					if(currentState==ISO8859_1 || currentState==ISO8859_7){
						concatEscape(args, &myTargetIndex, &myTargetLength,
									UCNV_SS2,err,strlen(UCNV_SS2));
					

						if(*err ==U_BUFFER_OVERFLOW_ERROR){
							/*save the state and return */
							args->target += myTargetIndex;
							args->source += mySourceIndex;
							myConverterData->sourceIndex = 0;
							myConverterData->targetIndex = 0;
							args->converter->fromUnicodeStatus = isTargetUCharDBCS;

							return;
						}
					}
				}
				else{

					if(oldIsTargetUCharDBCS != isTargetUCharDBCS  ){
						/*Shifting from a double byte to single byte mode*/
						if(!isTargetUCharDBCS){

							concatChar(args, &myTargetIndex, 
										&myTargetLength, UCNV_SI,err);
							soAppended =FALSE;
							if(*err ==U_BUFFER_OVERFLOW_ERROR){
								/*save the state and return */
								args->target += myTargetIndex;
								args->source += mySourceIndex;
								myConverterData->sourceIndex = 0;
								myConverterData->targetIndex = 0;
								args->converter->fromUnicodeStatus = isTargetUCharDBCS;

								return;
							}
						}
						else{ /* Shifting from a single byte to double byte mode*/
								concatChar(args, &myTargetIndex, 
									&myTargetLength, UCNV_SO,err);
							soAppended =TRUE;
							myConverterData->soAppended =soAppended;
							if(*err ==U_BUFFER_OVERFLOW_ERROR){
								/*save the state and return */
								args->target += myTargetIndex;
								args->source += mySourceIndex;
								myConverterData->sourceIndex = 0;
								myConverterData->targetIndex = 0;
								args->converter->fromUnicodeStatus = isTargetUCharDBCS;

								return;
							}
						}
					}
				}

				concatString(args, &myTargetIndex, &myTargetLength,
								&targetUniChar,err, &mySourceIndex);
					if(*err ==U_BUFFER_OVERFLOW_ERROR){
						/*save the state and return */
						args->target += myTargetIndex;
						args->source += mySourceIndex;
						myConverterData->sourceIndex = 0;
						myConverterData->targetIndex = 0;
						args->converter->fromUnicodeStatus = isTargetUCharDBCS;

						return;
					}

			}/* end of end if(targetUniChar==missingCharMarker)*/
			else{
				myConverterData->currentState=currentState=(currentState<7)? currentState+1:0;
				iterCount = (iterCount<8)? iterCount+1 : 0;
				
				if((currentState!= initIterState) ){

					/* explicitly decrement source since it has already been incremented */
					mySourceIndex--;
					targetUniChar =missingCharMarker;
					isEscapeAppended = FALSE; 
					/* save the state */
					myConverterData->isEscapeAppended = isEscapeAppended;
					myConverterData->soAppended =soAppended;
					args->converter->fromUnicodeStatus = isTargetUCharDBCS;
					myConverterData->sourceIndex = mySourceIndex;
					myConverterData->targetIndex = myTargetIndex;
					continue;
				}
				else{
					/* if we cannot find the character after checking all codepages 
					 * then this is an error
					 */
					reason = UCNV_UNASSIGNED;
					 *err = U_INVALID_CHAR_FOUND;

CALLBACK:
					saveSource = args->source;
					saveTarget = args->target;
					saveOffsets = args->offsets;
					args->target = (char*)myTarget + myTargetIndex;
					args->source = mySource + mySourceIndex;
					myConverterData->isEscapeAppended = isEscapeAppended;
					myConverterData->soAppended =soAppended;
					args->converter->fromUnicodeStatus = isTargetUCharDBCS;
					myConverterData->sourceIndex = mySourceIndex;
					myConverterData->targetIndex = myTargetIndex;

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

					args->source = saveSource;
					args->target = saveTarget;
					args->offsets = saveOffsets;
					if (U_FAILURE (*err)){
					  break;
					}
					args->converter->invalidUCharLength = 0;
				}
			}
		} /* end if(myTargetIndex<myTargetLength) */
		else{
			*err =U_BUFFER_OVERFLOW_ERROR;
			break;
		} 

	}/* end while(mySourceIndex<mySourceLength) */
	
	free(uBuf); /* free the malloced memory */

	/*save the state and return */
	args->target += myTargetIndex;
	args->source += mySourceIndex;
	myConverterData->sourceIndex = 0;
	myConverterData->targetIndex = 0;
	args->converter->fromUnicodeStatus = isTargetUCharDBCS;

}

static void concatString(UConverterFromUnicodeArgs* args, int32_t *targetIndex, int32_t *targetLength,
				  const UChar* strToAppend,UErrorCode* err, int32_t *sourceIndex){

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
					args->offsets[*targetIndex] = *sourceIndex-1;
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
				  const char* strToAppend,UErrorCode* err,int len){
	while(len-->0){
		if(*targetIndex < *targetLength){
			args->target[(*targetIndex)++] = (unsigned char) *strToAppend;
		}
		else{
			args->converter->charErrorBuffer[(int)args->converter->charErrorBufferLength++] = (unsigned char) *strToAppend;
			*err =U_BUFFER_OVERFLOW_ERROR;
		}
		strToAppend++;
	}
}
	
static void concatChar(UConverterFromUnicodeArgs* args, int32_t *targetIndex, int32_t *targetLength,
				  int8_t charToAppend,UErrorCode* err){
	if( *targetIndex < *targetLength){
		args->target[(*targetIndex)++] = (unsigned char) charToAppend;
	}else{
		args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (unsigned char) charToAppend;
		*err = U_BUFFER_OVERFLOW_ERROR;
	}
}

/*************** to unicode *******************/

UCNV_TableStates_2022 getKey_2022(char c,
                                  int32_t* key,
                                  int32_t* offset){
	int32_t togo = *key;
	int32_t low = 0;
	int32_t hi = MAX_STATES_2022;
	int32_t oldmid=0;

	if (*key == 0){
		togo = (int8_t)normalize_esq_chars_2022[c];
	}
	else{
		togo <<= 5;
		togo += (int8_t)normalize_esq_chars_2022[c];
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



void changeState_2022(UConverter* _this,
                      const char** source, 
                      const char* sourceLimit,
                      UBool flush,
                      UErrorCode* err){
	UConverter* myUConverter;
	uint32_t key = _this->toUnicodeStatus;
	UCNV_TableStates_2022 value;
	UConverterDataISO2022* myData2022 = ((UConverterDataISO2022*)_this->extraInfo);
	const char* chosenConverterName = NULL;
	
	int32_t offset;

	/*In case we were in the process of consuming an escape sequence
	we need to reprocess it */

	do{

		/* Needed explicit cast for key on MVS to make compiler happy - JJD */
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
					_this->toUnicodeStatus = 0;
					*err = U_ILLEGAL_CHAR_FOUND;
					return;
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
						myValue=1;
						myOffset = 5;
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
	_this->toUnicodeStatus = key;

	if ((value == VALID_NON_TERMINAL_2022) || (value == VALID_MAYBE_TERMINAL_2022)) {
	  return;
	}
	if (value > 0) {
		if(uprv_strcmp(chosenConverterName,"latin1")==0 && uprv_strcmp(myData2022->currentLocale,"jp")==0){
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
					   myUConverter->subCharLen = _this->subCharLen);

			_this->mode = UCNV_SO;
		}
	}

	return;
}

/*Checks the characters of the buffer against valid 2022 escape sequences
 *if the match we return a pointer to the initial start of the sequence otherwise
 *we return sourceLimit
 */
const char* getEndOfBuffer_2022(const char* source,
                                const char* sourceLimit,
                                UBool flush){

	const char* mySource = source;

	if (source >= sourceLimit) 
		return sourceLimit;

	do{

		if (*mySource == ESC_2022){
			int8_t i;
			int32_t key = 0;
			int32_t offset;
			UCNV_TableStates_2022 value = VALID_NON_TERMINAL_2022;
            /* check for SS2*/
            if(*mySource+1 == 0x4E){
                if(mySource == source){
				    source++;
                    source++;
				    return getEndOfBuffer_2022(source,sourceLimit,flush);
			    }
			    else{
				    return mySource;
			    }
            }
  
			for (i=0; 
					(mySource+i < sourceLimit)&&(value == VALID_NON_TERMINAL_2022);
					i++) {
			  value =  getKey_2022(*(mySource+i), &key, &offset);
			}
		  
			if (value > 0) 
			  return mySource;

			if ((value == VALID_NON_TERMINAL_2022)&&(!flush) ) 
				return sourceLimit;
		}
		else if(*mySource == (char)UCNV_SI || *mySource==(char)UCNV_SO){
			if(mySource == source){
				source++;
				return getEndOfBuffer_2022(source,sourceLimit,flush);
			}
			else{
				return mySource;
			}
		}
	
	}while (mySource++ < sourceLimit);

	return sourceLimit;
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
    if(((UConverterDataISO2022*)(args->converter->extraInfo))->currentLocale){
        if(uprv_strcmp(((UConverterDataISO2022*)(args->converter->extraInfo))->currentLocale, "kr") ==0){
            UConverter_toUnicode_ISO_2022_KR(args,err);
            return;
        }
    }

	do{
		
		/*Find the end of the buffer e.g : Next Escape Seq | end of Buffer*/
		mySourceLimit =  getEndOfBuffer_2022(args->source, args->sourceLimit, args->flush); 
		
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
		/* in ISO-2022-jp the characters preceeding any escape sequence are assumed
		 * to be ASCII so we need to explicitly check for it
		 */
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
	UConverter* _this = NULL;

	/*Arguments Check*/
	if (U_FAILURE(*err)) 
		return;
	if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source)){
		*err = U_ILLEGAL_ARGUMENT_ERROR;
		return;
	}

	do{
		mySourceLimit =  getEndOfBuffer_2022(args->source, args->sourceLimit, args->flush); 
		/*Find the end of the buffer e.g : Next Escape Seq | end of Buffer*/

		if (args->converter->mode == UCNV_SO) /*Already doing some conversion*/{
			const UChar* myTargetStart = args->target;

			_this = args->converter;
			args->converter = ((UConverterDataISO2022*)(_this->extraInfo))->currentConverter;
			ucnv_toUnicode(args->converter, 
							&(args->target),
							args->targetLimit,
							&(args->source),
							mySourceLimit,
							args->offsets,
							args->flush,
							err);

			args->converter = _this;
			{
				int32_t lim =  args->target - myTargetStart;
				int32_t i = 0;
				for (i=base; i < lim;i++){   
					args->offsets[i] += myOffset;
				}
				base += lim;
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
						args->converter = _this;
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
						args->flush,
						err);
		myOffset += args->source - sourceStart;

	}while(mySourceLimit != args->sourceLimit);
	
	return;
}

/******************************* END *****************************/


/***************************************************************
 *   Rules for ISO-2022-KR encoding
 *   i) The KSC5601 designator sequence should appear only once in a file, 
 *      at the begining of a line before any KSC5601 characters. This usually
 *      means that it appears by itself on the first line of the file
 *  ii) There are only 2 shifting sequences SO to shift into double byte mode
 *      and SI to shift into single byte mode   
 */

U_CFUNC void UConverter_fromUnicode_ISO_2022_KR(UConverterFromUnicodeArgs* args, UErrorCode* err){

    const UChar *mySource = args->source;
    unsigned char *myTarget = (unsigned char *) args->target;
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    int32_t targetLength = args->targetLimit - args->target;
    int32_t sourceLength = args->sourceLimit - args->source;
    UChar* mySourceLimit;
    CompactShortArray *myFromUnicode = NULL;
    UChar targetUniChar = 0x0000;
    UChar mySourceChar = 0x0000;
    UBool isTargetUCharDBCS = (UBool)args->converter->fromUnicodeStatus;
    UBool oldIsTargetUCharDBCS = isTargetUCharDBCS;
    UConverterDataISO2022 *myConverterData=(UConverterDataISO2022*)args->converter->extraInfo;
    UConverterCallbackReason reason;
    
	char *uBuf =(char*) malloc(sizeof(char) * 4);
	char *targetChar;
	char *targetLimit; 
	uint8_t len;
    UChar *sourceCharPtr=NULL;
    isTargetUCharDBCS	= (UBool) args->converter->fromUnicodeStatus;
    
    /*writing the char to the output stream */
    while (mySourceIndex < sourceLength){

        

        if (myTargetIndex < targetLength){
        
            mySourceChar = (UChar) args->source[mySourceIndex++];

            /*Handle surrogates */
            if(UTF_IS_LEAD(mySourceChar)){
				    
			    args->converter->invalidUCharBuffer[0] = (UChar)mySource[mySourceIndex - 1];
			    args->converter->invalidUCharLength = 1;

			    /*mySourceIndex has already been incremented*/
			    if(mySourceIndex < sourceLength){
				    if(UTF_IS_TRAIL(mySource[mySourceIndex])){
					      args->converter->invalidUCharBuffer[1] = (UChar)mySource[mySourceIndex];
                          args->converter->invalidUCharLength++;
                          mySourceIndex++;
					      continue;
				    }
				    else if (args->flush == TRUE){
					    reason = UCNV_ILLEGAL;
					    *err = U_TRUNCATED_CHAR_FOUND;
					    goto CALLBACK;
				    } 
				    else{
					    reason=UCNV_ILLEGAL;
					    goto CALLBACK;
				    }
			    }
                else{
				    args->converter->invalidUCharBuffer[1] = (UChar)mySource[mySourceIndex];
				    continue;
			    }
		    }
            else if(UTF_IS_TRAIL(mySourceChar)){
			    if(args->converter->fromUSurrogateLead == 0){
				    reason = UCNV_ILLEGAL;
				    goto CALLBACK;
			    }
			    else{
				    /* the only way we can arrive here is if UTF lead surrogate was found
				     * at the end of previous buffer. So we need to check if the current
				     * current source index is 0 or not 
				     */
				    if(mySourceIndex-1 ==0){
					      args->converter->invalidUCharBuffer[1] = (UChar)mySource[mySourceIndex];
                          args->converter->invalidUCharLength++;
                          mySourceIndex++;
					      continue;
				    }
				    else{
					    reason=UCNV_ILLEGAL;
					    goto CALLBACK;
				    }
			    }
		    }
        
            
            sourceCharPtr =&mySourceChar;
		    mySourceLimit= sourceCharPtr+1;
		    targetChar =uBuf;
		    targetLimit = uBuf+4;

      
            ucnv_fromUnicode(myConverterData->fromUnicodeConverter,
									    &targetChar,targetLimit,
									    &sourceCharPtr,
									    mySourceLimit,args->offsets,args->flush,err);
            if(U_SUCCESS(*err)){
                len=(uint8_t)(targetChar-uBuf);
                targetChar=uBuf;
                targetUniChar=0;
                switch(len){
			        case 4:
				        targetUniChar+=(uint32_t)((uint8_t)(*targetChar++))<<24;
			        case 3:
				        targetUniChar+=(uint32_t)((uint8_t)(*targetChar++))<<16;
			        case 2:
				        targetUniChar+=(uint32_t)((uint8_t)(*targetChar++))<<8;
			        case 1:
				        targetUniChar+=(uint8_t)(*targetChar);
			        default:
				        break;
		        }
            }
            else{
                targetUniChar=missingCharMarker;
            }

      
            oldIsTargetUCharDBCS = isTargetUCharDBCS;
            isTargetUCharDBCS = (UBool)(targetUniChar>0x00FF);

            if (targetUniChar != missingCharMarker){

              if (oldIsTargetUCharDBCS != isTargetUCharDBCS){

                  if (isTargetUCharDBCS) 
                      args->target[myTargetIndex++] = UCNV_SO;
                  else 
                      args->target[myTargetIndex++] = UCNV_SI;


                  if ((!isTargetUCharDBCS)&&(myTargetIndex+1 >= targetLength)){
                      args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (char) targetUniChar;
                      *err = U_BUFFER_OVERFLOW_ERROR;
                      break;
                  }
                  else if (myTargetIndex+1 >= targetLength){

                      args->converter->charErrorBuffer[0] = (char) (targetUniChar >> 8);
                      args->converter->charErrorBuffer[1] = (char)(targetUniChar & 0x00FF);
                      args->converter->charErrorBufferLength = 2;
                      *err = U_BUFFER_OVERFLOW_ERROR;
                      break;
                  }

              }

              if (!isTargetUCharDBCS){

                  args->target[myTargetIndex++] = (char) targetUniChar;
              }
              else{
                  args->target[myTargetIndex++] = (char) (targetUniChar >> 8);
                  if (myTargetIndex < targetLength){
                      args->target[myTargetIndex++] = (char) targetUniChar;
                  }
                  else{
                      args->converter->charErrorBuffer[0] = (char) targetUniChar;
                      args->converter->charErrorBufferLength = 1;
                      *err = U_BUFFER_OVERFLOW_ERROR;
                      break;
                  }
              }
            }
            else{

CALLBACK:
                {
                  const UChar* saveSource = args->source;
                  char* saveTarget = args->target;
                  int32_t *saveOffsets = args->offsets;

                  isTargetUCharDBCS = oldIsTargetUCharDBCS;
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
                  isTargetUCharDBCS  = (UBool) args->converter->fromUnicodeStatus;
                  if (U_FAILURE (*err)) break;
                  args->converter->invalidUCharLength = 0;
                }
            }
        }
        else{
            *err = U_BUFFER_OVERFLOW_ERROR;
            break;
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
    UChar* mySourceLimit;
    CompactShortArray *myFromUnicode = NULL;
    UChar targetUniChar = 0x0000;
    UChar mySourceChar = 0x0000;
    UBool isTargetUCharDBCS = (UBool)args->converter->fromUnicodeStatus;
    UBool oldIsTargetUCharDBCS = isTargetUCharDBCS;
    UConverterDataISO2022 *myConverterData=(UConverterDataISO2022*)args->converter->extraInfo;
    UConverterCallbackReason reason;
    
	char *uBuf =(char*) malloc(sizeof(char) * 4);
	char *targetChar;
	char *targetLimit; 
	uint8_t len;
    UChar *sourceCharPtr=NULL;
    isTargetUCharDBCS	= (UBool) args->converter->fromUnicodeStatus;
    
    /*writing the char to the output stream */
    while (mySourceIndex < sourceLength){

        

        if (myTargetIndex < targetLength){
        
            mySourceChar = (UChar) args->source[mySourceIndex++];

            /*Handle surrogates */
            if(UTF_IS_LEAD(mySourceChar)){
				    
			    args->converter->invalidUCharBuffer[0] = (UChar)mySource[mySourceIndex - 1];
			    args->converter->invalidUCharLength = 1;

			    /*mySourceIndex has already been incremented*/
			    if(mySourceIndex < sourceLength){
				    if(UTF_IS_TRAIL(mySource[mySourceIndex])){
					      args->converter->invalidUCharBuffer[1] = (UChar)mySource[mySourceIndex];
                          args->converter->invalidUCharLength++;
                          mySourceIndex++;
					      continue;
				    }
				    else if (args->flush == TRUE){
					    reason = UCNV_ILLEGAL;
					    *err = U_TRUNCATED_CHAR_FOUND;
					    goto CALLBACK;
				    } 
				    else{
					    reason=UCNV_ILLEGAL;
					    goto CALLBACK;
				    }
			    }
                else{
				    args->converter->invalidUCharBuffer[1] = (UChar)mySource[mySourceIndex];
				    continue;
			    }
		    }
            else if(UTF_IS_TRAIL(mySourceChar)){
			    if(args->converter->fromUSurrogateLead == 0){
				    reason = UCNV_ILLEGAL;
				    goto CALLBACK;
			    }
			    else{
				    /* the only way we can arrive here is if UTF lead surrogate was found
				     * at the end of previous buffer. So we need to check if the current
				     * current source index is 0 or not 
				     */
				    if(mySourceIndex-1 ==0){
					      args->converter->invalidUCharBuffer[1] = (UChar)mySource[mySourceIndex];
                          args->converter->invalidUCharLength++;
                          mySourceIndex++;
					      continue;
				    }
				    else{
					    reason=UCNV_ILLEGAL;
					    goto CALLBACK;
				    }
			    }
		    }
        
            
            sourceCharPtr =&mySourceChar;
		    mySourceLimit= sourceCharPtr+1;
		    targetChar =uBuf;
		    targetLimit = uBuf+4;

      
            ucnv_fromUnicode(myConverterData->fromUnicodeConverter,
									    &targetChar,targetLimit,
									    &sourceCharPtr,
									    mySourceLimit,args->offsets,args->flush,err);
            if(U_SUCCESS(*err)){
                len=(uint8_t)(targetChar-uBuf);
                targetChar=uBuf;
                targetUniChar=0;
                switch(len){
			        case 4:
				        targetUniChar+=(uint32_t)((uint8_t)(*targetChar++))<<24;
			        case 3:
				        targetUniChar+=(uint32_t)((uint8_t)(*targetChar++))<<16;
			        case 2:
				        targetUniChar+=(uint32_t)((uint8_t)(*targetChar++))<<8;
			        case 1:
				        targetUniChar+=(uint8_t)(*targetChar);
			        default:
				        break;
		        }
            }
            else{
                targetUniChar=missingCharMarker;
            }

      
            oldIsTargetUCharDBCS = isTargetUCharDBCS;
            isTargetUCharDBCS = (UBool)(targetUniChar>0x00FF);
            if (targetUniChar != missingCharMarker){

                if (oldIsTargetUCharDBCS != isTargetUCharDBCS){

                    args->offsets[myTargetIndex] = mySourceIndex-1;
                    if (isTargetUCharDBCS) args->target[myTargetIndex++] = UCNV_SO;
                    else args->target[myTargetIndex++] = UCNV_SI;
                  
                  
                    if ((!isTargetUCharDBCS)&&(myTargetIndex+1 >= targetLength)){

                        args->converter->charErrorBuffer[0] = (char) targetUniChar;
                        args->converter->charErrorBufferLength = 1;
                        *err = U_BUFFER_OVERFLOW_ERROR;
                        break;
                    }
                    else if (myTargetIndex+1 >= targetLength){

                        args->converter->charErrorBuffer[0] = (char) (targetUniChar >> 8);
                        args->converter->charErrorBuffer[1] = (char) (targetUniChar & 0x00FF);
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
                    args->target[myTargetIndex++] = (char) (targetUniChar >> 8);
                    
                    if (myTargetIndex < targetLength){
                        args->offsets[myTargetIndex] = mySourceIndex-1;
                        args->target[myTargetIndex++] = (char) targetUniChar;
                    }
                    else{
                        args->converter->charErrorBuffer[0] = (char) targetUniChar;
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
                    args->target += myTargetIndex;
                    args->source += mySourceIndex;
                    args->offsets = args->offsets?args->offsets+myTargetIndex:0;
                    FromU_CALLBACK_OFFSETS_LOGIC_MACRO(args->converter->fromUContext,
                                         args,
                                         args->converter->invalidUCharBuffer,
                                         1,
                                         (UChar32)mySourceChar,
                                         UCNV_UNASSIGNED,
                                         err);
                    isTargetUCharDBCS  = (UBool)(args->converter->fromUnicodeStatus);
                    args->source = saveSource;
                    args->target = saveTarget;
                    args->offsets = saveOffsets;
                    isTargetUCharDBCS  = (UBool)(args->converter->fromUnicodeStatus);
                    if (U_FAILURE (*err))     
                        break;
                    args->converter->invalidUCharLength = 0;
                }
            }
        }
        else{
        
            *err = U_BUFFER_OVERFLOW_ERROR;
            break;
        }

    }


    args->target += myTargetIndex;
    args->source += mySourceIndex;
  

}
const char* getEndOfBuffer_2022_KR(UConverterToUnicodeArgs* args, UErrorCode* err){

	const char* mySource = args->source;

	if (args->source >= args->sourceLimit) 
		return args->sourceLimit;

	do{

        switch(*mySource){

            case ESC_2022:
                {
                    /* Already doing some conversion and found escape Sequence*/
                    if(args->converter->mode == UCNV_SO){
                        *err = U_ILLEGAL_ESCAPE_SEQUENCE;
                        return mySource;
                    }
                    else{
                        changeState_2022(args->converter,
						   &(args->source), 
						   args->sourceLimit,
						   args->flush,
						   err);
                    }
		
                   /* validateKREscape(args);*/
                    if(U_FAILURE(*err))
                        return mySource;
                    else
                        mySource = args->source;
                        mySource--;
                    break;
                }
            case UCNV_SI:
                if(mySource == args->source){
			        args->source++;
			        return getEndOfBuffer_2022_KR(args,err);
		        }
		        else{
			        return mySource;
		        }
                break;

            case UCNV_SO:
                if(mySource == args->source){
			        args->source++;
			        return getEndOfBuffer_2022_KR(args,err);
		        }
		        else{
			        return mySource;
		        }
                break;
            default:
                break;
        }
	
	}while (mySource++ < args->sourceLimit);

	return args->sourceLimit;
}


U_CFUNC void UConverter_toUnicode_ISO_2022_KR(UConverterToUnicodeArgs* args, UErrorCode* err){
    
    const char *mySourceLimit;
	UConverter *saveThis;

	/*Arguments Check*/
	if (U_FAILURE(*err)) 
		return;

	if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source)){
	  *err = U_ILLEGAL_ARGUMENT_ERROR;
	  return;
	}

	do{
        
		/*Find the end of the buffer e.g : shift in sequence or escape sequence | end of Buffer*/
        mySourceLimit =  getEndOfBuffer_2022_KR(args,err);
        
        if (U_FAILURE(*err) || (args->source == args->sourceLimit)) 
			return;

        saveThis = args->converter;
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
	
		if (U_FAILURE(*err) || (args->source == args->sourceLimit)) 
			return;
        
		/* args->source = sourceStart; */
	}while(args->source < args->sourceLimit);
    
	return;
}

/*************************** END ISO2022-KR *********************************/

U_CFUNC UChar32 T_UConverter_getNextUChar_ISO_2022(UConverterToUnicodeArgs* args,
                                     UErrorCode* err)
{
	const char* mySourceLimit;
	/*Arguments Check*/
	if  (args->sourceLimit < args->source){
		*err = U_ILLEGAL_ARGUMENT_ERROR;
		return 0xffff;
	}

	do{

		mySourceLimit =  getEndOfBuffer_2022(args->source, args->sourceLimit, TRUE); 
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

static const UConverterImpl _ISO2022Impl={
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
    1, { 0x1a, 0, 0, 0 }, FALSE, FALSE,
  { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0} /* reserved */
};


const UConverterSharedData _ISO2022Data={
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_ISO2022StaticData, FALSE, &_ISO2022Impl, 
    0
};

/* EBCDICStateful ----------------------------------------------------------- */

U_CFUNC void
_DBCSLoad(UConverterSharedData *sharedData, const uint8_t *raw, UErrorCode *pErrorCode);

U_CFUNC void
_DBCSUnload(UConverterSharedData *sharedData);

U_CFUNC void T_UConverter_toUnicode_EBCDIC_STATEFUL (UConverterToUnicodeArgs *args,
                                             UErrorCode * err)
{
  char *mySource = (char *) args->source;
  UChar *myTarget = args->target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = args->targetLimit - args->target;
  int32_t sourceLength = args->sourceLimit - args->source;
  CompactShortArray *myToUnicode = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;
  int32_t myMode = args->converter->mode;

  myToUnicode = &(args->converter->sharedData->table->dbcs.toUnicode);
    while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          /*gets the corresponding UniChar */
          mySourceChar = (unsigned char) (args->source[mySourceIndex++]);
          if (mySourceChar == UCNV_SI) myMode = UCNV_SI;
          else if (mySourceChar == UCNV_SO) myMode = UCNV_SO;
         else if ((myMode == UCNV_SO) &&
              (args->converter->toUnicodeStatus == 0x00))
            {
              args->converter->toUnicodeStatus = (unsigned char) mySourceChar;
            }
          else
            {
              /*In case there is a state, we update the source char
               *by concatenating the previous char with the current
               *one
               */
              if (args->converter->toUnicodeStatus != 0x00)
                {
                  mySourceChar |= (UChar) (args->converter->toUnicodeStatus << 8);
                  args->converter->toUnicodeStatus = 0x00;
                }
              else mySourceChar &= 0x00FF;

              /*gets the corresponding Unicode codepoint */
              targetUniChar = (UChar) ucmp16_getu (myToUnicode, mySourceChar);

              /*writing the UniChar to the output stream */
              if (targetUniChar < 0xfffe)
                {
                  /*writes the UniChar to the output stream */
                  args->target[myTargetIndex++] = targetUniChar;
                }

              else
                {
                  const char* saveSource = args->source;
                  UChar* saveTarget = args->target;
                  int32_t *saveOffsets = args->offsets;
                  UConverterCallbackReason reason;

                  if (targetUniChar == 0xfffe)
                  {
                    reason = UCNV_UNASSIGNED;
                    *err = U_INVALID_CHAR_FOUND;
                  }
                  else
                  {
                    reason = UCNV_ILLEGAL;
                    *err = U_ILLEGAL_CHAR_FOUND;
                  }

                  if (mySourceChar > 0xff)
                    {
                      args->converter->invalidCharLength = 2;
                      args->converter->invalidCharBuffer[0] = (char) (mySourceChar >> 8);
                      args->converter->invalidCharBuffer[1] = (char) mySourceChar;
                    }
                  else
                    {
                      args->converter->invalidCharLength = 1;
                      args->converter->invalidCharBuffer[0] = (char) mySourceChar;
                    }
                  args->converter->mode = myMode;
                  args->target += myTargetIndex;
                  args->source += mySourceIndex;
                  ToU_CALLBACK_MACRO(args->converter->toUContext,
                                     args,
                                     args->converter->invalidCharBuffer,
                                     args->converter->invalidCharLength,
                                     reason,
                                     err);

                  myMode = args->converter->mode;
                  args->source = saveSource;
                  args->target = saveTarget;
                  args->offsets = saveOffsets;
                  myMode = args->converter->mode;
                  if (U_FAILURE (*err))  break;
                  args->converter->invalidCharLength = 0;
                }
            }
        }
      else
        {
          *err = U_BUFFER_OVERFLOW_ERROR;
          break;
        }
    }

  /*If at the end of conversion we are still carrying state information
   *flush is TRUE, we can deduce that the input stream is truncated
   */
    if (args->converter->toUnicodeStatus
        && (mySourceIndex == sourceLength)
        && (args->flush == TRUE))
      {
        if (U_SUCCESS(*err)) 
          {
            *err = U_TRUNCATED_CHAR_FOUND;
            args->converter->toUnicodeStatus = 0x00;
          }
      }

  args->target += myTargetIndex;
  args->source += mySourceIndex;
  args->converter->mode = myMode;

  return;
}

U_CFUNC void T_UConverter_toUnicode_EBCDIC_STATEFUL_OFFSETS_LOGIC (UConverterToUnicodeArgs * args,
                                                           UErrorCode * err)
{
  char *mySource = (char *) args->source;
  UChar *myTarget = args->target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = args->targetLimit - args->target;
  int32_t sourceLength = args->sourceLimit - args->source;
  CompactShortArray *myToUnicode = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;
  int32_t myMode = args->converter->mode;

  myToUnicode = &args->converter->sharedData->table->dbcs.toUnicode;

    while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          /*gets the corresponding UniChar */
          mySourceChar = (unsigned char) (args->source[mySourceIndex++]);
          if (mySourceChar == UCNV_SI) myMode = UCNV_SI;
          else if (mySourceChar == UCNV_SO) myMode = UCNV_SO;
          else if ((myMode == UCNV_SO) &&
                   (args->converter->toUnicodeStatus == 0x00))
            {
              args->converter->toUnicodeStatus = (unsigned char) mySourceChar;
            }
          else
            {
              /*In case there is a state, we update the source char
               *by concatenating the previous char with the current
               *one
               */
              if (args->converter->toUnicodeStatus != 0x00)
                {
                  mySourceChar |= (UChar) (args->converter->toUnicodeStatus << 8);
                  args->converter->toUnicodeStatus = 0x00;
                }
              else mySourceChar &= 0x00FF;

              /*gets the corresponding Unicode codepoint */
              targetUniChar = (UChar) ucmp16_getu (myToUnicode, mySourceChar);

              /*writing the UniChar to the output stream */
              if (targetUniChar < 0xfffe)
                {
                  /*writes the UniChar to the output stream */
                  {
                        if(myMode == UCNV_SO)
                         args->offsets[myTargetIndex] = mySourceIndex-2; /* double byte */
                        else
                         args->offsets[myTargetIndex] = mySourceIndex-1; /* single byte */
                  }
                  args->target[myTargetIndex++] = targetUniChar;
                }
              else
                {
                  int32_t currentOffset = args->offsets[myTargetIndex-1] + 2;/* Because mySourceIndex was already incremented */
                  int32_t My_i = myTargetIndex;
                  const char* saveSource = args->source;
                  UChar* saveTarget = args->target;
                  int32_t *saveOffsets = args->offsets;
                  UConverterCallbackReason reason;

                  if (targetUniChar == 0xfffe)
                  {
                    reason = UCNV_UNASSIGNED;
                    *err = U_INVALID_CHAR_FOUND;
                  }
                  else
                  {
                    reason = UCNV_ILLEGAL;
                    *err = U_ILLEGAL_CHAR_FOUND;
                  }

                  if (mySourceChar > 0xFF)
                    {
                      args->converter->invalidCharLength = 2;
                      args->converter->invalidCharBuffer[0] = (char) (mySourceChar >> 8);
                      args->converter->invalidCharBuffer[1] = (char) mySourceChar;
                    }
                  else
                    {
                      args->converter->invalidCharLength = 1;
                      args->converter->invalidCharBuffer[0] = (char) mySourceChar;
                    }
                  args->converter->mode = myMode;

                  args->target = args->target + myTargetIndex;
                  args->source = args->source + mySourceIndex;
                  args->offsets = args->offsets?args->offsets+myTargetIndex:0;
                  /* call back handles the offset array */
                  ToU_CALLBACK_OFFSETS_LOGIC_MACRO(args->converter->toUContext,
                                     args,
                                     args->source,
                                     1, 
                                     reason,
                                     err);                  
                  
                  args->source = saveSource;
                  args->target = saveTarget;
                  myMode = args->converter->mode;
                  if (U_FAILURE (*err))   break;
                  args->converter->invalidCharLength = 0;
                  myMode = args->converter->mode;
                }
            }
        }
      else
        {
          *err = U_BUFFER_OVERFLOW_ERROR;
          break;
        }
    }

  /*If at the end of conversion we are still carrying state information
   *flush is TRUE, we can deduce that the input stream is truncated
   */
    if (args->converter->toUnicodeStatus
        && (mySourceIndex == sourceLength)
        && (args->flush == TRUE))
      {
        if (U_SUCCESS(*err)) 
          {
            *err = U_TRUNCATED_CHAR_FOUND;
            args->converter->toUnicodeStatus = 0x00;
          }
      }

  args->target += myTargetIndex;
  args->source += mySourceIndex;
  args->converter->mode = myMode;

  return;
}

U_CFUNC void T_UConverter_fromUnicode_EBCDIC_STATEFUL (UConverterFromUnicodeArgs * args,
                                               UErrorCode * err)

{
  const UChar *mySource = args->source;
  unsigned char *myTarget = (unsigned char *) args->target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = args->targetLimit - args->target;
  int32_t sourceLength = args->sourceLimit - args->source;
  CompactShortArray *myFromUnicode = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;
  UBool isTargetUCharDBCS = (UBool)args->converter->fromUnicodeStatus;
  UBool oldIsTargetUCharDBCS = isTargetUCharDBCS;

  myFromUnicode = &args->converter->sharedData->table->dbcs.fromUnicode;
  /*writing the char to the output stream */
  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          mySourceChar = (UChar) args->source[mySourceIndex++];
          targetUniChar = (UChar) ucmp16_getu (myFromUnicode, mySourceChar);
          oldIsTargetUCharDBCS = isTargetUCharDBCS;
          isTargetUCharDBCS = (UBool)(targetUniChar>0x00FF);
          
          if (targetUniChar != missingCharMarker)
            {
              if (oldIsTargetUCharDBCS != isTargetUCharDBCS)
                {
                  if (isTargetUCharDBCS) args->target[myTargetIndex++] = UCNV_SO;
                  else args->target[myTargetIndex++] = UCNV_SI;
                  
                  
                  if ((!isTargetUCharDBCS)&&(myTargetIndex+1 >= targetLength))
                    {
                      args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (char) targetUniChar;
                      *err = U_BUFFER_OVERFLOW_ERROR;
                      break;
                    }
                  else if (myTargetIndex+1 >= targetLength)
                    {
                      args->converter->charErrorBuffer[0] = (char) (targetUniChar >> 8);
                      args->converter->charErrorBuffer[1] = (char)(targetUniChar & 0x00FF);
                      args->converter->charErrorBufferLength = 2;
                      *err = U_BUFFER_OVERFLOW_ERROR;
                      break;
                    }
                  
                }
              
              if (!isTargetUCharDBCS)
                {
                  args->target[myTargetIndex++] = (char) targetUniChar;
                }
              else
                {
                  args->target[myTargetIndex++] = (char) (targetUniChar >> 8);
                  if (myTargetIndex < targetLength)
                    {
                      args->target[myTargetIndex++] = (char) targetUniChar;
                    }
                  else
                    {
                      args->converter->charErrorBuffer[0] = (char) targetUniChar;
                      args->converter->charErrorBufferLength = 1;
                      *err = U_BUFFER_OVERFLOW_ERROR;
                      break;
                    }
                }
            }
          else
            {
              const UChar* saveSource = args->source;
              char* saveTarget = args->target;
              int32_t *saveOffsets = args->offsets;

              isTargetUCharDBCS = oldIsTargetUCharDBCS;
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
              isTargetUCharDBCS  = (UBool) args->converter->fromUnicodeStatus;
              if (U_FAILURE (*err)) break;
              args->converter->invalidUCharLength = 0;
            }
        }
      else
        {
          *err = U_BUFFER_OVERFLOW_ERROR;
          break;
        }

    }


  args->target += myTargetIndex;
  args->source += mySourceIndex;
  
  args->converter->fromUnicodeStatus = (int32_t)isTargetUCharDBCS;

  return;
}

U_CFUNC void T_UConverter_fromUnicode_EBCDIC_STATEFUL_OFFSETS_LOGIC (UConverterFromUnicodeArgs * args,
                                                             UErrorCode * err)

{
  const UChar *mySource = args->source;
  unsigned char *myTarget = (unsigned char *) args->target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = args->targetLimit - args->target;
  int32_t sourceLength = args->sourceLimit - args->source;
  CompactShortArray *myFromUnicode = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;
  UBool isTargetUCharDBCS = (UBool)args->converter->fromUnicodeStatus;
  UBool oldIsTargetUCharDBCS = isTargetUCharDBCS;
  
  myFromUnicode = &args->converter->sharedData->table->dbcs.fromUnicode;
  /*writing the char to the output stream */
  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          mySourceChar = (UChar) args->source[mySourceIndex++];
          targetUniChar = (UChar) ucmp16_getu (myFromUnicode, mySourceChar);
          oldIsTargetUCharDBCS = isTargetUCharDBCS;
          isTargetUCharDBCS =(UBool) (targetUniChar>0x00FF);
          
          if (targetUniChar != missingCharMarker)
            {
              if (oldIsTargetUCharDBCS != isTargetUCharDBCS)
                {
                  args->offsets[myTargetIndex] = mySourceIndex-1;
                  if (isTargetUCharDBCS) args->target[myTargetIndex++] = UCNV_SO;
                  else args->target[myTargetIndex++] = UCNV_SI;
                  
                  
                  if ((!isTargetUCharDBCS)&&(myTargetIndex+1 >= targetLength))
                    {
                      args->converter->charErrorBuffer[0] = (char) targetUniChar;
                      args->converter->charErrorBufferLength = 1;
                      *err = U_BUFFER_OVERFLOW_ERROR;
                      break;
                    }
                  else if (myTargetIndex+1 >= targetLength)
                    {
                      args->converter->charErrorBuffer[0] = (char) (targetUniChar >> 8);
                      args->converter->charErrorBuffer[1] = (char) (targetUniChar & 0x00FF);
                      args->converter->charErrorBufferLength = 2;
                      *err = U_BUFFER_OVERFLOW_ERROR;
                      break;
                    }
                }
              
              if (!isTargetUCharDBCS)
              {
                  args->offsets[myTargetIndex] = mySourceIndex-1;
                  args->target[myTargetIndex++] = (char) targetUniChar;
              }
              else
              {
                  args->offsets[myTargetIndex] = mySourceIndex-1;
                  args->target[myTargetIndex++] = (char) (targetUniChar >> 8);
                  if (myTargetIndex < targetLength)
                    {
                      args->offsets[myTargetIndex] = mySourceIndex-1;
                      args->target[myTargetIndex++] = (char) targetUniChar;
                    }
                  else
                    {
                      args->converter->charErrorBuffer[0] = (char) targetUniChar;
                      args->converter->charErrorBufferLength = 1;
                      *err = U_BUFFER_OVERFLOW_ERROR;
                      break;
                    }
                }
            }
          else
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
              args->target += myTargetIndex;
              args->source += mySourceIndex;
              args->offsets = args->offsets?args->offsets+myTargetIndex:0;
              FromU_CALLBACK_OFFSETS_LOGIC_MACRO(args->converter->fromUContext,
                                     args,
                                     args->converter->invalidUCharBuffer,
                                     1,
                                     (UChar32)mySourceChar,
                                     UCNV_UNASSIGNED,
                                     err);
              isTargetUCharDBCS  = (UBool)(args->converter->fromUnicodeStatus);
              args->source = saveSource;
              args->target = saveTarget;
              args->offsets = saveOffsets;
              isTargetUCharDBCS  = (UBool)(args->converter->fromUnicodeStatus);
              if (U_FAILURE (*err))     break;
              args->converter->invalidUCharLength = 0;
            }
        }
      else
        {
          *err = U_BUFFER_OVERFLOW_ERROR;
          break;
        }

    }


  args->target += myTargetIndex;
  args->source += mySourceIndex;
  
  args->converter->fromUnicodeStatus = (int32_t)isTargetUCharDBCS;

  return;
}

U_CFUNC UChar32 T_UConverter_getNextUChar_EBCDIC_STATEFUL(UConverterToUnicodeArgs* args,
                                                UErrorCode* err)
{
  UChar myUChar;
  char const *sourceInitial = args->source;
  /*safe keeps a ptr to the beginning in case we need to step back*/
  
  /*Input boundary check*/
  if (args->source >= args->sourceLimit) 
    {
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
      return 0xffff;
    }
  
  /*Checks to see if with have SI/SO shifters
   if we do we change the mode appropriately and we consume the byte*/
  while ((*(args->source) == UCNV_SI) || (*(args->source) == UCNV_SO)) 
    {
      args->converter->mode = *(args->source);
      args->source++;
      sourceInitial = args->source;
      
      /*Rechecks boundary after consuming the shift sequence*/
      if (args->source >= args->sourceLimit) 
        {
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
          return 0xffff;
        }
    }
  
  if (args->converter->mode == UCNV_SI)
    {
      myUChar = ucmp16_getu( (&(args->converter->sharedData->table->dbcs.toUnicode)),
                             ((UChar)(uint8_t)(*(args->source))));
      args->source++;
    }
  else
    {
      /*Lead byte: we Build the codepoint and get the corresponding character
       * and update the source ptr*/
      if ((args->source + 2) > args->sourceLimit) 
        {
          *err = U_TRUNCATED_CHAR_FOUND;
          return 0xffff;
        }

      myUChar = ucmp16_getu( (&(args->converter->sharedData->table->dbcs.toUnicode)),
                             (((UChar)(uint8_t)((*(args->source))) << 8) |((uint8_t)*(args->source+1))) );

      args->source += 2;
    }
  
  if (myUChar < 0xfffe) return myUChar;
  else
    {      
      /* HSYS: Check logic here */
      UChar* myUCharPtr = &myUChar;
      UConverterCallbackReason reason;

      if (myUChar == 0xfffe)
      {
        reason = UCNV_UNASSIGNED;
        *err = U_INVALID_CHAR_FOUND;
      }
      else
      {
        reason = UCNV_ILLEGAL;
        *err = U_ILLEGAL_CHAR_FOUND;
      }

      /*It's is very likely that the ErrorFunctor will write to the
       *internal buffers */
      args->target = myUCharPtr;
      args->targetLimit = myUCharPtr + 1;

      args->converter->fromCharErrorBehaviour(args->converter->toUContext,
                                    args,
                                    sourceInitial,
                                    args->source - sourceInitial,
                                    reason,
                                    err);
      
      /*makes the internal caching transparent to the user*/
      if (*err == U_BUFFER_OVERFLOW_ERROR) *err = U_ZERO_ERROR;
      
      return myUChar;
    }
} 

static const UConverterImpl _EBCDICStatefulImpl={
    UCNV_EBCDIC_STATEFUL,

    _DBCSLoad,
    _DBCSUnload,

    NULL,
    NULL,
    NULL,

    T_UConverter_toUnicode_EBCDIC_STATEFUL,
    T_UConverter_toUnicode_EBCDIC_STATEFUL_OFFSETS_LOGIC,
    T_UConverter_fromUnicode_EBCDIC_STATEFUL,
    T_UConverter_fromUnicode_EBCDIC_STATEFUL_OFFSETS_LOGIC,
    T_UConverter_getNextUChar_EBCDIC_STATEFUL,

    NULL
};

/* Static data is in tools/makeconv/ucnvstat.c for data-based
 * converters. Be sure to update it as well.
 */

const UConverterSharedData _EBCDICStatefulData={
    sizeof(UConverterSharedData), 1,
    NULL, NULL, NULL, FALSE, &_EBCDICStatefulImpl,
    0
};

