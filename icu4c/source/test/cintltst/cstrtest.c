/*
**********************************************************************
* Copyright (C) 1998-2000, International Business Machines Corporation 
* and others.  All Rights Reserved.
**********************************************************************
*
* File cstrtest.c
*
* Modification History:
*
*   Date        Name        Description
*   07/13/2000  Madhu         created
*******************************************************************************
*/


#include "cstring.h"
#include "cintltst.h"

static void TestAPI(void);

void addCStringTest(TestNode** root) {
   
    addTest(root, &TestAPI,   "tsutil/cstrtest/TestAPI");
  
}

void TestAPI(void)
{

    int32_t intValue=0;
    char src[30]="HELLO THERE";
    const char *temp;
    
    log_verbose("Testing the API in cstring\n");
    T_CString_toLowerCase(src);
    if(uprv_strcmp(src, "hello there") != 0){
        log_err("FAIL: *** T_CString_toLowerCase() failed. Expected: \"hello there\", Got: \"%s\"\n", src);
    }
    T_CString_toUpperCase(src);
    if(uprv_strcmp(src, "HELLO THERE") != 0){
        log_err("FAIL: *** T_CString_toUpperCase() failed. Expected: \"HELLO THERE\", Got: \"%s\"\n", src);
    }
     
    intValue=T_CString_stringToInteger("34556", 10);
    if(intValue != 34556){
        log_err("FAIL: ****T_CString_stringToInteger(\"34556\", 10) failed. Expected: 34556, Got: %d\n", intValue);
    }
    intValue=T_CString_stringToInteger("100", 16);
    if(intValue != 256){
        log_err("FAIL: ****T_CString_stringToInteger(\"100\", 16) failed. Expected: 256, Got: %d\n", intValue);
    }
    T_CString_integerToString(src, 34556, 10);
    if(uprv_strcmp(src, "34556") != 0){
        log_err("FAIL: ****integerToString(src, 34566, 10); failed. Expected: \"34556\", Got: %s\n", src);
    }
    T_CString_integerToString(src, 256, 16);
    if(uprv_strcmp(src, "100") != 0){
        log_err("FAIL: ****integerToString(src, 256, 16); failed. Expected: \"100\", Got: %s\n", src);
    }

    uprv_strcpy(src, "this is lower case");
    if(T_CString_stricmp(src, "THIS is lower CASE") != 0){
        log_err("FAIL: *****T_CString_stricmp() failed.");
    }
    if((intValue=T_CString_stricmp(NULL, "first string is null") )!= -1){
        log_err("FAIL: T_CString_stricmp() where the first string is null failed. Expected: -1, returned %d\n", intValue);
    }
    if((intValue=T_CString_stricmp("second string is null", NULL)) != 1){
        log_err("FAIL: T_CString_stricmp() where the second string is null failed. Expected: 1, returned %d\n", intValue);
    }
    if((intValue=T_CString_stricmp(NULL, NULL)) != 0){
        log_err("FAIL: T_CString_stricmp(NULL, NULL) failed.  Expected:  0, returned %d\n", intValue);;
    }
    if((intValue=T_CString_stricmp("", "")) != 0){
        log_err("FAIL: T_CString_stricmp(\"\", \"\") failed.  Expected:  0, returned %d\n", intValue);;
    }
    if((intValue=T_CString_stricmp("", "abc")) != -1){
        log_err("FAIL: T_CString_stricmp(\"\", \"abc\") failed.  Expected: -1, returned %d\n", intValue);
    }
    if((intValue=T_CString_stricmp("abc", "")) != 1){
        log_err("FAIL: T_CString_stricmp(\"abc\", \"\") failed.  Expected: 1, returned %d\n", intValue);
    }

    temp=uprv_strdup("strdup");
    if(uprv_strcmp(temp, "strdup") !=0 ){
        log_err("FAIL: uprv_strdup() failed. Expected: \"strdup\", Got: %s\n", temp);
    }
    
}