/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1998-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/*
* File test.c
*
* Modification History:
*
*   Date          Name        Description
*   02/22/2000    Madhu	      Creation 
*******************************************************************************
*/

#include "unicode/utypes.h"
#include "unicode/udata.h"
#include "unicode/ucnv.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "cintltst.h"

#include <stdio.h>

void TestUDataOpen();
void TestUDataOpenChoiceDemo1();
void TestUDataOpenChoiceDemo2(); 
void TestUDataGetInfo();
void TestUDataGetMemory();


void
addUDataTest(TestNode** root)
{
  addTest(root, &TestUDataOpen,       "udatatst/TestUDataOpen"      );
  addTest(root, &TestUDataOpenChoiceDemo1, "udatatst/TestUDataOpenChoiceDemo1");
  addTest(root, &TestUDataOpenChoiceDemo2, "udatatst/TestUDataOpenChoiceDemo2"); 
  addTest(root, &TestUDataGetInfo,    "udatatst/TestUDataGetInfo"   );
  addTest(root, &TestUDataGetMemory,  "udatatst/TestUDataGetMemory" );

}

void TestUDataOpen(){
   int i;
   UDataMemory *result;
   UErrorCode status=U_ZERO_ERROR;
   const char* memMap[][2]={
	   {"tz", "dat"},
	   {"cnvalias", "dat"},
	   {"unames",   "dat"},
	   {"ibm-1141", "cnv"}
	   
   };
   const char* name = "test";
   const char* type="dat";
   
   char* path=malloc(sizeof(char) * (strlen(u_getDataDirectory()) + strlen("icudata") +1 ) );
   strcat(strcpy(path, u_getDataDirectory()), "icudata");

    
   log_verbose("Testing udata_open()\n");
   result=udata_open(NULL, type, name, &status);
   if(U_FAILURE(status)){
 	  log_err("FAIL: udata_open() failed for path = %s, name=%s, type=%s, \n errorcode=%s\n", u_getDataDirectory(), name, type, myErrorName(status));
   } else {
	   log_verbose("PASS: udata_open worked\n");
	   udata_close(result);
   }

   for(i=0; i<sizeof(memMap)/sizeof(memMap[0]); i++){
	    status=U_ZERO_ERROR;
        result=udata_open(path, memMap[i][1], memMap[i][0], &status);
		if(U_FAILURE(status)) {
 			log_err("FAIL: udata_open() failed for path = %s, name=%s, type=%s, \n errorcode=%s\n", path, memMap[i][0], memMap[i][1], myErrorName(status));
		} else {
			log_verbose("PASS: udata_open worked for path = %s, name=%s, type=%s\n",  path, memMap[i][0], memMap[i][1]);
			udata_close(result);
		}
    }
 
   log_verbose("Testing udata_open() with a non existing binary file\n");
	result=udata_open(path, "tst", "nonexist", &status);
	if(status==U_FILE_ACCESS_ERROR){
		log_verbose("Opening udata_open with non-existing file handled correctly.\n");
		status=U_ZERO_ERROR;
	} else {
		log_err("calling udata_open with non-existing file not handled correctly\n.  Expected: U_FILE_ACCESS_ERROR, Got: %s\n", myErrorName(status));
		if(U_SUCCESS(status)) {
			udata_close(result);
		}
	}

    if(result != NULL){
		log_err("calling udata_open with non-existing file didn't return a null value\n");
	} else {
		log_verbose("calling udat_open with non-existing file returned null as expected\n");
	}

	free(path);
}
static bool_t
isAcceptable1(void *context,
             const char *type, const char *name,
             UDataInfo *pInfo) {
	
    if(
        pInfo->size>=20 &&
        pInfo->isBigEndian==U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily==U_CHARSET_FAMILY &&
        pInfo->dataFormat[0]==0x43 &&   /* dataFormat="CvAl" */
        pInfo->dataFormat[1]==0x76 &&
        pInfo->dataFormat[2]==0x41 &&
        pInfo->dataFormat[3]==0x6c &&
        pInfo->formatVersion[0]==2 ){
		log_verbose("The data from \"%s.%s\" IS acceptable using the verifing function isAcceptable1()\n", name, type);
		return TRUE;
	} else {
		log_verbose("The data from \"%s.%s\" IS NOT acceptable using the verifing function isAcceptable1():-\n"
			"\tsize              = %d\n"
			"\tisBigEndian       = %d\n"
			"\tcharsetFamily     = %d\n"
			"\tformatVersion[0]  = %d\n"
		  	"\tdataVersion[0]    = %d\n"
			"\tdataFormat        = %c%c%c%c\n",
			  name, type, pInfo->size,  pInfo->isBigEndian, pInfo->charsetFamily, pInfo->formatVersion[0], 
			  pInfo->dataVersion[0], pInfo->dataFormat[0], pInfo->dataFormat[1], pInfo->dataFormat[2], 
			  pInfo->dataFormat[3]);  
		log_verbose("Call another verifing function to accept the data\n");
		return FALSE;
	}
}

static bool_t
isAcceptable2(void *context, 
			 const char *type, const char *name,
			 UDataInfo *pInfo){
	if(	pInfo->size>=20 &&
        pInfo->isBigEndian==U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily==U_CHARSET_FAMILY &&
        pInfo->dataFormat[0]==0x75 &&   /* dataFormat="unam" */
        pInfo->dataFormat[1]==0x6e &&
        pInfo->dataFormat[2]==0x61 &&
        pInfo->dataFormat[3]==0x6d &&
        pInfo->formatVersion[0]==1 &&
		pInfo->dataVersion[0]==3 ) {
		log_verbose("The data from \"%s.%s\" IS acceptable using the verifing function isAcceptable2()\n", name, type);
		return TRUE;
	} else {
		log_verbose("The data from \"%s.%s\" IS NOT acceptable using the verifing function isAcceptable2()\n", name, type);

		return FALSE;
	}


}
static bool_t
isAcceptable3(void *context, 
			 const char *type, const char *name,
			 UDataInfo *pInfo){
	
	if(	pInfo->size>=20 &&
        pInfo->isBigEndian==U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily==U_CHARSET_FAMILY &&
        pInfo->dataFormat[0]==0x54 &&   /* dataFormat="test" */
        pInfo->dataFormat[1]==0x65 &&
        pInfo->dataFormat[2]==0x73 &&
        pInfo->dataFormat[3]==0x74 &&
        pInfo->formatVersion[0]==1 &&
		pInfo->dataVersion[0]==1   ) {
   		log_verbose("The data from \"%s.%s\" IS acceptable using the verifing function isAcceptable3()\n", name, type);

		return TRUE;
	} else {
		log_verbose("The data from \"%s.%s\" IS NOT acceptable using the verifing function isAcceptable3()\n", name, type);
		return FALSE;
	}

			
}

void TestUDataOpenChoiceDemo1() {
   
    UDataMemory *result;
    UErrorCode status=U_ZERO_ERROR;	
 
    const char* name[]={
		"cnvalias",
		"unames",
		"test"
	};
    char* type="dat";
   
    result=udata_openChoice(NULL, type, name[0], isAcceptable1, NULL, &status);
    if(U_FAILURE(status)){
        log_err("FAIL: udata_openChoice() failed name=%s, type=%s, \n errorcode=%s\n", name[0], type, myErrorName(status));
	} else {
	    log_verbose("PASS: udata_openChoice worked\n");
		udata_close(result);
	}
     
	result=udata_openChoice(NULL, type, name[1], isAcceptable1, NULL, &status);
	if(U_FAILURE(status)){
		status=U_ZERO_ERROR;
		result=udata_openChoice(NULL, type, name[1], isAcceptable2, NULL, &status);
		if(U_FAILURE(status)){
			log_err("FAIL: udata_openChoice() failed name=%s, type=%s, \n errorcode=%s\n", name[1], type, myErrorName(status));
		}
    }
	
	if(U_SUCCESS(status)){
		udata_close(result);
	}

    result=udata_openChoice(NULL, type, name[2], isAcceptable1, NULL, &status);
	if(U_FAILURE(status)){
		status=U_ZERO_ERROR;
		result=udata_openChoice(NULL, type, name[2], isAcceptable3, NULL, &status);
		if(U_FAILURE(status)){
			log_err("FAIL: udata_openChoice() failed name=%s, type=%s, \n errorcode=%s\n", name[2], type, myErrorName(status));
		}
    }
    
	if(U_SUCCESS(status)){
		udata_close(result);
	}

}
static bool_t
isAcceptable(void *context, 
			 const char *type, const char *name,
			 UDataInfo *pInfo){
	if(	pInfo->size>=20 &&
        pInfo->isBigEndian==U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily==U_CHARSET_FAMILY &&
        pInfo->dataFormat[0]==0x54 &&   /* dataFormat="test" */
        pInfo->dataFormat[1]==0x65 &&
        pInfo->dataFormat[2]==0x73 &&
        pInfo->dataFormat[3]==0x74 &&
        pInfo->formatVersion[0]==1 &&
		pInfo->dataVersion[0]==1   &&
		*((int*)context) == 2 ) {
   		log_verbose("The data from\"%s.%s\" IS acceptable using the verifing function isAcceptable()\n", name, type);

		return TRUE;
	} else {
		log_verbose("The data from \"%s.%s\" IS NOT acceptable using the verifing function isAcceptable()\n", name, type);
		return FALSE;
	}
}

void TestUDataOpenChoiceDemo2() {
	UDataMemory *result;
    UErrorCode status=U_ZERO_ERROR;	
	int i;
	int p=2;
 
    const char* name="test";
    char* type="dat";

	const char* base[]={
		"base_dat",
		"base_test_dat",
		 NULL,
	};
	
	char* path=malloc(sizeof(char) * (strlen(u_getDataDirectory()) + strlen("base[0]") + 1) );
    strcpy(path, u_getDataDirectory());
	strcat(path, "base[0]");	
	
	result=udata_openChoice(path, type, name, isAcceptable, &p, &status);
	if(U_FAILURE(status)){
		log_err("failed to load data");
	}
	if(U_SUCCESS(status) ) {
		udata_close(result);
	}
	strcpy(path, "");
	
	p=0;
	for(i=0;i<sizeof(base)/sizeof(base[0]); i++){
		path=realloc(path, sizeof(char) * (strlen(u_getDataDirectory()) + strlen("base[i]") +1 ) );
		strcat(strcpy(path, u_getDataDirectory()), "base[i]");
		result=udata_openChoice(path, type, name, isAcceptable, &p, &status);
		if(p<2) {
			if(U_FAILURE(status) && status==U_INVALID_FORMAT_ERROR){
				log_verbose("Loads the data but rejects it as expected %s\n");
				status=U_ZERO_ERROR;
				p++;
			}
			else {
				log_err("ERROR: failed to either load the data or to reject the loaded data. ERROR=%s\n", myErrorName(status) );
			}
		}	
		else if(p == 2) {
			if(U_FAILURE(status)) {
				log_err("ERROR: failed to load the data and accept it.  ERROR=%s\n", myErrorName(status) );
			}
			else {
			log_verbose("Loads the data and accepts it for p==2 as expected\n");
			udata_close(result);
			}
		}
		strcpy(path, "");
		    
	}

   free(path);  

}
    


void TestUDataGetInfo() {

	UDataMemory *result;
	/* UDataInfo cf. udata.h */
	static UDataInfo dataInfo={
    30,    /*sizeof(UDataInfo),*/
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    sizeof(UChar),
    0,

    0x54, 0x65, 0x73, 0x74,     /* dataFormat="Test" */
    9, 0, 0, 0,                 /* formatVersion */
    4, 0, 0, 0                  /* dataVersion */
	};
    UErrorCode status=U_ZERO_ERROR;
	const char* name="cnvalias";
	const char* name2="test";
    const char* type="dat";
    
	char* path=malloc(sizeof(char) * (strlen(u_getDataDirectory()) + strlen("icudata") +1 ) );
	strcat(strcpy(path, u_getDataDirectory()), "icudata");


    log_verbose("Testing udata_getInfo() for cnvalias.dat\n");
    result=udata_open(path, type, name, &status);
    if(U_FAILURE(status)){
		log_err("FAIL: udata_open() failed for path = %s, name=%s, type=%s, \n errorcode=%s\n", path, name, type, myErrorName(status));
		return;
    }
	udata_getInfo(result, &dataInfo);
			if(dataInfo.size==20            &&  dataInfo.size!=30 &&
				dataInfo.isBigEndian==U_IS_BIG_ENDIAN       &&  
				dataInfo.charsetFamily==U_CHARSET_FAMILY    &&
				dataInfo.dataFormat[0]==0x43 &&  dataInfo.dataFormat[0]!=0x54 && /* dataFormat="CvAl" and not "Test". The values are set for cnvalias.dat*/
				dataInfo.dataFormat[1]==0x76 &&  dataInfo.dataFormat[1]!=0x65 &&
				dataInfo.dataFormat[2]==0x41 &&  dataInfo.dataFormat[2]!=0x73 &&
				dataInfo.dataFormat[3]==0x6c &&  dataInfo.dataFormat[3]!=0x74 &&
				dataInfo.formatVersion[0]==2 &&  dataInfo.formatVersion[0]!=9 && /*formatVersion is also set to the one for cnvalias*/
				dataInfo.dataVersion[0]==1   &&  dataInfo.dataVersion[0]!=4   && /*dataVersion*/
				dataInfo.dataVersion[1]==3   &&  dataInfo.dataVersion[1]!=0   ){
					log_verbose("PASS: udata_getInfo() filled in the right values\n");
			} else {
				log_err("FAIL: udata_getInfo() filled in the wrong values\n");
			}
	udata_close(result);
	

    log_verbose("Testing udata_getInfo() for test.dat\n");
	result=udata_open(NULL, type, name2, &status);
    if(U_FAILURE(status)) {
	   log_err("FAIL: udata_open() failed for name2=%s, type=%s, \n errorcode=%s\n", name2, type, myErrorName(status));
	   return;
	}
	udata_getInfo(result, &dataInfo);
			if(dataInfo.size==20             &&
				dataInfo.isBigEndian==U_IS_BIG_ENDIAN       &&  
				dataInfo.charsetFamily==U_CHARSET_FAMILY    &&
				dataInfo.dataFormat[0]==0x54 &&   /* dataFormat="Test". The values are set for test.dat*/
				dataInfo.dataFormat[1]==0x65 && 
				dataInfo.dataFormat[2]==0x73 &&  
				dataInfo.dataFormat[3]==0x74 &&  
				dataInfo.formatVersion[0]==1 &&  /*formatVersion is also set to the one for test*/
				dataInfo.dataVersion[0]==1   &&  /*dataVersion*/
				dataInfo.dataVersion[1]==0   ) {
					log_verbose("PASS: udata_getInfo() filled in the right values\n");
			} else {
				log_err("FAIL: udata_getInfo() filled in the wrong values\n");
			}
	udata_close(result);
	free(path);
	
}

void TestUDataGetMemory() {

	FILE *fHandler =NULL;
	UDataMemory *result;
	const uint16_t *table=NULL;
	uint16_t* intValue=0;
    UErrorCode status=U_ZERO_ERROR;
	const char* name="cnvalias";
    const char* type="dat";

	const char* name2="test";

    log_verbose("Testing udata_getMemory for \"cnvalias.dat()\"\n");
    result=udata_openChoice(NULL, type, name, isAcceptable1, NULL, &status);
    if(U_FAILURE(status)){
		 log_err("FAIL: udata_openChoice() failed for name=%s, type=%s, \n errorcode=%s\n", name, type, myErrorName(status));
         return;
	}
    table=(const uint16_t *)udata_getMemory(result);
	
    if(ucnv_countAvailable() !=  table[1+2*(*table)])      /*???*/
		 log_err("FAIL: udata_getMemory() failed ucnv_countAvailable returned = %d, expected = %d ", ucnv_countAvailable(), table[1+2*(*table)]);
	 
	udata_close(result);
    
    log_verbose("Testing udata_getMemory for \"test.dat\"()\n");
    result=udata_openChoice(NULL, type, name2, isAcceptable3, NULL, &status);
    if(U_FAILURE(status)){
		 log_err("FAIL: udata_openChoice() failed for name=%s, type=%s, \n errorcode=%s\n", name2, type, myErrorName(status));
         return;
	}
    intValue=(uint16_t *)udata_getMemory(result);
    /*printf("%d ..... %s", *(intValue), intValue+1));*/
	if( *intValue != 2000 || strcmp((char*)(intValue+1), "YEAR") != 0 )
		log_err("FAIL: udata_getMemory() failed: intValue :- Expected:2000 Got:%d \n\tstringValue:- Expected:YEAR Got:%s\n", *intValue, (intValue+1));
    
   	 udata_close(result);
 }



